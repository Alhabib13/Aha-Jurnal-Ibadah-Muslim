package com.islami.Aha.ui.settings.components

import android.media.RingtoneManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.ui.settings.TimeFormatOption
import com.islami.Aha.ui.theme.ThemeMode
import com.islami.Aha.util.NotificationScheduler
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Lembar bawah bersama: judul, keterangan, lalu isi pilihan. Memilih langsung menyimpan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PilihanSheet(
    judul: String,
    keterangan: String,
    onDismiss: () -> Unit,
    isi: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(judul, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(keterangan, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            isi()
        }
    }
}

@Composable
private fun TandaDipilih(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
    }
}

private fun Modifier.kartuPilihan(dipilih: Boolean, warnaBingkai: Color, onClick: () -> Unit): Modifier =
    this
        .clip(RoundedCornerShape(18.dp))
        .border(BorderStroke(if (dipilih) 2.dp else 1.dp, warnaBingkai), RoundedCornerShape(18.dp))
        .semantics { role = Role.RadioButton; selected = dipilih }
        .clickable(onClick = onClick)

// ---------------------------------------------------------------- Mode gelap

@Composable
fun ThemeModeSelectionDialog(
    currentMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    PilihanSheet("Tampilan", "Pilih tema aplikasi. Perubahan langsung diterapkan.", onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEach { mode ->
                val dipilih = mode == currentMode
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .kartuPilihan(
                            dipilih,
                            if (dipilih) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ) { onSelect(mode) }
                        .padding(10.dp)
                ) {
                    Box {
                        PratinjauTema(mode)
                        if (dipilih) TandaDipilih(Modifier.align(Alignment.TopEnd).padding(4.dp))
                    }
                    Text(mode.displayName, fontSize = 14.sp, fontWeight = if (dipilih) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}

/** Gambar mini layar: terang, gelap, atau dibelah dua untuk "Ikuti Sistem". */
@Composable
private fun PratinjauTema(mode: ThemeMode) {
    val terang = Color(0xFFF4F6F8)
    val gelap = Color(0xFF151B22)
    val latar = when (mode) {
        ThemeMode.LIGHT -> Brush.linearGradient(listOf(terang, terang))
        ThemeMode.DARK -> Brush.linearGradient(listOf(gelap, gelap))
        ThemeMode.SYSTEM -> Brush.horizontalGradient(0.5f to terang, 0.5f to gelap)
    }
    val garis = when (mode) {
        ThemeMode.LIGHT -> Color(0xFFCBD3DB)
        else -> Color(0xFF3A4654)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(72.dp)
            .height(104.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(latar)
            .padding(10.dp)
    ) {
        Box(Modifier.width(28.dp).height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Box(Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(5.dp)).background(garis))
        Box(Modifier.fillMaxWidth(0.8f).height(6.dp).clip(CircleShape).background(garis))
        Box(Modifier.fillMaxWidth(0.6f).height(6.dp).clip(CircleShape).background(garis))
    }
}

// ---------------------------------------------------------------- Format waktu

@Composable
fun TimeFormatSelectionDialog(
    currentFormat: TimeFormatOption,
    onSelect: (TimeFormatOption) -> Unit,
    onDismiss: () -> Unit
) {
    val sekarang = remember { LocalTime.now() }
    PilihanSheet("Format waktu", "Dipakai untuk jadwal sholat dan jam pengingat.", onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TimeFormatOption.entries.forEach { format ->
                val dipilih = format == currentFormat
                val contoh = when (format) {
                    TimeFormatOption.HOUR_24 -> sekarang.format(DateTimeFormatter.ofPattern("HH:mm"))
                    TimeFormatOption.HOUR_12 -> sekarang.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .kartuPilihan(
                            dipilih,
                            if (dipilih) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ) { onSelect(format) }
                        .background(if (dipilih) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(contoh, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text(format.displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Sekarang", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (dipilih) TandaDipilih(Modifier.align(Alignment.TopEnd))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Suara notifikasi

@Composable
fun NotificationSoundSelectionDialog(
    currentOption: NotificationScheduler.NotificationSoundOption,
    onSelect: (NotificationScheduler.NotificationSoundOption) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    PilihanSheet("Suara notifikasi", "Suara saat pengingat ibadah muncul.", onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NotificationScheduler.NotificationSoundOption.entries.forEach { option ->
                val dipilih = option == currentOption
                val ikon: ImageVector = when (option) {
                    NotificationScheduler.NotificationSoundOption.SYSTEM_DEFAULT -> Icons.Rounded.VolumeUp
                    NotificationScheduler.NotificationSoundOption.GENTLE -> Icons.Rounded.VolumeDown
                    NotificationScheduler.NotificationSoundOption.SILENT -> Icons.Rounded.VolumeOff
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .kartuPilihan(
                            dipilih,
                            if (dipilih) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ) { onSelect(option) }
                        .background(if (dipilih) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(ikon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(option.displayName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(option.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val uri = NotificationScheduler.getSoundUri(option)
                    if (uri != null) {
                        TextButton(onClick = {
                            runCatching { RingtoneManager.getRingtone(context, uri)?.play() }
                        }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Dengar")
                        }
                    }
                    if (dipilih) TandaDipilih()
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Pengaturan notifikasi

/**
 * Dibuka dari ikon gear di halaman Notifikasi. Satu-satunya tempat mengatur suara & getar
 * (menu lama di Pengaturan sudah dihapus).
 */
@Composable
fun NotificationSettingsSheet(
    ringkasanSuaraSholat: Map<String, String>,
    namaWaktu: Map<String, String>,
    suaraLainnya: NotificationScheduler.NotificationSoundOption,
    getarAktif: Boolean,
    onWaktuClick: (String) -> Unit,
    onSuaraLainnyaClick: () -> Unit,
    onToggleGetar: () -> Unit,
    onPengaturanHpClick: () -> Unit,
    onDismiss: () -> Unit
) {
    PilihanSheet("Pengaturan notifikasi", "Suara dan getar untuk pengingat ibadah.", onDismiss) {
        Text(
            "SUARA TIAP WAKTU SHOLAT",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(18.dp))
        ) {
            namaWaktu.entries.forEachIndexed { i, (slug, nama) ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                BarisPengaturan(
                    ikon = Icons.Rounded.Mosque,
                    judul = nama,
                    nilai = ringkasanSuaraSholat[slug].orEmpty(),
                    onClick = { onWaktuClick(slug) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "LAINNYA",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(18.dp))
        ) {
            BarisPengaturan(
                ikon = Icons.AutoMirrored.Rounded.VolumeUp,
                judul = "Suara pengingat lainnya",
                keterangan = "Sholat sunnah, puasa, dan pengingat lain",
                nilai = suaraLainnya.displayName,
                onClick = onSuaraLainnyaClick
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleGetar)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                IkonBulat(Icons.Rounded.Vibration)
                Text("Getar", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Switch(checked = getarAktif, onCheckedChange = { onToggleGetar() })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BarisPengaturan(
                ikon = Icons.Rounded.Settings,
                judul = "Pengaturan notifikasi HP",
                keterangan = "Izin, layar kunci, dan mode Jangan Ganggu",
                nilai = "",
                onClick = onPengaturanHpClick
            )
        }
    }
}

@Composable
private fun IkonBulat(ikon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) { Icon(ikon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun BarisPengaturan(
    ikon: ImageVector,
    judul: String,
    nilai: String,
    keterangan: String? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        IkonBulat(ikon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(judul, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (keterangan != null) {
                Text(keterangan, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (nilai.isNotEmpty()) {
            Text(nilai, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
