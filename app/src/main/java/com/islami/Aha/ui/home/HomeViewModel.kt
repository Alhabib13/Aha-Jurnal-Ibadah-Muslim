package com.islami.Aha.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.model.Habit
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import com.islami.Aha.ui.shared.SunnahHabitSharedViewModel
import com.islami.Aha.util.LocationHelper
import com.islami.Aha.util.DateUtils
import com.islami.Aha.util.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLocationLoading: Boolean = false,
    val currentTime: String = "",
    val location: String = "Memuat lokasi...",
    val gregorianDate: String = "",
    val hijriDate: String = "",
    val userName: String = "",
    val isLoggedIn: Boolean = false,
    val nextPrayerName: String = "",
    val nextPrayerTimeRemaining: String = "",
    val nextPrayerProgress: Float = 0f,
    val selectedMainCategory: String = "Sholat",
    val selectedSubTabIndex: Int = 0,
    val allHabits: List<Habit> = emptyList(),
    val motivationalQuote: String = "",
    val quoteSource: String = "",
    val sunnahHabits: List<SunnahHabit> = emptyList()
) {
    val subTabCategories: List<String>
        get() = when (selectedMainCategory) {
            "Sholat" -> listOf("Sholat Fardhu", "Sholat Sunnah")
            "Puasa" -> listOf("Puasa Wajib", "Puasa Sunnah")
            else -> emptyList()
        }

    val subTabDisplayNames: List<String>
        get() = when (selectedMainCategory) {
            "Sholat" -> listOf("Sholat Fardhu", "Sholat Sunnah")
            "Puasa" -> listOf("Puasa Wajib (Ramadan)", "Puasa Sunnah")
            else -> emptyList()
        }

    val comingSoonCategories = listOf("Dzikir", "Tilawah")

    val isComingSoon: Boolean
        get() = selectedMainCategory in comingSoonCategories

    val filteredHabits: List<Habit>
        get() {
            if (isComingSoon) return emptyList()
            val subCategory = subTabCategories.getOrNull(selectedSubTabIndex) ?: return emptyList()
            return allHabits.filter { it.category == subCategory }
        }

    val filteredSunnahHabits: List<SunnahHabit>
        get() {
            if (isComingSoon) return emptyList()
            return when {
                selectedMainCategory == "Sholat" && selectedSubTabIndex == 1 ->
                    sunnahHabits.filter { it.category == SunnahCategoryType.SHOLAT }
                selectedMainCategory == "Puasa" && selectedSubTabIndex == 1 ->
                    sunnahHabits.filter { it.category == SunnahCategoryType.PUASA }
                else -> emptyList()
            }
        }

    val completedHabitsCount: Int
        get() = filteredHabits.count { it.isCompleted } + filteredSunnahHabits.count { it.isCompletedToday }

    val totalHabitsCount: Int
        get() = filteredHabits.size + filteredSunnahHabits.size

    fun getCategoryBadge(mainCategory: String): String {
        return when (mainCategory) {
            "Sholat" -> {
                val habits = allHabits.filter { it.category.startsWith("Sholat") }
                val sunnah = sunnahHabits.filter { it.category == SunnahCategoryType.SHOLAT }
                val completed = habits.count { it.isCompleted } + sunnah.count { it.isCompletedToday }
                val total = habits.size + sunnah.size
                "$completed/$total"
            }
            "Puasa" -> {
                val habits = allHabits.filter { it.category.startsWith("Puasa") }
                val sunnah = sunnahHabits.filter { it.category == SunnahCategoryType.PUASA }
                val completed = habits.count { it.isCompleted } + sunnah.count { it.isCompletedToday }
                val total = habits.size + sunnah.size
                "$completed/$total"
            }
            else -> "Segera"
        }
    }
}

