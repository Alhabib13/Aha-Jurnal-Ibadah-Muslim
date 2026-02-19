package com.islami.Aha.ui.statistic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.data.repository.AdminConfigRepository
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.model.DailyCompletionCount
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import com.islami.Aha.ui.shared.SunnahHabitSharedViewModel
import com.islami.Aha.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class DailyStatistic(
    val dayName: String,
    val completedCount: Int,
    val isToday: Boolean = false
)

data class CategoryStatistic(
    val name: String,
    val icon: String,
    val completedCount: Int,
    val totalCount: Int,
    val percentage: Int,
    val streak: Int,
    val gradientIndex: Int = 0
)

data class StatisticUiState(
    val isLoading: Boolean = true,
    val currentDate: String = "",
    val todayCompleted: Int = 0,
    val todayTotal: Int = 0,
    val todayPercentage: Int = 0,
    val weeklyStats: List<DailyStatistic> = emptyList(),
    val categoryStats: List<CategoryStatistic> = emptyList(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompleted: Int = 0,
    val averagePerDay: Float = 0f,
    val sunnahTotal: Int = 0,
    val sunnahCompleted: Int = 0
)

@HiltViewModel
class StatisticViewModel @Inject constructor(
    private val habitCompletionDao: HabitCompletionDao,
    private val habitDao: HabitDao,
    private val sunnahHabitSharedViewModel: SunnahHabitSharedViewModel,
    private val adminConfigRepository: AdminConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticUiState())
    val uiState: StateFlow<StatisticUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                habitDao.getHabits(),
                sunnahHabitSharedViewModel.sunnahHabits,
                adminConfigRepository.featureConfig
            ) { habits, sunnahHabits, config ->
                Triple(habits, sunnahHabits, config)
            }.collect { (habits, sunnahHabits, config) ->
                val visibleHabits = habits.filter {
                    if (it.category == "Puasa Wajib" && (!DateUtils.isRamadanMonth() || !config.puasaWajibRamadanEnabled)) {
                        return@filter false
                    }
                    if (it.category == "Sholat Tarawih" && (!DateUtils.isRamadanMonth() || !config.sholatTarawihEnabled)) {
                        return@filter false
                    }
                    true
                }
                val fardhuCompleted = visibleHabits.count { it.isCompleted }
                val sunnahCompleted = sunnahHabits.count { it.isCompletedToday }
                val todayCompleted = fardhuCompleted + sunnahCompleted
                val todayTotal = visibleHabits.size + sunnahHabits.size
                val todayPercentage = if (todayTotal > 0) (todayCompleted * 100) / todayTotal else 0

                val categoryOrder = buildList {
                    add("Sholat Fardhu")
                    add("Sholat Sunnah")
                    if (DateUtils.isRamadanMonth() && config.sholatTarawihEnabled) {
                        add("Sholat Tarawih")
                    }
                    add("Puasa Wajib")
                    add("Puasa Sunnah")
                }
                val categoryIcons = mapOf(
                    "Sholat Fardhu" to "masjid",
                    "Sholat Sunnah" to "sun",
                    "Sholat Tarawih" to "moon",
                    "Puasa Wajib" to "plate",
                    "Puasa Sunnah" to "moon"
                )

                val categoryStats = categoryOrder.mapIndexed { index, categoryName ->
                    val categoryHabits = visibleHabits.filter { it.category == categoryName }
                    var completed = categoryHabits.count { it.isCompleted }
                    var total = categoryHabits.size

                    // Include user-added sunnah habits in their respective categories
                    when (categoryName) {
                        "Sholat Sunnah" -> {
                            val sunnah = sunnahHabits.filter { it.category == SunnahCategoryType.SHOLAT }
                            total += sunnah.size
                            completed += sunnah.count { it.isCompletedToday }
                        }
                        "Puasa Sunnah" -> {
                            val sunnah = sunnahHabits.filter { it.category == SunnahCategoryType.PUASA }
                            total += sunnah.size
                            completed += sunnah.count { it.isCompletedToday }
                        }
                    }

                    CategoryStatistic(
                        name = categoryName,
                        icon = categoryIcons[categoryName] ?: "chart",
                        completedCount = completed,
                        totalCount = total,
                        percentage = if (total > 0) (completed * 100) / total else 0,
                        streak = 0,
                        gradientIndex = index
                    )
                }

                val last7DateKeys = DateUtils.getDateKeysLastDays(7)
                val dailyCounts = if (last7DateKeys.isNotEmpty()) {
                    habitCompletionDao.getDailyCountsBetween(last7DateKeys.first(), last7DateKeys.last())
                } else {
                    emptyList()
                }
                val completionDates = habitCompletionDao.getDistinctCompletionDatesDesc()
                val currentStreak = calculateCurrentStreak(completionDates)
                val longestStreak = calculateLongestStreak(completionDates)
                val historicalTotal = habitCompletionDao.getTotalCompletionCount()
                val activeDaysCount = completionDates.size
                val averagePerDay = if (activeDaysCount > 0) {
                    historicalTotal.toFloat() / activeDaysCount
                } else 0f

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentDate = getCurrentDateFormatted(),
                        todayCompleted = todayCompleted,
                        todayTotal = todayTotal,
                        todayPercentage = todayPercentage,
                        categoryStats = categoryStats,
                        weeklyStats = generateWeeklyStats(last7DateKeys, dailyCounts),
                        currentStreak = currentStreak,
                        longestStreak = longestStreak,
                        totalCompleted = historicalTotal,
                        averagePerDay = averagePerDay,
                        sunnahTotal = sunnahHabits.size,
                        sunnahCompleted = sunnahCompleted
                    )
                }
            }
        }
    }

    private fun getCurrentDateFormatted(): String {
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.forLanguageTag("id-ID"))
        return dateFormat.format(Date())
    }

    private fun generateWeeklyStats(
        last7DateKeys: List<String>,
        dailyCounts: List<DailyCompletionCount>
    ): List<DailyStatistic> {
        val countsByDate = dailyCounts.associate { it.dateKey to it.count }
        val todayKey = DateUtils.getTodayKey()
        return last7DateKeys.map { key ->
            val dayName = runCatching {
                val day = java.time.LocalDate.parse(key)
                when (day.dayOfWeek) {
                    java.time.DayOfWeek.SUNDAY -> "Min"
                    java.time.DayOfWeek.MONDAY -> "Sen"
                    java.time.DayOfWeek.TUESDAY -> "Sel"
                    java.time.DayOfWeek.WEDNESDAY -> "Rab"
                    java.time.DayOfWeek.THURSDAY -> "Kam"
                    java.time.DayOfWeek.FRIDAY -> "Jum"
                    java.time.DayOfWeek.SATURDAY -> "Sab"
                }
            }.getOrDefault("-")
            DailyStatistic(
                dayName = dayName,
                completedCount = countsByDate[key] ?: 0,
                isToday = key == todayKey
            )
        }
    }

    private fun calculateCurrentStreak(distinctDatesDesc: List<String>): Int {
        if (distinctDatesDesc.isEmpty()) return 0
        val dateSet = distinctDatesDesc.toSet()
        var cursor = java.time.LocalDate.now()
        var streak = 0
        while (dateSet.contains(cursor.toString())) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun calculateLongestStreak(distinctDatesDesc: List<String>): Int {
        if (distinctDatesDesc.isEmpty()) return 0
        val sortedAsc = distinctDatesDesc.mapNotNull { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }.sorted()
        if (sortedAsc.isEmpty()) return 0
        var longest = 1
        var current = 1
        for (i in 1 until sortedAsc.size) {
            if (sortedAsc[i - 1].plusDays(1) == sortedAsc[i]) {
                current++
                if (current > longest) longest = current
            } else {
                current = 1
            }
        }
        return longest
    }
}
