package com.islami.Aha.util

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.location.Geocoder
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val cityName: String
)

object LocationHelper {

    private const val TAG = "LocationHelper"
    private const val KEY_LAST_LAT = "last_location_lat"
    private const val KEY_LAST_LON = "last_location_lon"
    private const val KEY_LAST_CITY = "last_location_city"
    private const val CURRENT_LOCATION_TIMEOUT_MS = 3500L

    private fun debugLog(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message)
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return hasFine || hasCoarse
    }

    fun getLastLocation(
        context: Context,
        onResult: (LocationResult) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "Location permission not granted")
            onError("Izin lokasi belum diberikan")
            return
        }
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            if (!isLocationEnabled(context)) {
                Log.w(TAG, "Location service is disabled, fallback to last known or cached location")
                emitLastKnownOrCached(
                    context = context,
                    onResult = onResult,
                    onError = onError,
                    errorMessage = "Layanan lokasi nonaktif dan lokasi terakhir belum tersedia"
                )
                return
            }

            val callbackDelivered = AtomicBoolean(false)
            val cancellationTokenSource = CancellationTokenSource()
            val timeoutHandler = Handler(Looper.getMainLooper())

            fun emitResultSafely(result: LocationResult) {
                if (callbackDelivered.compareAndSet(false, true)) {
                    onResult(result)
                }
            }

            fun emitErrorSafely(message: String) {
                if (callbackDelivered.compareAndSet(false, true)) {
                    onError(message)
                }
            }

            fun fallbackToLastKnownOrCache(errorMessage: String) {
                emitLastKnownOrCached(
                    context = context,
                    onResult = { emitResultSafely(it) },
                    onError = { emitErrorSafely(it) },
                    errorMessage = errorMessage
                )
            }

            val timeoutRunnable = Runnable {
                if (callbackDelivered.get()) return@Runnable
                cancellationTokenSource.cancel()
                Log.w(TAG, "Current location request timed out, falling back to last known/cached")
                fallbackToLastKnownOrCache("Lokasi sedang lambat, memakai lokasi terakhir")
            }
            timeoutHandler.postDelayed(timeoutRunnable, CURRENT_LOCATION_TIMEOUT_MS)

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                timeoutHandler.removeCallbacks(timeoutRunnable)
                if (callbackDelivered.get()) return@addOnSuccessListener
                if (location != null) {
                    val result = resolveLocationResult(context, location.latitude, location.longitude)
                    debugLog("Current location: lat=${result.latitude}, lon=${result.longitude}, city=${result.cityName}")
                    emitResultSafely(result)
                } else {
                    fallbackToLastKnownOrCache("Lokasi tidak tersedia")
                }
            }.addOnFailureListener { e ->
                timeoutHandler.removeCallbacks(timeoutRunnable)
                if (callbackDelivered.get()) return@addOnFailureListener
                Log.e(TAG, "Failed to get current location", e)
                fallbackToLastKnownOrCache("Gagal mendapatkan lokasi")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting location", e)
            onError("Izin lokasi belum diberikan")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error getting location", e)
            emitCachedOrError(
                context = context,
                onResult = onResult,
                onError = onError,
                errorMessage = "Layanan lokasi tidak tersedia"
            )
        }
    }

    fun getCachedLocationQuick(context: Context): LocationResult? = getCachedLocation(context)

    fun requestEnableLocationFromApp(
        context: Context,
        onResolvable: (IntentSender) -> Unit,
        onAlreadyEnabled: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (isLocationEnabled(context)) {
            onAlreadyEnabled()
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L).build()
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(request)
            .setAlwaysShow(true)
            .build()

        LocationServices.getSettingsClient(context)
            .checkLocationSettings(settingsRequest)
            .addOnSuccessListener {
                onAlreadyEnabled()
            }
            .addOnFailureListener { error ->
                val resolvable = error as? ResolvableApiException
                if (resolvable != null) {
                    onResolvable(resolvable.resolution.intentSender)
                } else {
                    Log.e(TAG, "Unable to resolve location settings dialog", error)
                    onFailure("Prompt aktifkan lokasi tidak tersedia")
                }
            }
    }

    fun isLocationEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return runCatching {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    private fun emitLastKnownOrCached(
        context: Context,
        onResult: (LocationResult) -> Unit,
        onError: (String) -> Unit,
        errorMessage: String
    ) {
        if (!hasLocationPermission(context)) {
            onError("Izin lokasi belum diberikan")
            return
        }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        @Suppress("MissingPermission")
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val result = resolveLocationResult(context, location.latitude, location.longitude)
                    debugLog("Last known location: lat=${result.latitude}, lon=${result.longitude}, city=${result.cityName}")
                    onResult(result)
                } else {
                    emitCachedOrError(context, onResult, onError, errorMessage)
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to get last known location", error)
                emitCachedOrError(context, onResult, onError, errorMessage)
            }
    }

    private fun emitCachedOrError(
        context: Context,
        onResult: (LocationResult) -> Unit,
        onError: (String) -> Unit,
        errorMessage: String
    ) {
        val cached = getCachedLocation(context)
        if (cached != null) {
            debugLog("Using cached location: lat=${cached.latitude}, lon=${cached.longitude}, city=${cached.cityName}")
            onResult(cached)
        } else {
            onError(errorMessage)
        }
    }

    private fun resolveLocationResult(context: Context, lat: Double, lon: Double): LocationResult {
        val cityName = getCityName(context, lat, lon)
        val result = LocationResult(lat, lon, cityName)
        cacheLocation(context, result)
        return result
    }

    private fun cacheLocation(context: Context, result: LocationResult) {
        SecurePrefsProvider.get(context).edit()
            .putString(KEY_LAST_LAT, result.latitude.toString())
            .putString(KEY_LAST_LON, result.longitude.toString())
            .putString(KEY_LAST_CITY, result.cityName)
            .apply()
    }

    private fun getCachedLocation(context: Context): LocationResult? {
        val prefs = SecurePrefsProvider.get(context)
        val lat = prefs.getString(KEY_LAST_LAT, null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString(KEY_LAST_LON, null)?.toDoubleOrNull() ?: return null
        val city = prefs.getString(KEY_LAST_CITY, null)?.takeIf { it.isNotBlank() } ?: "Lokasi Terakhir"
        return LocationResult(lat, lon, city)
    }

    private fun getCityName(context: Context, lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val address = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()

            val district = address?.subLocality?.trim().orEmpty().takeIf { it.isNotBlank() }
            val regency = address?.subAdminArea?.trim().orEmpty().takeIf { it.isNotBlank() }
            val city = address?.locality?.trim().orEmpty().takeIf { it.isNotBlank() }
            val province = address?.adminArea?.trim().orEmpty().takeIf { it.isNotBlank() }

            val result = when {
                district != null && regency != null -> "$district, $regency"
                district != null && city != null -> "$district, $city"
                regency != null -> regency
                city != null -> city
                province != null -> province
                else -> "Lokasi Tidak Dikenal"
            }
            debugLog("Geocoded: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding failed", e)
            "Lokasi Tidak Dikenal"
        }
    }
}
