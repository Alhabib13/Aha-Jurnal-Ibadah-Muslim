package com.islami.Aha.ui.home.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.R
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import com.islami.Aha.ui.theme.teks
import com.islami.Aha.ui.theme.Emerald

@Composable
fun HaidhEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_flower), // Using flower icon
            contentDescription = "Cuti Ibadah",
            modifier = Modifier.size(80.dp),
            colorFilter = ColorFilter.tint(Color(0xFFF48FB1))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_haidh_mode_active),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_haidh_mode_desc),
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun ComingSoonState(categoryName: String, customMessage: String? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.HourglassTop,
                contentDescription = stringResource(R.string.home_coming_soon_cd),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.home_coming_soon_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = customMessage ?: stringResource(R.string.home_coming_soon_desc_format, categoryName),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyHabitState(onAddHabitClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.home_tutorial_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.home_tutorial_subtitle),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            TutorialStepItem(
                icon = androidx.compose.material.icons.Icons.Default.Add,
                text = stringResource(R.string.home_tutorial_step1)
            )
            TutorialStepItem(
                icon = androidx.compose.material.icons.Icons.Default.Check,
                text = stringResource(R.string.home_tutorial_step2)
            )
            TutorialStepItem(
                icon = androidx.compose.material.icons.Icons.Default.List,
                text = stringResource(R.string.home_tutorial_step3)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAddHabitClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.home_add_new_habit_button),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun TutorialStepItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp).padding(top = 2.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun IslamicMotivationCard(quote: String, source: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.home_quote_format, quote),
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                lineHeight = 22.sp
            )
            Text(
                text = source,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Kartu ajakan saat sebuah tab masih kosong: ikon berwarna kategori, judul, penjelasan singkat,
 * dan (bila ada) satu tombol aksi. Tanpa angka progres.
 */
@Composable
fun AjakanKosong(
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    warna: Color,
    judul: String,
    isi: String,
    catatan: String? = null,
    tombol: String? = null,
    onTombol: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(warna.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(ikon, contentDescription = null, tint = warna.teks(), modifier = Modifier.size(28.dp)) }
            Spacer(Modifier.height(14.dp))
            Text(judul, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                isi,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (catatan != null) {
                Spacer(Modifier.height(10.dp))
                Text(catatan, fontSize = 12.sp, color = warna.teks(), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
            if (tombol != null) {
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onTombol,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = warna, contentColor = Color(0xFF0B1F18)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(tombol, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Tab Sholat sunnah / Puasa sunnah yang belum berisi ibadah. */
@Composable
fun TabKosong(kategori: String, onTambah: () -> Unit) {
    when (kategori) {
        "Sholat" -> AjakanKosong(
            ikon = Icons.Rounded.Mosque,
            warna = Color(0xFF2ECC9B),
            judul = "Belum ada sholat sunnah",
            isi = "Tambahkan sholat sunnah yang ingin kamu jaga, seperti Dhuha, Rawatib, atau Tahajud. Setelah itu cukup centang setiap kali selesai.",
            tombol = "Tambah sholat sunnah",
            onTombol = onTambah
        )
        "Puasa" -> AjakanKosong(
            ikon = Icons.Rounded.NightlightRound,
            warna = Color(0xFFA48BF0),
            judul = "Belum ada puasa sunnah",
            isi = "Tambahkan puasa sunnah yang ingin kamu jaga, seperti Senin–Kamis atau Ayyamul Bidh, lalu catat setiap kali kamu berpuasa.",
            tombol = "Tambah puasa sunnah",
            onTombol = onTambah
        )
        else -> EmptyHabitState(onAddHabitClick = onTambah)
    }
}

/** Tab Puasa Wajib di luar bulan Ramadhan: kapan kartu puasa Ramadan akan muncul. */
@Composable
fun MenungguRamadan() {
    val awal = androidx.compose.runtime.remember { com.islami.Aha.util.DateUtils.awalRamadanBerikutnya() }
    val catatan = awal?.let { (tanggal, tahun) ->
        val hari = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), tanggal)
        val tgl = tanggal.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("id", "ID")))
        "1 Ramadhan $tahun H ≈ $tgl · $hari hari lagi"
    }
    AjakanKosong(
        ikon = Icons.Rounded.NightlightRound,
        warna = Color(0xFFA48BF0),
        judul = "Puasa Ramadan",
        isi = "Pencatatan puasa Ramadan akan muncul dengan sendirinya saat bulan Ramadhan tiba. Sambil menunggu, puasa sunnah bisa kamu catat di tab Sunnah.",
        catatan = catatan?.let { "$it\nPerkiraan; mengikuti penetapan awal Ramadhan." }
    )
}
