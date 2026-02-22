package com.islami.Aha.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.islami.Aha.BuildConfig
import com.islami.Aha.R
import com.islami.Aha.ui.theme.*
import com.islami.Aha.util.NotificationScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportDataToUri(uri)
        } else {
            viewModel.onExportCancelled()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onImportFileSelected(uri)
        } else {
            viewModel.onImportCancelled()
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(uiState.launchExportPicker, uiState.exportFileName) {
        if (uiState.launchExportPicker) {
            exportLauncher.launch(uiState.exportFileName.ifBlank { "aha_backup.json" })
            viewModel.onExportPickerHandled()
        }
    }

    LaunchedEffect(uiState.launchImportPicker) {
        if (uiState.launchImportPicker) {
            importLauncher.launch(arrayOf("application/json", "text/plain"))
            viewModel.onImportPickerHandled()
        }
    }

    SettingsScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onShowLocationDialog = viewModel::showLocationDialog,
        onHideLocationDialog = viewModel::hideLocationDialog,
        onSetLocation = viewModel::setLocation,
        onShowTimeFormatDialog = viewModel::showTimeFormatDialog,
        onHideTimeFormatDialog = viewModel::hideTimeFormatDialog,
        onSetTimeFormat = viewModel::setTimeFormat,
        onToggleDarkMode = viewModel::toggleDarkMode,
        onToggleNotification = viewModel::toggleNotification,
        onNotificationSoundClick = viewModel::onNotificationSoundClick,
        onHideNotificationSoundDialog = viewModel::hideNotificationSoundDialog,
        onSetNotificationSound = viewModel::setNotificationSound,
        onToggleNotificationVibration = viewModel::toggleNotificationVibration,
        onChangePasswordClick = viewModel::onChangePasswordClick,
        onHideChangePasswordDialog = viewModel::hideChangePasswordDialog,
        onSubmitChangePassword = viewModel::submitPasswordChange,
        onForgotPasswordFromSettings = viewModel::sendForgotPasswordFromSettings,
        onAccountSecurityClick = viewModel::onAccountSecurityClick,
        onHideAccountSecurityDialog = viewModel::hideAccountSecurityDialog,
        onRefreshEmailVerificationStatus = viewModel::refreshEmailVerificationStatus,
        onSendEmailVerificationFromSecurity = viewModel::sendEmailVerificationFromSecurity,
        onSendPasswordResetFromSecurity = viewModel::sendForgotPasswordFromSecurity,
        onDeleteAccountFromSecurity = viewModel::showDeleteAccountFromSecurity,
        onShowDeleteAccountConfirmation = viewModel::showDeleteAccountConfirmation,
        onHideDeleteAccountConfirmation = viewModel::hideDeleteAccountConfirmation,
        onConfirmDeleteAccount = {
            viewModel.confirmDeleteAccount {
                onNavigateToLogin()
            }
        },
        onImportDataClick = viewModel::onImportDataClick,
        onHideImportConfirmationDialog = viewModel::hideImportConfirmationDialog,
        onSetImportMode = viewModel::setImportMode,
        onConfirmImportData = viewModel::confirmImportData,
        onExportDataClick = viewModel::onExportDataClick,
        onShowResetConfirmation = viewModel::showResetConfirmation,
        onHideResetConfirmation = viewModel::hideResetConfirmation,
        onConfirmReset = viewModel::confirmResetData,
        onPrivacyPolicyClick = {
            context.startActivity(
                Intent(context, LegalDocumentActivity::class.java).apply {
                    putExtra(LegalDocumentActivity.EXTRA_DOCUMENT_TYPE, LegalDocumentActivity.DOC_PRIVACY)
                }
            )
        },
        onTermsClick = {
            context.startActivity(
                Intent(context, LegalDocumentActivity::class.java).apply {
                    putExtra(LegalDocumentActivity.EXTRA_DOCUMENT_TYPE, LegalDocumentActivity.DOC_TERMS)
                }
            )
        },
        onLoginClick = onNavigateToLogin,
        onLogoutClick = {
            viewModel.logout()
            onNavigateToLogin()
        },
        onHideReAuthDialog = viewModel::hideReAuthDialog,
        onConfirmReAuthDelete = { password ->
            viewModel.confirmReAuthDelete(password) {
                onNavigateToLogin()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onShowLocationDialog: () -> Unit,
    onHideLocationDialog: () -> Unit,
    onSetLocation: (String) -> Unit,
    onShowTimeFormatDialog: () -> Unit,
    onHideTimeFormatDialog: () -> Unit,
    onSetTimeFormat: (TimeFormatOption) -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleNotification: () -> Unit,
    onNotificationSoundClick: () -> Unit,
    onHideNotificationSoundDialog: () -> Unit,
    onSetNotificationSound: (NotificationScheduler.NotificationSoundOption) -> Unit,
    onToggleNotificationVibration: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onHideChangePasswordDialog: () -> Unit,
    onSubmitChangePassword: (String, String, String) -> Unit,
    onForgotPasswordFromSettings: () -> Unit,
    onAccountSecurityClick: () -> Unit,
    onHideAccountSecurityDialog: () -> Unit,
    onRefreshEmailVerificationStatus: () -> Unit,
    onSendEmailVerificationFromSecurity: () -> Unit,
    onSendPasswordResetFromSecurity: () -> Unit,
    onDeleteAccountFromSecurity: () -> Unit,
    onShowDeleteAccountConfirmation: () -> Unit,
    onHideDeleteAccountConfirmation: () -> Unit,
    onConfirmDeleteAccount: () -> Unit,
    onImportDataClick: () -> Unit,
    onHideImportConfirmationDialog: () -> Unit,
    onSetImportMode: (ImportMode) -> Unit,
    onConfirmImportData: () -> Unit,
    onExportDataClick: () -> Unit,
    onShowResetConfirmation: () -> Unit,
    onHideResetConfirmation: () -> Unit,
    onConfirmReset: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsClick: () -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onHideReAuthDialog: () -> Unit = {},
    onConfirmReAuthDelete: (String) -> Unit = {}
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0).only(WindowInsetsSides.Horizontal),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Gray800,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(top = 0.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                SettingsHeader(onNavigateBack = onNavigateBack)
            }
            // =============================================================
            // SECTION: UMUM
            // =============================================================
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_general))
            }

            // Format Waktu
            item {
                SettingsClickableItem(
                    icon = Icons.Outlined.Schedule,
                    iconBackground = WarningAmber.copy(alpha = 0.1f),
                    iconTint = WarningAmber,
                    title = stringResource(R.string.settings_time_format_title),
                    subtitle = uiState.selectedTimeFormat.displayName,
                    onClick = onShowTimeFormatDialog
                )
            }

            // Mode Gelap
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleItem(
                    icon = Icons.Outlined.DarkMode,
                    iconBackground = CategoryPuasaStart.copy(alpha = 0.1f),
                    iconTint = CategoryPuasaStart,
                    title = stringResource(R.string.settings_dark_mode_title),
                    subtitle = stringResource(R.string.settings_dark_mode_subtitle),
                    isChecked = uiState.darkModeEnabled,
                    onToggle = onToggleDarkMode
                )
            }

            // =============================================================
            // SECTION: NOTIFIKASI
            // =============================================================
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = stringResource(R.string.settings_section_notifications))
            }

            // Pengingat Ibadah
            item {
                SettingsToggleItem(
                    icon = Icons.Outlined.Notifications,
                    iconBackground = InfoBlue.copy(alpha = 0.1f),
                    iconTint = InfoBlue,
                    title = stringResource(R.string.settings_reminder_title),
                    subtitle = stringResource(R.string.settings_reminder_subtitle),
                    isChecked = uiState.notificationEnabled,
                    onToggle = onToggleNotification
                )
            }

            // Suara Notifikasi
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsClickableItem(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    iconBackground = CategoryDzikirStart.copy(alpha = 0.1f),
                    iconTint = CategoryDzikirStart,
                    title = stringResource(R.string.settings_sound_title),
                    subtitle = uiState.notificationSound.displayName,
                    onClick = onNotificationSoundClick
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleItem(
                    icon = Icons.Outlined.Vibration,
                    iconBackground = InfoBlue.copy(alpha = 0.1f),
                    iconTint = InfoBlue,
                    title = stringResource(R.string.settings_vibration_title),
                    subtitle = stringResource(R.string.settings_vibration_subtitle),
                    isChecked = uiState.notificationVibrationEnabled,
                    onToggle = onToggleNotificationVibration
                )
            }

            // =============================================================
            // SECTION: AKUN
            // =============================================================
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = stringResource(R.string.settings_section_account))
            }

            if (uiState.isLoggedIn) {
                item {
                    SettingsInfoItem(
                        icon = Icons.Outlined.Person,
                        iconBackground = CategoryDzikirStart.copy(alpha = 0.1f),
                        iconTint = CategoryDzikirStart,
                        title = stringResource(R.string.settings_active_account_title),
                        value = uiState.userEmail.ifBlank { stringResource(R.string.settings_user_fallback) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsClickableItem(
                        icon = Icons.Outlined.Lock,
                        iconBackground = WarningAmber.copy(alpha = 0.1f),
                        iconTint = WarningAmber,
                        title = stringResource(R.string.settings_change_password_title),
                        subtitle = stringResource(R.string.settings_change_password_subtitle),
                        onClick = onChangePasswordClick
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsClickableItem(
                        icon = Icons.Outlined.Shield,
                        iconBackground = InfoBlue.copy(alpha = 0.1f),
                        iconTint = InfoBlue,
                        title = stringResource(R.string.settings_account_security_title),
                        subtitle = stringResource(R.string.settings_account_security_subtitle),
                        onClick = onAccountSecurityClick
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsClickableItem(
                        icon = Icons.Outlined.Logout,
                        iconBackground = ErrorRed.copy(alpha = 0.1f),
                        iconTint = ErrorRed,
                        title = stringResource(R.string.settings_logout_title),
                        subtitle = stringResource(R.string.settings_logout_subtitle),
                        titleColor = ErrorRed,
                        onClick = onLogoutClick
                    )
                }
            } else {
                item {
                    GuestLoginCard(onLoginClick = onLoginClick)
                }
            }

            // =============================================================
            // SECTION: DATA
            // =============================================================
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = stringResource(R.string.settings_section_data))
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Outlined.Download,
                    iconBackground = CategoryTilawahStart.copy(alpha = 0.1f),
                    iconTint = CategoryTilawahStart,
                    title = stringResource(R.string.settings_import_title),
                    subtitle = stringResource(R.string.settings_import_subtitle),
                    onClick = onImportDataClick
                )
            }

            if (uiState.isLoggedIn) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsClickableItem(
                        icon = Icons.Outlined.Upload,
                        iconBackground = InfoBlue.copy(alpha = 0.1f),
                        iconTint = InfoBlue,
                        title = stringResource(R.string.settings_export_title),
                        subtitle = stringResource(R.string.settings_export_subtitle),
                        onClick = onExportDataClick
                    )
                }
            }

            // Reset Data
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsClickableItem(
                    icon = Icons.Outlined.DeleteForever,
                    iconBackground = ErrorRed.copy(alpha = 0.1f),
                    iconTint = ErrorRed,
                    title = stringResource(R.string.settings_reset_title),
                    subtitle = if (uiState.isLoggedIn) {
                        stringResource(R.string.settings_reset_subtitle_logged_in)
                    } else {
                        stringResource(R.string.settings_reset_subtitle_guest)
                    },
                    titleColor = ErrorRed,
                    onClick = onShowResetConfirmation
                )
            }

            // =============================================================
            // SECTION: TENTANG
            // =============================================================
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = stringResource(R.string.settings_section_about))
            }

            // Versi Aplikasi
            item {
                SettingsInfoItem(
                    icon = Icons.Outlined.Info,
                    iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = stringResource(R.string.settings_app_version_title),
                    value = BuildConfig.VERSION_NAME.ifBlank { "-" }
                )
            }

            // Kebijakan Privasi
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsClickableItem(
                    icon = Icons.Outlined.PrivacyTip,
                    iconBackground = CategoryDzikirStart.copy(alpha = 0.1f),
                    iconTint = CategoryDzikirStart,
                    title = stringResource(R.string.settings_privacy_title),
                    subtitle = stringResource(R.string.settings_privacy_subtitle),
                    onClick = onPrivacyPolicyClick
                )
            }

            // Syarat & Ketentuan
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsClickableItem(
                    icon = Icons.Outlined.Description,
                    iconBackground = CategoryTilawahStart.copy(alpha = 0.1f),
                    iconTint = CategoryTilawahStart,
                    title = stringResource(R.string.settings_terms_title),
                    subtitle = stringResource(R.string.settings_terms_subtitle),
                    onClick = onTermsClick
                )
            }

            // Footer
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // === DIALOGS ===

    if (uiState.showLocationDialog) {
        LocationInputDialog(
            currentLocation = uiState.location,
            onConfirm = onSetLocation,
            onDismiss = onHideLocationDialog
        )
    }

    if (uiState.showTimeFormatDialog) {
        TimeFormatSelectionDialog(
            currentFormat = uiState.selectedTimeFormat,
            onSelect = onSetTimeFormat,
            onDismiss = onHideTimeFormatDialog
        )
    }

    if (uiState.showNotificationSoundDialog) {
        NotificationSoundSelectionDialog(
            currentOption = uiState.notificationSound,
            onSelect = onSetNotificationSound,
            onDismiss = onHideNotificationSoundDialog
        )
    }

    if (uiState.showChangePasswordDialog) {
        ChangePasswordDialog(
            userEmail = uiState.userEmail,
            isSubmitting = uiState.isChangingPassword,
            onDismiss = onHideChangePasswordDialog,
            onForgotPassword = onForgotPasswordFromSettings,
            onSubmit = onSubmitChangePassword
        )
    }

    if (uiState.showAccountSecurityDialog) {
        AccountSecurityDialog(
            userEmail = uiState.userEmail,
            isEmailVerified = uiState.isEmailVerified,
            isRefreshingStatus = uiState.isRefreshingSecurityStatus,
            isSendingVerificationEmail = uiState.isSendingVerificationEmail,
            verificationResendCooldownSeconds = uiState.verificationResendCooldownSeconds,
            isSendingResetPasswordEmail = uiState.isSendingResetPasswordEmail,
            onRefreshStatus = onRefreshEmailVerificationStatus,
            onSendVerificationEmail = onSendEmailVerificationFromSecurity,
            onSendResetPasswordEmail = onSendPasswordResetFromSecurity,
            onDeleteAccount = onDeleteAccountFromSecurity,
            onDismiss = onHideAccountSecurityDialog
        )
    }

    if (uiState.showImportConfirmationDialog) {
        ImportDataConfirmationDialog(
            selectedMode = uiState.selectedImportMode,
            isImporting = uiState.isImportingData,
            onSelectMode = onSetImportMode,
            onConfirm = onConfirmImportData,
            onDismiss = onHideImportConfirmationDialog
        )
    }

    if (uiState.showResetConfirmation) {
        ResetConfirmationDialog(
            onConfirm = onConfirmReset,
            onDismiss = onHideResetConfirmation
        )
    }

    if (uiState.showDeleteAccountConfirmation) {
        DeleteAccountConfirmationDialog(
            isDeleting = uiState.isDeletingAccount,
            onConfirm = onConfirmDeleteAccount,
            onDismiss = onHideDeleteAccountConfirmation
        )
    }

    if (uiState.showReAuthDialog) {
        ReAuthDialog(
            isDeleting = uiState.isDeletingAccount,
            onConfirm = onConfirmReAuthDelete,
            onDismiss = onHideReAuthDialog
        )
    }
}

