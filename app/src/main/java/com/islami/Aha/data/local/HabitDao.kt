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

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()
}
