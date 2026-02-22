package com.islami.Aha.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.io.IOException
import java.util.Locale

object PrayerTimeApiService {
    private const val TAG = "PrayerTimeApiService"
    private const val BASE_URL = "https://api.aladhan.com/v1/timings"
    private const val METHOD_ID = 11 // Kemenag-like regional method for Indonesia
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT)

    data class FardhuTimes(
        val imsak: String,
        val subuh: String,
        val dzuhur: String,
        val ashar: String,
        val maghrib: String,
        val isya: String
    )

    sealed class FetchResult {
        data class Success(val times: FardhuTimes) : FetchResult()
        data class Failure(val userMessage: String) : FetchResult()
    }

    fun fetchTodayFardhuTimes(latitude: Double, longitude: Double): FardhuTimes? {
        return when (val result = fetchTodayFardhuTimesResult(latitude, longitude)) {
            is FetchResult.Success -> result.times
            is FetchResult.Failure -> null
        }
    }

    fun fetchTodayFardhuTimesResult(latitude: Double, longitude: Double): FetchResult {
        val date = LocalDate.now().format(DATE_FORMATTER)
        val url =
            "$BASE_URL/$date?latitude=$latitude&longitude=$longitude&method=$METHOD_ID"
        val connection = (URL(url).openConnection() as? HttpURLConnection)
            ?: return FetchResult.Failure("Sinkron jadwal gagal: koneksi API tidak tersedia.")
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return FetchResult.Failure(
                    "Sinkron jadwal gagal: server API merespons kode $responseCode."
                )
            }
            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(payload)
            val timings = root.optJSONObject("data")?.optJSONObject("timings")
                ?: return FetchResult.Failure(
                    "Sinkron jadwal gagal: format data API tidak valid."
                )

            val times = FardhuTimes(
                imsak = normalize(timings.optString("Imsak").ifBlank { timings.optString("Fajr") }),
                subuh = normalize(timings.optString("Fajr")),
                dzuhur = normalize(timings.optString("Dhuhr")),
                ashar = normalize(timings.optString("Asr")),
                maghrib = normalize(timings.optString("Maghrib")),
                isya = normalize(timings.optString("Isha"))
            )
            if (
                times.subuh.isBlank() ||
                times.dzuhur.isBlank() ||
                times.ashar.isBlank() ||
                times.maghrib.isBlank() ||
                times.isya.isBlank()
            ) {
                return FetchResult.Failure(
                    "Sinkron jadwal gagal: data waktu sholat dari API belum lengkap."
                )
            }
            FetchResult.Success(times)
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timed out fetching prayer times", e)
            runCatching { FirebaseCrashlytics.getInstance().recordException(e) }
            FetchResult.Failure("Sinkron jadwal gagal: API timeout, coba lagi.")
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Host resolution failed", e)
            runCatching { FirebaseCrashlytics.getInstance().recordException(e) }
            FetchResult.Failure("Sinkron jadwal gagal: internet tidak tersedia.")
        } catch (e: IOException) {
            Log.e(TAG, "I/O failure fetching prayer times", e)
            runCatching { FirebaseCrashlytics.getInstance().recordException(e) }
            FetchResult.Failure("Sinkron jadwal gagal: koneksi jaringan bermasalah.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch prayer times", e)
            runCatching { FirebaseCrashlytics.getInstance().recordException(e) }
            FetchResult.Failure("Sinkron jadwal gagal: terjadi kendala pada layanan jadwal.")
        } finally {
            connection.disconnect()
        }
    }

    private fun normalize(value: String): String {
        return value.substringBefore(" ").trim()
    }
}
