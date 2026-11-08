package com.islami.Aha.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.R
import com.islami.Aha.ui.home.JadwalWaktu
import com.islami.Aha.util.GenderProfile

private val HijauGelap = Color(0xFF0B6B52)
private val HijauTengah = Color(0xFF0D7C5F)
private val HijauTerang = Color(0xFF17A27B)
private val Emas = Color(0xFFF2C66D)
private val TeksLembut = Color(0xFFD4F3E8)
private val TeksRedup = Color(0xFFBFEBDB)
private val PanelGelap = Color(0x6B062B1F)
private val PinkLembut = Color(0xFFFFE0EB)
private val PinkTua = Color(0xFF8A2350)
private val PinkSakelar = Color(0xFFE25A8E)

@Composable
fun HomeHeader(
    isLoggedIn: Boolean,
    userName: String,
    gregorianDate: String,
    hijriDate: String,
    location: String,
    nextPrayerName: String,
    nextPrayerJam: String,
    hitungMundur: String,
    nextPrayerProgress: Float,
    jadwal: List<JadwalWaktu>,
    prayerTimeStatusText: String = "",
    showRamadanSchedule: Boolean = false,
    ramadanImsakTime: String = "",
    ramadanIftarTime: String = "",
    ramadanStatusText: String = "",
    isLocationLoading: Boolean = false,
    onRefreshLocation: () -> Unit = {},
    isHaidhMode: Boolean = false,
    genderProfile: GenderProfile = GenderProfile.UNSPECIFIED,
    onToggleHaidhMode: (Boolean) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.linearGradient(listOf(HijauGelap, HijauTengah, HijauTerang)))
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sapaan + Mode Cuti (khusus profil perempuan)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Assalamu'alaikum,", fontSize = 13.sp, color = TeksLembut)
                    Text(
                        if (isLoggedIn && userName.isNotBlank()) userName else "Sahabat Aha",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (genderProfile == GenderProfile.FEMALE) {
                    TombolModeCuti(aktif = isHaidhMode, onToggle = { onToggleHaidhMode(!isHaidhMode) })
                }
            }

            // Tanggal + lokasi
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$gregorianDate · $hijriDate", fontSize = 12.sp, color = TeksLembut)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(enabled = !isLocationLoading, role = Role.Button, onClick = onRefreshLocation)
                        .heightIn(min = 36.dp)
                        .padding(horizontal = 12.dp)
                        .semantics { contentDescription = "Lokasi: $location. Ketuk untuk memperbarui" }
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        location,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    if (isLocationLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = TeksRedup)
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.home_refresh_location_cd),
                            tint = TeksRedup,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            if (isHaidhMode) {
                KartuModeCuti(jadwal)
            } else if (nextPrayerName.isNotEmpty()) {
                KartuWaktuSholat(nextPrayerName, nextPrayerJam, hitungMundur, nextPrayerProgress, jadwal, prayerTimeStatusText)
            }

            if (showRamadanSchedule) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(PanelGelap)
                        .padding(14.dp)
                ) {
                    Text(
                        stringResource(R.string.home_ramadan_schedule_summary, ramadanImsakTime, ramadanIftarTime),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (ramadanStatusText.isNotBlank()) {
                        Text(ramadanStatusText, fontSize = 12.sp, color = TeksRedup)
                    }
                }
            }
        }
    }
}

@Composable
private fun TombolModeCuti(aktif: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (aktif) PinkLembut else Color.Transparent)
            .border(1.dp, if (aktif) Color.Transparent else Color.White.copy(alpha = 0.4f), CircleShape)
            .clickable(role = Role.Switch, onClick = onToggle)
            .heightIn(min = 40.dp)
            .padding(start = 12.dp, end = 6.dp)
            .semantics { stateDescription = if (aktif) "Aktif" else "Nonaktif" }
    ) {
        Text(
            stringResource(R.string.home_haidh_mode_label),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (aktif) PinkTua else Color.White
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 20.dp)
                .clip(CircleShape)
                .background(if (aktif) PinkSakelar else Color.White.copy(alpha = 0.3f))
                .padding(2.dp),
            contentAlignment = if (aktif) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
private fun KartuWaktuSholat(
    nama: String,
    jam: String,
    hitungMundur: String,
    progres: Float,
    jadwal: List<JadwalWaktu>,
    status: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(PanelGelap)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text("SHOLAT BERIKUTNYA", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TeksRedup, letterSpacing = 0.5.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(nama, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(jam, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Emas)
                }
            }
            if (hitungMundur.isNotBlank()) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(hitungMundur, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("jam : menit lagi", fontSize = 11.sp, color = TeksRedup)
                }
            }
        }
        LinearProgressIndicator(
            progress = { progres },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = Emas,
            trackColor = Color.White.copy(alpha = 0.18f),
            drawStopIndicator = {}
        )
        BarisJadwal(jadwal, redup = false)
        if (status.isNotBlank()) {
            Text(status, fontSize = 11.sp, color = TeksRedup)
        }
    }
}

@Composable
private fun KartuModeCuti(jadwal: List<JadwalWaktu>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(PanelGelap)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x2EFFC1D6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_flower),
                    contentDescription = null,
                    tint = Color(0xFFFFC1D6),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Mode Cuti aktif", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "Sholat dan puasa tidak dihitung. Dzikir dan tilawah tetap bisa dicatat.",
                    fontSize = 12.sp,
                    color = TeksLembut,
                    lineHeight = 16.sp
                )
            }
        }
        BarisJadwal(jadwal, redup = true)
        Text("Jadwal tetap tampil sebagai informasi.", fontSize = 11.sp, color = TeksRedup)
    }
}

/** Tujuh waktu dalam satu baris; waktu yang sedang berjalan diberi bingkai emas. */
@Composable
private fun BarisJadwal(jadwal: List<JadwalWaktu>, redup: Boolean) {
    if (jadwal.isEmpty()) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        jadwal.forEach { w ->
            val sorot = w.sekarang && !redup
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (sorot) Modifier.background(Emas.copy(alpha = 0.16f)).border(1.dp, Emas.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        else Modifier
                    )
                    .padding(vertical = 6.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${w.nama} ${w.jam}" + if (sorot) ", sedang berjalan" else ""
                    }
            ) {
                Text(
                    w.nama,
                    fontSize = 10.sp,
                    fontWeight = if (sorot) FontWeight.Bold else FontWeight.Normal,
                    color = if (sorot) Emas else TeksRedup,
                    maxLines = 1
                )
                Text(
                    w.jam,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        sorot -> Emas
                        !w.sholat -> TeksLembut.copy(alpha = if (redup) 0.6f else 0.85f)
                        redup -> Color.White.copy(alpha = 0.7f)
                        else -> Color.White
                    },
                    maxLines = 1
                )
            }
        }
    }
}
