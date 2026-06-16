package com.islami.Aha.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.islami.Aha.AhaApplication
import dagger.hilt.android.EntryPointAccessors

/**
 * Worker yang meng-upload semua [HabitCompletionRecord] dengan isSynced=false ke Firestore.
 * Dijadwalkan dengan constraint [NetworkType.CONNECTED] sehingga otomatis berjalan
 * begitu koneksi internet tersedia, bahkan setelah app restart.
 */
class SyncCompletionsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                AhaApplication.AppEntryPoint::class.java
            )
            val syncStatus = entryPoint.completionSyncRepository().syncPendingRecords()
            when {
                syncStatus.shouldRetry && runAttemptCount < 3 -> Result.retry()
                syncStatus.hasIssue -> Result.failure()
                else -> Result.success()
            }
        } catch (e: Exception) {
            // Retry on transient errors (network, Firestore unavailable, etc.)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
