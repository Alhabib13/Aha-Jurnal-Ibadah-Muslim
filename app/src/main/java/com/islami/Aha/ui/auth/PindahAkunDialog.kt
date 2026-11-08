package com.islami.Aha.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Peringatan sebelum tamu masuk ke akun yang SUDAH ADA (README, keputusan 2): catatan selama
 * menjadi tamu tidak ikut ke akun itu dan hilang dari HP ini.
 */
@Composable
fun DialogPindahAkun(baris: List<String>, onLanjut: () -> Unit, onBatal: () -> Unit) {
    AlertDialog(
        onDismissRequest = onBatal,
        title = { Text("Masuk ke akun yang sudah ada?") },
        text = {
            Column {
                Text(
                    "Akun ini sudah punya data sendiri. Catatanmu selama memakai Aha tanpa akun tidak bisa " +
                        "digabung ke sana, dan akan hilang dari HP ini:",
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                baris.take(12).forEach { Text("• $it", fontSize = 14.sp) }
                if (baris.size > 12) Text("• dan ${baris.size - 12} lainnya", fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Kalau ingin menyimpannya, batalkan lalu daftar akun baru: catatanmu akan ikut ke akun baru itu.",
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = { TextButton(onClick = onLanjut) { Text("Tetap masuk") } },
        dismissButton = { TextButton(onClick = onBatal) { Text("Batal") } }
    )
}
