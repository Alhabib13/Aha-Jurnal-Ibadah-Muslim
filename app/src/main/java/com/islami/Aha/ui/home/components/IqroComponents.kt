package com.islami.Aha.ui.home.components

import com.islami.Aha.ui.theme.teks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.ui.home.IqroProgressUi
import com.islami.Aha.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Locale

// Versi paling sederhana (keputusan skripsi): tanpa isi materi Iqro, hanya jilid dan halaman.

private val EmasTua = Color(0xFFB8741F)
private val EmasTengah = Color(0xFFD9952F)
private val EmasMuda = Color(0xFFEDB84E)
private val CoklatTeks = Color(0xFF2B1A00)
private val CoklatRedup = Color(0xFF4A2E00)

private val JILID = ResearchSchema.IQRO_JILID
private val HALAMAN = ResearchSchema.IQRO_HALAMAN

/** "2026-10-12" -> "12 Okt"; teks asli bila format tak dikenali. */
private fun tanggalPendek(tanggal: String): String = runCatching {
    val id = Locale.forLanguageTag("id-ID")
    SimpleDateFormat("d MMM", id).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(tanggal)!!)
}.getOrDefault(tanggal)

/** Iqro / Qiroati / Tilawati / UMMI: kartu posisi terakhir + kartu catat. */
@Composable
fun IqroSection(
    progress: IqroProgressUi?,
    onSave: (jilid: Int, halaman: Int) -> Unit
) {
    val hariIni = progress?.tanggal == DateUtils.getTodayKey()
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KartuPosisiIqro(progress, hariIni)
        // Kunci = posisi terakhir: setelah simpan, isian kembali ke posisi yang baru.
        androidx.compose.runtime.key(progress) {
            KartuCatatIqro(progress, hariIni, onSave)
        }
    }
}

@Composable
private fun KartuPosisiIqro(progress: IqroProgressUi?, hariIni: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(EmasTua, EmasTengah, EmasMuda)))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "IQRO · QIROATI · TILAWATI · UMMI",
            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CoklatRedup, letterSpacing = 0.5.sp
        )
        if (progress != null) {
            Column {
                Text(
                    "Jilid ${progress.jilid} · halaman ${progress.halaman}",
                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CoklatTeks
                )
                Text(
                    when {
                        hariIni && progress.jam.isNotEmpty() -> "Dicatat hari ini pukul ${progress.jam}"
                        hariIni -> "Dicatat hari ini"
                        else -> "Terakhir dicatat ${tanggalPendek(progress.tanggal)}"
                    },
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CoklatRedup
                )
            }
        } else {
            Column {
                Text("Belum ada catatan", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CoklatTeks)
                Text("Pilih jilid dan halaman yang sedang dipelajari di bawah.", fontSize = 13.sp, color = CoklatRedup)
            }
        }
        // Jejak jilid: posisi, bukan persentase.
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            JILID.forEach { n ->
                val sekarang = progress?.jilid
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (n == sekarang) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                sekarang == null -> CoklatTeks.copy(alpha = 0.15f)
                                n < sekarang -> CoklatTeks.copy(alpha = 0.55f)
                                n == sekarang -> CoklatTeks
                                else -> CoklatTeks.copy(alpha = 0.15f)
                            }
                        )
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Jilid ${JILID.first}", fontSize = 11.sp, color = CoklatRedup, modifier = Modifier.weight(1f))
            Text("Jilid ${JILID.last}", fontSize = 11.sp, color = CoklatRedup)
        }
    }
}

