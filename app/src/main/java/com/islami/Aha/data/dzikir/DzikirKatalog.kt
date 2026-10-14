package com.islami.Aha.data.dzikir

import android.content.Context
import com.islami.Aha.data.research.ResearchSchema
import org.json.JSONArray
import org.json.JSONObject

/**
 * Satu bacaan dzikir. Teks ibadah — TIDAK BOLEH dikarang: diisi pemilik proyek dari buku
 * rujukan cetak ke assets/dzikir_katalog.json. Kode ini hanya wadah + parser.
 */
data class BacaanDzikir(
    val arab: String,
    val latin: String,
    val arti: String,
    val sumber: String,
    val jumlahAnjuran: Int?
)

/** Satu entri katalog dzikir (assets/dzikir_katalog.json). */
data class DzikirKatalogEntry(
    /** Dipakai apa adanya sebagai nama_detail di logs. */
    val slug: String,
    val nama: String,
    val waktuDefault: String,
    val targetDefault: Int?,
    val statusValidasi: String,
    val bacaan: List<BacaanDzikir>
) {
    /**
     * Mode "layar" butuh bacaan untuk ditampilkan. Selama bacaan masih kosong, hanya
     * mode "centang" yang boleh dipilih — aplikasi tidak menampilkan layar dzikir kosong.
     */
    val modeTersedia: List<String>
        get() = if (bacaan.isEmpty()) {
            listOf(ResearchSchema.ModePencatatan.CENTANG)
        } else {
            listOf(ResearchSchema.ModePencatatan.LAYAR, ResearchSchema.ModePencatatan.CENTANG)
        }

    val bolehModeLayar: Boolean get() = ResearchSchema.ModePencatatan.LAYAR in modeTersedia
}

object DzikirKatalog {
    const val ASSET_PATH = "dzikir_katalog.json"
    const val STATUS_MENUNGGU_VALIDASI = "MENUNGGU_VALIDASI_PAKAR"

    /** Baca katalog dari assets. Katalog rusak/hilang = daftar kosong, bukan crash. */
    fun load(context: Context): List<DzikirKatalogEntry> =
        runCatching {
            context.assets.open(ASSET_PATH).bufferedReader(Charsets.UTF_8).use { parse(it.readText()) }
        }.onFailure { runCatching { com.islami.Aha.util.logCrashlyticsSafe(it) } }
            .getOrDefault(emptyList())

    /**
     * Parse isi JSON. Entri dengan slug/waktu di luar skema dibuang (aturan Firestore akan
     * menolaknya juga), bacaan yang tidak lengkap dibuang, dan `bacaan` kosong itu sah.
     */
    fun parse(json: String): List<DzikirKatalogEntry> {
        val root = JSONObject(json)
        val entri = root.optJSONArray("entri") ?: return emptyList()
        val slugSah = ResearchSchema.DETAIL_PER_JENIS.getValue(ResearchSchema.Jenis.DZIKIR)
        return (0 until entri.length()).mapNotNull { i ->
            val o = entri.optJSONObject(i) ?: return@mapNotNull null
            val slug = o.optString("slug")
            val waktu = o.optString("waktu_default")
            if (slug !in slugSah || waktu !in ResearchSchema.WAKTU_DZIKIR) return@mapNotNull null
            DzikirKatalogEntry(
                slug = slug,
                nama = o.optString("nama").ifBlank { slug },
                waktuDefault = waktu,
                targetDefault = o.optPositiveIntOrNull("target_default")
                    ?.takeIf { it in ResearchSchema.TARGET_HITUNGAN },
                statusValidasi = o.optString("status_validasi").ifBlank { STATUS_MENUNGGU_VALIDASI },
                bacaan = parseBacaan(o.optJSONArray("bacaan"))
            )
        }.distinctBy { it.slug }
    }

    private fun parseBacaan(arr: JSONArray?): List<BacaanDzikir> {
        arr ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val arab = o.optString("arab").trim()
            val sumber = o.optString("sumber").trim()
            // Tanpa teks atau tanpa sumber rujukan = tidak ditampilkan.
            if (arab.isEmpty() || sumber.isEmpty()) return@mapNotNull null
            BacaanDzikir(
                arab = arab,
                latin = o.optString("latin").trim(),
                arti = o.optString("arti").trim(),
                sumber = sumber,
                jumlahAnjuran = o.optPositiveIntOrNull("jumlah_anjuran")
            )
        }
    }

    private fun JSONObject.optPositiveIntOrNull(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key, 0).takeIf { it > 0 }
}
