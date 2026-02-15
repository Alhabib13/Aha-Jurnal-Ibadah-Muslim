package com.islami.Aha.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.local.UserHabitDao
import com.islami.Aha.data.repository.AuthRepository
import com.islami.Aha.data.repository.ReAuthRequiredException
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

data class SettingsUiState(
    // Umum
    val location: String = "Jakarta",
    val selectedTimeFormat: TimeFormatOption = TimeFormatOption.HOUR_24,
    val darkModeEnabled: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userEmail: String = "",

    // Notifikasi
    val notificationEnabled: Boolean = true,
    val notificationSound: String = "Default",

    // Dialog states
    val showLocationDialog: Boolean = false,
    val showTimeFormatDialog: Boolean = false,
    val showResetConfirmation: Boolean = false,
    val showDeleteAccountConfirmation: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val showReAuthDialog: Boolean = false,

    // Snackbar
    val snackbarMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val habitDao: HabitDao,
    private val userHabitDao: UserHabitDao,
    private val habitCompletionDao: HabitCompletionDao,
    private val sharedPreferences: SharedPreferences,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            darkModeEnabled = ThemeManager.isDarkMode.value,
            isLoggedIn = authRepository.isLoggedIn,
            userEmail = authRepository.currentUser?.email.orEmpty()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // === Umum ===

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

    fun showDeleteAccountConfirmation() {
        _uiState.update { it.copy(showDeleteAccountConfirmation = true) }
    }

    fun hideDeleteAccountConfirmation() {
        _uiState.update { it.copy(showDeleteAccountConfirmation = false) }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                userEmail = ""
            )
        }
        showSnackbar("Anda telah keluar dari akun")
    }

    fun confirmDeleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true) }

            val result = authRepository.deleteAccount()
            if (result.isSuccess) {
                clearLocalData()
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        showDeleteAccountConfirmation = false,
                        isLoggedIn = false,
                        userEmail = ""
                    )
                }
                onSuccess()
            } else {
                val error = result.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        showDeleteAccountConfirmation = false
                    )
                }
                if (error is ReAuthRequiredException) {
                    _uiState.update { it.copy(showReAuthDialog = true) }
                } else {
                    showSnackbar(error?.message ?: "Gagal menghapus akun")
                }
            }
        }
    }

    fun hideReAuthDialog() {
        _uiState.update { it.copy(showReAuthDialog = false) }
    }

    fun confirmReAuthDelete(password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true) }

            val result = authRepository.reAuthenticateAndDelete(password)
            if (result.isSuccess) {
                clearLocalData()
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        showReAuthDialog = false,
                        isLoggedIn = false,
                        userEmail = ""
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(isDeletingAccount = false, showReAuthDialog = false)
                }
                showSnackbar("Password salah atau gagal menghapus akun")
            }
        }
    }

    private suspend fun clearLocalData() {
        habitDao.deleteAllHabits()
        userHabitDao.deleteAll()
        habitCompletionDao.deleteAll()
        sharedPreferences.edit()
            .remove("hasSeeded")
            .remove("dark_mode_enabled")
            .apply()
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
            userHabitDao.deleteAll()
            habitCompletionDao.deleteAll()
            sharedPreferences.edit().remove("hasSeeded").apply()
            _uiState.update { it.copy(showResetConfirmation = false) }
            showSnackbar("Semua data telah direset")
        }
    }

    // === Snackbar ===

    private fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
