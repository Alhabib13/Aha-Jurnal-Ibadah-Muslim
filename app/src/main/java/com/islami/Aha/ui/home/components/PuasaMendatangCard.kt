package com.islami.Aha.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.ui.theme.teks
import com.islami.Aha.util.PuasaMendatang
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Ungu = Color(0xFFA48BF0)
private val BULAN_PENDEK = DateTimeFormatter.ofPattern("MMM", Locale("id", "ID"))

/**
 * Kartu "Puasa sunnah mendatang" di kategori Puasa. Puasa yang punya preset bisa langsung
 * dijadikan habit; saat Mode Cuti, kartu mengingatkan bahwa puasa sunnah menunggu cuti selesai.
 */
@Composable
fun PuasaMendatangCard(
    daftar: List<PuasaMendatang.Entri>,
    namaHabitAda: Set<String>,
    modeCuti: Boolean,
    onJadikanHabit: (PuasaMendatang.Entri) -> Unit
) {
    if (daftar.isEmpty()) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 18.dp)
            ) {
                Icon(Icons.Rounded.NightlightRound, contentDescription = null, tint = Ungu.teks(), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Puasa sunnah mendatang", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            if (modeCuti) {
                Text(
                    "Mode Cuti sedang aktif. Puasa sunnah bisa dilanjutkan setelah cuti selesai; jadwal di bawah tetap ditampilkan sebagai pengingat.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            daftar.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 76.dp, end = 18.dp)
                )
                BarisPuasa(e, sudahJadiHabit = e.namaHabit?.lowercase() in namaHabitAda, bolehTambah = !modeCuti, onJadikanHabit)
            }
            Text(
                "Tanggal Hijriah perkiraan (Umm al-Qura); bisa selisih sehari dengan penetapan di Indonesia.",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 8.dp)
            )
        }
    }
}

@Composable
private fun BarisPuasa(
    e: PuasaMendatang.Entri,
    sudahJadiHabit: Boolean,
    bolehTambah: Boolean,
    onJadikanHabit: (PuasaMendatang.Entri) -> Unit
) {
    val dekat = e.hariLagi <= 1
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        // Blok tanggal Masehi.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(width = 44.dp, height = 48.dp)
                .background(
                    if (dekat) Ungu.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(12.dp)
                )
        ) {
            Text("${e.tanggal.dayOfMonth}", fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
            Text(e.tanggal.format(BULAN_PENDEK), fontSize = 11.sp, lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(e.nama, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (e.keteranganHijri.isNotBlank()) {
                Text(e.keteranganHijri, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (e.namaHabit != null) {
                if (sudahJadiHabit) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ada di daftar ibadahmu", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                } else if (bolehTambah) {
                    TextButton(
                        onClick = { onJadikanHabit(e) },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) { Text("+ Jadikan habit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        Box(
            modifier = Modifier
                .background(
                    if (dekat) Ungu.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(50)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                when (e.hariLagi) {
                    0L -> "Hari ini"
                    1L -> "Besok"
                    else -> "${e.hariLagi} hari"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (dekat) Ungu.teks() else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
