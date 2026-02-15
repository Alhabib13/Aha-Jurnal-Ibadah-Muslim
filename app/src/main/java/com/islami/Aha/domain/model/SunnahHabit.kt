package com.islami.Aha.domain.model

import com.islami.Aha.ui.addhabit.SunnahCategoryType
import java.util.UUID

data class SunnahHabit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: SunnahCategoryType,
    val frequencyLabel: String = "",
    val rakaat: Int? = null,
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null,
    val isCompletedToday: Boolean = false
)
