package com.islami.Aha.data.notifikasi

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tahap 1: hanya menyimpan token. Pesan masuk belum ditangani (server belum mengirim apa pun);
 * pengingat tetap sepenuhnya dari alarm lokal.
 */
@AndroidEntryPoint
class AhaMessagingService : FirebaseMessagingService() {
    @Inject lateinit var penyimpanToken: PenyimpanTokenFcm

    // Tidak dibatalkan di onDestroy: service ini berumur pendek, penulisan token harus tuntas.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { runCatching { penyimpanToken.tokenBaru(token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Sengaja kosong sampai Tahap 2.
    }
}
