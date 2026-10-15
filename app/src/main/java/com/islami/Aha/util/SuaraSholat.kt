package com.islami.Aha.util

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.islami.Aha.R

/**
 * Suara pengingat per waktu sholat fardhu. Tiap waktu punya channel notifikasi sendiri
 * (Android 8+ mengunci suara di channel), jadi mengganti suara = membuat channel baru
 * dengan nomor versi baru. Channel lama dihapus; memakai ulang id yang sama akan
 * memulihkan setelan lama (perilaku Android), karena itu id selalu berversi.
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

            /** Adzan Subuh hanya ditawarkan untuk Subuh (lafal tatswib khusus Subuh). */
            fun untukWaktu(slug: String) =
                if (slug == "subuh") listOf(SUBUH_MALMO) + entries.filter { it != SUBUH_MALMO }
                else entries.filter { it != SUBUH_MALMO }
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


    private const val TAG = "SuaraSholat"
    private const val PREFS = "suara_sholat"
    private const val GROUP_ID = "waktu_sholat"

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

    fun setelan(context: Context, slug: String): Setelan {
        val p = prefs(context)
        val pilihan = Pilihan.from(p.getString("pilihan_$slug", null))
        val uri = p.getString("uri_$slug", null)?.let(Uri::parse)
        val rekaman = Rekaman.from(p.getString("rekaman_$slug", null), slug)
        return Setelan(pilihan, uri, p.getString("nama_$slug", null), rekaman)
    }

    fun simpanRekaman(context: Context, slug: String, rekaman: Rekaman) {
        prefs(context).edit().putString("rekaman_$slug", rekaman.id).apply()
        simpan(context, slug, Pilihan.ADZAN)
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

    fun ringkasan(context: Context, slug: String): String {
        val s = setelan(context, slug)
        return when (s.pilihan) {
            Pilihan.ADZAN -> s.rekaman.nama.substringBefore(" · ")
            Pilihan.NADA_HP -> if (s.uri == null) "Nada default" else (s.namaFile ?: "Nada HP")
            Pilihan.FILE_SENDIRI -> s.namaFile ?: "File sendiri"
            Pilihan.SENYAP -> "Senyap"
        }
    }

    fun simpan(context: Context, slug: String, pilihan: Pilihan, uri: Uri? = null, nama: String? = null) {
        val lama = setelan(context, slug)
        // Lepaskan izin file lama bila tidak dipakai lagi.
        if (lama.pilihan == Pilihan.FILE_SENDIRI && lama.uri != null && lama.uri != uri) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(lama.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        prefs(context).edit()
            .putString("pilihan_$slug", pilihan.value)
            .putString("uri_$slug", uri?.toString())
            .putString("nama_$slug", nama)
            .apply()
        if (pilihan == Pilihan.FILE_SENDIRI && uri != null) izinkanSystemUi(context, uri)
        bangunUlangChannel(context, slug)
    }

    /** Untuk file milik pengguna: pastikan masih bisa dibaca; kalau tidak, kembali ke nada default. */
    fun pastikanFileMasihAda(context: Context, slug: String) {
        val s = setelan(context, slug)
        if (s.pilihan != Pilihan.FILE_SENDIRI) return
        val bisaDibaca = s.uri != null && runCatching {
            context.contentResolver.openInputStream(s.uri)?.use { true } ?: false
        }.getOrDefault(false)
        if (!bisaDibaca) {
            Log.w(TAG, "File suara $slug tidak bisa dibaca lagi, kembali ke nada default")
            simpan(context, slug, Pilihan.NADA_HP)
        }
    }

    private fun izinkanSystemUi(context: Context, uri: Uri) {
        // Pemutar suara notifikasi ada di proses sistem; beri izin baca eksplisit.
        runCatching {
            context.grantUriPermission("com.android.systemui", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // ---------------------------------------------------------------- channel

    /** Penanda jalur volume kanal; nilai berbeda = kanal lama dibangun ulang. */
    private const val JALUR_VOLUME = "alarm"

    fun channelId(context: Context, slug: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return NotificationScheduler.CHANNEL_ID
        val versi = prefs(context).getInt("versi_$slug", 1)
        val id = "sholat_${slug}_v$versi"
        val nm = context.getSystemService(NotificationManager::class.java)
        // Channel dibangun dengan suara lain atau jalur volume lama (mis. versi aplikasi lama): bangun ulang.
        if (prefs(context).getString("uri_kanal_$slug", null) != soundUri(context, slug)?.toString() ||
            prefs(context).getString("jalur_kanal_$slug", null) != JALUR_VOLUME
        ) {
            bangunUlangChannel(context, slug)
            return "sholat_${slug}_v${prefs(context).getInt("versi_$slug", 1)}"
        }
        if (nm.getNotificationChannel(id) == null) buatChannel(context, slug, id)
        return id
    }

    fun bangunUlangChannel(context: Context, slug: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val p = prefs(context)
        val versiLama = p.getInt("versi_$slug", 1)
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.deleteNotificationChannel("sholat_${slug}_v$versiLama")
        p.edit().putInt("versi_$slug", versiLama + 1).commit()
        buatChannel(context, slug, "sholat_${slug}_v${versiLama + 1}")
    }

    /** Getar diatur global di Pengaturan; dipanggil saat setelan itu berubah. */
    fun bangunUlangSemua(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        WAKTU.keys.forEach { bangunUlangChannel(context, it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buatChannel(context: Context, slug: String, id: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannelGroup(NotificationChannelGroup(GROUP_ID, "Waktu sholat"))
        val getar = NotificationScheduler.getChannelSettings(context).vibrationEnabled
        val uri = soundUri(context, slug)
        val channel = NotificationChannel(id, "Waktu ${WAKTU[slug]}", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Pengingat sholat ${WAKTU[slug]} dengan tombol Sudah / Nanti / Tertinggal"
            group = GROUP_ID
            setSound(
                uri,
                AudioAttributes.Builder()
                    // Jalur volume ALARM: adzan tetap berbunyi walau volume notifikasi dibisukan
                    // atau HP dalam mode senyap. Pilihan "Senyap" di aplikasi tetap tanpa suara.
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(
                        if (setelan(context, slug).pilihan == Pilihan.NADA_HP) AudioAttributes.CONTENT_TYPE_SONIFICATION
                        else AudioAttributes.CONTENT_TYPE_MUSIC
                    )
                    .build()
            )
            enableVibration(getar)
            vibrationPattern = if (getar) longArrayOf(0, 180, 80, 180) else longArrayOf(0)
        }
        nm.createNotificationChannel(channel)
        prefs(context).edit()
            .putString("uri_kanal_$slug", uri?.toString())
            .putString("jalur_kanal_$slug", JALUR_VOLUME)
            .apply()
    }
}
