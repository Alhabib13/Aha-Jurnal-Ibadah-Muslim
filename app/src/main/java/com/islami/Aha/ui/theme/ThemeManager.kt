package com.islami.Aha.ui.theme

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val displayName: String) { 
    LIGHT("Terang"), 
    DARK("Gelap"), 
    SYSTEM("Ikuti Sistem") 
}

/**
 * Singleton yang mengelola state dark mode secara global.
 * Diobservasi oleh AhaApp untuk menentukan tema yang aktif.
 */
object ThemeManager {
    private const val KEY_THEME_MODE = "theme_mode_setting"
    private const val KEY_DARK_MODE_LEGACY = "dark_mode_enabled"

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(sharedPreferences: SharedPreferences) {
        prefs = sharedPreferences
        val savedModeStr = sharedPreferences.getString(KEY_THEME_MODE, null)
        if (savedModeStr != null) {
            try {
                _themeMode.value = ThemeMode.valueOf(savedModeStr)
            } catch (e: Exception) {
                _themeMode.value = ThemeMode.SYSTEM
            }
        } else {
            // Migrasi dari boolean lama
            if (sharedPreferences.contains(KEY_DARK_MODE_LEGACY)) {
                val oldDark = sharedPreferences.getBoolean(KEY_DARK_MODE_LEGACY, false)
                _themeMode.value = if (oldDark) ThemeMode.DARK else ThemeMode.LIGHT
            } else {
                _themeMode.value = ThemeMode.SYSTEM
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }
}
