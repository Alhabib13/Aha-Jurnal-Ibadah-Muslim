package com.islami.Aha.ui.statistic

import com.islami.Aha.ui.theme.temaGelap

import com.islami.Aha.ui.theme.teks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.islami.Aha.ui.statistic.components.PastDayHabitsList

private val HijauTua = Color(0xFF0B6B52)
private val HijauTengah = Color(0xFF0D7C5F)
private val HijauMuda = Color(0xFF17A27B)
private val HijauAksen = Color(0xFF2ECC9B)
private val HijauSedikit = Color(0xFF0F4D3D)
private val HijauBanyak = Color(0xFF14A87D)
private val Emas = Color(0xFFF2C66D)
private val EmasTilawah = Color(0xFFEDB84E)
private val Toska = Color(0xFF4FD1C5)
private val Ungu = Color(0xFFA48BF0)
private val MerahMuda = Color(0xFFFFC1D6)
private val HARI = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticScreen(
    viewModel: StatisticViewModel = hiltViewModel(),
    onNavigateToAddHabit: () -> Unit = {},
    onNavigateToAddDzikir: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPastDate by viewModel.selectedPastDate.collectAsStateWithLifecycle()
    val pastDayHabits by viewModel.pastDayHabits.collectAsStateWithLifecycle()

    StatisticScreenContent(
        uiState = uiState,
        onPilihTanggal = viewModel::pilihTanggal,
        onBulanSebelumnya = viewModel::bulanSebelumnya,
        onBulanBerikutnya = viewModel::bulanBerikutnya,
        onUbahCatatan = { viewModel.onDayClicked(uiState.tanggalDipilih) },
        onNavigateToAddHabit = onNavigateToAddHabit,
        onNavigateToAddDzikir = onNavigateToAddDzikir
    )

    if (selectedPastDate != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissPastDayPopup,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            PastDayHabitsList(
                dateKey = selectedPastDate!!,
                habits = pastDayHabits,
                cuti = uiState.tanggalDipilihCuti,
                onToggle = viewModel::togglePastDayHabitCompletion,
                onDismiss = viewModel::dismissPastDayPopup
            )
        }
    }
}

@Composable
fun StatisticScreenContent(
    uiState: StatisticUiState,
    onPilihTanggal: (String) -> Unit = {},
    onBulanSebelumnya: () -> Unit = {},
    onBulanBerikutnya: () -> Unit = {},
    onUbahCatatan: () -> Unit = {},
    onNavigateToAddHabit: () -> Unit = {},
    onNavigateToAddDzikir: () -> Unit = {}
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Statistik", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                Text(uiState.judulTanggal, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { KartuHariIni(uiState) }
        item { KartuKalender(uiState, onPilihTanggal, onBulanSebelumnya, onBulanBerikutnya) }
        item { KartuDetailHari(uiState, onUbahCatatan) }
        item {
            Text("Per ibadah, 7 hari terakhir", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
        item { KisiPerIbadah(uiState, onNavigateToAddHabit, onNavigateToAddDzikir) }
        item { KartuKhatam(uiState) }
    }
}

@Composable
private fun Kartu(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) { content() }
}

