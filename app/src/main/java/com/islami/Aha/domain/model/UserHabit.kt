package com.islami.Aha.domain.model

import com.islami.Aha.ui.addhabit.SunnahCategoryType

data class UserHabit(
    val id: String,
    val name: String,
    val category: SunnahCategoryType,
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    val createdAt: Long
) {
    val formattedReminderTime: String
        get() = String.format("%02d:%02d", reminderHour, reminderMinute)
}
