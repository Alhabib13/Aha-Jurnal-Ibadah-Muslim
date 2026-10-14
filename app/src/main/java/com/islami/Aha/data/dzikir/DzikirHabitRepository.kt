package com.islami.Aha.data.dzikir

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.islami.Aha.data.local.DzikirHabitDao
import com.islami.Aha.data.model.DzikirHabitEntity
import com.islami.Aha.data.research.ResearchSchema
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Permintaan menambah satu habit dzikir dari Add Habit. */
data class DzikirHabitDraft(
    val slug: String,
    val modePencatatan: String = ResearchSchema.ModePencatatan.CENTANG,
    val targetHitungan: Int? = null,
    val namaDetailAsli: String? = null
)

/**
 * Habit dzikir (KONFIGURASI). Lokal di tabel `dzikir_habits`; bila login juga di
 * users/{uid}/dzikir_habits/{id}. Repository ini TIDAK PERNAH menulis research_logs/logs.
 */
@Singleton
class DzikirHabitRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: DzikirHabitDao
) {
    companion object {
        const val SUBCOLLECTION = "dzikir_habits"
        private const val NAMA_MAKS = 100
        private const val TIMEOUT_MS = 10_000L

        /** Field dokumen Firestore (kunci bernilai null tidak ditulis). */
        const val F_SLUG = "slug"
        const val F_NAMA_DETAIL_ASLI = "nama_detail_asli"
        const val F_MODE = "mode_pencatatan"
        const val F_WAKTU = "waktu_dzikir"
        const val F_TARGET = "target_hitungan"
        const val F_DIBUAT = "dibuat_pada"

        /**
         * Validasi murni (diuji unit). Mengembalikan pesan kesalahan untuk pengguna, atau null
         * bila sah. Satu habit per slug katalog; dzikir_lainnya boleh banyak asal namanya beda.
         */
        fun validate(
            draft: DzikirHabitDraft,
            katalog: List<DzikirKatalogEntry>,
            existing: List<DzikirHabitEntity>
        ): String? {
            val entry = katalog.firstOrNull { it.slug == draft.slug } ?: return "Dzikir tidak dikenal."
            if (draft.modePencatatan !in entry.modeTersedia) {
                return "Mode layar belum tersedia karena bacaannya belum ada."
            }
            if (draft.targetHitungan != null && draft.targetHitungan !in ResearchSchema.TARGET_HITUNGAN) {
                return "Target hitungan tidak sah."
            }
            if (draft.slug == ResearchSchema.Detail.DZIKIR_LAINNYA) {
                val nama = draft.namaDetailAsli?.trim().orEmpty()
                if (nama.isEmpty()) return "Tulis nama dzikirnya."
                if (nama.length > NAMA_MAKS) return "Nama dzikir maksimal $NAMA_MAKS karakter."
                if (existing.any { it.slug == draft.slug && it.namaDetailAsli.equals(nama, ignoreCase = true) }) {
                    return "\"$nama\" sudah ada di daftar dzikirmu."
                }
            } else if (existing.any { it.slug == draft.slug }) {
                return "${entry.nama} sudah ada di daftar dzikirmu."
            }
            return null
        }

        fun toFirestore(h: DzikirHabitEntity): Map<String, Any> = buildMap {
            put(F_SLUG, h.slug)
            h.namaDetailAsli?.let { put(F_NAMA_DETAIL_ASLI, it) }
            put(F_MODE, h.modePencatatan)
            put(F_WAKTU, h.waktuDzikir)
            h.targetHitungan?.let { put(F_TARGET, it.toLong()) }
            put(F_DIBUAT, h.createdAt)
        }
    }

    private val auth: FirebaseAuth? by lazy { runCatching { FirebaseAuth.getInstance() }.getOrNull() }
    private val firestore: FirebaseFirestore? by lazy { runCatching { FirebaseFirestore.getInstance() }.getOrNull() }
    val katalog: List<DzikirKatalogEntry> by lazy { DzikirKatalog.load(context) }

    private fun currentUid(): String? = auth?.currentUser?.uid

    fun observeAll(): Flow<List<DzikirHabitEntity>> = dao.observeAll()

    suspend fun getAll(): List<DzikirHabitEntity> = dao.getAll()

    /**
     * Tambah beberapa habit sekaligus. Semua divalidasi dulu (termasuk terhadap sesama
     * draft); bila ada satu yang tidak sah, tidak ada yang disimpan.
     * @return null bila tersimpan, atau pesan kesalahan.
     */
    suspend fun addAll(drafts: List<DzikirHabitDraft>, nowMs: Long = System.currentTimeMillis()): String? {
        if (drafts.isEmpty()) return "Pilih minimal satu dzikir."
        val katalog = katalog
        val accepted = dao.getAll().toMutableList()
        val baru = mutableListOf<DzikirHabitEntity>()
        drafts.forEachIndexed { i, d ->
            validate(d, katalog, accepted)?.let { return it }
            val entry = katalog.first { it.slug == d.slug }
            val habit = DzikirHabitEntity(
                id = UUID.randomUUID().toString(),
                slug = d.slug,
                namaDetailAsli = if (d.slug == ResearchSchema.Detail.DZIKIR_LAINNYA) d.namaDetailAsli?.trim() else null,
                modePencatatan = d.modePencatatan,
                waktuDzikir = entry.waktuDefault,
                targetHitungan = d.targetHitungan,
                createdAt = nowMs + i, // urutan tampil = urutan dipilih
                ownerUid = currentUid()
            )
            accepted += habit
            baru += habit
        }
        baru.forEach { dao.upsert(it); push(it) }
        return null
    }

    /** Hapus habit. Riwayat log-nya TIDAK dihapus (catatan masa lalu tetap catatan). */
    suspend fun remove(id: String) {
        dao.deleteById(id)
        val uid = currentUid() ?: return
        runCatching {
            firestore?.collection(ResearchSchema.COLLECTION_USERS)?.document(uid)
                ?.collection(SUBCOLLECTION)?.document(id)?.delete()
                ?.addOnFailureListener { runCatching { com.islami.Aha.util.logCrashlyticsSafe(it) } }
        }
    }

    /** Saat login: habit tamu diadopsi & didorong ke cloud; habit dari cloud yang belum ada diambil. */
    suspend fun syncOnLogin() {
        val uid = currentUid() ?: return
        val col = firestore?.collection(ResearchSchema.COLLECTION_USERS)?.document(uid)?.collection(SUBCOLLECTION) ?: return
        dao.adoptGuestRows(uid)
        dao.getAll().forEach { push(it) }
        runCatching {
            val docs = withTimeoutOrNull(TIMEOUT_MS) { col.get().await() }?.documents ?: return
            val slugSah = ResearchSchema.DETAIL_PER_JENIS.getValue(ResearchSchema.Jenis.DZIKIR)
            val rows = docs.mapNotNull { doc ->
                val slug = doc.getString(F_SLUG)?.takeIf { it in slugSah } ?: return@mapNotNull null
                val mode = doc.getString(F_MODE)?.takeIf { it in ResearchSchema.MODE_PENCATATAN } ?: return@mapNotNull null
                val waktu = doc.getString(F_WAKTU)?.takeIf { it in ResearchSchema.WAKTU_DZIKIR } ?: return@mapNotNull null
                DzikirHabitEntity(
                    id = doc.id, slug = slug,
                    namaDetailAsli = doc.getString(F_NAMA_DETAIL_ASLI),
                    modePencatatan = mode, waktuDzikir = waktu,
                    targetHitungan = doc.getLong(F_TARGET)?.toInt(),
                    createdAt = doc.getLong(F_DIBUAT) ?: 0L,
                    ownerUid = uid
                )
            }
            dao.insertIgnore(rows)
        }.onFailure { runCatching { com.islami.Aha.util.logCrashlyticsSafe(it) } }
    }

    /** Logout / hapus akun: habit milik akun sebelumnya dibuang dari HP ini. */
    suspend fun clearLocal() = dao.deleteAll()

    /** Akun berganti tanpa lewat logout. */
    suspend fun clearNotOwnedBy(uid: String) = dao.deleteNotOwnedBy(uid)

    /** Tulis ke Firestore tanpa menunggu; saat offline, SDK mengantre dan mengirimnya nanti. */
    private fun push(h: DzikirHabitEntity) {
        val uid = currentUid() ?: return
        runCatching {
            firestore?.collection(ResearchSchema.COLLECTION_USERS)?.document(uid)
                ?.collection(SUBCOLLECTION)?.document(h.id)?.set(toFirestore(h))
                ?.addOnFailureListener { runCatching { com.islami.Aha.util.logCrashlyticsSafe(it) } }
        }
    }
}
