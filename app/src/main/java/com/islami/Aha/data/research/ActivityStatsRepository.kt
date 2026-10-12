package com.islami.Aha.data.research

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
    private val researchLogDao: ResearchLogDao
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

    fun observeChanges(): kotlinx.coroutines.flow.Flow<String> = researchLogDao.observeChangeTick()

    /** Slot dzikir yang sudah dicentang pada [tanggal] (0..2), untuk rasio "Progress Hari Ini". */
    suspend fun getDzikirDoneCount(tanggal: String): Int = researchLogDao.countDzikirDikerjakan(tanggal)

    companion object {
        const val DZIKIR_SLOTS_PER_DAY = 2

        internal fun mergeCounts(
            a: List<DailyCompletionCount>,
            b: List<DailyCompletionCount>
        ): List<DailyCompletionCount> =
            (a + b).groupBy { it.dateKey }
                .map { (date, rows) -> DailyCompletionCount(date, rows.sumOf { it.count }) }
                .sortedBy { it.dateKey }
    }
}
