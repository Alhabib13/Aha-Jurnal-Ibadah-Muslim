package com.islami.Aha.ui.notification.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.islami.Aha.R
import com.islami.Aha.data.model.Habit
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import com.islami.Aha.ui.components.AhaLoadingOverlay
import com.islami.Aha.ui.components.AhaToastTone
import com.islami.Aha.ui.components.AhaToastHost
import com.islami.Aha.ui.theme.*
import com.islami.Aha.util.rememberNotificationPermissionState

import com.islami.Aha.ui.notification.NotificationUiState
@Composable
fun EditSunnahHabitDialog(
    habit: SunnahHabit,
    onDismiss: () -> Unit,
    onSave: (String?, String, Int?) -> Unit
) {
    val timeRegex = remember { Regex("^([01]\\d|2[0-3]):([0-5]\\d)$") }
    val dailyFrequencyText = stringResource(R.string.notification_frequency_daily)
    var frequency by remember(habit.id, dailyFrequencyText) {
        mutableStateOf(habit.frequencyLabel.ifBlank { dailyFrequencyText })
    }
    var reminderTime by remember(habit.id) { mutableStateOf(habit.reminderTime.orEmpty()) }
    var rakaatText by remember(habit.id) { mutableStateOf(habit.rakaat?.toString().orEmpty()) }
    val normalizedTime = reminderTime.trim()
    val hasTimeInput = normalizedTime.isNotEmpty()
    val hasInvalidTime = hasTimeInput && !timeRegex.matches(normalizedTime)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.notification_edit_sunnah_title),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text(stringResource(R.string.notification_frequency_label)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = reminderTime,
                    onValueChange = { reminderTime = it },
                    label = { Text(stringResource(R.string.notification_time_label)) },
                    placeholder = { Text(stringResource(R.string.notification_time_placeholder)) },
                    singleLine = true,
                    isError = hasInvalidTime,
                    supportingText = {
                        if (hasInvalidTime) {
                            Text(stringResource(R.string.notification_time_error_format))
                        }
                    }
                )
                if (habit.category == SunnahCategoryType.SHOLAT) {
                    OutlinedTextField(
                        value = rakaatText,
                        onValueChange = { rakaatText = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.notification_rakaat_label)) },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !hasInvalidTime,
                onClick = {
                    val rakaat = rakaatText.toIntOrNull()
                    onSave(
                        normalizedTime.ifBlank { null },
                        frequency.trim().ifBlank { dailyFrequencyText },
                        rakaat
                    )
                }
            ) {
                Text(
                    text = stringResource(R.string.common_save),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    habitName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.notification_delete_dialog_title),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(stringResource(R.string.notification_delete_dialog_desc_format, habitName))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.settings_delete_action),
                    color = ErrorRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}