@Composable
private fun SettingsHeader(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(colors = listOf(EmeraldDark, Emerald)),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 104.dp)
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back_cd),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Text(
                    text = stringResource(R.string.settings_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_header_subtitle),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// ============================================================================
// REUSABLE SETTINGS COMPONENTS
// ============================================================================

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.3.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
fun GuestLoginCard(onLoginClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_guest_login_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.settings_guest_login_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onLoginClick,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_guest_login_action),
                    color = Emerald,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    iconBackground: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    val resolvedIconBackground = if (iconBackground == Color.Unspecified) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        iconBackground
    }
    val resolvedIconTint = if (iconTint == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        iconTint
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = isChecked,
                    role = Role.Switch,
                    onValueChange = { onToggle() }
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = title
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(resolvedIconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedIconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = isChecked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = Emerald,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    iconBackground: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified,
    title: String,
    subtitle: String,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val resolvedIconBackground = if (iconBackground == Color.Unspecified) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        iconBackground
    }
    val resolvedIconTint = if (iconTint == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        iconTint
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(role = Role.Button) { onClick() }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = title
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(resolvedIconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedIconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (titleColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else titleColor
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SettingsInfoItem(
    icon: ImageVector,
    iconBackground: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified,
    title: String,
    value: String
) {
    val resolvedIconBackground = if (iconBackground == Color.Unspecified) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        iconBackground
    }
    val resolvedIconTint = if (iconTint == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        iconTint
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(resolvedIconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedIconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================================
// DIALOGS
// ============================================================================

@Composable
fun LocationInputDialog(
    currentLocation: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentLocation) }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_change_location_title),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.settings_city_name_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (text.isNotBlank()) onConfirm(text)
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald,
                    focusedLabelColor = Emerald,
                    cursorColor = Emerald
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text) }
            ) {
                Text(
                    text = stringResource(R.string.common_save),
                    color = Emerald,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun TimeFormatSelectionDialog(
    currentFormat: TimeFormatOption,
    onSelect: (TimeFormatOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_time_format_title),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                TimeFormatOption.entries.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(format) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = format == currentFormat,
                            onClick = { onSelect(format) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Emerald
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = format.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = format.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun NotificationSoundSelectionDialog(
    currentOption: NotificationScheduler.NotificationSoundOption,
    onSelect: (NotificationScheduler.NotificationSoundOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_sound_title),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                NotificationScheduler.NotificationSoundOption.values().forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == currentOption,
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Emerald
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = option.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = option.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AccountSecurityDialog(
    userEmail: String,
    isEmailVerified: Boolean,
    isRefreshingStatus: Boolean,
    isSendingVerificationEmail: Boolean,
    verificationResendCooldownSeconds: Int,
    isSendingResetPasswordEmail: Boolean,
    onRefreshStatus: () -> Unit,
    onSendVerificationEmail: () -> Unit,
    onSendResetPasswordEmail: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    val isActionRunning = isRefreshingStatus || isSendingVerificationEmail || isSendingResetPasswordEmail
    val isVerificationCooldown = verificationResendCooldownSeconds > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_account_security_title),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (userEmail.isBlank()) {
                        stringResource(R.string.settings_security_account_missing)
                    } else {
                        stringResource(R.string.settings_security_email_format, userEmail)
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEmailVerified) {
                            stringResource(R.string.settings_security_status_verified)
                        } else {
                            stringResource(R.string.settings_security_status_unverified)
                        },
                        fontSize = 13.sp,
                        color = if (isEmailVerified) Emerald else WarningAmber
                    )
                    TextButton(
                        onClick = onRefreshStatus,
                        enabled = !isActionRunning
                    ) {
                        if (isRefreshingStatus) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_loading))
                        } else {
                            Text(stringResource(R.string.settings_refresh))
                        }
                    }
                }

                if (!isEmailVerified) {
                    OutlinedButton(
                        onClick = onSendVerificationEmail,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isActionRunning && !isVerificationCooldown
                    ) {
                        if (isSendingVerificationEmail) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_sending))
                        } else if (isVerificationCooldown) {
                            Text(
                                pluralStringResource(
                                    R.plurals.settings_resend_verification_cooldown,
                                    verificationResendCooldownSeconds,
                                    verificationResendCooldownSeconds
                                )
                            )
                        } else {
                            Text(stringResource(R.string.settings_resend_verification_email))
                        }
                    }
                }

                OutlinedButton(
                    onClick = onSendResetPasswordEmail,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActionRunning
                ) {
                    if (isSendingResetPasswordEmail) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_sending))
                    } else {
                        Text(stringResource(R.string.settings_send_reset_link))
                    }
                }

                HorizontalDivider(color = Gray200)

                OutlinedButton(
                    onClick = onDeleteAccount,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActionRunning,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Text(stringResource(R.string.settings_delete_account_permanent))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.settings_close), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ImportDataConfirmationDialog(
    selectedMode: ImportMode,
    isImporting: Boolean,
    onSelectMode: (ImportMode) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.settings_import_title),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.settings_import_dialog_desc),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ImportMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isImporting) { onSelectMode(mode) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMode == mode,
                            onClick = { onSelectMode(mode) },
                            enabled = !isImporting,
                            colors = RadioButtonDefaults.colors(selectedColor = Emerald)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = mode.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = mode.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isImporting) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_importing))
                } else {
                    Text(
                        text = stringResource(R.string.settings_import_action),
                        color = Emerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isImporting) {
                Text(text = stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ChangePasswordDialog(
    userEmail: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onForgotPassword: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.settings_change_password_title),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_change_password_dialog_desc),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text(stringResource(R.string.settings_old_password_label)) },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.settings_new_password_label)) },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.settings_confirm_new_password_label)) },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (!isSubmitting) {
                                onSubmit(oldPassword, newPassword, confirmPassword)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                )
                TextButton(
                    onClick = onForgotPassword,
                    enabled = !isSubmitting,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(text = stringResource(R.string.settings_forgot_password_send_email))
                }
                if (userEmail.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.settings_account_email_format, userEmail),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(oldPassword, newPassword, confirmPassword) },
                enabled = !isSubmitting &&
                    oldPassword.isNotBlank() &&
                    newPassword.isNotBlank() &&
                    confirmPassword.isNotBlank()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_saving))
                } else {
                    Text(
                        text = stringResource(R.string.common_save),
                        color = Emerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(text = stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_reset_title),
                fontWeight = FontWeight.SemiBold,
                color = ErrorRed
            )
        },
        text = {
            Text(
                text = stringResource(R.string.settings_reset_dialog_desc),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.settings_reset_action),
                    color = ErrorRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun DeleteAccountConfirmationDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.settings_delete_account_dialog_title),
                fontWeight = FontWeight.SemiBold,
                color = ErrorRed
            )
        },
        text = {
            Text(
                text = stringResource(R.string.settings_delete_account_dialog_desc),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_deleting))
                } else {
                    Text(
                        text = stringResource(R.string.settings_delete_action),
                        color = ErrorRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(text = stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ReAuthDialog(
    isDeleting: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.settings_reauth_dialog_title),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_reauth_dialog_desc),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.settings_password_label)) },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (password.isNotBlank() && !isDeleting) onConfirm(password) }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDeleting
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank() && !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_deleting))
                } else {
                    Text(
                        text = stringResource(R.string.settings_delete_account_action),
                        color = ErrorRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(text = stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

// ============================================================================
// PREVIEW
// ============================================================================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    HabitIslamiTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onShowLocationDialog = {},
            onHideLocationDialog = {},
            onSetLocation = {},
            onShowTimeFormatDialog = {},
            onHideTimeFormatDialog = {},
            onSetTimeFormat = {},
            onToggleDarkMode = {},
            onToggleNotification = {},
            onNotificationSoundClick = {},
            onHideNotificationSoundDialog = {},
            onSetNotificationSound = {},
            onToggleNotificationVibration = {},
            onChangePasswordClick = {},
            onHideChangePasswordDialog = {},
            onSubmitChangePassword = { _, _, _ -> },
            onForgotPasswordFromSettings = {},
            onAccountSecurityClick = {},
            onHideAccountSecurityDialog = {},
            onRefreshEmailVerificationStatus = {},
            onSendEmailVerificationFromSecurity = {},
            onSendPasswordResetFromSecurity = {},
            onDeleteAccountFromSecurity = {},
            onShowDeleteAccountConfirmation = {},
            onHideDeleteAccountConfirmation = {},
            onConfirmDeleteAccount = {},
            onImportDataClick = {},
            onHideImportConfirmationDialog = {},
            onSetImportMode = {},
            onConfirmImportData = {},
            onExportDataClick = {},
            onShowResetConfirmation = {},
            onHideResetConfirmation = {},
            onConfirmReset = {},
            onPrivacyPolicyClick = {},
            onTermsClick = {},
            onLoginClick = {},
            onLogoutClick = {}
        )
    }
}
