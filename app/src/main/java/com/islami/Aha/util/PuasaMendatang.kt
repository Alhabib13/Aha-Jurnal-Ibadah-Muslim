package com.islami.Aha.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

/**
 * Jadwal puasa sunnah berikutnya, dihitung dari kalender Hijriah Umm al-Qura (sama dengan yang
 * dipakai Beranda). Hanya PERKIRAAN: penetapan awal bulan di Indonesia bisa selisih sehari.
 * Murni tampilan; tidak menulis data apa pun.
 */
object PuasaMendatang {

    /**
     * Satu baris daftar. [namaHabit] = nama preset Add Habit bila puasa ini bisa langsung dijadikan
     * habit (supaya pemetaan ke nama_detail sama), null bila tidak ada presetnya.
     */
    data class Entri(
        val nama: String,
        val tanggal: LocalDate,
        val keteranganHijri: String,
        val hariLagi: Long,
        val namaHabit: String? = null,
        val frekuensiHabit: String = "Setiap hari"
    )

    private const val DZULHIJJAH = 12
    private const val RAMADHAN = 9

    fun hitung(hariIni: LocalDate = LocalDate.now()): List<Entri> {
        val daftar = mutableListOf<Entri>()
        seninKamis(hariIni)?.let(daftar::add)
        ayyamulBidh(hariIni)?.let(daftar::add)
        tanggalHijriBerikutnya(hariIni, 10, 2)?.let {
            daftar += entri("Puasa Syawal", it, hariIni, namaHabit = "Puasa Syawal", catatan = "6 hari di bulan Syawal")
        }
        tanggalHijriBerikutnya(hariIni, DZULHIJJAH, 9)?.let { daftar += entri("Puasa Arafah", it, hariIni) }
        tanggalHijriBerikutnya(hariIni, 1, 10)?.let { daftar += entri("Puasa Asyura", it, hariIni) }
        // Selama Ramadhan kartu Ramadan sudah tampil; yang ditunjukkan Ramadhan berikutnya.
        tanggalHijriBerikutnya(hariIni, RAMADHAN, 1, lewatiBulanIni = true)?.let { daftar += entri("Ramadhan", it, hariIni) }
        return daftar.sortedBy { it.tanggal }
    }

    /**
     * Nama puasa sunnah pada [tanggal] untuk penanda kalender, atau null. Ramadhan (wajib),
     * 1 Syawal, dan 10–13 Dzulhijjah (hari raya & tasyrik) tidak pernah ditandai.
     */
    fun puasaSunnahPada(tanggal: LocalDate): String? {
        val h = runCatching { HijrahDate.from(tanggal) }.getOrNull()
        val bulan = h?.get(ChronoField.MONTH_OF_YEAR)
        val hari = h?.get(ChronoField.DAY_OF_MONTH)
        if (bulan == RAMADHAN) return null
        if (bulan == 10 && hari == 1) return null
        if (bulan == DZULHIJJAH && hari != null && hari in 10..13) return null
        return when {
            bulan == DZULHIJJAH && hari == 9 -> "Puasa Arafah"
            bulan == 1 && hari == 10 -> "Puasa Asyura"
            hari != null && hari in 13..15 -> "Ayyamul Bidh"
            tanggal.dayOfWeek == DayOfWeek.MONDAY || tanggal.dayOfWeek == DayOfWeek.THURSDAY -> "Senin Kamis"
            else -> null
        }
    }

    private fun seninKamis(hariIni: LocalDate): Entri? {
        val tgl = (0L..6L).map { hariIni.plusDays(it) }
            .firstOrNull { it.dayOfWeek == DayOfWeek.MONDAY || it.dayOfWeek == DayOfWeek.THURSDAY } ?: return null
        val hari = if (tgl.dayOfWeek == DayOfWeek.MONDAY) "Senin" else "Kamis"
        return entri("Senin Kamis", tgl, hariIni, namaHabit = "Senin Kamis",
            frekuensi = "Hari: Sen • Kam", catatan = hari)
    }

    /** Tanggal 13–15 Hijriah terdekat. 13 Dzulhijjah dilewati (hari tasyrik); Ramadhan dilewati. */
    private fun ayyamulBidh(hariIni: LocalDate): Entri? = runCatching {
        var bulanIni = HijrahDate.from(hariIni)
        repeat(3) {
            val tahun = bulanIni.get(ChronoField.YEAR)
            val bulan = bulanIni.get(ChronoField.MONTH_OF_YEAR)
            if (bulan != RAMADHAN) {
                val hariMulai = if (bulan == DZULHIJJAH) 14 else 13
                val kandidat = (hariMulai..15)
                    .map { LocalDate.from(HijrahChronology.INSTANCE.date(tahun, bulan, it)) }
                    .firstOrNull { !it.isBefore(hariIni) }
                if (kandidat != null) {
                    return@runCatching entri("Ayyamul Bidh", kandidat, hariIni, namaHabit = "Ayyamul Bidh",
                        catatan = if (bulan == DZULHIJJAH) "14–15 (13 = hari tasyrik)" else "13–15 Hijriah")
                }
            }
            bulanIni = bulanIni.plus(1, ChronoUnit.MONTHS).with(ChronoField.DAY_OF_MONTH, 1)
        }
        null
    }.getOrNull()

    /** Tanggal Masehi berikutnya (>= hari ini) untuk [hari] [bulan] Hijriah. */
    private fun tanggalHijriBerikutnya(
        hariIni: LocalDate,
        bulan: Int,
        hari: Int,
        lewatiBulanIni: Boolean = false
    ): LocalDate? = runCatching {
        val h = HijrahDate.from(hariIni)
        val tahun = h.get(ChronoField.YEAR)
        val sedangDiBulanItu = h.get(ChronoField.MONTH_OF_YEAR) == bulan
        val tahunKandidat = if (lewatiBulanIni && sedangDiBulanItu) listOf(tahun + 1) else listOf(tahun, tahun + 1)
        tahunKandidat
            .map { LocalDate.from(HijrahChronology.INSTANCE.date(it, bulan, hari)) }
            .firstOrNull { !it.isBefore(hariIni) }
    }.getOrNull()

    private fun entri(
        nama: String,
        tanggal: LocalDate,
        hariIni: LocalDate,
        namaHabit: String? = null,
        frekuensi: String = "Setiap hari",
        catatan: String? = null
    ): Entri {
        val hijri = DateUtils.hijriFormatted(tanggal).removeSuffix(" H")
        return Entri(
            nama = nama,
            tanggal = tanggal,
            keteranganHijri = listOfNotNull(catatan, hijri.takeIf { it.isNotBlank() }).joinToString(" · "),
            hariLagi = ChronoUnit.DAYS.between(hariIni, tanggal),
            namaHabit = namaHabit,
            frekuensiHabit = frekuensi
        )
    }
}
