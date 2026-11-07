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
import androidx.compose.runtime.LaunchedEffect
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
 * aktif setelah [JEDA_BACA_DETIK] detik atau setelah salah satu langkah dibuka.
 */
@Composable
fun ColumnScope.PanduanPengingatLayar(labelLanjut: String = "Lanjut", onLanjut: () -> Unit) {
    val context = LocalContext.current
    var autostartDibuka by rememberSaveable { mutableStateOf(false) }
    var bateraiDibuka by rememberSaveable { mutableStateOf(false) }
    var sisaDetik by rememberSaveable { mutableIntStateOf(JEDA_BACA_DETIK) }
    LaunchedEffect(Unit) {
        while (sisaDetik > 0) {
            delay(1_000)
            sisaDetik--
        }
    }
    val bolehLanjut = sisaDetik == 0 || autostartDibuka || bateraiDibuka
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
            selesai = autostartDibuka
        ) {
            autostartDibuka = true
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
            selesai = bateraiDibuka
        ) {
            bateraiDibuka = true
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
    onClick: () -> Unit
) {
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
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("$nomor. $judul", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(keterangan, fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (selesai) "Sudah dibuka · ketuk lagi bila perlu" else "Ketuk untuk membuka",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
    }
}
