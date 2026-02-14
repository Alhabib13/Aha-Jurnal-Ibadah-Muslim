package com.islami.Aha.ui.addhabit

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.islami.Aha.ui.shared.SunnahHabitSharedViewModel
import com.islami.Aha.util.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class SunnahCategoryType(val displayName: String) {
    SHOLAT("Sholat Sunnah"),
    PUASA("Puasa Sunnah")
}

enum class FrequencyType(val displayName: String) {
    EVERY_DAY("Setiap hari"),
    SPECIFIC_DAYS("Hari tertentu")
}

data class SunnahHabitPreset(
    val id: String,
    val name: String,
    val description: String
)

data class WeekDay(val id: Int, val shortName: String, val fullName: String)

val WEEK_DAYS = listOf(
    WeekDay(1, "Sen", "Senin"),
    WeekDay(2, "Sel", "Selasa"),
    WeekDay(3, "Rab", "Rabu"),
    WeekDay(4, "Kam", "Kamis"),
    WeekDay(5, "Jum", "Jumat"),
    WeekDay(6, "Sab", "Sabtu"),
    WeekDay(7, "Min", "Minggu")
)

val RAKAAT_OPTIONS = listOf(2, 4, 6, 8, 10, 12)

// Main presets (shown as cards)
private val SHOLAT_PRESETS = listOf(
    SunnahHabitPreset("dhuha", "Dhuha", "Sholat sunnah pagi (06:00\u201311:00)"),
    SunnahHabitPreset("tahajud", "Tahajud", "Sholat malam setelah tidur"),
    SunnahHabitPreset("witir", "Witir", "Sholat penutup malam"),
    SunnahHabitPreset("rawatib", "Rawatib", "Sebelum & sesudah sholat fardhu"),
    SunnahHabitPreset(LAINNYA_ID, "Lainnya", "Pilih atau tulis sendiri")
)

private val PUASA_PRESETS = listOf(
    SunnahHabitPreset("senin_kamis", "Senin Kamis", "Puasa sunnah rutin mingguan"),
    SunnahHabitPreset("ayyamul_bidh", "Ayyamul Bidh", "Puasa tanggal 13\u201315 Hijriah"),
    SunnahHabitPreset("puasa_daud", "Puasa Daud", "Puasa selang-seling sehari"),
    SunnahHabitPreset("puasa_syawal", "Puasa Syawal", "6 hari puasa di bulan Syawal"),
    SunnahHabitPreset(LAINNYA_ID, "Lainnya", "Pilih atau tulis sendiri")
)

// Extra presets shown when "Lainnya" is selected
private val SHOLAT_EXTRA = listOf(
    SunnahHabitPreset("taubat", "Taubat", "Sholat sunnah taubat"),
    SunnahHabitPreset("istikharah", "Istikharah", "Sholat meminta petunjuk"),
    SunnahHabitPreset("qadha", "Qadha", "Sholat pengganti yang terlewat")
)

private val PUASA_EXTRA = listOf(
    SunnahHabitPreset("pengganti_ramadan", "Pengganti Ramadan", "Puasa pengganti Ramadan"),
    SunnahHabitPreset("puasa_nazar", "Puasa Nazar", "Puasa karena nazar")
)

const val LAINNYA_ID = "lainnya"
private const val CUSTOM_ID = "custom"

fun getPresetsFor(category: SunnahCategoryType): List<SunnahHabitPreset> {
    return when (category) {
        SunnahCategoryType.SHOLAT -> SHOLAT_PRESETS
        SunnahCategoryType.PUASA -> PUASA_PRESETS
    }
}

fun getExtraPresetsFor(category: SunnahCategoryType): List<SunnahHabitPreset> {
    return when (category) {
        SunnahCategoryType.SHOLAT -> SHOLAT_EXTRA
        SunnahCategoryType.PUASA -> PUASA_EXTRA
    }
}

data class AddHabitUiState(
    val selectedCategory: SunnahCategoryType = SunnahCategoryType.SHOLAT,
    val selectedHabitId: String = SHOLAT_PRESETS.first().id,
    val isCustomHabit: Boolean = false,
    val customHabitName: String = "",
    val selectedExtraId: String? = null,
    val selectedRakaat: Int? = null,
    val frequencyType: FrequencyType = FrequencyType.EVERY_DAY,
    val selectedDays: Set<Int> = emptySet(),
    val isReminderEnabled: Boolean = false,
    val reminderHour: Int = 5,
    val reminderMinute: Int = 0,
    val saveSuccess: Boolean = false,
    val showTimePicker: Boolean = false
) {
    val availableHabits: List<SunnahHabitPreset>
        get() = getPresetsFor(selectedCategory)

    val extraHabits: List<SunnahHabitPreset>
        get() = getExtraPresetsFor(selectedCategory)

    val isLainnya: Boolean
        get() = selectedHabitId == LAINNYA_ID

    val selectedHabitName: String
        get() = when {
            isCustomHabit -> customHabitName
            selectedExtraId != null -> extraHabits.firstOrNull { it.id == selectedExtraId }?.name.orEmpty()
            else -> availableHabits.firstOrNull { it.id == selectedHabitId }?.name
                ?: availableHabits.first().name
        }

    val showRakaat: Boolean
        get() = selectedCategory == SunnahCategoryType.SHOLAT
}

