package com.islami.Aha.ui.profile

import com.islami.Aha.data.model.HabitCompletionRecord
import com.islami.Aha.data.model.ResearchLogEntity
import com.islami.Aha.data.research.ResearchSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JejakIbadahTest {

    private fun rec(key: String, tgl: String, kategori: String = "Sholat Fardhu") =
        HabitCompletionRecord(habitKey = key, dateKey = tgl, category = kategori, source = "DEFAULT")

    private fun log(
        jenis: String, tgl: String, slot: String, jam: Long = 0L, waktu: String? = null,
        jilid: Int? = null, detail: String? = null, juz: Int? = null
    ) = ResearchLogEntity(
        slotKey = slot, tanggal = tgl, jenisIbadah = jenis, namaDetail = detail, namaDetailAsli = null,
        status = "dikerjakan", jumlah = 1, jilid = jilid, jamPencatatan = jam, sumberPencatatan = null,
        ownerUid = null, updatedAt = 0L, offsetWaktu = "+07:00", juz = juz, waktuDzikir = waktu
    )

    private val nama = mapOf("default_1" to "Sholat Subuh", "sunnah_d" to "Dhuha")

    @Test
    fun `kosong tidak menghasilkan jejak`() {
        assertTrue(susunJejak(emptyList(), nama, emptyList(), tahunIni = 2026).isEmpty())
    }

    @Test
    fun `momen pertama, dzikir, iqro naik jilid, juz baru - terbaru di atas`() {
        val j = susunJejak(
            records = listOf(rec("default_1", "2026-10-09"), rec("sunnah_d", "2026-10-11", "SHOLAT")),
            namaHabit = nama,
            aktivitas = listOf(
                log(ResearchSchema.Jenis.DZIKIR, "2026-10-12", "2026-10-12_dzikir_x_h1_1234567890123", waktu = ResearchSchema.WaktuDzikir.BADA_SHOLAT),
                log(ResearchSchema.Jenis.IQRO, "2026-10-10", "i1", jilid = 2),
                log(ResearchSchema.Jenis.IQRO, "2026-10-13", "i2", jilid = 3),
                log(ResearchSchema.Jenis.TILAWAH, "2026-10-10", "t1", detail = ResearchSchema.Detail.BACAAN_HARIAN, juz = 1),
                log(ResearchSchema.Jenis.TILAWAH, "2026-10-14", "t2", detail = ResearchSchema.Detail.BACAAN_HARIAN, juz = 2)
            ),
            tahunIni = 2026
        )
        val judul = j.map { it.judul }
        // 9–14 Okt berturut: rekor dan juz 2 sama-sama jatuh di 14 Okt.
        assertEquals(setOf("6 hari berturut", "Sampai di juz 2"), judul.take(2).toSet())
        assertTrue("Naik ke jilid 3" in judul)
        assertTrue("Mulai belajar Iqro" in judul)
        assertTrue("Mulai mencatat dzikir" in judul)
        assertTrue("Sholat sunnah pertama dicatat" in judul)
        assertEquals("Catatan ibadah pertama", judul.last())
        assertEquals("Sholat Subuh · 9 Okt", j.last().keterangan)
        assertEquals("Dzikir setelah sholat · 12 Okt", j.first { it.jenis == JenisJejak.DZIKIR }.keterangan)
        // Tanggal menurun.
        assertEquals(j.map { it.tanggal }.sortedDescending(), j.map { it.tanggal })
    }

    @Test
    fun `catatan ke-10 memakai satuan yang sama dengan total dicatat`() {
        // 8 sholat + dzikir 3 sesi dari 1 habit di hari yang sama (= 1 satuan) + tilawah 2 sesi sehari (= 1).
        val recs = (1..8).map { rec("default_$it", "2026-10-0${if (it <= 4) 1 else 2}") }
        val akt = listOf(
            log(ResearchSchema.Jenis.DZIKIR, "2026-10-03", "2026-10-03_dzikir_x_h1_1000000000001"),
            log(ResearchSchema.Jenis.DZIKIR, "2026-10-03", "2026-10-03_dzikir_x_h1_1000000000002"),
            log(ResearchSchema.Jenis.TILAWAH, "2026-10-04", "a"),
            log(ResearchSchema.Jenis.TILAWAH, "2026-10-04", "b")
        )
        assertEquals(10, tanggalSatuanCatatan(recs, akt).size)
        val ke10 = susunJejak(recs, emptyMap(), akt, tahunIni = 2026).first { it.jenis == JenisJejak.KELIPATAN }
        assertEquals("Catatan ke-10", ke10.judul)
        assertEquals("2026-10-04", ke10.tanggal)
    }

    @Test
    fun `rekor berturut menyambung lewat hari Mode Cuti tanpa menghitungnya`() {
        val aktif = setOf("2026-10-01", "2026-10-02", "2026-10-05", "2026-10-06", "2026-10-09")
        assertEquals(4 to "2026-10-06", rekorBerturut(aktif, setOf("2026-10-03", "2026-10-04")))
        assertEquals(2 to "2026-10-02", rekorBerturut(aktif, emptySet()))
        assertNull(rekorBerturut(emptySet(), emptySet()))
    }
}
