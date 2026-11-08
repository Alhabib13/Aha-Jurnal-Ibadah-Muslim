package com.islami.Aha.ui.statistic

import com.islami.Aha.data.model.ResearchLogEntity
import com.islami.Aha.data.research.ResearchSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PerkiraanKhatamTest {
    private val hariIni = LocalDate.of(2026, 10, 18)

    private fun sesi(tgl: String, halaman: Int, detail: String = ResearchSchema.Detail.BACAAN_HARIAN) = ResearchLogEntity(
        slotKey = "${tgl}_tilawah_${detail}_$halaman", tanggal = tgl, jenisIbadah = ResearchSchema.Jenis.TILAWAH,
        namaDetail = detail, namaDetailAsli = null, status = ResearchSchema.Status.DIKERJAKAN, jumlah = halaman,
        jilid = null, jamPencatatan = 1L, sumberPencatatan = "buka_aplikasi", ownerUid = null, updatedAt = 1L,
        offsetWaktu = "+07:00"
    )

    @Test
    fun `perkiraan dari kecepatan 14 hari terakhir dan dari target`() {
        // 28 halaman dalam 14 hari = 2 hlm/hari; sisa 604 - 504 = 100 -> 50 hari.
        val p = perkiraanKhatam(504, 604, listOf(sesi("2026-10-05", 20), sesi("2026-10-18", 8)), hariIni, target = 5)
        assertEquals(100, p.sisaHalaman)
        assertEquals(2.0, p.lajuHarian!!, 0.0001)
        assertEquals(hariIni.plusDays(50), p.selesaiMenurutLaju)
        assertEquals(hariIni.plusDays(20), p.selesaiMenurutTarget)
    }

    @Test
    fun `sesi di luar jendela, hafalan, dan murajaah tidak dihitung`() {
        val p = perkiraanKhatam(10, 604, listOf(
            sesi("2026-10-04", 50), // 15 hari lalu
            sesi("2026-10-10", 30, ResearchSchema.Detail.HAFALAN),
            sesi("2026-10-11", 30, ResearchSchema.Detail.MURAJAAH)
        ), hariIni, target = null)
        assertNull(p.lajuHarian)
        assertNull(p.selesaiMenurutLaju)
        assertNull(p.selesaiMenurutTarget)
    }

    @Test
    fun `sudah di halaman terakhir, tidak ada perkiraan`() {
        val p = perkiraanKhatam(604, 604, listOf(sesi("2026-10-18", 4)), hariIni, target = 3)
        assertEquals(0, p.sisaHalaman)
        assertNull(p.selesaiMenurutLaju)
        assertNull(p.selesaiMenurutTarget)
    }
}