data class PrayerTimeInfo(
    val name: String,
    val hour: Int,
    val minute: Int
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitDao: HabitDao,
    private val sharedPreferences: SharedPreferences,
    private val sunnahHabitSharedViewModel: SunnahHabitSharedViewModel
) : ViewModel() {
    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val authPrefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_IS_LOGGED_IN || key == KEY_USER_NAME) {
                refreshAuthState()
            }
        }

    private val prayerTimes = listOf(
        PrayerTimeInfo("Subuh", 4, 30),
        PrayerTimeInfo("Dzuhur", 11, 55),
        PrayerTimeInfo("Ashar", 15, 10),
        PrayerTimeInfo("Maghrib", 18, 0),
        PrayerTimeInfo("Isya", 19, 15)
    )

    init {
        refreshAuthState()
        sharedPreferences.registerOnSharedPreferenceChangeListener(authPrefsListener)
        seedDataIfNeeded()
        loadHabits()
        startTimeUpdates()
        loadQuote()
        refreshLocation()

        // Observe changes from SunnahHabitSharedViewModel
        viewModelScope.launch {
            sunnahHabitSharedViewModel.sunnahHabits.collect { newSunnahHabits ->
                _uiState.update { it.copy(sunnahHabits = newSunnahHabits) }
            }
        }
    }

    override fun onCleared() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(authPrefsListener)
        super.onCleared()
    }

    private fun seedDataIfNeeded() {
        viewModelScope.launch {
            val hasSeeded = sharedPreferences.getBoolean("hasSeeded", false)
            if (!hasSeeded) {
                val count = habitDao.getHabitCount()
                if (count == 0) {
                    habitDao.insertAll(getDefaultHabits())
                    sharedPreferences.edit().putBoolean("hasSeeded", true).apply()
                }
            }
        }
    }

    private fun getDefaultHabits(): List<Habit> = listOf(
        // Sholat Fardhu
        Habit(name = "Sholat Subuh", category = "Sholat Fardhu", icon = "sunrise", description = "", time = "04:30"),
        Habit(name = "Sholat Dzuhur", category = "Sholat Fardhu", icon = "sun", description = "", time = "11:55"),
        Habit(name = "Sholat Ashar", category = "Sholat Fardhu", icon = "cloud", description = "", time = "15:10"),
        Habit(name = "Sholat Maghrib", category = "Sholat Fardhu", icon = "moon", description = "", time = "18:00"),
        Habit(name = "Sholat Isya", category = "Sholat Fardhu", icon = "moon", description = "", time = "19:15"),
        // Sholat Sunnah
        Habit(name = "Sholat Dhuha", category = "Sholat Sunnah", icon = "sun", description = "06:00 - 11:00", time = "06:00"),
        Habit(name = "Qabliyah Dzuhur", category = "Sholat Sunnah", icon = "sun", description = "", time = "11:30"),
        Habit(name = "Ba'diyah Dzuhur", category = "Sholat Sunnah", icon = "sun", description = "", time = "12:15"),
        Habit(name = "Ba'diyah Maghrib", category = "Sholat Sunnah", icon = "moon", description = "", time = "18:20"),
        Habit(name = "Ba'diyah Isya", category = "Sholat Sunnah", icon = "moon", description = "", time = "19:35"),
        Habit(name = "Tahajud", category = "Sholat Sunnah", icon = "night", description = "", time = "03:00"),
        Habit(name = "Witir", category = "Sholat Sunnah", icon = "night", description = "", time = "03:30"),
        // Puasa Wajib
        Habit(name = "Puasa Ramadan", category = "Puasa Wajib", icon = "plate", description = "Sahur - Maghrib", time = ""),
        // Puasa Sunnah
        Habit(name = "Puasa Senin", category = "Puasa Sunnah", icon = "moon", description = "Setiap Senin", time = ""),
        Habit(name = "Puasa Kamis", category = "Puasa Sunnah", icon = "moon", description = "Setiap Kamis", time = ""),
        Habit(name = "Puasa Ayyamul Bidh", category = "Puasa Sunnah", icon = "moon", description = "13-15 Hijriah", time = ""),
        Habit(name = "Puasa Daud", category = "Puasa Sunnah", icon = "moon", description = "Selang-seling", time = ""),
        Habit(name = "Puasa Syawal", category = "Puasa Sunnah", icon = "moon", description = "6 hari di bulan Syawal", time = "")
    )

    private fun loadHabits() {
        viewModelScope.launch {
            habitDao.getHabits().collect { habits ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        allHabits = habits,
                        gregorianDate = getGregorianDate(),
                        hijriDate = getHijriDate()
                    )
                }
            }
        }
    }

    private fun startTimeUpdates() {
        viewModelScope.launch {
            while (true) {
                val now = Calendar.getInstance()
                val prayerInfo = calculateNextPrayer(now)
                _uiState.update {
                    it.copy(
                        currentTime = getCurrentTime(),
                        nextPrayerName = prayerInfo.first,
                        nextPrayerTimeRemaining = prayerInfo.second,
                        nextPrayerProgress = prayerInfo.third
                    )
                }
                kotlinx.coroutines.delay(30000)
            }
        }
    }

    private fun calculateNextPrayer(now: Calendar): Triple<String, String, Float> {
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        for (i in prayerTimes.indices) {
            val prayerMinutes = prayerTimes[i].hour * 60 + prayerTimes[i].minute
            if (currentMinutes < prayerMinutes) {
                val remaining = prayerMinutes - currentMinutes
                val hours = remaining / 60
                val minutes = remaining % 60
                val timeText = if (hours > 0) "$hours jam $minutes menit lagi" else "$minutes menit lagi"

                val prevPrayerMinutes = if (i > 0) {
                    prayerTimes[i - 1].hour * 60 + prayerTimes[i - 1].minute
                } else {
                    0
                }
                val totalInterval = prayerMinutes - prevPrayerMinutes
                val elapsed = currentMinutes - prevPrayerMinutes
                val progress = if (totalInterval > 0) elapsed.toFloat() / totalInterval else 0f

                return Triple(prayerTimes[i].name, timeText, progress.coerceIn(0f, 1f))
            }
        }

        // All prayers have passed, next is Subuh tomorrow
        val subuhMinutes = prayerTimes[0].hour * 60 + prayerTimes[0].minute
        val remaining = (24 * 60 - currentMinutes) + subuhMinutes
        val hours = remaining / 60
        val minutes = remaining % 60
        val timeText = "$hours jam $minutes menit lagi"
        return Triple("Subuh", timeText, 0.9f)
    }

    private fun loadQuote() {
        val quote = getRandomMotivationalQuote()
        _uiState.update { it.copy(motivationalQuote = quote.first, quoteSource = quote.second) }
    }

    fun toggleHabitCompletion(habit: Habit) {
        viewModelScope.launch {
            val updatedHabit = habit.copy(isCompleted = !habit.isCompleted)
            habitDao.updateHabit(updatedHabit)
        }
    }

    fun toggleReminderEnabled(habit: Habit) {
        viewModelScope.launch {
            val updatedHabit = habit.copy(isReminderEnabled = !habit.isReminderEnabled)
            habitDao.updateHabit(updatedHabit)
        }
    }

    fun selectMainCategory(category: String) {
        _uiState.update { it.copy(selectedMainCategory = category, selectedSubTabIndex = 0) }
    }

    fun selectSubTab(index: Int) {
        _uiState.update { it.copy(selectedSubTabIndex = index) }
    }

    fun toggleSunnahHabitCompletion(id: String) {
        sunnahHabitSharedViewModel.toggleHabitComplete(id)
    }

    fun toggleSunnahReminder(id: String) {
        val habit = sunnahHabitSharedViewModel.getHabitById(id) ?: return
        val willEnable = !habit.reminderEnabled
        sunnahHabitSharedViewModel.toggleReminder(id)

        if (willEnable && habit.reminderTime != null) {
            val parts = habit.reminderTime.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: return
                val minute = parts[1].toIntOrNull() ?: return
                NotificationScheduler.scheduleHabitReminder(context, id, habit.name, hour, minute)
            }
        } else {
            NotificationScheduler.cancelHabitReminder(context, id)
        }
    }

    fun removeSunnahHabit(id: String) {
        val habit = sunnahHabitSharedViewModel.getHabitById(id)
        if (habit?.reminderEnabled == true) {
            NotificationScheduler.cancelHabitReminder(context, id)
        }
        sunnahHabitSharedViewModel.removeHabit(id)
    }

    fun refreshData() {
        _uiState.update { it.copy(isRefreshing = true) }
        LocationHelper.getLastLocation(
            context = context,
            onResult = { result ->
                _uiState.update {
                    it.copy(location = result.cityName, isRefreshing = false)
                }
            },
            onError = {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        )
        // Timeout fallback - dismiss refresh after 5 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            if (_uiState.value.isRefreshing) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun refreshLocation() {
        _uiState.update { it.copy(isLocationLoading = true) }
        LocationHelper.getLastLocation(
            context = context,
            onResult = { result ->
                _uiState.update {
                    it.copy(location = result.cityName, isLocationLoading = false)
                }
            },
            onError = {
                _uiState.update { it.copy(isLocationLoading = false) }
            }
        )
    }

    fun refreshAuthState() {
        val isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        val userName = if (isLoggedIn) {
            sharedPreferences.getString(KEY_USER_NAME, "Pengguna").orEmpty().ifBlank { "Pengguna" }
        } else {
            ""
        }
        _uiState.update {
            it.copy(
                isLoggedIn = isLoggedIn,
                userName = userName
            )
        }
    }

    private fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return String.format("%02d:%02d", hour, minute)
    }

    private fun getGregorianDate(): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
        return dateFormat.format(Date())
    }

    private fun getHijriDate(): String {
        return DateUtils.getHijriDateFormatted()
    }

    private fun getRandomMotivationalQuote(): Pair<String, String> {
        val quotes = listOf(
            Pair(
                "Dan bersegeralah kamu kepada ampunan dari Tuhanmu dan kepada surga yang luasnya seluas langit dan bumi yang disediakan untuk orang-orang yang bertakwa.",
                "- QS. Ali Imran: 133"
            ),
            Pair(
                "Sesungguhnya shalat itu mencegah dari perbuatan keji dan mungkar.",
                "- QS. Al-Ankabut: 45"
            ),
            Pair(
                "Amalan yang paling dicintai oleh Allah adalah yang paling konsisten meskipun sedikit.",
                "- HR. Bukhari & Muslim"
            )
        )
        return quotes.random()
    }
}