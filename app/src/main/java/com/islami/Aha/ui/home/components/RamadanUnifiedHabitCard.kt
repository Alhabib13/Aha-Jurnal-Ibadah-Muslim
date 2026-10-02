package com.islami.Aha.ui.home.components

import androidx.compose.material3.MaterialTheme

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.islami.Aha.R
import com.islami.Aha.data.model.Habit
import com.islami.Aha.ui.home.CATEGORY_PUASA_WAJIB
import com.islami.Aha.ui.home.CATEGORY_SHOLAT_TARAWIH
import com.islami.Aha.ui.theme.Emerald
import com.islami.Aha.ui.theme.InfoBlue

@Composable
fun RamadanUnifiedHabitCard(
    puasaHabit: Habit?,
    tarawihHabit: Habit?,
    onToggleHabitCompletion: (Habit) -> Unit,
    onToggleHabitReminder: (Habit) -> Unit
) {
    val habits = buildList {
        puasaHabit?.let { add(it) }
        tarawihHabit?.let { add(it) }
    }
    if (habits.isEmpty()) return
    val hasPuasa = habits.any { it.category == CATEGORY_PUASA_WAJIB }
    val hasTarawih = habits.any { it.category == CATEGORY_SHOLAT_TARAWIH }
    val titleText = when {
        hasPuasa && !hasTarawih -> stringResource(R.string.home_ramadan_unified_title_puasa)
        hasTarawih && !hasPuasa -> stringResource(R.string.home_ramadan_unified_title_tarawih)
        else -> stringResource(R.string.home_ramadan_unified_title)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titleText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            habits.forEachIndexed { index, habit ->
                RamadanUnifiedHabitRow(
                    habit = habit,
                    displayName = when (habit.category) {
                        CATEGORY_PUASA_WAJIB -> stringResource(R.string.home_ramadan_unified_puasa_label)
                        CATEGORY_SHOLAT_TARAWIH -> stringResource(R.string.home_ramadan_unified_tarawih_label)
                        else -> habit.name
                    },
                    onToggleHabitCompletion = { onToggleHabitCompletion(habit) },
                    onToggleHabitReminder = { onToggleHabitReminder(habit) }
                )
                if (index < habits.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun RamadanUnifiedHabitRow(
    habit: Habit,
    displayName: String,
    onToggleHabitCompletion: () -> Unit,
    onToggleHabitReminder: () -> Unit
) {
    val completionStateDescription = if (habit.isCompleted) {
        stringResource(R.string.home_completed_cd)
    } else {
        stringResource(R.string.home_not_completed_cd)
    }
    val reminderStateDescription = if (habit.isReminderEnabled) {
        stringResource(R.string.notification_switch_habit_on_format, displayName)
    } else {
        stringResource(R.string.notification_switch_habit_off_format, displayName)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = habitIconContainerColor(habit),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = getHabitItemIcon(habit),
                    contentDescription = displayName,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val secondaryText = habit.time.ifBlank { habit.description }
            if (secondaryText.isNotBlank()) {
                Text(
                    text = secondaryText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = onToggleHabitReminder,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = if (habit.isReminderEnabled) {
                    Icons.Outlined.Notifications
                } else {
                    Icons.Outlined.NotificationsOff
                },
                contentDescription = reminderStateDescription,
                tint = if (habit.isReminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .then(
                    if (habit.isCompleted) {
                        Modifier.background(InfoBlue, CircleShape)
                    } else {
                        Modifier.border(2.dp, InfoBlue, CircleShape)
                    }
                )
                .semantics {
                    role = Role.Checkbox
                    stateDescription = completionStateDescription
                }
                .toggleable(
                    value = habit.isCompleted,
                    role = Role.Checkbox,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggleHabitCompletion() },
            contentAlignment = Alignment.Center
        ) {
            if (habit.isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.home_completed_cd),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
