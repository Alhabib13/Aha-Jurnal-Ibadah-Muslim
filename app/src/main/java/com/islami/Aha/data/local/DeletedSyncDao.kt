package com.islami.Aha.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.islami.Aha.data.model.DeletedSyncRecord

@Dao
interface DeletedSyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DeletedSyncRecord)

    @Query("SELECT * FROM deleted_sync_records")
    suspend fun getAllDeletedRecords(): List<DeletedSyncRecord>

    @Query("DELETE FROM deleted_sync_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM deleted_sync_records WHERE recordId = :recordId AND recordType = :recordType")
    suspend fun deleteByRecordIdAndType(recordId: String, recordType: String)

    @Query("DELETE FROM deleted_sync_records")
    suspend fun deleteAll()
}
