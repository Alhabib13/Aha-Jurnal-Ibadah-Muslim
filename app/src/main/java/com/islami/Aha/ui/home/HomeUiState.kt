package com.islami.Aha.ui.home

import com.islami.Aha.data.model.Habit
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import com.islami.Aha.util.DateUtils
import com.islami.Aha.util.GenderProfile
import java.util.Calendar

/** Satu sesi tilawah hari ini (satu baris research_logs). */
data class TilawahSessionUi(val slotKey: String, val halaman: Int, val jam: String)

/** Posisi terakhir Iqro/metode sejenis. */
data class IqroProgressUi(val jilid: Int, val halaman: Int, val tanggal: String)

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
    val showGenderPrompt: Boolean = false,
    val showResearchNotice: Boolean = false,
    val dzikirPagiDone: Boolean = false,
    val dzikirPetangDone: Boolean = false,
    val tilawahSessions: List<TilawahSessionUi> = emptyList(),
    val iqroProgress: IqroProgressUi? = null
) {
    val dzikirDoneCount: Int
        get() = (if (dzikirPagiDone) 1 else 0) + (if (dzikirPetangDone) 1 else 0)

    val tilawahHalamanToday: Int
        get() = tilawahSessions.sumOf { it.halaman }

    val isDzikirCategory: Boolean
        get() = selectedMainCategory == "Dzikir"

    val isTilawahCategory: Boolean
        get() = selectedMainCategory == "Tilawah"

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

    val comingSoonCategories: List<String> = emptyList()

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
            if (isDzikirCategory) return dzikirDoneCount
            val habitCount = if (showRamadanUnifiedCard) {
                ramadanUnifiedHabits.count { it.isCompleted }
            } else {
                filteredHabits.count { it.isCompleted }
            }
            return habitCount + filteredSunnahHabits.count { it.isCompletedToday }
        }

    val totalHabitsCount: Int
        get() {
            if (isDzikirCategory) return 2
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
            // Dzikir & tilawah tetap tampil saat Mode Cuti (tanpa udzur).
            // MENUNGGU VALIDASI PAKAR: tilawah saat haid khilafiyah; tidak disembunyikan = netral.
            "Dzikir" -> "$dzikirDoneCount/2"
            "Tilawah" -> "$tilawahHalamanToday hlm"
            else -> "Segera"
        }
    }

    val showRamadanScheduleCard: Boolean
        get() = isRamadanMonth &&
            ramadanScheduleByLocationEnabled &&
            ramadanImsakTime.isNotBlank() &&
            ramadanIftarTime.isNotBlank()
}
