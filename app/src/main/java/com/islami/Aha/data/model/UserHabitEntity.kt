package com.islami.Aha.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_habits")
data class UserHabitEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val frequencyLabel: String = "",
    val rakaat: Int? = null,
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null,
    val completedDateKey: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
