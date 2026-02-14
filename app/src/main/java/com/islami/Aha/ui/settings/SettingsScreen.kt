package com.islami.Aha.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.islami.Aha.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    SettingsScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onShowLanguageDialog = viewModel::showLanguageDialog,
        onHideLanguageDialog = viewModel::hideLanguageDialog,
        onSetLanguage = viewModel::setLanguage,
        onShowLocationDialog = viewModel::showLocationDialog,
        onHideLocationDialog = viewModel::hideLocationDialog,
        onSetLocation = viewModel::setLocation,
        onShowTimeFormatDialog = viewModel::showTimeFormatDialog,
        onHideTimeFormatDialog = viewModel::hideTimeFormatDialog,
        onSetTimeFormat = viewModel::setTimeFormat,
        onToggleDarkMode = viewModel::toggleDarkMode,
        onToggleNotification = viewModel::toggleNotification,
        onNotificationSoundClick = viewModel::onNotificationSoundClick,
        onChangePasswordClick = viewModel::onChangePasswordClick,
        onAccountSecurityClick = viewModel::onAccountSecurityClick,
        onExportDataClick = viewModel::onExportDataClick,
        onShowResetConfirmation = viewModel::showResetConfirmation,
        onHideResetConfirmation = viewModel::hideResetConfirmation,
        onConfirmReset = viewModel::confirmResetData,
        onPrivacyPolicyClick = viewModel::onPrivacyPolicyClick,
        onTermsClick = viewModel::onTermsClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onShowLanguageDialog: () -> Unit,
    onHideLanguageDialog: () -> Unit,
    onSetLanguage: (LanguageOption) -> Unit,
    onShowLocationDialog: () -> Unit,
    onHideLocationDialog: () -> Unit,
    onSetLocation: (String) -> Unit,
    onShowTimeFormatDialog: () -> Unit,
    onHideTimeFormatDialog: () -> Unit,
    onSetTimeFormat: (TimeFormatOption) -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleNotification: () -> Unit,
    onNotificationSoundClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onAccountSecurityClick: () -> Unit,
    onExportDataClick: () -> Unit,
    onShowResetConfirmation: () -> Unit,
    onHideResetConfirmation: () -> Unit,
    onConfirmReset: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsClick: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0).only(WindowInsetsSides.Horizontal),
        topBar = {
            SettingsHeader(onNavigateBack = onNavigateBack)
        },
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
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // =============================================================
            // SECTION: UMUM
            // =============================================================
            item {
                SettingsSectionHeader(title = "Umum")
            }

            // Bahasa
            item {
                SettingsClickableItem(
                    icon = Icons.Outlined.Language,
                    iconBackground = InfoBlue.copy(alpha = 0.1f),

                    iconTint = InfoBlue,
                    title = "Bahasa",
                    subtitle = uiState.selectedLanguage.displayName,
                    onClick = onShowLanguageDialog
                )
            }

            // Format Waktu
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsClickableItem(
                    icon = Icons.Outlined.Schedule,
                    iconBackground = WarningAmber.copy(alpha = 0.1f),
                    iconTint = WarningAmber,
                    title = "Format Waktu",
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
                    title = "Mode Gelap",
                    subtitle = "Gunakan tema gelap",
                    isChecked = uiState.darkModeEnabled,
                    onToggle = onToggleDarkMode
                )
            }

            // =============================================================
            // SECTION: NOTIFIKASI
            // =============================================================
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = "Notifikasi")
            }

            // Pengingat Ibadah
            item {
                SettingsToggleItem(
                    icon = Icons.Outlined.Notifications,
                    iconBackground = EmeraldLight,
                    iconTint = Emerald,
                    title = "Pengingat Ibadah",
                    subtitle = "Terima notifikasi pengingat",
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
                    title = "Suara Notifikasi",
                    subtitle = uiState.notificationSound,
                    onClick = onNotificationSoundClick
                )
            }

            // =============================================================
            // SECTION: PRIVASI & KEAMANAN
            // =============================================================
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = "Privasi & Keamanan")
            }

            // Ubah Password
            item {
                SettingsClickableItem(
                    icon = Icons.Outlined.Lock,
                    iconBackground = WarningAmber.copy(alpha = 0.1f),
                    iconTint = WarningAmber,
                    title = "Ubah Password",
                    subtitle = "Ganti password akun",
                    onClick = onChangePasswordClick
                )
            }

            // Keamanan Akun
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsClickableItem(
                    icon = Icons.Outlined.Shield,
                    iconBackground = EmeraldLight,
                    iconTint = Emerald,
                    title = "Keamanan Akun",
                    subtitle = "Verifikasi 2 langkah",
                    onClick = onAccountSecurityClick
                )
            }

            // =============================================================
            // SECTION: DATA
            // =============================================================
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = "Data")
            }

            // Ekspor Data
            item {
                SettingsClickableItem(
                    icon = Icons.Outlined.Upload,
                    iconBackground = InfoBlue.copy(alpha = 0.1f),
                    iconTint = InfoBlue,
                    title = "Ekspor Data",
                    subtitle = "Backup data kebiasaan",
                    onClick = onExportDataClick
                )
            }

            // Reset Data
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsClickableItem(
                    icon = Icons.Outlined.DeleteForever,
                    iconBackground = ErrorRed.copy(alpha = 0.1f),
                    iconTint = ErrorRed,
                    title = "Reset Data",
                    subtitle = "Hapus semua data",
                    titleColor = ErrorRed,
                    onClick = onShowResetConfirmation
                )
            }

            // =============================================================
            // SECTION: TENTANG
            // =============================================================
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = "Tentang")
            }

            // Versi Aplikasi
            item {
                SettingsInfoItem(
                    icon = Icons.Outlined.Info,
                    iconBackground = Gray100,
                    iconTint = Gray500,
                    title = "Versi Aplikasi",
                    value = "1.0.0"
                )
            }

            // Kebijakan Privasi
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsClickableItem(
                    icon = Icons.Outlined.PrivacyTip,
                    iconBackground = EmeraldLight,
                    iconTint = Emerald,
                    title = "Kebijakan Privasi",
                    subtitle = "Baca kebijakan privasi",
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
                    title = "Syarat & Ketentuan",
                    subtitle = "Baca syarat penggunaan",
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

    if (uiState.showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = uiState.selectedLanguage,
            onSelect = onSetLanguage,
            onDismiss = onHideLanguageDialog
        )
    }

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

    if (uiState.showResetConfirmation) {
        ResetConfirmationDialog(
            onConfirm = onConfirmReset,
            onDismiss = onHideResetConfirmation
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
                .heightIn(min = 90.dp)
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
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Text(
                    text = "Pengaturan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
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
        color = Gray500,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    iconBackground: Color = EmeraldLight,
    iconTint: Color = Emerald,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
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
                    color = Gray500
                )
            }

            Switch(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = Emerald,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedTrackColor = Gray300
                )
            )
        }
    }
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    iconBackground: Color = EmeraldLight,
    iconTint: Color = Emerald,
    title: String,
    subtitle: String,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
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
                    color = Gray500
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Gray400,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SettingsInfoItem(
    icon: ImageVector,
    iconBackground: Color = EmeraldLight,
    iconTint: Color = Emerald,
    title: String,
    value: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
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
                color = Gray500
            )
        }
    }
}

