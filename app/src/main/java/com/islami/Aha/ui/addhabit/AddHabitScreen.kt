package com.islami.Aha.ui.addhabit

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.islami.Aha.R
import com.islami.Aha.ui.components.AhaLoadingOverlay
import com.islami.Aha.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview
import com.islami.Aha.ui.theme.HabitIslamiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    viewModel: AddHabitViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onHabitSaved: () -> Unit = {},
    bukaKategoriDzikir: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(bukaKategoriDzikir) {
        if (bukaKategoriDzikir) viewModel.selectDzikirCategory()
    }

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
        onNavigateBack = onNavigateBack,
        dzikirActions = DzikirActions(
            onSelectCategory = viewModel::selectDzikirCategory,
            onToggleEntry = viewModel::toggleDzikirEntry,
            onAddLainnya = viewModel::addDzikirLainnya,
            onRemove = viewModel::removeDzikirPilihan,
            onTargetChange = viewModel::updateDzikirTarget,
            onNamaChange = viewModel::updateDzikirNama,
            onModeChange = viewModel::selectDzikirMode
        )
    )
}

private val HijauTua = Color(0xFF0D7C5F)
private val HijauMuda = Color(0xFF14A87D)
private val HijauAksen = Color(0xFF2ECC9B)
private val TeksDiHijau = Color(0xFF062B1F)
private val Emas = Color(0xFFF2C66D)
private val Ungu = Color(0xFFA48BF0)
private val UnguTua = Color(0xFF5B3CC4)
private val Toska = Color(0xFF4FD1C5)
private val ToskaTua = Color(0xFF1E8C84)

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
    onNavigateBack: () -> Unit,
    dzikirActions: DzikirActions = DzikirActions()
) {
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.reminderHour,
        initialMinute = uiState.reminderMinute,
        is24Hour = true
    )
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val bisaSimpan = !uiState.isHaidhMode || (uiState.isDzikir && uiState.dzikirBolehSaatCuti)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            KepalaTambah(onNavigateBack)
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (uiState.isHaidhMode) {
                    KartuCuti()
                    if (uiState.dzikirBolehSaatCuti) {
                        Text("Dzikir tetap bisa ditambahkan selama Mode Cuti.", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                        DzikirSection(uiState = uiState, actions = dzikirActions)
                    }
                } else {
                    PilihKategori(uiState, onSelectCategory, dzikirActions.onSelectCategory)
                    if (uiState.isDzikir) {
                        DzikirSection(uiState = uiState, actions = dzikirActions)
                    } else {
                        PilihIbadah(uiState, onSelectHabit)
                        AnimatedVisibility(visible = uiState.isLainnya) {
                            LainnyaSection(uiState, onSelectExtraHabit, onEnableCustomInput, onCustomHabitNameChange)
                        }
                        AnimatedVisibility(visible = uiState.showRakaat) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                JudulBagian("Jumlah rakaat", "(opsional)")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    RAKAAT_OPTIONS.forEach { r ->
                                        TombolPilih("$r", uiState.selectedRakaat == r, Modifier.weight(1f)) { onSelectRakaat(r) }
                                    }
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            JudulBagian("Kapan")
                            Segmen(
                                pilihan = FrequencyType.entries.map { it.displayName },
                                dipilih = FrequencyType.entries.indexOf(uiState.frequencyType),
                                onPilih = { onSelectFrequency(FrequencyType.entries[it]) }
                            )
                            AnimatedVisibility(visible = uiState.frequencyType == FrequencyType.SPECIFIC_DAYS) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    WEEK_DAYS.forEach { d ->
                                        TombolPilih(d.shortName, d.id in uiState.selectedDays, Modifier.weight(1f), bulat = true) { onToggleDay(d.id) }
                                    }
                                }
                            }
                        }
                        KartuPengingat(
                            aktif = uiState.isReminderEnabled,
                            jam = getFormattedReminderTime().replace(":", "."),
                            onToggle = onToggleReminder,
                            onUbahJam = onShowTimePicker
                        )
                    }
                }
                Spacer(Modifier.height(if (bisaSimpan) 96.dp else 24.dp))
            }
        }

        if (bisaSimpan) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background)))
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 14.dp)
            ) {
                Button(
                    onClick = onSaveClick,
                    enabled = !uiState.isSaving,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = HijauAksen, contentColor = TeksDiHijau),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text(labelSimpan(uiState), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }

        AhaLoadingOverlay(
            visible = uiState.isSaving,
            message = stringResource(R.string.add_habit_loading_message)
        )
    }

    if (uiState.showTimePicker) {
        TimePickerDialog(
            onDismiss = onHideTimePicker,
            onConfirm = { onReminderTimeChange(timePickerState.hour, timePickerState.minute) }
        ) {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectorColor = HijauAksen,
                    containerColor = MaterialTheme.colorScheme.surface,
                    timeSelectorSelectedContainerColor = HijauAksen,
                    timeSelectorSelectedContentColor = TeksDiHijau,
                    clockDialSelectedContentColor = TeksDiHijau
                )
            )
        }
    }
}

