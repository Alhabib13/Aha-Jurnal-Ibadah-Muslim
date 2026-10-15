package com.islami.Aha.ui.notification.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.islami.Aha.util.SuaraSholat

/** Lembar bawah "Suara <waktu>": adzan bawaan, nada HP, file sendiri, atau senyap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuaraSholatSheet(
    slug: String,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val namaWaktu = SuaraSholat.WAKTU[slug] ?: slug
    var versi by remember { mutableIntStateOf(0) }
    val setelan = remember(versi) { SuaraSholat.setelan(context, slug) }
    var sedangDiputar by remember { mutableStateOf<Ringtone?>(null) }

    DisposableEffect(Unit) {
        onDispose { runCatching { sedangDiputar?.stop() } }
    }

    fun simpan(pilihan: SuaraSholat.Pilihan, uri: Uri? = null, nama: String? = null) {
        SuaraSholat.simpan(context, slug, pilihan, uri, nama)
        versi++
        onChanged()
    }

    val nadaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { hasil ->
        if (hasil.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri: Uri? = hasil.data?.let {
            if (Build.VERSION.SDK_INT >= 33) it.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            else @Suppress("DEPRECATION") it.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        if (uri == null) {
            simpan(SuaraSholat.Pilihan.SENYAP)
        } else {
            val judul = runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }.getOrNull()
            simpan(SuaraSholat.Pilihan.NADA_HP, uri, judul)
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bisaDisimpan = runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.isSuccess
        if (!bisaDisimpan) {
            Toast.makeText(context, "File ini tidak bisa dipakai. Coba pilih dari penyimpanan HP.", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        simpan(SuaraSholat.Pilihan.FILE_SENDIRI, uri, namaFile(context, uri))
    }

    fun putar(uri: Uri?) {
        runCatching { sedangDiputar?.stop() }
        sedangDiputar = uri?.let { u -> runCatching { RingtoneManager.getRingtone(context, u)?.also { it.play() } }.getOrNull() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Suara $namaWaktu", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Bisa berbeda untuk tiap waktu sholat.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))

            Judul("ADZAN")
            SuaraSholat.Rekaman.untukWaktu(slug).forEach { rekaman ->
                val dipilih = setelan.pilihan == SuaraSholat.Pilihan.ADZAN && setelan.rekaman == rekaman
                KartuSuara(
                    ikon = Icons.Rounded.Mosque,
                    warnaIkon = Color(0xFFE2B65A),
                    judul = rekaman.nama,
                    keterangan = if (rekaman == SuaraSholat.Rekaman.SUBUH_MALMO) "Dengan lafal khusus Subuh · ${rekaman.durasi}"
                    else "Durasi ${rekaman.durasi}",
                    dipilih = dipilih,
                    onDengar = { putar(SuaraSholat.adzanUri(context, rekaman)) },
                    onClick = {
                        SuaraSholat.simpanRekaman(context, slug, rekaman)
                        versi++
                        onChanged()
                    }
                )
            }
            Spacer(Modifier.height(4.dp))
            Judul("LAINNYA")
            KartuSuara(
                ikon = Icons.Rounded.NotificationsActive,
                warnaIkon = MaterialTheme.colorScheme.primary,
                judul = SuaraSholat.Pilihan.NADA_HP.label,
                keterangan = if (setelan.pilihan == SuaraSholat.Pilihan.NADA_HP) {
                    setelan.namaFile ?: "Nada default HP"
                } else {
                    "Pilih dari nada yang ada di HP"
                },
                dipilih = setelan.pilihan == SuaraSholat.Pilihan.NADA_HP,
                panah = true,
                onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Nada untuk $namaWaktu")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        val sekarang = if (setelan.pilihan == SuaraSholat.Pilihan.NADA_HP) setelan.uri else null
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, sekarang)
                    }
                    runCatching { nadaLauncher.launch(intent) }
                        .onFailure { simpan(SuaraSholat.Pilihan.NADA_HP) }
                }
            )
            KartuSuara(
                ikon = Icons.Rounded.LibraryMusic,
                warnaIkon = Color(0xFFF48FB1),
                judul = SuaraSholat.Pilihan.FILE_SENDIRI.label,
                keterangan = if (setelan.pilihan == SuaraSholat.Pilihan.FILE_SENDIRI) {
                    setelan.namaFile ?: "File dipilih"
                } else {
                    "MP3/OGG dari penyimpanan HP"
                },
                dipilih = setelan.pilihan == SuaraSholat.Pilihan.FILE_SENDIRI,
                panah = true,
                onDengar = if (setelan.pilihan == SuaraSholat.Pilihan.FILE_SENDIRI) {
                    { putar(setelan.uri) }
                } else null,
                onClick = { runCatching { fileLauncher.launch(arrayOf("audio/*")) } }
            )
            KartuSuara(
                ikon = Icons.Rounded.VolumeOff,
                warnaIkon = MaterialTheme.colorScheme.onSurfaceVariant,
                judul = SuaraSholat.Pilihan.SENYAP.label,
                keterangan = null,
                dipilih = setelan.pilihan == SuaraSholat.Pilihan.SENYAP,
                onClick = { simpan(SuaraSholat.Pilihan.SENYAP) }
            )
            // Atribusi rekaman (wajib lisensi CC BY/BY-SA) ada di Kebijakan Privasi, bagian "Atribusi Data".
        }
    }
}

@Composable
private fun Judul(teks: String) {
    Text(
        teks,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun KartuSuara(
    ikon: ImageVector,
    warnaIkon: Color,
    judul: String,
    keterangan: String?,
    dipilih: Boolean,
    panah: Boolean = false,
    onDengar: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val bingkai = if (dipilih) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(BorderStroke(if (dipilih) 2.dp else 1.dp, bingkai), RoundedCornerShape(18.dp))
            .semantics { role = Role.RadioButton; selected = dipilih }
            .clickable(onClick = onClick)
            .background(if (dipilih) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(warnaIkon.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { Icon(ikon, contentDescription = null, tint = warnaIkon) }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(judul, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (keterangan != null) {
                Text(keterangan, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        if (onDengar != null) {
            TextButton(onClick = onDengar) { Text("Dengar") }
        }
        if (dipilih) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
            }
        } else if (panah) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun namaFile(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
}.getOrNull()
