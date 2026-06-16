package com.islami.Aha.ui.shared

import com.islami.Aha.data.repository.UserHabitRepository
import com.islami.Aha.data.repository.CloudSyncStatus
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SunnahHabitSharedViewModel @Inject constructor(
    private val repository: UserHabitRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val sunnahHabits: StateFlow<List<SunnahHabit>> =
        repository.getAllHabits().stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun addHabit(
        name: String,
        category: SunnahCategoryType,
        frequencyLabel: String = "",
        rakaat: Int? = null,
        reminderEnabled: Boolean = false,
        reminderTime: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val habit = SunnahHabit(
            id = id,
            name = name,
            category = category,
            frequencyLabel = frequencyLabel,
            rakaat = rakaat,
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTime
        )
        scope.launch { repository.insertHabit(habit) }
        return id
    }

    fun toggleHabitComplete(id: String) {
        scope.launch {
            repository.toggleHabitCompletion(id)
        }
    }

    fun toggleReminder(id: String) {
        scope.launch {
            val habit = repository.getHabitById(id) ?: return@launch
            repository.updateHabit(habit.copy(reminderEnabled = !habit.reminderEnabled))
        }
    }

    fun updateHabitSettings(
        id: String,
        frequencyLabel: String,
        reminderTime: String?,
        rakaat: Int?
    ) {
        scope.launch {
            val habit = repository.getHabitById(id) ?: return@launch
            repository.updateHabit(
                habit.copy(
                    frequencyLabel = frequencyLabel,
                    reminderTime = reminderTime,
                    rakaat = rakaat
                )
            )
        }
    }

    fun getHabitById(id: String): SunnahHabit? {
        return sunnahHabits.value.firstOrNull { it.id == id }
    }

    fun removeHabit(id: String) {
        scope.launch { repository.deleteHabit(id) }
    }

    fun syncFromCloudIfLoggedIn() {
        scope.launch { repository.syncFromCloudIfLoggedIn() }
    }

    suspend fun syncFromCloudIfLoggedInAwait(): CloudSyncStatus {
        return repository.syncFromCloudIfLoggedIn()
    }
}
