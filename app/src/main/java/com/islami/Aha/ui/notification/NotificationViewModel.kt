package com.islami.Aha.ui.notification

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.repository.AdminConfigRepository
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.shared.SunnahHabitSharedViewModel
import com.islami.Aha.util.DateUtils
import com.islami.Aha.util.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val isLoading: Boolean = true,
    val habits: List<Habit> = emptyList(),
    val sunnahHabits: List<SunnahHabit> = emptyList(),
    val isRamadanMonth: Boolean = DateUtils.isRamadanMonth(),
    val globalNotificationEnabled: Boolean = true,
    val showDeleteConfirmation: Boolean = false,
    val habitToDelete: Habit? = null,
    val sunnahHabitToDelete: SunnahHabit? = null,
    val showEditSunnahDialog: Boolean = false,
    val sunnahHabitToEdit: SunnahHabit? = null
) {
    val deleteTargetName: String
        get() = habitToDelete?.name ?: sunnahHabitToDelete?.name ?: ""
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitDao: HabitDao,
    private val sharedPreferences: SharedPreferences,
    private val sunnahHabitSharedViewModel: SunnahHabitSharedViewModel,
    private val adminConfigRepository: AdminConfigRepository
) : ViewModel() {
    companion object {
        private const val KEY_GLOBAL_NOTIFICATION_ENABLED = "global_notification_enabled"
    }

    private val _uiState = MutableStateFlow(
        NotificationUiState(
            globalNotificationEnabled = sharedPreferences.getBoolean(KEY_GLOBAL_NOTIFICATION_ENABLED, true)
        )
    )
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        sunnahHabitSharedViewModel.syncFromCloudIfLoggedIn()
        loadHabitsAsReminders()
        observeSunnahHabits()
    }

    private fun loadHabitsAsReminders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            combine(
                habitDao.getHabits(),
                adminConfigRepository.featureConfig
            ) { habits, config ->
                habits.filter {
                    !(it.category == "Puasa Wajib" && !config.puasaWajibRamadanEnabled)
                }
            }.collect { habits ->
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
        viewModelScope.launch {
            val enable = !_uiState.value.globalNotificationEnabled
            sharedPreferences.edit().putBoolean(KEY_GLOBAL_NOTIFICATION_ENABLED, enable).apply()
            _uiState.update { it.copy(globalNotificationEnabled = enable) }
            applyGlobalNotificationState(enable)
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
                        if (willEnable && _uiState.value.globalNotificationEnabled) {
                            NotificationScheduler.scheduleHabitReminder(context, "default_${habit.id}", habit.name, hour, minute)
                        } else {
                            NotificationScheduler.cancelHabitReminder(context, "default_${habit.id}")
                        }
                    }
                }
            }
        }
    }

    fun toggleSunnahReminder(sunnahHabit: SunnahHabit) {
        val willEnable = !sunnahHabit.reminderEnabled
        sunnahHabitSharedViewModel.toggleReminder(sunnahHabit.id)

        if (willEnable && _uiState.value.globalNotificationEnabled && sunnahHabit.reminderTime != null) {
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

    private fun applyGlobalNotificationState(enable: Boolean) {
        val habits = _uiState.value.habits
        val sunnahHabits = _uiState.value.sunnahHabits

        habits.forEach { habit ->
            val habitId = "default_${habit.id}"
            if (!enable) {
                NotificationScheduler.cancelHabitReminder(context, habitId)
            } else if (habit.isReminderEnabled && habit.time.isNotBlank()) {
                val parts = habit.time.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull()
                    val minute = parts[1].toIntOrNull()
                    if (hour != null && minute != null) {
                        NotificationScheduler.scheduleHabitReminder(context, habitId, habit.name, hour, minute)
                    }
                }
            }
        }

        sunnahHabits.forEach { habit ->
            if (!enable) {
                NotificationScheduler.cancelHabitReminder(context, habit.id)
            } else if (habit.reminderEnabled && !habit.reminderTime.isNullOrBlank()) {
                val parts = habit.reminderTime.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull()
                    val minute = parts[1].toIntOrNull()
                    if (hour != null && minute != null) {
                        NotificationScheduler.scheduleHabitReminder(context, habit.id, habit.name, hour, minute)
                    }
                }
            }
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

    fun showEditSunnahDialog(sunnahHabit: SunnahHabit) {
        _uiState.update { it.copy(showEditSunnahDialog = true, sunnahHabitToEdit = sunnahHabit) }
    }

    fun hideEditSunnahDialog() {
        _uiState.update { it.copy(showEditSunnahDialog = false, sunnahHabitToEdit = null) }
    }

    fun updateSunnahHabit(
        habitId: String,
        reminderTime: String?,
        frequencyLabel: String,
        rakaat: Int?
    ) {
        val habit = _uiState.value.sunnahHabits.firstOrNull { it.id == habitId } ?: return
        sunnahHabitSharedViewModel.updateHabitSettings(
            id = habitId,
            frequencyLabel = frequencyLabel,
            reminderTime = reminderTime,
            rakaat = rakaat
        )

        if (habit.reminderEnabled) {
            if (!reminderTime.isNullOrBlank()) {
                val parts = reminderTime.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull()
                    val minute = parts[1].toIntOrNull()
                    if (hour != null && minute != null) {
                        NotificationScheduler.scheduleHabitReminder(context, habitId, habit.name, hour, minute)
                    }
                }
            } else {
                NotificationScheduler.cancelHabitReminder(context, habitId)
            }
        }
        hideEditSunnahDialog()
    }

    fun deleteReminder() {
        val habit = _uiState.value.habitToDelete
        val sunnahHabit = _uiState.value.sunnahHabitToDelete

        viewModelScope.launch {
            if (habit != null) {
                if (habit.isCustom) {
                    habitDao.deleteHabit(habit)
                }
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
