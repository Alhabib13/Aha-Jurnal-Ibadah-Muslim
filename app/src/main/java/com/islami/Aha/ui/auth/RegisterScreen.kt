package com.islami.Aha.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.islami.Aha.R
import com.islami.Aha.ui.theme.*

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onTermsClick: () -> Unit = {}
) {
    val uiState by viewModel.registerState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val googleSignInClient = remember(context) { createGoogleSignInClient(context) }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching {
            task.getResult(ApiException::class.java)?.idToken
        }.onSuccess { token ->
            viewModel.registerWithGoogleIdToken(token)
        }.onFailure { error ->
            val statusCode = (error as? ApiException)?.statusCode
            viewModel.onGoogleRegisterFailed(statusCode)
        }
    }

    LaunchedEffect(uiState.registerSuccess) {
        if (uiState.registerSuccess) {
            onNavigateToHome()
            viewModel.resetRegisterState()
        }
    }

    RegisterScreenContent(
        uiState = uiState,
        onNameChange = viewModel::onRegisterNameChange,
        onEmailChange = viewModel::onRegisterEmailChange,
        onPasswordChange = viewModel::onRegisterPasswordChange,
        onConfirmPasswordChange = viewModel::onRegisterConfirmPasswordChange,
        onTogglePasswordVisibility = viewModel::toggleRegisterPasswordVisibility,
        onToggleConfirmPasswordVisibility = viewModel::toggleRegisterConfirmPasswordVisibility,
        onRegisterClick = {
            focusManager.clearFocus()
            viewModel.register()
        },
        onGoogleRegisterClick = {
            focusManager.clearFocus()
            val client = googleSignInClient
            if (client == null) {
                viewModel.onGoogleRegisterUnavailable()
            } else {
                client.signOut().addOnCompleteListener {
                    googleSignInLauncher.launch(client.signInIntent)
                }
            }
        },
        onLoginClick = onNavigateToLogin,
        onPrivacyPolicyClick = onPrivacyPolicyClick,
        onTermsClick = onTermsClick
    )
}

@Composable
fun RegisterScreenContent(
    uiState: RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onRegisterClick: () -> Unit,
    onGoogleRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = stringResource(R.string.auth_logo_cd),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.auth_register_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.auth_register_subtitle),
                fontSize = 14.sp,
                color = Gray500,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ================================================================
            // FORM CARD
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
                    // Name
                    AuthTextField(
                        value = uiState.name,
                        onValueChange = onNameChange,
                        label = stringResource(R.string.auth_full_name_label),
                        placeholder = stringResource(R.string.auth_full_name_placeholder),
                        leadingIcon = Icons.Default.Person,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        isError = uiState.nameError != null,
                        errorMessage = uiState.nameError
                    )

                    // Email
                    AuthTextField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        label = stringResource(R.string.auth_email_label),
                        placeholder = stringResource(R.string.auth_email_placeholder),
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

                    // Password
                    AuthTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = stringResource(R.string.auth_password_label),
                        placeholder = stringResource(R.string.auth_password_min_placeholder),
                        leadingIcon = Icons.Default.Lock,
                        trailingIcon = if (uiState.isPasswordVisible)
                            Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        onTrailingIconClick = onTogglePasswordVisibility,
                        visualTransformation = if (uiState.isPasswordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        isError = uiState.passwordError != null,
                        errorMessage = uiState.passwordError
                    )

                    // Confirm Password
                    AuthTextField(
                        value = uiState.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = stringResource(R.string.auth_confirm_password_label),
                        placeholder = stringResource(R.string.auth_confirm_password_placeholder),
                        leadingIcon = Icons.Default.Lock,
                        trailingIcon = if (uiState.isConfirmPasswordVisible)
                            Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        onTrailingIconClick = onToggleConfirmPasswordVisibility,
                        visualTransformation = if (uiState.isConfirmPasswordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onRegisterClick()
                            }
                        ),
                        isError = uiState.confirmPasswordError != null,
                        errorMessage = uiState.confirmPasswordError
                    )
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
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ================================================================
            // REGISTER BUTTON with gradient
            // ================================================================
            Button(
                onClick = onRegisterClick,
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
                            text = stringResource(R.string.auth_register_button),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Gray200)
                Text(text = stringResource(R.string.auth_or_separator), fontSize = 14.sp, color = Gray400)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Gray200)
            }

            Spacer(modifier = Modifier.height(16.dp))

            RegisterSocialLoginButton(
                text = stringResource(R.string.auth_register_google_button),
                onClick = onGoogleRegisterClick,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auth_register_google_hint),
                fontSize = 12.sp,
                color = Gray500
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ================================================================
            // TERMS
            // ================================================================
            Text(
                text = stringResource(R.string.auth_register_terms_prefix),
                fontSize = 12.sp,
                color = Gray500,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onTermsClick,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.auth_terms_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Emerald
                    )
                }
                Text(text = stringResource(R.string.auth_and), fontSize = 12.sp, color = Gray500)
                TextButton(
                    onClick = onPrivacyPolicyClick,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.auth_privacy_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Emerald
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ================================================================
            // FOOTER
            // ================================================================
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.auth_have_account), fontSize = 14.sp, color = Gray500)
                TextButton(
                    onClick = onLoginClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.auth_login_action),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RegisterSocialLoginButton(
    text: String,
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
        Image(
            painter = painterResource(id = R.drawable.ic_google_logo),
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ============================================================================
// PREVIEW SECTION
// ============================================================================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenPreview() {
    HabitIslamiTheme {
        RegisterScreenContent(
            uiState = RegisterUiState(),
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTogglePasswordVisibility = {},
            onToggleConfirmPasswordVisibility = {},
            onRegisterClick = {},
            onGoogleRegisterClick = {},
            onLoginClick = {},
            onPrivacyPolicyClick = {},
            onTermsClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Register with Errors")
@Composable
fun RegisterScreenErrorPreview() {
    HabitIslamiTheme {
        RegisterScreenContent(
            uiState = RegisterUiState(
                name = "Ab",
                email = "invalid",
                password = "123",
                confirmPassword = "456",
                nameError = "Nama minimal 3 karakter",
                emailError = "Format email tidak valid",
                passwordError = "Password minimal 6 karakter",
                confirmPasswordError = "Password tidak cocok"
            ),
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTogglePasswordVisibility = {},
            onToggleConfirmPasswordVisibility = {},
            onRegisterClick = {},
            onGoogleRegisterClick = {},
            onLoginClick = {},
            onPrivacyPolicyClick = {},
            onTermsClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Register Loading")
@Composable
fun RegisterScreenLoadingPreview() {
    HabitIslamiTheme {
        RegisterScreenContent(
            uiState = RegisterUiState(
                name = "John Doe",
                email = "john@example.com",
                password = "password123",
                confirmPassword = "password123",
                isLoading = true
            ),
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTogglePasswordVisibility = {},
            onToggleConfirmPasswordVisibility = {},
            onRegisterClick = {},
            onGoogleRegisterClick = {},
            onLoginClick = {},
            onPrivacyPolicyClick = {},
            onTermsClick = {}
        )
    }
}
