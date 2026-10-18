package com.islami.Aha.ui.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Persetujuan penelitian berlapis (Google Play Prominent Disclosure & Consent):
 * lapis 1 = 4 poin singkat, lapis 2 = penjelasan lengkap, lapis 3 = kebijakan privasi.
 * Setuju/Tidak = tindakan aktif; menutup layar tidak dianggap setuju. Menolak tidak
 * membatasi fitur apa pun — catatan tetap tersimpan di HP, hanya tidak diunggah.
 */
@Composable
fun ResearchConsentContent(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lengkap by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        if (lengkap) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = { lengkap = false }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                }
                Text("Penjelasan lengkap", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Bagian(
                    "Apa yang dicatat",
                    "• Ibadah yang kamu catat (mis. sholat Subuh, dzikir pagi) dan statusnya.\n" +
                        "• Tanggal ibadah, jam kamu mencatat, dan zona waktu HP.\n" +
                        "• Tilawah: jumlah ayat, halaman, dan juz. Iqro: jilid dan halaman.\n" +
                        "• Dzikir: cara mencatat dan target hitungan bila kamu mengaturnya.\n" +
                        "• Selisih menit antara jam mencatat dan waktu adzan, dihitung di HP.\n" +
                        "• Profil ibadah (laki-laki/perempuan), rentang usia, kesibukan utama, dan ibadah yang kamu centang sebagai kebiasaan di awal."
                )
                Bagian(
                    "Apa yang tidak dicatat",
                    "• Lokasimu. Lokasi hanya dipakai untuk menghitung jadwal sholat dan tidak dikirim untuk penelitian.\n" +
                        "• Aktivitas di luar aplikasi Aha."
                )
                Bagian(
                    "Untuk apa dan oleh siapa",
                    "Untuk penelitian skripsi Alhabib Husein Alaydrus tentang pola kebiasaan ibadah, " +
                        "agar pengingat bisa lebih sesuai. Hasil penelitian hanya disajikan dalam bentuk gabungan, " +
                        "tanpa nama atau email."
                )
                Bagian(
                    "Di mana disimpan",
                    "Di Google Firebase. Data baru dikirim bila kamu masuk ke akun; dalam mode tamu catatan " +
                        "hanya tersimpan di HP. Bila kamu setuju, catatan yang sudah ada di HP ikut dikirim."
                )
                Bagian(
                    "Hakmu",
                    "• Berhenti ikut kapan saja di Pengaturan › Privasi & Data.\n" +
                        "• Menghapus data penelitian yang sudah terkirim, tanpa menghapus akun.\n" +
                        "• Menghapus akun beserta seluruh datanya."
                )
                TextButton(onClick = onOpenPrivacyPolicy) { Text("Baca kebijakan privasi") }
                Spacer(Modifier.height(8.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Bantu penelitian Aha", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp)
                Text(
                    "Aha dikembangkan untuk penelitian skripsi. Catatan ibadahmu bisa ikut membantu — kalau kamu setuju.",
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Poin(Icons.Rounded.Checklist, "Yang dicatat:", " jenis ibadah, tanggal, dan jam kamu mencatat")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Poin(Icons.Rounded.Insights, "Untuk:", " penelitian skripsi agar pengingat lebih pas dengan kebiasaanmu")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Poin(Icons.Rounded.Shield, "Tidak dijual", " dan tidak dipakai untuk iklan")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Poin(Icons.Rounded.Settings, "Bisa berhenti kapan saja", " di Pengaturan › Privasi & Data")
                }
                TextButton(onClick = { lengkap = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Baca penjelasan lengkap", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Aplikasi tetap bisa dipakai penuh walaupun kamu tidak ikut.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAccept,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) { Text("Setuju & lanjut", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            TextButton(onClick = onDecline, modifier = Modifier.height(48.dp)) {
                Text("Tidak, terima kasih", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Poin(icon: ImageVector, tebal: String, sisa: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(
            androidx.compose.ui.text.buildAnnotatedString {
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                append(tebal)
                pop()
                append(sisa)
            },
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun Bagian(judul: String, isi: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(judul, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(isi, fontSize = 14.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Layar penuh untuk pengguna yang belum memutuskan (mis. pembaruan dari v1.1.1). Tidak bisa ditutup tanpa memilih. */
@Composable
fun ResearchConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            ResearchConsentContent(onAccept = onAccept, onDecline = onDecline, onOpenPrivacyPolicy = onOpenPrivacyPolicy)
        }
    }
}
