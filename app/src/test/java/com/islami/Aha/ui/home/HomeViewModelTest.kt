package com.islami.Aha.ui.home

import com.islami.Aha.data.model.Habit
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {

    // ===================== Default State =====================

    @Test
    fun `default state keeps expected base values`() {
        val state = HomeUiState()
        assertEquals("Memuat lokasi...", state.location)
        assertEquals("Sholat", state.selectedMainCategory)
        assertEquals(0, state.selectedSubTabIndex)
        assertTrue(state.isLoading)
    }

    // ===================== Sub Tab Categories =====================

    @Test
    fun `sholat category has fardhu and sunnah sub tabs`() {
        val state = HomeUiState(
            selectedMainCategory = "Sholat",
            isRamadanMonth = false
        )
        assertEquals(listOf("Sholat Fardhu", "Sholat Sunnah"), state.subTabCategories)
    }

    @Test
    fun `sholat category adds tarawih tab when ramadan and enabled`() {
        val state = HomeUiState(
            selectedMainCategory = "Sholat",
            isRamadanMonth = true,
            sholatTarawihEnabled = true
        )
        assertEquals(
            listOf("Sholat Fardhu", "Sholat Sunnah", "Sholat Tarawih"),
            state.subTabCategories
        )
    }

    @Test
    fun `puasa category has wajib and sunnah sub tabs`() {
        val state = HomeUiState(selectedMainCategory = "Puasa")
        assertEquals(listOf("Puasa Wajib", "Puasa Sunnah"), state.subTabCategories)
    }

    @Test
    fun `dzikir category has empty sub tabs`() {
        val state = HomeUiState(selectedMainCategory = "Dzikir")
        assertTrue(state.subTabCategories.isEmpty())
    }

    // ===================== Coming Soon =====================

    @Test
    fun `dzikir is coming soon`() {
        val state = HomeUiState(selectedMainCategory = "Dzikir")
        assertTrue(state.isComingSoon)
    }

    @Test
    fun `tilawah is coming soon`() {
        val state = HomeUiState(selectedMainCategory = "Tilawah")
        assertTrue(state.isComingSoon)
    }

    @Test
    fun `sholat is not coming soon`() {
        val state = HomeUiState(selectedMainCategory = "Sholat")
        assertFalse(state.isComingSoon)
    }

    // ===================== Filtered Habits =====================

    private val sampleHabits = listOf(
        Habit(id = 1, name = "Subuh", category = "Sholat Fardhu", icon = "sunrise", description = ""),
        Habit(id = 2, name = "Dzuhur", category = "Sholat Fardhu", icon = "sun", description = ""),
        Habit(id = 3, name = "Dhuha", category = "Sholat Sunnah", icon = "sun", description = ""),
        Habit(id = 4, name = "Ramadan", category = "Puasa Wajib", icon = "plate", description = ""),
        Habit(id = 5, name = "Senin", category = "Puasa Sunnah", icon = "moon", description = "")
    )

    @Test
    fun `filtered habits returns sholat fardhu for sholat tab 0`() {
        val state = HomeUiState(
            selectedMainCategory = "Sholat",
            selectedSubTabIndex = 0,
            allHabits = sampleHabits
        )
        assertEquals(2, state.filteredHabits.size)
        assertTrue(state.filteredHabits.all { it.category == "Sholat Fardhu" })
    }

    @Test
    fun `filtered habits returns sholat sunnah for sholat tab 1`() {
        val state = HomeUiState(
            selectedMainCategory = "Sholat",
            selectedSubTabIndex = 1,
            allHabits = sampleHabits
        )
        assertEquals(1, state.filteredHabits.size)
        assertEquals("Dhuha", state.filteredHabits.first().name)
    }

    @Test
    fun `filtered habits returns puasa wajib for puasa tab 0`() {
        val state = HomeUiState(
            selectedMainCategory = "Puasa",
            selectedSubTabIndex = 0,
            allHabits = sampleHabits,
            isRamadanMonth = true
        )
        assertEquals(1, state.filteredHabits.size)
        assertEquals("Ramadan", state.filteredHabits.first().name)
    }

    @Test
    fun `filtered habits returns empty for coming soon category`() {
        val state = HomeUiState(
            selectedMainCategory = "Dzikir",
            selectedSubTabIndex = 0,
            allHabits = sampleHabits
        )
        assertTrue(state.filteredHabits.isEmpty())
    }

    // ===================== Filtered Sunnah Habits =====================

    private val sampleSunnahHabits = listOf(
        SunnahHabit(id = "1", name = "Tahajud", category = SunnahCategoryType.SHOLAT, isCompletedToday = true),
        SunnahHabit(id = "2", name = "Istikharah", category = SunnahCategoryType.SHOLAT),
        SunnahHabit(id = "3", name = "Senin Kamis", category = SunnahCategoryType.PUASA, isCompletedToday = true)
    )

    @Test
    fun `filtered sunnah habits returns sholat sunnah on sholat tab 1`() {
        val state = HomeUiState(
            selectedMainCategory = "Sholat",
            selectedSubTabIndex = 1,
            sunnahHabits = sampleSunnahHabits
        )
        assertEquals(2, state.filteredSunnahHabits.size)
        assertTrue(state.filteredSunnahHabits.all { it.category == SunnahCategoryType.SHOLAT })
    }

    @Test
    fun `filtered sunnah habits returns puasa sunnah on puasa tab 1`() {
        val state = HomeUiState(
            selectedMainCategory = "Puasa",
            selectedSubTabIndex = 1,
            sunnahHabits = sampleSunnahHabits
        )
        assertEquals(1, state.filteredSunnahHabits.size)
        assertEquals("Senin Kamis", state.filteredSunnahHabits.first().name)
    }

    @Test
    fun `filtered sunnah habits returns empty on fardhu tab`() {
        val state = HomeUiState(
            selectedMainCategory = "Sholat",
            selectedSubTabIndex = 0,
            sunnahHabits = sampleSunnahHabits
        )
        assertTrue(state.filteredSunnahHabits.isEmpty())
    }

    // ===================== Completed & Total Counts =====================

    @Test
    fun `completed count includes both fardhu and sunnah`() {
        val state = HomeUiState(
            selectedMainCategory = "Sholat",
            selectedSubTabIndex = 1,
            allHabits = listOf(
                Habit(id = 3, name = "Dhuha", category = "Sholat Sunnah", icon = "sun", description = "", isCompleted = true)
            ),
            sunnahHabits = sampleSunnahHabits
        )
        // 1 fardhu habit completed + 1 sunnah completed (Tahajud)
        assertEquals(2, state.completedHabitsCount)
        assertEquals(3, state.totalHabitsCount) // 1 fardhu + 2 sunnah
    }

    // ===================== Category Badge =====================

    @Test
    fun `getCategoryBadge for sholat shows correct counts`() {
        val state = HomeUiState(
            allHabits = listOf(
                Habit(id = 1, name = "Subuh", category = "Sholat Fardhu", icon = "sunrise", description = "", isCompleted = true),
                Habit(id = 2, name = "Dzuhur", category = "Sholat Fardhu", icon = "sun", description = ""),
                Habit(id = 3, name = "Dhuha", category = "Sholat Sunnah", icon = "sun", description = "", isCompleted = true)
            ),
            sunnahHabits = listOf(
                SunnahHabit(id = "1", name = "Tahajud", category = SunnahCategoryType.SHOLAT, isCompletedToday = true)
            )
        )
        assertEquals("3/4", state.getCategoryBadge("Sholat"))
    }

    @Test
    fun `getCategoryBadge for puasa shows correct counts`() {
        val state = HomeUiState(
            allHabits = listOf(
                Habit(id = 4, name = "Ramadan", category = "Puasa Wajib", icon = "plate", description = "", isCompleted = true)
            ),
            sunnahHabits = listOf(
                SunnahHabit(id = "3", name = "Senin Kamis", category = SunnahCategoryType.PUASA)
            ),
            isRamadanMonth = true
        )
        assertEquals("1/2", state.getCategoryBadge("Puasa"))
    }

    @Test
    fun `getCategoryBadge for dzikir returns segera`() {
        val state = HomeUiState()
        assertEquals("Segera", state.getCategoryBadge("Dzikir"))
    }

    // ===================== Display Names =====================

    @Test
    fun `puasa sub tab display names include ramadan label`() {
        val state = HomeUiState(selectedMainCategory = "Puasa")
        assertEquals("Puasa Wajib (Ramadan)", state.subTabDisplayNames[0])
        assertEquals("Puasa Sunnah", state.subTabDisplayNames[1])
    }
}
