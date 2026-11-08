package com.islami.Aha.data.notifikasi

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.local.UserHabitDao
import com.islami.Aha.data.repository.UserHabitRepository
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.util.NotificationScheduler
import com.islami.Aha.util.UserPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mengunggah jadwal pengingat 7 hari ke `users/{uid}/jadwal_pengingat` (lihat [JadwalPengingat]).
 * Hanya menyiapkan data untuk server; tidak mengirim notifikasi dan tidak menyentuh AlarmManager.
 *
 * Unggah ulang terjadi bila isi jadwal berubah (pengingat ditambah/diubah/dihapus, jam sholat
 * berubah karena lokasi, Mode Cuti, notifikasi global) atau sisa jadwal < 2 hari. Penggantian
 * dilakukan lengkap: dokumen lama yang tidak ada di jadwal baru ikut dihapus. Gagal (offline)
 * = sidik tidak disimpan, dicoba lagi di pemicu berikutnya.
 */
@Singleton
class SinkronJadwalPengingat @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SharedPreferences,
    private val habitDao: HabitDao,
    private val userHabitDao: UserHabitDao,
    private val userHabitRepository: UserHabitRepository,
    private val penyimpanToken: PenyimpanTokenFcm
) {
    companion object {
        private const val TAG = "SinkronJadwal"
        private const val KEY_SIDIK = "jadwal_pengingat_sidik_"
        private const val KEY_AKHIR = "jadwal_pengingat_akhir_ms_"
        fun kunciSidik(uid: String) = KEY_SIDIK + uid
        fun kunciAkhir(uid: String) = KEY_AKHIR + uid
        private const val BATAS_WAKTU_MS = 20_000L
        private const val UKURAN_BATCH = 450
        private const val JEDA_PEMICU_MS = 3_000L
    }

    private val auth: FirebaseAuth? by lazy { runCatching { FirebaseAuth.getInstance() }.getOrNull() }
    private val firestore: FirebaseFirestore? by lazy { runCatching { FirebaseFirestore.getInstance() }.getOrNull() }
    /** true = jadwal diunggah. Berbagi kunci dengan logout (lihat [PenyimpanTokenFcm.eksklusif]). */
    suspend fun sinkronkan(): Boolean = penyimpanToken.eksklusif { sinkronkanTerkunci() }

    private suspend fun sinkronkanTerkunci(): Boolean {
        val uid = auth?.currentUser?.uid ?: return false
        val db = firestore ?: return false
        val sumber = JadwalPengingat.sumberDari(
            habitBawaan = habitDao.getActiveReminderHabits(),
            habitSunnah = userHabitRepository.getActiveReminderHabits(),
            notifikasiGlobalAktif = NotificationScheduler.isGlobalNotificationEnabled(context),
            modeCuti = UserPreferencesManager.isHaidhMode.value
        )
        val sidik = JadwalPengingat.sidik(sumber)
        val sekarang = ZonedDateTime.now()
        val sekarangMs = sekarang.toInstant().toEpochMilli()
        val akhirTerkirim = prefs.getLong(kunciAkhir(uid), -1L).takeIf { it >= 0 }
        if (!JadwalPengingat.perluUnggah(sidik, prefs.getString(kunciSidik(uid), null), akhirTerkirim, sekarangMs)) {
            return false
        }
        val entri = JadwalPengingat.susun(sumber, sekarang)
        val ok = runCatching {
            withTimeoutOrNull(BATAS_WAKTU_MS) {
                val koleksi = db.collection(ResearchSchema.COLLECTION_USERS).document(uid)
                    .collection(JadwalPengingat.KOLEKSI)
                // Baca dari server: dokumen basi harus benar-benar terhapus, bukan ditebak dari cache.
                val lama = koleksi.get(Source.SERVER).await().documents.map { it.id }.toSet()
                val baru = entri.associateBy { it.docId }
                // null = hapus dokumen basi; selain itu = tulis entri.
                val operasi: List<Pair<String, JadwalPengingat.Entri?>> =
                    (lama - baru.keys).map { it to null } + baru.map { (id, e) -> id to e }
                operasi.chunked(UKURAN_BATCH).forEach { potongan ->
                    val batch = db.batch()
                    potongan.forEach { (id, e) ->
                        if (e == null) batch.delete(koleksi.document(id)) else batch.set(koleksi.document(id), e.keFirestore())
                    }
                    batch.commit().await()
                }
                true
            }
        }.onFailure { Log.w(TAG, "Unggah jadwal gagal", it) }.getOrNull() == true
        if (ok) {
            prefs.edit()
                .putString(kunciSidik(uid), sidik)
                .putLong(kunciAkhir(uid), entri.maxOfOrNull { it.waktuKirimMs } ?: -1L)
                .apply()
        }
        return ok
    }

    /**
     * Selama aplikasi tampil: segarkan token saat akun berganti/aplikasi dibuka, dan unggah ulang
     * jadwal setiap kali sumbernya berubah (dengan jeda supaya perubahan beruntun cukup sekali).
     */
    @OptIn(FlowPreview::class)
    suspend fun pantau() {
        coroutineScope {
            launch {
                uidFlow().distinctUntilChanged().collectLatest { uid ->
                    if (uid != null) runCatching { penyimpanToken.segarkan() }
                }
            }
            combine(
                uidFlow(),
                habitDao.getHabits(),
                userHabitDao.getAllHabits(),
                UserPreferencesManager.isHaidhMode,
                globalNotifikasiFlow()
            ) { uid, _, _, _, _ -> uid }
                .debounce(JEDA_PEMICU_MS)
                .collectLatest { uid -> if (uid != null) runCatching { sinkronkan() } }
        }
    }

    private fun uidFlow(): Flow<String?> = callbackFlow {
        val a = auth
        if (a == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val pendengar = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        a.addAuthStateListener(pendengar)
        awaitClose { a.removeAuthStateListener(pendengar) }
    }

    private fun globalNotifikasiFlow(): Flow<Boolean> = callbackFlow {
        trySend(NotificationScheduler.isGlobalNotificationEnabled(context))
        val pendengar = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == NotificationScheduler.KEY_GLOBAL_NOTIFICATION_ENABLED) {
                trySend(NotificationScheduler.isGlobalNotificationEnabled(context))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(pendengar)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(pendengar) }
    }.distinctUntilChanged()
}
