package com.islami.Aha.data.repository

import com.islami.Aha.data.local.UserHabitDao
import com.islami.Aha.data.model.UserHabitEntity
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserHabitRepository @Inject constructor(
    private val userHabitDao: UserHabitDao
) {

    fun getAllHabits(): Flow<List<SunnahHabit>> {
        return userHabitDao.getAllHabits().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getHabitCount(): Flow<Int> = userHabitDao.getHabitCount()

    fun getHabitCountByCategory(category: SunnahCategoryType): Flow<Int> {
        return userHabitDao.getHabitCountByCategory(category.name)
    }

    suspend fun insertHabit(habit: SunnahHabit) {
        userHabitDao.insertHabit(habit.toEntity())
    }

    suspend fun updateHabit(habit: SunnahHabit) {
        userHabitDao.updateHabit(habit.toEntity())
    }

    suspend fun deleteHabit(id: String) {
        userHabitDao.deleteHabit(id)
    }

    suspend fun getHabitById(id: String): SunnahHabit? {
        return userHabitDao.getHabitById(id)?.toDomain()
    }

    suspend fun getActiveReminderHabits(): List<SunnahHabit> {
        return userHabitDao.getActiveReminderHabits().map { it.toDomain() }
    }
}

private fun UserHabitEntity.toDomain(): SunnahHabit {
    return SunnahHabit(
        id = id,
        name = name,
        category = try {
            SunnahCategoryType.valueOf(category)
        } catch (_: IllegalArgumentException) {
            SunnahCategoryType.SHOLAT
        },
        frequencyLabel = frequencyLabel,
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime,
        isCompletedToday = false
    )
}

private fun SunnahHabit.toEntity(): UserHabitEntity {
    return UserHabitEntity(
        id = id,
        name = name,
        category = category.name,
        frequencyLabel = frequencyLabel,
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime
    )
}
