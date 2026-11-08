package com.islami.Aha.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Panduan "Agar pengingat tidak terlewat" untuk HP yang manajemen baterainya agresif
 * (Xiaomi/Redmi/POCO, Oppo/Realme, Vivo/iQOO, Huawei/Honor): nyalakan Autostart dan bebaskan Aha
 * dari penghemat baterai. Tanpa ini, setelah aplikasi diusap dari daftar terbaru, alarm lokal
 * bisa tidak berbunyi dan pesan FCM tidak dikirim ke aplikasi yang dihentikan.
 *
 * Hanya urusan perangkat: tanda "sudah ditampilkan" disimpan di SharedPreferences, tidak pernah
 * dikirim ke Firestore.
 */
object PanduanPengingat {
    const val KEY_DITAMPILKAN = "panduan_autostart_ditampilkan"

    private val MEREK = setOf("xiaomi", "redmi", "poco", "oppo", "realme", "vivo", "iqoo", "huawei", "honor")

    /** true bila pabrikan atau merek HP termasuk yang butuh panduan. */
    fun berlakuUntuk(manufaktur: String?, merek: String?): Boolean =
        listOfNotNull(manufaktur, merek).any { it.trim().lowercase() in MEREK }

    fun berlaku(): Boolean = berlakuUntuk(Build.MANUFACTURER, Build.BRAND)

    fun sudahDitampilkan(prefs: SharedPreferences): Boolean = prefs.getBoolean(KEY_DITAMPILKAN, false)

    fun tandaiDitampilkan(prefs: SharedPreferences) {
        prefs.edit().putBoolean(KEY_DITAMPILKAN, true).apply()
    }

    /** Onboarding menampilkan panduan sekali, hanya di HP yang membutuhkannya. */
    fun perluDitampilkan(prefs: SharedPreferences): Boolean = berlaku() && !sudahDitampilkan(prefs)

    /** Letak pengaturan baterai per aplikasi, sesuai keluarga merek. */
    fun petunjukBaterai(manufaktur: String? = Build.MANUFACTURER, merek: String? = Build.BRAND): String {
        val m = listOfNotNull(manufaktur, merek).joinToString(" ").lowercase()
        return when {
            listOf("xiaomi", "redmi", "poco").any { it in m } -> "Pilih Penghemat baterai › Tanpa pembatasan."
            listOf("oppo", "realme").any { it in m } -> "Pilih Penggunaan baterai › izinkan aktivitas latar belakang."
            listOf("vivo", "iqoo").any { it in m } -> "Pilih Baterai › izinkan penggunaan daya tinggi di latar belakang."
            listOf("huawei", "honor").any { it in m } -> "Pilih Peluncuran aplikasi › atur manual, nyalakan semua."
            else -> "Pilih Baterai › Tanpa pembatasan."
        }
    }

    /** Halaman Info aplikasi Aha (memuat pengaturan baterai per aplikasi di semua merek). */
    fun bukaPengaturanBaterai(context: Context): Boolean = runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.isSuccess

    fun bukaAutostart(context: Context): Boolean = AutoStartHelper.openAutoStartSettings(context)

    // ---------- status pengaturan (supaya layar panduan menampilkan centang bila sudah aktif) ----------

    /** AKTIF/BELUM = terbaca dari sistem; TIDAK_DIKETAHUI = HP tidak menyediakan cara membacanya. */
    enum class Status { AKTIF, BELUM, TIDAK_DIKETAHUI }

    const val KEY_AUTOSTART_DIBUKA = "panduan_autostart_dibuka"
    const val KEY_BATERAI_DIBUKA = "panduan_baterai_dibuka"

    /** Pengecualian penghemat baterai Android (MIUI "Tanpa pembatasan" juga mencatatnya di sini). */
    fun statusBaterai(context: Context): Status =
        if (AutoStartHelper.isIgnoringBatteryOptimizations(context)) Status.AKTIF else Status.BELUM

    /**
     * Autostart hanya bisa dibaca di MIUI/HyperOS (op 10008 "mulai otomatis"). Merek lain tidak
     * menyediakannya: TIDAK_DIKETAHUI, layar menampilkan "sudah dibuka" berdasar tanda di HP.
     */
    fun statusAutostart(context: Context): Status {
        if (!keluargaXiaomi(Build.MANUFACTURER, Build.BRAND)) return Status.TIDAK_DIKETAHUI
        val mode = runCatching {
            val ops = context.getSystemService(android.app.AppOpsManager::class.java)
            val cek = android.app.AppOpsManager::class.java.getMethod(
                "checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java
            )
            cek.invoke(ops, OP_AUTOSTART_MIUI, android.os.Process.myUid(), context.packageName) as Int
        }.getOrNull()
        return statusDariModeMiui(mode)
    }

    private const val OP_AUTOSTART_MIUI = 10008

    internal fun keluargaXiaomi(manufaktur: String?, merek: String?): Boolean =
        listOfNotNull(manufaktur, merek).any { it.trim().lowercase() in setOf("xiaomi", "redmi", "poco") }

    /** Mode AppOps MIUI: 0 = diizinkan, 1 = ditolak; lainnya / gagal dibaca = tidak diketahui. */
    internal fun statusDariModeMiui(mode: Int?): Status = when (mode) {
        android.app.AppOpsManager.MODE_ALLOWED -> Status.AKTIF
        android.app.AppOpsManager.MODE_IGNORED -> Status.BELUM
        else -> Status.TIDAK_DIKETAHUI
    }

    /** Langkah dianggap beres: terbaca aktif, atau tak terbaca tapi sudah pernah dibuka. */
    fun beres(status: Status, dibuka: Boolean): Boolean =
        status == Status.AKTIF || (status == Status.TIDAK_DIKETAHUI && dibuka)

    /** Keterangan di bawah judul langkah. */
    fun keteranganStatus(status: Status, dibuka: Boolean): String = when {
        status == Status.AKTIF -> "Sudah aktif"
        status == Status.BELUM && dibuka -> "Belum aktif · ketuk untuk membuka lagi"
        status == Status.BELUM -> "Belum aktif · ketuk untuk membuka"
        dibuka -> "Sudah dibuka · pastikan Aha sudah dinyalakan"
        else -> "Ketuk untuk membuka"
    }

    fun tandaiDibuka(prefs: SharedPreferences, key: String) {
        prefs.edit().putBoolean(key, true).apply()
    }
}
