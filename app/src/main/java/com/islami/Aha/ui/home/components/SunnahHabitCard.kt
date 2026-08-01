package com.islami.Aha.ui.home.components

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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
import com.islami.Aha.R
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.theme.Emerald
import com.islami.Aha.ui.theme.InfoBlue
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NightlightRound
import com.islami.Aha.ui.theme.WarningAmber

@Composable
fun SunnahHabitCard(
    sunnahHabit: SunnahHabit,
    onToggleComplete: () -> Unit = {},
    onToggleReminder: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val completionStateDescription = if (sunnahHabit.isCompletedToday) {
        stringResource(R.string.home_completed_cd)
    } else {
        stringResource(R.string.home_not_completed_cd)
    }
    val backgroundColor = if (sunnahHabit.isCompletedToday) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val checkScale = if (sunnahHabit.isCompletedToday) 1f else 0.8f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (sunnahHabit.isCompletedToday) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconVector = when (sunnahHabit.category) {
                SunnahCategoryType.SHOLAT -> androidx.compose.material.icons.Icons.Rounded.Mosque
                SunnahCategoryType.PUASA -> androidx.compose.material.icons.Icons.Rounded.NightlightRound
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (sunnahHabit.category) {
                    SunnahCategoryType.PUASA -> WarningAmber.copy(alpha = 0.12f)
                    SunnahCategoryType.SHOLAT -> InfoBlue.copy(alpha = 0.12f)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = sunnahHabit.name,
                        modifier = Modifier.size(24.dp),
                        tint = when (sunnahHabit.category) {
                            SunnahCategoryType.PUASA -> WarningAmber
                            SunnahCategoryType.SHOLAT -> Emerald
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sunnahHabit.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (sunnahHabit.isCompletedToday) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = sunnahHabit.frequencyLabel,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                sunnahHabit.rakaat?.let { rakaat ->
                    Text(
                        text = pluralStringResource(R.plurals.home_rakaat_format, rakaat, rakaat),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (sunnahHabit.reminderEnabled && sunnahHabit.reminderTime != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = stringResource(R.string.home_reminder_time_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = sunnahHabit.reminderTime,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Circular checkbox
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .scale(checkScale)
                    .clip(CircleShape)
                    .then(
                        if (sunnahHabit.isCompletedToday) {
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
                        value = sunnahHabit.isCompletedToday,
                        role = Role.Checkbox,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleComplete() },
                contentAlignment = Alignment.Center
            ) {
                if (sunnahHabit.isCompletedToday) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.home_completed_cd),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
