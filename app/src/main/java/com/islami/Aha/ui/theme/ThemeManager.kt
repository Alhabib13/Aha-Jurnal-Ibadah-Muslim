package com.islami.Aha.ui.theme

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton yang mengelola state dark mode secara global.
 * Diobservasi oleh AhaApp untuk menentukan tema yang aktif.
 * Di-toggle oleh SettingsViewModel.
 */
object ThemeManager {
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(sharedPreferences: SharedPreferences) {
        prefs = sharedPreferences
        _isDarkMode.value = sharedPreferences.getBoolean(KEY_DARK_MODE, false)
    }

    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        prefs?.edit()?.putBoolean(KEY_DARK_MODE, newValue)?.apply()
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs?.edit()?.putBoolean(KEY_DARK_MODE, enabled)?.apply()
    }
}
