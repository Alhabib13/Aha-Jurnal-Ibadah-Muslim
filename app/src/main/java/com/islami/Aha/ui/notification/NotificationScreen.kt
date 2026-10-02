package com.islami.Aha.ui.notification

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

import com.islami.Aha.ui.notification.components.NotificationHeader
import com.islami.Aha.ui.notification.components.NotificationPermissionBanner
import com.islami.Aha.ui.notification.components.CategorySectionHeader
import com.islami.Aha.ui.notification.components.HabitReminderCard
import com.islami.Aha.ui.notification.components.SunnahHabitReminderCard
import com.islami.Aha.ui.notification.components.EmptyNotificationState
import com.islami.Aha.ui.notification.components.EditSunnahHabitDialog
import com.islami.Aha.ui.notification.components.DeleteConfirmationDialog


@Composable
fun NotificationScreen(viewModel: NotificationViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isHaidhMode by com.islami.Aha.util.UserPreferencesManager.isHaidhMode.collectAsStateWithLifecycle()
    val notificationPermission = rememberNotificationPermissionState()
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val permissionMessage = stringResource(R.string.notification_permission_required_message)

    val requireNotificationPermissionBeforeEnable: () -> Unit = {
        notificationPermission.requestPermission()
        toastMessage = permissionMessage
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage ?: return@LaunchedEffect
        toastMessage = message
    }

    val toastTone = if (
        toastMessage == permissionMessage ||
            toastMessage == uiState.snackbarMessage && !uiState.snackbarMessage.isNullOrBlank()
    ) {
        AhaToastTone.AUTO
    } else {
        AhaToastTone.INFO
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NotificationScreenContent(
            uiState = uiState,
            isHaidhMode = isHaidhMode,
            notificationPermissionGranted = notificationPermission.isGranted,
            onRequestNotificationPermission = requireNotificationPermissionBeforeEnable,
            onToggleGlobalNotification = {
                val enablingGlobal = !uiState.globalNotificationEnabled
                if (enablingGlobal && !notificationPermission.isGranted) {
                    requireNotificationPermissionBeforeEnable()
                } else {
                    viewModel.toggleGlobalNotification()
                }
            },
            onToggleReminder = { habit ->
                val enablingReminder = !habit.isReminderEnabled
                if (enablingReminder && !notificationPermission.isGranted) {
                    requireNotificationPermissionBeforeEnable()
                } else {
                    viewModel.toggleReminderEnabled(habit)
                }
            },
            onDeleteClick = viewModel::showDeleteConfirmation,
            onConfirmDelete = viewModel::deleteReminder,
            onDismissDelete = viewModel::hideDeleteConfirmation,
            onToggleSunnahReminder = { habit ->
                val enablingReminder = !habit.reminderEnabled
                if (enablingReminder && !notificationPermission.isGranted) {
                    requireNotificationPermissionBeforeEnable()
                } else {
                    viewModel.toggleSunnahReminder(habit)
                }
            },
            onDeleteSunnahClick = viewModel::showSunnahDeleteConfirmation,
            onEditSunnahClick = viewModel::showEditSunnahDialog,
            onDismissEditSunnah = viewModel::hideEditSunnahDialog,
            onSaveEditSunnah = viewModel::updateSunnahHabit
        )

        AhaToastHost(
            message = toastMessage,
            tone = toastTone,
            onDismissed = {
                if (toastMessage == uiState.snackbarMessage) {
                    viewModel.clearSnackbar()
                }
                toastMessage = null
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        )

        AhaLoadingOverlay(
            visible = uiState.isLoading,
            message = stringResource(R.string.notification_loading_message)
        )
    }
}

@Composable
fun NotificationScreenContent(
    uiState: NotificationUiState,
    isHaidhMode: Boolean = false,
    notificationPermissionGranted: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onToggleGlobalNotification: () -> Unit,
    onToggleReminder: (Habit) -> Unit,
    onDeleteClick: (Habit) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onToggleSunnahReminder: (SunnahHabit) -> Unit,
    onDeleteSunnahClick: (SunnahHabit) -> Unit,
    onEditSunnahClick: (SunnahHabit) -> Unit,
    onDismissEditSunnah: () -> Unit,
    onSaveEditSunnah: (String, String?, String, Int?) -> Unit
) {
    val sholatFardhu = uiState.habits.filter { it.category == "Sholat Fardhu" }
    val sholatSunnah = uiState.habits.filter {
        it.category == "Sholat Sunnah" || it.category == "Sholat Tarawih"
    }
    val sunnahSholat = uiState.sunnahHabits.filter { it.category == SunnahCategoryType.SHOLAT }
    val puasaWajib = uiState.habits.filter { it.category == "Puasa Wajib" && uiState.isRamadanMonth }
    val puasaSunnah = uiState.habits.filter { it.category == "Puasa Sunnah" }
    val sunnahPuasa = uiState.sunnahHabits.filter { it.category == SunnahCategoryType.PUASA }
    val isEmpty = uiState.habits.isEmpty() && uiState.sunnahHabits.isEmpty()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!uiState.isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 0.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                    item {
                        NotificationHeader(
                            isEnabled = uiState.globalNotificationEnabled,
                            onToggle = onToggleGlobalNotification
                        )
                    }
                    if (isHaidhMode) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = "Info",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.notification_haidh_mode_warning),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.notification_reminder_title),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Gray700,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp)
                        )
                    }
                    if (!notificationPermissionGranted) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                NotificationPermissionBanner(
                                    onRequestPermission = onRequestNotificationPermission
                                )
                            }
                        }
                    }

                    if (isEmpty) {
                        item {
                            EmptyNotificationState(
                                modifier = Modifier
                                    .fillParentMaxHeight(0.5f)
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        // ── Sholat Fardhu ──
                        if (sholatFardhu.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    CategorySectionHeader(
                                        title = stringResource(R.string.notification_category_sholat_fardhu),
                                        count = sholatFardhu.size
                                    )
                                }
                            }
                            items(sholatFardhu, key = { "fardhu_${it.id}" }) { habit ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    HabitReminderCard(
                                        habit = habit,
                                        globalEnabled = uiState.globalNotificationEnabled && !isHaidhMode,
                                        onToggle = { onToggleReminder(habit) }
                                    )
                                }
                            }
                        }

                        // ── Sholat Sunnah ──
                        if (sholatSunnah.isNotEmpty() || sunnahSholat.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    CategorySectionHeader(
                                        title = stringResource(R.string.notification_category_sholat_sunnah),
                                        count = sholatSunnah.size + sunnahSholat.size
                                    )
                                }
                            }
                            items(sholatSunnah, key = { "sunnah_room_${it.id}" }) { habit ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    HabitReminderCard(
                                        habit = habit,
                                        globalEnabled = uiState.globalNotificationEnabled && !isHaidhMode,
                                        onToggle = { onToggleReminder(habit) }
                                    )
                                }
                            }
                            items(sunnahSholat, key = { "sunnah_custom_${it.id}" }) { sunnahHabit ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    SunnahHabitReminderCard(
                                        sunnahHabit = sunnahHabit,
                                        globalEnabled = uiState.globalNotificationEnabled && !isHaidhMode,
                                        onToggleReminder = { onToggleSunnahReminder(sunnahHabit) },
                                        onDelete = { onDeleteSunnahClick(sunnahHabit) },
                                        onEdit = { onEditSunnahClick(sunnahHabit) }
                                    )
                                }
                            }
                        }

                        // ── Puasa Wajib ──
                        if (puasaWajib.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    CategorySectionHeader(
                                        title = stringResource(R.string.notification_category_puasa_wajib),
                                        count = puasaWajib.size
                                    )
                                }
                            }
                            items(puasaWajib, key = { "puasa_wajib_${it.id}" }) { habit ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    HabitReminderCard(
                                        habit = habit,
                                        globalEnabled = uiState.globalNotificationEnabled && !isHaidhMode,
                                        onToggle = { onToggleReminder(habit) }
                                    )
                                }
                            }
                        }

                        // ── Puasa Sunnah ──
                        if (puasaSunnah.isNotEmpty() || sunnahPuasa.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    CategorySectionHeader(
                                        title = stringResource(R.string.notification_category_puasa_sunnah),
                                        count = puasaSunnah.size + sunnahPuasa.size
                                    )
                                }
                            }
                            items(puasaSunnah, key = { "puasa_sunnah_${it.id}" }) { habit ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    HabitReminderCard(
                                        habit = habit,
                                        globalEnabled = uiState.globalNotificationEnabled && !isHaidhMode,
                                        onToggle = { onToggleReminder(habit) }
                                    )
                                }
                            }
                            items(sunnahPuasa, key = { "puasa_custom_${it.id}" }) { sunnahHabit ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    SunnahHabitReminderCard(
                                        sunnahHabit = sunnahHabit,
                                        globalEnabled = uiState.globalNotificationEnabled && !isHaidhMode,
                                        onToggleReminder = { onToggleSunnahReminder(sunnahHabit) },
                                        onDelete = { onDeleteSunnahClick(sunnahHabit) },
                                        onEdit = { onEditSunnahClick(sunnahHabit) }
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }

    if (uiState.showDeleteConfirmation && uiState.deleteTargetName.isNotEmpty()) {
        DeleteConfirmationDialog(
            habitName = uiState.deleteTargetName,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete
        )
    }

    if (uiState.showEditSunnahDialog && uiState.sunnahHabitToEdit != null) {
        EditSunnahHabitDialog(
            habit = uiState.sunnahHabitToEdit,
            onDismiss = onDismissEditSunnah,
            onSave = { reminderTime, frequency, rakaat ->
                onSaveEditSunnah(uiState.sunnahHabitToEdit.id, reminderTime, frequency, rakaat)
            }
        )
    }
}


// Extracted NotificationHeader


// ── Section Header ──


// Extracted NotificationPermissionBanner



// Extracted CategorySectionHeader


// ── Room DB Habit Card (seed data) ──


// Extracted HabitReminderCard


// ── Sunnah Habit Card (manually added via AddHabit) ──


// Extracted SunnahHabitReminderCard



// Extracted EditSunnahHabitDialog


// ── Dialogs & Empty State ──


// Extracted DeleteConfirmationDialog



// Extracted EmptyNotificationState


// ── Preview ──

@Preview(showBackground = true, device = "id:pixel_5")
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
            notificationPermissionGranted = true,
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

