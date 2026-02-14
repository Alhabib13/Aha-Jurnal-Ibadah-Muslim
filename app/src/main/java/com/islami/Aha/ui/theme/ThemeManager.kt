package com.islami.Aha.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton yang mengelola state dark mode secara global.
 * Diobservasi oleh AhaApp untuk menentukan tema yang aktif.
 * Di-toggle oleh SettingsViewModel.
 */
object ThemeManager {
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }
}
