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
 * Satu-satunya jalur menandai habit bawaan (tabel `habits`) selesai/batal, untuk hari ini
 * atau (dari notifikasi yang tertinggal di laci) hari ibadah sebelumnya.
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

    suspend fun isCompletedToday(habitId: Int): Boolean = isCompletedOn(habitId, DateUtils.getTodayKey())

    suspend fun isCompletedOn(habitId: Int, dateKey: String): Boolean =
        habitCompletionDao.getRecordsForDate(dateKey).any { it.habitKey == habitKey(habitId) }

    /** Berapa sholat fardhu yang sudah dicatat pada [dateKey], dan dari berapa. */
    suspend fun fardhuProgressOn(dateKey: String): Pair<Int, Int> {
        val fardhu = habitDao.getFardhuHabits()
        val done = habitCompletionDao.getRecordsForDate(dateKey).map { it.habitKey }.toSet()
        return fardhu.count { habitKey(it.id) in done } to fardhu.size
    }

    /**
     * Tandai habit [habitId] selesai ([completed] = true) atau batal pada [dateKey] (default hari ini).
     * Hari lampau = pencatatan susulan (keputusan F): tanggal tetap hari ibadahnya, jam = sekarang,
     * dan flag tampilan `isCompleted` (khusus hari ini) tidak disentuh.
     * Mengembalikan null bila habit tidak ada atau [dateKey] di masa depan.
     * `berubah` = false bila statusnya sudah sama.
     */
    suspend fun setCompleted(
        habitId: Int,
        completed: Boolean,
        sumber: String = ResearchSchema.Sumber.BUKA_APLIKASI,
        dateKey: String = DateUtils.getTodayKey()
    ): Hasil? {
        ensureDailyReset()
        val habit = habitDao.getHabitById(habitId) ?: return null
        val todayKey = DateUtils.getTodayKey()
        if (dateKey > todayKey) return null
        val hariIni = dateKey == todayKey
        val key = habitKey(habitId)
        if (isCompletedOn(habitId, dateKey) == completed) {
            // Catatan sudah sesuai; cukup samakan flag tampilan hari ini bila tertinggal.
            if (hariIni && habit.isCompleted != completed) habitDao.updateHabit(habit.copy(isCompleted = completed))
            return Hasil(habit.copy(isCompleted = if (hariIni) completed else habit.isCompleted), berubah = false, syncStatus = CloudSyncStatus.success())
        }
        val record = HabitCompletionRecord(
            habitKey = key,
            dateKey = dateKey,
            category = habit.category,
            source = "DEFAULT"
        )
        val researchSlot = ResearchLogRepository.slotForDefaultHabit(habit.name, habit.category)
        appDatabase.withTransaction {
            if (hariIni) habitDao.updateHabit(habit.copy(isCompleted = completed))
            if (completed) {
                habitCompletionDao.insert(record)
                researchLogRepository.recordDikerjakan(researchSlot, dateKey, sumber = sumber)
            } else {
                habitCompletionDao.deleteByHabitAndDate(key, dateKey)
                researchLogRepository.remove(researchSlot, dateKey)
            }
        }
        researchLogRepository.scheduleUpload()
        AhaWidgetUpdater.requestUpdate(context)
        // Sync ke Firestore (gagal → tetap tersimpan lokal, worker mengulang saat online)
        val syncStatus = if (completed) {
            completionSyncRepository.syncAdd(record)
        } else {
            completionSyncRepository.syncDelete(key, dateKey)
        }
        scheduleSyncCompletionsWork()
        return Hasil(habit.copy(isCompleted = if (hariIni) completed else habit.isCompleted), berubah = true, syncStatus = syncStatus)
    }

    /**
     * Tombol "Tertinggal" (pilihan B): catat status "tidak" untuk sholat wajib [habitId] pada
     * [dateKey]. Hanya data penelitian; centang Beranda / habit_completion_records tidak disentuh,
     * dan centang belakangan menimpa baris ini menjadi "dikerjakan".
     * false bila habit tidak ada, bukan sholat wajib, tanggal di masa depan, atau sudah dicatat.
     */
    suspend fun markTertinggal(habitId: Int, dateKey: String = DateUtils.getTodayKey()): Boolean {
        val habit = habitDao.getHabitById(habitId) ?: return false
        if (dateKey > DateUtils.getTodayKey() || isCompletedOn(habitId, dateKey)) return false
        val slot = ResearchLogRepository.slotForDefaultHabit(habit.name, habit.category)
        if (!researchLogRepository.recordTidakDariNotifikasi(slot, dateKey)) return false
        researchLogRepository.scheduleUpload()
        return true
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