@Composable
private fun KartuCatatIqro(
    progress: IqroProgressUi?,
    hariIni: Boolean,
    onSave: (Int, Int) -> Unit
) {
    var jilid by rememberSaveable { mutableIntStateOf(progress?.jilid ?: JILID.first) }
    var halaman by rememberSaveable { mutableIntStateOf(progress?.halaman ?: HALAMAN.first) }

    fun pilihJilid(n: Int) {
        if (n == jilid) return
        jilid = n
        // Jilid posisi terakhir: lanjut dari halamannya; jilid lain mulai dari halaman 1.
        halaman = if (n == progress?.jilid) progress.halaman else HALAMAN.first
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Jilid", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        JILID.chunked(5).forEach { baris ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                baris.forEach { n -> TombolJilid(n, dipilih = n == jilid, onClick = { pilihJilid(n) }, modifier = Modifier.weight(1f)) }
            }
        }

        Text("Sampai halaman", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .border(1.dp, EmasMuda.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(6.dp)
        ) {
            IconButton(onClick = { halaman = (halaman - 1).coerceAtLeast(HALAMAN.first) }, enabled = halaman > HALAMAN.first) {
                Icon(Icons.Rounded.Remove, contentDescription = "Kurangi halaman")
            }
            Spacer(Modifier.width(8.dp))
            var teks by remember(halaman) { mutableStateOf(halaman.toString()) }
            BasicTextField(
                value = teks,
                onValueChange = { v ->
                    teks = v.filter(Char::isDigit).take(3)
                    teks.toIntOrNull()?.let { n -> halaman = n.coerceIn(HALAMAN.first, HALAMAN.last) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = EmasMuda.teks(), textAlign = TextAlign.Center),
                cursorBrush = SolidColor(EmasMuda),
                modifier = Modifier
                    .width(80.dp)
                    .semantics { contentDescription = "Halaman" }
                    .padding(vertical = 6.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { halaman = (halaman + 1).coerceAtMost(HALAMAN.last) }, enabled = halaman < HALAMAN.last) {
                Icon(Icons.Rounded.Add, contentDescription = "Tambah halaman")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            // Pintasan dihitung dari posisi terakhir bila jilidnya sama.
            val dasar = if (progress != null && progress.jilid == jilid) progress.halaman else halaman
            AssistChip(onClick = { halaman = (dasar + 1).coerceAtMost(HALAMAN.last) }, label = { Text("+1 halaman") }, shape = CircleShape)
            AssistChip(onClick = { halaman = (dasar + 2).coerceAtMost(HALAMAN.last) }, label = { Text("+2 halaman") }, shape = CircleShape)
            if (jilid < JILID.last) {
                AssistChip(
                    onClick = { jilid += 1; halaman = HALAMAN.first },
                    label = { Text("Naik ke jilid ${jilid + 1}") },
                    shape = CircleShape
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    ringkasan(progress, jilid, halaman),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C66D).teks()
                )
                if (hariIni) {
                    Text(
                        "Menyimpan lagi akan memperbarui catatan hari ini",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onSave(jilid, halaman) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = EmasMuda, contentColor = CoklatTeks)
            ) { Text(if (hariIni) "Perbarui" else "Simpan", fontWeight = FontWeight.Bold) }
        }
    }
}

private fun ringkasan(progress: IqroProgressUi?, jilid: Int, halaman: Int): String = when {
    progress == null -> "Jilid $jilid · halaman $halaman"
    progress.jilid == jilid && progress.halaman == halaman -> "Sama dengan posisi terakhir"
    progress.jilid == jilid -> "Jilid $jilid · hal. ${progress.halaman} → $halaman"
    else -> "Jilid ${progress.jilid} → jilid $jilid · halaman $halaman"
}

@Composable
private fun TombolJilid(n: Int, dipilih: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(46.dp)
            .clip(CircleShape)
            .then(
                if (dipilih) Modifier.background(EmasMuda)
                else Modifier.background(MaterialTheme.colorScheme.background)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            .semantics { selected = dipilih; contentDescription = "Jilid $n" }
            .clickable(role = Role.RadioButton, onClick = onClick)
    ) {
        Text(
            "$n",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (dipilih) CoklatTeks else MaterialTheme.colorScheme.onSurface
        )
    }
}
