package com.islami.Aha.ui.auth

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.R
import com.islami.Aha.data.repository.AuthRepository
import com.islami.Aha.data.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val infoMessage: String? = null,
    val showForgotPassword: Boolean = false
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
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object {
        private const val INVALID_LOGIN_CREDENTIAL_KEYWORD = "invalid login credentials"
    }

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
        _loginState.update {
            it.copy(
                email = email,
                emailError = null,
                passwordError = null,
                errorMessage = null,
                infoMessage = null,
                showForgotPassword = false
            )
        }
    }

    fun onLoginPasswordChange(password: String) {
        _loginState.update {
            it.copy(
                password = password,
                emailError = null,
                passwordError = null,
                errorMessage = null,
                infoMessage = null,
                showForgotPassword = false
            )
        }
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
                        it.copy(
                            isLoading = false,
                            loginSuccess = true,
                            showForgotPassword = false
                        )
                    }
                }
                is AuthResult.Error -> {
                    val credentialMessage = normalizeLoginCredentialError(result.message)
                    _loginState.update {
                        if (credentialMessage != null) {
                            it.copy(
                                isLoading = false,
                                emailError = credentialMessage,
                                passwordError = credentialMessage,
                                errorMessage = credentialMessage,
                                showForgotPassword = true
                            )
                        } else {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message,
                                showForgotPassword = false
                            )
                        }
                    }
                }
            }
        }
    }

    fun loginWithGoogleIdToken(idToken: String?) {
        if (idToken.isNullOrBlank()) {
            _loginState.update { it.copy(errorMessage = text(R.string.auth_google_login_cancelled)) }
            return
        }
        loginWithGoogleProviderForLogin(idToken)
    }

    fun onGoogleLoginUnavailable() {
        _loginState.update { it.copy(errorMessage = text(R.string.auth_google_config_incomplete)) }
    }

    fun onGoogleLoginFailed(statusCode: Int?) {
        val message = when (statusCode) {
            12501, 16 -> text(R.string.auth_google_login_cancelled)
            7 -> text(R.string.auth_error_no_internet)
            10 -> text(R.string.auth_error_google_config_invalid)
            else -> text(R.string.auth_google_login_failed_retry)
        }
        _loginState.update { it.copy(errorMessage = message) }
    }

    fun requestPasswordReset() {
        val currentState = _loginState.value
        val emailError = validateEmail(currentState.email)
        if (emailError != null) {
            _loginState.update {
                it.copy(
                    emailError = emailError,
                    errorMessage = null,
                    infoMessage = null,
                    showForgotPassword = false
                )
            }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.sendPasswordReset(currentState.email)
            if (result.isSuccess) {
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        infoMessage = text(R.string.auth_info_reset_password_sent),
                        showForgotPassword = true
                    )
                }
            } else {
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message
                            ?: text(R.string.auth_error_reset_password_failed),
                        showForgotPassword = true
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

    fun registerWithGoogleIdToken(idToken: String?) {
        if (idToken.isNullOrBlank()) {
            _registerState.update { it.copy(errorMessage = text(R.string.auth_google_login_cancelled)) }
            return
        }
        loginWithGoogleProviderForRegister(idToken)
    }

    fun onGoogleRegisterUnavailable() {
        _registerState.update { it.copy(errorMessage = text(R.string.auth_google_config_incomplete)) }
    }

    fun onGoogleRegisterFailed(statusCode: Int?) {
        val message = when (statusCode) {
            12501, 16 -> text(R.string.auth_google_login_cancelled)
            7 -> text(R.string.auth_error_no_internet)
            10 -> text(R.string.auth_error_google_config_invalid)
            else -> text(R.string.auth_google_login_failed_retry)
        }
        _registerState.update { it.copy(errorMessage = message) }
    }

    fun resetRegisterState() {
        _registerState.value = RegisterUiState()
    }

    // ========================================================================
    // VALIDATION FUNCTIONS
    // ========================================================================

    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> text(R.string.auth_validation_name_empty)
            name.length < 3 -> text(R.string.auth_validation_name_min)
            else -> null
        }
    }

    private fun validateEmail(email: String): String? {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return when {
            email.isBlank() -> text(R.string.auth_validation_email_empty)
            !email.matches(emailRegex) -> text(R.string.auth_validation_email_invalid)
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> text(R.string.auth_validation_password_empty)
            password.length < 6 -> text(R.string.auth_validation_password_min)
            else -> null
        }
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> text(R.string.auth_validation_confirm_password_empty)
            confirmPassword != password -> text(R.string.auth_validation_confirm_password_mismatch)
            else -> null
        }
    }

    private fun normalizeLoginCredentialError(message: String): String? {
        val lowerMessage = message.lowercase()
        val invalidLoginCredentialMessage = text(R.string.auth_error_invalid_credentials)
        return when {
            invalidLoginCredentialMessage.lowercase() in lowerMessage -> invalidLoginCredentialMessage
            "password salah" in lowerMessage -> invalidLoginCredentialMessage
            INVALID_LOGIN_CREDENTIAL_KEYWORD in lowerMessage -> invalidLoginCredentialMessage
            else -> null
        }
    }

    private fun text(@StringRes resId: Int, vararg formatArgs: Any): String {
        return appContext.getString(resId, *formatArgs)
    }

    private fun loginWithGoogleProviderForLogin(idToken: String) {
        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.loginWithGoogleIdToken(idToken)

            when (result) {
                is AuthResult.Success -> {
                    _loginState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                is AuthResult.Error -> {
                    _loginState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    private fun loginWithGoogleProviderForRegister(idToken: String) {
        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.loginWithGoogleIdToken(idToken)

            when (result) {
                is AuthResult.Success -> {
                    _registerState.update { it.copy(isLoading = false, registerSuccess = true) }
                }
                is AuthResult.Error -> {
                    _registerState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }
}
