package com.islami.Aha.util

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Channel notifikasi Aha. ID di sini PERMANEN setelah rilis: Android menyimpan setelan pengguna
 * (suara, getar, prioritas, Jangan Ganggu) per ID, dan aplikasi tidak bisa mengubah suara atau
 * getar channel yang sudah dibuat. Pengguna mengatur nada tiap jenis lewat Pengaturan Android.
 *
 *  - Grup "Sholat wajib": satu channel per waktu (sholat_wajib_subuh … sholat_wajib_isya), karena
 *    suara melekat di channel dan Subuh memakai adzan Subuh. Jalur volume alarm.
 *  - sholat_sunnah   : sholat sunnah & qadha (termasuk Tarawih).
 *  - puasa_sunnah    : pengingat puasa yang disetel pengguna (puasa sunnah, juga habit puasa
 *                      Ramadan). Notifikasi biasa: pengingat malam hari tidak berbunyi seperti alarm.
 *  - sahur           : pengingat sahur (fitur Ramadan, belum ada pengirimnya). Berkarakter alarm:
 *                      prioritas tertinggi, jalur volume alarm, kategori alarm.
 *  - dzikir_tilawah  : dzikir dan tilawah.
 *  - mode_cuti       : pengingat untuk mengakhiri Mode Cuti (bukan pengingat ibadah).
 *  - aha_konfirmasi  : konfirmasi setelah "Sudah" di notifikasi (ID lama, tidak berubah).
 *
 * Migrasi dari channel lama (habit_reminder_channel[_vN], sholat_{waktu}_vN, grup waktu_sholat,
 * dan puasa_sahur dari build uji sebelum channel puasa dipecah):
 * lihat [pastikan].
 */
object KanalNotifikasi {
    const val GRUP_SHOLAT_WAJIB = "grup_sholat_wajib"
    const val SHOLAT_SUNNAH = "sholat_sunnah"
    const val PUASA_SUNNAH = "puasa_sunnah"
    const val SAHUR = "sahur"
    const val DZIKIR_TILAWAH = "dzikir_tilawah"
    const val MODE_CUTI = "mode_cuti"
    const val KONFIRMASI = "aha_konfirmasi"

    /** slug waktu sholat → ID channel ("subuh" → "sholat_wajib_subuh"). */
    fun sholatWajib(slug: String) = "sholat_wajib_$slug"

    val WAKTU_SHOLAT = listOf("subuh", "dzuhur", "ashar", "maghrib", "isya")

    /** Semua ID channel milik versi ini; channel lain milik Aha dianggap peninggalan versi lama. */
    val SEMUA: Set<String> = WAKTU_SHOLAT.map(::sholatWajib).toSet() +
        setOf(SHOLAT_SUNNAH, PUASA_SUNNAH, SAHUR, DZIKIR_TILAWAH, MODE_CUTI, KONFIRMASI)

    enum class Jenis { SHOLAT_WAJIB, SHOLAT_SUNNAH, PUASA_SUNNAH, SAHUR, DZIKIR_TILAWAH, MODE_CUTI }

    /** Jenis yang berbunyi seperti alarm (kategori alarm, prioritas maksimum). */
    fun berkarakterAlarm(jenis: Jenis): Boolean = jenis == Jenis.SAHUR

    /** Habit bawaan (tabel `habits`) menurut kategorinya. */
    fun jenisHabitBawaan(kategori: String): Jenis = when (kategori) {
        "Sholat Fardhu" -> Jenis.SHOLAT_WAJIB
        // Pengingat habit puasa (termasuk puasa Ramadan) disetel pengguna: notifikasi biasa.
        // Sahur punya jenis sendiri (fitur pengingat sahur).
        "Puasa Wajib", "Puasa Sunnah" -> Jenis.PUASA_SUNNAH
        else -> Jenis.SHOLAT_SUNNAH
    }

    /** Habit buatan pengguna (`user_habits`): "SHOLAT" / "PUASA"; dzikir & tilawah disiapkan. */
    fun jenisHabitSunnah(kategori: String?): Jenis = when (kategori?.uppercase()) {
        "PUASA" -> Jenis.PUASA_SUNNAH
        "DZIKIR", "TILAWAH" -> Jenis.DZIKIR_TILAWAH
        else -> Jenis.SHOLAT_SUNNAH
    }

