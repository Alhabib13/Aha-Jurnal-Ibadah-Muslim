package com.islami.Aha.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val cityName: String
)

object LocationHelper {

    private const val TAG = "LocationHelper"

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
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    val cityName = getCityName(context, location.latitude, location.longitude)
                    Log.d(TAG, "Current location: lat=${location.latitude}, lon=${location.longitude}, city=$cityName")
                    onResult(LocationResult(location.latitude, location.longitude, cityName))
                } else {
                    // Fallback to last known location
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        if (lastLocation != null) {
                            val cityName = getCityName(context, lastLocation.latitude, lastLocation.longitude)
                            Log.d(TAG, "Last known location: lat=${lastLocation.latitude}, lon=${lastLocation.longitude}, city=$cityName")
                            onResult(LocationResult(lastLocation.latitude, lastLocation.longitude, cityName))
                        } else {
                            Log.w(TAG, "No location available")
                            onError("Lokasi tidak tersedia")
                        }
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Failed to get last location", e)
                        onError("Gagal mendapatkan lokasi")
                    }
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to get current location", e)
                onError("Gagal mendapatkan lokasi")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting location", e)
            onError("Izin lokasi belum diberikan")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error getting location", e)
            onError("Layanan lokasi tidak tersedia")
        }
    }

    private fun getCityName(context: Context, lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val result = addresses?.firstOrNull()?.locality
                ?: addresses?.firstOrNull()?.subAdminArea
                ?: addresses?.firstOrNull()?.adminArea
                ?: "Lokasi Tidak Dikenal"
            Log.d(TAG, "Geocoded: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding failed", e)
            "Lokasi Tidak Dikenal"
        }
    }
}
