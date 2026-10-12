package com.islami.Aha.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.islami.Aha.data.model.DzikirHabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DzikirHabitDao {
    @Query("SELECT * FROM dzikir_habits ORDER BY createdAt ASC, id ASC")
    fun observeAll(): Flow<List<DzikirHabitEntity>>

    @Query("SELECT * FROM dzikir_habits ORDER BY createdAt ASC, id ASC")
    suspend fun getAll(): List<DzikirHabitEntity>

    @Query("SELECT * FROM dzikir_habits WHERE id = :id")
    suspend fun getById(id: String): DzikirHabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: DzikirHabitEntity)

    /** Pemulihan dari cloud / migrasi: jangan timpa habit lokal yang sudah ada. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(habits: List<DzikirHabitEntity>)

    @Query("DELETE FROM dzikir_habits WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM dzikir_habits")
    suspend fun deleteAll()

    @Query("DELETE FROM dzikir_habits WHERE ownerUid IS NOT NULL AND ownerUid != :uid")
    suspend fun deleteNotOwnedBy(uid: String)

    @Query("UPDATE dzikir_habits SET ownerUid = :uid WHERE ownerUid IS NULL")
    suspend fun adoptGuestRows(uid: String)
}
