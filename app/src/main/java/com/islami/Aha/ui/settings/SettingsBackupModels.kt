package com.islami.Aha.ui.settings

import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.model.HabitCompletionRecord
import com.islami.Aha.data.model.UserHabitEntity

internal data class ParsedImport(
    val defaultHabits: List<Habit>,
    val sunnahHabits: List<UserHabitEntity>,
    val completionRecords: List<HabitCompletionRecord>
)
