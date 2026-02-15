package com.islami.Aha.ui.settings

import android.content.SharedPreferences
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.local.UserHabitDao
import com.islami.Aha.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
    private lateinit var mockHabitDao: HabitDao
    private lateinit var mockUserHabitDao: UserHabitDao
    private lateinit var mockHabitCompletionDao: HabitCompletionDao
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockAuthRepository: AuthRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockHabitDao = mockk(relaxed = true)
        mockUserHabitDao = mockk(relaxed = true)
        mockHabitCompletionDao = mockk(relaxed = true)
        mockSharedPreferences = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        mockAuthRepository = mockk(relaxed = true)
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
        every { mockAuthRepository.isLoggedIn } returns false
        every { mockAuthRepository.currentUser } returns null
        viewModel = SettingsViewModel(
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
        coVerify { mockHabitDao.deleteAllHabits() }
        coVerify { mockUserHabitDao.deleteAll() }
        coVerify { mockHabitCompletionDao.deleteAll() }
        assertFalse(viewModel.uiState.value.showResetConfirmation)
        assertEquals("Semua data telah direset", viewModel.uiState.value.snackbarMessage)
    }

    // ===================== Snackbar =====================

    @Test
    fun `clear snackbar sets message to null`() {
        viewModel.onNotificationSoundClick() // triggers "Segera hadir"
        assertEquals("Segera hadir", viewModel.uiState.value.snackbarMessage)
        viewModel.clearSnackbar()
        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `coming soon actions show snackbar`() {
        viewModel.onChangePasswordClick()
        assertEquals("Segera hadir", viewModel.uiState.value.snackbarMessage)

        viewModel.clearSnackbar()
        viewModel.onAccountSecurityClick()
        assertEquals("Segera hadir", viewModel.uiState.value.snackbarMessage)

        viewModel.clearSnackbar()
        viewModel.onExportDataClick()
        assertEquals("Segera hadir", viewModel.uiState.value.snackbarMessage)

        viewModel.clearSnackbar()
        viewModel.onPrivacyPolicyClick()
        assertEquals("Segera hadir", viewModel.uiState.value.snackbarMessage)

        viewModel.clearSnackbar()
        viewModel.onTermsClick()
        assertEquals("Segera hadir", viewModel.uiState.value.snackbarMessage)
    }

    // ===================== Account State =====================

    @Test
    fun `initial state reflects logged in user`() {
        val firebaseUser = mockk<FirebaseUser>()
        every { firebaseUser.email } returns "user@example.com"
        every { mockAuthRepository.isLoggedIn } returns true
        every { mockAuthRepository.currentUser } returns firebaseUser

        val loggedInViewModel = SettingsViewModel(
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
