package com.islami.Aha.ui.addhabit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.data.dzikir.DzikirKatalogEntry
import com.islami.Aha.data.research.ResearchSchema

private val Toska = Color(0xFF4FD1C5)
private val TeksDiToska = Color(0xFF062B28)

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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column {
            Text("Pilih dzikir", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Boleh pilih lebih dari satu. Setiap dzikir dicatat sendiri-sendiri di beranda.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        uiState.dzikirKatalog.forEach { entry ->
            DzikirEntryCard(
                entry = entry,
                pilihan = uiState.dzikirPilihan.filter { it.slug == entry.slug },
                sudahAda = entry.slug in uiState.dzikirSlugSudahAda,
                actions = actions
            )
        }
        uiState.dzikirError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
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
    val bentuk = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(bentuk)
            .then(
                if (dipilih) Modifier.background(Toska.copy(alpha = 0.10f)).border(1.5.dp, Toska, bentuk)
                else Modifier.background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outlineVariant, bentuk)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = dipilih || sudahAda, enabled = !sudahAda, role = Role.Checkbox) { actions.onToggleEntry(entry.slug) }
                .padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .then(
                        when {
                            dipilih -> Modifier.background(Toska)
                            sudahAda -> Modifier.background(MaterialTheme.colorScheme.outlineVariant)
                            else -> Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        }
                    )
            ) {
                if (dipilih || sudahAda) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = if (dipilih) TeksDiToska else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.nama, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = if (sudahAda) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (sudahAda) "Sudah ada di daftar dzikirmu" else waktuLabel(entry.waktuDefault),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        pilihan.forEachIndexed { i, p ->
            DzikirConfig(entry = entry, pilihan = p, bisaDihapus = pilihan.size > 1, actions = actions)
            if (entry.slug == ResearchSchema.Detail.DZIKIR_LAINNYA && i == pilihan.lastIndex) {
                Text(
                    "+ Tambah dzikir lainnya",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Toska,
                    modifier = Modifier
                        .padding(start = 12.dp, bottom = 12.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = actions.onAddLainnya)
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                )
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
    val warnaField = OutlinedTextFieldDefaults.colors(focusedBorderColor = Toska, focusedLabelColor = Toska, cursorColor = Toska)
    Column(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (entry.slug == ResearchSchema.Detail.DZIKIR_LAINNYA) {
            OutlinedTextField(
                value = pilihan.namaAsli,
                onValueChange = { actions.onNamaChange(pilihan.key, it) },
                label = { Text("Nama dzikir (wajib)") },
                singleLine = true,
                isError = pilihan.namaAsli.isBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = warnaField,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text("Cara mencatat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Segmen(
            pilihan = listOf("Centang", "Layar"),
            dipilih = if (pilihan.mode == ResearchSchema.ModePencatatan.LAYAR) 1 else 0,
            onPilih = {
                actions.onModeChange(
                    pilihan.key,
                    if (it == 1) ResearchSchema.ModePencatatan.LAYAR else ResearchSchema.ModePencatatan.CENTANG
                )
            },
            aktifDiizinkan = { it == 0 || entry.bolehModeLayar }
        )
        if (!entry.bolehModeLayar) {
            Text(
                "Mode layar belum bisa dipilih karena bacaannya belum tersedia.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedTextField(
            value = pilihan.targetText,
            onValueChange = { actions.onTargetChange(pilihan.key, it) },
            label = { Text("Target hitungan (opsional)") },
            singleLine = true,
            isError = pilihan.targetTidakSah,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = warnaField,
            modifier = Modifier.fillMaxWidth()
        )
        if (bisaDihapus) {
            Text(
                "Hapus isian ini",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clip(CircleShape).clickable(role = Role.Button) { actions.onRemove(pilihan.key) }.padding(vertical = 4.dp, horizontal = 4.dp)
            )
        }
    }
}
