package com.islami.Aha.ui.addhabit

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.islami.Aha.BuildConfig
import com.islami.Aha.data.dzikir.DzikirHabitDraft
import com.islami.Aha.data.dzikir.DzikirHabitRepository
import com.islami.Aha.data.dzikir.DzikirKatalogEntry
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.ui.shared.SunnahHabitSharedViewModel
import com.islami.Aha.util.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import java.util.Locale
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

/**
 * Satu dzikir yang sedang dipilih di Add Habit. [key] = slug, kecuali dzikir_lainnya
 * yang boleh lebih dari satu ("dzikir_lainnya#1", "#2", ...).
 */
data class DzikirPilihan(
    val key: String,
    val slug: String,
    val mode: String = ResearchSchema.ModePencatatan.CENTANG,
    /** Kosong secara bawaan; tidak ada nilai anjuran. */
    val targetText: String = "",
    /** Wajib untuk dzikir_lainnya. */
    val namaAsli: String = ""
) {
    val target: Int? get() = targetText.toIntOrNull()?.takeIf { it in ResearchSchema.TARGET_HITUNGAN }
    val targetTidakSah: Boolean get() = targetText.isNotEmpty() && target == null
}

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
    val isHaidhMode: Boolean = false,
    val isReminderEnabled: Boolean = false,
    val reminderHour: Int = 5,
    val reminderMinute: Int = 0,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val showTimePicker: Boolean = false,
    val savedHabitName: String = "",
    /** Kategori Dzikir dipilih (terpisah dari SunnahCategoryType: disimpan ke dzikir_habits). */
    val isDzikir: Boolean = false,
    val dzikirKatalog: List<DzikirKatalogEntry> = emptyList(),
    val dzikirPilihan: List<DzikirPilihan> = emptyList(),
    /** Slug non-lainnya yang sudah ada di daftar pengguna (satu habit per slug). */
    val dzikirSlugSudahAda: Set<String> = emptySet(),
    val dzikirError: String? = null
) {
    /**
     * Selama Mode Cuti hanya kategori yang tetap aktif menurut ModeCutiPolicy yang bisa
     * ditambahkan (bawaan: Dzikir). Sholat & Puasa Sunnah tetap terkunci.
     */
    val dzikirBolehSaatCuti: Boolean
        get() = com.islami.Aha.data.research.ModeCutiPolicy.aktif(ResearchSchema.Jenis.DZIKIR, modeCutiAktif = true)

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
    private val sunnahHabitSharedViewModel: SunnahHabitSharedViewModel,
    private val dzikirHabitRepository: DzikirHabitRepository
) : ViewModel() {

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message)
        }
    }

    companion object {
        private const val TAG = "AddHabitVM"
    }

    private val _uiState = MutableStateFlow(AddHabitUiState())
    val uiState: StateFlow<AddHabitUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            com.islami.Aha.util.UserPreferencesManager.isHaidhMode.collect { isHaidhMode ->
                _uiState.update { it.copy(isHaidhMode = isHaidhMode) }
                // Mode Cuti: satu-satunya kategori yang terbuka adalah Dzikir.
                if (isHaidhMode && _uiState.value.dzikirBolehSaatCuti && !_uiState.value.isDzikir) {
                    selectDzikirCategory()
                }
            }
        }
    }

    fun selectCategory(category: SunnahCategoryType) {
        if (_uiState.value.isHaidhMode) return // Sholat & Puasa Sunnah terkunci saat Mode Cuti
        _uiState.update { current ->
            val defaultHabit = getPresetsFor(category).first().id
            current.copy(
                isDzikir = false,
                dzikirPilihan = emptyList(),
                dzikirError = null,
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

    // ---------- Dzikir ----------

    fun selectDzikirCategory() {
        _uiState.update { it.copy(isDzikir = true, dzikirError = null) }
        viewModelScope.launch {
            val katalog = dzikirHabitRepository.katalog
            val sudahAda = dzikirHabitRepository.getAll()
                .filter { it.slug != ResearchSchema.Detail.DZIKIR_LAINNYA }
                .map { it.slug }.toSet()
            _uiState.update { it.copy(dzikirKatalog = katalog, dzikirSlugSudahAda = sudahAda) }
        }
    }

    /** Pilih / batal pilih satu entri katalog. dzikir_lainnya: menambah satu isian baru. */
    fun toggleDzikirEntry(slug: String) {
        _uiState.update { s ->
            if (slug == ResearchSchema.Detail.DZIKIR_LAINNYA) {
                val lainnya = s.dzikirPilihan.filter { it.slug == slug }
                if (lainnya.isNotEmpty()) {
                    s.copy(dzikirPilihan = s.dzikirPilihan.filterNot { it.slug == slug }, dzikirError = null)
                } else {
                    s.copy(dzikirPilihan = s.dzikirPilihan + DzikirPilihan("$slug#1", slug), dzikirError = null)
                }
            } else if (slug in s.dzikirSlugSudahAda) {
                s
            } else if (s.dzikirPilihan.any { it.key == slug }) {
                s.copy(dzikirPilihan = s.dzikirPilihan.filterNot { it.key == slug }, dzikirError = null)
            } else {
                s.copy(dzikirPilihan = s.dzikirPilihan + DzikirPilihan(slug, slug), dzikirError = null)
            }
        }
    }

    fun addDzikirLainnya() {
        _uiState.update { s ->
            val slug = ResearchSchema.Detail.DZIKIR_LAINNYA
            val next = (s.dzikirPilihan.filter { it.slug == slug }
                .mapNotNull { it.key.substringAfter('#').toIntOrNull() }.maxOrNull() ?: 0) + 1
            s.copy(dzikirPilihan = s.dzikirPilihan + DzikirPilihan("$slug#$next", slug), dzikirError = null)
        }
    }

    fun removeDzikirPilihan(key: String) {
        _uiState.update { s -> s.copy(dzikirPilihan = s.dzikirPilihan.filterNot { it.key == key }, dzikirError = null) }
    }

    fun updateDzikirTarget(key: String, text: String) {
        val digits = text.filter(Char::isDigit).take(6)
        updatePilihan(key) { it.copy(targetText = digits) }
    }

    fun updateDzikirNama(key: String, nama: String) {
        updatePilihan(key) { it.copy(namaAsli = nama.take(100)) }
    }

    /** Mode "layar" hanya bisa dipilih bila entri katalog punya bacaan (versi ini: belum ada). */
    fun selectDzikirMode(key: String, mode: String) {
        val s = _uiState.value
        val pilihan = s.dzikirPilihan.firstOrNull { it.key == key } ?: return
        val entry = s.dzikirKatalog.firstOrNull { it.slug == pilihan.slug } ?: return
        if (mode !in entry.modeTersedia) return
        updatePilihan(key) { it.copy(mode = mode) }
    }

    private fun updatePilihan(key: String, f: (DzikirPilihan) -> DzikirPilihan) {
        _uiState.update { s ->
            s.copy(dzikirPilihan = s.dzikirPilihan.map { if (it.key == key) f(it) else it }, dzikirError = null)
        }
    }

    private fun saveDzikir(state: AddHabitUiState) {
        if (state.dzikirPilihan.isEmpty()) {
            _uiState.update { it.copy(dzikirError = "Pilih minimal satu dzikir.") }
            return
        }
        if (state.dzikirPilihan.any { it.targetTidakSah }) {
            _uiState.update { it.copy(dzikirError = "Target hitungan harus angka 1–100.000, atau dikosongkan.") }
            return
        }
        val drafts = state.dzikirPilihan.map {
            DzikirHabitDraft(
                slug = it.slug,
                modePencatatan = it.mode,
                targetHitungan = it.target,
                namaDetailAsli = it.namaAsli.takeIf { _ -> it.slug == ResearchSchema.Detail.DZIKIR_LAINNYA }
            )
        }
        _uiState.update { it.copy(isSaving = true, dzikirError = null) }
        viewModelScope.launch {
            val error = runCatching { dzikirHabitRepository.addAll(drafts) }
                .getOrElse { "Gagal menyimpan. Coba lagi." }
            _uiState.update {
                if (error == null) {
                    it.copy(isSaving = false, saveSuccess = true, savedHabitName = "${drafts.size} dzikir berhasil disimpan")
                } else {
                    it.copy(isSaving = false, dzikirError = error)
                }
            }
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
        if (state.isDzikir) {
            if (state.isHaidhMode && !state.dzikirBolehSaatCuti) return
            saveDzikir(state)
            return
        }
        if (state.isHaidhMode) return
        val habitName = state.selectedHabitName
        if (habitName.isBlank()) return
        _uiState.update { it.copy(isSaving = true) }

        debugLog("Saving habit: name=$habitName, category=${state.selectedCategory}, reminder=${state.isReminderEnabled}")

        val frequencyLabel = buildFrequencyLabel(state)

        // Jika global notification dinonaktifkan, simpan reminderEnabled=false agar
        // state DB konsisten dengan kondisi nyata (tidak ada alarm terjadwal).
        // Saat user mengaktifkan kembali global notification di Settings,
        // SettingsViewModel.rescheduleAllActiveReminders() hanya menjadwalkan
        // habit yang reminderEnabled=true — sehingga tidak ada efek sampingan.
        val globalEnabled = NotificationScheduler.isGlobalNotificationEnabled(context)
        val effectiveReminderEnabled = state.isReminderEnabled && globalEnabled
        val reminderTime = if (effectiveReminderEnabled) getFormattedReminderTime(state) else null

        val habitId = sunnahHabitSharedViewModel.addHabit(
            name = habitName,
            category = state.selectedCategory,
            frequencyLabel = frequencyLabel,
            rakaat = if (state.selectedCategory == SunnahCategoryType.SHOLAT) state.selectedRakaat else null,
            reminderEnabled = effectiveReminderEnabled,
            reminderTime = reminderTime
        )

        if (effectiveReminderEnabled) {
            debugLog("Scheduling reminder: habitId=$habitId at ${state.reminderHour}:${state.reminderMinute}")
            NotificationScheduler.scheduleHabitReminder(
                context = context,
                habitId = habitId,
                habitName = habitName,
                hour = state.reminderHour,
                minute = state.reminderMinute
            )
        } else if (state.isReminderEnabled) {
            // Reminder dipilih user tapi global off → simpan tanpa alarm
            debugLog("Global notification disabled — saving '$habitName' with reminderEnabled=false")
        }

        _uiState.update {
            it.copy(
                isSaving = false,
                saveSuccess = true,
                savedHabitName = if (effectiveReminderEnabled) {
                    "$habitName disimpan dengan pengingat"
                } else {
                    "$habitName berhasil disimpan"
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = AddHabitUiState()
    }

    fun getFormattedReminderTime(state: AddHabitUiState = _uiState.value): String {
        return String.format(Locale.ROOT, "%02d:%02d", state.reminderHour, state.reminderMinute)
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
