package com.islami.Aha.data.notifikasi

import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class JadwalPengingatTest {
    private val wib = ZoneId.of("Asia/Jakarta")
    // Rabu 4 November 2026, 12.00 WIB.
    private val sekarang = ZonedDateTime.of(2026, 11, 4, 12, 0, 0, 0, wib)

    private fun fardhu(id: Int, nama: String, jam: String, aktif: Boolean = true) =
        Habit(id = id, name = nama, category = "Sholat Fardhu", icon = "", description = "", time = jam, isReminderEnabled = aktif)

    private fun sunnah(id: String, nama: String, kategori: SunnahCategoryType, jam: String?, aktif: Boolean = true) =
        SunnahHabit(id = id, name = nama, category = kategori, reminderEnabled = aktif, reminderTime = jam)

    private val subuh = JadwalPengingat.Sumber("default_1", ResearchSchema.Jenis.SHOLAT_WAJIB, ResearchSchema.Detail.SUBUH, LocalTime.of(4, 20))
    private val ashar = JadwalPengingat.Sumber("default_3", ResearchSchema.Jenis.SHOLAT_WAJIB, ResearchSchema.Detail.ASHAR, LocalTime.of(15, 10))

    @Test
    fun `tepat 7 entri per pengingat, mulai hari ini bila jamnya belum lewat`() {
        val e = JadwalPengingat.susun(listOf(subuh, ashar), sekarang)
        assertEquals(14, e.size)
        val tglAshar = e.filter { it.idPengingat == "default_3" }.map { it.docId }
        assertEquals("default_3_20261104", tglAshar.first()) // 15.10 hari ini belum lewat
        assertEquals("default_3_20261110", tglAshar.last())
        val tglSubuh = e.filter { it.idPengingat == "default_1" }.map { it.docId }
        assertEquals("default_1_20261105", tglSubuh.first()) // 04.20 hari ini sudah lewat
        assertEquals("default_1_20261111", tglSubuh.last())
    }

    @Test
    fun `semua dalam jendela sekarang sampai 7 hari, urut waktu, waktu kirim UTC benar`() {
        val e = JadwalPengingat.susun(listOf(subuh, ashar), sekarang)
        val mulai = sekarang.toInstant().toEpochMilli()
        val akhir = mulai + Duration.ofDays(7).toMillis()
        assertTrue(e.all { it.waktuKirimMs in (mulai + 1)..akhir })
        assertEquals(e.sortedBy { it.waktuKirimMs }, e)
        // 15.10 WIB = 08.10 UTC.
        assertEquals(
            ZonedDateTime.of(2026, 11, 4, 8, 10, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli(),
            e.first().waktuKirimMs
        )
    }

    @Test
    fun `jam yang sama dengan sekarang tidak dikirim hari ini`() {
        val pas = subuh.copy(jam = LocalTime.of(12, 0))
        val e = JadwalPengingat.susun(listOf(pas), sekarang)
        assertEquals(7, e.size)
        assertEquals("default_1_20261105", e.first().docId)
    }

    @Test
    fun `isi dokumen hanya empat field, tanpa koordinat`() {
        val m = JadwalPengingat.susun(listOf(ashar), sekarang).first().keFirestore()
        assertEquals(setOf("waktu_kirim", "jenis_ibadah", "nama_detail", "id_pengingat"), m.keys)
        assertEquals("sholat_wajib", m["jenis_ibadah"])
        assertEquals("ashar", m["nama_detail"])
        assertEquals("default_3", m["id_pengingat"])
    }

    @Test
    fun `sumber mengikuti aturan alarm lokal`() {
        val s = JadwalPengingat.sumberDari(
            habitBawaan = listOf(
                fardhu(1, "Sholat Subuh", "04:20"),
                fardhu(2, "Sholat Dzuhur", "11:45", aktif = false),
                fardhu(3, "Sholat Ashar", "")
            ),
            habitSunnah = listOf(
                sunnah("u1", "Sholat Dhuha", SunnahCategoryType.SHOLAT, "08:00"),
                sunnah("u2", "Puasa Senin Kamis", SunnahCategoryType.PUASA, "03:30"),
                sunnah("u3", "Sholat Tahajud", SunnahCategoryType.SHOLAT, null),
                sunnah("u4", "Sholat Witir", SunnahCategoryType.SHOLAT, "21:00", aktif = false),
                sunnah("u5", "Sholat Rawatib", SunnahCategoryType.SHOLAT, "25:99")
            ),
            notifikasiGlobalAktif = true,
            modeCuti = false
        )
        assertEquals(listOf("u2", "default_1", "u1"), s.map { it.idPengingat })
        assertEquals(ResearchSchema.Detail.SENIN_KAMIS, s[0].namaDetail)
        assertEquals(ResearchSchema.Detail.DHUHA, s[2].namaDetail)
    }

    @Test
    fun `notifikasi global mati = jadwal kosong`() {
        val s = JadwalPengingat.sumberDari(listOf(fardhu(1, "Sholat Subuh", "04:20")), emptyList(), false, false)
        assertTrue(s.isEmpty())
        assertTrue(JadwalPengingat.susun(s, sekarang).isEmpty())
    }

    @Test
    fun `mode cuti melewati sholat dan puasa`() {
        val s = JadwalPengingat.sumberDari(
            listOf(fardhu(1, "Sholat Subuh", "04:20")),
            listOf(sunnah("u2", "Puasa Senin Kamis", SunnahCategoryType.PUASA, "03:30")),
            notifikasiGlobalAktif = true,
            modeCuti = true
        )
        assertTrue(s.isEmpty())
    }

    @Test
    fun `habit yang tidak bisa dipetakan tidak diunggah`() {
        val s = JadwalPengingat.sumberDari(
            listOf(Habit(id = 9, name = "Baca Buku", category = "Lainnya", icon = "", description = "", time = "20:00")),
            emptyList(), true, false
        )
        assertTrue(s.isEmpty())
    }

    @Test
    fun `sidik berubah bila jam sholat berubah`() {
        val a = JadwalPengingat.sidik(listOf(subuh, ashar))
        val b = JadwalPengingat.sidik(listOf(subuh, ashar.copy(jam = LocalTime.of(15, 11))))
        assertFalse(a == b)
    }

    @Test
    fun `perlu unggah ulang bila isi berubah atau sisa kurang dari 2 hari`() {
        val now = sekarang.toInstant().toEpochMilli()
        val hari = Duration.ofDays(1).toMillis()
        assertTrue(JadwalPengingat.perluUnggah("a", null, null, now))
        assertTrue(JadwalPengingat.perluUnggah("b", "a", now + 6 * hari, now))
        assertFalse(JadwalPengingat.perluUnggah("a", "a", now + 6 * hari, now))
        assertFalse(JadwalPengingat.perluUnggah("a", "a", now + 2 * hari, now))
        assertTrue(JadwalPengingat.perluUnggah("a", "a", now + 2 * hari - 1, now))
        assertTrue(JadwalPengingat.perluUnggah("a", "a", null, now))
        // Jadwal kosong yang sudah terkirim tidak perlu diperpanjang.
        assertFalse(JadwalPengingat.perluUnggah("", "", null, now))
    }
}