@HiltViewModel
class AddHabitViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sunnahHabitSharedViewModel: SunnahHabitSharedViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddHabitUiState())
    val uiState: StateFlow<AddHabitUiState> = _uiState.asStateFlow()

    fun selectCategory(category: SunnahCategoryType) {
        _uiState.update { current ->
            val defaultHabit = getPresetsFor(category).first().id
            current.copy(
                selectedCategory = category,
                selectedHabitId = defaultHabit,
                isCustomHabit = false,
                customHabitName = "",
                selectedExtraId = null,
                selectedRakaat = if (category == SunnahCategoryType.PUASA) null else current.selectedRakaat,
                frequencyType = FrequencyType.EVERY_DAY,
                selectedDays = emptySet()
            )
        }
    }

    fun selectHabit(habitId: String) {
        _uiState.update {
            it.copy(
                selectedHabitId = habitId,
                isCustomHabit = false,
                customHabitName = "",
                selectedExtraId = null
            )
        }
    }

    fun selectExtraHabit(extraId: String) {
        _uiState.update {
            it.copy(
                selectedExtraId = extraId,
                isCustomHabit = false,
                customHabitName = ""
            )
        }
    }

    fun enableCustomInput() {
        _uiState.update {
            it.copy(
                isCustomHabit = true,
                selectedExtraId = null
            )
        }
    }

    fun updateCustomHabitName(name: String) {
        _uiState.update { it.copy(customHabitName = name) }
    }

    fun selectRakaat(rakaat: Int) {
        _uiState.update { it.copy(selectedRakaat = rakaat) }
    }

    fun selectFrequency(type: FrequencyType) {
        _uiState.update {
            it.copy(
                frequencyType = type,
                selectedDays = if (type == FrequencyType.EVERY_DAY) emptySet() else it.selectedDays
            )
        }
    }

    fun toggleDay(dayId: Int) {
        _uiState.update { current ->
            val updated = current.selectedDays.toMutableSet()
            if (updated.contains(dayId)) {
                if (updated.size > 1) updated.remove(dayId)
            } else {
                updated.add(dayId)
            }
            current.copy(selectedDays = updated)
        }
    }

    fun toggleReminder() {
        _uiState.update { it.copy(isReminderEnabled = !it.isReminderEnabled) }
    }

    fun showTimePicker() {
        _uiState.update { it.copy(showTimePicker = true) }
    }

    fun hideTimePicker() {
        _uiState.update { it.copy(showTimePicker = false) }
    }

    fun onReminderTimeChange(hour: Int, minute: Int) {
        _uiState.update { it.copy(reminderHour = hour, reminderMinute = minute, showTimePicker = false) }
    }

    fun saveHabit() {
        val state = _uiState.value
        val habitName = state.selectedHabitName
        if (habitName.isBlank()) return

        Log.d("AddHabitVM", "Saving habit: name=$habitName, category=${state.selectedCategory}, reminder=${state.isReminderEnabled}")

        val frequencyLabel = buildFrequencyLabel(state)
        val reminderTime = if (state.isReminderEnabled) getFormattedReminderTime(state) else null

        val habitId = sunnahHabitSharedViewModel.addHabit(
            name = habitName,
            category = state.selectedCategory,
            frequencyLabel = frequencyLabel,
            reminderEnabled = state.isReminderEnabled,
            reminderTime = reminderTime
        )

        // Schedule notification if reminder is enabled
        if (state.isReminderEnabled) {
            Log.d("AddHabitVM", "Scheduling reminder: habitId=$habitId at ${state.reminderHour}:${state.reminderMinute}")
            NotificationScheduler.scheduleHabitReminder(
                context = context,
                habitId = habitId,
                habitName = habitName,
                hour = state.reminderHour,
                minute = state.reminderMinute
            )
        }

        _uiState.update { it.copy(saveSuccess = true) }
    }

    fun resetState() {
        _uiState.value = AddHabitUiState()
    }

    fun getFormattedReminderTime(state: AddHabitUiState = _uiState.value): String {
        return String.format("%02d:%02d", state.reminderHour, state.reminderMinute)
    }

    private fun buildFrequencyLabel(state: AddHabitUiState): String {
        return when (state.frequencyType) {
            FrequencyType.EVERY_DAY -> "Setiap hari"
            FrequencyType.SPECIFIC_DAYS -> {
                val days = state.selectedDays.sorted().mapNotNull { id ->
                    WEEK_DAYS.firstOrNull { it.id == id }?.shortName
                }.joinToString(" \u2022 ")
                if (days.isNotEmpty()) "Hari: $days" else "Setiap hari"
            }
        }
    }
}
