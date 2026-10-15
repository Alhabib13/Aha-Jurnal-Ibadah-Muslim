package com.islami.Aha.ui.home.components

import com.islami.Aha.ui.theme.teks

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import com.islami.Aha.R
import com.islami.Aha.data.model.Habit
import com.islami.Aha.ui.theme.Emerald
import com.islami.Aha.ui.theme.Gold
import com.islami.Aha.ui.theme.InfoBlue

@Composable
fun HabitSectionHeader(
    completedCount: Int,
    totalCount: Int,
    progressTextOverride: String? = null,
    judul: String? = null,
    keterangan: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = judul ?: stringResource(R.string.home_habit_today_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (keterangan != null) {
                Text(keterangan, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFF2C66D).copy(alpha = 0.16f)
        ) {
            Text(
                text = progressTextOverride
                    ?: stringResource(R.string.home_habit_progress_format, completedCount, totalCount),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF2C66D).teks(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
fun HomeHabitItem(
    habit: Habit,
    onCheckedChange: (Boolean) -> Unit,
    onToggleReminder: () -> Unit,
    currentTime: String,
    jamDicatat: String? = null,
    modifier: Modifier = Modifier
) {
    val completionStateDescription = if (habit.isCompleted) {
        stringResource(R.string.home_completed_cd)
    } else {
        stringResource(R.string.home_not_completed_cd)
    }
    val reminderStateDescription = if (habit.isReminderEnabled) {
        stringResource(R.string.notification_switch_habit_on_format, habit.name)
    } else {
        stringResource(R.string.notification_switch_habit_off_format, habit.name)
    }

    // Waktu sholat yang sedang berjalan diberi bingkai emas.
    val isCurrentPrayer = isCurrentPrayerTime(habit.time, currentTime) && !habit.isCompleted
    val hijau = Color(0xFF14A87D)
    val emas = Color(0xFFF2C66D)
    val shape = RoundedCornerShape(16.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (isCurrentPrayer) Modifier.border(1.dp, emas.copy(alpha = 0.7f), shape) else Modifier)
            .toggleable(value = habit.isCompleted, role = Role.Checkbox) { onCheckedChange(!habit.isCompleted) }
            .semantics { stateDescription = completionStateDescription }
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(
                    if (habit.isCompleted) Modifier.background(hijau)
                    else Modifier.border(2.dp, if (isCurrentPrayer) emas else MaterialTheme.colorScheme.outline, CircleShape)
                )
        ) {
            if (habit.isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name.removePrefix("Sholat ").ifBlank { habit.name },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (habit.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val keterangan = habit.time.ifEmpty { habit.description }
                if (keterangan.isNotEmpty()) {
                    Text(keterangan.replace(":", "."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isCurrentPrayer) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Sekarang",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = emas.teks(),
                        modifier = Modifier.clip(CircleShape).background(emas.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 1.dp)
                    )
                } else if (habit.isCompleted) {
                    Spacer(Modifier.width(8.dp))
                    Text(if (jamDicatat != null) "Dicatat $jamDicatat" else "Dicatat", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = hijau.teks())
                }
            }
        }
        IconButton(onClick = onToggleReminder) {
            Icon(
                imageVector = if (habit.isReminderEnabled) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                contentDescription = reminderStateDescription,
                tint = if (habit.isReminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
