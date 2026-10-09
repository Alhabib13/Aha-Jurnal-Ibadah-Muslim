package com.islami.Aha.data.research

import com.islami.Aha.data.research.ResearchSchema.Detail
import com.islami.Aha.data.research.ResearchSchema.Jenis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
    fun `preset qadha sholat menjadi jenis qadha_sholat tanpa detail`() {
        listOf("Qadha", "Sholat Qadha").forEach { name ->
            val slot = ResearchMapping.fromSunnahHabit(name, "SHOLAT")!!
            assertEquals(Jenis.QADHA_SHOLAT, slot.jenisIbadah)
            assertNull(slot.namaDetail)
            assertEquals(name, slot.namaDetailAsli)
        }
        assertFalse(Detail.QADHA in ResearchSchema.DETAIL_PER_JENIS.getValue(Jenis.SHOLAT_SUNNAH))
        assertFalse(Detail.QADHA in ResearchSchema.DETAIL_PER_JENIS.getValue(Jenis.SHOLAT_WAJIB))
    }

    @Test
    fun `logId slot tetap tanpa akhiran`() {
        assertEquals(
            "uid1_2026-10-09_sholat_wajib_subuh",
            ResearchSchema.logId("uid1", "2026-10-09", Jenis.SHOLAT_WAJIB, Detail.SUBUH)
        )
        assertEquals("uid1_2026-10-09_iqro", ResearchSchema.logId("uid1", "2026-10-09", Jenis.IQRO, null))
        assertThrows(IllegalArgumentException::class.java) {
            ResearchSchema.logId("uid1", "2026-10-09", Jenis.SHOLAT_WAJIB, Detail.SUBUH, "x1")
        }
    }

    @Test
    fun `logId multi baris wajib id lokal`() {
        assertEquals(
            "uid1_2026-10-09_tilawah_sesi-a1",
            ResearchSchema.logId("uid1", "2026-10-09", Jenis.TILAWAH, null, "sesi-a1")
        )
        assertEquals(
            "uid1_2026-10-09_qadha_sholat_6f1c-2a9e",
            ResearchSchema.logId("uid1", "2026-10-09", Jenis.QADHA_SHOLAT, null, "6f1c-2a9e")
        )
        assertEquals(
            "uid1_2026-10-09_sholat_sunnah_sunnah_lainnya_6f1c-2a9e",
            ResearchSchema.logId("uid1", "2026-10-09", Jenis.SHOLAT_SUNNAH, Detail.SUNNAH_LAINNYA, "6f1c-2a9e")
        )
        assertThrows(IllegalArgumentException::class.java) {
            ResearchSchema.logId("uid1", "2026-10-09", Jenis.TILAWAH, null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ResearchSchema.logId("uid1", "2026-10-09", Jenis.TILAWAH, null, "ada_garis_bawah")
        }
    }

    @Test
    fun `batas nilai sama dengan rules`() {
        assertEquals(1..10, ResearchSchema.IQRO_JILID)
        assertEquals(1..100, ResearchSchema.IQRO_HALAMAN)
        assertEquals(1..1000, ResearchSchema.TILAWAH_HALAMAN)
    }
}
