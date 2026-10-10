package com.islami.Aha.data.research

import android.content.Context
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
import com.islami.Aha.data.local.ResearchLogDao
import com.islami.Aha.data.model.DeletedSyncRecord
import com.islami.Aha.data.model.ResearchLogEntity
import com.islami.Aha.data.repository.CloudSyncStatus
import com.islami.Aha.worker.SyncResearchLogsWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
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
    private val deletedSyncDao: DeletedSyncDao
) {
    companion object {
        const val RECORD_TYPE = "RESEARCH_LOG"
        private const val UNIQUE_WORK_NAME = "sync_research_logs"
        private const val WRITE_TIMEOUT_MS = 15_000L
        private const val FLUSH_TIMEOUT_MS = 5_000L
        private const val UPLOAD_FAILED_NOTICE = "Sinkronisasi log ibadah ke cloud belum berhasil."

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
        sumber: String = ResearchSchema.Sumber.TIDAK_DIKETAHUI
    ) {
        ref ?: return
        val key = slotKeyOrNull(ref, tanggal) ?: return
        researchLogDao.upsert(
            ResearchLogEntity(
                slotKey = key,
                tanggal = tanggal,
                jenisIbadah = ref.slot.jenisIbadah,
                namaDetail = ref.slot.namaDetail,
                namaDetailAsli = ref.slot.namaDetailAsli?.take(100),
                status = ResearchSchema.Status.DIKERJAKAN,
                jumlah = null,
                jilid = null,
                jamPencatatan = jamPencatatanMs,
                sumberPencatatan = sumber,
                ownerUid = currentUid(),
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
        )
        // Dicentang ulang sebelum hapusan sempat terkirim: cukup timpa dokumennya.
        deletedSyncDao.deleteByRecordIdAndType(key, RECORD_TYPE)
    }

    /** Centang dibatalkan (keputusan C): baris dihapus, bukan diubah jadi "tidak". */
    suspend fun remove(ref: SlotRef?, tanggal: String) {
        ref ?: return
        val key = slotKeyOrNull(ref, tanggal) ?: return
        researchLogDao.getBySlotKey(key) ?: return
        researchLogDao.deleteBySlotKey(key)
        if (currentUid() != null) {
            deletedSyncDao.insert(DeletedSyncRecord(recordId = key, recordType = RECORD_TYPE))
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
    }

    /** Akun berganti tanpa lewat logout: buang baris milik akun lain. */
    suspend fun clearRowsNotOwnedBy(uid: String) {
        researchLogDao.deleteNotOwnedBy(uid)
        deletedSyncDao.deleteByType(RECORD_TYPE)
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
        ResearchSchema.Log.SUMBER_PENCATATAN to sumberPencatatan
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
