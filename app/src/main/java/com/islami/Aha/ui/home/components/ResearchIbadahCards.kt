package com.islami.Aha.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Iqro pindah ke IqroComponents.kt; dzikir ke DzikirCards.kt.

/** Pengguna belum menambahkan habit dzikir: ajakan netral, tanpa angka progres. */
@Composable
fun DzikirEntryCard(onAdd: () -> Unit) {
    AjakanKosong(
        ikon = Icons.Rounded.SelfImprovement,
        warna = Color(0xFF4FD1C5),
        judul = "Belum ada dzikir",
        isi = "Pilih dzikir yang biasa kamu amalkan, misalnya setelah sholat fardhu, pagi dan petang, atau sebelum tidur. Setiap dzikir dicatat sendiri-sendiri.",
        tombol = "Pilih dzikir",
        onTombol = onAdd
    )
}

