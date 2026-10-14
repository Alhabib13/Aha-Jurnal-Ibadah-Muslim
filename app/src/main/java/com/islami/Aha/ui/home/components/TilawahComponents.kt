package com.islami.Aha.ui.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.data.quran.AyatPosition
import com.islami.Aha.data.quran.QuranCalculator
import com.islami.Aha.data.quran.QuranData
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.ui.home.TilawahSessionUi

// Pelacak POSISI bacaan, bukan pembaca Al-Qur'an: tidak ada teks Arab, terjemahan, atau audio.
// Semua angka (ayat, halaman, juz) dihitung dari data bundel QuranData; tanpa API.

private val JENIS_LABEL = mapOf(
    ResearchSchema.Detail.BACAAN_HARIAN to "Bacaan harian",
    ResearchSchema.Detail.HAFALAN to "Hafalan",
    ResearchSchema.Detail.MURAJAAH to "Murajaah"
)

private val TARGET_PILIHAN = listOf(1, 2, 3, 5, 10, 20)

fun namaSurah(surah: Int): String = QuranData.NAMA_SURAH[surah - 1]

fun labelPosisi(pos: AyatPosition): String = "${namaSurah(pos.surah)} ${pos.surah}:${pos.ayat}"

@Composable
private fun TilawahCardShell(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun TilawahQuranSection(
    jenisDipilih: String,
    penanda: Map<String, AyatPosition?>,
    targetHalaman: Int?,
    bacaanHarianHariIni: Int,
    sessions: List<TilawahSessionUi>,
    onSelectJenis: (String) -> Unit,
    onSave: (jenis: String, dari: AyatPosition, sampai: AyatPosition) -> Unit,
    onDeleteSession: (slotKey: String) -> Unit,
    onSetPenanda: (jenis: String, pos: AyatPosition) -> Unit,
    onSetTarget: (Int?) -> Unit
) {
    val terakhir = penanda[jenisDipilih]
    val isBacaanHarian = jenisDipilih == ResearchSchema.Detail.BACAAN_HARIAN
    // Hanya bacaan_harian yang maju otomatis; hafalan & murajaah bebas mulai dari mana saja
    // (hafalan juz 30 sering mundur dari An-Nas, murajaah melompat-lompat).
    val awalDefault = if (isBacaanHarian) {
        terakhir?.let { QuranCalculator.setelah(it) } ?: AyatPosition.AWAL
    } else {
        terakhir ?: AyatPosition.AWAL
    }
    var showUbahPosisi by remember { mutableStateOf(false) }

    TilawahCardShell {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            JENIS_LABEL.forEach { (jenis, label) ->
                FilterChip(selected = jenisDipilih == jenis, onClick = { onSelectJenis(jenis) }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (isBacaanHarian) "Lanjut dari" else "Terakhir",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val shown = if (isBacaanHarian) awalDefault else terakhir
                Text(
                    shown?.let { "${labelPosisi(it)} · Juz ${QuranCalculator.juzOf(it)} · hlm ${QuranCalculator.halamanOf(it)}" }
                        ?: "Belum ada sesi",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface
                )
            }
            TextButton(onClick = { showUbahPosisi = true }) { Text("Ubah posisi") }
        }
        if (isBacaanHarian) {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            TargetHarianRow(targetHalaman, bacaanHarianHariIni, onSetTarget)
        }
    }

    // key: isian rentang direset saat jenis sesi atau titik awal default berubah.
    androidx.compose.runtime.key(jenisDipilih, awalDefault) {
        RentangInputCard(awalDefault = awalDefault, onSave = { dari, sampai -> onSave(jenisDipilih, dari, sampai) })
    }

    if (sessions.isNotEmpty()) {
        TilawahCardShell {
            Text("Sesi hari ini", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            sessions.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${JENIS_LABEL[s.jenisSesi] ?: s.jenisSesi} · ${s.ayat} ayat · ${s.halaman} hlm · Juz ${s.juz} · ${s.jam}",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onDeleteSession(s.slotKey) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus sesi ${JENIS_LABEL[s.jenisSesi]} ${s.ayat} ayat")
                    }
                }
            }
        }
    }

    if (showUbahPosisi) {
        UbahPosisiDialog(
            judul = "Ubah posisi ${JENIS_LABEL[jenisDipilih]?.lowercase()}",
            keterangan = "Pilih ayat terakhir yang sudah dibaca.",
            awal = terakhir ?: AyatPosition.AWAL,
            onConfirm = { onSetPenanda(jenisDipilih, it); showUbahPosisi = false },
            onDismiss = { showUbahPosisi = false }
        )
    }
}

@Composable
private fun TargetHarianRow(target: Int?, hariIni: Int, onSetTarget: (Int?) -> Unit) {
    Text(
        when {
            target == null -> "Target harian belum diatur"
            hariIni == 0 -> "Target $target hlm/hari · hari ini belum dicatat"
            hariIni >= target -> "Target $target hlm/hari · hari ini $hariIni hlm ✓"
            else -> "Target $target hlm/hari · hari ini $hariIni hlm"
        },
        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        FilterChip(selected = target == null, onClick = { onSetTarget(null) }, label = { Text("Tanpa") })
        TARGET_PILIHAN.forEach { n ->
            FilterChip(selected = target == n, onClick = { onSetTarget(n) }, label = { Text("$n hlm") })
        }
    }
}

