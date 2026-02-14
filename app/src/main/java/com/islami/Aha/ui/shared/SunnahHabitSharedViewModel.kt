package com.islami.Aha.ui.shared

import com.islami.Aha.data.repository.UserHabitRepository
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SunnahHabitSharedViewModel @Inject constructor(
    private val repository: UserHabitRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory only: tracks today's completions (resets on app restart)
    private val _completedIds = MutableStateFlow<Set<String>>(emptySet())

    val sunnahHabits: StateFlow<List<SunnahHabit>> =
        combine(repository.getAllHabits(), _completedIds) { habits, completedIds ->
            habits.map { habit ->
                if (habit.id in completedIds) habit.copy(isCompletedToday = true)
                else habit
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun addHabit(
        name: String,
        category: SunnahCategoryType,
        frequencyLabel: String = "",
        reminderEnabled: Boolean = false,
        reminderTime: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val habit = SunnahHabit(
            id = id,
            name = name,
            category = category,
            frequencyLabel = frequencyLabel,
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTime
        )
        scope.launch { repository.insertHabit(habit) }
        return id
    }

    fun toggleHabitComplete(id: String) {
        _completedIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun toggleReminder(id: String) {
        scope.launch {
            val habit = repository.getHabitById(id) ?: return@launch
            repository.updateHabit(habit.copy(reminderEnabled = !habit.reminderEnabled))
        }
    }

    fun getHabitById(id: String): SunnahHabit? {
        return sunnahHabits.value.firstOrNull { it.id == id }
    }

    fun removeHabit(id: String) {
        _completedIds.update { it - id }
        scope.launch { repository.deleteHabit(id) }
    }
}
