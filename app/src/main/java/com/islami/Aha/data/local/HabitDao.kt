package com.islami.Aha.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.islami.Aha.data.model.Habit
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun getHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY id ASC")
    suspend fun getHabitsSnapshot(): List<Habit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(habits: List<Habit>)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Int): Habit?

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun getHabitCount(): Int

    @Query("UPDATE habits SET isCompleted = 0")
    suspend fun resetAllCompletions()

    @Query("UPDATE habits SET isCompleted = 0, streak = 0")
    suspend fun resetTrackerState()

    @Query("SELECT * FROM habits WHERE isReminderEnabled = 1 AND time != ''")
    suspend fun getActiveReminderHabits(): List<Habit>

    @Query("SELECT * FROM habits WHERE category = 'Sholat Fardhu' ORDER BY id ASC")
    suspend fun getFardhuHabits(): List<Habit>

    @Query("UPDATE habits SET time = :time WHERE name = :name AND category = 'Sholat Fardhu'")
    suspend fun updateFardhuTimeByName(name: String, time: String)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()
}
