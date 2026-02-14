package com.islami.Aha.ui.statistic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import com.islami.Aha.ui.shared.SunnahHabitSharedViewModel
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
    private val habitDao: HabitDao,
    private val sunnahHabitSharedViewModel: SunnahHabitSharedViewModel
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
                sunnahHabitSharedViewModel.sunnahHabits
            ) { habits, sunnahHabits ->
                Pair(habits, sunnahHabits)
            }.collect { (habits, sunnahHabits) ->
                val fardhuCompleted = habits.count { it.isCompleted }
                val sunnahCompleted = sunnahHabits.count { it.isCompletedToday }
                val todayCompleted = fardhuCompleted + sunnahCompleted
                val todayTotal = habits.size + sunnahHabits.size
                val todayPercentage = if (todayTotal > 0) (todayCompleted * 100) / todayTotal else 0

                val categoryOrder = listOf("Sholat Fardhu", "Sholat Sunnah", "Puasa Wajib", "Puasa Sunnah")
                val categoryIcons = mapOf(
                    "Sholat Fardhu" to "masjid",
                    "Sholat Sunnah" to "sun",
                    "Puasa Wajib" to "plate",
                    "Puasa Sunnah" to "moon"
                )

                val categoryStats = categoryOrder.mapIndexed { index, categoryName ->
                    val categoryHabits = habits.filter { it.category == categoryName }
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

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentDate = getCurrentDateFormatted(),
                        todayCompleted = todayCompleted,
                        todayTotal = todayTotal,
                        todayPercentage = todayPercentage,
                        categoryStats = categoryStats,
                        weeklyStats = generateWeeklyStats(todayCompleted),
                        currentStreak = 0,
                        longestStreak = 0,
                        totalCompleted = todayCompleted,
                        averagePerDay = if (todayTotal > 0) todayCompleted.toFloat() else 0f,
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

    private fun generateWeeklyStats(todayCompleted: Int): List<DailyStatistic> {
        val calendar = Calendar.getInstance()
        val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val dayNames = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")

        return dayNames.mapIndexed { index, name ->
            val dayOfWeek = index + 1 // Calendar.SUNDAY=1, MONDAY=2, etc.
            DailyStatistic(
                dayName = name,
                completedCount = if (dayOfWeek == todayDayOfWeek) todayCompleted else 0,
                isToday = dayOfWeek == todayDayOfWeek
            )
        }
    }
}
