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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.islami.Aha.data.local.DeletedSyncDao
import com.islami.Aha.data.local.DzikirHabitDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.quran.TilawahRange
import com.islami.Aha.data.local.ResearchLogDao
import com.islami.Aha.data.model.DeletedSyncRecord
import com.islami.Aha.data.model.ResearchLogEntity
import com.islami.Aha.data.repository.CloudSyncStatus
import com.islami.Aha.util.GenderProfile
import com.islami.Aha.util.UserPreferencesManager
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
    private val tilawahState: TilawahStateRepository,
    private val dzikirHabitDao: DzikirHabitDao
) {
    companion object {
        const val RECORD_TYPE = "RESEARCH_LOG"
        private const val UNIQUE_WORK_NAME = "sync_research_logs"
        private const val WRITE_TIMEOUT_MS = 15_000L
        private const val FLUSH_TIMEOUT_MS = 5_000L
        private const val UPLOAD_FAILED_NOTICE = "Sinkronisasi log ibadah ke cloud belum berhasil."
        private const val RESTORE_TIMEOUT_MS = 30_000L

        /**
         * v1.2.0 awal: dialog "Mengerti" tanpa pilihan menolak. TIDAK dihitung sebagai persetujuan:
         * pemiliknya dianggap belum memutuskan sampai menjawab layar persetujuan yang baru.
         * Kunci ini dihapus begitu keputusan disimpan.
         */
        const val KEY_RESEARCH_NOTICE_ACK = "research_notice_ack_v1"
        /** Keputusan pengguna atas penelitian: [CONSENT_SETUJU] / [CONSENT_TOLAK]; tidak ada = belum memutuskan. */
        const val KEY_RESEARCH_CONSENT = "research_consent_v2"
        const val CONSENT_SETUJU = "setuju"
        const val CONSENT_TOLAK = "tolak"
        private const val KEY_RESTORED_PREFIX = "research_logs_restored_"
        /** Sumber yang sah untuk "Tertinggal" (sama dengan validJamDanSumber di firestore.rules). */
        val SUMBER_TERTINGGAL = setOf(ResearchSchema.Sumber.DARI_NOTIFIKASI, ResearchSchema.Sumber.BUKA_APLIKASI)
        const val KEY_KODE_RESPONDEN = "research_kode_responden"
        /** Isi profil penelitian terakhir yang berhasil dikirim, per akun. */
        private const val KEY_PROFIL_TERKIRIM_PREFIX = "research_profil_terkirim_"
        /**
         * UID yang penarikan persetujuannya belum tercatat di server (ditarik_pada). Tidak ikut
         * dihapus saat logout, supaya penarikan yang belum terkirim tetap terkirim begitu akun
         * yang sama masuk lagi; dibersihkan saat terkirim atau saat pengguna setuju lagi.
         */
        const val KEY_PENARIKAN_TERTUNDA = "research_penarikan_tertunda_uid"
        /** Tanggal Mode Cuti sejauh ini yang disamakan dengan baris udzur (satu siklus + sisa). */
        const val UDZUR_JENDELA_HARI = 45L
        private const val KATEGORI_PUASA_WAJIB = "Puasa Wajib"
        /** Lima slot sholat wajib (perempuan tidak punya slot Jumat). */
        private val SLOT_SHOLAT_WAJIB = listOf(
            ResearchSchema.Detail.SUBUH, ResearchSchema.Detail.DZUHUR, ResearchSchema.Detail.ASHAR,
            ResearchSchema.Detail.MAGHRIB, ResearchSchema.Detail.ISYA
        ).map { IbadahSlot(ResearchSchema.Jenis.SHOLAT_WAJIB, it, null) }


        private fun prefixSesiDzikir(tanggal: String, slug: String, idHabit: String) =
            "${tanggal}_${ResearchSchema.Jenis.DZIKIR}_${slug}_${idHabit}_"

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
        sumber: String = ResearchSchema.Sumber.BUKA_APLIKASI,
        jumlah: Int? = null,
        jilid: Int? = null,
        jumlahAyat: Int? = null,
        juz: Int? = null,
        modePencatatan: String? = null,
        waktuDzikir: String? = null,
        targetHitungan: Int? = null,
        durasiDetik: Int? = null
    ) {
        ref ?: return
        // Catatan SELALU disimpan di HP (dzikir/tilawah/iqro hanya ada di tabel ini).
        // Persetujuan penelitian hanya mengatur pengunggahan: lihat syncPending().
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
                // Aturan kejujuran data: dzikir mode centang tidak pernah punya jumlah.
                jumlah = if (modePencatatan == ResearchSchema.ModePencatatan.CENTANG) null else jumlah,
                jilid = jilid,
                jamPencatatan = jamPencatatanMs,
                sumberPencatatan = sumber,
                ownerUid = currentUid(),
                isSynced = false,
                updatedAt = System.currentTimeMillis(),
                jumlahAyat = jumlahAyat,
                juz = juz,
                offsetWaktu = ResearchTime.offsetString(jamPencatatanMs),
                selisihMenitDariAdzan = selisih,
                modePencatatan = modePencatatan,
                waktuDzikir = waktuDzikir,
                targetHitungan = targetHitungan,
                // Mode centang: jumlah & durasi WAJIB null (tidak tahu != nol, jangan diisi target).
                durasiDetik = if (modePencatatan == ResearchSchema.ModePencatatan.LAYAR) durasiDetik else null
            )
        )
        // Dicentang ulang sebelum hapusan sempat terkirim: cukup timpa dokumennya.
        deletedSyncDao.deleteByRecordIdAndType(key, RECORD_TYPE)
    }

    /**
     * Pengguna sendiri menekan "Tertinggal" untuk sholat wajib, di notifikasi
     * ([sumber] = dari_notifikasi) atau di kartu Beranda (buka_aplikasi): baris "tidak" dengan
     * jam = saat menekan (beda dari "tidak" hasil penyimpulan, yang jam & sumbernya null).
     * ID sama dengan baris "dikerjakan", jadi centang belakangan menimpanya. Baris "dikerjakan"
     * yang sudah ada tidak pernah ditimpa jadi "tidak". Mengembalikan true bila baris ditulis.
     */
    suspend fun recordTidakDariNotifikasi(
        ref: SlotRef?,
        tanggal: String,
        jamPencatatanMs: Long = System.currentTimeMillis(),
        sumber: String = ResearchSchema.Sumber.DARI_NOTIFIKASI
    ): Boolean {
        if (sumber !in SUMBER_TERTINGGAL) return false
        ref ?: return false
        if (ref.slot.jenisIbadah !in ResearchSchema.JENIS_BOLEH_TERTINGGAL) return false
        val key = slotKeyOrNull(ref, tanggal) ?: return false
        if (researchLogDao.getBySlotKey(key)?.status == ResearchSchema.Status.DIKERJAKAN) return false
        researchLogDao.upsert(
            ResearchLogEntity(
                slotKey = key,
                tanggal = tanggal,
                jenisIbadah = ref.slot.jenisIbadah,
                namaDetail = ref.slot.namaDetail,
                namaDetailAsli = null,
                status = ResearchSchema.Status.TIDAK,
                jumlah = null,
                jilid = null,
                jamPencatatan = jamPencatatanMs,
                sumberPencatatan = sumber,
                ownerUid = currentUid(),
                isSynced = false,
                updatedAt = System.currentTimeMillis(),
                jumlahAyat = null,
                juz = null,
                offsetWaktu = ResearchTime.offsetString(jamPencatatanMs),
                selisihMenitDariAdzan = null,
                modePencatatan = null,
                waktuDzikir = null,
                targetHitungan = null,
                durasiDetik = null
            )
        )
        deletedSyncDao.deleteByRecordIdAndType(key, RECORD_TYPE)
        return true
    }

    // ---------- Mode Cuti: status udzur (Tahap 5) ----------

    /**
     * Menyamakan baris "udzur" dengan tanggal Mode Cuti dalam [jendelaHari] hari terakhir.
     *
     * - Tiap tanggal haid: slot sholat wajib (dan puasa Ramadan bila tanggalnya di bulan Ramadan
     *   dan habit Puasa Wajib ada) yang BELUM punya baris mendapat "udzur". Baris yang sudah ada
     *   ("dikerjakan" sebelum haid mulai, atau "tidak") dibiarkan.
     * - Baris "udzur" pada tanggal yang bukan lagi tanggal haid (Mode Cuti dimatikan) dihapus,
     *   termasuk di server. Hanya fungsi ini yang menulis "udzur", jadi udzur selalu = tanggal haid.
     *
     * Tanpa ini hari haid tampak seperti "tidak mencatat" dan membiaskan label profil.
     * Cakupan udzur (sholat wajib & puasa) MENUNGGU VALIDASI PAKAR, sama dengan firestore.rules.
     * @return jumlah baris yang ditulis atau dihapus.
     */
    suspend fun sinkronUdzur(
        tanggalHaid: Set<String>,
        perempuan: Boolean,
        hariIni: java.time.LocalDate = com.islami.Aha.util.DateUtils.hariIni(),
        jendelaHari: Long = UDZUR_JENDELA_HARI
    ): Int {
        val sejak = hariIni.minusDays(jendelaHari)
        val target = if (!perempuan) emptySet() else tanggalHaid.mapNotNull { t ->
            runCatching { java.time.LocalDate.parse(t) }.getOrNull()?.takeIf { !it.isBefore(sejak) && !it.isAfter(hariIni) }
        }.toSet()
        var berubah = 0
        // Udzur yang tanggalnya sudah bukan tanggal haid: hapus.
        researchLogDao.getByStatusBetween(ResearchSchema.Status.UDZUR, sejak.toString(), hariIni.toString())
            .filter { runCatching { java.time.LocalDate.parse(it.tanggal) }.getOrNull() !in target }
            .forEach { removeBySlotKey(it.slotKey); berubah++ }
        if (target.isEmpty()) return berubah
        val adaPuasaWajib = runCatching {
            habitDao.getHabitsSnapshot().any { it.category == KATEGORI_PUASA_WAJIB }
        }.getOrDefault(false)
        for (tanggal in target.sorted()) {
            val slots = SLOT_SHOLAT_WAJIB.toMutableList()
            if (adaPuasaWajib && bulanRamadan(tanggal)) slots += IbadahSlot(ResearchSchema.Jenis.PUASA, ResearchSchema.Detail.RAMADAN, null)
            for (slot in slots) {
                val key = slotKeyOrNull(SlotRef(slot, null), tanggal.toString()) ?: continue
                if (researchLogDao.getBySlotKey(key) != null) continue
                val sekarang = System.currentTimeMillis()
                researchLogDao.upsert(
                    ResearchLogEntity(
                        slotKey = key,
                        tanggal = tanggal.toString(),
                        jenisIbadah = slot.jenisIbadah,
                        namaDetail = slot.namaDetail,
                        namaDetailAsli = null,
                        status = ResearchSchema.Status.UDZUR,
                        jumlah = null,
                        jilid = null,
                        // Hasil penyimpulan aplikasi: tanpa jam & sumber (aturan validJamDanSumber).
                        jamPencatatan = null,
                        sumberPencatatan = null,
                        ownerUid = currentUid(),
                        isSynced = false,
                        updatedAt = sekarang,
                        jumlahAyat = null,
                        juz = null,
                        offsetWaktu = ResearchTime.offsetString(sekarang),
                        selisihMenitDariAdzan = null,
                        modePencatatan = null,
                        waktuDzikir = null,
                        targetHitungan = null,
                        durasiDetik = null
                    )
                )
                deletedSyncDao.deleteByRecordIdAndType(key, RECORD_TYPE)
                berubah++
            }
        }
        return berubah
    }

    private fun bulanRamadan(tanggal: java.time.LocalDate): Boolean = runCatching {
        java.time.chrono.HijrahDate.from(tanggal).get(java.time.temporal.ChronoField.MONTH_OF_YEAR) == 9
    }.getOrDefault(false)

    /** Status baris slot [ref] pada [tanggal] ("dikerjakan" / "tidak" / "udzur"), null bila belum ada. */
    suspend fun statusSlot(ref: SlotRef?, tanggal: String): String? {
        ref ?: return null
        return researchLogDao.getBySlotKey(slotKeyOrNull(ref, tanggal) ?: return null)?.status
    }

    /** "Tertinggal" dibatalkan: hanya baris berstatus "tidak" yang dihapus. */
    suspend fun removeTidak(ref: SlotRef?, tanggal: String): Boolean {
        if (statusSlot(ref, tanggal) != ResearchSchema.Status.TIDAK) return false
        remove(ref, tanggal)
        return true
    }

    fun observeSemuaStatus(tanggal: String, jenis: String): Flow<List<ResearchLogEntity>> =
        researchLogDao.observeSemuaStatus(tanggal, jenis)

    /** Semua baris pada [tanggal], semua jenis; kartu puasa & sunnah di Beranda. */
    fun observeHari(tanggal: String): Flow<List<ResearchLogEntity>> = researchLogDao.observeHari(tanggal)

    /** slotKey lokal [ref] pada [tanggal] (null bila tidak sah), untuk mencocokkan status di UI. */
    fun slotKey(ref: SlotRef?, tanggal: String): String? = ref?.let { slotKeyOrNull(it, tanggal) }

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

    // ---------- persetujuan penelitian ----------

    /** true = setuju, false = menolak, null = belum memutuskan (tampilkan layar persetujuan). */
    fun researchConsent(): Boolean? = when (sharedPreferences.getString(KEY_RESEARCH_CONSENT, null)) {
        CONSENT_SETUJU -> true
        CONSENT_TOLAK -> false
        // Termasuk pemilik KEY_RESEARCH_NOTICE_ACK lama: belum pernah menyetujui di layar baru.
        else -> null
    }

    fun hasDecidedConsent(): Boolean = researchConsent() != null

    /**
     * Simpan keputusan pengguna. Setuju = catatan di HP (termasuk yang dibuat sebelumnya)
     * mulai diunggah; menolak = tidak ada yang diunggah, aplikasi tetap berfungsi penuh.
     *
     * Menolak / berhenti saat sedang masuk akun = `ditarik_pada` dicatat di users/{uid} (bila
     * dokumennya ada) lewat worker unggah. Setuju lagi = penarikan tertunda dibatalkan dan profil
     * dikirim ulang supaya `ditarik_pada` dihapus dari server.
     */
    fun setResearchConsent(setuju: Boolean) {
        val uid = currentUid()
        sharedPreferences.edit().apply {
            putString(KEY_RESEARCH_CONSENT, if (setuju) CONSENT_SETUJU else CONSENT_TOLAK)
            remove(KEY_RESEARCH_NOTICE_ACK)
            if (setuju) {
                remove(KEY_PENARIKAN_TERTUNDA)
                if (uid != null) remove(KEY_PROFIL_TERKIRIM_PREFIX + uid)
            } else if (uid != null) {
                putString(KEY_PENARIKAN_TERTUNDA, uid)
            }
        }.apply()
        if (setuju || uid != null) scheduleUpload()
    }

    /** UID yang penarikannya belum tercatat di server, atau null. */
    fun penarikanTertunda(): String? = sharedPreferences.getString(KEY_PENARIKAN_TERTUNDA, null)

    /**
     * Keputusan di layar persetujuan beserta kode responden opsional. Kode hanya disimpan bila
     * setuju dan formatnya sah; tidak setuju / kosong = kode dihapus.
     */
    fun setResearchConsent(setuju: Boolean, kodeResponden: String?) {
        val kode = if (setuju) ResearchSchema.Profil.normalisasiKodeResponden(kodeResponden) else null
        sharedPreferences.edit().apply {
            if (kode != null) putString(KEY_KODE_RESPONDEN, kode) else remove(KEY_KODE_RESPONDEN)
        }.apply()
        setResearchConsent(setuju)
    }

    fun kodeResponden(): String? = sharedPreferences.getString(KEY_KODE_RESPONDEN, null)

    /**
     * "Hapus data penelitianku": hapus semua dokumen `logs` dan profil `users/{uid}` milik akun ini di server
     * (termasuk manufaktur; token FCM di meta/notifikasi tidak tersentuh),
     * berhenti ikut penelitian, dan tandai catatan lokal belum terkirim (catatan ibadah di HP
     * tetap ada; bila kelak setuju lagi, catatan itu ikut terkirim sesuai keputusan pemilik proyek).
     */
    suspend fun deleteMyResearchData(): CloudSyncStatus {
        // Kode responden ikut dihapus dari HP.
        setResearchConsent(false, null)
        val uid = currentUid() ?: run {
            researchLogDao.markAllUnsynced()
            return CloudSyncStatus.success()
        }
        val db = firestore ?: return CloudSyncStatus.success()
        return runCatching {
            val docs = withTimeout(RESTORE_TIMEOUT_MS) {
                db.collection(ResearchSchema.COLLECTION_LOGS)
                    .whereEqualTo(ResearchSchema.Log.USER_ID, uid)
                    .get(com.google.firebase.firestore.Source.SERVER).await()
            }.documents
            docs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                withTimeout(WRITE_TIMEOUT_MS) { batch.commit().await() }
            }
            // Profil penelitian ikut dihapus (subkoleksi penanda/target/dzikir_habits tidak tersentuh).
            withTimeout(WRITE_TIMEOUT_MS) {
                db.collection(ResearchSchema.COLLECTION_USERS).document(uid).delete().await()
            }
            // Dokumennya sudah tidak ada: tidak ada lagi yang perlu ditandai ditarik.
            sharedPreferences.edit().remove(KEY_PROFIL_TERKIRIM_PREFIX + uid).apply()
            if (penarikanTertunda() == uid) sharedPreferences.edit().remove(KEY_PENARIKAN_TERTUNDA).apply()
            researchLogDao.markAllUnsynced()
            deletedSyncDao.deleteByType(RECORD_TYPE)
            CloudSyncStatus.success(docs.size)
        }.getOrElse { error -> failure(0, retry = error.isRetryable(), error) }
    }

    // ---------- dzikir / tilawah / iqro ----------

    fun observeDikerjakan(tanggal: String, jenis: String): Flow<List<ResearchLogEntity>> =
        researchLogDao.observeDikerjakan(tanggal, jenis)

    fun observeLatestIqro(): Flow<ResearchLogEntity?> = researchLogDao.observeLatestIqro()

    /**
     * Catat satu SESI dzikir (satu baris per sesi; keputusan 4.2). ID deterministik per sesi:
     * {tanggal}_dzikir_{slug}_{idHabit}_{sesiMs}. Pencatatan slug + habit yang sama dalam
     * DZIKIR_JENDELA_SESI_MS dianggap sesi yang sama: ID dan jam pencatatan awal dipakai ulang,
     * sehingga ketukan ganda / percobaan ulang tidak membuat baris palsu.
     * @return slotKey sesi, atau null bila tidak tercatat (data tidak sah / belum ada persetujuan).
     */
    suspend fun recordDzikirSession(
        slug: String,
        idHabit: String,
        tanggal: String,
        modePencatatan: String,
        waktuDzikir: String,
        targetHitungan: Int? = null,
        jumlah: Int? = null,
        durasiDetik: Int? = null,
        namaDetailAsli: String? = null,
        nowMs: Long = System.currentTimeMillis(),
        sumber: String = ResearchSchema.Sumber.BUKA_APLIKASI
    ): String? {
        if (slug !in ResearchSchema.DETAIL_PER_JENIS.getValue(ResearchSchema.Jenis.DZIKIR)) return null
        if (modePencatatan !in ResearchSchema.MODE_PENCATATAN || waktuDzikir !in ResearchSchema.WAKTU_DZIKIR) return null
        val prefix = prefixSesiDzikir(tanggal, slug, idHabit)
        val sesiSebelumnya = researchLogDao.findRecentByPrefix(prefix, nowMs - ResearchSchema.DZIKIR_JENDELA_SESI_MS)
        val sesiMs = sesiSebelumnya?.slotKey?.substringAfterLast('_')?.toLongOrNull() ?: nowMs
        val localId = runCatching { ResearchSchema.dzikirLocalId(idHabit, sesiMs) }.getOrNull() ?: return null
        val asli = if (slug == ResearchSchema.Detail.DZIKIR_LAINNYA) namaDetailAsli?.trim()?.takeIf { it.isNotEmpty() } else null
        if (slug == ResearchSchema.Detail.DZIKIR_LAINNYA && asli == null) return null
        val ref = SlotRef(IbadahSlot(ResearchSchema.Jenis.DZIKIR, slug, asli), localId)
        recordDikerjakan(
            ref, tanggal,
            jamPencatatanMs = sesiSebelumnya?.jamPencatatan ?: nowMs,
            sumber = sumber,
            jumlah = jumlah,
            modePencatatan = modePencatatan,
            waktuDzikir = waktuDzikir,
            targetHitungan = targetHitungan,
            durasiDetik = durasiDetik
        )
        return slotKeyOrNull(ref, tanggal)
    }

    /** Sesi dzikir [idHabit] pada [tanggal], urut jam pencatatan. */
    suspend fun getDzikirSessions(slug: String, idHabit: String, tanggal: String): List<ResearchLogEntity> =
        researchLogDao.getBySlotKeyPrefix(prefixSesiDzikir(tanggal, slug, idHabit)).sortedBy { it.jamPencatatan ?: 0L }

    /** Batalkan sesi TERAKHIR habit ini hari itu (keputusan C: baris dihapus, bukan "tidak"). */
    suspend fun removeLatestDzikirSession(slug: String, idHabit: String, tanggal: String): Boolean {
        val terakhir = getDzikirSessions(slug, idHabit, tanggal).lastOrNull() ?: return false
        removeBySlotKey(terakhir.slotKey)
        return true
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
                    selisihMenitDariAdzan = doc.getLong(ResearchSchema.Log.SELISIH_MENIT_DARI_ADZAN)?.toInt(),
                    modePencatatan = doc.getString(ResearchSchema.Log.MODE_PENCATATAN),
                    waktuDzikir = doc.getString(ResearchSchema.Log.WAKTU_DZIKIR),
                    targetHitungan = doc.getLong(ResearchSchema.Log.TARGET_HITUNGAN)?.toInt(),
                    durasiDetik = doc.getLong(ResearchSchema.Log.DURASI_DETIK)?.toInt()
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
        // Tanpa persetujuan penelitian tidak ada yang diunggah (hapusan tetap boleh dikirim).
        val bolehUnggah = researchConsent() == true
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

        if (!bolehUnggah && penarikanTertunda() == uid) {
            runCatching { catatPenarikan(uid) }.onFailure { error ->
                if (error.isRetryable()) return failure(changed, retry = true, error)
                firstIssue = firstIssue ?: failure(changed, retry = false, error)
            }
        }

        // Log hanya diunggah bila profil penelitian (users/{uid} dengan disetujui_pada) sudah ada di
        // server: tanpa itu responden tidak masuk dataset, dan aturan Firestore pun menolak lognya.
        // Profil belum bisa dibuat (mis. jenis kelamin belum diisi) = log menunggu di HP.
        var profilSiap = false
        if (bolehUnggah) {
            runCatching { profilSiap = syncProfilPenelitian(uid) }.onFailure { error ->
                if (error.isRetryable()) return failure(changed, retry = true, error)
                firstIssue = firstIssue ?: failure(changed, retry = false, error)
            }
        }

        val pendingUploads = if (bolehUnggah && profilSiap) researchLogDao.getPendingUploads(uid) else emptyList()
        for (row in pendingUploads) {
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
        // Keputusan penelitian milik orang yang login; akun berikutnya ditanya ulang.
        sharedPreferences.edit().remove(KEY_RESEARCH_CONSENT).remove(KEY_RESEARCH_NOTICE_ACK).apply()
        researchLogDao.deleteAll()
        deletedSyncDao.deleteByType(RECORD_TYPE)
        clearRestoreFlags()
        tilawahState.clearLocal()
        dzikirHabitDao.deleteAll()
    }

    private fun clearRestoreFlags() {
        val editor = sharedPreferences.edit()
        sharedPreferences.all.keys
            .filter { it.startsWith(KEY_RESTORED_PREFIX) || it.startsWith(KEY_PROFIL_TERKIRIM_PREFIX) }
            .forEach { editor.remove(it) }
        editor.apply()
    }

    /** Akun berganti tanpa lewat logout: buang baris milik akun lain. */
    /**
     * Akun anonim baru dibuat: catatan tamu sebelumnya (tanpa pemilik) menjadi miliknya. Kalau
     * kelak tamu itu masuk ke akun lain, catatan ini tidak ikut terkirim atas nama akun itu.
     */
    suspend fun klaimCatatanTamu(uid: String) = researchLogDao.klaimTanpaPemilik(uid)

    suspend fun clearRowsNotOwnedBy(uid: String) {
        researchLogDao.deleteNotOwnedBy(uid)
        deletedSyncDao.deleteByType(RECORD_TYPE)
        clearRestoreFlags()
        tilawahState.clearLocal()
        dzikirHabitDao.deleteNotOwnedBy(uid)
    }

    /**
     * Isi profil penelitian `users/{uid}` dari pilihan onboarding, atau null bila profil ibadah
     * belum diatur (jenis_kelamin wajib di skema). Rentang umur kode lama dikirim null.
     */
    internal fun isiProfilPenelitian(): Map<String, Any?>? {
        val jenisKelamin = when (UserPreferencesManager.gender.value) {
            GenderProfile.MALE -> ResearchSchema.Profil.LAKI_LAKI
            GenderProfile.FEMALE -> ResearchSchema.Profil.PEREMPUAN
            GenderProfile.UNSPECIFIED -> return null
        }
        return mapOf(
            ResearchSchema.Profil.JENIS_KELAMIN to jenisKelamin,
            ResearchSchema.Profil.RENTANG_UMUR to UserPreferencesManager.ageRange.value?.code
                ?.takeIf { it in ResearchSchema.Profil.RENTANG_UMUR_VALUES },
            ResearchSchema.Profil.STATUS_AKTIVITAS to UserPreferencesManager.statusAktivitas.value?.code,
            ResearchSchema.Profil.PILIHAN_AWAL to ResearchSchema.Profil.normalisasiPilihanAwal(UserPreferencesManager.pilihanAwal.value)
        )
    }

    /**
     * Kirim profil penelitian bila isinya berubah sejak pengiriman terakhir. Dokumen baru
     * mendapat tanggal_mulai = waktu server; dokumen lama hanya diperbarui (tanggal_mulai tetap).
     * Isian yang kosong di HP (mis. setelah instal ulang lalu login) lebih dulu dipulihkan dari
     * server, supaya jawaban lama tidak tertimpa null.
     * @return true bila profil penelitian ada di server (baru dikirim atau sudah terkirim sebelumnya).
     */
    private suspend fun syncProfilPenelitian(uid: String): Boolean {
        val kunci = KEY_PROFIL_TERKIRIM_PREFIX + uid
        val awal = isiProfilPenelitian() ?: return false
        // Merek HP untuk mengukur keandalan notifikasi per merek; ikut sidik supaya profil yang
        // sudah terkirim sebelum field ini ada dikirim ulang sekali.
        val manufaktur = manufakturPerangkat()
        val kode = kodeResponden()
        if (sharedPreferences.getString(kunci, null) == sidikProfil(awal, manufaktur, kode)) return true
        val db = firestore ?: return false
        val ref = db.collection(ResearchSchema.COLLECTION_USERS).document(uid)
        val sidik = withTimeout(WRITE_TIMEOUT_MS) {
            val snap = ref.get(Source.SERVER).await()
            if (snap.exists()) {
                pulihkanProfilKosong(
                    rentangUmur = snap.getString(ResearchSchema.Profil.RENTANG_UMUR),
                    statusAktivitas = snap.getString(ResearchSchema.Profil.STATUS_AKTIVITAS),
                    pilihanAwal = (snap.get(ResearchSchema.Profil.PILIHAN_AWAL) as? List<*>)?.filterIsInstance<String>()
                )
            }
            val isi = isiProfilPenelitian() ?: return@withTimeout null
            val tulis = buildMap<String, Any?> {
                // Kunci profil wajib ada walau bernilai null (rules: hasAll).
                putAll(isi)
                // manufaktur null tidak dikirim (rules: string 1-64 bila ada).
                if (manufaktur != null) put(ResearchSchema.Profil.MANUFAKTUR, manufaktur)
                // Kode responden opsional: kosong di HP = dihapus dari server bila pernah ada.
                when {
                    kode != null -> put(ResearchSchema.Profil.KODE_RESPONDEN, kode)
                    snap.contains(ResearchSchema.Profil.KODE_RESPONDEN) -> put(ResearchSchema.Profil.KODE_RESPONDEN, FieldValue.delete())
                }
                // tanggal_mulai & disetujui_pada hanya diisi sekali (waktu server). Profil lama yang
                // dibuat sebelum disetujui_pada ada mendapatkannya di pengiriman berikutnya.
                if (!snap.contains(ResearchSchema.Profil.TANGGAL_MULAI)) put(ResearchSchema.Profil.TANGGAL_MULAI, FieldValue.serverTimestamp())
                if (!snap.contains(ResearchSchema.Profil.DISETUJUI_PADA)) put(ResearchSchema.Profil.DISETUJUI_PADA, FieldValue.serverTimestamp())
                // Profil hanya dikirim saat setuju: tanda penarikan sebelumnya dibatalkan. Selalu
                // dikirim (bukan hanya bila snap memuatnya): penarikan yang masih antre offline
                // sampai lebih dulu di server, lalu dihapus oleh tulisan ini (urutan tulisan dijaga SDK).
                put(ResearchSchema.Profil.DITARIK_PADA, FieldValue.delete())
            }
            ref.set(tulis, SetOptions.merge()).await()
            sidikProfil(isi, manufaktur, kode)
        } ?: return false
        sharedPreferences.edit().putString(kunci, sidik).apply()
        return true
    }

    /**
     * Tulis `ditarik_pada` (waktu server) di users/{uid}. Dokumen belum ada (tidak pernah ikut,
     * atau sudah dihapus) = tidak ada data penelitian yang perlu ditandai: selesai.
     */
    private suspend fun catatPenarikan(uid: String) {
        val db = firestore ?: return
        val ref = db.collection(ResearchSchema.COLLECTION_USERS).document(uid)
        try {
            withTimeout(WRITE_TIMEOUT_MS) {
                ref.update(ResearchSchema.Profil.DITARIK_PADA, FieldValue.serverTimestamp()).await()
            }
        } catch (e: FirebaseFirestoreException) {
            // Dokumen yang tidak ada bisa dilaporkan NOT_FOUND atau PERMISSION_DENIED (rules dinilai
            // sebagai create). Pastikan dari server bahwa memang tidak ada; selain itu = gagal sungguhan.
            if (e.code != FirebaseFirestoreException.Code.NOT_FOUND &&
                e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            val ada = withTimeout(WRITE_TIMEOUT_MS) { ref.get(Source.SERVER).await().exists() }
            if (ada) throw e
        }
        // Hanya dibersihkan bila belum berubah (mis. pengguna sudah setuju lagi sementara itu).
        if (penarikanTertunda() == uid) sharedPreferences.edit().remove(KEY_PENARIKAN_TERTUNDA).apply()
    }

    private fun sidikProfil(isi: Map<String, Any?>, manufaktur: String?, kode: String?): String =
        (isi.values + manufaktur + kode).joinToString("|")

    /** Build.MANUFACTURER apa adanya (dipangkas, maks 64 huruf), null bila kosong. */
    private fun manufakturPerangkat(): String? =
        android.os.Build.MANUFACTURER?.trim()?.take(64)?.takeIf { it.isNotEmpty() }

    /**
     * Saat login: pulihkan usia/kesibukan/pilihan awal dari profil penelitian di server, supaya
     * pengguna yang pernah mengisi tidak ditanya ulang. Gagal (offline) = diam saja; worker
     * unggah tetap memulihkan sebelum menulis.
     */
    suspend fun pulihkanProfilDariServer() {
        val uid = currentUid() ?: return
        val db = firestore ?: return
        runCatching {
            val snap = withTimeout(WRITE_TIMEOUT_MS) {
                db.collection(ResearchSchema.COLLECTION_USERS).document(uid).get(Source.SERVER).await()
            }
            if (snap.exists()) {
                pulihkanProfilKosong(
                    rentangUmur = snap.getString(ResearchSchema.Profil.RENTANG_UMUR),
                    statusAktivitas = snap.getString(ResearchSchema.Profil.STATUS_AKTIVITAS),
                    pilihanAwal = (snap.get(ResearchSchema.Profil.PILIHAN_AWAL) as? List<*>)?.filterIsInstance<String>()
                )
            }
        }
    }

    /** Isi pilihan lokal yang masih kosong dengan nilai dari server; nilai lokal yang ada menang. */
    internal fun pulihkanProfilKosong(rentangUmur: String?, statusAktivitas: String?, pilihanAwal: List<String>?) {
        if (UserPreferencesManager.ageRange.value == null) {
            com.islami.Aha.util.AgeRange.fromCode(rentangUmur)?.let {
                UserPreferencesManager.setAgeRange(it, UserPreferencesManager.guardianOk.value)
            }
        }
        if (UserPreferencesManager.statusAktivitas.value == null) {
            com.islami.Aha.util.StatusAktivitas.fromCode(statusAktivitas)?.let { UserPreferencesManager.setStatusAktivitas(it) }
        }
        if (UserPreferencesManager.pilihanAwal.value == null && pilihanAwal != null) {
            UserPreferencesManager.setPilihanAwal(pilihanAwal.toSet())
        }
    }

    private fun slotKeyOrNull(ref: SlotRef, tanggal: String): String? =
        // Pengaman: "jumat" di tanggal selain Jumat tidak pernah ditulis (server pasti menolaknya).
        if (ref.slot.namaDetail == ResearchSchema.Detail.JUMAT && !SholatJumat.hariJumat(tanggal)) null
        else runCatching {
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
        ResearchSchema.Log.SELISIH_MENIT_DARI_ADZAN to selisihMenitDariAdzan?.toLong(),
        ResearchSchema.Log.MODE_PENCATATAN to modePencatatan,
        ResearchSchema.Log.WAKTU_DZIKIR to waktuDzikir,
        ResearchSchema.Log.TARGET_HITUNGAN to targetHitungan?.toLong(),
        ResearchSchema.Log.DURASI_DETIK to durasiDetik?.toLong()
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
