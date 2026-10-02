package com.islami.Aha.ui.home

import com.islami.Aha.data.model.Habit
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import com.islami.Aha.util.DateUtils
import com.islami.Aha.util.GenderProfile
import java.util.Calendar

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
    val isJumatEnabled: Boolean = false,
    val isHaidhMode: Boolean = false,
    val genderProfile: GenderProfile = GenderProfile.UNSPECIFIED,
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
    val snackbarMessage: String? = null,
    val showSyncNotice: Boolean = false,
    val syncNoticeMessage: String = "",
    val isRamadanMonth: Boolean = DateUtils.isRamadanMonth(),
    val showGenderPrompt: Boolean = false
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
                    ramadanPuasaHabit?.takeIf { !isHaidhMode || it.isCompleted }?.let { add(it) }
                }
                if (isTarawihContext && sholatTarawihEnabled) {
                    ramadanTarawihHabit?.takeIf { !isHaidhMode || it.isCompleted }?.let { add(it) }
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
            "Puasa" -> listOf("Puasa Sunnah", "Puasa Wajib")
            else -> emptyList()
        }

    val subTabDisplayNames: List<String>
        get() = when (selectedMainCategory) {
            "Sholat" -> buildList {
                add("Fardhu")
                add("Sunnah")
                if (sholatTarawihEnabled && isRamadanMonth) {
                    add("Tarawih")
                }
            }
            "Puasa" -> listOf("Sunnah", "Wajib (Ramadan)")
            else -> emptyList()
        }

    val comingSoonCategories = listOf("Dzikir", "Tilawah")

    val isComingSoon: Boolean
        get() = selectedMainCategory in comingSoonCategories

    val isFriday: Boolean
        get() {
            val calendar = Calendar.getInstance()
            return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        }

    val filteredHabits: List<Habit>
        get() {
            if (isComingSoon) return emptyList()
            val subCategory = subTabCategories.getOrNull(selectedSubTabIndex) ?: return emptyList()
            return allHabits.filter { habit ->
                if (habit.category != subCategory) return@filter false
                if (habit.category == "Puasa Wajib" && (!isRamadanMonth || !puasaWajibRamadanEnabled)) return@filter false
                if (habit.category == "Sholat Tarawih" && (!isRamadanMonth || !sholatTarawihEnabled)) return@filter false
                
                if (isHaidhMode && (selectedMainCategory == "Sholat" || selectedMainCategory == "Puasa")) {
                    return@filter false
                }
                
                true
            }.map { habit ->
                if (isJumatEnabled && isFriday && habit.name == "Sholat Dzuhur") {
                    habit.copy(name = "Sholat Jumat")
                } else {
                    habit
                }
            }
        }

    val filteredSunnahHabits: List<SunnahHabit>
        get() {
            if (isComingSoon) return emptyList()
            return when {
                selectedMainCategory == "Sholat" && selectedSubTabIndex == 1 ->
                    if (isHaidhMode) emptyList() else sunnahHabits.filter { it.category == SunnahCategoryType.SHOLAT }
                selectedMainCategory == "Puasa" && selectedSubTabIndex == 1 ->
                    if (isHaidhMode) emptyList() else sunnahHabits.filter { it.category == SunnahCategoryType.PUASA }
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
                if (isHaidhMode) return "Cuti"
                val habits = allHabits.filter { it.category.startsWith("Sholat") }
                    .filterNot { it.category == "Sholat Tarawih" && (!isRamadanMonth || !sholatTarawihEnabled) }
                val sunnah = sunnahHabits.filter { it.category == SunnahCategoryType.SHOLAT }
                val completed = habits.count { it.isCompleted } + sunnah.count { it.isCompletedToday }
                val total = habits.size + sunnah.size
                "$completed/$total"
            }
            "Puasa" -> {
                if (isHaidhMode) return "Cuti"
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
