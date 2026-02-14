package com.islami.Aha.ui.notification

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.model.Habit
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.shared.SunnahHabitSharedViewModel
import com.islami.Aha.util.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val isLoading: Boolean = true,
    val habits: List<Habit> = emptyList(),
    val sunnahHabits: List<SunnahHabit> = emptyList(),
    val globalNotificationEnabled: Boolean = true,
    val showDeleteConfirmation: Boolean = false,
    val habitToDelete: Habit? = null,
    val sunnahHabitToDelete: SunnahHabit? = null
) {
    val deleteTargetName: String
        get() = habitToDelete?.name ?: sunnahHabitToDelete?.name ?: ""
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitDao: HabitDao,
    private val sharedPreferences: SharedPreferences,
    private val sunnahHabitSharedViewModel: SunnahHabitSharedViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        seedDataIfNeeded()
        loadHabitsAsReminders()
        observeSunnahHabits()
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
        Habit(name = "Sholat Subuh", category = "Sholat Fardhu", icon = "sunrise", description = "", time = "04:30"),
        Habit(name = "Sholat Dzuhur", category = "Sholat Fardhu", icon = "sun", description = "", time = "11:55"),
        Habit(name = "Sholat Ashar", category = "Sholat Fardhu", icon = "cloud", description = "", time = "15:10"),
        Habit(name = "Sholat Maghrib", category = "Sholat Fardhu", icon = "moon", description = "", time = "18:00"),
        Habit(name = "Sholat Isya", category = "Sholat Fardhu", icon = "moon", description = "", time = "19:15"),
        Habit(name = "Sholat Dhuha", category = "Sholat Sunnah", icon = "sun", description = "06:00 - 11:00", time = "06:00"),
        Habit(name = "Qabliyah Dzuhur", category = "Sholat Sunnah", icon = "sun", description = "", time = "11:30"),
        Habit(name = "Ba'diyah Dzuhur", category = "Sholat Sunnah", icon = "sun", description = "", time = "12:15"),
        Habit(name = "Ba'diyah Maghrib", category = "Sholat Sunnah", icon = "moon", description = "", time = "18:20"),
        Habit(name = "Ba'diyah Isya", category = "Sholat Sunnah", icon = "moon", description = "", time = "19:35"),
        Habit(name = "Tahajud", category = "Sholat Sunnah", icon = "night", description = "", time = "03:00"),
        Habit(name = "Witir", category = "Sholat Sunnah", icon = "night", description = "", time = "03:30"),
        Habit(name = "Puasa Ramadan", category = "Puasa Wajib", icon = "plate", description = "Sahur - Maghrib", time = ""),
        Habit(name = "Puasa Senin", category = "Puasa Sunnah", icon = "moon", description = "Setiap Senin", time = ""),
        Habit(name = "Puasa Kamis", category = "Puasa Sunnah", icon = "moon", description = "Setiap Kamis", time = ""),
        Habit(name = "Puasa Ayyamul Bidh", category = "Puasa Sunnah", icon = "moon", description = "13-15 Hijriah", time = ""),
        Habit(name = "Puasa Daud", category = "Puasa Sunnah", icon = "moon", description = "Selang-seling", time = ""),
        Habit(name = "Puasa Syawal", category = "Puasa Sunnah", icon = "moon", description = "6 hari di bulan Syawal", time = "")
    )

    private fun loadHabitsAsReminders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            habitDao.getHabits().collect { habits ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        habits = habits
                    )
                }
            }
        }
    }

    private fun observeSunnahHabits() {
        viewModelScope.launch {
            sunnahHabitSharedViewModel.sunnahHabits.collect { sunnahHabits ->
                _uiState.update { it.copy(sunnahHabits = sunnahHabits) }
            }
        }
    }

    fun toggleGlobalNotification() {
        _uiState.update { it.copy(globalNotificationEnabled = !it.globalNotificationEnabled) }
    }

    fun toggleReminderEnabled(habit: Habit) {
        viewModelScope.launch {
            val updatedHabit = habit.copy(isReminderEnabled = !habit.isReminderEnabled)
            habitDao.updateHabit(updatedHabit)
        }
    }

    fun toggleSunnahReminder(sunnahHabit: SunnahHabit) {
        val willEnable = !sunnahHabit.reminderEnabled
        sunnahHabitSharedViewModel.toggleReminder(sunnahHabit.id)

        if (willEnable && sunnahHabit.reminderTime != null) {
            val parts = sunnahHabit.reminderTime.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: return
                val minute = parts[1].toIntOrNull() ?: return
                Log.d("NotificationVM", "Scheduling alarm for ${sunnahHabit.name} at $hour:$minute")
                NotificationScheduler.scheduleHabitReminder(context, sunnahHabit.id, sunnahHabit.name, hour, minute)
            }
        } else {
            Log.d("NotificationVM", "Cancelling alarm for ${sunnahHabit.name}")
            NotificationScheduler.cancelHabitReminder(context, sunnahHabit.id)
        }
    }

    fun showDeleteConfirmation(habit: Habit) {
        _uiState.update { it.copy(showDeleteConfirmation = true, habitToDelete = habit) }
    }

    fun showSunnahDeleteConfirmation(sunnahHabit: SunnahHabit) {
        _uiState.update { it.copy(showDeleteConfirmation = true, sunnahHabitToDelete = sunnahHabit) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false, habitToDelete = null, sunnahHabitToDelete = null) }
    }

    fun deleteReminder() {
        val habit = _uiState.value.habitToDelete
        val sunnahHabit = _uiState.value.sunnahHabitToDelete

        viewModelScope.launch {
            if (habit != null) {
                habitDao.deleteHabit(habit)
            } else if (sunnahHabit != null) {
                if (sunnahHabit.reminderEnabled) {
                    Log.d("NotificationVM", "Cancelling alarm before delete: ${sunnahHabit.name}")
                    NotificationScheduler.cancelHabitReminder(context, sunnahHabit.id)
                }
                sunnahHabitSharedViewModel.removeHabit(sunnahHabit.id)
            }
            hideDeleteConfirmation()
        }
    }
}
