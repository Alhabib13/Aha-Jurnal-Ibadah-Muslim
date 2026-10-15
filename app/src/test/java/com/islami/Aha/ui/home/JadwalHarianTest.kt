package com.islami.Aha.ui.home

import com.islami.Aha.data.model.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class JadwalHarianTest {
    private val fardhu = listOf(
        Habit(1, "Sholat Subuh", "Sholat Fardhu", "sunrise", "", time = "03:51"),
        Habit(2, "Sholat Dzuhur", "Sholat Fardhu", "sun", "", time = "11:18"),
        Habit(3, "Sholat Ashar", "Sholat Fardhu", "cloud", "", time = "14:23"),
        Habit(4, "Sholat Maghrib", "Sholat Fardhu", "moon", "", time = "17:26"),
        Habit(5, "Sholat Isya", "Sholat Fardhu", "moon", "", time = "18:36")
    )

    @Test
    fun `tujuh waktu berurutan dengan imsak dan terbit dari API`() {
        val j = susunJadwalHarian(fardhu, "03:41", "05:10", "12:00")
        assertEquals(listOf("Imsak", "Subuh", "Terbit", "Dzuhur", "Ashar", "Maghrib", "Isya"), j.map { it.nama })
        assertEquals(listOf("03.41", "03.51", "05.10", "11.18", "14.23", "17.26", "18.36"), j.map { it.jam })
        assertFalse(j[0].sholat)
        assertFalse(j[2].sholat)
        assertTrue(j[3].sekarang) // 12.00 = waktu Dzuhur berjalan
        assertEquals(1, j.count { it.sekarang })
    }

    @Test
    fun `tanpa data API imsak dihitung subuh minus 10 dan terbit tidak ditebak`() {
        val j = susunJadwalHarian(fardhu, "", "", "04:00")
        assertEquals("03.41", j[0].jam)
        assertEquals("--.--", j[2].jam)
        assertTrue(j[1].sekarang)
    }

    @Test
    fun `setelah terbit sebelum dzuhur yang disorot adalah terbit`() {
        val j = susunJadwalHarian(fardhu, "03:41", "05:10", "07:30")
        assertTrue(j.first { it.nama == "Terbit" }.sekarang)
    }

    @Test
    fun `hitung mundur ke sholat berikutnya termasuk lewat tengah malam`() {
        val siang = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 8) }
        assertEquals(Triple("Isya", "18.36", 28), sholatBerikutnya(siang, fardhu))
        val malam = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 22); set(Calendar.MINUTE, 0) }
        assertEquals(Triple("Subuh", "03.51", 351), sholatBerikutnya(malam, fardhu))
        val s = HomeUiState(nextPrayerMenit = 351)
        assertEquals("05:51", s.hitungMundur)
    }
}
