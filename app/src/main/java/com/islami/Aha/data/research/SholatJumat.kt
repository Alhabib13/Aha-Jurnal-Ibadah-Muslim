package com.islami.Aha.data.research

import android.content.SharedPreferences
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Slot sholat tengah hari: Jumat atau Dzuhur.
 *
 * - Laki-laki di hari Jumat: slot tengah hari = Jumat, kecuali dia memilih menggantinya dengan
 *   Dzuhur untuk hari itu (bepergian, sakit, dsb.).
 * - Perempuan, atau hari selain Jumat: selalu Dzuhur.
 *
 * Jumat dan Dzuhur saling meniadakan: dalam satu hari hanya satu yang boleh punya baris log
 * (ditegakkan di DefaultHabitCompletionRepository dan di firestore.rules). Kelengkapan harian
 * tetap lima slot karena keduanya memakai habit bawaan yang sama ("Sholat Dzuhur").
 */
object SholatJumat {
    const val NAMA_JUMAT = "Sholat Jumat"
    const val NAMA_DZUHUR = "Sholat Dzuhur"

    fun hariJumat(tanggal: String): Boolean =
        runCatching { LocalDate.parse(tanggal).dayOfWeek == DayOfWeek.FRIDAY }.getOrDefault(false)

    /**
     * nama_detail slot tengah hari pada [tanggal].
     * [gantiDzuhur] = pilihan pengguna untuk tanggal itu; null = belum memilih (bawaan: Jumat).
     */
    fun detailTengahHari(tanggal: String, lakiLaki: Boolean, gantiDzuhur: Boolean?): String =
        if (lakiLaki && hariJumat(tanggal) && gantiDzuhur != true) ResearchSchema.Detail.JUMAT
        else ResearchSchema.Detail.DZUHUR

    /** Lawan dari [detail] di slot tengah hari, atau null bila [detail] bukan slot tengah hari. */
    fun lawan(detail: String?): String? = when (detail) {
        ResearchSchema.Detail.JUMAT -> ResearchSchema.Detail.DZUHUR
        ResearchSchema.Detail.DZUHUR -> ResearchSchema.Detail.JUMAT
        else -> null
    }

    fun namaHabit(detail: String): String =
        if (detail == ResearchSchema.Detail.JUMAT) NAMA_JUMAT else NAMA_DZUHUR

    /**
     * Pilihan "ganti dengan Dzuhur" belum tersimpan (mis. setelah instal ulang): disimpulkan dari
     * catatan yang ada. Hanya baris dzuhur di hari Jumat = sudah diganti; hanya jumat = belum;
     * tidak ada catatan = belum memilih.
     */
    fun simpulkanGanti(adaJumat: Boolean, adaDzuhur: Boolean): Boolean? = when {
        adaDzuhur && !adaJumat -> true
        adaJumat && !adaDzuhur -> false
        else -> null
    }
}

/** Pilihan "ganti Jumat dengan Dzuhur" per tanggal, hanya di HP. */
class PilihanJumat(private val prefs: SharedPreferences) {
    private fun kunci(tanggal: String) = "$AWALAN$tanggal"

    /** true = diganti Dzuhur, false = tetap Jumat, null = belum memilih. */
    fun gantiDzuhur(tanggal: String): Boolean? =
        if (prefs.contains(kunci(tanggal))) prefs.getBoolean(kunci(tanggal), false) else null

    fun simpan(tanggal: String, gantiDzuhur: Boolean) {
        val batas = LocalDate.now().minusDays(SIMPAN_HARI).toString()
        val editor = prefs.edit()
        // Pilihan lama tidak dibutuhkan lagi (catatan di log sudah cukup untuk menyimpulkannya).
        prefs.all.keys.filter { it.startsWith(AWALAN) && it.removePrefix(AWALAN) < batas }.forEach { editor.remove(it) }
        editor.putBoolean(kunci(tanggal), gantiDzuhur).apply()
    }

    companion object {
        private const val AWALAN = "jumat_ganti_dzuhur_"
        private const val SIMPAN_HARI = 60L
    }
}
