package com.islami.Aha.ui.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.ui.home.DzikirHabitUi
import com.islami.Aha.ui.home.IqroProgressUi

// Versi paling sederhana (keputusan skripsi): tanpa tasbih, tanpa teks/audio Al-Qur'an,
// tanpa isi materi Iqro. Hanya centang dan angka.

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

/**
 * Daftar habit dzikir pilihan pengguna (mode centang). Tidak dipanggil sama sekali bila
 * pengguna belum menambahkan habit dzikir. Nada netral: "belum dicatat", bukan "terlewat".
 */
@Composable
fun DzikirCard(
    habits: List<DzikirHabitUi>,
    onRecord: (habitId: String) -> Unit,
    onUndo: (habitId: String) -> Unit,
    onDelete: (habitId: String) -> Unit
) {
    var hapusId by rememberSaveable { mutableStateOf<String?>(null) }
    SectionCard {
        habits.forEachIndexed { i, h ->
            if (i > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DzikirHabitRow(h, onRecord = { onRecord(h.id) }, onUndo = { onUndo(h.id) }, onDelete = { hapusId = h.id })
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
private fun DzikirHabitRow(h: DzikirHabitUi, onRecord: () -> Unit, onUndo: () -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(h.nama, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            val info = buildList {
                add(h.waktuLabel)
                h.targetHitungan?.let { add("target $it") }
            }.joinToString(" · ")
            Text(info, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = when {
                    !h.dicatatHariIni -> "Belum dicatat hari ini"
                    h.sesiHariIni == 1 -> "Dicatat pukul ${h.jamTerakhir.orEmpty()}"
                    else -> "Dicatat ${h.sesiHariIni}× hari ini, terakhir ${h.jamTerakhir.orEmpty()}"
                },
                fontSize = 12.sp,
                color = if (h.dicatatHariIni) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                if (h.dicatatHariIni) {
                    TextButton(onClick = onUndo, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text("Batalkan terakhir", fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                }
                TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text("Hapus", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onRecord) { Text("Catat") }
    }
}

/** Angka bulat dalam [range]; null bila kosong/di luar rentang (tombol simpan nonaktif). */
private fun parseInRange(text: String, range: IntRange): Int? = text.toIntOrNull()?.takeIf { it in range }

private fun digitsOnly(input: String, maxLen: Int) = input.filter(Char::isDigit).take(maxLen)

@Composable
fun IqroCard(
    progress: IqroProgressUi?,
    onSave: (jilid: Int, halaman: Int) -> Unit
) {
    var jilid by rememberSaveable { mutableStateOf(progress?.jilid ?: 1) }
    var halamanInput by rememberSaveable { mutableStateOf("") }
    val halaman = parseInRange(halamanInput, ResearchSchema.IQRO_HALAMAN)
    SectionCard {
        Text("Iqro / Qiroati / Tilawati / UMMI", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(
            progress?.let { "Posisi terakhir: jilid ${it.jilid}, halaman ${it.halaman} (${it.tanggal})" }
                ?: "Catat jilid dan halaman terakhir yang sudah dipelajari.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text("Jilid", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            ResearchSchema.IQRO_JILID.forEach { n ->
                FilterChip(selected = jilid == n, onClick = { jilid = n }, label = { Text("$n") })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = halamanInput,
                onValueChange = { halamanInput = digitsOnly(it, 3) },
                label = { Text("Halaman") },
                singleLine = true,
                isError = halamanInput.isNotEmpty() && halaman == null,
                supportingText = { Text("${ResearchSchema.IQRO_HALAMAN.first}–${ResearchSchema.IQRO_HALAMAN.last}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { halaman?.let { onSave(jilid, it) }; halamanInput = "" },
                enabled = halaman != null
            ) { Text("Simpan") }
        }
    }
}

/** Pemberitahuan penelitian, sekali muncul, hanya bisa ditutup lewat "Mengerti". */
@Composable
fun ResearchNoticeDialog(
    onAcknowledge: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Data untuk penelitian") },
        text = {
            Text(
                "Mulai versi ini, catatan ibadahmu (jenis ibadah, tanggal, dan jam pencatatan) " +
                    "dipakai untuk penelitian pengembangan aplikasi Aha, termasuk pengingat yang lebih tepat waktu. " +
                    "Data tidak dijual dan tidak dipakai untuk iklan. Rinciannya ada di kebijakan privasi."
            )
        },
        confirmButton = { Button(onClick = onAcknowledge) { Text("Mengerti") } },
        dismissButton = { TextButton(onClick = onOpenPrivacyPolicy) { Text("Kebijakan privasi") } }
    )
}
