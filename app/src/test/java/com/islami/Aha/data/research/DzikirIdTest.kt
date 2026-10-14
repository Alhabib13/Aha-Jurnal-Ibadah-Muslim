package com.islami.Aha.data.research

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DzikirIdTest {
    private val sesi = 1_791_600_000_000L

    @Test
    fun `ID dzikir per sesi memuat slug id habit dan penanda sesi 13 digit`() {
        val local = ResearchSchema.dzikirLocalId("3f2a9c1e-7b4d-4e8a-9c0f-1a2b3c4d5e6f", sesi)
        assertEquals(
            "uid1_2026-10-12_dzikir_dzikir_bada_sholat_3f2a9c1e-7b4d-4e8a-9c0f-1a2b3c4d5e6f_1791600000000",
            ResearchSchema.logId("uid1", "2026-10-12", ResearchSchema.Jenis.DZIKIR, ResearchSchema.Detail.DZIKIR_BADA_SHOLAT, local)
        )
    }

    @Test
    fun `cocok dengan pola rules`() {
        val id = ResearchSchema.logId(
            "uid1", "2026-10-12", ResearchSchema.Jenis.DZIKIR, ResearchSchema.Detail.ISTIGHFAR,
            ResearchSchema.dzikirLocalId("habit-1", sesi)
        )
        assertTrue(id.matches(Regex("uid1_2026-10-12_dzikir_istighfar_[A-Za-z0-9-]{1,64}_[0-9]{13}")))
    }

    @Test
    fun `dzikir tanpa penanda sesi atau dengan format salah ditolak`() {
        assertThrows(IllegalArgumentException::class.java) {
            ResearchSchema.slotKey("2026-10-12", ResearchSchema.Jenis.DZIKIR, ResearchSchema.Detail.DZIKIR_PAGI)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ResearchSchema.slotKey("2026-10-12", ResearchSchema.Jenis.DZIKIR, ResearchSchema.Detail.DZIKIR_PAGI, "habit-1")
        }
        assertThrows(IllegalArgumentException::class.java) { ResearchSchema.dzikirLocalId("habit_1", sesi) }
        assertThrows(IllegalArgumentException::class.java) { ResearchSchema.dzikirLocalId("habit-1", 179_160_000L) }
    }

    @Test
    fun `jendela anti dobel ketuk 60 detik`() {
        assertEquals(60_000L, ResearchSchema.DZIKIR_JENDELA_SESI_MS)
    }
}