private fun labelSimpan(s: AddHabitUiState): String = when {
    s.isSaving -> "Menyimpan..."
    s.isDzikir -> if (s.dzikirPilihan.size > 1) "Simpan ${s.dzikirPilihan.size} dzikir" else "Simpan dzikir"
    s.isLainnya && !s.isCustomHabit && s.selectedExtraId == null -> "Simpan"
    s.selectedHabitName.isBlank() -> "Simpan"
    else -> "Simpan ${s.selectedHabitName}"
}

@Composable
private fun KepalaTambah(onNavigateBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp)) {
        IconButton(
            onClick = onNavigateBack,
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
        ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.back)) }
        Spacer(Modifier.width(8.dp))
        Column {
            Text("Tambah ibadah", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Text("Pilih yang ingin kamu jaga setiap hari", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun KartuCuti() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFC1D6).copy(alpha = 0.12f))
            .padding(20.dp)
    ) {
        Icon(painterResource(R.drawable.ic_flower), contentDescription = null, tint = Color(0xFFF48FB1), modifier = Modifier.size(48.dp))
        Text(stringResource(R.string.add_habit_haidh_mode_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFC1D6))
        Text(
            stringResource(R.string.add_habit_haidh_mode_desc),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun JudulBagian(judul: String, tambahan: String? = null) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(judul, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        if (tambahan != null) {
            Spacer(Modifier.width(4.dp))
            Text(tambahan, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PilihKategori(uiState: AddHabitUiState, onSelectCategory: (SunnahCategoryType) -> Unit, onDzikir: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        UbinKategori(
            "Sholat sunnah", Icons.Rounded.Mosque, HijauAksen, listOf(HijauTua, HijauMuda),
            !uiState.isDzikir && uiState.selectedCategory == SunnahCategoryType.SHOLAT, Modifier.weight(1f)
        ) { onSelectCategory(SunnahCategoryType.SHOLAT) }
        UbinKategori(
            "Puasa sunnah", Icons.Rounded.DarkMode, Ungu, listOf(UnguTua, Color(0xFF7C5CE0)),
            !uiState.isDzikir && uiState.selectedCategory == SunnahCategoryType.PUASA, Modifier.weight(1f)
        ) { onSelectCategory(SunnahCategoryType.PUASA) }
        UbinKategori(
            "Dzikir", Icons.Rounded.SelfImprovement, Toska, listOf(ToskaTua, Color(0xFF2FB5AA)),
            uiState.isDzikir, Modifier.weight(1f), onClick = onDzikir
        )
    }
}

@Composable
private fun UbinKategori(
    label: String,
    ikon: ImageVector,
    warna: Color,
    gradasi: List<Color>,
    dipilih: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bentuk = RoundedCornerShape(18.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        modifier = modifier
            .heightIn(min = 88.dp)
            .clip(bentuk)
            .then(
                if (dipilih) Modifier.background(Brush.linearGradient(gradasi)).border(2.dp, Emas, bentuk)
                else Modifier.background(MaterialTheme.colorScheme.surface)
            )
            .selectable(selected = dipilih, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp)
    ) {
        Icon(ikon, contentDescription = null, tint = if (dipilih) Color.White else warna, modifier = Modifier.size(24.dp))
        Text(
            label, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            color = if (dipilih) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PilihIbadah(uiState: AddHabitUiState, onSelectHabit: (String) -> Unit) {
    val utama = uiState.availableHabits.filter { it.id != LAINNYA_ID }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        JudulBagian(if (uiState.selectedCategory == SunnahCategoryType.SHOLAT) "Pilih sholat sunnah" else "Pilih puasa sunnah")
        utama.chunked(2).forEach { baris ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                baris.forEach { p ->
                    KartuPreset(p, p.id == uiState.selectedHabitId, Modifier.weight(1f).fillMaxHeight()) { onSelectHabit(p.id) }
                }
                if (baris.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        val lainnyaTeks = uiState.extraHabits.joinToString(", ") { it.name }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (uiState.isLainnya) Modifier.background(HijauMuda.copy(alpha = 0.12f)).border(1.5.dp, HijauAksen, RoundedCornerShape(16.dp))
                    else Modifier.garisPutus(MaterialTheme.colorScheme.outline, 16.dp)
                )
                .selectable(selected = uiState.isLainnya, role = Role.RadioButton) { onSelectHabit(LAINNYA_ID) }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                "Lainnya: $lainnyaTeks, atau tulis sendiri",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7BE3C2), textAlign = TextAlign.Center
            )
        }
    }
}

private fun Modifier.garisPutus(warna: Color, radius: androidx.compose.ui.unit.Dp) = drawBehind {
    drawRoundRect(
        color = warna,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx()),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
        )
    )
}

@Composable
private fun KartuPreset(p: SunnahHabitPreset, dipilih: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bentuk = RoundedCornerShape(16.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(bentuk)
            .then(
                if (dipilih) Modifier.background(HijauMuda.copy(alpha = 0.12f)).border(1.5.dp, HijauAksen, bentuk)
                else Modifier.background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outlineVariant, bentuk)
            )
            .selectable(selected = dipilih, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (dipilih) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(HijauAksen), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = TeksDiHijau, modifier = Modifier.size(13.dp))
                }
            }
        }
        Text(p.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TombolPilih(teks: String, dipilih: Boolean, modifier: Modifier, bulat: Boolean = false, onClick: () -> Unit) {
    val bentuk = if (bulat) CircleShape else RoundedCornerShape(12.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(bentuk)
            .then(
                if (dipilih) Modifier.background(HijauAksen)
                else Modifier.background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outlineVariant, bentuk)
            )
            .selectable(selected = dipilih, role = Role.Checkbox, onClick = onClick)
    ) {
        Text(
            teks, fontSize = 14.sp,
            fontWeight = if (dipilih) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (dipilih) TeksDiHijau else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun Segmen(pilihan: List<String>, dipilih: Int, onPilih: (Int) -> Unit, aktifDiizinkan: (Int) -> Boolean = { true }) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
    ) {
        pilihan.forEachIndexed { i, label ->
            val aktif = i == dipilih
            val boleh = aktifDiizinkan(i)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
                    .clip(CircleShape)
                    .background(if (aktif) HijauAksen else Color.Transparent)
                    .selectable(selected = aktif, enabled = boleh, role = Role.Tab) { onPilih(i) }
            ) {
                Text(
                    label, fontSize = 13.sp,
                    fontWeight = if (aktif) FontWeight.Bold else FontWeight.SemiBold,
                    color = when {
                        aktif -> TeksDiHijau
                        !boleh -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun KartuPengingat(aktif: Boolean, jam: String, onToggle: () -> Unit, onUbahJam: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Emas.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Emas, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Pengingat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.reminder_for_sunnah_only), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = aktif,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = HijauAksen)
            )
        }
        AnimatedVisibility(visible = aktif) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onUbahJam)
                    .padding(horizontal = 14.dp)
            ) {
                Text(stringResource(R.string.reminder_time), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(jam, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Emas)
            }
        }
    }
}

