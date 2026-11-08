package com.islami.Aha.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranCalculatorTest {

    // ---------- integritas data bundel ----------

    @Test
    fun `data berisi 114 surah 6236 ayat 604 halaman 30 juz`() {
        assertEquals(114, QuranData.NAMA_SURAH.size)
        assertEquals(114, QuranData.JUMLAH_AYAT.size)
        assertEquals(6236, QuranData.JUMLAH_AYAT.sum())
        assertEquals(604, QuranData.AWAL_HALAMAN.size)
        assertEquals(30, QuranData.AWAL_JUZ.size)
    }

    @Test
    fun `awal halaman dan juz terurut naik dan dimulai dari 1_1`() {
        assertEquals(0, QuranData.AWAL_HALAMAN[0])
        assertEquals(0, QuranData.AWAL_JUZ[0])
        for (i in 1 until 604) assertTrue("halaman $i", QuranData.AWAL_HALAMAN[i] > QuranData.AWAL_HALAMAN[i - 1])
        for (i in 1 until 30) assertTrue("juz $i", QuranData.AWAL_JUZ[i] > QuranData.AWAL_JUZ[i - 1])
    }

    @Test
    fun `titik acuan mushaf Madinah`() {
        assertEquals(1, QuranCalculator.halamanOf(AyatPosition(1, 1)))
        assertEquals(2, QuranCalculator.halamanOf(AyatPosition(2, 1)))
        assertEquals(3, QuranCalculator.halamanOf(AyatPosition(2, 6)))
        assertEquals(604, QuranCalculator.halamanOf(AyatPosition(112, 1)))
        assertEquals(604, QuranCalculator.halamanOf(AyatPosition(114, 6)))
        assertEquals(AyatPosition(2, 142), QuranCalculator.awalJuz(2))
        assertEquals(AyatPosition(78, 1), QuranCalculator.awalJuz(30))
        assertEquals(AyatPosition(114, 6), QuranCalculator.akhirJuz(30))
        assertEquals(1, QuranCalculator.juzOf(AyatPosition(2, 141)))
        assertEquals(2, QuranCalculator.juzOf(AyatPosition(2, 142)))
    }

    @Test
    fun `jumlah ayat acuan dan nama transliterasi`() {
        assertEquals(7, QuranData.JUMLAH_AYAT[0])
        assertEquals(286, QuranData.JUMLAH_AYAT[1])
        assertEquals(85, QuranData.JUMLAH_AYAT[39])   // Gafir
        assertEquals(8, QuranData.JUMLAH_AYAT[93])    // Asy-Syarh
        assertEquals(6, QuranData.JUMLAH_AYAT[113])   // An-Nas
        assertEquals("Al-Fatihah", QuranData.NAMA_SURAH[0])
        assertEquals("Yasin", QuranData.NAMA_SURAH[35])
        assertEquals("An-Nas", QuranData.NAMA_SURAH[113])
        assertTrue("tidak boleh ada huruf Arab", QuranData.NAMA_SURAH.all { name -> name.all { it.code < 0x0600 || it.code > 0x06FF } })
    }

    // ---------- penghitung rentang ----------

    @Test
    fun `rentang dalam satu surah dan lintas surah`() {
        val fatihah = QuranCalculator.hitung(AyatPosition(1, 1), AyatPosition(1, 7))!!
        assertEquals(7, fatihah.jumlahAyat)
        assertEquals(1, fatihah.jumlahHalaman)
        assertEquals(1, fatihah.juzAwal)

        val lintas = QuranCalculator.hitung(AyatPosition(1, 1), AyatPosition(2, 5))!!
        assertEquals(12, lintas.jumlahAyat)
        assertEquals(2, lintas.jumlahHalaman)
    }

    @Test
    fun `halaman tersentuh minimal 1`() {
        assertEquals(1, QuranCalculator.hitung(AyatPosition(2, 1), AyatPosition(2, 5))!!.jumlahHalaman)
        assertEquals(2, QuranCalculator.hitung(AyatPosition(2, 1), AyatPosition(2, 6))!!.jumlahHalaman)
        assertEquals(1, QuranCalculator.hitung(AyatPosition(2, 3), AyatPosition(2, 3))!!.jumlahHalaman)
    }

    @Test
    fun `seluruh Al-Quran`() {
        val semua = QuranCalculator.hitung(AyatPosition.AWAL, AyatPosition.AKHIR)!!
        assertEquals(6236, semua.jumlahAyat)
        assertEquals(604, semua.jumlahHalaman)
    }

    @Test
    fun `juz awal sesi`() {
        assertEquals(30, QuranCalculator.hitung(AyatPosition(78, 1), AyatPosition(78, 40))!!.juzAwal)
        assertEquals(1, QuranCalculator.hitung(AyatPosition(2, 140), AyatPosition(2, 150))!!.juzAwal)
    }

    @Test
    fun `rentang tidak valid ditolak`() {
        assertNull(QuranCalculator.hitung(AyatPosition(2, 10), AyatPosition(2, 9)))
        assertNull(QuranCalculator.hitung(AyatPosition(1, 8), AyatPosition(2, 1)))
        assertNull(QuranCalculator.hitung(AyatPosition(0, 1), AyatPosition(1, 1)))
        assertNull(QuranCalculator.hitung(AyatPosition(114, 1), AyatPosition(115, 1)))
        assertFalse(AyatPosition(114, 7).isValid)
    }

    // ---------- penanda ----------

    @Test
    fun `posisi setelah akhir rentang`() {
        assertEquals(AyatPosition(1, 6), QuranCalculator.setelah(AyatPosition(1, 5)))
        assertEquals(AyatPosition(2, 1), QuranCalculator.setelah(AyatPosition(1, 7)))
        assertEquals(AyatPosition(114, 1), QuranCalculator.setelah(AyatPosition(113, 5)))
        assertNull("khatam", QuranCalculator.setelah(AyatPosition.AKHIR))
    }

    @Test
    fun `surah dalam juz untuk pemilih juz`() {
        assertEquals(78..114, QuranCalculator.surahDalamJuz(30))
        assertEquals(1..2, QuranCalculator.surahDalamJuz(1))
    }

    @Test
    fun `absolut dan kembali konsisten untuk semua ayat`() {
        for (i in 0 until QuranData.TOTAL_AYAT) {
            assertEquals(i, AyatPosition.fromAbsolute(i).absolute)
        }
    }

    @Test
    fun `akhir halaman untuk tombol cepat tilawah`() {
        assertEquals(AyatPosition(1, 7), QuranCalculator.akhirHalaman(1))
        assertEquals(AyatPosition(114, 6), QuranCalculator.akhirHalaman(604))
        val p2 = QuranCalculator.akhirHalaman(2)
        assertEquals(2, QuranCalculator.halamanOf(p2))
        assertEquals(3, QuranCalculator.halamanOf(QuranCalculator.setelah(p2)!!))
    }
}
