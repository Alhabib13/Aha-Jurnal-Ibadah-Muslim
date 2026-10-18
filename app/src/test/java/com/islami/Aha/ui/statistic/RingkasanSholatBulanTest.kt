package com.islami.Aha.ui.statistic

import com.islami.Aha.data.model.HabitCompletionRecord
import com.islami.Aha.data.model.ResearchLogEntity
import com.islami.Aha.data.research.ResearchSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class RingkasanSholatBulanTest {
    private val okt = YearMonth.of(2026, 10)
    private val hariIni = LocalDate.of(2026, 10, 4)

    private fun fardhu(tgl: String, n: Int) = (1..n).map {
        HabitCompletionRecord(habitKey = "default_$it", dateKey = tgl, category = "Sholat Fardhu", source = "DEFAULT")
    }

    private fun log(tgl: String, status: String, selisih: Int? = null, detail: String = "subuh") = ResearchLogEntity(
        slotKey = "${tgl}_sholat_wajib_$detail", tanggal = tgl, jenisIbadah = "sholat_wajib", namaDetail = detail,
        namaDetailAsli = null, status = status, jumlah = null, jilid = null, jamPencatatan = 1L,
        sumberPencatatan = null, ownerUid = null, updatedAt = 1L, offsetWaktu = "+07:00",
        selisihMenitDariAdzan = selisih
    )

    @Test
    fun `target tidak menghitung hari mode cuti dan hari yang belum tiba`() {
        val r = susunRingkasanSholatBulan(
            okt, hariIni,
            records = fardhu("2026-10-01", 5) + fardhu("2026-10-02", 3),
            logSholat = emptyList(),
            tanggalCuti = setOf("2026-10-03"),
            jumlahFardhu = 5
        )
        assertEquals(8, r.tercatat)
        assertEquals(15, r.target) // 1, 2, 4 Oktober; tanggal 3 = cuti; 5-31 belum tiba
        assertEquals(1, r.hariCuti)
        assertEquals(31, r.harian.size)
        assertEquals(5, r.harian[0].jumlah)
        assertEquals(true, r.harian[2].cuti)
        assertEquals(true, r.harian[4].nanti)
    }

    @Test
    fun `sunnah tidak ikut, catatan ganda tidak dihitung dua kali`() {
        val sunnah = HabitCompletionRecord(habitKey = "sunnah_x", dateKey = "2026-10-01", category = "SHOLAT", source = "SUNNAH")
        val r = susunRingkasanSholatBulan(okt, hariIni, fardhu("2026-10-01", 2) + fardhu("2026-10-01", 2) + sunnah,
            emptyList(), emptySet(), 5)
        assertEquals(2, r.tercatat)
    }

    @Test
    fun `tertinggal dihitung dari status tidak, jarak adzan = median pencatatan dikerjakan`() {
        val r = susunRingkasanSholatBulan(okt, hariIni, emptyList(), listOf(
            log("2026-10-01", ResearchSchema.Status.TIDAK),
            log("2026-10-02", ResearchSchema.Status.DIKERJAKAN, 5, "subuh"),
            log("2026-10-02", ResearchSchema.Status.DIKERJAKAN, 40, "dzuhur"),
            log("2026-10-02", ResearchSchema.Status.DIKERJAKAN, 12, "ashar"),
            log("2026-10-03", ResearchSchema.Status.DIKERJAKAN, null, "isya")
        ), emptySet(), 5)
        assertEquals(1, r.ditandaiTertinggal)
        assertEquals(12, r.medianMenitAdzan)
    }

    @Test
    fun `hari sebelum mulai mencatat tidak ikut target`() {
        val r = susunRingkasanSholatBulan(okt, hariIni, fardhu("2026-10-03", 5), emptyList(), emptySet(), 5,
            mulaiMencatat = LocalDate.of(2026, 10, 3))
        assertEquals(10, r.target) // 3 & 4 Oktober
        assertEquals(true, r.harian[0].sebelumMulai)
    }

    @Test
    fun `tanpa data jarak adzan kosong`() {
        assertNull(susunRingkasanSholatBulan(okt, hariIni, emptyList(), emptyList(), emptySet(), 5).medianMenitAdzan)
    }
}
