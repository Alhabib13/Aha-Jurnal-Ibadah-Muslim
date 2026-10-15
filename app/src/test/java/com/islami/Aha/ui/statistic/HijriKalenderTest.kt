package com.islami.Aha.ui.statistic

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class HijriKalenderTest {
    @Test
    fun `angka arab indik`() {
        assertEquals("٤", angkaArab(4))
        assertEquals("٣٠", angkaArab(30))
    }

    @Test
    fun `hari hijriah sel kalender sama dengan Muslim Tracker`() {
        // 15 Okt 2026 = 4 Jumadil Awal 1448; 11 Okt = 30 Rabiul Akhir; 12 Okt = 1 Jumadil Awal.
        val k = susunBulan(YearMonth.of(2026, 10), emptyMap(), emptySet(), LocalDate.of(2026, 10, 15), 7)
            .filterNotNull().associateBy { it.hari }
        assertEquals("٤", k[15]!!.hijri)
        assertEquals("٣٠", k[11]!!.hijri)
        assertEquals("١", k[12]!!.hijri)
    }
}
