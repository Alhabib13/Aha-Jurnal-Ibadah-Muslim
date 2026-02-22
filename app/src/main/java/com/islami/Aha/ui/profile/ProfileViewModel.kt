
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
import com.islami.Aha.data.repository.AdminConfigRepository
import com.islami.Aha.data.repository.AuthRepository
import com.islami.Aha.data.repository.UserHabitRepository
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import com.islami.Aha.util.DateUtils
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

data class Achievement(
    val id: String,
    val emoji: String,
    val name: String,
    val description: String,
    val isUnlocked: Boolean,
    val progress: Float
)

data class WeeklySummary(
    val completionPercentage: Float = 0f,
    val activeDays: Int = 0,
    val totalDays: Int = 7,
    val bestCategory: String = "-"
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
    val reminderCount: Int = 0,
    val isAdmin: Boolean = false,
    val achievements: List<Achievement> = emptyList(),
    val weeklySummary: WeeklySummary = WeeklySummary(),
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
    private val adminConfigRepository: AdminConfigRepository
) : ViewModel() {

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_AVATAR_URI = "user_avatar_uri"
        private const val KEY_LAST_USERNAME_CHANGED = AuthRepository.KEY_LAST_USERNAME_CHANGED
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private val authPrefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_IS_LOGGED_IN || key == KEY_USER_NAME || key == KEY_USER_EMAIL || key == KEY_USER_AVATAR_URI) {
                _uiState.update { current ->
                    current.copy(userInfo = getCurrentUserInfo())
                }
                refreshAdminStatus()
            }
        }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(authPrefsListener)
        loadProfileData()
        refreshAdminStatus()
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
                adminConfigRepository.featureConfig
            ) { habits, sunnahHabits, config ->
                Triple(habits, sunnahHabits, config)
            }.collect { (habits, sunnahHabits, config) ->
                val visibleHabits = habits.filter {
                    if (it.category == "Puasa Wajib" && (!DateUtils.isRamadanMonth() || !config.puasaWajibRamadanEnabled)) {
                        return@filter false
                    }
                    if (it.category == "Sholat Tarawih" && (!DateUtils.isRamadanMonth() || !config.sholatTarawihEnabled)) {
                        return@filter false
                    }
                    true
                }
                val totalHabits = visibleHabits.size + sunnahHabits.size
                val totalCompletedToday = visibleHabits.count { it.isCompleted } + sunnahHabits.count { it.isCompletedToday }
                val allCompleteToday = totalHabits > 0 && totalCompletedToday == totalHabits

                val sholatCount = sunnahHabits.count { it.category == SunnahCategoryType.SHOLAT }
                val puasaCount = sunnahHabits.count { it.category == SunnahCategoryType.PUASA }
                val reminderCount = sunnahHabits.count { it.reminderEnabled }

                // Real historical data from completion records
                val completionDates = habitCompletionDao.getDistinctCompletionDatesDesc()
                val historicalTotal = habitCompletionDao.getTotalCompletionCount()
                val currentStreak = calculateCurrentStreak(completionDates)

                // Weekly active days (last 7 days)
                val last7DateKeys = DateUtils.getDateKeysLastDays(7)
                val weeklyDailyCounts = if (last7DateKeys.isNotEmpty()) {
                    habitCompletionDao.getDailyCountsBetween(last7DateKeys.first(), last7DateKeys.last())
                } else {
                    emptyList()
                }
                val weeklyActiveDays = weeklyDailyCounts.count { it.count > 0 }

                val categoryCompletions = visibleHabits.groupBy { it.category }
                    .mapValues { (_, categoryHabits) ->
                        val completed = categoryHabits.count { it.isCompleted }
                        val total = categoryHabits.size
                        if (total > 0) (completed * 100f) / total else 0f
                    }
                val bestCategory = categoryCompletions.maxByOrNull { it.value }
                val bestCategoryName = if ((bestCategory?.value ?: 0f) > 0f) {
                    bestCategory?.key ?: "-"
                } else "-"

                val weeklyPercentage = if (totalHabits > 0) {
                    val weeklyTotalCompleted = weeklyDailyCounts.sumOf { it.count }
                    (weeklyTotalCompleted * 100f) / (totalHabits * 7f)
                } else 0f

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userInfo = getCurrentUserInfo(),
                        totalHabits = totalHabits,
                        totalCompleted = historicalTotal,
                        currentStreak = currentStreak,
                        sholatCount = sholatCount,
                        puasaCount = puasaCount,
                        reminderCount = reminderCount,
                        lastUsernameChangedAt = sharedPreferences.getLong(KEY_LAST_USERNAME_CHANGED, 0L),
                        achievements = generateAchievements(
                            totalCompleted = historicalTotal,
                            currentStreak = currentStreak,
                            allCompleteToday = allCompleteToday
                        ),
                        weeklySummary = WeeklySummary(
                            completionPercentage = weeklyPercentage,
                            activeDays = weeklyActiveDays,
                            totalDays = 7,
                            bestCategory = bestCategoryName
                        )
                    )
                }
            }
        }
    }

    private fun calculateCurrentStreak(distinctDatesDesc: List<String>): Int {
        if (distinctDatesDesc.isEmpty()) return 0
        val dateSet = distinctDatesDesc.toSet()
        var cursor = java.time.LocalDate.now()
        var streak = 0
        while (dateSet.contains(cursor.toString())) {
            streak++
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

    private fun generateAchievements(
        totalCompleted: Int,
        currentStreak: Int,
        allCompleteToday: Boolean
    ): List<Achievement> {
        return listOf(
            Achievement(
                id = "first_step",
                emoji = "\uD83C\uDF1F",
                name = "Langkah Pertama",
                description = "Selesaikan ibadah pertama",
                isUnlocked = totalCompleted >= 1,
                progress = if (totalCompleted >= 1) 1f else 0f
            ),
            Achievement(
                id = "burning",
                emoji = "\uD83D\uDD25",
                name = "Semangat Membara",
                description = "Streak 7 hari berturut",
                isUnlocked = currentStreak >= 7,
                progress = (currentStreak / 7f).coerceAtMost(1f)
            ),
            Achievement(
                id = "consistent",
                emoji = "\u2B50",
                name = "Bintang Konsisten",
                description = "Streak 14 hari berturut",
                isUnlocked = currentStreak >= 14,
                progress = (currentStreak / 14f).coerceAtMost(1f)
            ),
            Achievement(
                id = "champion",
                emoji = "\uD83C\uDFC6",
                name = "Juara Istiqomah",
                description = "Streak 30 hari berturut",
                isUnlocked = currentStreak >= 30,
                progress = (currentStreak / 30f).coerceAtMost(1f)
            ),
            Achievement(
                id = "hundred",
                emoji = "\uD83D\uDCAF",
                name = "Seratus Ibadah",
                description = "100 ibadah total selesai",
                isUnlocked = totalCompleted >= 100,
                progress = (totalCompleted / 100f).coerceAtMost(1f)
            ),
            Achievement(
                id = "sharpshooter",
                emoji = "\uD83C\uDFAF",
                name = "Penembak Jitu",
                description = "Semua habit selesai 1 hari",
                isUnlocked = allCompleteToday,
                progress = if (allCompleteToday) 1f else 0f
            )
        )
    }

    fun showLogoutConfirmation() {
        _uiState.update { it.copy(showLogoutConfirmation = true) }
    }

    fun hideLogoutConfirmation() {
        _uiState.update { it.copy(showLogoutConfirmation = false) }
    }

    fun logout(): Boolean {
        authRepository.logout()

        _uiState.update {
            it.copy(
                showLogoutConfirmation = false,
                userInfo = UserInfo(),
                isAdmin = false,
                snackbarMessage = "Anda masuk sebagai tamu"
            )
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

    private fun refreshAdminStatus() {
        viewModelScope.launch {
            val admin = adminConfigRepository.isCurrentUserAdmin()
            _uiState.update { it.copy(isAdmin = admin) }
        }
    }
}
