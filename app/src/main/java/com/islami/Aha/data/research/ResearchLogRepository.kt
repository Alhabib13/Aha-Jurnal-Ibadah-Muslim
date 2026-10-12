package com.islami.Aha.data.research

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.islami.Aha.data.local.DeletedSyncDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.quran.TilawahRange
import com.islami.Aha.data.local.ResearchLogDao
import com.islami.Aha.data.model.DeletedSyncRecord
import com.islami.Aha.data.model.ResearchLogEntity
import com.islami.Aha.data.repository.CloudSyncStatus
import com.islami.Aha.worker.SyncResearchLogsWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Slot log beserta id lokal (hanya terisi untuk slot yang membutuhkannya). */
data class SlotRef(val slot: IbadahSlot, val localId: String?)

/**
 * Menulis log penelitian ke tabel lokal `research_logs` lalu mengunggahnya ke
 * koleksi Firestore `logs`. Berjalan berdampingan dengan jalur lama
 * (habit_completion_records -> habit_completions) dan tidak pernah menyentuhnya.
 *
 * Fungsi record/remove aman dipanggil di dalam `appDatabase.withTransaction`
 * (hanya akses Room). Upload selalu lewat [scheduleUpload] di luar transaksi.
 */
@Singleton
class ResearchLogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val researchLogDao: ResearchLogDao,
    private val deletedSyncDao: DeletedSyncDao,
    private val sharedPreferences: SharedPreferences,
    private val habitDao: HabitDao,
    private val tilawahState: TilawahStateRepository
) {
    companion object {
        const val RECORD_TYPE = "RESEARCH_LOG"
        private const val UNIQUE_WORK_NAME = "sync_research_logs"
        private const val WRITE_TIMEOUT_MS = 15_000L
        private const val FLUSH_TIMEOUT_MS = 5_000L
        private const val UPLOAD_FAILED_NOTICE = "Sinkronisasi log ibadah ke cloud belum berhasil."
        private const val RESTORE_TIMEOUT_MS = 30_000L

        /** Pengguna sudah membaca pemberitahuan penelitian (dialog sekali muncul). */
        const val KEY_RESEARCH_NOTICE_ACK = "research_notice_ack_v1"
        private const val KEY_RESTORED_PREFIX = "research_logs_restored_"

        fun dzikirSlot(detail: String): SlotRef = SlotRef(
            IbadahSlot(
                ResearchSchema.Jenis.DZIKIR,
                detail,
                if (detail == ResearchSchema.Detail.PAGI) "Dzikir Pagi" else "Dzikir Petang"
            ),
            null
        )

        // Kunci jadwal sholat yang ditulis HomeViewModel.
        private const val KEY_PRAYER_TIME_SOURCE = "prayer_time_source"
        private const val KEY_PRAYER_TIME_LAST_SYNC_AT = "prayer_time_last_sync_at"
        private const val PRAYER_TIME_SOURCE_API = "API"
        private val IQRO_SLOT = IbadahSlot(ResearchSchema.Jenis.IQRO, null, null)

        /** Habit bawaan (tabel `habits`). */
        fun slotForDefaultHabit(name: String, category: String): SlotRef? =
            ResearchMapping.fromDefaultHabit(name, category)?.let { SlotRef(it, null) }

        /** Habit sunnah (tabel `user_habits`); id habit jadi id lokal bila dibutuhkan. */
        fun slotForSunnahHabit(id: String, name: String, category: String): SlotRef? =
            ResearchMapping.fromSunnahHabit(name, category)?.let { slot ->
                val needsId = ResearchSchema.butuhIdLokal(slot.jenisIbadah, slot.namaDetail)
                SlotRef(slot, if (needsId) id else null)
            }
    }

    private val auth: FirebaseAuth? by lazy { runCatching { FirebaseAuth.getInstance() }.getOrNull() }
    private val firestore: FirebaseFirestore? by lazy { runCatching { FirebaseFirestore.getInstance() }.getOrNull() }

    private fun currentUid(): String? = auth?.currentUser?.uid

    /**
     * Catat satu ibadah "dikerjakan". [tanggal] = hari ibadahnya (bisa hari lampau),
     * [jamPencatatanMs] = saat pengguna mencatat.
     */
    suspend fun recordDikerjakan(
        ref: SlotRef?,
        tanggal: String,
        jamPencatatanMs: Long = System.currentTimeMillis(),
        sumber: String = ResearchSchema.Sumber.TIDAK_DIKETAHUI,
        jumlah: Int? = null,
        jilid: Int? = null,
        jumlahAyat: Int? = null,
        juz: Int? = null
    ) {
        ref ?: return
        // Data penelitian hanya dikumpulkan setelah pengguna diberi tahu.
        if (!isNoticeAcknowledged()) return
        val key = slotKeyOrNull(ref, tanggal) ?: return
        val selisih = if (ref.slot.jenisIbadah in ResearchSchema.JENIS_DENGAN_SELISIH) {
            hitungSelisihAdzan(ref.slot, tanggal, jamPencatatanMs)
        } else {
            null
        }
        researchLogDao.upsert(
            ResearchLogEntity(
                slotKey = key,
                tanggal = tanggal,
                jenisIbadah = ref.slot.jenisIbadah,
                namaDetail = ref.slot.namaDetail,
                namaDetailAsli = ref.slot.namaDetailAsli?.take(100),
                status = ResearchSchema.Status.DIKERJAKAN,
                jumlah = jumlah,
                jilid = jilid,
                jamPencatatan = jamPencatatanMs,
                sumberPencatatan = sumber,
                ownerUid = currentUid(),
                isSynced = false,
                updatedAt = System.currentTimeMillis(),
                jumlahAyat = jumlahAyat,
                juz = juz,
                offsetWaktu = ResearchTime.offsetString(jamPencatatanMs),
                selisihMenitDariAdzan = selisih
            )
        )
        // Dicentang ulang sebelum hapusan sempat terkirim: cukup timpa dokumennya.
        deletedSyncDao.deleteByRecordIdAndType(key, RECORD_TYPE)
    }

    /** Centang dibatalkan (keputusan C): baris dihapus, bukan diubah jadi "tidak". */
    suspend fun remove(ref: SlotRef?, tanggal: String) {
        ref ?: return
        removeBySlotKey(slotKeyOrNull(ref, tanggal) ?: return)
    }

    suspend fun removeBySlotKey(key: String) {
        researchLogDao.getBySlotKey(key) ?: return
        researchLogDao.deleteBySlotKey(key)
        if (currentUid() != null) {
            deletedSyncDao.insert(DeletedSyncRecord(recordId = key, recordType = RECORD_TYPE))
        }
    }

    // ---------- pemberitahuan penelitian ----------

    fun isNoticeAcknowledged(): Boolean = sharedPreferences.getBoolean(KEY_RESEARCH_NOTICE_ACK, false)

    fun acknowledgeNotice() {
        sharedPreferences.edit().putBoolean(KEY_RESEARCH_NOTICE_ACK, true).apply()
    }

    // ---------- dzikir / tilawah / iqro ----------

    fun observeDikerjakan(tanggal: String, jenis: String): Flow<List<ResearchLogEntity>> =
        researchLogDao.observeDikerjakan(tanggal, jenis)

    fun observeLatestIqro(): Flow<ResearchLogEntity?> = researchLogDao.observeLatestIqro()

    /** Dzikir pagi/petang: slot tetap, centang = catat, centang lagi = hapus. */
    suspend fun toggleDzikir(detail: String, tanggal: String) {
        require(detail == ResearchSchema.Detail.PAGI || detail == ResearchSchema.Detail.PETANG)
        val ref = dzikirSlot(detail)
        val key = slotKeyOrNull(ref, tanggal) ?: return
        if (researchLogDao.getBySlotKey(key) != null) remove(ref, tanggal) else recordDikerjakan(ref, tanggal)
    }

    /**
     * Tilawah model rentang: satu baris per sesi (idLokal acak), tidak ditimpa & tidak dijumlahkan.
     * jumlah = halaman tersentuh (untuk tampilan), jumlah_ayat = ukuran halus (untuk model),
     * juz = juz awal sesi. Semuanya dihitung QuranCalculator, bukan diketik pengguna.
     */
    suspend fun addTilawahSession(jenisSesi: String, range: TilawahRange, tanggal: String): Boolean {
        if (jenisSesi !in ResearchSchema.DETAIL_PER_JENIS.getValue(ResearchSchema.Jenis.TILAWAH)) return false
        if (range.jumlahHalaman !in ResearchSchema.TILAWAH_HALAMAN ||
            range.jumlahAyat !in ResearchSchema.TILAWAH_AYAT ||
            range.juzAwal !in ResearchSchema.JUZ
        ) return false
        recordDikerjakan(
            SlotRef(IbadahSlot(ResearchSchema.Jenis.TILAWAH, jenisSesi, null), UUID.randomUUID().toString()),
            tanggal,
            jumlah = range.jumlahHalaman,
            jumlahAyat = range.jumlahAyat,
            juz = range.juzAwal
        )
        return true
    }

    private suspend fun hitungSelisihAdzan(slot: IbadahSlot, tanggal: String, jamMs: Long): Int? {
        val acuan = ResearchTime.habitAcuanAdzan(slot.jenisIbadah, slot.namaDetail) ?: return null
        val adzan = runCatching { habitDao.getFardhuHabits().firstOrNull { it.name == acuan }?.time }.getOrNull()
        return ResearchTime.selisihMenitDariAdzan(
            jamPencatatanMs = jamMs,
            tanggalIbadah = tanggal,
            adzanHHmm = adzan,
            jadwalDariLokasi = sharedPreferences.getString(KEY_PRAYER_TIME_SOURCE, null) == PRAYER_TIME_SOURCE_API,
            jadwalDisinkronMs = sharedPreferences.getLong(KEY_PRAYER_TIME_LAST_SYNC_AT, 0L)
        )
    }

    /** Iqro (dan metode sejenis): posisi terakhir, satu baris per hari yang menimpa. */
    suspend fun saveIqro(jilid: Int, halaman: Int, tanggal: String): Boolean {
        if (jilid !in ResearchSchema.IQRO_JILID || halaman !in ResearchSchema.IQRO_HALAMAN) return false
        recordDikerjakan(SlotRef(IQRO_SLOT, null), tanggal, jumlah = halaman, jilid = jilid)
        tilawahState.setPenandaIqro(jilid, halaman)
        return true
    }

    // ---------- pemulihan dari cloud ----------

    /**
     * Isi ulang tabel lokal dari koleksi `logs` milik akun ini (setelah install ulang /
     * login di HP lain), supaya riwayat dzikir & tilawah tetap tampil di heatmap.
     * Hanya cache tampilan: baris ditandai terkirim dan tidak pernah membuat log baru.
     * Dijalankan sekali per akun per instalasi untuk menghemat kuota baca Firestore.
     */
    suspend fun restoreFromCloudIfNeeded(): CloudSyncStatus {
        val uid = currentUid() ?: return CloudSyncStatus.success()
        if (sharedPreferences.getBoolean(KEY_RESTORED_PREFIX + uid, false)) return CloudSyncStatus.success()
        val col = firestore?.collection(ResearchSchema.COLLECTION_LOGS) ?: return CloudSyncStatus.success()
        return runCatching {
            val snapshot = withTimeout(RESTORE_TIMEOUT_MS) {
                col.whereEqualTo(ResearchSchema.Log.USER_ID, uid).get().await()
            }
            val prefix = "${uid}_"
            val rows = snapshot.documents.mapNotNull { doc ->
                if (!doc.id.startsWith(prefix)) return@mapNotNull null
                ResearchLogEntity(
                    slotKey = doc.id.removePrefix(prefix),
                    tanggal = doc.getString(ResearchSchema.Log.TANGGAL) ?: return@mapNotNull null,
                    jenisIbadah = doc.getString(ResearchSchema.Log.JENIS_IBADAH) ?: return@mapNotNull null,
                    namaDetail = doc.getString(ResearchSchema.Log.NAMA_DETAIL),
                    namaDetailAsli = doc.getString(ResearchSchema.Log.NAMA_DETAIL_ASLI),
                    status = doc.getString(ResearchSchema.Log.STATUS) ?: return@mapNotNull null,
                    jumlah = doc.getLong(ResearchSchema.Log.JUMLAH)?.toInt(),
                    jilid = doc.getLong(ResearchSchema.Log.JILID)?.toInt(),
                    jamPencatatan = doc.getTimestamp(ResearchSchema.Log.JAM_PENCATATAN)?.toDate()?.time,
                    sumberPencatatan = doc.getString(ResearchSchema.Log.SUMBER_PENCATATAN),
                    ownerUid = uid,
                    isSynced = true,
                    updatedAt = 0L,
                    jumlahAyat = doc.getLong(ResearchSchema.Log.JUMLAH_AYAT)?.toInt(),
                    juz = doc.getLong(ResearchSchema.Log.JUZ)?.toInt(),
                    offsetWaktu = doc.getString(ResearchSchema.Log.OFFSET_WAKTU) ?: return@mapNotNull null,
                    selisihMenitDariAdzan = doc.getLong(ResearchSchema.Log.SELISIH_MENIT_DARI_ADZAN)?.toInt()
                )
            }
            researchLogDao.insertIgnore(rows)
            sharedPreferences.edit().putBoolean(KEY_RESTORED_PREFIX + uid, true).apply()
            CloudSyncStatus.success(rows.size)
        }.getOrElse { error ->
            failure(0, retry = error.isRetryable(), error)
        }
    }

    fun scheduleUpload() {
        runCatching {
            val request = OneTimeWorkRequestBuilder<SyncResearchLogsWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            // APPEND_OR_REPLACE: perubahan yang masuk saat worker berjalan tetap dapat giliran.
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }.onFailure { runCatching { com.islami.Aha.util.logCrashlyticsSafe(it) } }
    }

    /** Unggah hapusan & baris tertunda milik akun yang sedang login. */
    suspend fun syncPending(): CloudSyncStatus {
        val uid = currentUid() ?: return CloudSyncStatus.success()
        val col = firestore?.collection(ResearchSchema.COLLECTION_LOGS) ?: return CloudSyncStatus.success()
        var changed = 0
        var firstIssue: CloudSyncStatus? = null

        val pendingDeletes = deletedSyncDao.getAllDeletedRecords().filter { it.recordType == RECORD_TYPE }
        for (record in pendingDeletes) {
            val result = runCatching {
                withTimeout(WRITE_TIMEOUT_MS) {
                    col.document(ResearchSchema.logId(uid, record.recordId)).delete().await()
                }
            }
            val error = result.exceptionOrNull()
            when {
                error == null -> { deletedSyncDao.deleteRecordById(record.id); changed++ }
                // Dokumen tidak ada / bukan milik akun ini: tidak ada yang perlu dihapus.
                error.isPermissionDenied() -> deletedSyncDao.deleteRecordById(record.id)
                error.isRetryable() -> return failure(changed, retry = true, error)
                else -> firstIssue = firstIssue ?: failure(changed, retry = false, error)
            }
        }

        for (row in researchLogDao.getPendingUploads(uid)) {
            val result = runCatching {
                withTimeout(WRITE_TIMEOUT_MS) {
                    col.document(ResearchSchema.logId(uid, row.slotKey)).set(row.toFirestore(uid)).await()
                }
            }
            val error = result.exceptionOrNull()
            when {
                error == null -> { researchLogDao.markSynced(row.slotKey, row.updatedAt, uid); changed++ }
                error.isRetryable() -> return failure(changed, retry = true, error)
                // Ditolak rules (bentuk data salah): lanjut ke baris lain, jangan blokir antrean.
                else -> firstIssue = firstIssue ?: failure(changed, retry = false, error)
            }
        }
        return firstIssue?.copy(changedCount = changed) ?: CloudSyncStatus.success(changed)
    }

    /**
     * Sebelum logout: coba unggah atas nama akun yang masih login, lalu kosongkan
     * antrean lokal. Gagal karena offline = log hilang; lebih baik daripada
     * terunggah atas nama akun berikutnya.
     */
    suspend fun flushAndClear() {
        runCatching { withTimeoutOrNull(FLUSH_TIMEOUT_MS) { syncPending() } }
        clearLocal()
    }

    suspend fun clearLocal() {
        researchLogDao.deleteAll()
        deletedSyncDao.deleteByType(RECORD_TYPE)
        clearRestoreFlags()
        tilawahState.clearLocal()
    }

    private fun clearRestoreFlags() {
        val editor = sharedPreferences.edit()
        sharedPreferences.all.keys.filter { it.startsWith(KEY_RESTORED_PREFIX) }.forEach { editor.remove(it) }
        editor.apply()
    }

    /** Akun berganti tanpa lewat logout: buang baris milik akun lain. */
    suspend fun clearRowsNotOwnedBy(uid: String) {
        researchLogDao.deleteNotOwnedBy(uid)
        deletedSyncDao.deleteByType(RECORD_TYPE)
        clearRestoreFlags()
        tilawahState.clearLocal()
    }

    private fun slotKeyOrNull(ref: SlotRef, tanggal: String): String? =
        runCatching {
            ResearchSchema.slotKey(tanggal, ref.slot.jenisIbadah, ref.slot.namaDetail, ref.localId)
        }.onFailure { runCatching { com.islami.Aha.util.logCrashlyticsSafe(it) } }.getOrNull()

    private fun ResearchLogEntity.toFirestore(uid: String): Map<String, Any?> = mapOf(
        ResearchSchema.Log.USER_ID to uid,
        ResearchSchema.Log.TANGGAL to tanggal,
        ResearchSchema.Log.JENIS_IBADAH to jenisIbadah,
        ResearchSchema.Log.NAMA_DETAIL to namaDetail,
        ResearchSchema.Log.NAMA_DETAIL_ASLI to namaDetailAsli,
        ResearchSchema.Log.STATUS to status,
        ResearchSchema.Log.JUMLAH to jumlah?.toLong(),
        ResearchSchema.Log.JILID to jilid?.toLong(),
        ResearchSchema.Log.JAM_PENCATATAN to jamPencatatan?.let { Timestamp(Date(it)) },
        ResearchSchema.Log.SUMBER_PENCATATAN to sumberPencatatan,
        ResearchSchema.Log.JUMLAH_AYAT to jumlahAyat?.toLong(),
        ResearchSchema.Log.JUZ to juz?.toLong(),
        ResearchSchema.Log.OFFSET_WAKTU to offsetWaktu,
        ResearchSchema.Log.SELISIH_MENIT_DARI_ADZAN to selisihMenitDariAdzan?.toLong()
    )

    private fun failure(changed: Int, retry: Boolean, error: Throwable): CloudSyncStatus {
        runCatching { com.islami.Aha.util.logCrashlyticsSafe(error) }
        return CloudSyncStatus.failure(UPLOAD_FAILED_NOTICE, shouldRetry = retry, changedCount = changed)
    }

    private fun Throwable.isPermissionDenied(): Boolean =
        this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.PERMISSION_DENIED

    private fun Throwable.isRetryable(): Boolean =
        this is TimeoutCancellationException ||
            this is java.io.IOException ||
            (this is FirebaseFirestoreException && code in setOf(
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                FirebaseFirestoreException.Code.UNAUTHENTICATED,
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
                FirebaseFirestoreException.Code.ABORTED,
                FirebaseFirestoreException.Code.INTERNAL
            ))
}
