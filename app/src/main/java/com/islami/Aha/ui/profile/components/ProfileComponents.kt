package com.islami.Aha.ui.profile.components

import com.islami.Aha.ui.theme.teks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islami.Aha.R
import com.islami.Aha.ui.profile.Jejak
import com.islami.Aha.ui.profile.JenisJejak
import com.islami.Aha.ui.profile.UserInfo

private val HijauTua = Color(0xFF0B6B52)
private val HijauTengah = Color(0xFF0D7C5F)
private val HijauMuda = Color(0xFF17A27B)
private val HijauAksen = Color(0xFF2ECC9B)
private val HijauPucat = Color(0xFFD4F3E8)
private val Emas = Color(0xFFF2C66D)
private val Toska = Color(0xFF4FD1C5)
private val Ungu = Color(0xFFA48BF0)

@Composable
fun ProfileHeader(
    userInfo: UserInfo,
    totalHabits: Int,
    totalCompleted: Int,
    currentStreak: Int,
    isSavingAvatar: Boolean = false,
    onSettingsClick: () -> Unit,
    onLoginClick: () -> Unit,
    onEditProfileClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.linearGradient(listOf(HijauTua, HijauTengah, HijauMuda)))
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.profile_title), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.weight(1f))
            IconButton(
                onClick = onSettingsClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.14f), contentColor = Color.White)
            ) { Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.profile_settings_cd)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(userInfo, isSavingAvatar)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (userInfo.isLoggedIn) userInfo.name else stringResource(R.string.profile_guest_name),
                    fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (userInfo.isLoggedIn) userInfo.email else "Masuk agar catatanmu tersimpan di akun",
                    fontSize = 12.sp, color = HijauPucat, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (userInfo.isLoggedIn) "Ubah profil" else "Masuk",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(role = Role.Button, enabled = !isSavingAvatar) {
                            if (userInfo.isLoggedIn) onEditProfileClick() else onLoginClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            AngkaHeader("$totalHabits", "Kebiasaan aktif", Color.White, Modifier.weight(1f))
            AngkaHeader("$totalCompleted", "Total dicatat", Color.White, Modifier.weight(1f))
            AngkaHeader("$currentStreak", "Hari berturut", Emas, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Avatar(userInfo: UserInfo, isSavingAvatar: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(HijauPucat)
            .border(3.dp, Color.White, CircleShape)
    ) {
        if (userInfo.isLoggedIn && userInfo.avatarUri != null) {
            val model = remember(userInfo.avatarUri) {
                if (userInfo.avatarUri.startsWith("data:image")) {
                    android.util.Base64.decode(userInfo.avatarUri.substringAfter("base64,"), android.util.Base64.DEFAULT)
                } else userInfo.avatarUri
            }
            coil.compose.AsyncImage(
                model = model,
                contentDescription = stringResource(R.string.profile_photo_cd),
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                if (userInfo.isLoggedIn) userInfo.avatarInitial else "T",
                fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = HijauTengah
            )
        }
        if (isSavingAvatar) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color.White, strokeWidth = 3.dp)
            }
        }
    }
}

@Composable
private fun AngkaHeader(angka: String, label: String, warna: Color, modifier: Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x66062B1F))
            .padding(10.dp)
    ) {
        Text(angka, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = warna)
        Text(label, fontSize = 11.sp, color = Color(0xFFBFEBDB), textAlign = TextAlign.Center)
    }
}

private data class GayaJejak(val ikon: ImageVector, val warna: Color)

private fun gayaJejak(jenis: JenisJejak) = when (jenis) {
    JenisJejak.PERTAMA -> GayaJejak(Icons.Rounded.Check, HijauAksen)
    JenisJejak.SUNNAH -> GayaJejak(Icons.Rounded.WbSunny, HijauAksen)
    JenisJejak.PUASA -> GayaJejak(Icons.Rounded.DarkMode, Ungu)
    JenisJejak.DZIKIR -> GayaJejak(Icons.Rounded.SelfImprovement, Toska)
    JenisJejak.TILAWAH -> GayaJejak(Icons.Rounded.AutoStories, Emas)
    JenisJejak.IQRO -> GayaJejak(Icons.Rounded.MenuBook, Emas)
    JenisJejak.KELIPATAN -> GayaJejak(Icons.Rounded.Flag, HijauAksen)
    JenisJejak.REKOR -> GayaJejak(Icons.Rounded.LocalFireDepartment, Emas)
}

/** Linimasa "Jejak ibadah": 3 momen terbaru, sisanya lewat "Lihat semua". */
@Composable
fun JejakIbadahSection(jejak: List<Jejak>, onLihatSemua: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Jejak ibadah", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            if (jejak.size > 3) {
                Text(
                    "Lihat semua",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HijauAksen.teks(),
                    modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onLihatSemua).padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(14.dp)
        ) {
            if (jejak.isEmpty()) {
                Text("Belum ada jejak", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Catatan ibadah pertamamu akan muncul di sini.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Linimasa(jejak.take(3))
            }
        }
        Text(
            "Momen yang sudah kamu lalui. Tidak ada yang dikunci atau dinilai.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun Linimasa(jejak: List<Jejak>) {
    jejak.forEachIndexed { i, j ->
        val gaya = gayaJejak(j.jenis)
        val terakhir = i == jejak.lastIndex
        Row(Modifier.height(IntrinsicSize.Min)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(gaya.warna.copy(alpha = 0.16f))
                ) { Icon(gaya.ikon, contentDescription = null, tint = gaya.warna.teks(), modifier = Modifier.size(16.dp)) }
                if (!terakhir) {
                    Box(
                        Modifier
                            .padding(vertical = 4.dp)
                            .width(2.dp)
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(
                Modifier.weight(1f).padding(bottom = if (terakhir) 0.dp else 14.dp, top = 5.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(j.judul, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(j.keterangan, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Lembar berisi semua jejak. */
@Composable
fun SemuaJejakSheet(jejak: List<Jejak>) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp)
    ) {
        Text("Jejak ibadah", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text("${jejak.size} momen sejak mulai mencatat", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.verticalScroll(rememberScrollState())) { Linimasa(jejak) }
    }
}

@Composable
fun KebiasaanmuSection(sholatSunnah: Int, puasaSunnah: Int, dzikir: Int, pengingat: Int) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Kebiasaanmu", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            BarisKebiasaan(Icons.Rounded.Mosque, HijauAksen, "Sholat sunnah", sholatSunnah)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            BarisKebiasaan(Icons.Rounded.DarkMode, Ungu, "Puasa sunnah", puasaSunnah)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            BarisKebiasaan(Icons.Rounded.SelfImprovement, Toska, "Dzikir", dzikir)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            BarisKebiasaan(Icons.Rounded.Notifications, Emas, "Pengingat aktif", pengingat)
        }
    }
}

@Composable
private fun BarisKebiasaan(ikon: ImageVector, warna: Color, label: String, jumlah: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(warna.copy(alpha = 0.16f))
        ) { Icon(ikon, contentDescription = null, tint = warna.teks(), modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(
            if (jumlah > 0) "$jumlah" else "Belum ada",
            fontSize = 13.sp,
            fontWeight = if (jumlah > 0) FontWeight.Bold else FontWeight.Normal,
            color = if (jumlah > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
