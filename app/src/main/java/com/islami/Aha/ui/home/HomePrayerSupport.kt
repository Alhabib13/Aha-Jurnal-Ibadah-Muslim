package com.islami.Aha.ui.home

import com.islami.Aha.data.model.Habit
import com.islami.Aha.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Satu kolom di jadwal harian Beranda. */
data class JadwalWaktu(
    val nama: String,
    /** "HH.mm", atau "--.--" bila belum diketahui. */
    val jam: String,
    /** false untuk Imsak dan Terbit (penanda waktu, bukan sholat). */
    val sholat: Boolean,
    /** Waktu yang sedang berjalan = yang terakhir sudah lewat. */
    val sekarang: Boolean
)

private fun tampilJam(h: Int, m: Int) = String.format(Locale.ROOT, "%02d.%02d", h, m)

/**
 * Imsak & Terbit dari API (disimpan saat sinkron lokasi). Tanpa data API: Imsak = Subuh − 10 menit
 * (ketentuan umum di Indonesia), Terbit tidak ditebak ("--.--").
 */
internal fun susunJadwalHarian(
    habits: List<Habit>,
    imsak: String,
    terbit: String,
    sekarangHHmm: String
): List<JadwalWaktu> {
    val sholat = parsePrayerTimes(habits)
    if (sholat.isEmpty()) return emptyList()
    val subuh = sholat.first()
    val imsakJam = parseHourMinute(imsak) ?: run {
        val total = (subuh.hour * 60 + subuh.minute - 10 + 24 * 60) % (24 * 60)
        total / 60 to total % 60
    }
    val terbitJam = parseHourMinute(terbit)
    data class Baris(val nama: String, val jam: Pair<Int, Int>?, val sholat: Boolean)
    val baris = buildList {
        add(Baris("Imsak", imsakJam, false))
        add(Baris(subuh.name, subuh.hour to subuh.minute, true))
        add(Baris("Terbit", terbitJam, false))
        sholat.drop(1).forEach { add(Baris(it.name, it.hour to it.minute, true)) }
    }
    val now = parseHourMinute(sekarangHHmm)?.let { it.first * 60 + it.second }
    val idxSekarang = if (now == null) -1 else baris.indexOfLast { b ->
        b.jam != null && b.jam.first * 60 + b.jam.second <= now
    }
    return baris.mapIndexed { i, b ->
        JadwalWaktu(
            nama = b.nama,
            jam = b.jam?.let { tampilJam(it.first, it.second) } ?: "--.--",
            sholat = b.sholat,
            sekarang = i == idxSekarang
        )
    }
}

/** Sholat berikutnya: nama, jam ("HH.mm"), dan sisa menit. */
internal fun sholatBerikutnya(now: Calendar, habits: List<Habit>): Triple<String, String, Int>? {
    val sholat = parsePrayerTimes(habits)
    if (sholat.isEmpty()) return null
    val menit = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val berikut = sholat.firstOrNull { it.hour * 60 + it.minute >= menit }
    return if (berikut != null) {
        Triple(berikut.name, tampilJam(berikut.hour, berikut.minute), berikut.hour * 60 + berikut.minute - menit)
    } else {
        val besok = sholat.first()
        Triple(besok.name, tampilJam(besok.hour, besok.minute), 24 * 60 - menit + besok.hour * 60 + besok.minute)
    }
}

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
