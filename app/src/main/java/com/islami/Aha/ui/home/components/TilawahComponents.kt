package com.islami.Aha.ui.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.foundation.border
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
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

private val EmasTua = Color(0xFFB8741F)
private val EmasTengah = Color(0xFFD9952F)
private val EmasMuda = Color(0xFFEDB84E)
private val CoklatTeks = Color(0xFF2B1A00)
private val CoklatRedup = Color(0xFF4A2E00)

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
    var showTarget by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KartuPosisi(
            jenisLabel = JENIS_LABEL[jenisDipilih].orEmpty(),
            isBacaanHarian = isBacaanHarian,
            posisi = if (isBacaanHarian) awalDefault else terakhir,
            targetHalaman = targetHalaman,
            hariIni = bacaanHarianHariIni,
            onUbahPosisi = { showUbahPosisi = true },
            onAturTarget = { showTarget = true }
        )
        // key: isian rentang direset saat jenis sesi atau titik awal default berubah.
        androidx.compose.runtime.key(jenisDipilih, awalDefault) {
            KartuCatatSesi(
                jenisDipilih = jenisDipilih,
                awalDefault = awalDefault,
                onSelectJenis = onSelectJenis,
                onSave = { dari, sampai -> onSave(jenisDipilih, dari, sampai) }
            )
        }
        if (sessions.isNotEmpty()) {
            Text("Sesi hari ini", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            sessions.forEach { s ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(EmasMuda.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = EmasMuda, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${JENIS_LABEL[s.jenisSesi] ?: s.jenisSesi} · ${s.halaman} halaman",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("${s.ayat} ayat · Juz ${s.juz} · ${s.jam}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDeleteSession(s.slotKey) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus sesi ${JENIS_LABEL[s.jenisSesi]} ${s.ayat} ayat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showUbahPosisi) {
        PilihAyatSheet(
            judul = "Ayat terakhir yang sudah dibaca",
            awal = terakhir ?: AyatPosition.AWAL,
            onPilih = { onSetPenanda(jenisDipilih, it); showUbahPosisi = false },
            onDismiss = { showUbahPosisi = false }
        )
    }
    if (showTarget) {
        AlertDialog(
            onDismissRequest = { showTarget = false },
            title = { Text("Target bacaan harian") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Berapa halaman per hari? Boleh tanpa target.", fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        FilterChip(selected = targetHalaman == null, onClick = { onSetTarget(null); showTarget = false }, label = { Text("Tanpa") })
                        TARGET_PILIHAN.forEach { n ->
                            FilterChip(selected = targetHalaman == n, onClick = { onSetTarget(n); showTarget = false }, label = { Text("$n hlm") })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTarget = false }) { Text("Tutup") } }
        )
    }
}

/** Kartu emas: posisi terakhir / lanjutan, plus target harian (hanya bacaan harian). */
@Composable
private fun KartuPosisi(
    jenisLabel: String,
    isBacaanHarian: Boolean,
    posisi: AyatPosition?,
    targetHalaman: Int?,
    hariIni: Int,
    onUbahPosisi: () -> Unit,
    onAturTarget: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(EmasTua, EmasTengah, EmasMuda)))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                (if (isBacaanHarian) "LANJUT BACAAN HARIAN" else "${jenisLabel.uppercase()} TERAKHIR"),
                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CoklatRedup, letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onUbahPosisi, colors = ButtonDefaults.textButtonColors(contentColor = CoklatTeks)) {
                Text("Ubah posisi", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        if (posisi != null) {
            Column {
                Text("${namaSurah(posisi.surah)} · ayat ${posisi.ayat}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CoklatTeks)
                Text(
                    "Juz ${QuranCalculator.juzOf(posisi)} · halaman ${QuranCalculator.halamanOf(posisi)}",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CoklatRedup
                )
            }
        } else {
            Text("Belum ada sesi", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CoklatTeks)
        }
        if (isBacaanHarian) {
            if (targetHalaman != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Target hari ini", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CoklatTeks, modifier = Modifier.weight(1f))
                    Text(
                        if (hariIni == 0) "belum dicatat · $targetHalaman hlm" else "$hariIni dari $targetHalaman halaman",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CoklatTeks
                    )
                }
                LinearProgressIndicator(
                    progress = { (hariIni.toFloat() / targetHalaman).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = CoklatTeks,
                    trackColor = CoklatTeks.copy(alpha = 0.18f),
                    drawStopIndicator = {}
                )
                Text(
                    "Ubah target",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CoklatTeks,
                    modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onAturTarget).padding(vertical = 4.dp)
                )
            } else {
                Text(
                    "Atur target harian",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CoklatTeks,
                    modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onAturTarget).padding(vertical = 4.dp)
                )
            }
        }
    }
}

