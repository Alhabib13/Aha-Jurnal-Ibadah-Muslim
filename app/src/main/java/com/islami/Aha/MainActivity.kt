package com.islami.Aha

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.islami.Aha.app.AhaApp
import com.islami.Aha.data.notifikasi.SinkronJadwalPengingat
import com.islami.Aha.util.NotifikasiSesi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var sinkronJadwal: SinkronJadwalPengingat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dibuka dari badan notifikasi pengingat (bukan saat dibuat ulang karena rotasi).
        if (savedInstanceState == null) NotifikasiSesi.dariIntent(intent)

        enableEdgeToEdge()

        setContent {
            AhaApp()
        }

        // Data untuk notifikasi server (Tahap 1): token FCM + jadwal 7 hari, selama aplikasi tampil.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { runCatching { sinkronJadwal.pantau() } }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NotifikasiSesi.dariIntent(intent)
    }
}
