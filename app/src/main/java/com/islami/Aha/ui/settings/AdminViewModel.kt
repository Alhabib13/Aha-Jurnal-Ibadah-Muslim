package com.islami.Aha.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.data.repository.AdminConfigRepository
import com.islami.Aha.data.repository.AdminAccessDebug
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
    val ramadanScheduleByLocationEnabled: Boolean = true,
    val sholatTarawihEnabled: Boolean = true,
    val fardhuScheduleByLocationEnabled: Boolean = true,
    val adminDiagnosticText: String = "",
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
                        puasaWajibRamadanEnabled = config.puasaWajibRamadanEnabled,
                        ramadanScheduleByLocationEnabled = config.ramadanScheduleByLocationEnabled,
                        sholatTarawihEnabled = config.sholatTarawihEnabled,
                        fardhuScheduleByLocationEnabled = config.fardhuScheduleByLocationEnabled
                    )
                }
            }
        }
        refreshAdminState()
    }

    fun refreshAdminState() {
        viewModelScope.launch {
            val debug = adminConfigRepository.getAdminAccessDebug()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isAdmin = debug.isAdmin,
                    adminDiagnosticText = buildDiagnosticText(debug)
                )
            }
        }
    }

    private fun buildDiagnosticText(debug: AdminAccessDebug): String {
        return buildString {
            append("UID: ${debug.uid.ifBlank { "-"}}")
            append("\nEmail: ${debug.email.ifBlank { "-"}}")
            append("\nProject: ${debug.projectId.ifBlank { "-"}}")
            append("\nClaim admin: ${if (debug.hasAdminClaim) "YA" else "TIDAK"}")
            append("\nClaim role=admin: ${if (debug.hasRoleClaim) "YA" else "TIDAK"}")
            append("\nProfile role=admin: ${if (debug.hasProfileRole) "YA" else "TIDAK"}")
            append("\nStatus: ${debug.message.ifBlank { "Tidak diketahui" }}")
        }
    }

    fun setPuasaWajibRamadanEnabled(enabled: Boolean) {
        setFeatureWithAdminCheck(
            updater = { adminConfigRepository.setPuasaWajibRamadanEnabled(enabled) }
        )
    }

    fun setRamadanScheduleByLocationEnabled(enabled: Boolean) {
        setFeatureWithAdminCheck(
            updater = { adminConfigRepository.setRamadanScheduleByLocationEnabled(enabled) }
        )
    }

    fun setSholatTarawihEnabled(enabled: Boolean) {
        setFeatureWithAdminCheck(
            updater = { adminConfigRepository.setSholatTarawihEnabled(enabled) }
        )
    }

    fun setFardhuScheduleByLocationEnabled(enabled: Boolean) {
        setFeatureWithAdminCheck(
            updater = { adminConfigRepository.setFardhuScheduleByLocationEnabled(enabled) }
        )
    }

    private fun setFeatureWithAdminCheck(
        updater: suspend () -> Result<Unit>
    ) {
        if (!_uiState.value.isAdmin) {
            _uiState.update { it.copy(snackbarMessage = "Akses admin diperlukan") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = updater()
            val errorMessage = result.exceptionOrNull()?.message.orEmpty()
            val permissionDenied = errorMessage.contains("PERMISSION_DENIED", ignoreCase = true) ||
                errorMessage.contains("Missing or insufficient permissions", ignoreCase = true)
            _uiState.update {
                it.copy(
                    isSaving = false,
                    snackbarMessage = if (result.isSuccess) {
                        "Pengaturan berhasil diperbarui"
                    } else if (permissionDenied) {
                        "Gagal: akun ini belum punya izin admin Firestore."
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
