package com.islami.Aha.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val category: String,
    val icon: String,
    val description: String,
    val isCompleted: Boolean = false,
    val streak: Int = 0,
    val time: String = "",
    val isReminderEnabled: Boolean = true,
    val isCustom: Boolean = false
)
