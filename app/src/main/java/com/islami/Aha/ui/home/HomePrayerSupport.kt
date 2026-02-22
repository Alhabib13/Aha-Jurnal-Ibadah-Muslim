package com.islami.Aha.ui.home

import com.islami.Aha.data.model.Habit
import com.islami.Aha.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal data class PrayerTimeInfo(
    val name: String,
    val hour: Int,
    val minute: Int
)

internal fun calculateNextPrayerInfo(
    now: Calendar,
    habits: List<Habit>
): Triple<String, String, Float> {
    val prayerTimes = parsePrayerTimes(habits)
    if (prayerTimes.isEmpty()) return Triple("", "", 0f)
    val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

    for (i in prayerTimes.indices) {
        val prayerMinutes = prayerTimes[i].hour * 60 + prayerTimes[i].minute
        if (currentMinutes <= prayerMinutes) {
            val remaining = prayerMinutes - currentMinutes
            val timeText = formatRemainingTime(remaining)

            val prevPrayerMinutes = if (i > 0) {
                prayerTimes[i - 1].hour * 60 + prayerTimes[i - 1].minute
            } else {
                (prayerTimes.last().hour * 60 + prayerTimes.last().minute) - (24 * 60)
            }
            val totalInterval = prayerMinutes - prevPrayerMinutes
            val elapsed = currentMinutes - prevPrayerMinutes
            val progress = if (totalInterval > 0) elapsed.toFloat() / totalInterval else 0f

            return Triple(prayerTimes[i].name, timeText, progress.coerceIn(0f, 1f))
        }
    }

    // All prayers have passed, next is the first prayer tomorrow.
    val firstPrayerTomorrow = prayerTimes.first()
    val firstPrayerMinutes = firstPrayerTomorrow.hour * 60 + firstPrayerTomorrow.minute
    val lastPrayerMinutes = prayerTimes.last().hour * 60 + prayerTimes.last().minute
    val remaining = (24 * 60 - currentMinutes) + firstPrayerMinutes
    val totalInterval = (24 * 60 - lastPrayerMinutes) + firstPrayerMinutes
    val elapsed = currentMinutes - lastPrayerMinutes
    val progress = if (totalInterval > 0) elapsed.toFloat() / totalInterval else 0f
    return Triple(
        firstPrayerTomorrow.name,
        formatRemainingTime(remaining),
        progress.coerceIn(0f, 1f)
    )
}

internal fun parsePrayerTimes(habits: List<Habit>): List<PrayerTimeInfo> {
    val canonicalOrder = listOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
    val fardhuHabits = habits.filter {
        it.category.equals("Sholat Fardhu", ignoreCase = true) && it.time.contains(":")
    }

    val detectedByName = fardhuHabits.mapNotNull { habit ->
        val (hour, minute) = parseHourMinute(habit.time) ?: return@mapNotNull null
        val prayerName = detectPrayerName(habit.name) ?: return@mapNotNull null
        PrayerTimeInfo(prayerName, hour, minute)
    }.associateBy { it.name }

    val orderedFromName = canonicalOrder.mapNotNull { detectedByName[it] }
    if (orderedFromName.size == canonicalOrder.size) {
        return orderedFromName
    }

    // Fallback: use chronological order from fardhu times even if names vary.
    val byTime = fardhuHabits.mapNotNull { habit ->
        val (hour, minute) = parseHourMinute(habit.time) ?: return@mapNotNull null
        hour to minute
    }.sortedBy { it.first * 60 + it.second }

    if (byTime.size < canonicalOrder.size) return emptyList()

    return canonicalOrder.mapIndexed { index, prayerName ->
        val (hour, minute) = byTime[index]
        PrayerTimeInfo(prayerName, hour, minute)
    }
}

internal fun getCurrentTimeFormatted(): String {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    return String.format(Locale.ROOT, "%02d:%02d", hour, minute)
}

internal fun getGregorianDateFormatted(): String {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
    return dateFormat.format(Date())
}

internal fun getHijriDateFormatted(): String = DateUtils.getHijriDateFormatted()

private fun formatRemainingTime(totalMinutes: Int): String {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60
    return if (hours > 0) "$hours jam $minutes menit lagi" else "$minutes menit lagi"
}

internal fun parseHourMinute(raw: String?): Pair<Int, Int>? {
    if (raw.isNullOrBlank()) return null
    val parts = raw.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour to minute
}

private fun detectPrayerName(rawName: String): String? {
    val normalized = rawName
        .lowercase(Locale.ROOT)
        .replace("sholat", "")
        .replace("salat", "")
        .trim()
    return when {
        normalized.contains("subuh") || normalized.contains("fajr") -> "Subuh"
        normalized.contains("dzuhur") || normalized.contains("zuhur") || normalized.contains("dhuhr") -> "Dzuhur"
        normalized.contains("ashar") || normalized.contains("asar") -> "Ashar"
        normalized.contains("maghrib") || normalized.contains("magrib") -> "Maghrib"
        normalized.contains("isya") || normalized.contains("isha") -> "Isya"
        else -> null
    }
}
