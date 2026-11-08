package com.islami.Aha.ui.home

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.islami.Aha.data.model.Habit
import com.islami.Aha.ui.home.components.HomeHabitItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kartu sholat wajib: satu tombol "Tertinggal" untuk menandai dan membatalkan, dan kartu yang
 * ditandai tertinggal tidak bisa langsung dicentang (harus dibatalkan dulu lewat tombol itu).
 */
@RunWith(AndroidJUnit4::class)
class TertinggalKartuTest {
    @get:Rule val compose = createComposeRule()

    private val subuh = Habit(id = 1, name = "Sholat Subuh", category = "Sholat Fardhu", icon = "", description = "", time = "04:30")

    @Test
    fun tombolTertinggal_ketukSekaliTandai_ketukLagiBatal_lingkaranTidakMencentang() {
        val aksi = mutableListOf<String>()
        var tertinggal by mutableStateOf(false)
        compose.setContent {
            HomeHabitItem(
                habit = subuh,
                onCheckedChange = { aksi += "centang:$it" },
                onToggleReminder = {},
                currentTime = "06:00",
                tertinggal = tertinggal,
                onTertinggal = { aksi += "tandai"; tertinggal = true },
                onBatalTertinggal = { aksi += "batal"; tertinggal = false }
            )
        }
        compose.onNodeWithText("Tertinggal").assertIsOff().performClick()
        compose.onNodeWithText("Tertinggal").assertIsOn()
        // Kartu bertanda tertinggal: mengetuk kartu/lingkaran tidak mencentang.
        compose.onNodeWithText("Subuh").performClick()
        compose.onNodeWithText("Tertinggal").performClick()
        compose.onNodeWithText("Tertinggal").assertIsOff()
        // Setelah dibatalkan, kartu bisa dicentang seperti biasa.
        compose.onNodeWithText("Subuh").performClick()
        compose.waitForIdle()
        assertEquals(listOf("tandai", "batal", "centang:true"), aksi)
    }
}