@Composable
private fun LainnyaSection(
    uiState: AddHabitUiState,
    onSelectExtra: (String) -> Unit,
    onEnableCustom: () -> Unit,
    onCustomNameChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Text(stringResource(R.string.other_options_heading), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            uiState.extraHabits.forEach { extra ->
                val dipilih = !uiState.isCustomHabit && uiState.selectedExtraId == extra.id
                ChipPilih(extra.name, dipilih) { onSelectExtra(extra.id) }
            }
            ChipPilih("Tulis sendiri", uiState.isCustomHabit, onClick = onEnableCustom)
        }
        AnimatedVisibility(visible = uiState.isCustomHabit) {
            OutlinedTextField(
                value = uiState.customHabitName,
                onValueChange = onCustomNameChange,
                label = {
                    Text(
                        if (uiState.selectedCategory == SunnahCategoryType.SHOLAT) stringResource(R.string.custom_sholat_label)
                        else stringResource(R.string.custom_puasa_label)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HijauAksen, focusedLabelColor = HijauAksen, cursorColor = HijauAksen)
            )
        }
    }
}

@Composable
internal fun ChipPilih(teks: String, dipilih: Boolean, onClick: () -> Unit) {
    Text(
        teks,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (dipilih) TeksDiHijau else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(CircleShape)
            .then(
                if (dipilih) Modifier.background(HijauAksen)
                else Modifier.background(MaterialTheme.colorScheme.background).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            .selectable(selected = dipilih, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.ok), color = HijauAksen, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        title = { Text(stringResource(R.string.select_time), fontWeight = FontWeight.SemiBold) },
        text = { content() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Preview(showBackground = true, name = "Tambah ibadah")
@Composable
fun AddHabitScreenPreviewDark() {
    HabitIslamiTheme(darkTheme = true) {
        AddHabitScreenContent(
            uiState = AddHabitUiState(),
            onSelectCategory = {}, onSelectHabit = {}, onSelectExtraHabit = {}, onEnableCustomInput = {},
            onCustomHabitNameChange = {}, onSelectRakaat = {}, onSelectFrequency = {}, onToggleDay = {},
            onToggleReminder = {}, onShowTimePicker = {}, onHideTimePicker = {}, onReminderTimeChange = { _, _ -> },
            onSaveClick = {}, getFormattedReminderTime = { "05:00" }, onNavigateBack = {}
        )
    }
}
