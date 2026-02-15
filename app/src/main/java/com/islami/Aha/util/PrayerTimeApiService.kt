package com.islami.Aha.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object PrayerTimeApiService {
    private const val TAG = "PrayerTimeApiService"
    private const val BASE_URL = "https://api.aladhan.com/v1/timings"
    private const val METHOD_ID = 11 // Kemenag-like regional method for Indonesia
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT)

    data class FardhuTimes(
        val subuh: String,
        val dzuhur: String,
        val ashar: String,
        val maghrib: String,
        val isya: String
    )

    fun fetchTodayFardhuTimes(latitude: Double, longitude: Double): FardhuTimes? {
        val date = LocalDate.now().format(DATE_FORMATTER)
        val url =
            "$BASE_URL/$date?latitude=$latitude&longitude=$longitude&method=$METHOD_ID"
        val connection = (URL(url).openConnection() as? HttpURLConnection) ?: return null
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            if (connection.responseCode !in 200..299) return null
            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(payload)
            val timings = root.optJSONObject("data")?.optJSONObject("timings") ?: return null

            FardhuTimes(
                subuh = normalize(timings.optString("Fajr")),
                dzuhur = normalize(timings.optString("Dhuhr")),
                ashar = normalize(timings.optString("Asr")),
                maghrib = normalize(timings.optString("Maghrib")),
                isya = normalize(timings.optString("Isha"))
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch prayer times", e)
            runCatching { FirebaseCrashlytics.getInstance().recordException(e) }
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun normalize(value: String): String {
        return value.substringBefore(" ").trim()
    }
}
