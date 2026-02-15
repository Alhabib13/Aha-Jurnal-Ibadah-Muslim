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

    fun getHijriDateFormatted(): String {
        val hijriDate: HijrahDate = HijrahChronology.INSTANCE.dateNow()
        val day = hijriDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
        val month = hijriDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
        val year = hijriDate.get(java.time.temporal.ChronoField.YEAR)
        val monthName = hijriMonthNames[month - 1]
        return "$day $monthName $year H"
    }

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
