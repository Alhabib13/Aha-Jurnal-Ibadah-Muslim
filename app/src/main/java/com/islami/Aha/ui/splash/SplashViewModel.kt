package com.islami.Aha.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.islami.Aha.data.repository.AuthRepository

/**
 * UI State untuk Splash Screen.
 *
 * @property isLoading Menandakan apakah splash screen masih loading
 * @property isUserLoggedIn Menandakan apakah user sudah login sebelumnya
 * @property shouldNavigate Trigger untuk memulai navigasi
 */
data class SplashUiState(
    val isLoading: Boolean = true,
    val shouldNavigate: Boolean = false
)

/**
 * ViewModel untuk Splash Screen.
 *
 * Bertanggung jawab untuk:
 * 1. Menampilkan splash selama 2 detik
 * 2. Menentukan navigasi berikutnya (Home guest mode)
 *
 * Untuk saat ini, logic login menggunakan dummy data.
 * Di production, akan membaca dari SharedPreferences atau DataStore.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    // Durasi splash screen dalam milliseconds
    companion object {
        private const val SPLASH_DURATION = 2000L
    }

    init {
        startSplashTimer()
    }

    /**
     * Memulai timer splash screen.
     * Setelah durasi selesai, akan mengecek status login dan trigger navigasi.
     */
    private fun startSplashTimer() {
        viewModelScope.launch {
            // Restore Firebase session to SharedPreferences
            authRepository.restoreSessionFromFirebase()
            
            // Tampilkan splash selama durasi yang ditentukan
            delay(SPLASH_DURATION)

            // Update state untuk trigger navigasi
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    shouldNavigate = true
                )
            }
        }
    }

    /**
     * Reset state navigasi.
     * Dipanggil setelah navigasi selesai untuk mencegah navigasi berulang.
     */
    fun onNavigationComplete() {
        _uiState.update { currentState ->
            currentState.copy(shouldNavigate = false)
        }
    }
}
