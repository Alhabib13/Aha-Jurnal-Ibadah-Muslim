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
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import com.islami.Aha.ui.theme.*

@Composable
fun NotificationScreen(viewModel: NotificationViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationScreenContent(
        uiState = uiState,
        onToggleGlobalNotification = viewModel::toggleGlobalNotification,
        onToggleReminder = viewModel::toggleReminderEnabled,
        onDeleteClick = viewModel::showDeleteConfirmation,
        onConfirmDelete = viewModel::deleteReminder,
        onDismissDelete = viewModel::hideDeleteConfirmation,
        onToggleSunnahReminder = viewModel::toggleSunnahReminder,
        onDeleteSunnahClick = viewModel::showSunnahDeleteConfirmation
    )
}

@Composable
fun NotificationScreenContent(
    uiState: NotificationUiState,
    onToggleGlobalNotification: () -> Unit,
    onToggleReminder: (Habit) -> Unit,
    onDeleteClick: (Habit) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onToggleSunnahReminder: (SunnahHabit) -> Unit,
    onDeleteSunnahClick: (SunnahHabit) -> Unit
) {
    val sholatFardhu = uiState.habits.filter { it.category == "Sholat Fardhu" }
    val sholatSunnah = uiState.habits.filter { it.category == "Sholat Sunnah" }
    val sunnahSholat = uiState.sunnahHabits.filter { it.category == SunnahCategoryType.SHOLAT }
    val puasaWajib = uiState.habits.filter { it.category == "Puasa Wajib" }
    val puasaSunnah = uiState.habits.filter { it.category == "Puasa Sunnah" }
    val sunnahPuasa = uiState.sunnahHabits.filter { it.category == SunnahCategoryType.PUASA }
    val isEmpty = uiState.habits.isEmpty() && uiState.sunnahHabits.isEmpty()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Emerald)
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                NotificationHeader(
                    isEnabled = uiState.globalNotificationEnabled,
                    onToggle = onToggleGlobalNotification
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Pengingat Ibadah",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Gray700,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    if (isEmpty) {
                        item {
                            EmptyNotificationState(modifier = Modifier.fillParentMaxHeight(0.5f))
                        }
                    } else {
                        // ── Sholat Fardhu ──
                        if (sholatFardhu.isNotEmpty()) {
                            item {
                                CategorySectionHeader(
                                    title = "Sholat Fardhu",
                                    count = sholatFardhu.size
                                )
                            }
                            items(sholatFardhu, key = { "fardhu_${it.id}" }) { habit ->
                                HabitReminderCard(
                                    habit = habit,
                                    globalEnabled = uiState.globalNotificationEnabled,
                                    onToggle = { onToggleReminder(habit) }
                                )
                            }
                        }

                        // ── Sholat Sunnah ──
                        if (sholatSunnah.isNotEmpty() || sunnahSholat.isNotEmpty()) {
                            item {
                                CategorySectionHeader(
                                    title = "Sholat Sunnah",
                                    count = sholatSunnah.size + sunnahSholat.size
                                )
                            }
                            items(sholatSunnah, key = { "sunnah_room_${it.id}" }) { habit ->
                                HabitReminderCard(
                                    habit = habit,
                                    globalEnabled = uiState.globalNotificationEnabled,
                                    onToggle = { onToggleReminder(habit) }
                                )
                            }
                            items(sunnahSholat, key = { "sunnah_custom_${it.id}" }) { sunnahHabit ->
                                SunnahHabitReminderCard(
                                    sunnahHabit = sunnahHabit,
                                    globalEnabled = uiState.globalNotificationEnabled,
                                    onToggleReminder = { onToggleSunnahReminder(sunnahHabit) },
                                    onDelete = { onDeleteSunnahClick(sunnahHabit) }
                                )
                            }
                        }

                        // ── Puasa Wajib ──
                        if (puasaWajib.isNotEmpty()) {
                            item {
                                CategorySectionHeader(
                                    title = "Puasa Wajib",
                                    count = puasaWajib.size
                                )
                            }
                            items(puasaWajib, key = { "puasa_wajib_${it.id}" }) { habit ->
                                HabitReminderCard(
                                    habit = habit,
                                    globalEnabled = uiState.globalNotificationEnabled,
                                    onToggle = { onToggleReminder(habit) }
                                )
                            }
                        }

                        // ── Puasa Sunnah ──
                        if (puasaSunnah.isNotEmpty() || sunnahPuasa.isNotEmpty()) {
                            item {
                                CategorySectionHeader(
                                    title = "Puasa Sunnah",
                                    count = puasaSunnah.size + sunnahPuasa.size
                                )
                            }
                            items(puasaSunnah, key = { "puasa_sunnah_${it.id}" }) { habit ->
                                HabitReminderCard(
                                    habit = habit,
                                    globalEnabled = uiState.globalNotificationEnabled,
                                    onToggle = { onToggleReminder(habit) }
                                )
                            }
                            items(sunnahPuasa, key = { "puasa_custom_${it.id}" }) { sunnahHabit ->
                                SunnahHabitReminderCard(
                                    sunnahHabit = sunnahHabit,
                                    globalEnabled = uiState.globalNotificationEnabled,
                                    onToggleReminder = { onToggleSunnahReminder(sunnahHabit) },
                                    onDelete = { onDeleteSunnahClick(sunnahHabit) }
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
}

@Composable
fun NotificationHeader(
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Notifikasi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                                text = "Notifikasi Aktif",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isEnabled) "Semua pengingat aktif" else "Pengingat dinonaktifkan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                    Switch(
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
        }
    }
}

// ── Section Header ──

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
                text = "$count ibadah",
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
                    contentDescription = "Masjid",
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
                        text = "Setiap hari",
                        fontSize = 12.sp,
                        color = Gray500
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
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
    onDelete: () -> Unit
) {
    val isEffectivelyEnabled = globalEnabled && sunnahHabit.reminderEnabled
    val cardAlpha by animateFloatAsState(targetValue = if (globalEnabled) 1f else 0.6f, label = "")

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
                    contentDescription = null,
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
                        text = sunnahHabit.frequencyLabel.ifEmpty { "Setiap hari" },
                        fontSize = 12.sp,
                        color = Gray500
                    )
                }
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Hapus",
                    tint = ErrorRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Bell switch
            Switch(
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

// ── Dialogs & Empty State ──

@Composable
fun DeleteConfirmationDialog(
    habitName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus Ibadah", fontWeight = FontWeight.SemiBold) },
        text = { Text("Apakah Anda yakin ingin menghapus \"$habitName\"? Ini akan menghapus data terkait.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Hapus", color = ErrorRed, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
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
        Text("Tidak Ada Ibadah", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Belum ada pengingat aktif. Tambahkan kebiasaan dari Beranda agar pengingat muncul di sini.",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray500,
            textAlign = TextAlign.Center
        )
    }
}

// ── Preview ──

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
            onDeleteSunnahClick = {}
        )
    }
}
