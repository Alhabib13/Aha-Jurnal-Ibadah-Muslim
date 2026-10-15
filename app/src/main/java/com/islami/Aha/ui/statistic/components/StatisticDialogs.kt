package com.islami.Aha.ui.statistic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.ui.statistic.PastDayHabitItem
import com.islami.Aha.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Hijau = Color(0xFF14A87D)
private val HijauAksen = Color(0xFF2ECC9B)
private val TeksDiHijau = Color(0xFF062B1F)
private val Ungu = Color(0xFFA48BF0)
private val MerahMuda = Color(0xFFFFC1D6)

/** Lembar catat susulan untuk hari lampau: sholat fardhu, lalu sunnah & puasa. */
@Composable
fun PastDayHabitsList(
    dateKey: String,
    habits: List<PastDayHabitItem>,
    onToggle: (PastDayHabitItem) -> Unit,
    onDismiss: () -> Unit,
    cuti: Boolean = false
) {
    val tanggal = runCatching { LocalDate.parse(dateKey) }.getOrNull()
    val judulTanggal = tanggal?.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID"))) ?: dateKey
    val hijri = tanggal?.let(DateUtils::hijriFormatted).orEmpty()
    val fardhu = habits.filter { it.category == "Sholat Fardhu" }
    val lain = habits.filterNot { it.category == "Sholat Fardhu" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Text("Catat susulan", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text(
            listOf(judulTanggal, hijri).filter { it.isNotEmpty() }.joinToString(" · "),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (cuti) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Mode Cuti di tanggal ini: sholat dan puasa tidak perlu dicatat.",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MerahMuda,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MerahMuda.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (habits.isEmpty()) {
                Text(
                    if (cuti) "Tidak ada yang perlu dicatat." else "Belum ada sholat atau puasa di daftarmu.",
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            if (fardhu.isNotEmpty()) {
                JudulBagian("Sholat fardhu", "${fardhu.count { it.isCompleted }} dari ${fardhu.size}", Hijau)
                fardhu.forEach { BarisCatat(it, Hijau, onToggle) }
            }
            if (lain.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                JudulBagian("Sunnah & puasa", null, Ungu)
                lain.forEach { BarisCatat(it, Ungu, onToggle) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Disimpan sebagai catatan susulan untuk tanggal ini.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onDismiss,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = HijauAksen, contentColor = TeksDiHijau),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("Selesai", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
    }
}

@Composable
private fun JudulBagian(judul: String, ringkas: String?, warna: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)) {
        Text(judul.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = warna, letterSpacing = 0.5.sp, modifier = Modifier.weight(1f))
        if (ringkas != null) Text(ringkas, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BarisCatat(item: PastDayHabitItem, warna: Color, onToggle: (PastDayHabitItem) -> Unit) {
    val bentuk = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(bentuk)
            .background(MaterialTheme.colorScheme.background)
            .then(if (item.isCompleted) Modifier.border(1.dp, warna.copy(alpha = 0.5f), bentuk) else Modifier)
            .toggleable(value = item.isCompleted, role = Role.Checkbox) { onToggle(item) }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .then(
                    if (item.isCompleted) Modifier.background(warna)
                    else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
        ) {
            if (item.isCompleted) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            item.name.removePrefix("Sholat ").ifBlank { item.name },
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            if (item.isCompleted) "Dicatat" else "Belum dicatat",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (item.isCompleted) warna else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
