package com.islami.Aha

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.islami.Aha.app.AhaApp
import com.islami.Aha.util.NotifikasiSesi
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dibuka dari badan notifikasi pengingat (bukan saat dibuat ulang karena rotasi).
        if (savedInstanceState == null) NotifikasiSesi.dariIntent(intent)

        enableEdgeToEdge()

        setContent {
            AhaApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NotifikasiSesi.dariIntent(intent)
    }
}
