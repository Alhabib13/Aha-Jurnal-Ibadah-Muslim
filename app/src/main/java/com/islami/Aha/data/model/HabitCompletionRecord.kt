package com.islami.Aha.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_completion_records",
    indices = [Index(value = ["habitKey", "dateKey"], unique = true)]
)
data class HabitCompletionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitKey: String,
    val dateKey: String,
    val category: String,
    val source: String,
    val isSynced: Boolean = false
)

data class DailyCompletionCount(
    val dateKey: String,
    val count: Int
)

data class CategoryCompletionCount(
    val category: String,
    val count: Int
)