/** Catat sesi: titik mulai otomatis (bisa diubah), pengguna cukup memilih "Baca sampai". */
@Composable
private fun KartuCatatSesi(
    jenisDipilih: String,
    awalDefault: AyatPosition,
    onSelectJenis: (String) -> Unit,
    onSave: (AyatPosition, AyatPosition) -> Unit
) {
    var dari by remember { mutableStateOf(awalDefault) }
    var sampai by remember { mutableStateOf(awalDefault) }
    var ubahMulai by remember { mutableStateOf(false) }
    var ubahSampai by remember { mutableStateOf(false) }
    val hasil = QuranCalculator.hitung(dari, sampai)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            JENIS_LABEL.forEach { (jenis, label) ->
                FilterChip(
                    selected = jenisDipilih == jenis,
                    onClick = { onSelectJenis(jenis) },
                    label = { Text(label) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmasMuda,
                        selectedLabelColor = Color(0xFF3A2400)
                    )
                )
            }
        }
        BarisPosisi(label = "Mulai dari", posisi = dari, menonjol = false, onClick = { ubahMulai = true })
        BarisPosisi(label = "Baca sampai", posisi = sampai, menonjol = true, onClick = { ubahSampai = true })
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            val halAwal = QuranCalculator.halamanOf(dari)
            AssistChip(onClick = { sampai = QuranCalculator.akhirHalaman(halAwal) }, label = { Text("+1 halaman") }, shape = CircleShape)
            AssistChip(
                onClick = { sampai = QuranCalculator.akhirHalaman((halAwal + 1).coerceAtMost(QuranData.TOTAL_HALAMAN)) },
                label = { Text("+2 halaman") },
                shape = CircleShape
            )
            AssistChip(onClick = { sampai = QuranCalculator.akhirJuz(QuranCalculator.juzOf(dari)) }, label = { Text("Sampai akhir juz") }, shape = CircleShape)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                hasil?.let { "${it.jumlahAyat} ayat · ${it.jumlahHalaman} halaman" } ?: "\"Sampai\" harus sesudah titik mulai",
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (hasil != null) Color(0xFFF2C66D) else MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { hasil?.let { onSave(it.dari, it.sampai) } },
                enabled = hasil != null,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = EmasMuda, contentColor = CoklatTeks)
            ) { Text("Simpan sesi", fontWeight = FontWeight.Bold) }
        }
    }

    if (ubahMulai) {
        PilihAyatSheet(
            judul = "Mulai dari ayat",
            awal = dari,
            onPilih = { dari = it; if (sampai < it) sampai = it; ubahMulai = false },
            onDismiss = { ubahMulai = false }
        )
    }
    if (ubahSampai) {
        PilihAyatSheet(
            judul = "Baca sampai ayat",
            awal = sampai,
            minimal = dari,
            onPilih = { sampai = it; ubahSampai = false },
            onDismiss = { ubahSampai = false }
        )
    }
}

/** Baris posisi yang bisa diketuk: membuka lembar pemilih ayat. */
@Composable
private fun BarisPosisi(label: String, posisi: AyatPosition, menonjol: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
            .then(
                if (menonjol) Modifier.border(1.dp, EmasMuda.copy(alpha = 0.6f), RoundedCornerShape(16.dp)) else Modifier
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = if (menonjol) EmasMuda else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Text(
                "${namaSurah(posisi.surah)} ${posisi.ayat}",
                fontSize = if (menonjol) 17.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Juz ${QuranCalculator.juzOf(posisi)} · halaman ${QuranCalculator.halamanOf(posisi)}",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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
