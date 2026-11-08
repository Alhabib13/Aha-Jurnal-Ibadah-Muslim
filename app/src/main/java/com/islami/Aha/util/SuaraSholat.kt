package com.islami.Aha.util

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import com.islami.Aha.R

/**
 * Rekaman adzan bawaan dan pembaca setelan suara per waktu sholat dari versi lama.
 *
 * Sejak channel notifikasi dipecah (lihat [KanalNotifikasi]), suara diatur pengguna lewat
 * Pengaturan Android dan tidak ada lagi pemilih suara di aplikasi. Setelan lama (prefs
 * "suara_sholat") hanya dibaca sekali untuk suara awal channel baru, dan untuk Android 7 ke bawah.
 */
object SuaraSholat {
    enum class Pilihan(val value: String, val label: String) {
        ADZAN("adzan", "Adzan"),
        NADA_HP("nada_hp", "Nada notifikasi HP"),
        FILE_SENDIRI("file", "File suaraku sendiri"),
        SENYAP("senyap", "Senyap (hanya getar)");

        companion object {
            fun from(value: String?) = entries.firstOrNull { it.value == value } ?: NADA_HP
        }
    }

    data class Setelan(val pilihan: Pilihan, val uri: Uri?, val namaFile: String?, val rekaman: Rekaman)

    /** Rekaman adzan bawaan. Semua berlisensi terbuka; volume diseragamkan (-16 LUFS). */
    enum class Rekaman(val id: String, val nama: String, val durasi: String, val raw: Int, val atribusi: String) {
        AAQIB(
            "aaqib", "Adzan 1", "1:27", R.raw.adzan_aaqib_azeez,
            "Adzan 1: “The Adhan – Muslim Call to Prayer – Aaqib Azeez”, rekaman Atcovi (Wikimedia Commons), CC BY-SA 4.0; volume diseragamkan."
        ),
        FREESOUND(
            "freesound", "Adzan 2", "2:31", R.raw.adzan_umum_freesound,
            "Adzan 2: “Athan Of Muslims” oleh Senan_2009 (Freesound), CC0; volume diseragamkan."
        ),
        SHALQAR(
            "shalqar", "Adzan 3 · Masjid Shalqar", "2:15", R.raw.adzan_shalqar,
            "Adzan 3: “Adhan in Shalqar mosque”, rekaman Esetok (Wikimedia Commons), CC BY-SA 4.0; dipotong dan volume diseragamkan."
        ),
        SUBUH_MALMO(
            "subuh_malmo", "Adzan Subuh · Masjid Malmö", "4:03", R.raw.adzan_subuh_malmo,
            "Adzan Subuh: “Eid al-Fitr Fajr azan at Malmö Mosque – 19 August 2012”, Islamic Center Malmö (Wikimedia Commons), CC BY 3.0; dipotong dan volume diseragamkan."
        );

        companion object {
            fun from(id: String?, slug: String) = entries.firstOrNull { it.id == id } ?: bawaanUntuk(slug)

            /** Subuh memakai adzan Subuh (ada "ash-shalatu khairun minan naum"). */
            fun bawaanUntuk(slug: String) = if (slug == "subuh") SUBUH_MALMO else AAQIB
        }
    }

    /** slug → nama tampilan. Urutan = urutan waktu sholat. */
    val WAKTU = linkedMapOf(
        "subuh" to "Subuh",
        "dzuhur" to "Dzuhur",
        "ashar" to "Ashar",
        "maghrib" to "Maghrib",
        "isya" to "Isya"
    )


    private const val PREFS = "suara_sholat"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** "Sholat Subuh" → "subuh"; null bila bukan salah satu dari lima waktu. */
    fun slugDariNama(nama: String): String? {
        val n = nama.lowercase()
        return WAKTU.keys.firstOrNull { n.contains(it) }
    }

    /** Dirujuk dengan nama, bukan nomor resource (nomor bisa bergeser antar versi aplikasi). */
    fun adzanUri(context: Context, rekaman: Rekaman): Uri = Uri.parse(
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/" +
            context.resources.getResourceEntryName(rekaman.raw)
    )

    private fun setelan(context: Context, slug: String): Setelan {
        val p = prefs(context)
        val pilihan = Pilihan.from(p.getString("pilihan_$slug", null))
        val uri = p.getString("uri_$slug", null)?.let(Uri::parse)
        val rekaman = Rekaman.from(p.getString("rekaman_$slug", null), slug)
        return Setelan(pilihan, uri, p.getString("nama_$slug", null), rekaman)
    }

    /** Uri yang benar-benar dibunyikan; null = senyap. */
    fun soundUri(context: Context, slug: String): Uri? {
        val s = setelan(context, slug)
        return when (s.pilihan) {
            Pilihan.ADZAN -> adzanUri(context, s.rekaman)
            Pilihan.NADA_HP -> s.uri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            Pilihan.FILE_SENDIRI -> s.uri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            Pilihan.SENYAP -> null
        }
    }
}
