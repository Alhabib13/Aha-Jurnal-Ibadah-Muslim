package com.islami.Aha.ui.home

import com.islami.Aha.data.model.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JamDicatatTest {
    private val state = HomeUiState(jamSholatDicatat = mapOf("subuh" to "04.12", "dzuhur" to "11.40"))

    @Test
    fun `sholat yang dicentang menampilkan jam pencatatan`() {
        val subuh = Habit(1, "Sholat Subuh", "Sholat Fardhu", "sunrise", "", isCompleted = true)
        assertEquals("04.12", state.jamDicatat(subuh))
        // Jumat memakai slot dzuhur.
        val jumat = Habit(2, "Sholat Jumat", "Sholat Fardhu", "sun", "", isCompleted = true)
        assertEquals("11.40", state.jamDicatat(jumat))
    }

    @Test
    fun `belum dicentang atau tanpa log tidak menampilkan jam`() {
        assertNull(state.jamDicatat(Habit(1, "Sholat Subuh", "Sholat Fardhu", "sunrise", "", isCompleted = false)))
        assertNull(state.jamDicatat(Habit(3, "Sholat Ashar", "Sholat Fardhu", "cloud", "", isCompleted = true)))
    }
}
