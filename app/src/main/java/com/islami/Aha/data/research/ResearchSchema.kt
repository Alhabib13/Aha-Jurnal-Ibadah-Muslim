package com.islami.Aha.data.research

/**
 * Skema data penelitian (koleksi Firestore `users/{uid}` dan `logs/{logId}`).
 *
 * Nilai di sini HARUS sama persis dengan validasi di `firestore.rules`.
 * Mengubah salah satu tanpa yang lain membuat penulisan log ditolak server.
 */
object ResearchSchema {
    const val COLLECTION_USERS = "users"
    const val COLLECTION_LOGS = "logs"

    object Profil {
        const val JENIS_KELAMIN = "jenis_kelamin"
        const val RENTANG_UMUR = "rentang_umur"
        const val STATUS_AKTIVITAS = "status_aktivitas"
        const val TANGGAL_MULAI = "tanggal_mulai"

        const val LAKI_LAKI = "laki_laki"
        const val PEREMPUAN = "perempuan"

        val RENTANG_UMUR_VALUES = listOf("<15", "15-18", "19-25", "26-35", ">35")
        val STATUS_AKTIVITAS_VALUES = listOf("pelajar", "mahasiswa", "pekerja", "lainnya")
    }

    object Log {
        const val USER_ID = "user_id"
        const val TANGGAL = "tanggal"
        const val JENIS_IBADAH = "jenis_ibadah"
        const val NAMA_DETAIL = "nama_detail"
        const val NAMA_DETAIL_ASLI = "nama_detail_asli"
        const val STATUS = "status"
        const val JUMLAH = "jumlah"
        const val JILID = "jilid"
        const val JAM_PENCATATAN = "jam_pencatatan"
        const val SUMBER_PENCATATAN = "sumber_pencatatan"
    }

    object Jenis {
        const val SHOLAT_WAJIB = "sholat_wajib"
        const val SHOLAT_SUNNAH = "sholat_sunnah"
        const val PUASA = "puasa"
        const val DZIKIR = "dzikir"
        const val TILAWAH = "tilawah"
        const val IQRO = "iqro"
    }

    object Detail {
        const val SUBUH = "subuh"
        const val DZUHUR = "dzuhur"
        const val ASHAR = "ashar"
        const val MAGHRIB = "maghrib"
        const val ISYA = "isya"

        const val TAHAJUD = "tahajud"
        const val DHUHA = "dhuha"
        const val RAWATIB = "rawatib"
        const val WITIR = "witir"
        const val TARAWIH = "tarawih"
        const val TAUBAT = "taubat"
        const val ISTIKHARAH = "istikharah"

        const val SENIN_KAMIS = "senin_kamis"
        const val AYYAMUL_BIDH = "ayyamul_bidh"
        const val DAUD = "daud"
        const val SYAWAL = "syawal"
        const val RAMADAN = "ramadan"
        const val NAZAR = "nazar"

        const val QADHA = "qadha"
        const val SUNNAH_LAINNYA = "sunnah_lainnya"

        const val PAGI = "pagi"
        const val PETANG = "petang"
    }

    object Status {
        const val DIKERJAKAN = "dikerjakan"
        const val TIDAK = "tidak"
        const val UDZUR = "udzur"
    }

    object Sumber {
        const val DARI_NOTIFIKASI = "dari_notifikasi"
        const val BUKA_APLIKASI = "buka_aplikasi"
    }

    /** nama_detail yang sah per jenis_ibadah; tilawah & iqro tidak punya detail. */
    val DETAIL_PER_JENIS: Map<String, Set<String>> = mapOf(
        Jenis.SHOLAT_WAJIB to setOf(Detail.SUBUH, Detail.DZUHUR, Detail.ASHAR, Detail.MAGHRIB, Detail.ISYA),
        Jenis.SHOLAT_SUNNAH to setOf(
            Detail.TAHAJUD, Detail.DHUHA, Detail.RAWATIB, Detail.WITIR, Detail.TARAWIH,
            Detail.TAUBAT, Detail.ISTIKHARAH, Detail.QADHA, Detail.SUNNAH_LAINNYA
        ),
        Jenis.PUASA to setOf(
            Detail.SENIN_KAMIS, Detail.AYYAMUL_BIDH, Detail.DAUD, Detail.SYAWAL, Detail.RAMADAN,
            Detail.QADHA, Detail.NAZAR, Detail.SUNNAH_LAINNYA
        ),
        Jenis.DZIKIR to setOf(Detail.PAGI, Detail.PETANG)
    )

    /** Slot harian tetap: satu-satunya jenis yang boleh punya baris status "tidak". */
    val JENIS_BOLEH_TIDAK = setOf(Jenis.SHOLAT_WAJIB, Jenis.DZIKIR)

    /** Ibadah yang gugur saat Mode Cuti: satu-satunya jenis yang boleh berstatus "udzur". */
    val JENIS_BOLEH_UDZUR = setOf(Jenis.SHOLAT_WAJIB, Jenis.PUASA)

