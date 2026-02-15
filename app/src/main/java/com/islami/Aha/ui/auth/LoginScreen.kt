package com.islami.Aha.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.islami.Aha.R
import com.islami.Aha.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToRegister: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val uiState by viewModel.loginState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            onNavigateToHome()
            viewModel.resetLoginState()
        }
    }

    LoginScreenContent(
        uiState = uiState,
        onEmailChange = viewModel::onLoginEmailChange,
        onPasswordChange = viewModel::onLoginPasswordChange,
        onTogglePasswordVisibility = viewModel::toggleLoginPasswordVisibility,
        onLoginClick = {
            focusManager.clearFocus()
            viewModel.login()
        },
        onForgotPasswordClick = {
            focusManager.clearFocus()
            viewModel.requestPasswordReset()
        },
        onGoogleLoginClick = {},
        onFacebookLoginClick = {},
        onRegisterClick = onNavigateToRegister
    )
}

@Composable
fun LoginScreenContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onFacebookLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // ================================================================
        // HEADER - Logo smaller at top
        // ================================================================
        Surface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Aha Logo",
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Selamat Datang!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Masuk untuk melanjutkan perjalanan ibadah Anda",
            fontSize = 14.sp,
            color = Gray500,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ================================================================
        // FORM CARD with subtle shadow
        // ================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Email Field
                AuthTextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    label = "Email",
                    placeholder = "Masukkan email Anda",
                    leadingIcon = Icons.Default.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    isError = uiState.emailError != null,
                    errorMessage = uiState.emailError
                )

                // Password Field
                AuthTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    placeholder = "Masukkan password",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = if (uiState.isPasswordVisible)
                        Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    onTrailingIconClick = onTogglePasswordVisibility,
                    visualTransformation = if (uiState.isPasswordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onLoginClick()
                        }
                    ),
                    isError = uiState.passwordError != null,
                    errorMessage = uiState.passwordError
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onForgotPasswordClick,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Lupa password?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message from Firebase
        if (uiState.errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
            ) {
                Text(
                    text = uiState.errorMessage!!,
                    fontSize = 13.sp,
                    color = ErrorRed,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        } else if (uiState.infoMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Emerald.copy(alpha = 0.1f))
            ) {
                Text(
                    text = uiState.infoMessage,
                    fontSize = 13.sp,
                    color = EmeraldDark,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ================================================================
        // BUTTONS
        // ================================================================

        // Login button with gradient
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(),
            enabled = !uiState.isLoading
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(EmeraldDark, Emerald)
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Masuk",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Gray200)
            Text(text = "  atau  ", fontSize = 14.sp, color = Gray400)
            HorizontalDivider(modifier = Modifier.weight(1f), color = Gray200)
        }

        Spacer(modifier = Modifier.height(16.dp))

        SocialLoginButton(
            text = "Login Google",
            iconText = "G",
            onClick = onGoogleLoginClick,
            enabled = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        SocialLoginButton(
            text = "Login Facebook",
            iconText = "f",
            onClick = onFacebookLoginClick,
            enabled = false
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Login sosial segera hadir",
            fontSize = 12.sp,
            color = Gray500
        )

        Spacer(modifier = Modifier.weight(1f))

        // ================================================================
        // FOOTER
        // ================================================================
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Belum punya akun? ", fontSize = 14.sp, color = Gray500)
            TextButton(
                onClick = onRegisterClick,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Daftar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SocialLoginButton(
    text: String,
    iconText: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconText,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (enabled) text else "$text (Segera)",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Gray700,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = placeholder, color = Gray400) },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isError) ErrorRed else Emerald
                )
            },
            trailingIcon = if (trailingIcon != null) {
                {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = "Toggle visibility",
                        tint = Gray400,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onTrailingIconClick?.invoke() }
                    )
                }
            } else null,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald,
                unfocusedBorderColor = Gray200,
                errorBorderColor = ErrorRed,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = Gray50,
                errorContainerColor = ErrorRed.copy(alpha = 0.05f)
            )
        )

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 12.sp,
                color = ErrorRed,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

// ============================================================================
// PREVIEW SECTION
// ============================================================================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    HabitIslamiTheme {
        LoginScreenContent(
            uiState = LoginUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onGoogleLoginClick = {},
            onFacebookLoginClick = {},
            onRegisterClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Login with Error")
@Composable
fun LoginScreenErrorPreview() {
    HabitIslamiTheme {
        LoginScreenContent(
            uiState = LoginUiState(
                email = "invalid-email",
                password = "123",
                emailError = "Format email tidak valid",
                passwordError = "Password minimal 6 karakter"
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onGoogleLoginClick = {},
            onFacebookLoginClick = {},
            onRegisterClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Login Loading")
@Composable
fun LoginScreenLoadingPreview() {
    HabitIslamiTheme {
        LoginScreenContent(
            uiState = LoginUiState(
                email = "user@example.com",
                password = "password123",
                isLoading = true
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onGoogleLoginClick = {},
            onFacebookLoginClick = {},
            onRegisterClick = {}
        )
    }
}
