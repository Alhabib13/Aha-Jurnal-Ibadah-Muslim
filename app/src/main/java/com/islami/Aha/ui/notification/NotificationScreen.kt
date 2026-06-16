package com.islami.Aha.ui.notification

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

@Composable
fun NotificationScreen(viewModel: NotificationViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                                        globalEnabled = uiState.globalNotificationEnabled,
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
                                        globalEnabled = uiState.globalNotificationEnabled,
                                        onToggle = { onToggleReminder(habit) }
                                    )
                                }
                            }
                            items(sunnahSholat, key = { "sunnah_custom_${it.id}" }) { sunnahHabit ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    SunnahHabitReminderCard(
                                        sunnahHabit = sunnahHabit,
                                        globalEnabled = uiState.globalNotificationEnabled,
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
                                        globalEnabled = uiState.globalNotificationEnabled,
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
                                        globalEnabled = uiState.globalNotificationEnabled,
                                        onToggle = { onToggleReminder(habit) }
                                    )
                                }
                            }
                            items(sunnahPuasa, key = { "puasa_custom_${it.id}" }) { sunnahHabit ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    SunnahHabitReminderCard(
                                        sunnahHabit = sunnahHabit,
                                        globalEnabled = uiState.globalNotificationEnabled,
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

@Composable
fun NotificationHeader(
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    val globalSwitchDescription = if (isEnabled) {
        stringResource(R.string.notification_switch_global_on)
    } else {
        stringResource(R.string.notification_switch_global_off)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(colors = listOf(EmeraldDark, Emerald)),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.notification_screen_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Text(
                text = stringResource(R.string.notification_header_subtitle),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.notification_enabled_title),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isEnabled) {
                                    stringResource(R.string.notification_enabled_desc)
                                } else {
                                    stringResource(R.string.notification_disabled_desc)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                    Switch(
                        modifier = Modifier.semantics {
                            contentDescription = globalSwitchDescription
                        },
                        checked = isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.notification_header_summary),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

// ── Section Header ──

@Composable
fun NotificationPermissionBanner(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = GoldLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsOff,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.notification_permission_banner_text),
                color = Gray700,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRequestPermission) {
                Text(
                    text = stringResource(R.string.notification_permission_action),
                    color = Emerald,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun CategorySectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Emerald
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = EmeraldLight
        ) {
            Text(
                text = pluralStringResource(R.plurals.notification_count_ibadah_format, count, count),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Emerald,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// ── Room DB Habit Card (seed data) ──

@Composable
fun HabitReminderCard(
    habit: Habit,
    globalEnabled: Boolean,
    onToggle: () -> Unit
) {
    val isEffectivelyEnabled = globalEnabled && habit.isReminderEnabled
    val cardAlpha by animateFloatAsState(targetValue = if (globalEnabled) 1f else 0.6f, label = "")
    val reminderSwitchDescription = if (habit.isReminderEnabled) {
        stringResource(R.string.notification_switch_habit_on_format, habit.name)
    } else {
        stringResource(R.string.notification_switch_habit_off_format, habit.name)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mosque icon in circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EmeraldLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.masjid),
                    contentDescription = stringResource(R.string.notification_mosque_cd),
                    tint = Emerald,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEffectivelyEnabled) MaterialTheme.colorScheme.onSurface else Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Time badge
                    if (habit.time.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = EmeraldLight
                        ) {
                            Text(
                                text = habit.time,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Emerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    // Frequency
                    Text(
                        text = stringResource(R.string.notification_frequency_daily),
                        fontSize = 12.sp,
                        color = Gray500
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                modifier = Modifier.semantics {
                    contentDescription = reminderSwitchDescription
                },
                checked = habit.isReminderEnabled,
                onCheckedChange = { onToggle() },
                enabled = globalEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = Emerald,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedTrackColor = Gray300
                )
            )
        }
    }
}

// ── Sunnah Habit Card (manually added via AddHabit) ──

@Composable
fun SunnahHabitReminderCard(
    sunnahHabit: SunnahHabit,
    globalEnabled: Boolean,
    onToggleReminder: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val isEffectivelyEnabled = globalEnabled && sunnahHabit.reminderEnabled
    val cardAlpha by animateFloatAsState(targetValue = if (globalEnabled) 1f else 0.6f, label = "")
    val reminderSwitchDescription = if (sunnahHabit.reminderEnabled) {
        stringResource(R.string.notification_switch_habit_on_format, sunnahHabit.name)
    } else {
        stringResource(R.string.notification_switch_habit_off_format, sunnahHabit.name)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EmeraldLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.masjid),
                    contentDescription = stringResource(R.string.notification_mosque_cd),
                    tint = Emerald,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sunnahHabit.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEffectivelyEnabled) MaterialTheme.colorScheme.onSurface else Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (sunnahHabit.reminderTime != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = EmeraldLight
                        ) {
                            Text(
                                text = sunnahHabit.reminderTime,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Emerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = sunnahHabit.frequencyLabel.ifEmpty {
                            stringResource(R.string.notification_frequency_daily)
                        },
                        fontSize = 12.sp,
                        color = Gray500
                    )
                }
                sunnahHabit.rakaat?.let { rakaat ->
                    Text(
                        text = pluralStringResource(R.plurals.notification_rakaat_format, rakaat, rakaat),
                        fontSize = 12.sp,
                        color = Gray500
                    )
                }
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.notification_edit_cd),
                    tint = Gray500,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.notification_delete_cd),
                    tint = ErrorRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Bell switch
            Switch(
                modifier = Modifier.semantics {
                    contentDescription = reminderSwitchDescription
                },
                checked = sunnahHabit.reminderEnabled,
                onCheckedChange = { onToggleReminder() },
                enabled = globalEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = Emerald,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedTrackColor = Gray300
                )
            )
        }
    }
}

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
                    color = Emerald,
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

// ── Dialogs & Empty State ──

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

@Composable
fun EmptyNotificationState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.NotificationsOff, null, modifier = Modifier.size(48.dp), tint = Gray400)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.notification_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.notification_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = Gray500,
            textAlign = TextAlign.Center
        )
    }
}

// ── Preview ──

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

@Preview(name = "Notification Split Safe", showBackground = true)
@Composable
fun NotificationScreenSplitSafePreview() {
    NotificationScreenPreviewContent(uiState = notificationPreviewState())
}

@Preview(name = "Notification Loading Safe", showBackground = true)
@Composable
fun NotificationScreenLoadingSafePreview() {
    NotificationScreenPreviewContent(uiState = notificationPreviewState(isLoading = true))
}

@Preview(name = "Notification Permission Safe", showBackground = true)
@Composable
fun NotificationScreenPermissionSafePreview() {
    NotificationScreenPreviewContent(
        uiState = notificationPreviewState(globalEnabled = false),
        notificationPermissionGranted = false
    )
}

@Preview(showBackground = true)
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