@Composable
private fun RentangInputCard(awalDefault: AyatPosition, onSave: (AyatPosition, AyatPosition) -> Unit) {
    var dari by remember { mutableStateOf(awalDefault) }
    var sampai by remember { mutableStateOf(awalDefault) }
    val hasil = QuranCalculator.hitung(dari, sampai)

    TilawahCardShell {
        PositionPicker(label = "Dari", value = dari, onChange = { dari = it; if (sampai < it) sampai = it })
        Spacer(Modifier.height(12.dp))
        PositionPicker(label = "Sampai", value = sampai, onChange = { sampai = it })
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text(
            hasil?.let { "${it.jumlahAyat} ayat · ${it.jumlahHalaman} halaman · mulai Juz ${it.juzAwal}" }
                ?: "\"Sampai\" harus sesudah \"Dari\"",
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = if (hasil != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { hasil?.let { onSave(it.dari, it.sampai) } }, enabled = hasil != null, modifier = Modifier.fillMaxWidth()) {
            Text("Selesai")
        }
    }
}

/**
 * Pemilih posisi dengan dua cara masuk: per surah, atau PILIH JUZ dulu (lebih cepat bagi
 * yang membaca per juz) lalu surah & ayat di dalam juz itu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionPicker(label: String, value: AyatPosition, onChange: (AyatPosition) -> Unit) {
    var modeJuz by rememberSaveable { mutableStateOf(false) }
    val juzSekarang = QuranCalculator.juzOf(value)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        FilterChip(selected = !modeJuz, onClick = { modeJuz = false }, label = { Text("Surah") })
        Spacer(Modifier.width(6.dp))
        FilterChip(selected = modeJuz, onClick = { modeJuz = true }, label = { Text("Juz") })
    }
    if (modeJuz) {
        SimpleDropdown(
            label = "Juz",
            selectedText = "Juz $juzSekarang",
            items = (1..QuranData.TOTAL_JUZ).map { j ->
                val awal = QuranCalculator.awalJuz(j)
                j to "Juz $j · mulai ${labelPosisi(awal)}"
            },
            onSelect = { j -> onChange(QuranCalculator.awalJuz(j)) }
        )
        Spacer(Modifier.height(6.dp))
    }
    val surahPilihan = if (modeJuz) QuranCalculator.surahDalamJuz(juzSekarang) else 1..114
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1.6f)) {
            SimpleDropdown(
                label = "Surah",
                selectedText = "${value.surah}. ${namaSurah(value.surah)}",
                items = surahPilihan.map { s -> s to "$s. ${namaSurah(s)} (${QuranData.JUMLAH_AYAT[s - 1]} ayat)" },
                onSelect = { s -> onChange(AyatPosition(s, 1)) }
            )
        }
        Spacer(Modifier.width(8.dp))
        AyatField(
            surah = value.surah,
            ayat = value.ayat,
            onChange = { a -> onChange(AyatPosition(value.surah, a)) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AyatField(surah: Int, ayat: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val maks = QuranData.JUMLAH_AYAT[surah - 1]
    var text by remember(surah, ayat) { mutableStateOf(ayat.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input.filter(Char::isDigit).take(3)
            text.toIntOrNull()?.takeIf { it in 1..maks }?.let(onChange)
        },
        label = { Text("Ayat") },
        singleLine = true,
        isError = text.toIntOrNull()?.let { it !in 1..maks } ?: true,
        supportingText = { Text("1–$maks") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDropdown(
    label: String,
    selectedText: String,
    items: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { (id, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}

@Composable
private fun UbahPosisiDialog(
    judul: String,
    keterangan: String,
    awal: AyatPosition,
    onConfirm: (AyatPosition) -> Unit,
    onDismiss: () -> Unit
) {
    var pos by remember { mutableStateOf(awal) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(judul) },
        text = {
            Column {
                Text(keterangan, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                PositionPicker(label = "Posisi", value = pos, onChange = { pos = it })
            }
        },
        confirmButton = { Button(onClick = { onConfirm(pos) }, enabled = pos.isValid) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

/** Khatam: diberitahukan dulu, tidak di-reset diam-diam. */
@Composable
fun KhatamDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alhamdulillah, khatam!") },
        text = {
            Text(
                "Bacaan harianmu sudah sampai ${namaSurah(114)} ayat 6. " +
                    "Penanda bacaan harian kembali ke ${namaSurah(1)} ayat 1."
            )
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Mulai lagi dari ${namaSurah(1)}") } }
    )
}

/** Atribusi wajib lisensi CC BY 3.0 untuk data halaman & juz. */
@Composable
fun TanzilAttribution() {
    Text(
        "Data halaman & juz: Tanzil.net (CC BY 3.0)",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
