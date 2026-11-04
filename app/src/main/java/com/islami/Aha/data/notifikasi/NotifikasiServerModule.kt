package com.islami.Aha.data.notifikasi

import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.messaging.FirebaseMessaging
import com.islami.Aha.data.research.ResearchSchema
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
import javax.inject.Singleton

/**
 * [PenyimpanTokenFcm.Remote] ke Firestore `users/{uid}/meta/notifikasi` + `jadwal_pengingat`
 * dan FirebaseMessaging. Tidak pernah menulis dokumen `users/{uid}` itu sendiri.
 */
class TokenFcmRemoteFirestore : PenyimpanTokenFcm.Remote {
    private fun dokumenToken(uid: String) =
        FirebaseFirestore.getInstance().collection(ResearchSchema.COLLECTION_USERS).document(uid)
            .collection(PenyimpanTokenFcm.KOLEKSI_META).document(PenyimpanTokenFcm.DOC_NOTIFIKASI)

    override suspend fun simpan(uid: String, token: String) {
        dokumenToken(uid).set(
            mapOf(
                PenyimpanTokenFcm.FIELD_TOKEN to token,
                PenyimpanTokenFcm.FIELD_DIPERBARUI to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun hapus(uid: String) {
        // Token dulu (server tidak lagi punya alamat kirim), lalu seluruh jadwal.
        dokumenToken(uid).delete().await()
        hapusSemuaJadwal(FirebaseFirestore.getInstance(), uid)
    }

    override suspend fun hapusTokenPerangkat() {
        FirebaseMessaging.getInstance().deleteToken().await()
    }
}

/** Hapus seluruh `users/{uid}/jadwal_pengingat` (dibaca dari server, dihapus per batch). */
suspend fun hapusSemuaJadwal(db: FirebaseFirestore, uid: String) {
    db.collection(ResearchSchema.COLLECTION_USERS).document(uid).collection(JadwalPengingat.KOLEKSI)
        .get(Source.SERVER).await().documents.chunked(450).forEach { potongan ->
            val batch = db.batch()
            potongan.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
}

@Module
@InstallIn(SingletonComponent::class)
object NotifikasiServerModule {
    @Provides
    @Singleton
    fun providePenyimpanTokenFcm(prefs: SharedPreferences): PenyimpanTokenFcm =
        PenyimpanTokenFcm(
            lokal = TokenFcmLokalPrefs(prefs),
            remote = TokenFcmRemoteFirestore(),
            uidSaatIni = { runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull() },
            ambilToken = { FirebaseMessaging.getInstance().token.await() }
        )
}
