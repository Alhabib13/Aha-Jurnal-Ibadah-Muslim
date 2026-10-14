package com.islami.Aha.ui.addhabit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.data.dzikir.DzikirKatalogEntry
import com.islami.Aha.data.research.ResearchSchema

/** Aksi kategori Dzikir di Add Habit (bawaan kosong untuk preview). */
data class DzikirActions(
    val onSelectCategory: () -> Unit = {},
    val onToggleEntry: (slug: String) -> Unit = {},
    val onAddLainnya: () -> Unit = {},
    val onRemove: (key: String) -> Unit = {},
    val onTargetChange: (key: String, text: String) -> Unit = { _, _ -> },
    val onNamaChange: (key: String, nama: String) -> Unit = { _, _ -> },
    val onModeChange: (key: String, mode: String) -> Unit = { _, _ -> }
)

private fun waktuLabel(waktu: String): String = when (waktu) {
    ResearchSchema.WaktuDzikir.BADA_SHOLAT -> "Setelah sholat fardhu"
    ResearchSchema.WaktuDzikir.PAGI -> "Pagi"
    ResearchSchema.WaktuDzikir.PETANG -> "Petang"
    ResearchSchema.WaktuDzikir.SEBELUM_TIDUR -> "Sebelum tidur"
    else -> "Kapan saja"
}

/**
 * Daftar 9 entri katalog dzikir. Tidak ada teks bacaan apa pun di sini; mode "layar"
 * ditampilkan tetapi nonaktif selama bacaan katalog masih kosong. Target hitungan
 * opsional dan kosong secara bawaan (tanpa nilai anjuran).
 */
@Composable
internal fun DzikirSection(uiState: AddHabitUiState, actions: DzikirActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pilih dzikir", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(
            "Boleh pilih lebih dari satu. Setiap dzikir dicatat sendiri-sendiri di beranda.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        uiState.dzikirKatalog.forEach { entry ->
            val pilihan = uiState.dzikirPilihan.filter { it.slug == entry.slug }
            val sudahAda = entry.slug in uiState.dzikirSlugSudahAda
            DzikirEntryCard(
                entry = entry,
                pilihan = pilihan,
                sudahAda = sudahAda,
                actions = actions
            )
        }
        uiState.dzikirError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DzikirEntryCard(
    entry: DzikirKatalogEntry,
    pilihan: List<DzikirPilihan>,
    sudahAda: Boolean,
    actions: DzikirActions
) {
    val dipilih = pilihan.isNotEmpty()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dipilih) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !sudahAda) { actions.onToggleEntry(entry.slug) }
            ) {
                Checkbox(
                    checked = dipilih || sudahAda,
                    enabled = !sudahAda,
                    onCheckedChange = { actions.onToggleEntry(entry.slug) }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.nama, fontWeight = FontWeight.Medium)
                    Text(
                        if (sudahAda) "Sudah ada di daftar dzikirmu" else waktuLabel(entry.waktuDefault),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            pilihan.forEachIndexed { i, p ->
                DzikirConfig(entry = entry, pilihan = p, bisaDihapus = pilihan.size > 1, actions = actions)
                if (entry.slug == ResearchSchema.Detail.DZIKIR_LAINNYA && i == pilihan.lastIndex) {
                    TextButton(onClick = actions.onAddLainnya) { Text("+ Tambah dzikir lainnya") }
                }
            }
        }
    }
}

@Composable
private fun DzikirConfig(
    entry: DzikirKatalogEntry,
    pilihan: DzikirPilihan,
    bisaDihapus: Boolean,
    actions: DzikirActions
) {
    Column(
        modifier = Modifier.padding(start = 12.dp, end = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (entry.slug == ResearchSchema.Detail.DZIKIR_LAINNYA) {
            OutlinedTextField(
                value = pilihan.namaAsli,
                onValueChange = { actions.onNamaChange(pilihan.key, it) },
                label = { Text("Nama dzikir (wajib)") },
                singleLine = true,
                isError = pilihan.namaAsli.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text("Cara mencatat", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = pilihan.mode == ResearchSchema.ModePencatatan.CENTANG,
                onClick = { actions.onModeChange(pilihan.key, ResearchSchema.ModePencatatan.CENTANG) },
                label = { Text("Centang") }
            )
            FilterChip(
                selected = pilihan.mode == ResearchSchema.ModePencatatan.LAYAR,
                enabled = entry.bolehModeLayar,
                onClick = { actions.onModeChange(pilihan.key, ResearchSchema.ModePencatatan.LAYAR) },
                label = { Text("Layar") }
            )
        }
        if (!entry.bolehModeLayar) {
            Text(
                "Mode layar belum bisa dipilih karena bacaannya belum tersedia.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedTextField(
            value = pilihan.targetText,
            onValueChange = { actions.onTargetChange(pilihan.key, it) },
            label = { Text("Target hitungan (opsional)") },
            singleLine = true,
            isError = pilihan.targetTidakSah,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        if (bisaDihapus) {
            TextButton(onClick = { actions.onRemove(pilihan.key) }) { Text("Hapus isian ini") }
        }
    }
}
