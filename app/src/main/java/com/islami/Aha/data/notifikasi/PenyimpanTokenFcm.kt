package com.islami.Aha.data.notifikasi

import android.content.SharedPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration

/**
 * Menyimpan token FCM perangkat ke `users/{uid}/meta/notifikasi` (field [FIELD_TOKEN] +
 * [FIELD_DIPERBARUI]). Sengaja bukan di `users/{uid}`: dokumen itu hanya untuk peserta
 * penelitian, sedangkan notifikasi juga untuk yang menolak.
 *
 * - [tokenBaru]: dipanggil dari onNewToken; selalu ditulis bila token berbeda.
 * - [segarkan]: dipanggil saat aplikasi dibuka; menulis bila token berbeda atau penulisan
 *   terakhir lebih lama dari [SEGARKAN_SETELAH] (cap waktu ini dipakai server untuk
 *   membuang token basi).
 * - [hapusSaatLogout]: hapus token DAN seluruh jadwal_pengingat di server SELAGI masih login,
 *   jalankan signOut, lalu hapus token di perangkat (token lama mati di FCM walau penghapusan
 *   server gagal).
 * - [eksklusif]: kunci bersama dengan unggah jadwal, supaya jadwal tidak tertulis lagi di
 *   tengah logout.
 *
 * Tanpa login tidak ada yang ditulis. Belum ada notifikasi yang dikirim di tahap ini.
 */
class PenyimpanTokenFcm(
    private val lokal: Lokal,
    private val remote: Remote,
    private val uidSaatIni: () -> String?,
    private val ambilToken: suspend () -> String?,
    private val jam: () -> Long = System::currentTimeMillis
) {
    companion object {
        const val FIELD_TOKEN = "fcm_token"
        const val FIELD_DIPERBARUI = "fcm_token_diperbarui"
        const val KOLEKSI_META = "meta"
        const val DOC_NOTIFIKASI = "notifikasi"
        val SEGARKAN_SETELAH: Duration = Duration.ofHours(24)
        private const val BATAS_WAKTU_MS = 10_000L

        fun perluSimpan(tokenBaru: String, tersimpan: Tersimpan?, sekarangMs: Long): Boolean {
            if (tersimpan == null || tersimpan.token != tokenBaru) return true
            return sekarangMs - tersimpan.waktuMs >= SEGARKAN_SETELAH.toMillis()
        }
    }

    data class Tersimpan(val token: String, val waktuMs: Long)

    /** Catatan lokal token & jadwal terakhir yang berhasil ditulis, per akun. */
    interface Lokal {
        fun baca(uid: String): Tersimpan?
        fun tulis(uid: String, data: Tersimpan)
        fun hapus(uid: String)
    }

    interface Remote {
        suspend fun simpan(uid: String, token: String)
        /** Hapus meta/notifikasi dan semua dokumen jadwal_pengingat milik [uid]. */
        suspend fun hapus(uid: String)
        suspend fun hapusTokenPerangkat()
    }

    private val kunci = Mutex()

    /** Jalankan [blok] tanpa bertumpukan dengan penulisan token / logout. Tidak reentrant. */
    suspend fun <T> eksklusif(blok: suspend () -> T): T = kunci.withLock { blok() }

    /** Saat aplikasi dibuka. true = token ditulis ke server. */
    suspend fun segarkan(): Boolean {
        if (uidSaatIni() == null) return false
        val token = runCatching { withTimeoutOrNull(BATAS_WAKTU_MS) { ambilToken() } }.getOrNull() ?: return false
        return tulisBila(token, paksa = false)
    }

    /** Dari onNewToken. true = token ditulis ke server. */
    suspend fun tokenBaru(token: String): Boolean = tulisBila(token, paksa = false)

    private suspend fun tulisBila(token: String, paksa: Boolean): Boolean = kunci.withLock {
        if (token.isBlank()) return false
        val uid = uidSaatIni() ?: return false
        val sekarang = jam()
        if (!paksa && !perluSimpan(token, lokal.baca(uid), sekarang)) return false
        val ok = runCatching { withTimeoutOrNull(BATAS_WAKTU_MS) { remote.simpan(uid, token); true } }
            .getOrNull() == true
        if (ok) lokal.tulis(uid, Tersimpan(token, sekarang))
        ok
    }

    /**
     * Urutan penting: token & jadwal dihapus selagi masih login (aturan server butuh pemiliknya),
     * lalu [signOut], lalu token perangkat dihapus supaya FCM tidak memakai token lama lagi.
     */
    suspend fun hapusSaatLogout(signOut: () -> Unit) {
        kunci.withLock {
            val uid = uidSaatIni()
            if (uid != null) {
                runCatching { withTimeoutOrNull(BATAS_WAKTU_MS) { remote.hapus(uid) } }
                lokal.hapus(uid)
            }
            signOut()
            runCatching { withTimeoutOrNull(BATAS_WAKTU_MS) { remote.hapusTokenPerangkat() } }
        }
    }

    /** Catatan lokal dibuang: token & jadwal ditulis ulang di kesempatan berikutnya. */
    fun lupakan(uid: String) = lokal.hapus(uid)
}

/** [PenyimpanTokenFcm.Lokal] di SharedPreferences. */
class TokenFcmLokalPrefs(private val prefs: SharedPreferences) : PenyimpanTokenFcm.Lokal {
    private fun kunciToken(uid: String) = "fcm_token_terkirim_$uid"
    private fun kunciWaktu(uid: String) = "fcm_token_terkirim_ms_$uid"

    override fun baca(uid: String): PenyimpanTokenFcm.Tersimpan? {
        val token = prefs.getString(kunciToken(uid), null) ?: return null
        return PenyimpanTokenFcm.Tersimpan(token, prefs.getLong(kunciWaktu(uid), 0L))
    }

    override fun tulis(uid: String, data: PenyimpanTokenFcm.Tersimpan) {
        prefs.edit().putString(kunciToken(uid), data.token).putLong(kunciWaktu(uid), data.waktuMs).apply()
    }

    override fun hapus(uid: String) {
        // Sidik jadwal ikut dibuang: setelah login lagi jadwal diunggah ulang secara utuh.
        prefs.edit().remove(kunciToken(uid)).remove(kunciWaktu(uid))
            .remove(SinkronJadwalPengingat.kunciSidik(uid)).remove(SinkronJadwalPengingat.kunciAkhir(uid))
            .apply()
    }
}