    /** ID channel untuk [jenis]; sholat wajib butuh [slug] waktunya. */
    fun idUntuk(jenis: Jenis, slug: String? = null): String = when (jenis) {
        Jenis.SHOLAT_WAJIB -> sholatWajib(slug?.takeIf { it in WAKTU_SHOLAT } ?: "dzuhur")
        Jenis.SHOLAT_SUNNAH -> SHOLAT_SUNNAH
        Jenis.PUASA_SUNNAH -> PUASA_SUNNAH
        Jenis.SAHUR -> SAHUR
        Jenis.DZIKIR_TILAWAH -> DZIKIR_TILAWAH
        Jenis.MODE_CUTI -> MODE_CUTI
    }

    /** Channel lama yang diganti versi ini (dipakai migrasi dan tes). */
    fun termasukChannelLama(id: String): Boolean =
        id !in SEMUA && (id == "habit_reminder_channel" || id.startsWith("habit_reminder_channel_v") ||
            id == "puasa_sahur" ||
            Regex("^sholat_(subuh|dzuhur|ashar|maghrib|isya)_v\\d+$").matches(id))

    private const val TAG = "KanalNotifikasi"
    private const val GRUP_LAMA = "waktu_sholat"
    private const val PREFS_SUARA_LAMA = "suara_sholat"
    private val GETAR = longArrayOf(0, 180, 80, 180)
    private val GETAR_ALARM = longArrayOf(0, 600, 300, 600, 300, 600)

    /**
     * Dipanggil saat aplikasi mulai dan setelah HP menyala ulang. Idempoten.
     *
     * Migrasi: channel baru dibuat SEKALI dengan setelan awal yang meneruskan pilihan pengguna di
     * versi lama (suara per waktu sholat dari pemilih lama, channel yang dimatikan tetap mati,
     * suara/getar umum). Sesudah itu channel lama dan grup lama dihapus, sehingga tidak ada yang
     * tertinggal di Pengaturan Android. Setelah channel baru ada, aplikasi tidak menyentuhnya lagi.
     */
    fun pastikan(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching { pastikanO(context) }.onFailure { Log.e(TAG, "Gagal menyiapkan channel", it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun pastikanO(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val lama = nm.notificationChannels.filter { termasukChannelLama(it.id) }
        val umumLamaMati = lama.any { it.id.startsWith("habit_reminder_channel") && it.importance == NotificationManager.IMPORTANCE_NONE }
        val puasaLamaMati = umumLamaMati || lama.any { it.id == "puasa_sahur" && it.importance == NotificationManager.IMPORTANCE_NONE }
        val prefs = SecurePrefsProvider.get(context)
        val umumSenyap = prefs.getString(NotificationScheduler.KEY_NOTIFICATION_SOUND, null) == "silent"
        val getar = prefs.getBoolean(NotificationScheduler.KEY_NOTIFICATION_VIBRATION, true)

        nm.createNotificationChannelGroup(NotificationChannelGroup(GRUP_SHOLAT_WAJIB, "Sholat wajib"))
        WAKTU_SHOLAT.forEach { slug ->
            val id = sholatWajib(slug)
            if (nm.getNotificationChannel(id) != null) return@forEach
            val waktuLamaMati = lama.any { it.id.startsWith("sholat_${slug}_v") && it.importance == NotificationManager.IMPORTANCE_NONE }
            nm.createNotificationChannel(
                NotificationChannel(id, SuaraSholat.WAKTU[slug] ?: slug, if (waktuLamaMati) NotificationManager.IMPORTANCE_NONE else NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Pengingat sholat ${SuaraSholat.WAKTU[slug]} dengan tombol Sudah / Nanti / Tertinggal"
                    group = GRUP_SHOLAT_WAJIB
                    // Jalur volume ALARM: adzan tetap berbunyi walau notifikasi dibisukan / HP senyap.
                    setSound(suaraSholat(context, slug), atributAlarm(musik = true))
                    atur(getar, GETAR)
                }
            )
        }
        buatBila(nm, SHOLAT_SUNNAH, "Sholat sunnah", "Pengingat sholat sunnah dan qadha yang kamu atur",
            if (umumLamaMati) NotificationManager.IMPORTANCE_NONE else NotificationManager.IMPORTANCE_HIGH,
            if (umumSenyap) null else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), atributNotifikasi(), getar)
        // Pengingat puasa yang disetel pengguna (sering malam hari): notifikasi biasa.
        buatBila(nm, PUASA_SUNNAH, "Puasa sunnah", "Pengingat puasa yang kamu atur",
            if (puasaLamaMati) NotificationManager.IMPORTANCE_NONE else NotificationManager.IMPORTANCE_HIGH,
            if (umumSenyap) null else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), atributNotifikasi(), getar)
        // Sahur jam 3 pagi tidak boleh kalah oleh mode Jangan Ganggu: karakter alarm (prioritas
        // tertinggi yang boleh dipakai aplikasi, suara alarm di jalur volume alarm). Notifikasinya
        // juga diberi kategori alarm (lihat NotificationReceiver).
        buatBila(nm, SAHUR, "Sahur", "Pengingat sahur di bulan Ramadan. Berbunyi seperti alarm.",
            NotificationManager.IMPORTANCE_HIGH,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            atributAlarm(musik = false), getarAlarm = true, getar = true)
        buatBila(nm, DZIKIR_TILAWAH, "Dzikir dan tilawah", "Pengingat dzikir dan tilawah",
            if (umumLamaMati) NotificationManager.IMPORTANCE_NONE else NotificationManager.IMPORTANCE_DEFAULT,
            if (umumSenyap) null else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), atributNotifikasi(), getar)
        buatBila(nm, MODE_CUTI, "Mode Cuti", "Pengingat untuk mengakhiri Mode Cuti",
            NotificationManager.IMPORTANCE_DEFAULT,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), atributNotifikasi(), getar)