// ============================================================================
// DIALOGS
// ============================================================================

@Composable
fun LanguageSelectionDialog(
    currentLanguage: LanguageOption,
    onSelect: (LanguageOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pilih Bahasa",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                LanguageOption.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(language) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == currentLanguage,
                            onClick = { onSelect(language) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Emerald
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = language.displayName,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal", color = Gray500)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

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
                text = "Ubah Lokasi",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Nama Kota") },
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
                    text = "Simpan",
                    color = Emerald,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal", color = Gray500)
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
                text = "Format Waktu",
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
                                color = Gray500
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal", color = Gray500)
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
                text = "Reset Data",
                fontWeight = FontWeight.SemiBold,
                color = ErrorRed
            )
        },
        text = {
            Text(
                text = "Apakah Anda yakin ingin menghapus semua data kebiasaan? Tindakan ini tidak dapat dibatalkan.",
                fontSize = 14.sp,
                color = Gray700
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Reset",
                    color = ErrorRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal", color = Gray500)
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
            onShowLanguageDialog = {},
            onHideLanguageDialog = {},
            onSetLanguage = {},
            onShowLocationDialog = {},
            onHideLocationDialog = {},
            onSetLocation = {},
            onShowTimeFormatDialog = {},
            onHideTimeFormatDialog = {},
            onSetTimeFormat = {},
            onToggleDarkMode = {},
            onToggleNotification = {},
            onNotificationSoundClick = {},
            onChangePasswordClick = {},
            onAccountSecurityClick = {},
            onExportDataClick = {},
            onShowResetConfirmation = {},
            onHideResetConfirmation = {},
            onConfirmReset = {},
            onPrivacyPolicyClick = {},
            onTermsClick = {}
        )
    }
}
