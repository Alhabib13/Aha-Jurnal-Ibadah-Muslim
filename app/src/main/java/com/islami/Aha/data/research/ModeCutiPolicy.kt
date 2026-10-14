package com.islami.Aha.data.research

/**
 * Cakupan Mode Cuti (haid/nifas) per jenis_ibadah — SATU-SATUNYA tempat konfigurasinya.
 *
 * MENUNGGU_VALIDASI_PAKAR (lihat FITUR_DZIKIR_README.md, bagian "Mode Cuti"):
 * - dzikir TETAP AKTIF. Alasan sementara: dzikir tidak mensyaratkan suci dari hadats besar,
 *   jadi menyembunyikannya bisa terbaca sebagai "dilarang". Bila pakar memutuskan lain,
 *   cukup ubah nilai di peta ini; kode UI/statistik membaca dari sini.
 * - tilawah & iqro TETAP AKTIF: perilaku v1.2.0 (khilafiyah; tidak disembunyikan = netral).
 * - sholat_wajib, sholat_sunnah, qadha_sholat, puasa NONAKTIF: perilaku lama aplikasi.
 *
 * Catatan: kode lama sholat/puasa (HomeUiState, StatisticViewModel, ProfileViewModel,
 * HomeViewModel) masih memeriksa kategori secara langsung; nilainya sama dengan peta ini.
 */
object ModeCutiPolicy {
    enum class Perlakuan {
        /** Disembunyikan dan keluar dari penyebut rasio selama Mode Cuti. */
        NONAKTIF,
        /** Tetap tampil dan tetap masuk rasio selama Mode Cuti. */
        TETAP_AKTIF
    }

    val CAKUPAN: Map<String, Perlakuan> = mapOf(
        ResearchSchema.Jenis.SHOLAT_WAJIB to Perlakuan.NONAKTIF,
        ResearchSchema.Jenis.SHOLAT_SUNNAH to Perlakuan.NONAKTIF,
        ResearchSchema.Jenis.QADHA_SHOLAT to Perlakuan.NONAKTIF,
        ResearchSchema.Jenis.PUASA to Perlakuan.NONAKTIF,
        ResearchSchema.Jenis.DZIKIR to Perlakuan.TETAP_AKTIF,
        ResearchSchema.Jenis.TILAWAH to Perlakuan.TETAP_AKTIF,
        ResearchSchema.Jenis.IQRO to Perlakuan.TETAP_AKTIF
    )

    /** Jenis yang tidak terdaftar dianggap tetap aktif (tidak menyembunyikan apa pun diam-diam). */
    fun perlakuan(jenisIbadah: String): Perlakuan = CAKUPAN[jenisIbadah] ?: Perlakuan.TETAP_AKTIF

    /** true bila [jenisIbadah] tetap tampil & masuk rasio pada hari dengan Mode Cuti aktif. */
    fun aktif(jenisIbadah: String, modeCutiAktif: Boolean): Boolean =
        !modeCutiAktif || perlakuan(jenisIbadah) == Perlakuan.TETAP_AKTIF
}
