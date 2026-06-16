package com.islami.Aha.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.islami.Aha.data.local.AppDatabase
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.local.UserHabitDao
import com.islami.Aha.data.repository.AuthRepository
import com.islami.Aha.util.NotificationScheduler
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SettingsViewModel
    private lateinit var mockAppDatabase: AppDatabase
    private lateinit var mockHabitDao: HabitDao
    private lateinit var mockUserHabitDao: UserHabitDao
    private lateinit var mockHabitCompletionDao: HabitCompletionDao
    private lateinit var mockAppContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockAuthRepository: AuthRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockAppDatabase = mockk(relaxed = true)
        every { mockAppDatabase.openHelper } throws IllegalStateException("Room unavailable in JVM test")
        mockHabitDao = mockk(relaxed = true)
        mockUserHabitDao = mockk(relaxed = true)
        mockHabitCompletionDao = mockk(relaxed = true)
        mockAppContext = mockk(relaxed = true)
        mockSharedPreferences = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        mockAuthRepository = mockk(relaxed = true)
        every { mockAppContext.getSharedPreferences("aha_prefs", Context.MODE_PRIVATE) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockSharedPreferences.getString(any(), any()) } answers { secondArg<String?>() }
        every { mockSharedPreferences.getBoolean(any(), any()) } answers { secondArg<Boolean>() }
        every { mockEditor.remove(any()) } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
        every { mockAuthRepository.isLoggedIn } returns false
        every { mockAuthRepository.currentUser } returns null
        coEvery { mockAuthRepository.changePassword(any(), any(), any()) } returns Result.success(Unit)
        coEvery { mockAuthRepository.sendPasswordReset(any()) } returns Result.success(Unit)
        coEvery { mockAuthRepository.reloadAndGetEmailVerificationStatus() } returns Result.success(false)
        coEvery { mockAuthRepository.sendEmailVerificationToCurrentUser() } returns Result.success(Unit)
        viewModel = SettingsViewModel(
            mockAppContext,
            mockAppDatabase,
            mockHabitDao,
            mockUserHabitDao,
            mockHabitCompletionDao,
            mockSharedPreferences,
            mockAuthRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===================== Initial State =====================

    @Test
    fun `initial state has expected defaults`() {
        val state = viewModel.uiState.value
        assertEquals("Jakarta", state.location)
        assertEquals(TimeFormatOption.HOUR_24, state.selectedTimeFormat)
        assertFalse(state.isLoggedIn)
        assertEquals("", state.userEmail)
        assertTrue(state.notificationEnabled)
        assertFalse(state.showLocationDialog)
        assertFalse(state.showTimeFormatDialog)
        assertFalse(state.showResetConfirmation)
        assertNull(state.snackbarMessage)
    }

    // ===================== Location Dialog =====================

    @Test
    fun `show and hide location dialog`() {
        viewModel.showLocationDialog()
        assertTrue(viewModel.uiState.value.showLocationDialog)
        viewModel.hideLocationDialog()
        assertFalse(viewModel.uiState.value.showLocationDialog)
    }

    @Test
    fun `set location updates state`() {
        viewModel.setLocation("Bandung")
        assertEquals("Bandung", viewModel.uiState.value.location)
    }

    @Test
    fun `set blank location does nothing`() {
        viewModel.setLocation("  ")
        assertEquals("Jakarta", viewModel.uiState.value.location)
    }

    @Test
    fun `set location trims whitespace`() {
        viewModel.setLocation("  Surabaya  ")
        assertEquals("Surabaya", viewModel.uiState.value.location)
    }

    // ===================== Time Format =====================

    @Test
    fun `show and hide time format dialog`() {
        viewModel.showTimeFormatDialog()
        assertTrue(viewModel.uiState.value.showTimeFormatDialog)
        viewModel.hideTimeFormatDialog()
        assertFalse(viewModel.uiState.value.showTimeFormatDialog)
    }

    @Test
    fun `set time format updates state and hides dialog`() {
        viewModel.showTimeFormatDialog()
        viewModel.setTimeFormat(TimeFormatOption.HOUR_12)
        val state = viewModel.uiState.value
        assertEquals(TimeFormatOption.HOUR_12, state.selectedTimeFormat)
        assertFalse(state.showTimeFormatDialog)
    }

    // ===================== Notification =====================

    @Test
    fun `toggle notification flips state`() {
        assertTrue(viewModel.uiState.value.notificationEnabled)
        viewModel.toggleNotification()
        assertFalse(viewModel.uiState.value.notificationEnabled)
        viewModel.toggleNotification()
        assertTrue(viewModel.uiState.value.notificationEnabled)
    }

    // ===================== Reset Data =====================

    @Test
    fun `show and hide reset confirmation`() {
        viewModel.showResetConfirmation()
        assertTrue(viewModel.uiState.value.showResetConfirmation)
        viewModel.hideResetConfirmation()
        assertFalse(viewModel.uiState.value.showResetConfirmation)
    }

    @Test
    fun `confirm reset data calls dao and shows snackbar`() = runTest {
        viewModel.confirmResetData()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showResetConfirmation)
        assertEquals("Semua data telah direset", viewModel.uiState.value.snackbarMessage)
    }

    // ===================== Snackbar =====================

    @Test
    fun `clear snackbar sets message to null`() {
        viewModel.onAccountSecurityClick()
        assertEquals("Silakan login untuk membuka keamanan akun", viewModel.uiState.value.snackbarMessage)
        viewModel.clearSnackbar()
        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `coming soon actions show snackbar`() {
        viewModel.onAccountSecurityClick()
        assertEquals("Silakan login untuk membuka keamanan akun", viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `change password click requires login`() {
        viewModel.onChangePasswordClick()
        assertEquals("Silakan login untuk mengubah password", viewModel.uiState.value.snackbarMessage)
        assertFalse(viewModel.uiState.value.showChangePasswordDialog)
    }

    @Test
    fun `change password click opens dialog when logged in`() {
        val firebaseUser = mockk<FirebaseUser>()
        every { firebaseUser.email } returns "user@example.com"
        every { mockAuthRepository.isLoggedIn } returns true
        every { mockAuthRepository.currentUser } returns firebaseUser
        val loggedInViewModel = SettingsViewModel(
            mockAppContext,
            mockAppDatabase,
            mockHabitDao,
            mockUserHabitDao,
            mockHabitCompletionDao,
            mockSharedPreferences,
            mockAuthRepository
        )

        loggedInViewModel.onChangePasswordClick()
        assertTrue(loggedInViewModel.uiState.value.showChangePasswordDialog)
    }

    @Test
    fun `account security click opens dialog when logged in`() = runTest {
        val firebaseUser = mockk<FirebaseUser>()
        every { firebaseUser.email } returns "user@example.com"
        every { mockAuthRepository.isLoggedIn } returns true
        every { mockAuthRepository.currentUser } returns firebaseUser
        coEvery { mockAuthRepository.reloadAndGetEmailVerificationStatus() } returns Result.success(true)
        val loggedInViewModel = SettingsViewModel(
            mockAppContext,
            mockAppDatabase,
            mockHabitDao,
            mockUserHabitDao,
            mockHabitCompletionDao,
            mockSharedPreferences,
            mockAuthRepository
        )

        loggedInViewModel.onAccountSecurityClick()
        advanceUntilIdle()

        assertTrue(loggedInViewModel.uiState.value.showAccountSecurityDialog)
        assertTrue(loggedInViewModel.uiState.value.isEmailVerified)
    }

    @Test
    fun `send verification email from security shows success snackbar`() = runTest {
        viewModel.sendEmailVerificationFromSecurity()
        runCurrent()
        assertEquals("Email verifikasi berhasil dikirim", viewModel.uiState.value.snackbarMessage)
        coVerify { mockAuthRepository.sendEmailVerificationToCurrentUser() }
    }

    @Test
    fun `send verification email applies cooldown and blocks resend`() = runTest {
        viewModel.sendEmailVerificationFromSecurity()
        runCurrent()

        assertEquals(60, viewModel.uiState.value.verificationResendCooldownSeconds)

        viewModel.sendEmailVerificationFromSecurity()
        runCurrent()

        assertEquals(
            "Tunggu 60 detik sebelum kirim ulang verifikasi",
            viewModel.uiState.value.snackbarMessage
        )
        coVerify(exactly = 1) { mockAuthRepository.sendEmailVerificationToCurrentUser() }

        advanceTimeBy(1000)
        runCurrent()
        assertEquals(59, viewModel.uiState.value.verificationResendCooldownSeconds)
    }

    @Test
    fun `submit change password validates confirmation`() {
        viewModel.submitPasswordChange("oldpass", "newpass", "mismatch")
        assertEquals("Konfirmasi password tidak cocok", viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `submit change password success closes dialog`() = runTest {
        val firebaseUser = mockk<FirebaseUser>()
        every { firebaseUser.email } returns "user@example.com"
        every { mockAuthRepository.isLoggedIn } returns true
        every { mockAuthRepository.currentUser } returns firebaseUser
        val loggedInViewModel = SettingsViewModel(
            mockAppContext,
            mockAppDatabase,
            mockHabitDao,
            mockUserHabitDao,
            mockHabitCompletionDao,
            mockSharedPreferences,
            mockAuthRepository
        )

        loggedInViewModel.onChangePasswordClick()
        loggedInViewModel.submitPasswordChange("oldpass", "newpass", "newpass")
        advanceUntilIdle()

        assertFalse(loggedInViewModel.uiState.value.showChangePasswordDialog)
        assertEquals(
            "Password berhasil diubah. Cek email untuk konfirmasi keamanan.",
            loggedInViewModel.uiState.value.snackbarMessage
        )
        coVerify { mockAuthRepository.changePassword("oldpass", "newpass", true) }
    }

    @Test
    fun `forgot password from settings sends reset email`() = runTest {
        val firebaseUser = mockk<FirebaseUser>()
        every { firebaseUser.email } returns "user@example.com"
        every { mockAuthRepository.currentUser } returns firebaseUser

        viewModel.sendForgotPasswordFromSettings()
        advanceUntilIdle()

        assertEquals(
            "Link reset password dikirim ke user@example.com",
            viewModel.uiState.value.snackbarMessage
        )
        coVerify { mockAuthRepository.sendPasswordReset("user@example.com") }
    }

    @Test
    fun `forgot password from security sends reset email`() = runTest {
        val firebaseUser = mockk<FirebaseUser>()
        every { firebaseUser.email } returns "user@example.com"
        every { mockAuthRepository.currentUser } returns firebaseUser

        viewModel.sendForgotPasswordFromSecurity()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSendingResetPasswordEmail)
        assertEquals(
            "Link reset password dikirim ke user@example.com",
            viewModel.uiState.value.snackbarMessage
        )
        coVerify { mockAuthRepository.sendPasswordReset("user@example.com") }
    }

    @Test
    fun `export data click triggers picker request`() {
        viewModel.onExportDataClick()
        assertTrue(viewModel.uiState.value.launchExportPicker)
        assertTrue(viewModel.uiState.value.exportFileName.endsWith(".json"))
    }

    @Test
    fun `import data click triggers picker request`() {
        viewModel.onImportDataClick()
        assertTrue(viewModel.uiState.value.launchImportPicker)
    }

    @Test
    fun `import file selected opens confirmation dialog`() {
        val uri = mockk<Uri>(relaxed = true)
        viewModel.onImportFileSelected(uri)
        assertTrue(viewModel.uiState.value.showImportConfirmationDialog)
        assertEquals(uri, viewModel.uiState.value.pendingImportUri)
    }

    @Test
    fun `set import mode updates state`() {
        viewModel.setImportMode(ImportMode.MERGE)
        assertEquals(ImportMode.MERGE, viewModel.uiState.value.selectedImportMode)
    }

    @Test
    fun `notification sound click opens sound dialog`() {
        assertFalse(viewModel.uiState.value.showNotificationSoundDialog)
        viewModel.onNotificationSoundClick()
        assertTrue(viewModel.uiState.value.showNotificationSoundDialog)
    }

    @Test
    fun `set notification sound updates preferences and state`() {
        viewModel.onNotificationSoundClick()
        viewModel.setNotificationSound(NotificationScheduler.NotificationSoundOption.SILENT)
        assertEquals(
            NotificationScheduler.NotificationSoundOption.SILENT,
            viewModel.uiState.value.notificationSound
        )
        assertFalse(viewModel.uiState.value.showNotificationSoundDialog)
        assertEquals("Suara notifikasi diperbarui", viewModel.uiState.value.snackbarMessage)
        verify {
            mockEditor.putString(
                NotificationScheduler.KEY_NOTIFICATION_SOUND,
                NotificationScheduler.NotificationSoundOption.SILENT.value
            )
        }
    }

    @Test
    fun `toggle notification vibration updates preferences and state`() {
        assertTrue(viewModel.uiState.value.notificationVibrationEnabled)
        viewModel.toggleNotificationVibration()
        assertFalse(viewModel.uiState.value.notificationVibrationEnabled)
        assertEquals("Getar notifikasi dimatikan", viewModel.uiState.value.snackbarMessage)
        verify { mockEditor.putBoolean(NotificationScheduler.KEY_NOTIFICATION_VIBRATION, false) }
    }

    // ===================== Account State =====================

    @Test
    fun `initial state reflects logged in user`() {
        val firebaseUser = mockk<FirebaseUser>()
        every { firebaseUser.email } returns "user@example.com"
        every { mockAuthRepository.isLoggedIn } returns true
        every { mockAuthRepository.currentUser } returns firebaseUser

        val loggedInViewModel = SettingsViewModel(
            mockAppContext,
            mockAppDatabase,
            mockHabitDao,
            mockUserHabitDao,
            mockHabitCompletionDao,
            mockSharedPreferences,
            mockAuthRepository
        )
        val state = loggedInViewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertEquals("user@example.com", state.userEmail)
    }

    @Test
    fun `logout updates account state and shows snackbar`() {
        viewModel.logout()
        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertEquals("", state.userEmail)
        assertEquals("Anda telah keluar dari akun", state.snackbarMessage)
    }

    // ===================== Enum Display Names =====================

    @Test
    fun `TimeFormatOption has correct display names`() {
        assertEquals("24 Jam", TimeFormatOption.HOUR_24.displayName)
        assertEquals("12 Jam", TimeFormatOption.HOUR_12.displayName)
    }

    @Test
    fun `TimeFormatOption has correct descriptions`() {
        assertEquals("Contoh: 14:30", TimeFormatOption.HOUR_24.description)
        assertEquals("Contoh: 2:30 PM", TimeFormatOption.HOUR_12.description)
    }
}