        // Bersih-bersih: channel & grup versi lama (sesudah setelannya diteruskan di atas).
        lama.forEach { nm.deleteNotificationChannel(it.id) }
        if (nm.notificationChannelGroups.any { it.id == GRUP_LAMA }) nm.deleteNotificationChannelGroup(GRUP_LAMA)
    }

    /**
     * Suara sholat [slug]: pilihan pengguna di versi lama (pemilih suara yang sudah dihapus), atau
     * adzan bawaan (Subuh: adzan Subuh). Untuk channel baru dan untuk Android 7 ke bawah.
     */
    fun suaraSholat(context: Context, slug: String): Uri? {
        val pernahMemilih = context.getSharedPreferences(PREFS_SUARA_LAMA, Context.MODE_PRIVATE).contains("pilihan_$slug")
        return if (pernahMemilih) SuaraSholat.soundUri(context, slug)
        else SuaraSholat.adzanUri(context, SuaraSholat.Rekaman.bawaanUntuk(slug))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buatBila(
        nm: NotificationManager, id: String, nama: String, keterangan: String, importance: Int,
        suara: Uri?, atribut: AudioAttributes, getar: Boolean, getarAlarm: Boolean = false
    ) {
        if (nm.getNotificationChannel(id) != null) return
        nm.createNotificationChannel(NotificationChannel(id, nama, importance).apply {
            description = keterangan
            setSound(suara, atribut)
            atur(getar, if (getarAlarm) GETAR_ALARM else GETAR)
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun NotificationChannel.atur(getar: Boolean, pola: LongArray) {
        enableVibration(getar)
        vibrationPattern = if (getar) pola else longArrayOf(0)
    }

    private fun atributAlarm(musik: Boolean): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(if (musik) AudioAttributes.CONTENT_TYPE_MUSIC else AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun atributNotifikasi(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** Channel itu dimatikan pengguna di Pengaturan Android. */
    fun dimatikan(context: Context, id: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val nm = context.getSystemService(NotificationManager::class.java)
        return nm.getNotificationChannel(id)?.importance == NotificationManager.IMPORTANCE_NONE
    }

    /** Halaman Pengaturan Android untuk satu channel (suara, getar, Jangan Ganggu). */
    fun intentPengaturan(context: Context, id: String): android.content.Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.content.Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, id)
        } else {
            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
        }
}
