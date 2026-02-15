package com.islami.Aha.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.model.HabitCompletionRecord
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.repository.AdminConfigRepository
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import com.islami.Aha.ui.shared.SunnahHabitSharedViewModel
import com.islami.Aha.util.LocationHelper
import com.islami.Aha.util.DateUtils
import com.islami.Aha.util.NotificationScheduler
import com.islami.Aha.util.PrayerTimeApiService
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    val puasaWajibRamadanEnabled: Boolean = true,
    val selectedMainCategory: String = "Sholat",
    val selectedSubTabIndex: Int = 0,
    val allHabits: List<Habit> = emptyList(),
    val motivationalQuote: String = "",
    val quoteSource: String = "",
    val sunnahHabits: List<SunnahHabit> = emptyList(),
    val isRamadanMonth: Boolean = DateUtils.isRamadanMonth()
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
            return allHabits.filter {
                if (it.category != subCategory) return@filter false
                if (it.category == "Puasa Wajib" && (!isRamadanMonth || !puasaWajibRamadanEnabled)) return@filter false
                true
            }
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
                val habits = allHabits.filter {
                    it.category.startsWith("Puasa") &&
                        !(it.category == "Puasa Wajib" && (!isRamadanMonth || !puasaWajibRamadanEnabled))
                }
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
    private val habitCompletionDao: HabitCompletionDao,
    private val habitDao: HabitDao,
    private val sharedPreferences: SharedPreferences,
    private val sunnahHabitSharedViewModel: SunnahHabitSharedViewModel,
    private val adminConfigRepository: AdminConfigRepository
) : ViewModel() {
    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_LAST_DAILY_RESET = "last_daily_reset"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val authPrefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_IS_LOGGED_IN || key == KEY_USER_NAME) {
                refreshAuthState()
            }
        }

    init {
        refreshAuthState()
        sharedPreferences.registerOnSharedPreferenceChangeListener(authPrefsListener)
        resetDefaultHabitsIfNewDay()
        seedDataIfNeeded()
        sunnahHabitSharedViewModel.syncFromCloudIfLoggedIn()
        loadHabits()
        startTimeUpdates()
        loadQuote()
        refreshLocation()

        // Observe changes from SunnahHabitSharedViewModel
        launchSafely("observeSunnahHabits") {
            sunnahHabitSharedViewModel.sunnahHabits.collect { newSunnahHabits ->
                _uiState.update { it.copy(sunnahHabits = newSunnahHabits) }
            }
        }

        launchSafely("observeFeatureConfig") {
            adminConfigRepository.featureConfig.collect { config ->
                _uiState.update {
                    it.copy(puasaWajibRamadanEnabled = config.puasaWajibRamadanEnabled)
                }
            }
        }
    }

    override fun onCleared() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(authPrefsListener)
        super.onCleared()
    }

    private fun seedDataIfNeeded() {
        launchSafely("seedDataIfNeeded") {
            val count = habitDao.getHabitCount()
            if (count == 0) {
                habitDao.insertAll(resolveDefaultHabits())
            }
            sharedPreferences.edit().putBoolean("hasSeeded", true).apply()
        }
    }

    private suspend fun resolveDefaultHabits(): List<Habit> {
        val cloudFardhuHabits = fetchFardhuDefaultsFromFirestore()
        return if (cloudFardhuHabits.isNotEmpty()) cloudFardhuHabits else getFallbackFardhuHabits()
    }

    private suspend fun fetchFardhuDefaultsFromFirestore(): List<Habit> {
        val firestore = runCatching { FirebaseFirestore.getInstance() }.getOrNull() ?: return emptyList()
        val snapshot = runCatching {
            firestore.collection("fardhu_defaults")
                .orderBy("order")
                .get()
                .await()
        }.getOrElse {
            runCatching { FirebaseCrashlytics.getInstance().recordException(it) }
            return emptyList()
        }

        val canonicalOrder = listOf(
            "Sholat Subuh",
            "Sholat Dzuhur",
            "Sholat Ashar",
            "Sholat Maghrib",
            "Sholat Isya"
        )
        val canonicalSet = canonicalOrder.toSet()

        val byName = snapshot.documents.mapNotNull { doc ->
            val name = doc.getString("name").orEmpty()
            val time = doc.getString("defaultTime").orEmpty()
            val icon = doc.getString("icon").orEmpty().ifBlank { "sun" }
            if (name !in canonicalSet || !time.matches(Regex("^\\d{2}:\\d{2}$"))) return@mapNotNull null
            name to Habit(
                name = name,
                category = "Sholat Fardhu",
                icon = icon,
                description = "",
                time = time
            )
        }.toMap()

        return canonicalOrder.mapNotNull { byName[it] }
            .takeIf { it.size == canonicalOrder.size } ?: emptyList()
    }

    private fun getFallbackFardhuHabits(): List<Habit> = listOf(
        // Sholat Fardhu
        Habit(name = "Sholat Subuh", category = "Sholat Fardhu", icon = "sunrise", description = "", time = "04:30"),
        Habit(name = "Sholat Dzuhur", category = "Sholat Fardhu", icon = "sun", description = "", time = "11:55"),
        Habit(name = "Sholat Ashar", category = "Sholat Fardhu", icon = "cloud", description = "", time = "15:10"),
        Habit(name = "Sholat Maghrib", category = "Sholat Fardhu", icon = "moon", description = "", time = "18:00"),
        Habit(name = "Sholat Isya", category = "Sholat Fardhu", icon = "moon", description = "", time = "19:15")
    )

    private fun loadHabits() {
        launchSafely("loadHabits") {
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
        launchSafely("startTimeUpdates") {
            while (true) {
                val now = Calendar.getInstance()
                val prayerInfo = calculateNextPrayer(now, _uiState.value.allHabits)
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

    private fun calculateNextPrayer(now: Calendar, habits: List<Habit>): Triple<String, String, Float> {
        val prayerTimes = parsePrayerTimes(habits)
        if (prayerTimes.isEmpty()) return Triple("", "", 0f)
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

    private fun loadQuote() = Unit

    fun toggleHabitCompletion(habit: Habit) {
        viewModelScope.launch {
            val willComplete = !habit.isCompleted
            val updatedHabit = habit.copy(isCompleted = willComplete)
            habitDao.updateHabit(updatedHabit)

            val todayKey = DateUtils.getTodayKey()
            val habitKey = "default_${habit.id}"
            if (willComplete) {
                habitCompletionDao.insert(
                    HabitCompletionRecord(
                        habitKey = habitKey,
                        dateKey = todayKey,
                        category = habit.category,
                        source = "DEFAULT"
                    )
                )
            } else {
                habitCompletionDao.deleteByHabitAndDate(habitKey, todayKey)
            }
        }
    }

    fun toggleReminderEnabled(habit: Habit) {
        viewModelScope.launch {
            val willEnable = !habit.isReminderEnabled
            val updatedHabit = habit.copy(isReminderEnabled = willEnable)
            habitDao.updateHabit(updatedHabit)

            if (habit.time.isNotBlank()) {
                val parts = habit.time.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull()
                    val minute = parts[1].toIntOrNull()
                    if (hour != null && minute != null) {
                        if (willEnable) {
                            NotificationScheduler.scheduleHabitReminder(
                                context = context,
                                habitId = "default_${habit.id}",
                                habitName = habit.name,
                                hour = hour,
                                minute = minute
                            )
                        } else {
                            NotificationScheduler.cancelHabitReminder(context, "default_${habit.id}")
                        }
                    }
                }
            }
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
                launchSafely("syncPrayerTimesByLocation") {
                    syncPrayerTimesByLocation(result.latitude, result.longitude)
                }
                _uiState.update {
                    it.copy(location = result.cityName, isLocationLoading = false)
                }
            },
            onError = {
                _uiState.update { it.copy(isLocationLoading = false) }
            }
        )
    }

    private suspend fun syncPrayerTimesByLocation(latitude: Double, longitude: Double) {
        val times = PrayerTimeApiService.fetchTodayFardhuTimes(latitude, longitude) ?: return

        val mapping = listOf(
            "Sholat Subuh" to times.subuh,
            "Sholat Dzuhur" to times.dzuhur,
            "Sholat Ashar" to times.ashar,
            "Sholat Maghrib" to times.maghrib,
            "Sholat Isya" to times.isya
        )

        mapping.forEach { (habitName, time) ->
            if (time.matches(Regex("^\\d{2}:\\d{2}$"))) {
                habitDao.updateFardhuTimeByName(habitName, time)
            }
        }

        // Keep user reminder behavior intact: only reschedule habits that are currently enabled.
        habitDao.getFardhuHabits().forEach { habit ->
            val parts = habit.time.split(":")
            if (habit.isReminderEnabled && parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: return@forEach
                val minute = parts[1].toIntOrNull() ?: return@forEach
                NotificationScheduler.scheduleHabitReminder(
                    context = context,
                    habitId = "default_${habit.id}",
                    habitName = habit.name,
                    hour = hour,
                    minute = minute
                )
            }
        }
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

    private fun resetDefaultHabitsIfNewDay() {
        launchSafely("resetDefaultHabitsIfNewDay") {
            val todayKey = DateUtils.getTodayKey()
            val lastResetKey = sharedPreferences.getString(KEY_LAST_DAILY_RESET, null)
            if (lastResetKey == todayKey) return@launchSafely
            habitDao.resetAllCompletions()
            sharedPreferences.edit().putString(KEY_LAST_DAILY_RESET, todayKey).apply()
        }
    }

    private fun launchSafely(tag: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { error ->
                    Log.e("HomeViewModel", "Startup task failed: $tag", error)
                    runCatching { FirebaseCrashlytics.getInstance().recordException(error) }
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun parsePrayerTimes(habits: List<Habit>): List<PrayerTimeInfo> {
        val targets = listOf(
            "Sholat Subuh" to "Subuh",
            "Sholat Dzuhur" to "Dzuhur",
            "Sholat Ashar" to "Ashar",
            "Sholat Maghrib" to "Maghrib",
            "Sholat Isya" to "Isya"
        )
        val mapped = targets.mapNotNull { (habitName, prayerName) ->
            val habit = habits.firstOrNull { it.name == habitName && it.time.contains(":") } ?: return@mapNotNull null
            val parts = habit.time.split(":")
            if (parts.size != 2) return@mapNotNull null
            val hour = parts[0].toIntOrNull() ?: return@mapNotNull null
            val minute = parts[1].toIntOrNull() ?: return@mapNotNull null
            PrayerTimeInfo(prayerName, hour, minute)
        }
        return if (mapped.size == targets.size) mapped else emptyList()
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

}
