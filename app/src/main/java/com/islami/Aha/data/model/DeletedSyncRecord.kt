package com.islami.Aha.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_sync_records")
data class DeletedSyncRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: String,
    val recordType: String, // "SUNNAH_HABIT" or "HABIT_COMPLETION"
    val deletedAt: Long = System.currentTimeMillis()
)
