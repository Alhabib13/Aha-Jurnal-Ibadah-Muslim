package com.islami.Aha.ui.notification.components

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
import com.islami.Aha.ui.notification.NotificationScreenContent
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


private fun notificationPreviewState(
    isLoading: Boolean = false,
    globalEnabled: Boolean = true
): NotificationUiState {
    return NotificationUiState(
        isLoading = isLoading,
        globalNotificationEnabled = globalEnabled,
        habits = if (isLoading) {
            emptyList()
        } else {
            listOf(
                Habit(1, "Sholat Subuh", "Sholat Fardhu", "sunrise", "", false, time = "04:30"),
                Habit(2, "Sholat Dzuhur", "Sholat Fardhu", "sun", "", false, time = "12:00"),
                Habit(3, "Sholat Ashar", "Sholat Fardhu", "cloud", "", false, time = "15:15"),
                Habit(4, "Sholat Maghrib", "Sholat Fardhu", "moon", "", false, time = "18:00"),
                Habit(5, "Sholat Isya", "Sholat Fardhu", "moon", "", false, time = "19:15"),
                Habit(6, "Sholat Dhuha", "Sholat Sunnah", "sun", "", false, time = "06:00"),
                Habit(7, "Puasa Ramadan", "Puasa Wajib", "plate", "", false, time = ""),
                Habit(8, "Puasa Senin", "Puasa Sunnah", "moon", "", false, time = "")
            )
        },
        sunnahHabits = if (isLoading) {
            emptyList()
        } else {
            listOf(
                SunnahHabit(
                    name = "Taubat",
                    category = SunnahCategoryType.SHOLAT,
                    frequencyLabel = "Setiap hari",
                    reminderEnabled = true,
                    reminderTime = "05:00"
                ),
                SunnahHabit(
                    name = "Puasa Nazar",
                    category = SunnahCategoryType.PUASA,
                    frequencyLabel = "Hari: Sen",
                    reminderEnabled = false
                )
            )
        }
    )
}




@Composable
private fun NotificationScreenPreviewContent(
    uiState: NotificationUiState,
    notificationPermissionGranted: Boolean = true
) {
    HabitIslamiTheme {
        NotificationScreenContent(
            uiState = uiState,
            notificationPermissionGranted = notificationPermissionGranted,
            onToggleGlobalNotification = {},
            onToggleReminder = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onToggleSunnahReminder = {},
            onDeleteSunnahClick = {},
            onEditSunnahClick = {},
            onDismissEditSunnah = {},
            onSaveEditSunnah = { _, _, _, _ -> }
        )
    }
}


@Composable
fun NotificationScreenSplitSafePreview() {
    NotificationScreenPreviewContent(uiState = notificationPreviewState())
}


@Composable
fun NotificationScreenLoadingSafePreview() {
    NotificationScreenPreviewContent(uiState = notificationPreviewState(isLoading = true))
}


@Composable
fun NotificationScreenPermissionSafePreview() {
    NotificationScreenPreviewContent(
        uiState = notificationPreviewState(globalEnabled = false),
        notificationPermissionGranted = false
    )
}


@Composable
fun NotificationScreenPreview() {
    HabitIslamiTheme {
        NotificationScreenContent(
            uiState = NotificationUiState(
                isLoading = false,
                habits = listOf(
                    Habit(1, "Sholat Subuh", "Sholat Fardhu", "sunrise", "", false, time = "04:30"),
                    Habit(2, "Sholat Dzuhur", "Sholat Fardhu", "sun", "", false, time = "12:00"),
                    Habit(3, "Sholat Ashar", "Sholat Fardhu", "cloud", "", false, time = "15:15"),
                    Habit(4, "Sholat Maghrib", "Sholat Fardhu", "moon", "", false, time = "18:00"),
                    Habit(5, "Sholat Isya", "Sholat Fardhu", "moon", "", false, time = "19:15"),
                    Habit(6, "Sholat Dhuha", "Sholat Sunnah", "sun", "", false, time = "06:00"),
                    Habit(7, "Puasa Ramadan", "Puasa Wajib", "plate", "", false, time = ""),
                    Habit(8, "Puasa Senin", "Puasa Sunnah", "moon", "", false, time = "")
                ),
                sunnahHabits = listOf(
                    SunnahHabit(
                        name = "Taubat",
                        category = SunnahCategoryType.SHOLAT,
                        frequencyLabel = "Setiap hari",
                        reminderEnabled = true,
                        reminderTime = "05:00"
                    ),
                    SunnahHabit(
                        name = "Puasa Nazar",
                        category = SunnahCategoryType.PUASA,
                        frequencyLabel = "Hari: Sen",
                        reminderEnabled = false
                    )
                )
            ),
            onToggleGlobalNotification = {},
            onToggleReminder = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onToggleSunnahReminder = {},
            onDeleteSunnahClick = {},
            onEditSunnahClick = {},
            onDismissEditSunnah = {},
            onSaveEditSunnah = { _, _, _, _ -> }
        )
    }
}