package com.islami.Aha.data.research

import com.islami.Aha.data.repository.DataTamuRepository
import com.islami.Aha.data.research.ResearchSchema.Jenis
import com.islami.Aha.data.research.ResearchSchema.Profil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KodeRespondenTertinggalTest {
    @Test
    fun `kode responden dipangkas, kosong = null, tidak sah = null`() {
        assertEquals("UNPAM-017", Profil.normalisasiKodeResponden("  UNPAM-017 "))
        assertNull(Profil.normalisasiKodeResponden(""))
        assertNull(Profil.normalisasiKodeResponden("   "))
        assertNull(Profil.normalisasiKodeResponden(null))
        assertNull(Profil.normalisasiKodeResponden("AB1"))
        assertNull(Profil.normalisasiKodeResponden("x".repeat(21)))
        assertNull(Profil.normalisasiKodeResponden("ada spasi"))
        assertEquals("abcd", Profil.normalisasiKodeResponden("abcd"))
    }

    @Test
    fun `Tertinggal berlaku untuk sholat wajib, puasa, sholat sunnah, qadha - bukan tilawah`() {
        val boleh = ResearchSchema.JENIS_BOLEH_TERTINGGAL
        assertTrue(Jenis.SHOLAT_WAJIB in boleh)
        assertTrue(Jenis.PUASA in boleh)
        assertTrue(Jenis.SHOLAT_SUNNAH in boleh)
        assertTrue(Jenis.QADHA_SHOLAT in boleh)
        assertFalse(Jenis.TILAWAH in boleh)
        assertFalse(Jenis.IQRO in boleh)
        // "tidak" hasil penyimpulan tetap hanya untuk slot harian tetap.
        assertEquals(setOf(Jenis.SHOLAT_WAJIB, Jenis.DZIKIR), ResearchSchema.JENIS_BOLEH_TIDAK)
    }

    @Test
    fun `habit sunnah & puasa buatan pengguna terpetakan ke jenis yang punya Tertinggal`() {
        val dhuha = ResearchLogRepository.slotForSunnahHabit("h1", "Sholat Dhuha", "SHOLAT")!!
        val puasa = ResearchLogRepository.slotForSunnahHabit("h2", "Puasa Senin Kamis", "PUASA")!!
        val buatan = ResearchLogRepository.slotForSunnahHabit("h3", "Sholat Awwabin", "SHOLAT")!!
        listOf(dhuha, puasa, buatan).forEach { assertTrue(it.slot.jenisIbadah in ResearchSchema.JENIS_BOLEH_TERTINGGAL) }
        // Habit buatan sendiri punya id lokal, jadi Tertinggal-nya tidak bertabrakan dengan habit lain.
        assertEquals("h3", buatan.localId)
    }

    @Test
    fun `ringkasan data tamu untuk peringatan pindah akun`() {
        val kosong = DataTamuRepository.Ringkasan(emptyList(), 0, 0)
        assertTrue(kosong.kosong)
        assertTrue(kosong.baris().isEmpty())
        val ada = DataTamuRepository.Ringkasan(listOf("Sholat Dhuha"), 2, 15)
        assertFalse(ada.kosong)
        assertEquals(listOf("Sholat Dhuha", "2 habit dzikir", "15 catatan ibadah"), ada.baris())
    }
}
