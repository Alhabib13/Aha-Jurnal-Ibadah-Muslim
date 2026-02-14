package com.islami.Aha.util

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

data class LocationPermissionState(
    val isGranted: Boolean,
    val requestPermission: () -> Unit
)

@Composable
fun rememberLocationPermissionState(
    onPermissionResult: (Boolean) -> Unit = {}
): LocationPermissionState {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(LocationHelper.hasLocationPermission(context))
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        isGranted = granted
        onPermissionResult(granted)
    }

    return LocationPermissionState(
        isGranted = isGranted,
        requestPermission = {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    )
}
