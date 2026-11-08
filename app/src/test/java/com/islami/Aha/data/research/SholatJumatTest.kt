package com.islami.Aha.data.research

import com.islami.Aha.data.research.ResearchSchema.Detail
import com.islami.Aha.data.research.ResearchSchema.Jenis
import com.islami.Aha.ui.home.components.waktuSudahMasuk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SholatJumatTest {
    private val jumat = "2026-11-06"
    private val kamis = "2026-11-05"

    @Test
    fun `laki-laki di hari Jumat = jumat, kecuali diganti dzuhur`() {
        assertEquals(Detail.JUMAT, SholatJumat.detailTengahHari(jumat, lakiLaki = true, gantiDzuhur = null))
        assertEquals(Detail.JUMAT, SholatJumat.detailTengahHari(jumat, lakiLaki = true, gantiDzuhur = false))
        assertEquals(Detail.DZUHUR, SholatJumat.detailTengahHari(jumat, lakiLaki = true, gantiDzuhur = true))
    }

    @Test
    fun `perempuan dan hari selain Jumat selalu dzuhur`() {
        assertEquals(Detail.DZUHUR, SholatJumat.detailTengahHari(jumat, lakiLaki = false, gantiDzuhur = null))
        assertEquals(Detail.DZUHUR, SholatJumat.detailTengahHari(kamis, lakiLaki = true, gantiDzuhur = null))
        assertEquals(Detail.DZUHUR, SholatJumat.detailTengahHari("bukan-tanggal", lakiLaki = true, gantiDzuhur = null))
    }

    @Test
    fun `jumat dan dzuhur saling lawan, slot lain tidak punya lawan`() {
        assertEquals(Detail.DZUHUR, SholatJumat.lawan(Detail.JUMAT))
        assertEquals(Detail.JUMAT, SholatJumat.lawan(Detail.DZUHUR))
        assertNull(SholatJumat.lawan(Detail.ASHAR))
        assertNull(SholatJumat.lawan(null))
    }

    @Test
    fun `pilihan disimpulkan dari catatan bila belum tersimpan`() {
        assertEquals(true, SholatJumat.simpulkanGanti(adaJumat = false, adaDzuhur = true))
        assertEquals(false, SholatJumat.simpulkanGanti(adaJumat = true, adaDzuhur = false))
        assertNull(SholatJumat.simpulkanGanti(adaJumat = false, adaDzuhur = false))
    }

    @Test
    fun `skema - jumat sah untuk sholat wajib saja, diukur dari adzan dzuhur`() {
        assertTrue(Detail.JUMAT in ResearchSchema.DETAIL_PER_JENIS.getValue(Jenis.SHOLAT_WAJIB))
        assertFalse(Detail.JUMAT in ResearchSchema.DETAIL_PER_JENIS.getValue(Jenis.SHOLAT_SUNNAH))
        assertEquals("Sholat Dzuhur", ResearchTime.habitAcuanAdzan(Jenis.SHOLAT_WAJIB, Detail.JUMAT))
        assertEquals(Detail.JUMAT, ResearchMapping.fromDefaultHabit(SholatJumat.NAMA_JUMAT, "Sholat Fardhu")?.namaDetail)
        assertEquals(Detail.DZUHUR, ResearchMapping.fromDefaultHabit(SholatJumat.NAMA_DZUHUR, "Sholat Fardhu")?.namaDetail)
    }

    @Test
    fun `kelengkapan tetap lima slot - jumat bukan slot keenam`() {
        // Kelima slot harian: jumat menggantikan dzuhur, tidak menambah.
        val slotHarian = { lakiLaki: Boolean, tanggal: String ->
            listOf(Detail.SUBUH, SholatJumat.detailTengahHari(tanggal, lakiLaki, null), Detail.ASHAR, Detail.MAGHRIB, Detail.ISYA)
        }
        assertEquals(5, slotHarian(true, jumat).toSet().size)
        assertTrue(Detail.JUMAT in slotHarian(true, jumat) && Detail.DZUHUR !in slotHarian(true, jumat))
        assertTrue(Detail.DZUHUR in slotHarian(false, jumat))
    }

    @Test
    fun `tombol Tertinggal muncul setelah waktunya masuk`() {
        assertFalse(waktuSudahMasuk("11:45", "11:44"))
        assertTrue(waktuSudahMasuk("11:45", "11:45"))
        assertTrue(waktuSudahMasuk("04:20", "23:00"))
        assertFalse(waktuSudahMasuk("", "12:00"))
    }
}
