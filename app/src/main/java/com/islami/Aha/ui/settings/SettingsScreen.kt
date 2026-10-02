package com.islami.Aha.ui.settings

import androidx.compose.material3.MaterialTheme

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.islami.Aha.BuildConfig
import com.islami.Aha.R
import com.islami.Aha.ui.components.AhaLoadingOverlay
import com.islami.Aha.ui.components.AhaToastTone
import com.islami.Aha.ui.components.AhaToastHost
import com.islami.Aha.ui.theme.*
import com.islami.Aha.util.NotificationScheduler
import com.islami.Aha.ui.settings.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToLogin: (String?) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }
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
        toastMessage = uiState.snackbarMessage
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
        toastMessage = toastMessage,
        onToastDismissed = {
            toastMessage = null
            viewModel.clearSnackbar()
        },
        onNavigateBack = onNavigateBack,
        onShowLocationDialog = viewModel::showLocationDialog,
        onHideLocationDialog = viewModel::hideLocationDialog,
        onSetLocation = viewModel::setLocation,
        onShowTimeFormatDialog = viewModel::showTimeFormatDialog,
        onHideTimeFormatDialog = viewModel::hideTimeFormatDialog,
        onSetTimeFormat = viewModel::setTimeFormat,
        onShowThemeModeDialog = viewModel::showThemeModeDialog,
        onHideThemeModeDialog = viewModel::hideThemeModeDialog,
        onSetThemeMode = viewModel::setThemeMode,
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
                onNavigateToLogin(null)
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
        onLoginClick = { onNavigateToLogin(null) },
        onLogoutClick = {
            viewModel.logout {
                onNavigateToLogin(context.getString(R.string.auth_logout_success_snackbar))
            }
        },
        onHideReAuthDialog = viewModel::hideReAuthDialog,
        onConfirmReAuthDelete = { password ->
            viewModel.confirmReAuthDelete(password) {
                onNavigateToLogin(null)
            }
        },
        onShowGenderDialog = viewModel::showGenderDialog,
        onHideGenderDialog = viewModel::hideGenderDialog,
        onSetGenderProfile = viewModel::setGenderProfile,
        onFixNotificationClick = {
            if (!com.islami.Aha.util.AutoStartHelper.isIgnoringBatteryOptimizations(context)) {
                android.widget.Toast.makeText(context, context.getString(R.string.settings_battery_opt_prompt), android.widget.Toast.LENGTH_LONG).show()
                com.islami.Aha.util.AutoStartHelper.requestIgnoreBatteryOptimizations(context)
            } else {
                val success = com.islami.Aha.util.AutoStartHelper.openAutoStartSettings(context)
                if (!success) {
                    android.widget.Toast.makeText(context, context.getString(R.string.settings_battery_already_optimal), android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, context.getString(R.string.settings_autostart_prompt), android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    toastMessage: String?,
    onToastDismissed: () -> Unit,
    onNavigateBack: () -> Unit,
    onShowLocationDialog: () -> Unit,
    onHideLocationDialog: () -> Unit,
    onSetLocation: (String) -> Unit,
    onShowTimeFormatDialog: () -> Unit,
    onHideTimeFormatDialog: () -> Unit,
    onSetTimeFormat: (TimeFormatOption) -> Unit,
    onShowThemeModeDialog: () -> Unit,
    onHideThemeModeDialog: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onShowGenderDialog: () -> Unit,
    onHideGenderDialog: () -> Unit,
    onSetGenderProfile: (com.islami.Aha.util.GenderProfile) -> Unit,
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
    onConfirmReAuthDelete: (String) -> Unit = {},
    onFixNotificationClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0).only(WindowInsetsSides.Horizontal),
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

            // Mode Tema
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsClickableItem(
                    icon = Icons.Outlined.DarkMode,
                    iconBackground = CategoryPuasaStart.copy(alpha = 0.1f),
                    iconTint = CategoryPuasaStart,
                    title = stringResource(R.string.settings_dark_mode_title),
                    subtitle = uiState.themeMode.displayName,
                    onClick = onShowThemeModeDialog
                )
            }

            // Profil Ibadah (Gender)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                val genderText = when (uiState.genderProfile) {
                    com.islami.Aha.util.GenderProfile.MALE -> stringResource(R.string.settings_gender_male_active)
                    com.islami.Aha.util.GenderProfile.FEMALE -> stringResource(R.string.settings_gender_female_active)
                    else -> stringResource(R.string.settings_gender_not_set)
                }
                SettingsClickableItem(
                    icon = Icons.Outlined.Person,
                    iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.settings_gender_title),
                    subtitle = genderText,
                    onClick = {
                        if (uiState.isHaidhMode) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.settings_gender_haidh_warning),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            onShowGenderDialog()
                        }
                    }
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

            // Removed AutoStart per user request

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

        AhaToastHost(
            message = toastMessage,
            tone = if (
                toastMessage == stringResource(R.string.auth_logout_success_snackbar)
            ) AhaToastTone.SUCCESS else AhaToastTone.AUTO,
            onDismissed = onToastDismissed,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        )

        AhaLoadingOverlay(
            visible = uiState.isLoggingOut,
            message = stringResource(R.string.settings_logout_loading)
        )
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

    if (uiState.showThemeModeDialog) {
        ThemeModeSelectionDialog(
            currentMode = uiState.themeMode,
            onSelect = onSetThemeMode,
            onDismiss = onHideThemeModeDialog
        )
    }

    if (uiState.showGenderDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = onHideGenderDialog) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.settings_gender_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val descText = when (uiState.genderProfile) {
                        com.islami.Aha.util.GenderProfile.FEMALE -> stringResource(R.string.settings_gender_desc_female)
                        com.islami.Aha.util.GenderProfile.MALE -> stringResource(R.string.settings_gender_desc_male)
                        else -> stringResource(R.string.settings_gender_desc_default)
                    }
                    Text(
                        text = descText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.85f)
                                .clickable { onSetGenderProfile(com.islami.Aha.util.GenderProfile.MALE) },
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = if (uiState.genderProfile == com.islami.Aha.util.GenderProfile.MALE) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (uiState.genderProfile == com.islami.Aha.util.GenderProfile.MALE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_gender_male),
                                    contentDescription = stringResource(R.string.settings_gender_male),
                                    modifier = Modifier.size(56.dp),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.settings_gender_male),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.85f)
                                .clickable { onSetGenderProfile(com.islami.Aha.util.GenderProfile.FEMALE) },
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = if (uiState.genderProfile == com.islami.Aha.util.GenderProfile.FEMALE) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (uiState.genderProfile == com.islami.Aha.util.GenderProfile.FEMALE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_gender_female),
                                    contentDescription = stringResource(R.string.settings_gender_female),
                                    modifier = Modifier.size(56.dp),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.settings_gender_female),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    androidx.compose.material3.TextButton(
                        onClick = onHideGenderDialog,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
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



// ============================================================================
// PREVIEW
// ============================================================================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    HabitIslamiTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(),
            toastMessage = null,
            onToastDismissed = {},
            onNavigateBack = {},
            onShowLocationDialog = {},
            onHideLocationDialog = {},
            onSetLocation = {},
            onShowTimeFormatDialog = {},
            onHideTimeFormatDialog = {},
            onSetTimeFormat = {},
            onShowThemeModeDialog = {},
            onHideThemeModeDialog = {},
            onSetThemeMode = {},
            onShowGenderDialog = {},
            onHideGenderDialog = {},
            onSetGenderProfile = {},
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
