package com.islami.Aha.ui.onboarding

import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.ui.addhabit.SunnahCategoryType

/** Kelompok di halaman "Ibadah yang biasa kamu lakukan". */
enum class KelompokPilihan(val label: String) {
    SHOLAT("Sholat sunnah"),
    PUASA("Puasa sunnah"),
    DZIKIR("Dzikir"),
    TILAWAH("Al-Qur'an")
}

/**
 * Satu pilihan di onboarding. [kode] = nilai di users/{uid}.pilihan_awal
 * (ResearchSchema.Profil.PILIHAN_AWAL_VALUES).
 * [sunnah] terisi bila pilihan ini juga dibuat jadi habit sunnah (nama = nama preset Add Habit,
 * supaya pemetaan ke nama_detail sama persis). Dzikir dibuat lewat katalog dengan slug = [kode].
 * Tilawah/Iqro tidak punya habit (selalu tersedia di Beranda), hanya dicatat sebagai pilihan.
 */
data class OpsiPilihan(
    val kode: String,
    val label: String,
    val kelompok: KelompokPilihan,
    val sunnah: HabitSunnahAwal? = null
)

data class HabitSunnahAwal(val nama: String, val kategori: SunnahCategoryType, val frekuensi: String)

private const val SETIAP_HARI = "Setiap hari"

val OPSI_PILIHAN_AWAL: List<OpsiPilihan> = listOf(
    OpsiPilihan(ResearchSchema.Detail.DHUHA, "Dhuha", KelompokPilihan.SHOLAT,
        HabitSunnahAwal("Dhuha", SunnahCategoryType.SHOLAT, SETIAP_HARI)),
    OpsiPilihan(ResearchSchema.Detail.TAHAJUD, "Tahajud", KelompokPilihan.SHOLAT,
        HabitSunnahAwal("Tahajud", SunnahCategoryType.SHOLAT, SETIAP_HARI)),
    OpsiPilihan(ResearchSchema.Detail.WITIR, "Witir", KelompokPilihan.SHOLAT,
        HabitSunnahAwal("Witir", SunnahCategoryType.SHOLAT, SETIAP_HARI)),
    OpsiPilihan(ResearchSchema.Detail.RAWATIB, "Rawatib", KelompokPilihan.SHOLAT,
        HabitSunnahAwal("Rawatib", SunnahCategoryType.SHOLAT, SETIAP_HARI)),
    // Label frekuensi sama dengan hasil Add Habit untuk hari Senin & Kamis.
    OpsiPilihan(ResearchSchema.Detail.SENIN_KAMIS, "Senin Kamis", KelompokPilihan.PUASA,
        HabitSunnahAwal("Senin Kamis", SunnahCategoryType.PUASA, "Hari: Sen • Kam")),
    OpsiPilihan(ResearchSchema.Detail.AYYAMUL_BIDH, "Ayyamul Bidh", KelompokPilihan.PUASA,
        HabitSunnahAwal("Ayyamul Bidh", SunnahCategoryType.PUASA, SETIAP_HARI)),
    OpsiPilihan(ResearchSchema.Detail.DAUD, "Puasa Daud", KelompokPilihan.PUASA,
        HabitSunnahAwal("Puasa Daud", SunnahCategoryType.PUASA, SETIAP_HARI)),
    OpsiPilihan(ResearchSchema.Detail.DZIKIR_BADA_SHOLAT, "Dzikir setelah sholat fardhu", KelompokPilihan.DZIKIR),
    OpsiPilihan(ResearchSchema.Detail.DZIKIR_PAGI, "Dzikir pagi", KelompokPilihan.DZIKIR),
    OpsiPilihan(ResearchSchema.Detail.DZIKIR_PETANG, "Dzikir petang", KelompokPilihan.DZIKIR),
    OpsiPilihan(ResearchSchema.Detail.DZIKIR_SEBELUM_TIDUR, "Dzikir sebelum tidur", KelompokPilihan.DZIKIR),
    OpsiPilihan(ResearchSchema.Detail.ISTIGHFAR, "Istighfar", KelompokPilihan.DZIKIR),
    OpsiPilihan(ResearchSchema.Detail.SHOLAWAT, "Sholawat", KelompokPilihan.DZIKIR),
    OpsiPilihan(ResearchSchema.Detail.TAHLIL, "Tahlil", KelompokPilihan.DZIKIR),
    OpsiPilihan(ResearchSchema.Detail.ASMAUL_HUSNA, "Asmaul Husna", KelompokPilihan.DZIKIR),
    OpsiPilihan(ResearchSchema.Detail.BACAAN_HARIAN, "Membaca Al-Qur'an", KelompokPilihan.TILAWAH),
    OpsiPilihan(ResearchSchema.Detail.HAFALAN, "Menghafal Al-Qur'an", KelompokPilihan.TILAWAH),
    OpsiPilihan(ResearchSchema.Jenis.IQRO, "Belajar Iqro", KelompokPilihan.TILAWAH)
)