    /**
     * ID dokumen log deterministik: `{uid}_{tanggal}_{jenis}[_{detail}][_{localId}]`.
     * Sync ulang dari antrean offline akan menimpa dokumen yang sama, bukan menggandakan.
     * [localId] wajib untuk `sunnah_lainnya` karena beberapa habit buatan pengguna
     * bisa jatuh ke detail yang sama di hari yang sama.
     */
    fun logId(uid: String, tanggal: String, jenis: String, detail: String?, localId: String? = null): String =
        listOfNotNull(uid, tanggal, jenis, detail, localId).joinToString("_")
}

/** Hasil pemetaan habit aplikasi ke kolom jenis_ibadah / nama_detail. */
data class IbadahSlot(
    val jenisIbadah: String,
    val namaDetail: String?,
    val namaDetailAsli: String?
)

object ResearchMapping {
    private val FARDHU_BY_NAME = mapOf(
        "sholat subuh" to ResearchSchema.Detail.SUBUH,
        "sholat dzuhur" to ResearchSchema.Detail.DZUHUR,
        // Pengganti Dzuhur di hari Jumat; detail tetap dzuhur, nama asli menyimpan "Jumat".
        "sholat jumat" to ResearchSchema.Detail.DZUHUR,
        "sholat ashar" to ResearchSchema.Detail.ASHAR,
        "sholat maghrib" to ResearchSchema.Detail.MAGHRIB,
        "sholat isya" to ResearchSchema.Detail.ISYA
    )

    // Kunci = nama preset di AddHabitViewModel (tanpa awalan "sholat"/"puasa").
    private val SHOLAT_SUNNAH_BY_NAME = mapOf(
        "tahajud" to ResearchSchema.Detail.TAHAJUD,
        "dhuha" to ResearchSchema.Detail.DHUHA,
        "rawatib" to ResearchSchema.Detail.RAWATIB,
        "witir" to ResearchSchema.Detail.WITIR,
        "tarawih" to ResearchSchema.Detail.TARAWIH,
        "taubat" to ResearchSchema.Detail.TAUBAT,
        "istikharah" to ResearchSchema.Detail.ISTIKHARAH,
        "qadha" to ResearchSchema.Detail.QADHA
    )

    private val PUASA_BY_NAME = mapOf(
        "senin kamis" to ResearchSchema.Detail.SENIN_KAMIS,
        "ayyamul bidh" to ResearchSchema.Detail.AYYAMUL_BIDH,
        "daud" to ResearchSchema.Detail.DAUD,
        "syawal" to ResearchSchema.Detail.SYAWAL,
        "ramadan" to ResearchSchema.Detail.RAMADAN,
        "pengganti ramadan" to ResearchSchema.Detail.QADHA,
        "nazar" to ResearchSchema.Detail.NAZAR
    )

    /**
     * Habit bawaan (tabel `habits`): kategori "Sholat Fardhu", "Sholat Tarawih", "Puasa Wajib".
     * Dipetakan lewat nama, bukan lewat `default_{id}` karena id autoincrement berbeda per HP.
     */
    fun fromDefaultHabit(name: String, category: String): IbadahSlot? {
        val key = normalize(name)
        return when (category) {
            "Sholat Fardhu" -> FARDHU_BY_NAME[key]?.let {
                IbadahSlot(ResearchSchema.Jenis.SHOLAT_WAJIB, it, name)
            }
            "Sholat Tarawih" -> IbadahSlot(ResearchSchema.Jenis.SHOLAT_SUNNAH, ResearchSchema.Detail.TARAWIH, name)
            "Puasa Wajib" -> IbadahSlot(ResearchSchema.Jenis.PUASA, ResearchSchema.Detail.RAMADAN, name)
            else -> null
        }
    }

    /**
     * Habit sunnah (tabel `user_habits`): kategori enum "SHOLAT" / "PUASA".
     * Nama yang tidak dikenali (buatan pengguna) jatuh ke `sunnah_lainnya`.
     */
    fun fromSunnahHabit(name: String, category: String): IbadahSlot? {
        val key = normalize(name)
        return when (category) {
            "SHOLAT" -> IbadahSlot(
                ResearchSchema.Jenis.SHOLAT_SUNNAH,
                SHOLAT_SUNNAH_BY_NAME[key.removePrefix("sholat ")] ?: ResearchSchema.Detail.SUNNAH_LAINNYA,
                name
            )
            "PUASA" -> IbadahSlot(
                ResearchSchema.Jenis.PUASA,
                PUASA_BY_NAME[key.removePrefix("puasa ")] ?: ResearchSchema.Detail.SUNNAH_LAINNYA,
                name
            )
            else -> null
        }
    }

    private fun normalize(name: String): String =
        name.trim().lowercase().replace(Regex("[\\s_-]+"), " ")
}
