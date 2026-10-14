package com.islami.Aha.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.islami.Aha.data.local.AppDatabase
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.model.HabitCompletionRecord
import com.islami.Aha.data.research.ResearchLogRepository
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.ui.widget.AhaWidgetUpdater
import com.islami.Aha.util.DateUtils
import com.islami.Aha.worker.SyncCompletionsWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Satu-satunya jalur menandai habit bawaan (tabel `habits`) selesai/batal untuk hari ini.
 * Dipakai Beranda dan tombol di notifikasi, supaya keduanya menulis data yang sama:
 * flag habit, habit_completion_records, research_logs, lalu sinkron ke Firestore.
 */
@Singleton
class DefaultHabitCompletionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val habitDao: HabitDao,
    private val habitCompletionDao: HabitCompletionDao,
    private val researchLogRepository: ResearchLogRepository,
    private val completionSyncRepository: CompletionSyncRepository,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        const val KEY_LAST_DAILY_RESET = "last_daily_reset"
        fun habitKey(habitId: Int) = "default_$habitId"
    }

    data class Hasil(val habit: Habit, val berubah: Boolean, val syncStatus: CloudSyncStatus)

    /**
     * Flag `isCompleted` hanya berlaku untuk hari ini. Dipanggil sebelum menulis supaya
     * centang dari notifikasi pagi hari tidak terhapus saat aplikasi dibuka belakangan.
     */
    suspend fun ensureDailyReset() {
        val todayKey = DateUtils.getTodayKey()
        if (sharedPreferences.getString(KEY_LAST_DAILY_RESET, null) == todayKey) return
        habitDao.resetAllCompletions()
        sharedPreferences.edit().putString(KEY_LAST_DAILY_RESET, todayKey).apply()
    }

    suspend fun isCompletedToday(habitId: Int): Boolean =
        habitCompletionDao.getRecordsForDate(DateUtils.getTodayKey()).any { it.habitKey == habitKey(habitId) }

    /** Berapa sholat fardhu yang sudah dicatat hari ini, dan dari berapa. */
    suspend fun fardhuProgressToday(): Pair<Int, Int> {
        val fardhu = habitDao.getFardhuHabits()
        val done = habitCompletionDao.getRecordsForDate(DateUtils.getTodayKey()).map { it.habitKey }.toSet()
        return fardhu.count { habitKey(it.id) in done } to fardhu.size
    }

    /**
     * Tandai habit [habitId] selesai ([completed] = true) atau batal untuk hari ini.
     * Mengembalikan null bila habit tidak ada. `berubah` = false bila statusnya sudah sama.
     */
    suspend fun setCompleted(
        habitId: Int,
        completed: Boolean,
        sumber: String = ResearchSchema.Sumber.TIDAK_DIKETAHUI
    ): Hasil? {
        ensureDailyReset()
        val habit = habitDao.getHabitById(habitId) ?: return null
        val todayKey = DateUtils.getTodayKey()
        val key = habitKey(habitId)
        if (isCompletedToday(habitId) == completed) {
            // Catatan hari ini sudah sesuai; cukup samakan flag tampilan bila tertinggal.
            if (habit.isCompleted != completed) habitDao.updateHabit(habit.copy(isCompleted = completed))
            return Hasil(habit.copy(isCompleted = completed), berubah = false, syncStatus = CloudSyncStatus.success())
        }
        val record = HabitCompletionRecord(
            habitKey = key,
            dateKey = todayKey,
            category = habit.category,
            source = "DEFAULT"
        )
        val researchSlot = ResearchLogRepository.slotForDefaultHabit(habit.name, habit.category)
        appDatabase.withTransaction {
            habitDao.updateHabit(habit.copy(isCompleted = completed))
            if (completed) {
                habitCompletionDao.insert(record)
                researchLogRepository.recordDikerjakan(researchSlot, todayKey, sumber = sumber)
            } else {
                habitCompletionDao.deleteByHabitAndDate(key, todayKey)
                researchLogRepository.remove(researchSlot, todayKey)
            }
        }
        researchLogRepository.scheduleUpload()
        AhaWidgetUpdater.requestUpdate(context)
        // Sync ke Firestore (gagal → tetap tersimpan lokal, worker mengulang saat online)
        val syncStatus = if (completed) {
            completionSyncRepository.syncAdd(record)
        } else {
            completionSyncRepository.syncDelete(key, todayKey)
        }
        scheduleSyncCompletionsWork()
        return Hasil(habit.copy(isCompleted = completed), berubah = true, syncStatus = syncStatus)
    }

    private fun scheduleSyncCompletionsWork() {
        val request = OneTimeWorkRequestBuilder<SyncCompletionsWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_habit_completions",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
