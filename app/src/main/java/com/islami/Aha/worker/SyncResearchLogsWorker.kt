package com.islami.Aha.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.islami.Aha.AhaApplication
import dagger.hilt.android.EntryPointAccessors

/**
 * Mengunggah antrean `research_logs` ke koleksi Firestore `logs`.
 * Terpisah dari [SyncCompletionsWorker] agar jalur sinkronisasi lama tidak berubah.
 */
class SyncResearchLogsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                AhaApplication.AppEntryPoint::class.java
            )
            val syncStatus = entryPoint.researchLogRepository().syncPending()
            when {
                syncStatus.shouldRetry && runAttemptCount < 5 -> Result.retry()
                syncStatus.hasIssue -> Result.failure()
                else -> Result.success()
            }
        } catch (e: Exception) {
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }
}
