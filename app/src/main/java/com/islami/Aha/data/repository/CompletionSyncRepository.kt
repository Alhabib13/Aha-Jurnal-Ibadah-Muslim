package com.islami.Aha.data.repository

import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.model.HabitCompletionRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository yang menangani sinkronisasi [HabitCompletionRecord] antara
 * Room (lokal) dan Firestore (cloud) di path:
 *   users/{uid}/habit_completions/{dateKey}_{habitKey}
 */
@Singleton
class CompletionSyncRepository @Inject constructor(
    private val habitCompletionDao: HabitCompletionDao
) {
    companion object {
        const val OFFLINE_SYNC_NOTICE =
            "Mode offline aktif. Data cloud akan disinkronkan saat koneksi stabil."
        private const val RESTORE_FAILED_NOTICE =
            "Riwayat ibadah cloud belum bisa dipulihkan saat ini."
        private const val UPLOAD_FAILED_NOTICE =
            "Sinkronisasi riwayat ibadah ke cloud belum berhasil."
    }

    private val auth: FirebaseAuth? by lazy {
        runCatching { FirebaseAuth.getInstance() }.getOrNull()
    }
    private val firestore: FirebaseFirestore? by lazy {
        runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }

    private fun collection(uid: String) =
        firestore?.collection("users")?.document(uid)?.collection("habit_completions")

    private fun Throwable.toCloudSyncFailure(defaultMessage: String): CloudSyncStatus {
        val retryable = isLikelyNetworkIssue()
        return CloudSyncStatus.failure(
            message = if (retryable) OFFLINE_SYNC_NOTICE else defaultMessage,
            shouldRetry = retryable
        )
    }

    private fun Throwable.isLikelyNetworkIssue(): Boolean {
        if (this is IOException) return true
        val messageText = message?.lowercase().orEmpty()
        return "network" in messageText ||
            "timeout" in messageText ||
            "timed out" in messageText ||
            "unable to resolve host" in messageText ||
            "failed to connect" in messageText ||
            "unavailable" in messageText
    }

    /** Simpan satu completion record ke Firestore lalu tandai synced di Room. */
    suspend fun syncAdd(record: HabitCompletionRecord): CloudSyncStatus {
        val uid = auth?.currentUser?.uid ?: return CloudSyncStatus.success()
        val col = collection(uid) ?: return CloudSyncStatus.success()
        val docId = "${record.dateKey}_${record.habitKey}"
        return runCatching {
            col.document(docId).set(
                mapOf(
                    "habitKey" to record.habitKey,
                    "dateKey" to record.dateKey,
                    "category" to record.category,
                    "source" to record.source,
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
            habitCompletionDao.markSyncedByKey(record.habitKey, record.dateKey)
            CloudSyncStatus.success(changedCount = 1)
        }.getOrElse { error ->
            runCatching { FirebaseCrashlytics.getInstance().recordException(error) }
            error.toCloudSyncFailure(UPLOAD_FAILED_NOTICE)
        }
    }

    /** Hapus satu completion record dari Firestore (saat user uncheck habit). */
    suspend fun syncDelete(habitKey: String, dateKey: String): CloudSyncStatus {
        val uid = auth?.currentUser?.uid ?: return CloudSyncStatus.success()
        val col = collection(uid) ?: return CloudSyncStatus.success()
        val docId = "${dateKey}_${habitKey}"
        return runCatching {
            col.document(docId).delete().await()
            CloudSyncStatus.success(changedCount = 1)
        }.getOrElse { error ->
            runCatching { FirebaseCrashlytics.getInstance().recordException(error) }
            error.toCloudSyncFailure(UPLOAD_FAILED_NOTICE)
        }
    }

    /**
     * Ambil semua completion dari Firestore dan insert ke Room (jika belum ada).
     * Dipanggil saat login atau reinstall untuk memulihkan history ibadah.
     */
    suspend fun restoreFromCloud(): CloudSyncStatus {
        val uid = auth?.currentUser?.uid ?: return CloudSyncStatus.success()
        val col = collection(uid) ?: return CloudSyncStatus.success()
        return runCatching {
            val snapshot = col.get().await()
            var restoredCount = 0
            snapshot.documents.forEach { doc ->
                val habitKey = doc.getString("habitKey") ?: return@forEach
                val dateKey = doc.getString("dateKey") ?: return@forEach
                val category = doc.getString("category").orEmpty()
                val source = doc.getString("source").orEmpty()
                // OnConflictStrategy.IGNORE: jika sudah ada lokal, skip.
                habitCompletionDao.insert(
                    HabitCompletionRecord(
                        habitKey = habitKey,
                        dateKey = dateKey,
                        category = category,
                        source = source,
                        isSynced = true
                    )
                )
                restoredCount += 1
            }
            CloudSyncStatus.success(changedCount = restoredCount)
        }.getOrElse { error ->
            runCatching { FirebaseCrashlytics.getInstance().recordException(error) }
            error.toCloudSyncFailure(RESTORE_FAILED_NOTICE)
        }
    }

    /**
     * Upload semua record yang belum ter-sync (isSynced=false) ke Firestore.
     * Dipanggil saat koneksi kembali setelah offline.
     */
    suspend fun syncPendingRecords(): CloudSyncStatus {
        val uid = auth?.currentUser?.uid ?: return CloudSyncStatus.success()
        val col = collection(uid) ?: return CloudSyncStatus.success()
        val pending = habitCompletionDao.getUnsyncedRecords()
        if (pending.isEmpty()) return CloudSyncStatus.success()
        var syncedCount = 0
        pending.forEach { record ->
            val docId = "${record.dateKey}_${record.habitKey}"
            val result = runCatching {
                col.document(docId).set(
                    mapOf(
                        "habitKey" to record.habitKey,
                        "dateKey" to record.dateKey,
                        "category" to record.category,
                        "source" to record.source,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
                habitCompletionDao.markSyncedByKey(record.habitKey, record.dateKey)
                CloudSyncStatus.success(changedCount = 1)
            }.getOrElse { error ->
                runCatching { FirebaseCrashlytics.getInstance().recordException(error) }
                error.toCloudSyncFailure(UPLOAD_FAILED_NOTICE)
            }
            if (result.hasIssue) {
                return result.copy(changedCount = syncedCount)
            }
            syncedCount += 1
        }
        return CloudSyncStatus.success(changedCount = syncedCount)
    }
}
