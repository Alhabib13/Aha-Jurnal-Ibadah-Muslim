package com.islami.Aha.data.research

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ResearchTimeTest {
    private val wib = ZoneId.of("Asia/Jakarta")
    private val wita = ZoneId.of("Asia/Makassar")
    private val wit = ZoneId.of("Asia/Jayapura")

    private fun ms(text: String, zone: ZoneId) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `offset WIB WITA WIT dan zona setengah jam`() {
        val t = ms("2026-10-10T05:00", wib)
        assertEquals("+07:00", ResearchTime.offsetString(t, wib))
        assertEquals("+08:00", ResearchTime.offsetString(t, wita))
        assertEquals("+09:00", ResearchTime.offsetString(t, wit))
        assertEquals("+05:30", ResearchTime.offsetString(t, ZoneId.of("Asia/Kolkata")))
        val stJohns = ZoneId.of("America/St_Johns")
        assertEquals("-03:30", ResearchTime.offsetString(ms("2026-01-10T12:00", stJohns), stJohns)) // musim dingin
        assertEquals("-02:30", ResearchTime.offsetString(ms("2026-07-10T12:00", stJohns), stJohns)) // musim panas (DST)
        assertEquals("+00:00", ResearchTime.offsetString(t, ZoneId.of("UTC")))
    }

    @Test
    fun `instan yang sama beda zona menghasilkan offset berbeda`() {
        val t = ms("2026-10-10T05:00", wib)
        assertEquals(listOf("+07:00", "+08:00"), listOf(ResearchTime.offsetString(t, wib), ResearchTime.offsetString(t, wita)))
    }

    private fun selisih(
        jam: String, tanggal: String = "2026-10-10", adzan: String? = "04:30",
        lokasi: Boolean = true, disinkron: Long = ms("2026-10-10T00:10", wib), zone: ZoneId = wib
    ) = ResearchTime.selisihMenitDariAdzan(ms(jam, zone), tanggal, adzan, lokasi, disinkron, zone)

    @Test
    fun `selisih positif negatif dan nol`() {
        assertEquals(12, selisih("2026-10-10T04:42"))
        assertEquals(-5, selisih("2026-10-10T04:25"))
        assertEquals(0, selisih("2026-10-10T04:30"))
        assertEquals(870, selisih("2026-10-10T19:00"))
    }

    @Test
    fun `null bila jadwal bukan dari lokasi atau sudah usang`() {
        assertNull(selisih("2026-10-10T04:42", lokasi = false))
        assertNull(selisih("2026-10-10T04:42", disinkron = ms("2026-10-06T04:00", wib)))
    }

    @Test
    fun `null untuk pencatatan susulan dan input rusak`() {
        assertNull(selisih("2026-10-11T00:20", tanggal = "2026-10-10"))
        assertNull(selisih("2026-10-10T04:42", adzan = null))
        assertNull(selisih("2026-10-10T04:42", adzan = "4.30"))
        assertNull(selisih("2026-10-10T04:42", tanggal = "bukan-tanggal"))
    }

    @Test
    fun `acuan adzan per slot`() {
        assertEquals("Sholat Subuh", ResearchTime.habitAcuanAdzan("sholat_wajib", "subuh"))
        assertEquals("Sholat Isya", ResearchTime.habitAcuanAdzan("sholat_wajib", "isya"))
        assertEquals("Sholat Subuh", ResearchTime.habitAcuanAdzan("dzikir", "dzikir_pagi"))
        assertEquals("Sholat Ashar", ResearchTime.habitAcuanAdzan("dzikir", "dzikir_petang"))
        assertNull(ResearchTime.habitAcuanAdzan("dzikir", "dzikir_bada_sholat"))
        assertNull(ResearchTime.habitAcuanAdzan("dzikir", "istighfar"))
        assertNull(ResearchTime.habitAcuanAdzan("tilawah", "bacaan_harian"))
        assertNull(ResearchTime.habitAcuanAdzan("puasa", "senin_kamis"))
    }
}
