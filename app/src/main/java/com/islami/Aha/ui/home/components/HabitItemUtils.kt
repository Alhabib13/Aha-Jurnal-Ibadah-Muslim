package com.islami.Aha.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.islami.Aha.data.model.Habit
import com.islami.Aha.ui.home.CATEGORY_PREFIX_PUASA
import com.islami.Aha.ui.home.parseHourMinute
import com.islami.Aha.ui.theme.InfoBlue
import com.islami.Aha.ui.theme.WarningAmber

internal fun habitIconContainerColor(habit: Habit): Color {
    return if (habit.category.startsWith(CATEGORY_PREFIX_PUASA, ignoreCase = true)) {
        WarningAmber.copy(alpha = 0.12f)
    } else {
        InfoBlue.copy(alpha = 0.12f)
    }
}

internal fun getHabitItemIcon(habit: Habit): ImageVector {
    return when {
        habit.icon == "sunrise" -> Icons.Filled.WbSunny
        habit.icon == "sun" -> Icons.Filled.WbSunny
        habit.icon == "cloud" -> Icons.Filled.Cloud
        habit.icon == "moon" -> Icons.Filled.DarkMode
        habit.icon == "night" -> Icons.Filled.DarkMode
        habit.icon == "plate" -> Icons.Filled.Restaurant
        habit.icon == "\uD83C\uDF05" -> Icons.Filled.WbSunny // sunrise
        habit.icon == "\u2600\uFE0F" -> Icons.Filled.WbSunny // sun
        habit.icon == "\u2601\uFE0F" -> Icons.Filled.Cloud // cloud
        habit.icon == "\uD83C\uDF19" -> Icons.Filled.DarkMode // crescent moon
        habit.icon == "\uD83C\uDF1C" -> Icons.Filled.DarkMode // moon face
        habit.icon == "\uD83C\uDF7D\uFE0F" -> Icons.Filled.Restaurant // plate
        habit.category.startsWith(CATEGORY_PREFIX_PUASA, ignoreCase = true) -> Icons.Filled.Restaurant
        else -> Icons.Outlined.AccessTime
    }
}

internal fun isMasjidIcon(icon: String): Boolean {
    return icon == "masjid" || icon == "\uD83D\uDD4C"
}

internal fun isCurrentPrayerTime(habitTime: String, currentTime: String): Boolean {
    if (habitTime.length < 4 || currentTime.length < 4) return false
    
    // Fast path parsing without split or Regex to avoid allocation in composition
    var h1 = 0; var m1 = 0
    var h2 = 0; var m2 = 0
    
    try {
        val idx1 = habitTime.indexOf(':')
        if (idx1 != -1) {
            h1 = habitTime.substring(0, idx1).trim().toInt()
            m1 = habitTime.substring(idx1 + 1, minOf(idx1 + 3, habitTime.length)).trim().toInt()
        } else return false
        
        val idx2 = currentTime.indexOf(':')
        if (idx2 != -1) {
            h2 = currentTime.substring(0, idx2).trim().toInt()
            m2 = currentTime.substring(idx2 + 1, minOf(idx2 + 3, currentTime.length)).trim().toInt()
        } else return false
    } catch (e: Exception) {
        return false
    }
    
    val habitMinutes = h1 * 60 + m1
    val currentMinutes = h2 * 60 + m2
    return currentMinutes in (habitMinutes - 15)..(habitMinutes + 30)
}
