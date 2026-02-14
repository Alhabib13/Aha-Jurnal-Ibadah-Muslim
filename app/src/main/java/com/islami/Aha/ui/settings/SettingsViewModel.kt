package com.islami.Aha.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.ui.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TimeFormatOption(val displayName: String, val description: String) {
    HOUR_24("24 Jam", "Contoh: 14:30"),
    HOUR_12("12 Jam", "Contoh: 2:30 PM")
}

enum class LanguageOption(val displayName: String) {
    INDONESIA("Indonesia"),
    ENGLISH("English")
}

data class SettingsUiState(
    // Umum
    val selectedLanguage: LanguageOption = LanguageOption.INDONESIA,
    val location: String = "Jakarta",
    val selectedTimeFormat: TimeFormatOption = TimeFormatOption.HOUR_24,
    val darkModeEnabled: Boolean = false,

    // Notifikasi
    val notificationEnabled: Boolean = true,
    val notificationSound: String = "Default",

    // Dialog states
    val showLanguageDialog: Boolean = false,
    val showLocationDialog: Boolean = false,
    val showTimeFormatDialog: Boolean = false,
    val showResetConfirmation: Boolean = false,

    // Snackbar
    val snackbarMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val habitDao: HabitDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(darkModeEnabled = ThemeManager.isDarkMode.value)
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // === Umum ===

    fun showLanguageDialog() {
        _uiState.update { it.copy(showLanguageDialog = true) }
    }

    fun hideLanguageDialog() {
        _uiState.update { it.copy(showLanguageDialog = false) }
    }

    fun setLanguage(language: LanguageOption) {
        _uiState.update {
            it.copy(selectedLanguage = language, showLanguageDialog = false)
        }
    }

    fun showLocationDialog() {
        _uiState.update { it.copy(showLocationDialog = true) }
    }

    fun hideLocationDialog() {
        _uiState.update { it.copy(showLocationDialog = false) }
    }

    fun setLocation(location: String) {
        if (location.isNotBlank()) {
            _uiState.update {
                it.copy(location = location.trim(), showLocationDialog = false)
            }
        }
    }

    fun showTimeFormatDialog() {
        _uiState.update { it.copy(showTimeFormatDialog = true) }
    }

    fun hideTimeFormatDialog() {
        _uiState.update { it.copy(showTimeFormatDialog = false) }
    }

    fun setTimeFormat(format: TimeFormatOption) {
        _uiState.update {
            it.copy(selectedTimeFormat = format, showTimeFormatDialog = false)
        }
    }

    fun toggleDarkMode() {
        ThemeManager.toggleDarkMode()
        _uiState.update { it.copy(darkModeEnabled = ThemeManager.isDarkMode.value) }
    }

    // === Notifikasi ===

    fun toggleNotification() {
        _uiState.update { it.copy(notificationEnabled = !it.notificationEnabled) }
    }

    fun onNotificationSoundClick() {
        showSnackbar("Segera hadir")
    }

    // === Privasi & Keamanan ===

    fun onChangePasswordClick() {
        showSnackbar("Segera hadir")
    }

    fun onAccountSecurityClick() {
        showSnackbar("Segera hadir")
    }

    // === Data ===

    fun onExportDataClick() {
        showSnackbar("Segera hadir")
    }

    fun showResetConfirmation() {
        _uiState.update { it.copy(showResetConfirmation = true) }
    }

    fun hideResetConfirmation() {
        _uiState.update { it.copy(showResetConfirmation = false) }
    }

    fun confirmResetData() {
        viewModelScope.launch {
            habitDao.deleteAllHabits()
            _uiState.update { it.copy(showResetConfirmation = false) }
            showSnackbar("Semua data telah direset")
        }
    }

    // === Tentang ===

    fun onPrivacyPolicyClick() {
        showSnackbar("Segera hadir")
    }

    fun onTermsClick() {
        showSnackbar("Segera hadir")
    }

    // === Snackbar ===

    private fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
