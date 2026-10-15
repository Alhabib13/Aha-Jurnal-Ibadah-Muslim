package com.islami.Aha.ui.notification.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
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
private val HijauTua = Color(0xFF0B6B52)
private val HijauTengah = Color(0xFF0D7C5F)
private val HijauMuda = Color(0xFF17A27B)
private val HijauAksen = Color(0xFF2ECC9B)
private val TeksDiHijau = Color(0xFF062B1F)
private val EmasAksen = Color(0xFFF2C66D)
private val UnguAksen = Color(0xFFA48BF0)

private fun jamTitik(jam: String?) = jam?.takeIf { it.isNotBlank() }?.replace(":", ".")

@Composable
private fun warnaSaklar() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = HijauAksen,
    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline
)

@Composable
fun NotificationHeader(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    aktif: Int = 0,
    total: Int = 0
) {
    val globalSwitchDescription = if (isEnabled) {
        stringResource(R.string.notification_switch_global_on)
    } else {
        stringResource(R.string.notification_switch_global_off)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.notification_screen_title),
                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground
                )
                Text("Atur pengingat dan suara adzan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onSettingsClick != null) {
                IconButton(
                    onClick = onSettingsClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) { Icon(Icons.Outlined.Settings, contentDescription = "Pengaturan notifikasi") }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    if (isEnabled) Brush.linearGradient(listOf(HijauTua, HijauTengah, HijauMuda))
                    else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
                )
                .padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isEnabled) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.background)
            ) {
                Icon(
                    if (isEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                    contentDescription = null,
                    tint = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isEnabled) "PENGINGAT MENYALA" else "PENGINGAT MATI",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                    color = if (isEnabled) Color(0xFFBFEBDB) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    when {
                        !isEnabled -> "Semua pengingat dijeda"
                        total == 0 -> "Belum ada pengingat"
                        else -> "$aktif dari $total aktif"
                    },
                    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
            Switch(
                modifier = Modifier.semantics { contentDescription = globalSwitchDescription },
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = HijauTengah,
                    checkedTrackColor = Color.White,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.background
                )
            )
        }
    }
}

@Composable
private fun BannerEmas(ikon: androidx.compose.ui.graphics.vector.ImageVector, judul: String?, isi: String, aksi: String, onAksi: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(EmasAksen.copy(alpha = 0.12f))
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Icon(ikon, contentDescription = null, tint = EmasAksen.teks(), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            if (judul != null) Text(judul, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmasAksen.teks())
            Text(isi, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        TextButton(onClick = onAksi) { Text(aksi, color = EmasAksen.teks(), fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun NotificationPermissionBanner(onRequestPermission: () -> Unit) {
    BannerEmas(
        Icons.Outlined.NotificationsOff, null,
        stringResource(R.string.notification_permission_banner_text),
        stringResource(R.string.notification_permission_action),
        onRequestPermission
    )
}

/** Ajakan menyalakan izin "Alarm & pengingat" supaya pengingat sholat tidak telat. */
@Composable
fun ExactAlarmBanner(onAllow: () -> Unit) {
    BannerEmas(
        Icons.Outlined.Alarm, "Pengingat bisa terlambat",
        "Izinkan \"Alarm & pengingat\" supaya pengingat sholat muncul tepat waktu.",
        "Izinkan", onAllow
    )
}

@Composable
fun CategorySectionHeader(title: String, count: Int, warna: Color = HijauAksen) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = warna.teks(), modifier = Modifier.weight(1f))
        Text(
            pluralStringResource(R.plurals.notification_count_ibadah_format, count, count),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Kerangka baris pengingat: jam besar di kiri, isi di tengah, aksi di kanan. */
@Composable
private fun BarisPengingat(
    jam: String?,
    menyala: Boolean,
    redup: Boolean,
    warnaJam: Color,
    tengah: @Composable ColumnScope.() -> Unit,
    kanan: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (redup) 0.55f else 1f)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Column(Modifier.width(62.dp)) {
            Text(
                jamTitik(jam) ?: "--.--",
                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                color = if (menyala && jam != null) warnaJam.teks() else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { tengah() }
        kanan()
    }
}

@Composable
fun HabitReminderCard(
    habit: Habit,
    globalEnabled: Boolean,
    onToggle: () -> Unit,
    suara: String? = null,
    onSuaraClick: (() -> Unit)? = null
) {
    val menyala = globalEnabled && habit.isReminderEnabled
    val reminderSwitchDescription = if (habit.isReminderEnabled) {
        stringResource(R.string.notification_switch_habit_on_format, habit.name)
    } else {
        stringResource(R.string.notification_switch_habit_off_format, habit.name)
    }
    BarisPengingat(
        jam = habit.time,
        menyala = menyala,
        redup = !globalEnabled,
        warnaJam = EmasAksen,
        tengah = {
            Text(
                habit.name.removePrefix("Sholat ").ifBlank { habit.name },
                fontSize = 16.sp, fontWeight = FontWeight.Bold,
                color = if (menyala) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (suara != null && onSuaraClick != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(HijauAksen.copy(alpha = 0.12f))
                        .clickable(role = Role.Button, onClick = onSuaraClick)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = HijauAksen.teks(), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(suara, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HijauAksen.teks(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(stringResource(R.string.notification_frequency_daily), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        kanan = {
            Switch(
                modifier = Modifier.semantics { contentDescription = reminderSwitchDescription },
                checked = habit.isReminderEnabled,
                onCheckedChange = { onToggle() },
                enabled = globalEnabled,
                colors = warnaSaklar()
            )
        }
    )
}

@Composable
fun SunnahHabitReminderCard(
    sunnahHabit: SunnahHabit,
    globalEnabled: Boolean,
    onToggleReminder: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    isHaidhMode: Boolean = false
) {
    val menyala = globalEnabled && sunnahHabit.reminderEnabled
    val warna = if (sunnahHabit.category == SunnahCategoryType.PUASA) UnguAksen else HijauAksen
    var menu by remember { mutableStateOf(false) }
    val reminderSwitchDescription = if (sunnahHabit.reminderEnabled) {
        stringResource(R.string.notification_switch_habit_on_format, sunnahHabit.name)
    } else {
        stringResource(R.string.notification_switch_habit_off_format, sunnahHabit.name)
    }
    BarisPengingat(
        jam = sunnahHabit.reminderTime,
        menyala = menyala,
        redup = !globalEnabled,
        warnaJam = warna,
        tengah = {
            Text(
                sunnahHabit.name, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                color = if (menyala) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            val keterangan = listOfNotNull(
                sunnahHabit.frequencyLabel.ifEmpty { stringResource(R.string.notification_frequency_daily) },
                sunnahHabit.rakaat?.let { pluralStringResource(R.plurals.notification_rakaat_format, it, it) }
            ).joinToString(" · ")
            Text(keterangan, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        kanan = {
            Switch(
                modifier = Modifier.semantics { contentDescription = reminderSwitchDescription },
                checked = sunnahHabit.reminderEnabled,
                onCheckedChange = { onToggleReminder() },
                enabled = globalEnabled,
                colors = warnaSaklar()
            )
            Box {
                IconButton(onClick = { menu = true }, enabled = globalEnabled) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Pilihan lain untuk ${sunnahHabit.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Ubah pengingat") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; onDelete() }
                    )
                }
            }
        }
    )
}

@Composable
fun EmptyNotificationState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.NotificationsOff, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.notification_empty_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = stringResource(R.string.notification_empty_desc),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
