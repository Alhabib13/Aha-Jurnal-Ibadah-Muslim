package com.islami.Aha.ui.addhabit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.islami.Aha.R
import com.islami.Aha.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview
import com.islami.Aha.ui.theme.HabitIslamiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    viewModel: AddHabitViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onHabitSaved: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onHabitSaved()
            viewModel.resetState()
        }
    }

    AddHabitScreenContent(
        uiState = uiState,
        onSelectCategory = viewModel::selectCategory,
        onSelectHabit = viewModel::selectHabit,
        onSelectExtraHabit = viewModel::selectExtraHabit,
        onEnableCustomInput = viewModel::enableCustomInput,
        onCustomHabitNameChange = viewModel::updateCustomHabitName,
        onSelectRakaat = viewModel::selectRakaat,
        onSelectFrequency = viewModel::selectFrequency,
        onToggleDay = viewModel::toggleDay,
        onToggleReminder = viewModel::toggleReminder,
        onShowTimePicker = viewModel::showTimePicker,
        onHideTimePicker = viewModel::hideTimePicker,
        onReminderTimeChange = viewModel::onReminderTimeChange,
        onSaveClick = viewModel::saveHabit,
        getFormattedReminderTime = { viewModel.getFormattedReminderTime(uiState) },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreenContent(
    uiState: AddHabitUiState,
    onSelectCategory: (SunnahCategoryType) -> Unit,
    onSelectHabit: (String) -> Unit,
    onSelectExtraHabit: (String) -> Unit,
    onEnableCustomInput: () -> Unit,
    onCustomHabitNameChange: (String) -> Unit,
    onSelectRakaat: (Int) -> Unit,
    onSelectFrequency: (FrequencyType) -> Unit,
    onToggleDay: (Int) -> Unit,
    onToggleReminder: () -> Unit,
    onShowTimePicker: () -> Unit,
    onHideTimePicker: () -> Unit,
    onReminderTimeChange: (Int, Int) -> Unit,
    onSaveClick: () -> Unit,
    getFormattedReminderTime: () -> String,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.reminderHour,
        initialMinute = uiState.reminderMinute,
        is24Hour = true
    )

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.save_sunnah_worship),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // Header
            AddHabitHeader(onNavigateBack = onNavigateBack)

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // ── Kategori ──
                SectionHeading(stringResource(R.string.category_heading))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SunnahCategoryType.entries.forEach { category ->
                        val isSelected = uiState.selectedCategory == category
                        SelectableChip(
                            text = category.displayName,
                            isSelected = isSelected,
                            onClick = { onSelectCategory(category) }
                        )
                    }
                }

                // ── Pilih Ibadah ──
                SectionHeading(stringResource(R.string.select_worship_heading))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.availableHabits.forEach { habit ->
                        val isSelected = habit.id == uiState.selectedHabitId
                        Card(
                            modifier = Modifier
                                .width(150.dp)
                                .fillMaxHeight()
                                .clickable { onSelectHabit(habit.id) },
                            shape = RoundedCornerShape(16.dp),
                            border = if (isSelected) BorderStroke(2.dp, Emerald) else null,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) EmeraldLight.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = habit.name,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Emerald else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = habit.description,
                                    fontSize = 12.sp,
                                    color = Gray500
                                )
                            }
                        }
                    }
                }

                // ── Lainnya expandable section ──
                AnimatedVisibility(visible = uiState.isLainnya) {
                    LainnyaSection(
                        uiState = uiState,
                        onSelectExtra = onSelectExtraHabit,
                        onEnableCustom = onEnableCustomInput,
                        onCustomNameChange = onCustomHabitNameChange
                    )
                }

                // ── Jumlah Rakaat (Sholat only) ──
                AnimatedVisibility(visible = uiState.showRakaat) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeading(stringResource(R.string.rakaat_heading))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RAKAAT_OPTIONS.forEach { rakaat ->
                                val isSelected = uiState.selectedRakaat == rakaat
                                Surface(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { onSelectRakaat(rakaat) },
                                    shape = CircleShape,
                                    color = if (isSelected) Emerald else MaterialTheme.colorScheme.surface,
                                    border = if (!isSelected) BorderStroke(1.dp, Emerald.copy(alpha = 0.4f)) else null
                                ) {
                                    Text(
                                        text = "$rakaat",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Emerald
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Frekuensi ──
                SectionHeading(stringResource(R.string.frequency_heading))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FrequencyType.entries.forEach { freq ->
                            val isSelected = uiState.frequencyType == freq
                            SelectableChip(
                                text = freq.displayName,
                                isSelected = isSelected,
                                onClick = { onSelectFrequency(freq) }
                            )
                        }
                    }

                    AnimatedVisibility(visible = uiState.frequencyType == FrequencyType.SPECIFIC_DAYS) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WEEK_DAYS.forEach { day ->
                                DaySelectionChip(
                                    title = day.shortName,
                                    selected = uiState.selectedDays.contains(day.id),
                                    onClick = { onToggleDay(day.id) }
                                )
                            }
                        }
                    }
                }

                // ── Pengingat ──
                SectionHeading(stringResource(R.string.reminder_heading))
                ReminderCard(
                    isEnabled = uiState.isReminderEnabled,
                    reminderTime = getFormattedReminderTime(),
                    onToggle = onToggleReminder,
                    onShowTimePicker = onShowTimePicker
                )

                // Bottom spacer so content isn't hidden behind save button
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Time picker dialog
    if (uiState.showTimePicker) {
        TimePickerDialog(
            onDismiss = onHideTimePicker,
            onConfirm = {
                onReminderTimeChange(timePickerState.hour, timePickerState.minute)
            }
        ) {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = EmeraldLight,
                    selectorColor = Emerald,
                    containerColor = MaterialTheme.colorScheme.surface,
                    periodSelectorBorderColor = Emerald,
                    periodSelectorSelectedContainerColor = Emerald,
                    periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    timeSelectorSelectedContainerColor = Emerald,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

// ─── Header ───

@Composable
private fun AddHabitHeader(onNavigateBack: () -> Unit) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.add_sunnah_worship),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.add_habit_screen_description),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─── Lainnya Section ───

