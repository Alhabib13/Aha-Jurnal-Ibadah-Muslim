package com.islami.Aha.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.islami.Aha.data.model.UserHabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserHabitDao {

    @Query("SELECT * FROM user_habits ORDER BY createdAt ASC")
    fun getAllHabits(): Flow<List<UserHabitEntity>>

    @Query("SELECT * FROM user_habits ORDER BY createdAt ASC")
    suspend fun getAllHabitsSnapshot(): List<UserHabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: UserHabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHabits(habits: List<UserHabitEntity>)

    @Update
    suspend fun updateHabit(habit: UserHabitEntity)

    @Query("DELETE FROM user_habits WHERE id = :id")
    suspend fun deleteHabit(id: String)

    @Query("DELETE FROM user_habits WHERE id IN (:ids)")
    suspend fun deleteHabitsByIds(ids: List<String>)

    @Query(
        """
        UPDATE user_habits
        SET lastSyncedAt = CASE
            WHEN lastSyncedAt > :syncedAt THEN lastSyncedAt
            ELSE :syncedAt
        END
        WHERE id = :id
        """
    )
    suspend fun markHabitSynced(id: String, syncedAt: Long)

    @Query("SELECT * FROM user_habits WHERE id = :id")
    suspend fun getHabitById(id: String): UserHabitEntity?

    @Query("SELECT COUNT(*) FROM user_habits")
    fun getHabitCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_habits WHERE category = :category")
    fun getHabitCountByCategory(category: String): Flow<Int>

    @Query("SELECT * FROM user_habits WHERE reminderEnabled = 1 AND reminderTime IS NOT NULL")
    suspend fun getActiveReminderHabits(): List<UserHabitEntity>

    @Query("DELETE FROM user_habits")
    suspend fun deleteAll()
}
