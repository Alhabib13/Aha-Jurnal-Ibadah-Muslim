package com.islami.Aha.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.ui.home.DzikirHabitUi

private val HijauAksen = Color(0xFF2ECC9B)
private val TeksDiHijau = Color(0xFF062B1F)
private val Emas = Color(0xFFF2C66D)

private data class GayaWaktu(val ikon: ImageVector, val warna: Color)

private fun gayaWaktu(waktu: String) = when (waktu) {
    ResearchSchema.WaktuDzikir.BADA_SHOLAT -> GayaWaktu(Icons.Rounded.Mosque, HijauAksen)
    ResearchSchema.WaktuDzikir.PAGI -> GayaWaktu(Icons.Rounded.WbSunny, Emas)
    ResearchSchema.WaktuDzikir.PETANG -> GayaWaktu(Icons.Rounded.WbTwilight, Color(0xFFF4A261))
    ResearchSchema.WaktuDzikir.SEBELUM_TIDUR -> GayaWaktu(Icons.Rounded.Bedtime, Color(0xFFA48BF0))
    else -> GayaWaktu(Icons.Rounded.Schedule, Emas)
}

/**
 * Daftar habit dzikir pilihan pengguna (mode centang), satu kartu per dzikir. Nada netral:
 * "belum dicatat", bukan "terlewat". Hapus disembunyikan di menu supaya tidak terpencet.
 */
@Composable
fun DzikirCard(
    habits: List<DzikirHabitUi>,
    onRecord: (habitId: String) -> Unit,
    onUndo: (habitId: String) -> Unit,
    onDelete: (habitId: String) -> Unit,
    onAdd: () -> Unit = {}
) {
    var hapusId by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        habits.forEach { h ->
            KartuDzikir(h, onRecord = { onRecord(h.id) }, onUndo = { onUndo(h.id) }, onDelete = { hapusId = h.id })
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                .clickable(role = Role.Button, onClick = onAdd)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = HijauAksen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tambah dzikir", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HijauAksen)
        }
    }
    val target = habits.firstOrNull { it.id == hapusId }
    if (target != null) {
        AlertDialog(
            onDismissRequest = { hapusId = null },
            title = { Text("Hapus dari daftar?") },
            text = { Text("${target.nama} tidak lagi tampil di daftar dzikirmu. Catatan yang sudah ada tetap tersimpan.") },
            confirmButton = { TextButton(onClick = { onDelete(target.id); hapusId = null }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { hapusId = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun KartuDzikir(h: DzikirHabitUi, onRecord: () -> Unit, onUndo: () -> Unit, onDelete: () -> Unit) {
    val gaya = gayaWaktu(h.waktu)
    var menuTerbuka by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(gaya.warna.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) { Icon(gaya.ikon, contentDescription = null, tint = gaya.warna, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(h.nama, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        h.waktuLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = gaya.warna,
                        modifier = Modifier.clip(CircleShape).background(gaya.warna.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    h.targetHitungan?.let {
                        Text("Target $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Box {
                IconButton(onClick = { menuTerbuka = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Pilihan lain untuk ${h.nama}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuTerbuka, onDismissRequest = { menuTerbuka = false }) {
                    if (h.dicatatHariIni) {
                        DropdownMenuItem(text = { Text("Batalkan catatan terakhir") }, onClick = { menuTerbuka = false; onUndo() })
                    }
                    DropdownMenuItem(text = { Text("Hapus dari daftar") }, onClick = { menuTerbuka = false; onDelete() })
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            if (h.dicatatHariIni) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(minOf(h.sesiHariIni, 3)) {
                        Box(
                            modifier = Modifier.size(22.dp).clip(CircleShape).background(Color(0xFF14A87D)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (h.sesiHariIni == 1) "Dicatat pukul ${h.jamTerakhir.orEmpty()}" else "${h.sesiHariIni} kali hari ini",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (h.sesiHariIni > 1) {
                        Text("Terakhir ${h.jamTerakhir.orEmpty()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Belum dicatat hari ini", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(
                onClick = onRecord,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = HijauAksen, contentColor = TeksDiHijau),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Catat", fontWeight = FontWeight.Bold)
            }
        }
    }
}