@Composable
private fun LainnyaSection(
    uiState: AddHabitUiState,
    onSelectExtra: (String) -> Unit,
    onEnableCustom: () -> Unit,
    onCustomNameChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.other_options_heading),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            // Extra preset chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.extraHabits.forEach { extra ->
                    val isSelected = !uiState.isCustomHabit && uiState.selectedExtraId == extra.id
                    Surface(
                        modifier = Modifier
                            .clickable { onSelectExtra(extra.id) },
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) Emerald else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (!isSelected) BorderStroke(1.dp, Emerald) else null
                    ) {
                        Text(
                            text = extra.name,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider(color = Gray300.copy(alpha = 0.5f))

            // "Tulis sendiri" button + text field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onEnableCustom() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = Emerald,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.write_custom_label),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Emerald
                )
            }

            AnimatedVisibility(visible = uiState.isCustomHabit) {
                val label = if (uiState.selectedCategory == SunnahCategoryType.SHOLAT) {
                    stringResource(R.string.custom_sholat_label)
                } else {
                    stringResource(R.string.custom_puasa_label)
                }
                OutlinedTextField(
                    value = uiState.customHabitName,
                    onValueChange = onCustomNameChange,
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald,
                        focusedLabelColor = Emerald,
                        cursorColor = Emerald
                    )
                )
            }
        }
    }
}

// ─── Reusable components ───

@Composable
private fun SelectableChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (isSelected) Emerald else MaterialTheme.colorScheme.surface,
        border = if (!isSelected) BorderStroke(1.dp, Emerald) else null
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Emerald
        )
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
}

@Composable
private fun DaySelectionChip(title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) Emerald else MaterialTheme.colorScheme.surface,
        border = if (!selected) BorderStroke(1.dp, Gray300) else null
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else Gray700
        )
    }
}

@Composable
private fun ReminderCard(
    isEnabled: Boolean,
    reminderTime: String,
    onToggle: () -> Unit,
    onShowTimePicker: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isEnabled) Icons.Outlined.NotificationsActive
                        else Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = Emerald
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.enable_reminder),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.reminder_for_sunnah_only),
                            fontSize = 12.sp,
                            color = Gray500
                        )
                    }
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = Emerald
                    )
                )
            }
            AnimatedVisibility(visible = isEnabled) {
                Column {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onShowTimePicker)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = Gray500
                            )
                            Text(
                                text = stringResource(R.string.reminder_time),
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = reminderTime,
                            fontWeight = FontWeight.SemiBold,
                            color = Emerald
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.ok), color = Emerald)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = Gray500)
            }
        },
        title = {
            Text(text = stringResource(R.string.select_time), fontWeight = FontWeight.SemiBold)
        },
        text = { content() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Preview(showBackground = true, name = "Add Habit Light")
@Composable
fun AddHabitScreenPreview() {
    HabitIslamiTheme {
        AddHabitScreenContent(
            uiState = AddHabitUiState(),
            onSelectCategory = {},
            onSelectHabit = {},
            onSelectExtraHabit = {},
            onEnableCustomInput = {},
            onCustomHabitNameChange = {},
            onSelectRakaat = {},
            onSelectFrequency = {},
            onToggleDay = {},
            onToggleReminder = {},
            onShowTimePicker = {},
            onHideTimePicker = {},
            onReminderTimeChange = { _, _ -> },
            onSaveClick = {},
            getFormattedReminderTime = { "05:00" },
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Add Habit Dark")
@Composable
fun AddHabitScreenPreviewDark() {
    HabitIslamiTheme(darkTheme = true) {
        AddHabitScreenContent(
            uiState = AddHabitUiState(),
            onSelectCategory = {},
            onSelectHabit = {},
            onSelectExtraHabit = {},
            onEnableCustomInput = {},
            onCustomHabitNameChange = {},
            onSelectRakaat = {},
            onSelectFrequency = {},
            onToggleDay = {},
            onToggleReminder = {},
            onShowTimePicker = {},
            onHideTimePicker = {},
            onReminderTimeChange = { _, _ -> },
            onSaveClick = {},
            getFormattedReminderTime = { "05:00" },
            onNavigateBack = {}
        )
    }
}
