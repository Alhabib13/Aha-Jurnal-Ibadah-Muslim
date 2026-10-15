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

/** Pengguna belum menambahkan habit dzikir: ajakan netral, tanpa angka progres. */
@Composable
fun DzikirEntryCard(onAdd: () -> Unit) {
    SectionCard {
        Text("Dzikir", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Belum ada dzikir di daftarmu. Tambahkan dzikir yang biasa kamu amalkan untuk mulai mencatat.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAdd) { Text("Tambahkan dzikir") }
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
