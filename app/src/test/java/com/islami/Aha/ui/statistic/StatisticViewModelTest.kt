package com.islami.Aha.ui.statistic

import com.islami.Aha.data.model.HabitCompletionRecord
import com.islami.Aha.data.model.ResearchLogEntity
import com.islami.Aha.data.research.ResearchSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class StatisticViewModelTest {

    private fun rec(key: String, tgl: String, kategori: String = "Sholat Fardhu") =
        HabitCompletionRecord(habitKey = key, dateKey = tgl, category = kategori, source = "DEFAULT")

    private fun log(jenis: String, tgl: String, slot: String, jumlah: Int? = null, waktu: String? = null, jilid: Int? = null) =
        ResearchLogEntity(
            slotKey = slot, tanggal = tgl, jenisIbadah = jenis, namaDetail = null, namaDetailAsli = null,
            status = "dikerjakan", jumlah = jumlah, jilid = jilid, jamPencatatan = 0L, sumberPencatatan = null,
            ownerUid = null, updatedAt = 0L, offsetWaktu = "+07:00", waktuDzikir = waktu
        )

    private val nama = mapOf(
        "default_1" to "Sholat Subuh", "default_2" to "Sholat Dzuhur", "default_4" to "Sholat Maghrib",
        "sunnah_x" to "Dhuha"
    )

    @Test
    fun `default state tidak memuat persentase dan memulai di hari ini`() {
        val s = StatisticUiState()
        assertTrue(s.isLoading)
        assertTrue(s.kalender.isEmpty())
        assertNull(s.khatamJuz)
    }

    @Test
    fun `intensitas dua tingkat mengikuti separuh target harian`() {
        assertEquals(Intensitas.KOSONG, intensitas(0, 7))
        assertEquals(Intensitas.SEDIKIT, intensitas(3, 7))
        assertEquals(Intensitas.BANYAK, intensitas(4, 7))
        assertEquals(Intensitas.BANYAK, intensitas(3, 0))
        assertEquals(Intensitas.SEDIKIT, intensitas(2, 0))
    }

    @Test
    fun `kalender senin pertama dengan sel kosong, cuti, hari ini, dan tanggal nanti`() {
        // 1 Oktober 2026 = Kamis -> 3 sel kosong (Sen, Sel, Rab).
        val k = susunBulan(
            YearMonth.of(2026, 10),
            mapOf("2026-10-02" to 6, "2026-10-03" to 1),
            setOf("2026-10-12"),
            LocalDate.of(2026, 10, 15),
            targetHarian = 7
        )
        assertEquals(3, k.takeWhile { it == null }.size)
        assertEquals(3 + 31, k.size)
        val hari = k.filterNotNull().associateBy { it.hari }
        assertEquals(Intensitas.BANYAK, hari[2]!!.intensitas)
        assertEquals(Intensitas.SEDIKIT, hari[3]!!.intensitas)
        assertTrue(hari[12]!!.cuti)
        assertTrue(hari[15]!!.hariIni)
        assertFalse(hari[15]!!.nanti)
        assertTrue(hari[16]!!.nanti)
    }

    @Test
    fun `chip satu hari - sholat urut waktu, dzikir per waktu, tilawah dijumlah, iqro terakhir`() {
        val chips = susunCatatanHari(
            records = listOf(rec("default_4", "d"), rec("default_1", "d"), rec("sunnah_x", "d", "SHOLAT")),
            namaHabit = nama,
            urutanKunci = listOf("default_1", "default_2", "default_4", "sunnah_x"),
            aktivitas = listOf(
                log(ResearchSchema.Jenis.DZIKIR, "d", "a", waktu = ResearchSchema.WaktuDzikir.BADA_SHOLAT),
                log(ResearchSchema.Jenis.DZIKIR, "d", "b", waktu = ResearchSchema.WaktuDzikir.BADA_SHOLAT),
                log(ResearchSchema.Jenis.TILAWAH, "d", "c", jumlah = 2),
                log(ResearchSchema.Jenis.TILAWAH, "d", "e", jumlah = 1),
                log(ResearchSchema.Jenis.IQRO, "d", "f", jumlah = 24, jilid = 3)
            )
        )
        assertEquals(
            listOf("Subuh", "Maghrib", "Dhuha", "Dzikir setelah sholat ×2", "Tilawah 3 hlm", "Iqro jilid 3 hal. 24"),
            chips.map { it.label }
        )
        assertEquals(JenisChip.SUNNAH_PUASA, chips[2].jenis)
    }

    @Test
    fun `ringkasan pekan - tersering hanya bila tidak seri`() {
        val p = susunRingkasanPekan(
            records = listOf(
                rec("default_4", "1"), rec("default_4", "2"), rec("default_1", "2"),
                rec("sunnah_x", "2", "SHOLAT")
            ),
            namaHabit = nama,
            aktivitas = listOf(
                log(ResearchSchema.Jenis.TILAWAH, "1", "t1", jumlah = 3),
                log(ResearchSchema.Jenis.TILAWAH, "3", "t2", jumlah = 5),
                log(ResearchSchema.Jenis.DZIKIR, "1", "d1", waktu = ResearchSchema.WaktuDzikir.PAGI),
                log(ResearchSchema.Jenis.DZIKIR, "1", "d2", waktu = ResearchSchema.WaktuDzikir.PETANG)
            )
        )
        assertEquals(3, p.sholatFardhu)
        assertEquals("Maghrib", p.sholatTersering)
        assertEquals(2, p.dzikirSesi)
        assertNull(p.dzikirTersering) // pagi 1, petang 1: seri
        assertEquals(8, p.tilawahHalaman)
        assertEquals(2, p.tilawahHari)
        assertEquals(1, p.sunnahPuasa)
    }
}
