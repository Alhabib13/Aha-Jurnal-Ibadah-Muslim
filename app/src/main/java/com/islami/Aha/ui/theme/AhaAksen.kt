package com.islami.Aha.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** true bila tema yang sedang aktif gelap (dibaca dari latar, jadi ikut tema dinamis). */
@Composable
@ReadOnlyComposable
fun temaGelap(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

// Warna aksen di desain baru disetel untuk latar gelap (terang & jenuh). Di atas latar
// putih warna itu terlalu pucat untuk teks/ikon, jadi tema terang memakai versi yang
// lebih tua. Isian (latar tombol/chip) tetap memakai warna aslinya.
private val VERSI_TERANG = mapOf(
    Color(0xFF2ECC9B) to Color(0xFF0B7A5B),
    Color(0xFF7BE3C2) to Color(0xFF0B7A5B),
    Color(0xFF14A87D) to Color(0xFF0B7A5B),
    Color(0xFFF2C66D) to Color(0xFF8A5A00),
    Color(0xFFEDB84E) to Color(0xFF8A5A00),
    Color(0xFF4FD1C5) to Color(0xFF0F6E68),
    Color(0xFFA48BF0) to Color(0xFF5B3CC4),
    Color(0xFFFFC1D6) to Color(0xFFB0335E),
    Color(0xFFF4A261) to Color(0xFFA04E12)
)

/** Warna aksen untuk TEKS/IKON di atas latar kartu atau halaman. */
@Composable
@ReadOnlyComposable
fun Color.teks(): Color = if (temaGelap()) this else VERSI_TERANG[this] ?: this

/**
 * Ikon status bar putih selama [aktif] (mis. kepala hijau masih terlihat di atas layar).
 * Di luar itu, dan saat layar ditinggalkan, kembali mengikuti tema.
 */
@Composable
fun IkonStatusBarTerang(aktif: Boolean) {
    val view = androidx.compose.ui.platform.LocalView.current
    if (view.isInEditMode) return
    val gelap = temaGelap()
    val window = (view.context as? android.app.Activity)?.window ?: return
    val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
    androidx.compose.runtime.SideEffect { controller.isAppearanceLightStatusBars = !gelap && !aktif }
    androidx.compose.runtime.DisposableEffect(controller) {
        onDispose { controller.isAppearanceLightStatusBars = !gelap }
    }
}

/**
 * Lapisan di belakang status bar saat daftar di-scroll, supaya teks halaman tidak bertumpuk
 * dengan jam/ikon sistem. Selama kepala hijau masih terlihat warnanya [warnaKepala], setelah
 * itu warna latar. Letakkan setelah daftar di dalam Box yang sama.
 */
@Composable
fun ScrimStatusBar(daftar: androidx.compose.foundation.lazy.LazyListState, warnaKepala: Color) {
    val terscroll = daftar.firstVisibleItemIndex > 0 || daftar.firstVisibleItemScrollOffset > 0
    if (!terscroll) return
    val warna = if (daftar.firstVisibleItemIndex == 0) warnaKepala else MaterialTheme.colorScheme.background
    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(warna)
    )
}
