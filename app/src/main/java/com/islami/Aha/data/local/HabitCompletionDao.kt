package com.islami.Aha.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.islami.Aha.data.model.CategoryCompletionCount
import com.islami.Aha.data.model.DailyCompletionCount
import com.islami.Aha.data.model.HabitCompletionRecord

@Dao
interface HabitCompletionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: HabitCompletionRecord)

    @Query("DELETE FROM habit_completion_records WHERE habitKey = :habitKey AND dateKey = :dateKey")
    suspend fun deleteByHabitAndDate(habitKey: String, dateKey: String)

    @Query("SELECT dateKey, COUNT(*) as count FROM habit_completion_records WHERE dateKey BETWEEN :startDateKey AND :endDateKey GROUP BY dateKey")
    suspend fun getDailyCountsBetween(startDateKey: String, endDateKey: String): List<DailyCompletionCount>

    @Query("SELECT category, COUNT(*) as count FROM habit_completion_records WHERE dateKey BETWEEN :startDateKey AND :endDateKey GROUP BY category ORDER BY count DESC")
    suspend fun getCategoryCountsBetween(startDateKey: String, endDateKey: String): List<CategoryCompletionCount>

    @Query("SELECT DISTINCT dateKey FROM habit_completion_records ORDER BY dateKey DESC")
    suspend fun getDistinctCompletionDatesDesc(): List<String>

    @Query("SELECT COUNT(*) FROM habit_completion_records")
    suspend fun getTotalCompletionCount(): Int

    @Query("SELECT * FROM habit_completion_records ORDER BY dateKey ASC, id ASC")
    suspend fun getAllRecords(): List<HabitCompletionRecord>

    @Query("DELETE FROM habit_completion_records")
    suspend fun deleteAll()
}
