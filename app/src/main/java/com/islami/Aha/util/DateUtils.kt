package com.islami.Aha.util

import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate

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
}
