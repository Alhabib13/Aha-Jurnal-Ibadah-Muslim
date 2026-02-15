package com.islami.Aha.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.data.repository.AdminConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isAdmin: Boolean = false,
    val puasaWajibRamadanEnabled: Boolean = true,
    val snackbarMessage: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminConfigRepository: AdminConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            adminConfigRepository.featureConfig.collect { config ->
                _uiState.update {
                    it.copy(
                        puasaWajibRamadanEnabled = config.puasaWajibRamadanEnabled
                    )
                }
            }
        }
        refreshAdminState()
    }

    fun refreshAdminState() {
        viewModelScope.launch {
            val isAdmin = adminConfigRepository.isCurrentUserAdmin()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isAdmin = isAdmin
                )
            }
        }
    }

    fun setPuasaWajibRamadanEnabled(enabled: Boolean) {
        if (!_uiState.value.isAdmin) {
            _uiState.update { it.copy(snackbarMessage = "Akses admin diperlukan") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = adminConfigRepository.setPuasaWajibRamadanEnabled(enabled)
            _uiState.update {
                it.copy(
                    isSaving = false,
                    snackbarMessage = if (result.isSuccess) {
                        "Pengaturan berhasil diperbarui"
                    } else {
                        "Gagal memperbarui pengaturan"
                    }
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