@Composable
private fun KartuHariIni(s: StatisticUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(HijauTua, HijauTengah, HijauMuda)))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("HARI INI", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFBFEBDB), letterSpacing = 0.5.sp)
                Text(
                    when {
                        s.todayTotal > 0 -> "${s.todayCompleted} dari ${s.todayTotal} dicatat"
                        s.modeCuti -> "Mode Cuti aktif"
                        else -> "Belum ada ibadah harian"
                    },
                    fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                )
            }
            if (s.currentStreak > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(CircleShape).background(Emas.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = Emas, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${s.currentStreak} hari berturut", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emas)
                }
            }
        }
        if (s.todayTotal in 1..12) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(s.todayTotal) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(if (i < s.todayCompleted) Color.White else Color.White.copy(alpha = 0.22f))
                    )
                }
            }
        } else if (s.todayTotal > 12) {
            LinearProgressIndicator(
                progress = { s.todayCompleted.toFloat() / s.todayTotal },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.22f),
                drawStopIndicator = {}
            )
        }
        Text(
            when {
                s.rincianHariIni.isNotEmpty() -> s.rincianHariIni
                s.modeCuti -> "Sholat dan puasa tidak dihitung selama Mode Cuti"
                else -> "Tambahkan ibadah dari tombol + di bawah"
            },
            fontSize = 12.sp, color = Color(0xFFD4F3E8)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KartuKalender(
    s: StatisticUiState,
    onPilihTanggal: (String) -> Unit,
    onSebelumnya: () -> Unit,
    onBerikutnya: () -> Unit
) {
    Kartu {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onSebelumnya,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.background)
            ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.labelBulan, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (s.labelHijriBulan.isNotEmpty()) {
                    Text(s.labelHijriBulan, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(
                onClick = onBerikutnya,
                enabled = s.bisaMaju,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.background)
            ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Bulan berikutnya") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HARI.forEach {
                Text(
                    it, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        s.kalender.chunked(7).forEach { minggu ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until 7).forEach { i ->
                    val hari = minggu.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (hari != null) SelTanggal(hari, dipilih = hari.tanggal == s.tanggalDipilih, onClick = { onPilihTanggal(hari.tanggal) })
                    }
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Legenda(warnaSedikit(), "Sedikit")
            Legenda(HijauBanyak, "Banyak")
            Legenda(null, "Mode Cuti")
            Text("Ketuk tanggal untuk melihat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Legenda(warna: Color?, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .then(if (warna != null) Modifier.clip(RoundedCornerShape(3.dp)).background(warna) else Modifier.garisPutus(3.dp))
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Bingkai putus-putus merah muda untuk hari Mode Cuti. */
private fun Modifier.garisPutus(radius: androidx.compose.ui.unit.Dp) = drawBehind {
    drawRoundRect(
        color = MerahMuda,
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
    )
}

/** "Sedikit" di kalender: hijau tua di tema gelap, hijau muda di tema terang. */
@Composable
private fun warnaSedikit(): Color = if (temaGelap()) HijauSedikit else Color(0xFFBDEBDB)

@Composable
private fun SelTanggal(h: HariKalender, dipilih: Boolean, onClick: () -> Unit) {
    val bentuk = RoundedCornerShape(10.dp)
    val latar = when {
        h.nanti -> Color.Transparent
        h.cuti && h.intensitas == Intensitas.KOSONG -> MerahMuda.copy(alpha = 0.16f)
        h.intensitas == Intensitas.BANYAK -> HijauBanyak
        h.intensitas == Intensitas.SEDIKIT -> warnaSedikit()
        else -> MaterialTheme.colorScheme.background
    }
    val warnaTeks = when {
        h.nanti -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        h.hariIni && h.intensitas == Intensitas.KOSONG -> Emas.teks()
        // Tema terang: emas tidak terbaca di atas hijau, jadi angka ikut warna isian; bingkai emas tetap penanda hari ini.
        h.hariIni && temaGelap() -> Emas
        h.intensitas == Intensitas.BANYAK -> Color.White
        h.intensitas == Intensitas.SEDIKIT -> if (temaGelap()) Color.White else Color(0xFF0B5A44)
        h.cuti -> MerahMuda.teks()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val keterangan = buildString {
        append("Tanggal ${h.hari}")
        if (h.jumlah > 0) append(", ${h.jumlah} catatan") else if (!h.nanti) append(", belum ada catatan")
        if (h.cuti) append(", Mode Cuti")
        if (h.hariIni) append(", hari ini")
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .clip(bentuk)
            .background(latar)
            .then(if (h.cuti) Modifier.garisPutus(10.dp) else Modifier)
            .then(
                when {
                    dipilih -> Modifier.border(2.dp, if (h.hariIni) Emas.teks() else MaterialTheme.colorScheme.onSurface, bentuk)
                    h.hariIni -> Modifier.border(2.dp, Emas.teks(), bentuk)
                    else -> Modifier
                }
            )
            .then(if (h.nanti) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick))
            .semantics { contentDescription = keterangan }
    ) {
        Text(
            "${h.hari}",
            fontSize = 13.sp,
            fontWeight = if (h.hariIni || h.intensitas == Intensitas.BANYAK) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = warnaTeks
        )
        if (h.hijri.isNotEmpty()) {
            Text(
                h.hijri,
                fontSize = 9.sp,
                color = warnaTeks.copy(alpha = 0.75f),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 5.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KartuDetailHari(s: StatisticUiState, onUbahCatatan: () -> Unit) {
    Kartu {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (s.tanggalDipilihHariIni) "Hari ini" else s.judulTanggalDipilih,
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                when {
                    s.chipsDipilih.isNotEmpty() -> "${s.chipsDipilih.size} catatan"
                    s.tanggalDipilihCuti -> "Mode Cuti"
                    else -> "Belum ada catatan"
                },
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (s.chipsDipilih.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                s.chipsDipilih.forEach { ChipDetail(it) }
            }
        }
        if (s.tanggalDipilihHariIni) {
            Text("Catatan hari ini diisi dari Beranda.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            OutlinedButton(onClick = onUbahCatatan, shape = CircleShape) {
                Text("Catat atau ubah sholat & puasa", fontWeight = FontWeight.SemiBold, color = HijauAksen.teks())
            }
        }
    }
}

@Composable
private fun ChipDetail(c: ChipCatatan) {
    val warna = when (c.jenis) {
        JenisChip.SHOLAT -> HijauAksen
        JenisChip.SUNNAH_PUASA -> Ungu
        JenisChip.DZIKIR -> Toska
        JenisChip.TILAWAH, JenisChip.IQRO -> Emas
    }
    Text(
        c.label,
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = warna.teks(),
        modifier = Modifier.clip(CircleShape).background(warna.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun KisiPerIbadah(s: StatisticUiState, onAddHabit: () -> Unit, onAddDzikir: () -> Unit) {
    val p = s.pekan
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KotakIbadah(
                judul = "Sholat fardhu", warna = HijauAksen, angka = p.sholatFardhu, satuan = "kali",
                keterangan = p.sholatTersering?.let { "Paling sering dicatat: $it" } ?: "Dari 7 hari terakhir",
                kosong = "Belum dicatat", modifier = Modifier.weight(1f)
            )
            KotakIbadah(
                judul = "Dzikir", warna = Toska, angka = p.dzikirSesi, satuan = "sesi",
                keterangan = p.dzikirTersering?.let { "Paling sering: $it" } ?: "Dari 7 hari terakhir",
                kosong = if (s.punyaDzikir) "Belum dicatat" else "Belum ada",
                aksi = if (!s.punyaDzikir) "Tambahkan" to onAddDzikir else null,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KotakIbadah(
                judul = "Tilawah", warna = EmasTilawah, angka = p.tilawahHalaman, satuan = "halaman",
                keterangan = "${p.tilawahHari} hari ada bacaan",
                kosong = "Belum dicatat", modifier = Modifier.weight(1f)
            )
            KotakIbadah(
                judul = "Sunnah & puasa", warna = Ungu, angka = p.sunnahPuasa, satuan = "kali",
                keterangan = "Dari 7 hari terakhir",
                kosong = if (s.punyaSunnah) "Belum dicatat" else "Belum ada",
                aksi = if (!s.punyaSunnah) "Tambahkan" to onAddHabit else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KotakIbadah(
    judul: String,
    warna: Color,
    angka: Int,
    satuan: String,
    keterangan: String,
    kosong: String,
    modifier: Modifier = Modifier,
    aksi: Pair<String, () -> Unit>? = null
) {
    Column(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(judul, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = warna.teks())
        if (angka > 0) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$angka", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(4.dp))
                Text(satuan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
            }
            Text(keterangan, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        } else {
            Text(kosong, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            if (aksi != null) {
                Text(
                    aksi.first,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HijauAksen.teks(),
                    modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = aksi.second).padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun KartuKhatam(s: StatisticUiState) {
    Kartu {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Perjalanan khatam", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (s.khatamJuz != null) {
                Text("Juz ${s.khatamJuz} dari 30", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emas.teks())
            }
        }
        (1..30).chunked(15).forEach { baris ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                baris.forEach { juz ->
                    val j = s.khatamJuz
                    Box(
                        Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    j == null -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    juz < j -> EmasTilawah
                                    juz == j -> EmasTilawah.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                }
                            )
                    )
                }
            }
        }
        Text(s.khatamKeterangan, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
