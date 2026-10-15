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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.data.quran.AyatPosition
import com.islami.Aha.data.quran.QuranCalculator
import com.islami.Aha.data.quran.QuranData

private val Emas = Color(0xFFEDB84E)
private val CoklatTeks = Color(0xFF2B1A00)

/**
 * Pemilih posisi ayat dari bawah layar: cari surah (atau pilih per juz), lalu atur ayat
 * dengan − / + atau ketik. Menggantikan dropdown + kolom ayat yang kaku.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilihAyatSheet(
    judul: String,
    awal: AyatPosition,
    /** Batas bawah (mis. "Baca sampai" tidak boleh sebelum titik mulai). */
    minimal: AyatPosition? = null,
    onPilih: (AyatPosition) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var surah by remember { mutableStateOf(awal.surah) }
    var ayat by remember { mutableStateOf(awal.ayat) }
    var cari by remember { mutableStateOf("") }
    var modeJuz by remember { mutableStateOf(false) }
    val maks = QuranData.JUMLAH_AYAT[surah - 1]
    val pos = AyatPosition(surah, ayat.coerceIn(1, maks))
    val sah = pos.isValid && (minimal == null || pos >= minimal)
    val daftar = remember(cari) {
        (1..114).filter { s ->
            cari.isBlank() || namaSurah(s).contains(cari.trim(), ignoreCase = true) || s.toString() == cari.trim()
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { listState.scrollToItem((awal.surah - 3).coerceAtLeast(0)) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(judul, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)

            // Surah / Juz
            Row(
                modifier = Modifier.fillMaxWidth().clip(CircleShape).background(MaterialTheme.colorScheme.background).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(false to "Per surah", true to "Per juz").forEach { (juz, label) ->
                    val dipilih = modeJuz == juz
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 36.dp)
                            .clip(CircleShape)
                            .background(if (dipilih) Emas else Color.Transparent)
                            .clickable(role = Role.Tab) { modeJuz = juz }
                    ) {
                        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (dipilih) CoklatTeks else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (!modeJuz) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 12.dp)
                        .heightIn(min = 46.dp)
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (cari.isEmpty()) Text("Cari surah, mis. Yasin", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BasicTextField(
                            value = cari,
                            onValueChange = { cari = it.take(30) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(Emas),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().height(300.dp)) {
                if (modeJuz) {
                    items((1..QuranData.TOTAL_JUZ).toList()) { j ->
                        val mulai = QuranCalculator.awalJuz(j)
                        val dipilih = QuranCalculator.juzOf(pos) == j
                        BarisPilihan(
                            nomor = "$j",
                            judul = "Juz $j",
                            keterangan = "Mulai ${namaSurah(mulai.surah)} ${mulai.ayat}",
                            dipilih = dipilih
                        ) { surah = mulai.surah; ayat = mulai.ayat; modeJuz = false }
                    }
                } else {
                    items(daftar) { s ->
                        BarisPilihan(
                            nomor = "$s",
                            judul = namaSurah(s),
                            keterangan = "${QuranData.JUMLAH_AYAT[s - 1]} ayat · Juz ${QuranCalculator.juzOf(AyatPosition(s, 1))}",
                            dipilih = s == surah
                        ) { surah = s; ayat = if (s == awal.surah) awal.ayat else 1 }
                    }
                }
            }

            // Ayat
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(namaSurah(surah), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Ayat 1–$maks · halaman ${QuranCalculator.halamanOf(pos)}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { ayat = (pos.ayat - 1).coerceAtLeast(1) }) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Kurangi ayat", tint = MaterialTheme.colorScheme.onSurface)
                }
                var teks by remember(surah, pos.ayat) { mutableStateOf(pos.ayat.toString()) }
                BasicTextField(
                    value = teks,
                    onValueChange = { v ->
                        teks = v.filter(Char::isDigit).take(3)
                        teks.toIntOrNull()?.let { n -> ayat = n.coerceIn(1, maks) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Emas, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(Emas),
                    modifier = Modifier
                        .width(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .padding(vertical = 8.dp)
                )
                IconButton(onClick = { ayat = (pos.ayat + 1).coerceAtMost(maks) }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Tambah ayat", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Ayat terakhir surah",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emas,
                    modifier = Modifier.clip(CircleShape).clickable(role = Role.Button) { ayat = maks }.padding(vertical = 4.dp)
                )
            }
            if (minimal != null && !sah) {
                Text("Harus sesudah ${namaSurah(minimal.surah)} ${minimal.ayat}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { onPilih(pos) },
                enabled = sah,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Emas, contentColor = CoklatTeks),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) { Text("Pilih ${namaSurah(pos.surah)} ${pos.ayat}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BarisPilihan(nomor: String, judul: String, keterangan: String, dipilih: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (dipilih) Emas.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (dipilih) Emas else MaterialTheme.colorScheme.background)
        ) {
            Text(nomor, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (dipilih) CoklatTeks else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(judul, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (dipilih) Emas else MaterialTheme.colorScheme.onSurface)
            Text(keterangan, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
