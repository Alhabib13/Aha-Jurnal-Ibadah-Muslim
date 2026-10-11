package com.islami.Aha.data.research

import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.islami.Aha.data.quran.AyatPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** Posisi Iqro (atau metode sejenis) terakhir. */
data class IqroPosition(val jilid: Int, val halaman: Int)

/**
 * Penanda & target tilawah = KEADAAN, bukan kejadian: tidak masuk koleksi `logs`.
 * Disimpan lokal (berlaku juga untuk tamu) dan, bila login, di:
 *   users/{uid}/penanda/{bacaan_harian|hafalan|murajaah}  { surah, ayat }  (posisi AKHIR sesi terakhir)
 *   users/{uid}/penanda/iqro                              { jilid, halaman }
 *   users/{uid}/target/tilawah                            { halaman_per_hari }  (hanya bacaan_harian)
 */
@Singleton
class TilawahStateRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        val JENIS_QURAN = listOf(
            ResearchSchema.Detail.BACAAN_HARIAN,
            ResearchSchema.Detail.HAFALAN,
            ResearchSchema.Detail.MURAJAAH
        )
        const val JENIS_IQRO = "iqro"
        private const val PREFIX = "tilawah_penanda_"
        private const val KEY_TARGET = "tilawah_target_halaman"
        private const val TIMEOUT_MS = 10_000L
    }

    private val auth: FirebaseAuth? by lazy { runCatching { FirebaseAuth.getInstance() }.getOrNull() }
    private val firestore: FirebaseFirestore? by lazy { runCatching { FirebaseFirestore.getInstance() }.getOrNull() }

    /** Naik setiap kali keadaan berubah; UI mengamati ini untuk membaca ulang. */
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    fun penanda(jenis: String): AyatPosition? {
        val raw = sharedPreferences.getString(PREFIX + jenis, null) ?: return null
        val (s, a) = raw.split(":").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 2 } ?: return null
        return AyatPosition(s, a).takeIf { it.isValid }
    }

    fun penandaIqro(): IqroPosition? {
        val raw = sharedPreferences.getString(PREFIX + JENIS_IQRO, null) ?: return null
        val (j, h) = raw.split(":").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 2 } ?: return null
        return IqroPosition(j, h).takeIf { j in ResearchSchema.IQRO_JILID && h in ResearchSchema.IQRO_HALAMAN }
    }

    fun targetHalaman(): Int? =
        sharedPreferences.getInt(KEY_TARGET, 0).takeIf { it in ResearchSchema.TARGET_HALAMAN }

    fun setPenanda(jenis: String, pos: AyatPosition) {
        require(jenis in JENIS_QURAN && pos.isValid)
        sharedPreferences.edit().putString(PREFIX + jenis, "${pos.surah}:${pos.ayat}").apply()
        bump()
        pushDoc("penanda", jenis, mapOf("surah" to pos.surah.toLong(), "ayat" to pos.ayat.toLong()))
    }

    fun setPenandaIqro(jilid: Int, halaman: Int) {
        if (jilid !in ResearchSchema.IQRO_JILID || halaman !in ResearchSchema.IQRO_HALAMAN) return
        sharedPreferences.edit().putString(PREFIX + JENIS_IQRO, "$jilid:$halaman").apply()
        bump()
        pushDoc("penanda", JENIS_IQRO, mapOf("jilid" to jilid.toLong(), "halaman" to halaman.toLong()))
    }

    /** null = hapus target. */
    fun setTargetHalaman(halaman: Int?) {
        if (halaman == null) {
            sharedPreferences.edit().remove(KEY_TARGET).apply()
            bump()
            val uid = auth?.currentUser?.uid ?: return
            runCatching { firestore?.collection("users")?.document(uid)?.collection("target")?.document("tilawah")?.delete() }
            return
        }
        if (halaman !in ResearchSchema.TARGET_HALAMAN) return
        sharedPreferences.edit().putInt(KEY_TARGET, halaman).apply()
        bump()
        pushDoc("target", "tilawah", mapOf("halaman_per_hari" to halaman.toLong()))
    }

    /**
     * Saat login: keadaan lokal yang sudah ada didorong ke cloud (aktivitas terbaru di HP ini),
     * yang belum ada lokal diambil dari cloud (install ulang / HP baru).
     */
    suspend fun syncOnLogin() {
        val uid = auth?.currentUser?.uid ?: return
        val db = firestore ?: return
        val userDoc = db.collection("users").document(uid)
        runCatching {
            withTimeoutOrNull(TIMEOUT_MS) {
                for (jenis in JENIS_QURAN) {
                    val local = penanda(jenis)
                    if (local != null) {
                        pushDoc("penanda", jenis, mapOf("surah" to local.surah.toLong(), "ayat" to local.ayat.toLong()))
                    } else {
                        val doc = userDoc.collection("penanda").document(jenis).get().await()
                        val s = doc.getLong("surah")?.toInt()
                        val a = doc.getLong("ayat")?.toInt()
                        if (s != null && a != null && AyatPosition(s, a).isValid) {
                            sharedPreferences.edit().putString(PREFIX + jenis, "$s:$a").apply()
                        }
                    }
                }
                val iqro = penandaIqro()
                if (iqro != null) {
                    pushDoc("penanda", JENIS_IQRO, mapOf("jilid" to iqro.jilid.toLong(), "halaman" to iqro.halaman.toLong()))
                } else {
                    val doc = userDoc.collection("penanda").document(JENIS_IQRO).get().await()
                    val j = doc.getLong("jilid")?.toInt()
                    val h = doc.getLong("halaman")?.toInt()
                    if (j != null && h != null) setLocalIqro(j, h)
                }
                val target = targetHalaman()
                if (target != null) {
                    pushDoc("target", "tilawah", mapOf("halaman_per_hari" to target.toLong()))
                } else {
                    val t = userDoc.collection("target").document("tilawah").get().await()
                        .getLong("halaman_per_hari")?.toInt()
                    if (t != null && t in ResearchSchema.TARGET_HALAMAN) {
                        sharedPreferences.edit().putInt(KEY_TARGET, t).apply()
                    }
                }
            }
        }.onFailure { runCatching { com.islami.Aha.util.logCrashlyticsSafe(it) } }
        bump()
    }

    /** Logout / ganti akun / hapus akun: keadaan milik akun sebelumnya dibuang. */
    fun clearLocal() {
        val editor = sharedPreferences.edit()
        (JENIS_QURAN + JENIS_IQRO).forEach { editor.remove(PREFIX + it) }
        editor.remove(KEY_TARGET).apply()
        bump()
    }

    private fun setLocalIqro(jilid: Int, halaman: Int) {
        if (jilid in ResearchSchema.IQRO_JILID && halaman in ResearchSchema.IQRO_HALAMAN) {
            sharedPreferences.edit().putString(PREFIX + JENIS_IQRO, "$jilid:$halaman").apply()
        }
    }

    /** Tulis ke Firestore tanpa menunggu; saat offline, SDK mengantre dan mengirimnya nanti. */
    private fun pushDoc(collection: String, docId: String, data: Map<String, Any>) {
        val uid = auth?.currentUser?.uid ?: return
        runCatching {
            firestore?.collection("users")?.document(uid)?.collection(collection)?.document(docId)?.set(data)
                ?.addOnFailureListener { runCatching { com.islami.Aha.util.logCrashlyticsSafe(it) } }
        }
    }

    private fun bump() {
        _version.value = _version.value + 1
    }
}
