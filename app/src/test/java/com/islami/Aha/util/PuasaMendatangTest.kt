package com.islami.Aha.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

class PuasaMendatangTest {
    // Minggu 18 Oktober 2026 = 7 Jumadil Awal 1448. Angka pembanding sama dengan aplikasi
    // pelacak lain yang memakai Umm al-Qura pada hari yang sama.
    private val minggu = LocalDate.of(2026, 10, 18)
    private fun cari(daftar: List<PuasaMendatang.Entri>, nama: String) = daftar.single { it.nama == nama }

    @Test
    fun `hari-hari menuju puasa berikutnya`() {
        val d = PuasaMendatang.hitung(minggu)
        assertEquals(1, cari(d, "Senin Kamis").hariLagi)
        assertEquals(6, cari(d, "Ayyamul Bidh").hariLagi)
        assertEquals(113, cari(d, "Ramadhan").hariLagi)
        assertEquals(143, cari(d, "Puasa Syawal").hariLagi)
    }

    @Test
    fun `urut dari yang paling dekat dan tidak ada tanggal lampau`() {
        val d = PuasaMendatang.hitung(minggu)
        assertEquals(d.sortedBy { it.tanggal }, d)
        assertTrue(d.all { it.hariLagi >= 0 })
    }

    @Test
    fun `senin atau kamis hari ini = hari ini`() {
        val senin = LocalDate.of(2026, 10, 19)
        assertEquals(0, cari(PuasaMendatang.hitung(senin), "Senin Kamis").hariLagi)
        assertEquals("Hari: Sen • Kam", cari(PuasaMendatang.hitung(senin), "Senin Kamis").frekuensiHabit)
    }

    @Test
    fun `ayyamul bidh di bulan dzulhijjah dimulai tanggal 14`() {
        val tahun = HijrahDate.from(minggu).get(ChronoField.YEAR)
        val awalDzulhijjah = LocalDate.from(HijrahChronology.INSTANCE.date(tahun, 12, 1))
        val e = cari(PuasaMendatang.hitung(awalDzulhijjah), "Ayyamul Bidh")
        assertEquals(14, HijrahDate.from(e.tanggal).get(ChronoField.DAY_OF_MONTH))
    }

    @Test
    fun `selama ramadhan yang ditunjukkan ramadhan tahun berikutnya, ayyamul bidh dilewati`() {
        val tahun = HijrahDate.from(minggu).get(ChronoField.YEAR) + 1
        val tengahRamadhan = LocalDate.from(HijrahChronology.INSTANCE.date(tahun, 9, 10))
        val d = PuasaMendatang.hitung(tengahRamadhan)
        assertTrue(cari(d, "Ramadhan").hariLagi > 300)
        assertFalse(HijrahDate.from(cari(d, "Ayyamul Bidh").tanggal).get(ChronoField.MONTH_OF_YEAR) == 9)
    }

    @Test
    fun `penanda kalender puasa sunnah`() {
        val tahun = HijrahDate.from(minggu).get(ChronoField.YEAR)
        fun hijri(t: Int, b: Int, h: Int) = LocalDate.from(HijrahChronology.INSTANCE.date(t, b, h))
        assertEquals("Senin Kamis", PuasaMendatang.puasaSunnahPada(LocalDate.of(2026, 10, 19)))
        assertEquals(null, PuasaMendatang.puasaSunnahPada(minggu))
        assertEquals("Ayyamul Bidh", PuasaMendatang.puasaSunnahPada(LocalDate.of(2026, 10, 24)))
        assertEquals("Puasa Arafah", PuasaMendatang.puasaSunnahPada(hijri(tahun, 12, 9)))
        // Hari raya, tasyrik, dan Ramadhan tidak ditandai.
        assertEquals(null, PuasaMendatang.puasaSunnahPada(hijri(tahun, 12, 10)))
        assertEquals(null, PuasaMendatang.puasaSunnahPada(hijri(tahun, 12, 13)))
        assertEquals(null, PuasaMendatang.puasaSunnahPada(hijri(tahun + 1, 10, 1)))
        assertEquals(null, PuasaMendatang.puasaSunnahPada(hijri(tahun + 1, 9, 14)))
    }

    @Test
    fun `yang punya preset add habit bisa dijadikan habit`() {
        val d = PuasaMendatang.hitung(minggu)
        assertEquals(setOf("Senin Kamis", "Ayyamul Bidh", "Puasa Syawal"), d.mapNotNull { it.namaHabit }.toSet())
    }
}
