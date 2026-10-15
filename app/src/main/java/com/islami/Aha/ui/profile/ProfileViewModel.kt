
package com.islami.Aha.ui.profile

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.repository.AuthRepository
import com.islami.Aha.data.repository.FeatureConfigRepository
import com.islami.Aha.data.repository.UserHabitRepository
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import com.islami.Aha.util.DateUtils
import com.islami.Aha.util.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserInfo(
    val name: String = "Tamu",
    val email: String = "",
    val avatarInitial: String = "Tamu",
    val avatarUri: String? = null,
    val isLoggedIn: Boolean = false
)

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSavingAvatar: Boolean = false,
    val isSavingUsername: Boolean = false,
    val userInfo: UserInfo = UserInfo(),
    val totalHabits: Int = 0,
    val totalCompleted: Int = 0,
    val currentStreak: Int = 0,
    val sholatCount: Int = 0,
    val puasaCount: Int = 0,
    val dzikirCount: Int = 0,
    val reminderCount: Int = 0,
    /** Momen yang sudah dilalui, terbaru di atas. */
    val jejak: List<Jejak> = emptyList(),
    val showLogoutConfirmation: Boolean = false,
    val snackbarMessage: String? = null,
    val lastUsernameChangedAt: Long = 0L
) {
    private val cooldownMs = 30L * 24 * 60 * 60 * 1000

    val canChangeUsername: Boolean
        get() {
            if (lastUsernameChangedAt <= 0L) return true
            return System.currentTimeMillis() - lastUsernameChangedAt >= cooldownMs
        }

    val daysUntilUsernameChange: Int
        get() {
            if (canChangeUsername) return 0
            val elapsed = System.currentTimeMillis() - lastUsernameChangedAt
            val remaining = cooldownMs - elapsed
            return ((remaining + 86_399_999L) / 86_400_000L).toInt()
        }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitDao: HabitDao,
    private val habitCompletionDao: HabitCompletionDao,
    private val sharedPreferences: SharedPreferences,
    private val userHabitRepository: UserHabitRepository,
    private val authRepository: AuthRepository,
    private val featureConfigRepository: FeatureConfigRepository,
    private val researchLogRepository: com.islami.Aha.data.research.ResearchLogRepository,
    private val activityStats: com.islami.Aha.data.research.ActivityStatsRepository
) : ViewModel() {

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_AVATAR_URI = "user_avatar_uri"
        private const val KEY_LAST_USERNAME_CHANGED = AuthRepository.KEY_LAST_USERNAME_CHANGED
        private const val SEMUA_AWAL = "0000-01-01"
        private const val SEMUA_AKHIR = "9999-12-31"
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private val authPrefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_IS_LOGGED_IN || key == KEY_USER_NAME || key == KEY_USER_EMAIL || key == KEY_USER_AVATAR_URI) {
                _uiState.update { current ->
                    current.copy(userInfo = getCurrentUserInfo())
                }
            }
        }

    init {
        featureConfigRepository.refresh()
        sharedPreferences.registerOnSharedPreferenceChangeListener(authPrefsListener)
        loadProfileData()
    }

    override fun onCleared() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(authPrefsListener)
        super.onCleared()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                habitDao.getHabits(),
                userHabitRepository.getAllHabits(),
                featureConfigRepository.featureConfig,
                activityStats.observeChanges()
            ) { habits, sunnahHabits, config, _ ->
                Triple(habits, sunnahHabits, config)
            }.collect { (habits, sunnahHabits, config) ->
                val haidhDates = com.islami.Aha.util.UserPreferencesManager.getHaidhDates()
                val isTodayHaidh = haidhDates.contains(DateUtils.getTodayKey())
                
                val visibleHabits = habits.filter {
                    if (it.category == "Puasa Wajib" && (!DateUtils.isRamadanMonth() || !config.puasaWajibRamadanEnabled)) {
                        return@filter false
                    }
                    if (it.category == "Sholat Tarawih" && (!DateUtils.isRamadanMonth() || !config.sholatTarawihEnabled)) {
                        return@filter false
                    }
                    if (isTodayHaidh && (it.category.startsWith("Sholat") || it.category.startsWith("Puasa"))) {
                        return@filter false
                    }
                    true
                }
                
                val visibleSunnahHabits = sunnahHabits.filter {
                    if (isTodayHaidh && (it.category == SunnahCategoryType.SHOLAT || it.category == SunnahCategoryType.PUASA)) {
                        return@filter false
                    }
                    true
                }
                
                // Dzikir: mengikuti habit dzikir pilihan pengguna (tilawah & iqro di luar rasio).
                val dzikir = activityStats.getDzikirProgress(DateUtils.getTodayKey(), isTodayHaidh)
                val totalHabits = visibleHabits.size + visibleSunnahHabits.size + dzikir.total
                val totalCompletedToday = visibleHabits.count { it.isCompleted } +
                    visibleSunnahHabits.count { it.isCompletedToday } +
                    dzikir.done
                val sholatCount = sunnahHabits.count { it.category == SunnahCategoryType.SHOLAT }
                val puasaCount = sunnahHabits.count { it.category == SunnahCategoryType.PUASA }
                // Pengingat aktif: sholat fardhu bawaan + habit sunnah yang pengingatnya menyala.
                val reminderCount = habits.count { it.category == "Sholat Fardhu" && it.isReminderEnabled } + sunnahHabits.count { it.reminderEnabled }

                val completionDates = activityStats.getDistinctCompletionDatesDesc()
                val historicalTotal = activityStats.getTotalCompletionCount()
                val currentStreak = calculateCurrentStreak(completionDates, haidhDates)

                val namaHabit = habits.associate { "default_${it.id}" to it.name } +
                    sunnahHabits.associate { "sunnah_${it.id}" to it.name }
                val jejak = susunJejak(
                    records = activityStats.getCompletionRecordsBetween(SEMUA_AWAL, SEMUA_AKHIR),
                    namaHabit = namaHabit,
                    aktivitas = activityStats.getAktivitasLainBetween(SEMUA_AWAL, SEMUA_AKHIR),
                    tanggalCuti = haidhDates
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userInfo = getCurrentUserInfo(),
                        totalHabits = totalHabits,
                        totalCompleted = historicalTotal,
                        currentStreak = currentStreak,
                        sholatCount = sholatCount,
                        puasaCount = puasaCount,
                        dzikirCount = activityStats.countDzikirHabits(),
                        reminderCount = reminderCount,
                        lastUsernameChangedAt = sharedPreferences.getLong(KEY_LAST_USERNAME_CHANGED, 0L),
                        jejak = jejak
                    )
                }
            }
        }
    }

    private fun calculateCurrentStreak(distinctDatesDesc: List<String>, haidhDates: Set<String>): Int {
        if (distinctDatesDesc.isEmpty() && haidhDates.isEmpty()) return 0
        val dateSet = distinctDatesDesc.toSet()
        var cursor = java.time.LocalDate.now()
        var streak = 0
        
        if (!dateSet.contains(cursor.toString()) && !haidhDates.contains(cursor.toString())) {
            cursor = cursor.minusDays(1)
        }
        
        while (dateSet.contains(cursor.toString()) || haidhDates.contains(cursor.toString())) {
            if (dateSet.contains(cursor.toString())) {
                streak++ 
            }
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun getCurrentUserInfo(): UserInfo {
        val isLoggedIn = authRepository.isLoggedIn ||
            sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) {
            return UserInfo()
        }

        val name = sharedPreferences.getString(KEY_USER_NAME, "Pengguna") ?: "Pengguna"
        val email = sharedPreferences.getString(KEY_USER_EMAIL, "") ?: ""
        val avatarUri = sharedPreferences.getString(KEY_USER_AVATAR_URI, null)
        val avatarInitial = name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "U" }

        return UserInfo(
            name = name,
            email = email,
            avatarInitial = avatarInitial,
            avatarUri = avatarUri,
            isLoggedIn = true
        )
    }

    fun showLogoutConfirmation() {
        _uiState.update { it.copy(showLogoutConfirmation = true) }
    }

    fun hideLogoutConfirmation() {
        _uiState.update { it.copy(showLogoutConfirmation = false) }
    }

    fun logout(): Boolean {
        viewModelScope.launch {
            runCatching {
                userHabitRepository.getActiveReminderHabits().forEach { habit ->
                    NotificationScheduler.cancelHabitReminder(context, habit.id)
                }
                userHabitRepository.clearLocalHabitsForGuestMode()
                habitCompletionDao.deleteAll()
                habitDao.resetTrackerState()
            }
            // Unggah log penelitian selagi akun lama masih login, lalu kosongkan.
            runCatching { researchLogRepository.flushAndClear() }
            authRepository.logout()

            _uiState.update {
                it.copy(
                    showLogoutConfirmation = false,
                    userInfo = UserInfo(),
                    snackbarMessage = "Anda masuk sebagai tamu"
                )
            }
        }
        return true
    }

    fun updateUsername(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return

        val current = _uiState.value
        if (!current.canChangeUsername) {
            _uiState.update {
                it.copy(snackbarMessage = "Username bisa diubah lagi dalam ${current.daysUntilUsernameChange} hari")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingUsername = true) }
            val result = authRepository.updateDisplayName(trimmed)
            val now = System.currentTimeMillis()
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isSavingUsername = false,
                        userInfo = getCurrentUserInfo(),
                        lastUsernameChangedAt = now,
                        snackbarMessage = "Username diperbarui"
                    )
                } else {
                    it.copy(
                        isSavingUsername = false,
                        snackbarMessage = result.exceptionOrNull()?.message ?: "Gagal memperbarui username"
                    )
                }
            }
        }
    }

    fun updateAvatar(uriString: String) {
        if (uriString.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingAvatar = true) }

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(uriString)
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Tidak bisa membuka file gambar")
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (originalBitmap == null) throw IllegalStateException("File bukan gambar yang valid")

                    val maxDimension = 512
                    val scaled = if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
                        val ratio = minOf(
                            maxDimension.toFloat() / originalBitmap.width,
                            maxDimension.toFloat() / originalBitmap.height
                        )
                        Bitmap.createScaledBitmap(
                            originalBitmap,
                            (originalBitmap.width * ratio).toInt(),
                            (originalBitmap.height * ratio).toInt(),
                            true
                        )
                    } else originalBitmap

                    val output = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 70, output)
                    val bytes = output.toByteArray()

                    if (bytes.size > 750_000) {
                        throw IllegalStateException("Gambar terlalu besar, gunakan gambar yang lebih kecil")
                    }

                    val base64 = "data:image/jpeg;base64," +
                        Base64.encodeToString(bytes, Base64.NO_WRAP)
                    if (base64.length > AuthRepository.MAX_AVATAR_BASE64_LENGTH) {
                        throw IllegalStateException("Foto profil terlalu besar, gunakan gambar yang lebih kecil")
                    }

                    authRepository.updateAvatar(base64).getOrThrow()
                }
            }

            _uiState.update {
                it.copy(
                    isSavingAvatar = false,
                    userInfo = getCurrentUserInfo(),
                    snackbarMessage = if (result.isSuccess) {
                        "Foto profil diperbarui"
                    } else {
                        result.exceptionOrNull()?.message ?: "Gagal menyimpan foto profil"
                    }
                )
            }
        }
    }

    fun clearAvatar() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingAvatar = true) }
            val result = authRepository.clearAvatar()
            _uiState.update {
                it.copy(
                    isSavingAvatar = false,
                    userInfo = getCurrentUserInfo(),
                    snackbarMessage = if (result.isSuccess) {
                        "Foto profil dihapus"
                    } else {
                        result.exceptionOrNull()?.message ?: "Gagal menghapus foto profil"
                    }
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
