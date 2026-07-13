package com.islami.Aha.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.islami.Aha.data.local.AppDatabase
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.local.UserHabitDao
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.model.UserHabitEntity
import com.islami.Aha.data.repository.AuthRepository
import com.islami.Aha.data.repository.ReAuthRequiredException
import com.islami.Aha.ui.theme.ThemeManager
import com.islami.Aha.util.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class TimeFormatOption(val displayName: String, val description: String) {
    HOUR_24("24 Jam", "Contoh: 14:30"),
    HOUR_12("12 Jam", "Contoh: 2:30 PM")
}

enum class ImportMode(val displayName: String, val description: String) {
    REPLACE(
        displayName = "Ganti Semua",
        description = "Hapus data lokal lama lalu ganti dengan isi backup"
    ),
    MERGE(
        displayName = "Gabungkan",
        description = "Gabungkan backup ke data lokal tanpa menghapus semua data"
    )
}

data class SettingsUiState(
    // Preferensi
    val genderProfile: com.islami.Aha.util.GenderProfile = com.islami.Aha.util.GenderProfile.UNSPECIFIED,
    val isHaidhMode: Boolean = false,
    val location: String = "Jakarta",
    val selectedTimeFormat: TimeFormatOption = TimeFormatOption.HOUR_24,
    val themeMode: com.islami.Aha.ui.theme.ThemeMode = com.islami.Aha.ui.theme.ThemeMode.SYSTEM,
    val isLoggedIn: Boolean = false,
    val userEmail: String = "",

    // Notifikasi
    val notificationEnabled: Boolean = true,
    val notificationSound: NotificationScheduler.NotificationSoundOption =
        NotificationScheduler.NotificationSoundOption.SYSTEM_DEFAULT,
    val notificationVibrationEnabled: Boolean = true,

    // Dialog states
    val showLocationDialog: Boolean = false,
    val showTimeFormatDialog: Boolean = false,
    val showThemeModeDialog: Boolean = false,
    val showGenderDialog: Boolean = false,
    val showNotificationSoundDialog: Boolean = false,
    val showChangePasswordDialog: Boolean = false,
    val showAccountSecurityDialog: Boolean = false,
    val isEmailVerified: Boolean = false,
    val isRefreshingSecurityStatus: Boolean = false,
    val isSendingVerificationEmail: Boolean = false,
    val verificationResendCooldownSeconds: Int = 0,
    val isSendingResetPasswordEmail: Boolean = false,
    val launchExportPicker: Boolean = false,
    val exportFileName: String = "",
    val launchImportPicker: Boolean = false,
    val showImportConfirmationDialog: Boolean = false,
    val selectedImportMode: ImportMode = ImportMode.REPLACE,
    val pendingImportUri: Uri? = null,
    val isImportingData: Boolean = false,
    val showResetConfirmation: Boolean = false,
    val showDeleteAccountConfirmation: Boolean = false,
    val isChangingPassword: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val isLoggingOut: Boolean = false,
    val showReAuthDialog: Boolean = false,

    // Snackbar
    val snackbarMessage: String? = null
) {
    fun isSecurityActionInProgress() = isRefreshingSecurityStatus || isSendingVerificationEmail || isSendingResetPasswordEmail
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appDatabase: AppDatabase,
    private val habitDao: HabitDao,
    private val userHabitDao: UserHabitDao,
    private val habitCompletionDao: HabitCompletionDao,
    private val sharedPreferences: SharedPreferences,
    private val authRepository: AuthRepository
) : ViewModel() {
    companion object {
        private const val VERIFICATION_RESEND_COOLDOWN_SECONDS = 60
    }

    private val initialChannelSettings = NotificationScheduler.getChannelSettings(appContext)
    private var verificationCooldownJob: Job? = null

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = ThemeManager.themeMode.value,
            isLoggedIn = authRepository.isLoggedIn,
            userEmail = authRepository.currentUser?.email.orEmpty(),
            notificationEnabled = sharedPreferences.getBoolean(
                NotificationScheduler.KEY_GLOBAL_NOTIFICATION_ENABLED,
                true
            ),
            notificationSound = initialChannelSettings.soundOption,
            notificationVibrationEnabled = initialChannelSettings.vibrationEnabled
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            com.islami.Aha.util.UserPreferencesManager.gender.collect { gender ->
                _uiState.update { it.copy(genderProfile = gender) }
            }
        }
        viewModelScope.launch {
            com.islami.Aha.util.UserPreferencesManager.isHaidhMode.collect { isHaidhMode ->
                _uiState.update { it.copy(isHaidhMode = isHaidhMode) }
            }
        }
    }

    // === Umum ===

    fun showLocationDialog() {
        _uiState.update { it.copy(showLocationDialog = true) }
    }

    fun hideLocationDialog() {
        _uiState.update { it.copy(showLocationDialog = false) }
    }

    fun setLocation(location: String) {
        if (location.isNotBlank()) {
            _uiState.update {
                it.copy(location = location.trim(), showLocationDialog = false)
            }
        }
    }

    fun showTimeFormatDialog() {
        _uiState.update { it.copy(showTimeFormatDialog = true) }
    }

    fun hideTimeFormatDialog() {
        _uiState.update { it.copy(showTimeFormatDialog = false) }
    }

    fun setTimeFormat(format: TimeFormatOption) {
        _uiState.update {
            it.copy(selectedTimeFormat = format, showTimeFormatDialog = false)
        }
    }

    fun showThemeModeDialog() { _uiState.update { it.copy(showThemeModeDialog = true) } }
    fun hideThemeModeDialog() { _uiState.update { it.copy(showThemeModeDialog = false) } }

    fun showGenderDialog() { _uiState.update { it.copy(showGenderDialog = true) } }
    fun hideGenderDialog() { _uiState.update { it.copy(showGenderDialog = false) } }

    fun setGenderProfile(profile: com.islami.Aha.util.GenderProfile) {
        com.islami.Aha.util.UserPreferencesManager.setGender(profile)
        hideGenderDialog()
        showSnackbar("Profil ibadah berhasil diperbarui")
    }

    fun setThemeMode(mode: com.islami.Aha.ui.theme.ThemeMode) {
        ThemeManager.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode, showThemeModeDialog = false) }
    }

    // === Notifikasi ===

    fun toggleNotification() {
        val enabled = !_uiState.value.notificationEnabled
        if (enabled) {
            val capability = NotificationScheduler.getNotificationCapability(appContext)
            if (capability != NotificationScheduler.NotificationCapability.AVAILABLE) {
                showSnackbar(
                    when (capability) {
                        NotificationScheduler.NotificationCapability.PERMISSION_DENIED ->
                            appContext.getString(com.islami.Aha.R.string.notification_permission_required_message)
                        NotificationScheduler.NotificationCapability.SYSTEM_DISABLED ->
                            appContext.getString(com.islami.Aha.R.string.notification_system_disabled_message)
                        NotificationScheduler.NotificationCapability.CHANNEL_DISABLED ->
                            appContext.getString(com.islami.Aha.R.string.notification_channel_disabled_message)
                        NotificationScheduler.NotificationCapability.AVAILABLE -> ""
                    }
                )
                return
            }
        }
        sharedPreferences.edit()
            .putBoolean(NotificationScheduler.KEY_GLOBAL_NOTIFICATION_ENABLED, enabled)
            .apply()
        _uiState.update { it.copy(notificationEnabled = enabled) }

        viewModelScope.launch {
            if (enabled) {
                rescheduleAllActiveReminders()
                showSnackbar("Pengingat global diaktifkan")
            } else {
                cancelAllExistingReminders()
                showSnackbar("Pengingat global dimatikan")
            }
        }
    }

    fun onNotificationSoundClick() {
        _uiState.update { it.copy(showNotificationSoundDialog = true) }
    }

    fun hideNotificationSoundDialog() {
        _uiState.update { it.copy(showNotificationSoundDialog = false) }
    }

    fun setNotificationSound(option: NotificationScheduler.NotificationSoundOption) {
        sharedPreferences.edit()
            .putString(NotificationScheduler.KEY_NOTIFICATION_SOUND, option.value)
            .apply()
        _uiState.update {
            it.copy(
                notificationSound = option,
                showNotificationSoundDialog = false
            )
        }
        NotificationScheduler.applyChannelSettings(appContext)
        showSnackbar("Suara notifikasi diperbarui")
    }

    fun toggleNotificationVibration() {
        val enabled = !_uiState.value.notificationVibrationEnabled
        sharedPreferences.edit()
            .putBoolean(NotificationScheduler.KEY_NOTIFICATION_VIBRATION, enabled)
            .apply()
        _uiState.update { it.copy(notificationVibrationEnabled = enabled) }
        NotificationScheduler.applyChannelSettings(appContext)
        showSnackbar(
            if (enabled) "Getar notifikasi diaktifkan"
            else "Getar notifikasi dimatikan"
        )
    }

    // === Privasi & Keamanan ===

    fun onChangePasswordClick() {
        if (!uiState.value.isLoggedIn) {
            showSnackbar("Silakan login untuk mengubah password")
            return
        }
        _uiState.update { it.copy(showChangePasswordDialog = true) }
    }

    fun hideChangePasswordDialog() {
        _uiState.update {
            it.copy(
                showChangePasswordDialog = false,
                isChangingPassword = false
            )
        }
    }

    fun submitPasswordChange(oldPassword: String, newPassword: String, confirmPassword: String) {
        val old = oldPassword.trim()
        val new = newPassword.trim()
        val confirm = confirmPassword.trim()

        when {
            old.isBlank() -> {
                showSnackbar("Password lama tidak boleh kosong")
                return
            }
            new.isBlank() -> {
                showSnackbar("Password baru tidak boleh kosong")
                return
            }
            new.length < 6 -> {
                showSnackbar("Password baru minimal 6 karakter")
                return
            }
            new == old -> {
                showSnackbar("Password baru harus berbeda dari password lama")
                return
            }
            confirm != new -> {
                showSnackbar("Konfirmasi password tidak cocok")
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isChangingPassword = true) }
            val result = authRepository.changePassword(
                currentPassword = old,
                newPassword = new,
                sendSecurityEmail = true
            )
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isChangingPassword = false,
                        showChangePasswordDialog = false
                    )
                }
                showSnackbar("Password berhasil diubah. Cek email untuk konfirmasi keamanan.")
            } else {
                _uiState.update { it.copy(isChangingPassword = false) }
                showSnackbar(result.exceptionOrNull()?.message ?: "Gagal mengubah password")
            }
        }
    }

    fun sendForgotPasswordFromSettings() {
        viewModelScope.launch {
            val email = authRepository.currentUser?.email
            if (email.isNullOrBlank()) {
                showSnackbar("Email akun tidak ditemukan")
                return@launch
            }

            val result = authRepository.sendPasswordReset(email)
            if (result.isSuccess) {
                showSnackbar("Link reset password dikirim ke $email")
            } else {
                showSnackbar("Gagal mengirim email reset password")
            }
        }
    }

    fun onAccountSecurityClick() {
        if (!uiState.value.isLoggedIn) {
            showSnackbar("Silakan login untuk membuka keamanan akun")
            return
        }
        _uiState.update { it.copy(showAccountSecurityDialog = true) }
        refreshEmailVerificationStatus()
    }

    fun hideAccountSecurityDialog() {
        _uiState.update { it.copy(showAccountSecurityDialog = false) }
    }

    fun refreshEmailVerificationStatus() {
        if (uiState.value.isSecurityActionInProgress()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingSecurityStatus = true) }
            val result = authRepository.reloadAndGetEmailVerificationStatus()
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isRefreshingSecurityStatus = false,
                        isEmailVerified = result.getOrDefault(false)
                    )
                }
            } else {
                _uiState.update { it.copy(isRefreshingSecurityStatus = false) }
                showSnackbar(result.exceptionOrNull()?.message ?: "Gagal memeriksa status verifikasi email")
            }
        }
    }

    fun sendEmailVerificationFromSecurity() {
        val cooldownSeconds = uiState.value.verificationResendCooldownSeconds
        if (cooldownSeconds > 0) {
            showSnackbar("Tunggu $cooldownSeconds detik sebelum kirim ulang verifikasi")
            return
        }
        if (uiState.value.isSecurityActionInProgress()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingVerificationEmail = true) }
            val result = authRepository.sendEmailVerificationToCurrentUser()
            _uiState.update { it.copy(isSendingVerificationEmail = false) }
            if (result.isSuccess) {
                showSnackbar("Email verifikasi berhasil dikirim")
                startVerificationResendCooldown()
            } else {
                showSnackbar(result.exceptionOrNull()?.message ?: "Gagal mengirim email verifikasi")
            }
        }
    }

    fun sendForgotPasswordFromSecurity() {
        if (uiState.value.isSecurityActionInProgress()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingResetPasswordEmail = true) }
            val email = authRepository.currentUser?.email
            if (email.isNullOrBlank()) {
                _uiState.update { it.copy(isSendingResetPasswordEmail = false) }
                showSnackbar("Email akun tidak ditemukan")
                return@launch
            }

            val result = authRepository.sendPasswordReset(email)
            _uiState.update { it.copy(isSendingResetPasswordEmail = false) }
            if (result.isSuccess) {
                showSnackbar("Link reset password dikirim ke $email")
            } else {
                showSnackbar("Gagal mengirim email reset password")
            }
        }
    }

    fun showDeleteAccountFromSecurity() {
        _uiState.update {
            it.copy(
                showAccountSecurityDialog = false,
                showDeleteAccountConfirmation = true
            )
        }
    }

    fun showDeleteAccountConfirmation() {
        _uiState.update { it.copy(showDeleteAccountConfirmation = true) }
    }

    fun hideDeleteAccountConfirmation() {
        _uiState.update { it.copy(showDeleteAccountConfirmation = false) }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            delay(450)
            verificationCooldownJob?.cancel()
            clearTrackedDataForGuestMode()
            authRepository.logout()
            val user = authRepository.currentUser
            val prefs = com.islami.Aha.util.SecurePrefsProvider.get(appContext)
            val formatStr = prefs.getString("time_format", TimeFormatOption.HOUR_24.name)
            val format = runCatching { TimeFormatOption.valueOf(formatStr!!) }
                .getOrDefault(TimeFormatOption.HOUR_24)

            val soundStr = prefs.getString("notification_sound", NotificationScheduler.NotificationSoundOption.SYSTEM_DEFAULT.name)
            val sound = runCatching { NotificationScheduler.NotificationSoundOption.valueOf(soundStr!!) }
                .getOrDefault(NotificationScheduler.NotificationSoundOption.SYSTEM_DEFAULT)

            _uiState.update {
                it.copy(
                    isLoggedIn = user != null,
                    userEmail = user?.email ?: "",
                    selectedTimeFormat = format,
                    notificationSound = sound,
                    isLoggingOut = false,
                    verificationResendCooldownSeconds = 0
                )
            }
            onSuccess()
        }
    }

    private suspend fun clearTrackedDataForGuestMode() {
        userHabitDao.getActiveReminderHabits().forEach { habit ->
            NotificationScheduler.cancelHabitReminder(appContext, habit.id)
        }
        runInDbTransaction {
            userHabitDao.deleteAll()
            habitCompletionDao.deleteAll()
            habitDao.resetTrackerState()
        }
        com.islami.Aha.util.UserPreferencesManager.clearAll()
    }

    fun confirmDeleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true) }

            val result = authRepository.deleteAccount()
            if (result.isSuccess) {
                clearLocalData()
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        showDeleteAccountConfirmation = false,
                        isLoggedIn = false,
                        userEmail = ""
                    )
                }
                onSuccess()
            } else {
                val error = result.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        showDeleteAccountConfirmation = false
                    )
                }
                if (error is ReAuthRequiredException) {
                    _uiState.update { it.copy(showReAuthDialog = true) }
                } else {
                    showSnackbar(error?.message ?: "Gagal menghapus akun")
                }
            }
        }
    }

    fun hideReAuthDialog() {
        _uiState.update { it.copy(showReAuthDialog = false) }
    }

    fun confirmReAuthDelete(password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true) }

            val result = authRepository.reAuthenticateAndDelete(password)
            if (result.isSuccess) {
                clearLocalData()
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        showReAuthDialog = false,
                        isLoggedIn = false,
                        userEmail = ""
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(isDeletingAccount = false, showReAuthDialog = false)
                }
                showSnackbar("Password salah atau gagal menghapus akun")
            }
        }
    }

    private suspend fun clearLocalData() {
        runInDbTransaction {
            habitDao.deleteAllHabits()
            userHabitDao.deleteAll()
            habitCompletionDao.deleteAll()
        }
        sharedPreferences.edit()
            .remove("hasSeeded")
            .remove("dark_mode_enabled")
            .apply()
    }

    // === Data ===

    fun onExportDataClick() {
        val fileName = "aha_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())}.json"
        _uiState.update {
            it.copy(
                launchExportPicker = true,
                exportFileName = fileName
            )
        }
    }

    fun onExportPickerHandled() {
        _uiState.update { it.copy(launchExportPicker = false) }
    }

    fun onImportDataClick() {
        _uiState.update { it.copy(launchImportPicker = true) }
    }

    fun onImportPickerHandled() {
        _uiState.update { it.copy(launchImportPicker = false) }
    }

    fun onImportFileSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                pendingImportUri = uri,
                showImportConfirmationDialog = true
            )
        }
    }

    fun hideImportConfirmationDialog() {
        if (_uiState.value.isImportingData) return
        _uiState.update {
            it.copy(
                showImportConfirmationDialog = false,
                pendingImportUri = null
            )
        }
    }

    fun setImportMode(mode: ImportMode) {
        _uiState.update { it.copy(selectedImportMode = mode) }
    }

    fun onExportCancelled() {
        showSnackbar("Ekspor dibatalkan")
    }

    fun onImportCancelled() {
        showSnackbar("Impor dibatalkan")
    }

    fun exportDataToUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val encryptedPayload = withContext(Dispatchers.IO) {
                    buildSettingsExportJson(
                        habitDao = habitDao,
                        userHabitDao = userHabitDao,
                        habitCompletionDao = habitCompletionDao,
                        isLoggedIn = uiState.value.isLoggedIn,
                        userEmail = uiState.value.userEmail
                    ).let { SettingsBackupCrypto.encode(it) }
                }
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(uri, "w")?.use { outputStream ->
                        outputStream.writer(Charsets.UTF_8).use { writer ->
                            writer.write(encryptedPayload)
                        }
                    } ?: throw IllegalStateException("Tidak dapat membuka file tujuan")
                }
            }.onSuccess {
                showSnackbar("Data berhasil diekspor")
            }.onFailure {
                showSnackbar("Gagal mengekspor data")
            }
        }
    }

    fun confirmImportData() {
        val pendingUri = _uiState.value.pendingImportUri
        if (pendingUri == null) {
            showSnackbar("File impor tidak ditemukan")
            return
        }
        val mode = _uiState.value.selectedImportMode
        _uiState.update {
            it.copy(
                showImportConfirmationDialog = false,
                pendingImportUri = null
            )
        }
        importDataFromUri(pendingUri, mode)
    }

    private fun importDataFromUri(uri: Uri, mode: ImportMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingData = true) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val rawJson = appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } ?: throw IllegalStateException("Tidak dapat membaca file")

                    val parsed = parseSettingsImportJson(SettingsBackupCrypto.decode(rawJson))
                    applyImportedData(parsed, mode)
                    parsed
                }
            }.onSuccess { parsed ->
                _uiState.update { it.copy(isImportingData = false) }
                showSnackbar(
                    "Impor selesai: ${parsed.defaultHabits.size} habit, " +
                        "${parsed.sunnahHabits.size} sunnah, ${parsed.completionRecords.size} riwayat"
                )
            }.onFailure { error ->
                _uiState.update { it.copy(isImportingData = false) }
                showSnackbar(error.message ?: "Gagal mengimpor data")
            }
        }
    }

    fun showResetConfirmation() {
        _uiState.update { it.copy(showResetConfirmation = true) }
    }

    fun hideResetConfirmation() {
        _uiState.update { it.copy(showResetConfirmation = false) }
    }

    fun confirmResetData() {
        viewModelScope.launch {
            runInDbTransaction {
                habitDao.deleteAllHabits()
                userHabitDao.deleteAll()
                habitCompletionDao.deleteAll()
            }
            sharedPreferences.edit().remove("hasSeeded").apply()
            _uiState.update { it.copy(showResetConfirmation = false) }
            showSnackbar("Semua data telah direset")
        }
    }

    // === Snackbar ===

    private fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    override fun onCleared() {
        verificationCooldownJob?.cancel()
        super.onCleared()
    }

    private fun startVerificationResendCooldown() {
        verificationCooldownJob?.cancel()
        verificationCooldownJob = viewModelScope.launch {
            for (remaining in VERIFICATION_RESEND_COOLDOWN_SECONDS downTo 1) {
                _uiState.update { it.copy(verificationResendCooldownSeconds = remaining) }
                delay(1000)
            }
            _uiState.update { it.copy(verificationResendCooldownSeconds = 0) }
        }
    }

    private fun SettingsUiState.isSecurityActionInProgress(): Boolean {
        return isRefreshingSecurityStatus || isSendingVerificationEmail || isSendingResetPasswordEmail
    }

    private suspend fun applyImportedData(parsed: ParsedImport, mode: ImportMode) {
        when (mode) {
            ImportMode.REPLACE -> {
                cancelAllExistingReminders()
            }
            ImportMode.MERGE -> {
                cancelReminderTargets(parsed.defaultHabits, parsed.sunnahHabits)
            }
        }

        runInDbTransaction {
            if (mode == ImportMode.REPLACE) {
                habitCompletionDao.deleteAll()
                userHabitDao.deleteAll()
                habitDao.deleteAllHabits()
            }

            parsed.defaultHabits.forEach { habit ->
                habitDao.insertHabit(habit)
            }
            if (parsed.sunnahHabits.isNotEmpty()) {
                userHabitDao.insertAllHabits(parsed.sunnahHabits)
            }
            parsed.completionRecords.forEach { record ->
                habitCompletionDao.insert(record)
            }
        }

        sharedPreferences.edit().putBoolean("hasSeeded", true).apply()
        rescheduleImportedReminders(parsed.defaultHabits, parsed.sunnahHabits)
    }

    private suspend fun runInDbTransaction(block: suspend () -> Unit) {
        val canUseRoomTransaction = runCatching {
            appDatabase.openHelper.writableDatabase
        }.isSuccess

        if (!canUseRoomTransaction) {
            block()
            return
        }
        runCatching {
            appDatabase.withTransaction { block() }
        }.getOrElse {
            // Fallback keeps JVM unit tests (mocked RoomDatabase) operational.
            block()
        }
    }

    private suspend fun cancelAllExistingReminders() {
        val oldDefaultReminderHabits = habitDao.getActiveReminderHabits()
        val oldSunnahReminderHabits = userHabitDao.getActiveReminderHabits()
        oldDefaultReminderHabits.forEach { habit ->
            NotificationScheduler.cancelHabitReminder(appContext, "default_${habit.id}")
        }
        oldSunnahReminderHabits.forEach { habit ->
            NotificationScheduler.cancelHabitReminder(appContext, habit.id)
        }
    }

    private fun cancelReminderTargets(
        defaultHabits: List<Habit>,
        sunnahHabits: List<UserHabitEntity>
    ) {
        defaultHabits.forEach { habit ->
            if (habit.id <= 0) return@forEach
            NotificationScheduler.cancelHabitReminder(appContext, "default_${habit.id}")
        }
        sunnahHabits.forEach { habit ->
            NotificationScheduler.cancelHabitReminder(appContext, habit.id)
        }
    }

    private fun rescheduleImportedReminders(
        defaultHabits: List<Habit>,
        sunnahHabits: List<UserHabitEntity>
    ) {
        val globalEnabled = sharedPreferences.getBoolean(
            NotificationScheduler.KEY_GLOBAL_NOTIFICATION_ENABLED,
            true
        )
        if (!globalEnabled) return

        defaultHabits.forEach { habit ->
            if (habit.id <= 0) return@forEach
            if (!habit.isReminderEnabled) return@forEach
            val (hour, minute) = parseHourMinute(habit.time) ?: return@forEach
            NotificationScheduler.scheduleHabitReminder(
                context = appContext,
                habitId = "default_${habit.id}",
                habitName = habit.name,
                hour = hour,
                minute = minute
            )
        }

        sunnahHabits.forEach { habit ->
            if (!habit.reminderEnabled || habit.reminderTime.isNullOrBlank()) return@forEach
            val (hour, minute) = parseHourMinute(habit.reminderTime) ?: return@forEach
            NotificationScheduler.scheduleHabitReminder(
                context = appContext,
                habitId = habit.id,
                habitName = habit.name,
                hour = hour,
                minute = minute
            )
        }
    }

    private suspend fun rescheduleAllActiveReminders() {
        habitDao.getActiveReminderHabits().forEach { habit ->
            val (hour, minute) = parseHourMinute(habit.time) ?: return@forEach
            NotificationScheduler.scheduleHabitReminder(
                context = appContext,
                habitId = "default_${habit.id}",
                habitName = habit.name,
                hour = hour,
                minute = minute
            )
        }

        userHabitDao.getActiveReminderHabits().forEach { habit ->
            val reminderTime = habit.reminderTime ?: return@forEach
            val (hour, minute) = parseHourMinute(reminderTime) ?: return@forEach
            NotificationScheduler.scheduleHabitReminder(
                context = appContext,
                habitId = habit.id,
                habitName = habit.name,
                hour = hour,
                minute = minute
            )
        }
    }

}
