package com.islami.Aha.data.research

import com.islami.Aha.data.local.DzikirHabitDao
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.ResearchLogDao
import com.islami.Aha.data.model.DailyCompletionCount
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sumber angka heatmap, streak, total & rata-rata: gabungan dua tabel.
 * - habit_completion_records: sholat & puasa (tidak diubah sama sekali).
 * - research_logs: HANYA dzikir, tilawah, iqro berstatus dikerjakan (filter ada di DAO),
 *   sehingga sholat/puasa yang ditulis ke kedua tabel tidak terhitung ganda.
 * Untuk pengguna tanpa dzikir/tilawah, semua angka identik dengan versi sebelumnya.
 */
@Singleton
class ActivityStatsRepository @Inject constructor(
    private val habitCompletionDao: HabitCompletionDao,
    private val researchLogDao: ResearchLogDao,
    private val dzikirHabitDao: DzikirHabitDao
) {
    suspend fun getDailyCountsBetween(startDateKey: String, endDateKey: String): List<DailyCompletionCount> =
        mergeCounts(
            habitCompletionDao.getDailyCountsBetween(startDateKey, endDateKey),
            researchLogDao.getActivityDailyCountsBetween(startDateKey, endDateKey)
        )

    suspend fun getDistinctCompletionDatesDesc(): List<String> =
        (habitCompletionDao.getDistinctCompletionDatesDesc() + researchLogDao.getActivityDistinctDatesDesc())
            .distinct()
            .sortedDescending()

    suspend fun getTotalCompletionCount(): Int =
        habitCompletionDao.getTotalCompletionCount() + researchLogDao.getActivityTotalCount()

    /** Berubah bila research_logs ATAU daftar habit dzikir berubah (rasio dzikir ikut habit). */
    fun observeChanges(): kotlinx.coroutines.flow.Flow<String> =
        kotlinx.coroutines.flow.combine(researchLogDao.observeChangeTick(), dzikirHabitDao.observeAll()) { tick, habits ->
            "$tick-${habits.joinToString(",") { it.id }}"
        }

    /**
     * Rasio dzikir "Progress Hari Ini": (habit dengan minimal satu sesi pada [tanggal]) /
     * (semua habit dzikir pilihan pengguna, termasuk yang ber-waktu "bebas").
     * Mode Cuti mengikuti ModeCutiPolicy (bawaan: dzikir tetap aktif).
     */
    suspend fun getDzikirProgress(tanggal: String, modeCutiAktif: Boolean): DzikirProgress {
        if (!ModeCutiPolicy.aktif(ResearchSchema.Jenis.DZIKIR, modeCutiAktif)) return DzikirProgress(0, 0)
        val habits = dzikirHabitDao.getAll()
        val prefixes = researchLogDao.getDzikirHabitPrefixesDikerjakan(tanggal).toSet()
        val done = habits.count { "${tanggal}_${ResearchSchema.Jenis.DZIKIR}_${it.slug}_${it.id}" in prefixes }
        return DzikirProgress(done = done, total = habits.size)
    }

    data class DzikirProgress(val done: Int, val total: Int)

    companion object {

        internal fun mergeCounts(
            a: List<DailyCompletionCount>,
            b: List<DailyCompletionCount>
        ): List<DailyCompletionCount> =
            (a + b).groupBy { it.dateKey }
                .map { (date, rows) -> DailyCompletionCount(date, rows.sumOf { it.count }) }
                .sortedBy { it.dateKey }
    }
}
