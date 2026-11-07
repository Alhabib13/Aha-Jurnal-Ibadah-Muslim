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
}
