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
import com.islami.Aha.data.research.IbadahSlot
import com.islami.Aha.data.research.PilihanJumat
import com.islami.Aha.data.research.ResearchLogRepository
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.data.research.SholatJumat
import com.islami.Aha.data.research.SlotRef
import com.islami.Aha.util.UserPreferencesManager
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

    private val pilihanJumat = PilihanJumat(sharedPreferences)

    private fun slotTengahHari(detail: String) =
        SlotRef(IbadahSlot(ResearchSchema.Jenis.SHOLAT_WAJIB, detail, SholatJumat.namaHabit(detail)), null)

    /**
     * nama_detail slot tengah hari pada [dateKey]: Jumat untuk laki-laki di hari Jumat (kecuali
     * diganti Dzuhur), selain itu Dzuhur. Lihat [SholatJumat].
     */
    suspend fun detailTengahHari(dateKey: String): String {
        val lakiLaki = UserPreferencesManager.isJumatEnabled.value
        if (!lakiLaki || !SholatJumat.hariJumat(dateKey)) return ResearchSchema.Detail.DZUHUR
        val ganti = pilihanJumat.gantiDzuhur(dateKey) ?: SholatJumat.simpulkanGanti(
            adaJumat = researchLogRepository.statusSlot(slotTengahHari(ResearchSchema.Detail.JUMAT), dateKey) != null,
            adaDzuhur = researchLogRepository.statusSlot(slotTengahHari(ResearchSchema.Detail.DZUHUR), dateKey) != null
        )
        return SholatJumat.detailTengahHari(dateKey, lakiLaki, ganti)
    }

    /** Slot penelitian habit bawaan pada [dateKey]; habit Dzuhur mengikuti slot tengah hari. */
    suspend fun slotUntuk(habit: Habit, dateKey: String): SlotRef? {
        val dasar = ResearchLogRepository.slotForDefaultHabit(habit.name, habit.category) ?: return null
        if (dasar.slot.jenisIbadah != ResearchSchema.Jenis.SHOLAT_WAJIB ||
            SholatJumat.lawan(dasar.slot.namaDetail) == null
        ) return dasar
        return slotTengahHari(detailTengahHari(dateKey))
    }

    /** Baris slot lawan (Jumat <-> Dzuhur) yang harus hilang saat [slot] ditulis; null bila bukan tengah hari. */
    private fun slotLawan(slot: SlotRef?): SlotRef? =
        SholatJumat.lawan(slot?.slot?.namaDetail)
            ?.takeIf { slot?.slot?.jenisIbadah == ResearchSchema.Jenis.SHOLAT_WAJIB }
            ?.let { slotTengahHari(it) }

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
        val researchSlot = slotUntuk(habit, dateKey)
        val lawan = slotLawan(researchSlot)
        appDatabase.withTransaction {
            if (hariIni) habitDao.updateHabit(habit.copy(isCompleted = completed))
            if (completed) {
                habitCompletionDao.insert(record)
                researchLogRepository.recordDikerjakan(researchSlot, dateKey, sumber = sumber)
            } else {
                habitCompletionDao.deleteByHabitAndDate(key, dateKey)
                researchLogRepository.remove(researchSlot, dateKey)
            }
            // Jumat dan Dzuhur saling meniadakan: slot tengah hari hanya punya satu baris.
            researchLogRepository.remove(lawan, dateKey)
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
     * Tombol "Tertinggal" (pilihan B), di notifikasi ([sumber] = dari_notifikasi) atau di kartu
     * Beranda (buka_aplikasi): catat status "tidak" untuk sholat wajib [habitId] pada [dateKey].
     * Hanya data penelitian; centang Beranda / habit_completion_records tidak disentuh, dan
     * centang belakangan menimpa baris ini menjadi "dikerjakan".
     * false bila habit tidak ada, bukan sholat wajib, tanggal di masa depan, atau sudah dicatat.
     */
    suspend fun markTertinggal(
        habitId: Int,
        dateKey: String = DateUtils.getTodayKey(),
        sumber: String = ResearchSchema.Sumber.DARI_NOTIFIKASI
    ): Boolean {
        val habit = habitDao.getHabitById(habitId) ?: return false
        if (dateKey > DateUtils.getTodayKey() || isCompletedOn(habitId, dateKey)) return false
        val slot = slotUntuk(habit, dateKey)
        val ditulis = appDatabase.withTransaction {
            researchLogRepository.recordTidakDariNotifikasi(slot, dateKey, sumber = sumber).also { ok ->
                if (ok) researchLogRepository.remove(slotLawan(slot), dateKey)
            }
        }
        if (!ditulis) return false
        researchLogRepository.scheduleUpload()
        AhaWidgetUpdater.requestUpdate(context)
        return true
    }

    /** "Tertinggal" dibatalkan dari Beranda: baris "tidak" dihapus (baris "dikerjakan" tidak disentuh). */
    suspend fun batalkanTertinggal(habitId: Int, dateKey: String = DateUtils.getTodayKey()): Boolean {
        val habit = habitDao.getHabitById(habitId) ?: return false
        if (!researchLogRepository.removeTidak(slotUntuk(habit, dateKey), dateKey)) return false
        researchLogRepository.scheduleUpload()
        AhaWidgetUpdater.requestUpdate(context)
        return true
    }

    /**
     * Laki-laki di hari Jumat: ganti slot tengah hari ke Dzuhur ([keDzuhur] = true) atau kembali ke
     * Jumat. Catatan slot itu pada [dateKey] (centang maupun Tertinggal) ikut dihapus karena
     * ibadahnya berbeda; UI meminta konfirmasi bila ada catatan. false bila tidak berlaku.
     */
    suspend fun gantiTengahHari(habitId: Int, dateKey: String, keDzuhur: Boolean): Boolean {
        val habit = habitDao.getHabitById(habitId) ?: return false
        val dasar = ResearchLogRepository.slotForDefaultHabit(habit.name, habit.category) ?: return false
        if (SholatJumat.lawan(dasar.slot.namaDetail) == null) return false
        if (!UserPreferencesManager.isJumatEnabled.value || !SholatJumat.hariJumat(dateKey)) return false
        val key = habitKey(habitId)
        val adaCentang = isCompletedOn(habitId, dateKey)
        val hariIni = dateKey == DateUtils.getTodayKey()
        appDatabase.withTransaction {
            pilihanJumat.simpan(dateKey, keDzuhur)
            if (adaCentang) {
                habitCompletionDao.deleteByHabitAndDate(key, dateKey)
                if (hariIni) habitDao.updateHabit(habit.copy(isCompleted = false))
            }
            researchLogRepository.remove(slotTengahHari(ResearchSchema.Detail.JUMAT), dateKey)
            researchLogRepository.remove(slotTengahHari(ResearchSchema.Detail.DZUHUR), dateKey)
        }
        researchLogRepository.scheduleUpload()
        AhaWidgetUpdater.requestUpdate(context)
        if (adaCentang) {
            completionSyncRepository.syncDelete(key, dateKey)
            scheduleSyncCompletionsWork()
        }
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
