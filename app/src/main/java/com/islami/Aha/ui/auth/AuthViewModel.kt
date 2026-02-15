package com.islami.Aha.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.data.repository.AuthRepository
import com.islami.Aha.data.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val loginSuccess: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val registerSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // ========================================================================
    // LOGIN STATE
    // ========================================================================

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    // ========================================================================
    // REGISTER STATE
    // ========================================================================

    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    // ========================================================================
    // LOGIN FUNCTIONS
    // ========================================================================

    fun onLoginEmailChange(email: String) {
        _loginState.update { it.copy(email = email, emailError = null, errorMessage = null, infoMessage = null) }
    }

    fun onLoginPasswordChange(password: String) {
        _loginState.update { it.copy(password = password, passwordError = null, errorMessage = null, infoMessage = null) }
    }

    fun toggleLoginPasswordVisibility() {
        _loginState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun login() {
        val currentState = _loginState.value

        val emailError = validateEmail(currentState.email)
        val passwordError = validatePassword(currentState.password)

        if (emailError != null || passwordError != null) {
            _loginState.update {
                it.copy(emailError = emailError, passwordError = passwordError)
            }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

            when (val result = authRepository.login(currentState.email, currentState.password)) {
                is AuthResult.Success -> {
                    _loginState.update {
                        it.copy(isLoading = false, loginSuccess = true)
                    }
                }
                is AuthResult.Error -> {
                    _loginState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun requestPasswordReset() {
        val currentState = _loginState.value
        val emailError = validateEmail(currentState.email)
        if (emailError != null) {
            _loginState.update { it.copy(emailError = emailError, errorMessage = null, infoMessage = null) }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.sendPasswordReset(currentState.email)
            if (result.isSuccess) {
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        infoMessage = "Email reset password telah dikirim"
                    )
                }
            } else {
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gagal mengirim email reset password"
                    )
                }
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginUiState()
    }

    // ========================================================================
    // REGISTER FUNCTIONS
    // ========================================================================

    fun onRegisterNameChange(name: String) {
        _registerState.update { it.copy(name = name, nameError = null, errorMessage = null) }
    }

    fun onRegisterEmailChange(email: String) {
        _registerState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    fun onRegisterPasswordChange(password: String) {
        _registerState.update { it.copy(password = password, passwordError = null, errorMessage = null) }
    }

    fun onRegisterConfirmPasswordChange(confirmPassword: String) {
        _registerState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null, errorMessage = null) }
    }

    fun toggleRegisterPasswordVisibility() {
        _registerState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleRegisterConfirmPasswordVisibility() {
        _registerState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun register() {
        val currentState = _registerState.value

        val nameError = validateName(currentState.name)
        val emailError = validateEmail(currentState.email)
        val passwordError = validatePassword(currentState.password)
        val confirmPasswordError = validateConfirmPassword(
            currentState.password,
            currentState.confirmPassword
        )

        if (nameError != null || emailError != null ||
            passwordError != null || confirmPasswordError != null) {
            _registerState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError
                )
            }
            return
        }

        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.register(
                currentState.name,
                currentState.email,
                currentState.password
            )) {
                is AuthResult.Success -> {
                    _registerState.update {
                        it.copy(isLoading = false, registerSuccess = true)
                    }
                }
                is AuthResult.Error -> {
                    _registerState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun resetRegisterState() {
        _registerState.value = RegisterUiState()
    }

    // ========================================================================
    // VALIDATION FUNCTIONS
    // ========================================================================

    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Nama tidak boleh kosong"
            name.length < 3 -> "Nama minimal 3 karakter"
            else -> null
        }
    }

    private fun validateEmail(email: String): String? {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return when {
            email.isBlank() -> "Email tidak boleh kosong"
            !email.matches(emailRegex) -> "Format email tidak valid"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password tidak boleh kosong"
            password.length < 6 -> "Password minimal 6 karakter"
            else -> null
        }
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Konfirmasi password tidak boleh kosong"
            confirmPassword != password -> "Password tidak cocok"
            else -> null
        }
    }
}
