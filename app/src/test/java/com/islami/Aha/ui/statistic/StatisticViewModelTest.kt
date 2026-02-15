package com.islami.Aha.ui.statistic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticViewModelTest {

    @Test
    fun `default state has expected values`() {
        val state = StatisticUiState()
        assertTrue(state.isLoading)
        assertEquals("", state.currentDate)
        assertEquals(0, state.todayCompleted)
        assertEquals(0, state.todayTotal)
        assertEquals(0, state.todayPercentage)
        assertTrue(state.weeklyStats.isEmpty())
        assertTrue(state.categoryStats.isEmpty())
        assertEquals(0, state.currentStreak)
        assertEquals(0, state.longestStreak)
        assertEquals(0, state.totalCompleted)
        assertEquals(0f, state.averagePerDay)
    }

    @Test
    fun `DailyStatistic holds correct values`() {
        val stat = DailyStatistic(dayName = "Sen", completedCount = 5, isToday = true)
        assertEquals("Sen", stat.dayName)
        assertEquals(5, stat.completedCount)
        assertTrue(stat.isToday)
    }

    @Test
    fun `DailyStatistic isToday defaults to false`() {
        val stat = DailyStatistic(dayName = "Sel", completedCount = 3)
        assertEquals(false, stat.isToday)
    }

    @Test
    fun `CategoryStatistic holds correct values`() {
        val stat = CategoryStatistic(
            name = "Sholat Fardhu",
            icon = "masjid",
            completedCount = 3,
            totalCount = 5,
            percentage = 60,
            streak = 7,
            gradientIndex = 0
        )
        assertEquals("Sholat Fardhu", stat.name)
        assertEquals("masjid", stat.icon)
        assertEquals(3, stat.completedCount)
        assertEquals(5, stat.totalCount)
        assertEquals(60, stat.percentage)
        assertEquals(7, stat.streak)
    }

    @Test
    fun `CategoryStatistic gradientIndex defaults to 0`() {
        val stat = CategoryStatistic(
            name = "Test",
            icon = "test",
            completedCount = 0,
            totalCount = 0,
            percentage = 0,
            streak = 0
        )
        assertEquals(0, stat.gradientIndex)
    }

    @Test
    fun `StatisticUiState with populated data`() {
        val weeklyStats = listOf(
            DailyStatistic("Sen", 3),
            DailyStatistic("Sel", 5, isToday = true)
        )
        val categoryStats = listOf(
            CategoryStatistic("Sholat Fardhu", "masjid", 4, 5, 80, 3)
        )

        val state = StatisticUiState(
            isLoading = false,
            currentDate = "Jumat, 14 Februari 2026",
            todayCompleted = 8,
            todayTotal = 12,
            todayPercentage = 66,
            weeklyStats = weeklyStats,
            categoryStats = categoryStats,
            currentStreak = 5,
            longestStreak = 10,
            totalCompleted = 42,
            averagePerDay = 6.0f,
            sunnahTotal = 4,
            sunnahCompleted = 2
        )

        assertEquals(false, state.isLoading)
        assertEquals(8, state.todayCompleted)
        assertEquals(12, state.todayTotal)
        assertEquals(66, state.todayPercentage)
        assertEquals(2, state.weeklyStats.size)
        assertEquals(1, state.categoryStats.size)
        assertEquals(5, state.currentStreak)
        assertEquals(10, state.longestStreak)
        assertEquals(42, state.totalCompleted)
        assertEquals(6.0f, state.averagePerDay)
        assertEquals(4, state.sunnahTotal)
        assertEquals(2, state.sunnahCompleted)
    }
}
