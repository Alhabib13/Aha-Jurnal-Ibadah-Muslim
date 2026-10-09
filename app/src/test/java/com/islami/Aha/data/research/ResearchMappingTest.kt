package com.islami.Aha.data.research

import com.islami.Aha.data.research.ResearchSchema.Detail
import com.islami.Aha.data.research.ResearchSchema.Jenis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearchMappingTest {

    @Test
    fun `lima sholat fardhu bawaan terpetakan`() {
        val expected = mapOf(
            "Sholat Subuh" to Detail.SUBUH,
            "Sholat Dzuhur" to Detail.DZUHUR,
            "Sholat Ashar" to Detail.ASHAR,
            "Sholat Maghrib" to Detail.MAGHRIB,
            "Sholat Isya" to Detail.ISYA
        )
        expected.forEach { (name, detail) ->
            val slot = ResearchMapping.fromDefaultHabit(name, "Sholat Fardhu")!!
            assertEquals(Jenis.SHOLAT_WAJIB, slot.jenisIbadah)
            assertEquals(detail, slot.namaDetail)
            assertEquals(name, slot.namaDetailAsli)
        }
    }

    @Test
    fun `sholat jumat dicatat sebagai dzuhur dengan nama asli jumat`() {
        val slot = ResearchMapping.fromDefaultHabit("Sholat Jumat", "Sholat Fardhu")!!
        assertEquals(Detail.DZUHUR, slot.namaDetail)
        assertEquals("Sholat Jumat", slot.namaDetailAsli)
    }

    @Test
    fun `habit ramadan terpetakan`() {
        assertEquals(Detail.TARAWIH, ResearchMapping.fromDefaultHabit("Sholat Tarawih", "Sholat Tarawih")!!.namaDetail)
        val puasa = ResearchMapping.fromDefaultHabit("Puasa Ramadan", "Puasa Wajib")!!
        assertEquals(Jenis.PUASA, puasa.jenisIbadah)
        assertEquals(Detail.RAMADAN, puasa.namaDetail)
    }

    @Test
    fun `fardhu dengan nama tak dikenal tidak dipetakan`() {
        assertNull(ResearchMapping.fromDefaultHabit("Sholat Lain", "Sholat Fardhu"))
        assertNull(ResearchMapping.fromDefaultHabit("Apa saja", "Kategori Asing"))
    }

    @Test
    fun `preset sunnah terpetakan`() {
        assertEquals(Detail.DHUHA, ResearchMapping.fromSunnahHabit("Dhuha", "SHOLAT")!!.namaDetail)
        assertEquals(Detail.DHUHA, ResearchMapping.fromSunnahHabit("Sholat Dhuha", "SHOLAT")!!.namaDetail)
        assertEquals(Detail.QADHA, ResearchMapping.fromSunnahHabit("Qadha", "SHOLAT")!!.namaDetail)
        assertEquals(Detail.SENIN_KAMIS, ResearchMapping.fromSunnahHabit("Senin Kamis", "PUASA")!!.namaDetail)
        assertEquals(Detail.AYYAMUL_BIDH, ResearchMapping.fromSunnahHabit("Ayyamul Bidh", "PUASA")!!.namaDetail)
        assertEquals(Detail.DAUD, ResearchMapping.fromSunnahHabit("Puasa Daud", "PUASA")!!.namaDetail)
        assertEquals(Detail.SYAWAL, ResearchMapping.fromSunnahHabit("Puasa Syawal", "PUASA")!!.namaDetail)
        assertEquals(Detail.NAZAR, ResearchMapping.fromSunnahHabit("Puasa Nazar", "PUASA")!!.namaDetail)
        assertEquals(Detail.QADHA, ResearchMapping.fromSunnahHabit("Pengganti Ramadan", "PUASA")!!.namaDetail)
    }

    @Test
    fun `nama buatan pengguna jatuh ke sunnah_lainnya dan nama asli disimpan`() {
        val slot = ResearchMapping.fromSunnahHabit("Sholat Hajat", "SHOLAT")!!
        assertEquals(Jenis.SHOLAT_SUNNAH, slot.jenisIbadah)
        assertEquals(Detail.SUNNAH_LAINNYA, slot.namaDetail)
        assertEquals("Sholat Hajat", slot.namaDetailAsli)
        assertEquals(Detail.SUNNAH_LAINNYA, ResearchMapping.fromSunnahHabit("Puasa Arafah", "PUASA")!!.namaDetail)
    }

    @Test
    fun `semua hasil pemetaan sah menurut DETAIL_PER_JENIS`() {
        val samples = listOf(
            ResearchMapping.fromDefaultHabit("Sholat Subuh", "Sholat Fardhu"),
            ResearchMapping.fromDefaultHabit("Sholat Tarawih", "Sholat Tarawih"),
            ResearchMapping.fromDefaultHabit("Puasa Ramadan", "Puasa Wajib"),
            ResearchMapping.fromSunnahHabit("Witir", "SHOLAT"),
            ResearchMapping.fromSunnahHabit("Rawatib", "SHOLAT"),
            ResearchMapping.fromSunnahHabit("Tahajud", "SHOLAT"),
            ResearchMapping.fromSunnahHabit("Istikharah", "SHOLAT"),
            ResearchMapping.fromSunnahHabit("Taubat", "SHOLAT"),
            ResearchMapping.fromSunnahHabit("Bebas", "PUASA")
        )
        samples.forEach { maybeSlot ->
            val slot = requireNotNull(maybeSlot)
            assertTrue(slot.namaDetail in ResearchSchema.DETAIL_PER_JENIS.getValue(slot.jenisIbadah))
        }
    }

    @Test
    fun `logId deterministik dan cocok dengan pola rules`() {
        assertEquals(
            "uid1_2026-10-09_sholat_wajib_subuh",
            ResearchSchema.logId("uid1", "2026-10-09", Jenis.SHOLAT_WAJIB, Detail.SUBUH)
        )
        assertEquals("uid1_2026-10-09_tilawah", ResearchSchema.logId("uid1", "2026-10-09", Jenis.TILAWAH, null))
        val custom = ResearchSchema.logId("uid1", "2026-10-09", Jenis.SHOLAT_SUNNAH, Detail.SUNNAH_LAINNYA, "6f1c-2a9e")
        assertTrue(custom.matches(Regex("uid1_2026-10-09_[A-Za-z0-9_-]+")))
    }
}
