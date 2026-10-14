package com.islami.Aha.ui.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.islami.Aha.R
import com.islami.Aha.ui.consent.ResearchConsentContent
import com.islami.Aha.ui.settings.LegalDocumentActivity
import com.islami.Aha.util.AgeRange
import com.islami.Aha.util.GenderProfile

private const val HAL_SELAMAT_DATANG = 0
private const val HAL_FITUR = 1
private const val HAL_PROFIL = 2
private const val HAL_USIA = 3
private const val HAL_PERSETUJUAN = 4
private const val HAL_LOKASI = 5
private const val HAL_NOTIFIKASI = 6
private const val JUMLAH_HAL = 7

private val PinkCuti = Color(0xFFF48FB1)
private val Emas = Color(0xFFE2B65A)

/**
 * Onboarding: pengenalan → profil ibadah (permanen) → rentang usia → persetujuan penelitian
 * → izin lokasi → izin notifikasi. Halaman tidak bisa digeser; berpindah hanya lewat tombol,
 * supaya setiap keputusan diambil dengan tindakan aktif.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var halaman by rememberSaveable { mutableIntStateOf(HAL_SELAMAT_DATANG) }
    val butuhIzinNotifikasi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun lanjut() {
        val berikut = halaman + 1
        if (berikut >= JUMLAH_HAL || (berikut == HAL_NOTIFIKASI && !butuhIzinNotifikasi)) onFinish() else halaman = berikut
    }

    val izinLokasi = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { lanjut() }
    val izinNotifikasi = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { lanjut() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when (halaman) {
            HAL_SELAMAT_DATANG -> SelamatDatang(onMulai = { lanjut() }, onMasuk = onNavigateToLogin)
            HAL_FITUR -> Kerangka(halaman, aksiKanan = "Lewati" to { halaman = HAL_PROFIL }) {
                PengenalanFitur(onLanjut = { lanjut() })
            }
            HAL_PROFIL -> Kerangka(halaman) {
                PilihProfil(
                    onSimpan = { profil -> viewModel.simpanProfil(profil); lanjut() },
                    onNanti = { lanjut() }
                )
            }
            HAL_USIA -> Kerangka(halaman) {
                PilihUsia(onLanjut = { usia, izinWali -> viewModel.simpanUsia(usia, izinWali); lanjut() })
            }
            HAL_PERSETUJUAN -> Kerangka(halaman) {
                ResearchConsentContent(
                    onAccept = { viewModel.simpanPersetujuan(true); lanjut() },
                    onDecline = { viewModel.simpanPersetujuan(false); lanjut() },
                    onOpenPrivacyPolicy = {
                        context.startActivity(
                            Intent(context, LegalDocumentActivity::class.java)
                                .putExtra(LegalDocumentActivity.EXTRA_DOCUMENT_TYPE, LegalDocumentActivity.DOC_PRIVACY)
                        )
                    }
                )
            }
            HAL_LOKASI -> Kerangka(halaman) {
                LayarIzin(
                    icon = Icons.Rounded.LocationOn,
                    warnaIcon = MaterialTheme.colorScheme.primary,
                    judul = "Jadwal sholat yang tepat\nuntuk kotamu",
                    isi = "Aha memakai lokasi HP hanya untuk menghitung waktu sholat. Lokasimu tidak disimpan di server dan tidak dipakai untuk penelitian.",
                    petunjuk = "Setelah ini Android akan menanyakan izin. Pilih “Saat aplikasi digunakan”.",
                    tombol = "Izinkan lokasi",
                    tombolNanti = "Nanti saja, pakai jadwal default",
                    onIzinkan = {
                        izinLokasi.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    onNanti = { lanjut() }
                )
            }
            HAL_NOTIFIKASI -> Kerangka(halaman) {
                LayarIzin(
                    icon = Icons.Rounded.NotificationsActive,
                    warnaIcon = Emas,
                    judul = "Pengingat yang\ntidak mengganggu",
                    isi = "Aha mengingatkan waktu sholat dan ibadah sunnah yang kamu pilih. Kamu bisa mematikan pengingat mana pun kapan saja.",
                    petunjuk = null,
                    tombol = "Aktifkan pengingat",
                    tombolNanti = "Nanti saja",
                    onIzinkan = {
                        if (butuhIzinNotifikasi) izinNotifikasi.launch(Manifest.permission.POST_NOTIFICATIONS) else lanjut()
                    },
                    onNanti = { lanjut() }
                )
            }
        }
    }
}

/** Indikator halaman + tombol kanan atas opsional, lalu isi halaman. */
@Composable
private fun Kerangka(
    halaman: Int,
    aksiKanan: Pair<String, () -> Unit>? = null,
    isi: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 24.dp, end = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                repeat(JUMLAH_HAL) { i ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (i == halaman) 24.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (i <= halaman) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
            if (aksiKanan != null) {
                TextButton(onClick = aksiKanan.second) {
                    Text(aksiKanan.first, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        isi()
    }
}

@Composable
private fun TombolUtama(teks: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) { Text(teks, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun TombolKecil(teks: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)) {
        Text(teks, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SelamatDatang(onMulai: () -> Unit, onMasuk: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "Logo Aha",
                    modifier = Modifier.size(84.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "AHA · JURNAL IBADAH MUSLIM",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = Emas
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Catat ibadahmu,\npelan-pelan tapi konsisten",
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Sholat, puasa, dzikir, dan tilawah dalam satu tempat. Tanpa skor, tanpa peringkat — hanya catatanmu sendiri.",
                fontSize = 16.sp,
                lineHeight = 25.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TombolUtama("Mulai", onMulai)
        TombolKecil("Sudah punya akun? Masuk", onMasuk)
    }
}

@Composable
private fun ColumnScope.PengenalanFitur(onLanjut: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Semua ibadah harian,\nsatu jurnal", fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Catat yang kamu kerjakan. Yang belum dicatat tidak pernah dihitung gagal.",
            fontSize = 15.sp,
            lineHeight = 23.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        KartuFitur(Icons.Rounded.Mosque, MaterialTheme.colorScheme.primary, "Sholat & puasa",
            "Jadwal sholat sesuai lokasimu, sholat sunnah, puasa Ramadan dan sunnah.")
        KartuFitur(Icons.AutoMirrored.Rounded.MenuBook, Emas, "Dzikir, tilawah & Iqro",
            "Pilih dzikir yang biasa kamu amalkan, catat rentang ayat yang dibaca, atau jilid Iqro terakhir.")
        KartuFitur(Icons.Rounded.BarChart, PinkCuti, "Statistik tanpa penilaian",
            "Lihat kebiasaanmu dari waktu ke waktu. Saat Mode Cuti, ibadah yang tidak wajib tidak dihitung.")
    }
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        TombolUtama("Lanjut", onLanjut)
    }
}

@Composable
private fun KartuFitur(icon: ImageVector, warna: Color, judul: String, isi: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(18.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(warna.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = warna, modifier = Modifier.size(26.dp)) }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(judul, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(isi, fontSize = 14.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ColumnScope.PilihProfil(onSimpan: (GenderProfile) -> Unit, onNanti: () -> Unit) {
    var pilihan by rememberSaveable { mutableStateOf<GenderProfile?>(null) }
    var konfirmasi by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Profil ibadahmu", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Dipakai untuk menyesuaikan jadwal: Mode Cuti untuk perempuan, pengingat Sholat Jumat untuk laki-laki.",
            fontSize = 15.sp,
            lineHeight = 23.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // Ikon yang sama dengan dialog Profil Ibadah di Pengaturan.
            KartuProfil(R.drawable.ic_gender_male, "Laki-laki", pilihan == GenderProfile.MALE,
                Modifier.weight(1f)) { pilihan = GenderProfile.MALE }
            KartuProfil(R.drawable.ic_gender_female, "Perempuan", pilihan == GenderProfile.FEMALE,
                Modifier.weight(1f)) { pilihan = GenderProfile.FEMALE }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Emas.copy(alpha = 0.14f))
                .padding(16.dp)
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Emas, modifier = Modifier.size(22.dp))
            Text(
                "Profil tidak bisa diubah setelah disimpan. Pastikan pilihanmu benar.",
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
    }
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        TombolUtama("Simpan profil", onClick = { konfirmasi = true }, enabled = pilihan != null)
        TombolKecil("Atur nanti", onNanti)
    }
    val terpilih = pilihan
    if (konfirmasi && terpilih != null) {
        val nama = if (terpilih == GenderProfile.MALE) "laki-laki" else "perempuan"
        AlertDialog(
            onDismissRequest = { konfirmasi = false },
            title = { Text("Simpan sebagai $nama?") },
            text = { Text("Profil ibadah tidak bisa diubah lagi setelah disimpan.") },
            confirmButton = { TextButton(onClick = { konfirmasi = false; onSimpan(terpilih) }) { Text("Ya, simpan") } },
            dismissButton = { TextButton(onClick = { konfirmasi = false }) { Text("Periksa lagi") } }
        )
    }
}

@Composable
private fun KartuProfil(
    iconRes: Int,
    label: String,
    dipilih: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val border = if (dipilih) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        modifier = modifier
            .height(168.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (dipilih) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(BorderStroke(2.dp, border), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
        )
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (dipilih) Text("Dipilih", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ColumnScope.PilihUsia(onLanjut: (AgeRange, Boolean) -> Unit) {
    var pilihan by rememberSaveable { mutableStateOf<AgeRange?>(null) }
    var izinWali by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Berapa usiamu?", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Cukup rentangnya saja. Kami tidak meminta tanggal lahir.",
            fontSize = 15.sp,
            lineHeight = 23.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        AgeRange.entries.forEach { usia ->
            val dipilih = pilihan == usia
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (dipilih) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { pilihan = usia }
                    .padding(start = 20.dp, end = 8.dp)
            ) {
                Text(usia.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                RadioButton(selected = dipilih, onClick = { pilihan = usia })
            }
        }
        if (pilihan?.isMinor == true) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { izinWali = !izinWali }
                    .padding(8.dp)
            ) {
                Checkbox(checked = izinWali, onCheckedChange = { izinWali = it })
                Text(
                    "Orang tua atau waliku sudah mengizinkan aku memakai aplikasi ini dan ikut penelitiannya.",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 12.dp, end = 8.dp)
                )
            }
        }
    }
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        TombolUtama("Lanjut", onClick = { pilihan?.let { onLanjut(it, izinWali) } }, enabled = pilihan != null)
    }
}

@Composable
private fun ColumnScope.LayarIzin(
    icon: ImageVector,
    warnaIcon: Color,
    judul: String,
    isi: String,
    petunjuk: String?,
    tombol: String,
    tombolNanti: String,
    onIzinkan: () -> Unit,
    onNanti: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(warnaIcon.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = warnaIcon, modifier = Modifier.size(56.dp)) }
        Spacer(Modifier.height(28.dp))
        Text(judul, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(isi, fontSize = 15.sp, lineHeight = 23.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (petunjuk != null) {
            Spacer(Modifier.height(20.dp))
            Text(petunjuk, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        TombolUtama(tombol, onIzinkan)
        TombolKecil(tombolNanti, onNanti)
    }
}
