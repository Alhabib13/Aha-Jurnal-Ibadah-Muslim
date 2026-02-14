
package com.islami.Aha.ui.profile

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.repository.AuthRepository
import com.islami.Aha.data.repository.UserHabitRepository
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
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
    val isSaving: Boolean = false,
    val userInfo: UserInfo = UserInfo(),
    val totalHabits: Int = 0,
    val totalCompleted: Int = 0,
    val currentStreak: Int = 0,
    val sholatCount: Int = 0,
    val puasaCount: Int = 0,
    val reminderCount: Int = 0,
    val achievements: List<Achievement> = emptyList(),
    val weeklySummary: WeeklySummary = WeeklySummary(),
    val showLogoutConfirmation: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val habitDao: HabitDao,
    private val sharedPreferences: SharedPreferences,
    private val userHabitRepository: UserHabitRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_AVATAR_URI = "user_avatar_uri"
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
                userHabitRepository.getAllHabits()
            ) { habits, sunnahHabits ->
                Pair(habits, sunnahHabits)
            }.collect { (habits, sunnahHabits) ->
                val totalHabits = habits.size + sunnahHabits.size
                val totalCompletedToday = habits.count { it.isCompleted }
                val allCompleteToday = totalHabits > 0 && totalCompletedToday == totalHabits

                val sholatCount = sunnahHabits.count { it.category == SunnahCategoryType.SHOLAT }
                val puasaCount = sunnahHabits.count { it.category == SunnahCategoryType.PUASA }
                val reminderCount = sunnahHabits.count { it.reminderEnabled }

                val categoryCompletions = habits.groupBy { it.category }
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
                    (totalCompletedToday * 100f) / totalHabits
                } else 0f

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userInfo = getCurrentUserInfo(),
                        totalHabits = totalHabits,
                        totalCompleted = totalCompletedToday,
                        currentStreak = 0,
                        sholatCount = sholatCount,
                        puasaCount = puasaCount,
                        reminderCount = reminderCount,
                        achievements = generateAchievements(
                            totalCompleted = totalCompletedToday,
                            currentStreak = 0,
                            allCompleteToday = allCompleteToday
                        ),
                        weeklySummary = WeeklySummary(
                            completionPercentage = weeklyPercentage,
                            activeDays = if (totalCompletedToday > 0) 1 else 0,
                            totalDays = 7,
                            bestCategory = bestCategoryName
                        )
                    )
                }
            }
        }
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
                snackbarMessage = "Anda masuk sebagai tamu"
            )
        }
        return true
    }

    fun updateUsername(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            sharedPreferences.edit().putString(KEY_USER_NAME, trimmed).apply()
            _uiState.update {
                it.copy(
                    isSaving = false,
                    userInfo = getCurrentUserInfo(),
                    snackbarMessage = "Username diperbarui"
                )
            }
        }
    }

    fun updateAvatar(uri: String) {
        if (uri.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            sharedPreferences.edit().putString(KEY_USER_AVATAR_URI, uri).apply()
            _uiState.update {
                it.copy(
                    isSaving = false,
                    userInfo = getCurrentUserInfo(),
                    snackbarMessage = "Foto profil diperbarui"
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
