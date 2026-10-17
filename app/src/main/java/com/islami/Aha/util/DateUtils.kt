package com.islami.Aha.util

import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {

    private val hijriMonthNames = arrayOf(
        "Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir",
        "Jumadil Awal", "Jumadil Akhir", "Rajab", "Sya'ban",
        "Ramadhan", "Syawal", "Dzulqa'dah", "Dzulhijjah"
    )

    /** "3 Jumadil Awal 1448 H" untuk tanggal Masehi tertentu; "" di luar kalender Hijriah. */
    fun hijriFormatted(tanggal: LocalDate): String = runCatching {
        val h = HijrahDate.from(tanggal)
        val bulan = hijriMonthNames[h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR) - 1]
        "${h.get(java.time.temporal.ChronoField.DAY_OF_MONTH)} $bulan ${h.get(java.time.temporal.ChronoField.YEAR)} H"
    }.getOrDefault("")

    fun getHijriDateFormatted(): String {
        val hijriDate: HijrahDate = HijrahChronology.INSTANCE.dateNow()
        val day = hijriDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
        val month = hijriDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
        val year = hijriDate.get(java.time.temporal.ChronoField.YEAR)
        val monthName = hijriMonthNames[month - 1]
        return "$day $monthName $year H"
    }

    /** "Rabiul Akhir – Jumadil Awal 1448" untuk rentang tanggal Masehi; "" bila di luar kalender Hijriah. */
    fun hijriRentang(awal: LocalDate, akhir: LocalDate): String = runCatching {
        val a = HijrahDate.from(awal)
        val b = HijrahDate.from(akhir)
        fun bulan(h: HijrahDate) = hijriMonthNames[h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR) - 1]
        fun tahun(h: HijrahDate) = h.get(java.time.temporal.ChronoField.YEAR)
        when {
            bulan(a) == bulan(b) && tahun(a) == tahun(b) -> "${bulan(a)} ${tahun(a)}"
            tahun(a) == tahun(b) -> "${bulan(a)} – ${bulan(b)} ${tahun(b)}"
            else -> "${bulan(a)} ${tahun(a)} – ${bulan(b)} ${tahun(b)}"
        }
    }.getOrDefault("")

    /**
     * Tanggal Masehi 1 Ramadhan berikutnya (Umm al-Qura) beserta tahun Hijriahnya, atau null
     * di luar jangkauan kalender. Hanya perkiraan: penetapan di Indonesia bisa selisih sehari.
     */
    fun awalRamadanBerikutnya(dari: LocalDate = LocalDate.now()): Pair<LocalDate, Int>? = runCatching {
        val h = HijrahDate.from(dari)
        val tahun = h.get(java.time.temporal.ChronoField.YEAR)
        val bulan = h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
        val tahunRamadan = if (bulan < 9) tahun else tahun + 1
        LocalDate.from(HijrahChronology.INSTANCE.date(tahunRamadan, 9, 1)) to tahunRamadan
    }.getOrNull()

    fun getTodayKey(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
        return LocalDate.now().format(formatter)
    }

    fun isToday(dateKey: String?): Boolean {
        if (dateKey.isNullOrBlank()) return false
        return dateKey == getTodayKey()
    }

    fun isRamadanMonth(): Boolean {
        val hijriDate: HijrahDate = HijrahChronology.INSTANCE.dateNow()
        val month = hijriDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
        return month == 9
    }

    fun getDateKeysLastDays(days: Int): List<String> {
        if (days <= 0) return emptyList()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
        val today = LocalDate.now()
        return (0 until days).map { offset ->
            today.minus(offset.toLong(), ChronoUnit.DAYS).format(formatter)
        }.reversed()
    }
}
