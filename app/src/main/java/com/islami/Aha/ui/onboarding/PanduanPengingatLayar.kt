package com.islami.Aha.ui.onboarding

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.util.PanduanPengingat
import kotlinx.coroutines.delay

/** Detik sebelum tombol lanjut aktif, agar layar ini dibaca dulu (kecuali salah satu tombol sudah dipakai). */
private const val JEDA_BACA_DETIK = 5

/**
 * "Agar pengingat tidak terlewat": dua langkah untuk HP dengan penghemat baterai agresif.
 * Dipakai di onboarding (langkah Notifikasi) dan dari Pengaturan. Tidak memaksa: tombol lanjut
 * aktif setelah [JEDA_BACA_DETIK] detik, setelah salah satu langkah dibuka, atau bila keduanya beres.
 *
 * Status tiap langkah dibaca ulang dari sistem setiap layar tampil lagi (kembali dari Pengaturan HP):
 * yang sudah aktif langsung tercentang. Autostart hanya terbaca di MIUI/HyperOS; di merek lain
 * layar mengingat bahwa langkahnya sudah pernah dibuka.
 */
@Composable
fun ColumnScope.PanduanPengingatLayar(labelLanjut: String = "Lanjut", onLanjut: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { com.islami.Aha.util.SecurePrefsProvider.get(context) }
    var autostartDibuka by rememberSaveable { mutableStateOf(prefs.getBoolean(PanduanPengingat.KEY_AUTOSTART_DIBUKA, false)) }
    var bateraiDibuka by rememberSaveable { mutableStateOf(prefs.getBoolean(PanduanPengingat.KEY_BATERAI_DIBUKA, false)) }
    var statusAutostart by remember { mutableStateOf(PanduanPengingat.statusAutostart(context)) }
    var statusBaterai by remember { mutableStateOf(PanduanPengingat.statusBaterai(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                statusAutostart = PanduanPengingat.statusAutostart(context)
                statusBaterai = PanduanPengingat.statusBaterai(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val autostartBeres = PanduanPengingat.beres(statusAutostart, autostartDibuka)
    val bateraiBeres = PanduanPengingat.beres(statusBaterai, bateraiDibuka)
    var sisaDetik by rememberSaveable { mutableIntStateOf(JEDA_BACA_DETIK) }
    LaunchedEffect(Unit) {
        while (sisaDetik > 0) {
            delay(1_000)
            sisaDetik--
        }
    }
    val bolehLanjut = sisaDetik == 0 || autostartDibuka || bateraiDibuka || (autostartBeres && bateraiBeres)
    val emas = Color(0xFFE2B65A)

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.Shield, contentDescription = null, tint = emas, modifier = Modifier.size(44.dp))
        Text("Agar pengingat\ntidak terlewat", fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "HP ini menghemat baterai dengan menghentikan aplikasi yang ditutup. Akibatnya pengingat sholat " +
                "bisa tidak berbunyi. Dua pengaturan di bawah ini mencegahnya. Masing-masing cukup sekali.",
            fontSize = 15.sp,
            lineHeight = 23.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        LangkahPanduan(
            nomor = 1,
            ikon = Icons.Rounded.PlayCircle,
            judul = "Nyalakan Autostart",
            keterangan = "Cari Aha di daftar, lalu nyalakan.",
            selesai = autostartBeres,
            status = PanduanPengingat.keteranganStatus(statusAutostart, autostartDibuka)
        ) {
            autostartDibuka = true
            PanduanPengingat.tandaiDibuka(prefs, PanduanPengingat.KEY_AUTOSTART_DIBUKA)
            if (!PanduanPengingat.bukaAutostart(context)) {
                Toast.makeText(
                    context,
                    "Layar Autostart tidak bisa dibuka otomatis. Cari menu \"Autostart\" atau \"Mulai otomatis\" di Pengaturan HP.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        LangkahPanduan(
            nomor = 2,
            ikon = Icons.Rounded.BatteryChargingFull,
            judul = "Bebaskan dari penghemat baterai",
            keterangan = PanduanPengingat.petunjukBaterai(),
            selesai = bateraiBeres,
            status = PanduanPengingat.keteranganStatus(statusBaterai, bateraiDibuka)
        ) {
            bateraiDibuka = true
            PanduanPengingat.tandaiDibuka(prefs, PanduanPengingat.KEY_BATERAI_DIBUKA)
            if (!PanduanPengingat.bukaPengaturanBaterai(context)) {
                Toast.makeText(context, "Buka Pengaturan HP › Aplikasi › Aha › Baterai.", Toast.LENGTH_LONG).show()
            }
        }
        Text(
            "Bisa diatur nanti dari Pengaturan › Agar pengingat tidak terlewat.",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        Button(
            onClick = onLanjut,
            enabled = bolehLanjut,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                if (bolehLanjut) labelLanjut else "$labelLanjut ($sisaDetik)",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LangkahPanduan(
    nomor: Int,
    ikon: ImageVector,
    judul: String,
    keterangan: String,
    selesai: Boolean,
    status: String,
    onClick: () -> Unit
) {
    val hijau = Color(0xFF14A87D)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(
            if (selesai) Icons.Rounded.CheckCircle else ikon,
            contentDescription = null,
            tint = if (selesai) hijau else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("$nomor. $judul", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(keterangan, fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                status,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selesai) hijau else MaterialTheme.colorScheme.primary
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
    }
}
