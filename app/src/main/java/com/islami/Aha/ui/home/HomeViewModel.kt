package com.islami.Aha.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.islami.Aha.data.local.AppDatabase
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.model.HabitCompletionRecord
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.repository.AdminConfigRepository
import com.islami.Aha.data.repository.DailyIslamicContentRepository
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import java.io.IOException
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
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
    val prayerTimeSource: String = "DEFAULT",
    val prayerTimeLastSyncAtMs: Long = 0L,
    val prayerTimeStatusText: String = "",
    val puasaWajibRamadanEnabled: Boolean = true,
    val ramadanScheduleByLocationEnabled: Boolean = true,
    val sholatTarawihEnabled: Boolean = true,
    val fardhuScheduleByLocationEnabled: Boolean = true,
    val ramadanImsakTime: String = "",
    val ramadanIftarTime: String = "",
    val ramadanStatusText: String = "",
    val selectedMainCategory: String = "Sholat",
    val selectedSubTabIndex: Int = 0,
    val allHabits: List<Habit> = emptyList(),
    val hadithText: String = "Memuat hadis...",
    val hadithSource: String = "",
    val surahMeaning: String = "Memuat arti surah...",
    val surahReference: String = "",
    val showHadithContent: Boolean = true,
    val motivationalQuote: String = "Memuat hadis...",
    val quoteSource: String = "",
    val sunnahHabits: List<SunnahHabit> = emptyList(),
    val showSyncNotice: Boolean = false,
    val syncNoticeMessage: String = "",
    val isRamadanMonth: Boolean = DateUtils.isRamadanMonth()
) {
    private val selectedSubCategory: String?
        get() = subTabCategories.getOrNull(selectedSubTabIndex)

    val ramadanPuasaHabit: Habit?
        get() = allHabits.firstOrNull { it.category == "Puasa Wajib" }

    val ramadanTarawihHabit: Habit?
        get() = allHabits.firstOrNull { it.category == "Sholat Tarawih" }

    private val isPuasaRamadanContext: Boolean
        get() = selectedMainCategory == "Puasa" && selectedSubCategory == "Puasa Wajib"

    private val isTarawihContext: Boolean
        get() = selectedMainCategory == "Sholat" && selectedSubCategory == "Sholat Tarawih"

    private val ramadanUnifiedHabits: List<Habit>
        get() {
            return buildList {
                if (isPuasaRamadanContext && puasaWajibRamadanEnabled) {
                    ramadanPuasaHabit?.let { add(it) }
                }
                if (isTarawihContext && sholatTarawihEnabled) {
                    ramadanTarawihHabit?.let { add(it) }
                }
            }
        }

    val subTabCategories: List<String>
        get() = when (selectedMainCategory) {
            "Sholat" -> buildList {
                add("Sholat Fardhu")
                add("Sholat Sunnah")
                if (sholatTarawihEnabled && isRamadanMonth) {
                    add("Sholat Tarawih")
                }
            }
            "Puasa" -> listOf("Puasa Wajib", "Puasa Sunnah")
            else -> emptyList()
        }

    val subTabDisplayNames: List<String>
        get() = when (selectedMainCategory) {
            "Sholat" -> buildList {
                add("Sholat Fardhu")
                add("Sholat Sunnah")
                if (sholatTarawihEnabled && isRamadanMonth) {
                    add("Sholat Tarawih")
                }
            }
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
                if (it.category == "Sholat Tarawih" && (!isRamadanMonth || !sholatTarawihEnabled)) return@filter false
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

    val showRamadanUnifiedCard: Boolean
        get() {
            if (!isRamadanMonth) return false
            return ramadanUnifiedHabits.isNotEmpty()
        }

    val completedHabitsCount: Int
        get() {
            val habitCount = if (showRamadanUnifiedCard) {
                ramadanUnifiedHabits.count { it.isCompleted }
            } else {
                filteredHabits.count { it.isCompleted }
            }
            return habitCount + filteredSunnahHabits.count { it.isCompletedToday }
        }

    val totalHabitsCount: Int
        get() {
            val habitCount = if (showRamadanUnifiedCard) {
                ramadanUnifiedHabits.size
            } else {
                filteredHabits.size
            }
            return habitCount + filteredSunnahHabits.size
        }

    fun getCategoryBadge(mainCategory: String): String {
        return when (mainCategory) {
            "Sholat" -> {
                val habits = allHabits.filter { it.category.startsWith("Sholat") }
                    .filterNot { it.category == "Sholat Tarawih" && (!isRamadanMonth || !sholatTarawihEnabled) }
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

    val showRamadanScheduleCard: Boolean
        get() = isRamadanMonth &&
            ramadanScheduleByLocationEnabled &&
            ramadanImsakTime.isNotBlank() &&
            ramadanIftarTime.isNotBlank()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val habitCompletionDao: HabitCompletionDao,
    private val habitDao: HabitDao,
    private val sharedPreferences: SharedPreferences,
    private val sunnahHabitSharedViewModel: SunnahHabitSharedViewModel,
    private val adminConfigRepository: AdminConfigRepository,
    private val dailyIslamicContentRepository: DailyIslamicContentRepository
) : ViewModel() {
    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_LAST_DAILY_RESET = "last_daily_reset"
        private const val KEY_PRAYER_TIME_SOURCE = "prayer_time_source"
        private const val KEY_PRAYER_TIME_LAST_SYNC_AT = "prayer_time_last_sync_at"
        private const val PRAYER_TIME_SOURCE_DEFAULT = "DEFAULT"
        private const val PRAYER_TIME_SOURCE_API = "API"
        private const val TIME_UPDATE_INTERVAL_MS = 60_000L
        private const val ISLAMIC_CONTENT_ROTATE_INTERVAL_MS = 60_000L
        private const val LOCATION_REFRESH_MIN_INTERVAL_MS = 30_000L
        private const val REFRESH_TIMEOUT_MS = 5_000L
        private const val TARAWIH_DEFAULT_TIME = "20:00"
        private const val DEFAULT_OFFLINE_NOTICE =
            "Mode offline aktif. Data cloud akan disinkronkan saat koneksi stabil."
        private val HHMM_REGEX = Regex("^\\d{2}:\\d{2}$")
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var lastLocationRefreshAtMs: Long = 0L
    private val authPrefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_IS_LOGGED_IN || key == KEY_USER_NAME) {
                refreshAuthState()
            }
        }

    init {
        refreshAuthState()
        restorePrayerTimeSyncState()
        sharedPreferences.registerOnSharedPreferenceChangeListener(authPrefsListener)
        resetDefaultHabitsIfNewDay()
        seedDataIfNeeded()
        ensureRamadanFeatureHabits()
        sunnahHabitSharedViewModel.syncFromCloudIfLoggedIn()
        loadHabits()
        startPeriodicHomeUpdates()
        loadDailyIslamicContent()
        refreshLocation()

        // Observe changes from SunnahHabitSharedViewModel
        launchSafely("observeSunnahHabits") {
            sunnahHabitSharedViewModel.sunnahHabits.collect { newSunnahHabits ->
                _uiState.update { it.copy(sunnahHabits = newSunnahHabits) }
            }
        }

        launchSafely("observeFeatureConfig") {
            adminConfigRepository.featureConfig.collect { config ->
                val previousState = _uiState.value
                ensureRamadanFeatureHabits(config)
                _uiState.update { current ->
                    val updated = current.copy(
                        puasaWajibRamadanEnabled = config.puasaWajibRamadanEnabled,
                        ramadanScheduleByLocationEnabled = config.ramadanScheduleByLocationEnabled,
                        sholatTarawihEnabled = config.sholatTarawihEnabled,
                        fardhuScheduleByLocationEnabled = config.fardhuScheduleByLocationEnabled
                    )
                    updated.copy(
                        selectedSubTabIndex = normalizeSelectedSubTabIndex(updated)
                    )
                }

                val wasFardhuApiEnabled = previousState.fardhuScheduleByLocationEnabled
                val isFardhuApiEnabled = config.fardhuScheduleByLocationEnabled
                when {
                    wasFardhuApiEnabled && !isFardhuApiEnabled -> {
                        launchSafely("disableFardhuScheduleByLocation") {
                            applyDefaultFardhuTimes()
                            markPrayerTimeUsingDefault()
                            updateCurrentTimeAndPrayerInfo()
                            clearSyncNotice()
                        }
                    }

                    !wasFardhuApiEnabled && isFardhuApiEnabled -> {
                        refreshLocation(force = true)
                    }
                }
            }
        }

        launchSafely("syncIslamicContent") {
            val changed = dailyIslamicContentRepository.syncFromCloudIfAvailable()
            if (changed) {
                val content = dailyIslamicContentRepository.getDailyContent()
                val showHadith = Random.nextBoolean()
                val motivation = resolveMotivationContent(
                    showHadith = showHadith,
                    hadithText = content.hadithText,
                    hadithSource = content.hadithSource,
                    surahMeaning = content.surahMeaning,
                    surahReference = content.surahReference
                )
                _uiState.update {
                    it.copy(
                        hadithText = content.hadithText,
                        hadithSource = content.hadithSource,
                        surahMeaning = content.surahMeaning,
                        surahReference = content.surahReference,
                        showHadithContent = showHadith,
                        motivationalQuote = motivation.first,
                        quoteSource = motivation.second
                    )
                }
            }
            clearSyncNotice()
        }
    }

    override fun onCleared() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(authPrefsListener)
        super.onCleared()
    }

    private fun restorePrayerTimeSyncState() {
        val source = sharedPreferences.getString(
            KEY_PRAYER_TIME_SOURCE,
            PRAYER_TIME_SOURCE_DEFAULT
        ).orEmpty().ifBlank { PRAYER_TIME_SOURCE_DEFAULT }
        val lastSyncAtMs = sharedPreferences.getLong(KEY_PRAYER_TIME_LAST_SYNC_AT, 0L)

        if (!sharedPreferences.contains(KEY_PRAYER_TIME_SOURCE)) {
            sharedPreferences.edit()
                .putString(KEY_PRAYER_TIME_SOURCE, PRAYER_TIME_SOURCE_DEFAULT)
                .apply()
        }

        _uiState.update {
            it.copy(
                prayerTimeSource = source,
                prayerTimeLastSyncAtMs = lastSyncAtMs,
                prayerTimeStatusText = buildPrayerTimeStatusText(source, lastSyncAtMs)
            )
        }
    }

    private fun markPrayerTimeSyncedFromApi() {
        val syncedAt = System.currentTimeMillis()
        sharedPreferences.edit()
            .putString(KEY_PRAYER_TIME_SOURCE, PRAYER_TIME_SOURCE_API)
            .putLong(KEY_PRAYER_TIME_LAST_SYNC_AT, syncedAt)
            .apply()
        _uiState.update {
            it.copy(
                prayerTimeSource = PRAYER_TIME_SOURCE_API,
                prayerTimeLastSyncAtMs = syncedAt,
                prayerTimeStatusText = buildPrayerTimeStatusText(
                    source = PRAYER_TIME_SOURCE_API,
                    lastSyncAtMs = syncedAt
                )
            )
        }
    }

    private fun markPrayerTimeUsingDefault() {
        sharedPreferences.edit()
            .putString(KEY_PRAYER_TIME_SOURCE, PRAYER_TIME_SOURCE_DEFAULT)
            .putLong(KEY_PRAYER_TIME_LAST_SYNC_AT, 0L)
            .apply()
        _uiState.update {
            it.copy(
                prayerTimeSource = PRAYER_TIME_SOURCE_DEFAULT,
                prayerTimeLastSyncAtMs = 0L,
                prayerTimeStatusText = buildPrayerTimeStatusText(
                    source = PRAYER_TIME_SOURCE_DEFAULT,
                    lastSyncAtMs = 0L
                )
            )
        }
    }

    private fun buildPrayerTimeStatusText(source: String, lastSyncAtMs: Long): String {
        return if (source == PRAYER_TIME_SOURCE_API) {
            if (lastSyncAtMs <= 0L) {
                "Jadwal sholat: API wilayah"
            } else {
                val formatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID"))
                val formattedTime = formatter.format(Date(lastSyncAtMs))
                "Jadwal sholat: sinkron $formattedTime"
            }
        } else {
            "Jadwal sholat: Default lokal"
        }
    }

    private fun seedDataIfNeeded() {
        launchSafely("seedDataIfNeeded") {
            val count = habitDao.getHabitCount()
            if (count == 0) {
                habitDao.insertAll(resolveDefaultHabits())
            }
            ensureFardhuTimesFallback()
            sharedPreferences.edit().putBoolean("hasSeeded", true).apply()
        }
    }

    private suspend fun ensureFardhuTimesFallback() {
        val fallbackHabits = fallbackFardhuHabits()
        val existingByName = habitDao.getFardhuHabits().associateBy { it.name }

        fallbackHabits.forEach { fallbackHabit ->
            val existing = existingByName[fallbackHabit.name]
            when {
                existing == null -> habitDao.insertHabit(fallbackHabit)
                !HHMM_REGEX.matches(existing.time) -> {
                    habitDao.updateFardhuTimeByName(fallbackHabit.name, fallbackHabit.time)
                }
            }
        }
    }

    private suspend fun applyDefaultFardhuTimes() {
        fallbackFardhuHabits().forEach { fallbackHabit ->
            habitDao.updateFardhuTimeByName(fallbackHabit.name, fallbackHabit.time)
        }
    }

    private suspend fun resolveDefaultHabits(): List<Habit> {
        val firestore = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
        val cloudFardhuHabits = if (firestore != null) {
            fetchCompleteFardhuHabitsFromFirestore(
                firestore = firestore,
                onError = { error ->
                    runCatching { FirebaseCrashlytics.getInstance().recordException(error) }
                }
            )
        } else {
            emptyList()
        }
        return if (cloudFardhuHabits.isNotEmpty()) cloudFardhuHabits else fallbackFardhuHabits()
    }

    private fun ensureRamadanFeatureHabits(config: com.islami.Aha.data.repository.AppFeatureConfig? = null) {
        launchSafely("ensureRamadanFeatureHabits") {
            val activeConfig = config ?: adminConfigRepository.featureConfig.value
            if (activeConfig.puasaWajibRamadanEnabled) {
                ensureHabitExists(
                    name = "Puasa Ramadan",
                    category = "Puasa Wajib",
                    icon = "plate",
                    description = "Puasa wajib di bulan Ramadan",
                    time = "",
                    reminderEnabled = false
                )
            }
            if (activeConfig.sholatTarawihEnabled) {
                ensureHabitExists(
                    name = "Sholat Tarawih",
                    category = "Sholat Tarawih",
                    icon = "moon",
                    description = "Sholat malam berjamaah di bulan Ramadan",
                    time = TARAWIH_DEFAULT_TIME,
                    reminderEnabled = true
                )
            }
        }
    }

    private suspend fun ensureHabitExists(
        name: String,
        category: String,
        icon: String,
        description: String,
        time: String,
        reminderEnabled: Boolean
    ) {
        val exists = habitDao.getHabitsSnapshot().any { it.name == name && it.category == category }
        if (exists) return
        habitDao.insertHabit(
            Habit(
                name = name,
                category = category,
                icon = icon,
                description = description,
                time = time,
                isReminderEnabled = reminderEnabled
            )
        )
    }

    private fun normalizeSelectedSubTabIndex(state: HomeUiState): Int {
        val maxIndex = (state.subTabCategories.size - 1).coerceAtLeast(0)
        return state.selectedSubTabIndex.coerceIn(0, maxIndex)
    }

    private fun loadHabits() {
        launchSafely("loadHabits") {
            habitDao.getHabits().collect { habits ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        allHabits = habits,
                        gregorianDate = getGregorianDateFormatted(),
                        hijriDate = getHijriDateFormatted()
                    )
                }
                // Recompute immediately when habits/times change so next-prayer info
                // appears without waiting for the next periodic tick.
                updateCurrentTimeAndPrayerInfo()
            }
        }
    }

    private fun loadDailyIslamicContent() {
        launchSafely("loadDailyIslamicContent") {
            val content = dailyIslamicContentRepository.getDailyContent()
            val showHadith = Random.nextBoolean()
            val motivation = resolveMotivationContent(
                showHadith = showHadith,
                hadithText = content.hadithText,
                hadithSource = content.hadithSource,
                surahMeaning = content.surahMeaning,
                surahReference = content.surahReference
            )
            _uiState.update {
                it.copy(
                    hadithText = content.hadithText,
                    hadithSource = content.hadithSource,
                    surahMeaning = content.surahMeaning,
                    surahReference = content.surahReference,
                    showHadithContent = showHadith,
                    motivationalQuote = motivation.first,
                    quoteSource = motivation.second
                )
            }
        }
    }

    private fun startPeriodicHomeUpdates() {
        launchSafely("startPeriodicHomeUpdates") {
            var lastContentRotateAtMs = System.currentTimeMillis()
            while (currentCoroutineContext().isActive) {
                updateCurrentTimeAndPrayerInfo()
                val nowMs = System.currentTimeMillis()
                if (nowMs - lastContentRotateAtMs >= ISLAMIC_CONTENT_ROTATE_INTERVAL_MS) {
                    rotateIslamicContentIfAvailable()
                    lastContentRotateAtMs = nowMs
                }
                delay(calculateDelayUntilNextMinute(nowMs))
            }
        }
    }

    private fun updateCurrentTimeAndPrayerInfo() {
        val now = Calendar.getInstance()
        val prayerInfo = calculateNextPrayerInfo(now, _uiState.value.allHabits)
        val newCurrentTime = getCurrentTimeFormatted()
        _uiState.update { current ->
            val nextRamadanStatus = resolveRamadanStatusText(
                now = now,
                isRamadanMonth = current.isRamadanMonth,
                ramadanScheduleEnabled = current.ramadanScheduleByLocationEnabled,
                imsakTime = current.ramadanImsakTime,
                iftarTime = current.ramadanIftarTime
            )
            if (
                current.currentTime == newCurrentTime &&
                current.nextPrayerName == prayerInfo.first &&
                current.nextPrayerTimeRemaining == prayerInfo.second &&
                current.nextPrayerProgress == prayerInfo.third &&
                current.ramadanStatusText == nextRamadanStatus
            ) {
                current
            } else {
                current.copy(
                    currentTime = newCurrentTime,
                    nextPrayerName = prayerInfo.first,
                    nextPrayerTimeRemaining = prayerInfo.second,
                    nextPrayerProgress = prayerInfo.third,
                    ramadanStatusText = nextRamadanStatus
                )
            }
        }
    }

    private fun resolveRamadanStatusText(
        now: Calendar,
        isRamadanMonth: Boolean,
        ramadanScheduleEnabled: Boolean,
        imsakTime: String,
        iftarTime: String
    ): String {
        if (!isRamadanMonth || !ramadanScheduleEnabled) return ""
        val imsak = parseHourMinute(imsakTime) ?: return ""
        val iftar = parseHourMinute(iftarTime) ?: return ""
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val imsakMinutes = imsak.first * 60 + imsak.second
        val iftarMinutes = iftar.first * 60 + iftar.second
        return when {
            nowMinutes < imsakMinutes -> {
                "Imsak dalam ${formatRemainingTime(imsakMinutes - nowMinutes)}"
            }
            nowMinutes < iftarMinutes -> {
                "Berbuka dalam ${formatRemainingTime(iftarMinutes - nowMinutes)}"
            }
            else -> "Waktu berbuka telah tiba"
        }
    }

    private fun formatRemainingTime(totalMinutes: Int): String {
        val safe = totalMinutes.coerceAtLeast(0)
        val hours = safe / 60
        val minutes = safe % 60
        return if (hours > 0) "$hours jam $minutes menit" else "$minutes menit"
    }

    private fun rotateIslamicContentIfAvailable() {
        _uiState.update { current ->
            val canRotate = current.hadithSource.isNotBlank() && current.surahReference.isNotBlank()
            if (!canRotate) {
                current
            } else {
                val nextShowHadith = !current.showHadithContent
                val motivation = resolveMotivationContent(
                    showHadith = nextShowHadith,
                    hadithText = current.hadithText,
                    hadithSource = current.hadithSource,
                    surahMeaning = current.surahMeaning,
                    surahReference = current.surahReference
                )
                current.copy(
                    showHadithContent = nextShowHadith,
                    motivationalQuote = motivation.first,
                    quoteSource = motivation.second
                )
            }
        }
    }

    private fun calculateDelayUntilNextMinute(nowMs: Long): Long {
        val elapsedWithinWindow = nowMs % TIME_UPDATE_INTERVAL_MS
        return if (elapsedWithinWindow == 0L) TIME_UPDATE_INTERVAL_MS
        else TIME_UPDATE_INTERVAL_MS - elapsedWithinWindow
    }

    private fun resolveMotivationContent(
        showHadith: Boolean,
        hadithText: String,
        hadithSource: String,
        surahMeaning: String,
        surahReference: String
    ): Pair<String, String> {
        return if (showHadith) {
            hadithText to hadithSource
        } else {
            surahMeaning to surahReference
        }
    }

    fun toggleHabitCompletion(habit: Habit) {
        viewModelScope.launch {
            val willComplete = !habit.isCompleted
            val updatedHabit = habit.copy(isCompleted = willComplete)
            val todayKey = DateUtils.getTodayKey()
            val habitKey = "default_${habit.id}"
            appDatabase.withTransaction {
                habitDao.updateHabit(updatedHabit)
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
    }

    fun toggleReminderEnabled(habit: Habit) {
        viewModelScope.launch {
            val willEnable = !habit.isReminderEnabled
            if (willEnable) {
                val capability = NotificationScheduler.getNotificationCapability(context)
                if (capability != NotificationScheduler.NotificationCapability.AVAILABLE) {
                    showNotificationCapabilityNotice(capability)
                    return@launch
                }
            }
            val updatedHabit = habit.copy(isReminderEnabled = willEnable)
            habitDao.updateHabit(updatedHabit)

            val reminderTime = parseHourMinute(habit.time)
            if (willEnable && isGlobalNotificationEnabled() && reminderTime != null) {
                NotificationScheduler.scheduleHabitReminder(
                    context = context,
                    habitId = "default_${habit.id}",
                    habitName = habit.name,
                    hour = reminderTime.first,
                    minute = reminderTime.second
                )
            } else {
                NotificationScheduler.cancelHabitReminder(context, "default_${habit.id}")
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
        if (willEnable) {
            val capability = NotificationScheduler.getNotificationCapability(context)
            if (capability != NotificationScheduler.NotificationCapability.AVAILABLE) {
                showNotificationCapabilityNotice(capability)
                return
            }
        }
        sunnahHabitSharedViewModel.toggleReminder(id)

        val reminderTime = parseHourMinute(habit.reminderTime)
        if (willEnable && reminderTime != null && isGlobalNotificationEnabled()) {
            NotificationScheduler.scheduleHabitReminder(
                context = context,
                habitId = id,
                habitName = habit.name,
                hour = reminderTime.first,
                minute = reminderTime.second
            )
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
        refreshLocation(force = true, closeRefreshingOnFinish = true)
        // Timeout fallback - dismiss refresh after 5 seconds
        viewModelScope.launch {
            delay(REFRESH_TIMEOUT_MS)
            if (_uiState.value.isRefreshing) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun refreshLocation(force: Boolean = false, closeRefreshingOnFinish: Boolean = false) {
        val now = System.currentTimeMillis()
        if (_uiState.value.isLocationLoading) {
            if (closeRefreshingOnFinish) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
            return
        }
        if (!force) {
            val elapsed = now - lastLocationRefreshAtMs
            if (elapsed in 1 until LOCATION_REFRESH_MIN_INTERVAL_MS) {
                return
            }
        }
        lastLocationRefreshAtMs = now
        val cached = LocationHelper.getCachedLocationQuick(context)
        _uiState.update { current ->
            current.copy(
                location = cached?.cityName ?: current.location,
                isLocationLoading = true
            )
        }
        LocationHelper.getLastLocation(
            context = context,
            onResult = { result ->
                launchSafely("syncPrayerTimesByLocation") {
                    try {
                        if (_uiState.value.fardhuScheduleByLocationEnabled) {
                            syncPrayerTimesByLocation(result.latitude, result.longitude)
                        } else {
                            clearSyncNotice()
                        }
                    } finally {
                        _uiState.update { current ->
                            current.copy(
                                location = result.cityName,
                                isLocationLoading = false,
                                isRefreshing = if (closeRefreshingOnFinish) false else current.isRefreshing
                            )
                        }
                    }
                }
            },
            onError = { message ->
                _uiState.update { current ->
                    current.copy(
                        location = message,
                        isLocationLoading = false,
                        isRefreshing = if (closeRefreshingOnFinish) false else current.isRefreshing
                    )
                }
            }
        )
    }

    private suspend fun syncPrayerTimesByLocation(latitude: Double, longitude: Double) {
        if (!_uiState.value.fardhuScheduleByLocationEnabled) return

        val times = when (
            val result = withContext(Dispatchers.IO) {
                PrayerTimeApiService.fetchTodayFardhuTimesResult(latitude, longitude)
            }
        ) {
            is PrayerTimeApiService.FetchResult.Success -> result.times
            is PrayerTimeApiService.FetchResult.Failure -> {
                showSyncNotice(result.userMessage)
                return
            }
        }

        val mapping = listOf(
            "Sholat Subuh" to times.subuh,
            "Sholat Dzuhur" to times.dzuhur,
            "Sholat Ashar" to times.ashar,
            "Sholat Maghrib" to times.maghrib,
            "Sholat Isya" to times.isya
        )

        mapping.forEach { (habitName, time) ->
            if (HHMM_REGEX.matches(time)) {
                habitDao.updateFardhuTimeByName(habitName, time)
            }
        }

        markPrayerTimeSyncedFromApi()

        _uiState.update {
            it.copy(
                ramadanImsakTime = times.imsak,
                ramadanIftarTime = times.maghrib
            )
        }
        updateCurrentTimeAndPrayerInfo()

        val globalEnabled = isGlobalNotificationEnabled()

        // Keep user reminder behavior intact: only reschedule habits that are currently enabled.
        habitDao.getFardhuHabits().forEach { habit ->
            if (!globalEnabled) {
                NotificationScheduler.cancelHabitReminder(context, "default_${habit.id}")
                return@forEach
            }
            val reminderTime = parseHourMinute(habit.time)
            if (habit.isReminderEnabled && reminderTime != null) {
                NotificationScheduler.scheduleHabitReminder(
                    context = context,
                    habitId = "default_${habit.id}",
                    habitName = habit.name,
                    hour = reminderTime.first,
                    minute = reminderTime.second
                )
            }
        }
        clearSyncNotice()
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

    private fun isGlobalNotificationEnabled(): Boolean {
        return sharedPreferences.getBoolean(NotificationScheduler.KEY_GLOBAL_NOTIFICATION_ENABLED, true)
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showSyncNotice = if (error.isLikelyNetworkErrorForHome()) true else it.showSyncNotice,
                            syncNoticeMessage = if (error.isLikelyNetworkErrorForHome()) {
                                DEFAULT_OFFLINE_NOTICE
                            } else {
                                it.syncNoticeMessage
                            }
                        )
                    }
                }
        }
    }

    private fun showSyncNotice(message: String = DEFAULT_OFFLINE_NOTICE) {
        _uiState.update {
            it.copy(
                showSyncNotice = true,
                syncNoticeMessage = message
            )
        }
    }

    private fun showNotificationCapabilityNotice(
        capability: NotificationScheduler.NotificationCapability
    ) {
        val message = when (capability) {
            NotificationScheduler.NotificationCapability.PERMISSION_DENIED ->
                context.getString(com.islami.Aha.R.string.notification_permission_required_message)
            NotificationScheduler.NotificationCapability.SYSTEM_DISABLED ->
                context.getString(com.islami.Aha.R.string.notification_system_disabled_message)
            NotificationScheduler.NotificationCapability.CHANNEL_DISABLED ->
                context.getString(com.islami.Aha.R.string.notification_channel_disabled_message)
            NotificationScheduler.NotificationCapability.AVAILABLE -> return
        }
        _uiState.update {
            it.copy(
                showSyncNotice = true,
                syncNoticeMessage = message
            )
        }
    }

    private fun clearSyncNotice() {
        _uiState.update { current ->
            if (!current.showSyncNotice) return@update current
            current.copy(showSyncNotice = false, syncNoticeMessage = "")
        }
    }

}
