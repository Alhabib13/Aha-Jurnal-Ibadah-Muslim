package com.islami.Aha.ui.home

import com.islami.Aha.data.model.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {

    @Test
    fun `default state keeps expected base values`() {
        val state = HomeUiState()
        assertEquals("Jakarta", state.location)
        assertEquals("Sholat", state.selectedMainCategory)
        assertEquals(0, state.selectedSubTabIndex)
    }

    @Test
    fun `filtered habits follows selected category and tab`() {
        val state = HomeUiState(
            selectedMainCategory = "Sholat",
            selectedSubTabIndex = 0,
            allHabits = listOf(
                Habit(id = 1, name = "Subuh", category = "Sholat Fardhu", icon = "\uD83C\uDF05", description = ""),
                Habit(id = 2, name = "Tahajud", category = "Sholat Sunnah", icon = "\uD83C\uDF19", description = "")
            )
        )

        assertEquals(1, state.filteredHabits.size)
        assertEquals("Subuh", state.filteredHabits.first().name)
        assertTrue(!state.isComingSoon)
    }
}
