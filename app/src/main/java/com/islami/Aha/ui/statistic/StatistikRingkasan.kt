package com.islami.Aha.ui.statistic

import com.islami.Aha.data.model.HabitCompletionRecord
import com.islami.Aha.data.model.ResearchLogEntity
import com.islami.Aha.data.research.ResearchSchema
import java.time.LocalDate
import java.time.YearMonth

// Logika murni halaman Statistik (tanpa Android), supaya bisa dites. Tanpa persentase:
// yang ditampilkan jumlah dan posisi, dengan kata netral bila belum ada catatan.

enum class Intensitas { KOSONG, SEDIKIT, BANYAK }

data class HariKalender(
    val tanggal: String,
    val hari: Int,
    val jumlah: Int,
    val intensitas: Intensitas,
    val cuti: Boolean,
    val hariIni: Boolean,
    /** Tanggal sesudah hari ini: tidak bisa diketuk. */
    val nanti: Boolean,
    /** Hari Hijriah dalam angka Arab (mis. "٤"); kosong bila di luar kalender Hijriah. */
    val hijri: String = "",
    /** Nama puasa sunnah pada tanggal ini (penanda kalender), null bila tidak ada. */
    val puasaSunnah: String? = null
)

/** 15 -> "١٥" (angka Arab-Indik). */
fun angkaArab(n: Int): String = n.toString().map { c -> (c - '0' + 0x0660).toChar() }.joinToString("")

/** Hari Hijriah (Umm al-Qura, sama dengan tanggal Hijriah di Beranda); null di luar rentang. */
fun hariHijri(tanggal: LocalDate): Int? = runCatching {
    java.time.chrono.HijrahDate.from(tanggal).get(java.time.temporal.ChronoField.DAY_OF_MONTH)
}.getOrNull()

enum class JenisChip { SHOLAT, SUNNAH_PUASA, DZIKIR, TILAWAH, IQRO }

data class ChipCatatan(val label: String, val jenis: JenisChip)

data class RingkasanPekan(
    val sholatFardhu: Int = 0,
    /** Nama waktu sholat yang paling sering dicatat; null bila seri atau kosong. */
    val sholatTersering: String? = null,
    val dzikirSesi: Int = 0,
    val dzikirTersering: String? = null,
    val tilawahHalaman: Int = 0,
    val tilawahHari: Int = 0,
    val sunnahPuasa: Int = 0
)

/** Dua tingkat saja: "Banyak" bila minimal separuh target harian tercatat. */
fun intensitas(jumlah: Int, targetHarian: Int): Intensitas = when {
    jumlah <= 0 -> Intensitas.KOSONG
    targetHarian > 0 && jumlah * 2 >= targetHarian -> Intensitas.BANYAK
    targetHarian <= 0 && jumlah >= 3 -> Intensitas.BANYAK
    else -> Intensitas.SEDIKIT
}

/** Kisi kalender Senin-pertama; null = sel kosong sebelum tanggal 1. */
fun susunBulan(
    bulan: YearMonth,
    jumlahPerTanggal: Map<String, Int>,
    tanggalCuti: Set<String>,
    hariIni: LocalDate,
    targetHarian: Int
): List<HariKalender?> {
    val awal = bulan.atDay(1)
    val kosong = awal.dayOfWeek.value - 1
    val hari = (1..bulan.lengthOfMonth()).map { d ->
        val tgl = bulan.atDay(d)
        val key = tgl.toString()
        val jumlah = jumlahPerTanggal[key] ?: 0
        HariKalender(
            tanggal = key,
            hari = d,
            jumlah = jumlah,
            intensitas = intensitas(jumlah, targetHarian),
            cuti = key in tanggalCuti,
            hariIni = tgl == hariIni,
            nanti = tgl.isAfter(hariIni),
            hijri = hariHijri(tgl)?.let(::angkaArab).orEmpty(),
            puasaSunnah = com.islami.Aha.util.PuasaMendatang.puasaSunnahPada(tgl)
        )
    }
    return List(kosong) { null } + hari
}

fun labelWaktuDzikir(waktu: String?): String = when (waktu) {
    ResearchSchema.WaktuDzikir.BADA_SHOLAT -> "setelah sholat"
    ResearchSchema.WaktuDzikir.PAGI -> "pagi"
    ResearchSchema.WaktuDzikir.PETANG -> "petang"
    ResearchSchema.WaktuDzikir.SEBELUM_TIDUR -> "sebelum tidur"
    else -> "kapan saja"
}

private fun namaPendek(nama: String) = nama.removePrefix("Sholat ").ifBlank { nama }

/**
 * Chip catatan satu hari. Sholat & puasa dari habit_completion_records (urut [urutanKunci]),
 * dzikir dikelompokkan per waktu (×sesi), tilawah dijumlah halamannya, iqro posisi terakhir.
 */
fun susunCatatanHari(
    records: List<HabitCompletionRecord>,
    namaHabit: Map<String, String>,
    urutanKunci: List<String>,
    aktivitas: List<ResearchLogEntity>
): List<ChipCatatan> {
    val chips = mutableListOf<ChipCatatan>()
    val urutan = urutanKunci.withIndex().associate { it.value to it.index }
    records.distinctBy { it.habitKey }
        .sortedWith(compareBy({ if (it.category == "Sholat Fardhu") 0 else 1 }, { urutan[it.habitKey] ?: Int.MAX_VALUE }))
        .forEach { r ->
            val nama = namaHabit[r.habitKey]?.let(::namaPendek) ?: r.category
            chips += ChipCatatan(nama, if (r.category == "Sholat Fardhu") JenisChip.SHOLAT else JenisChip.SUNNAH_PUASA)
        }
    aktivitas.filter { it.jenisIbadah == ResearchSchema.Jenis.DZIKIR }
        .groupBy { it.waktuDzikir }
        .forEach { (waktu, sesi) -> chips += ChipCatatan("Dzikir ${labelWaktuDzikir(waktu)} ×${sesi.size}", JenisChip.DZIKIR) }
    val halaman = aktivitas.filter { it.jenisIbadah == ResearchSchema.Jenis.TILAWAH }.sumOf { it.jumlah ?: 0 }
    if (aktivitas.any { it.jenisIbadah == ResearchSchema.Jenis.TILAWAH }) {
        chips += ChipCatatan("Tilawah $halaman hlm", JenisChip.TILAWAH)
    }
    aktivitas.lastOrNull { it.jenisIbadah == ResearchSchema.Jenis.IQRO }?.let {
        chips += ChipCatatan("Iqro jilid ${it.jilid ?: "-"} hal. ${it.jumlah ?: "-"}", JenisChip.IQRO)
    }
    return chips
}

fun susunRingkasanPekan(
    records: List<HabitCompletionRecord>,
    namaHabit: Map<String, String>,
    aktivitas: List<ResearchLogEntity>
): RingkasanPekan {
    val fardhu = records.filter { it.category == "Sholat Fardhu" }.distinctBy { it.habitKey to it.dateKey }
    val dzikir = aktivitas.filter { it.jenisIbadah == ResearchSchema.Jenis.DZIKIR }
    val tilawah = aktivitas.filter { it.jenisIbadah == ResearchSchema.Jenis.TILAWAH }
    return RingkasanPekan(
        sholatFardhu = fardhu.size,
        sholatTersering = terseringUnik(fardhu.map { it.habitKey })?.let { k -> namaHabit[k]?.let(::namaPendek) },
        dzikirSesi = dzikir.size,
        dzikirTersering = terseringUnik(dzikir.map { it.waktuDzikir.orEmpty() })?.let { labelWaktuDzikir(it.ifEmpty { null }) },
        tilawahHalaman = tilawah.sumOf { it.jumlah ?: 0 },
        tilawahHari = tilawah.map { it.tanggal }.distinct().size,
        sunnahPuasa = records.filter { it.category != "Sholat Fardhu" }.distinctBy { it.habitKey to it.dateKey }.size
    )
}

/** Nilai yang paling sering muncul; null bila kosong atau ada yang seri di puncak. */
internal fun <T> terseringUnik(nilai: List<T>): T? {
    val hitung = nilai.groupingBy { it }.eachCount()
    val maks = hitung.values.maxOrNull() ?: return null
    val puncak = hitung.filterValues { it == maks }.keys
    return puncak.singleOrNull()
}

/** [sebelumMulai] = sebelum hari pertama pengguna mencatat apa pun (tidak ikut target). */
data class HarianSholat(
    val hari: Int,
    val jumlah: Int,
    val cuti: Boolean,
    val nanti: Boolean,
    val sebelumMulai: Boolean = false
)

/**
 * Ringkasan sholat fardhu satu bulan. [target] tidak menghitung hari Mode Cuti, hari yang
 * belum tiba, dan hari sebelum pengguna mulai mencatat (hari Mode Cuti bukan "belum tercatat"). [medianMenitAdzan] = median
 * selisih_menit_dari_adzan dari pencatatan tepat hari (bukan diakui sendiri); null bila kosong.
 */
data class RingkasanSholatBulan(
    val tercatat: Int = 0,
    val target: Int = 0,
    val hariCuti: Int = 0,
    val ditandaiTertinggal: Int = 0,
    val medianMenitAdzan: Int? = null,
    val harian: List<HarianSholat> = emptyList()
)

fun susunRingkasanSholatBulan(
    bulan: YearMonth,
    hariIni: LocalDate,
    records: List<HabitCompletionRecord>,
    logSholat: List<ResearchLogEntity>,
    tanggalCuti: Set<String>,
    jumlahFardhu: Int,
    mulaiMencatat: LocalDate? = null
): RingkasanSholatBulan {
    val perTanggal = records.filter { it.category == "Sholat Fardhu" }
        .distinctBy { it.habitKey to it.dateKey }
        .groupingBy { it.dateKey }.eachCount()
    val harian = (1..bulan.lengthOfMonth()).map { d ->
        val tgl = bulan.atDay(d)
        val key = tgl.toString()
        HarianSholat(
            hari = d,
            jumlah = (perTanggal[key] ?: 0).coerceAtMost(jumlahFardhu.coerceAtLeast(0)),
            cuti = key in tanggalCuti,
            nanti = tgl.isAfter(hariIni),
            sebelumMulai = mulaiMencatat != null && tgl.isBefore(mulaiMencatat)
        )
    }
    val berlaku = harian.filter { !it.nanti && !it.sebelumMulai }
    val selisih = logSholat
        .filter { it.status == ResearchSchema.Status.DIKERJAKAN }
        .mapNotNull { it.selisihMenitDariAdzan }
        .sorted()
    return RingkasanSholatBulan(
        tercatat = berlaku.sumOf { it.jumlah },
        target = berlaku.count { !it.cuti } * jumlahFardhu,
        hariCuti = berlaku.count { it.cuti },
        ditandaiTertinggal = logSholat.count { it.status == ResearchSchema.Status.TIDAK },
        medianMenitAdzan = selisih.takeIf { it.isNotEmpty() }?.let { it[it.size / 2] },
        harian = harian
    )
}

/**
 * Perkiraan khatam bacaan harian. [lajuHarian] = halaman bacaan harian 14 hari terakhir / 14
 * (kecepatan nyata, bukan target); null bila tidak ada bacaan. Tanggal perkiraan dihitung dari
 * laju nyata dan, bila ada, dari target halaman per hari.
 */
data class PerkiraanKhatam(
    val halaman: Int,
    val sisaHalaman: Int,
    val lajuHarian: Double?,
    val selesaiMenurutLaju: LocalDate?,
    val target: Int?,
    val selesaiMenurutTarget: LocalDate?
)

const val JENDELA_LAJU_HARI = 14

fun perkiraanKhatam(
    halamanSekarang: Int,
    totalHalaman: Int,
    sesiTilawah: List<ResearchLogEntity>,
    hariIni: LocalDate,
    target: Int?
): PerkiraanKhatam {
    val awalJendela = hariIni.minusDays((JENDELA_LAJU_HARI - 1).toLong()).toString()
    val halamanJendela = sesiTilawah
        .filter {
            it.jenisIbadah == ResearchSchema.Jenis.TILAWAH &&
                it.namaDetail == ResearchSchema.Detail.BACAAN_HARIAN &&
                it.status == ResearchSchema.Status.DIKERJAKAN &&
                it.tanggal >= awalJendela && it.tanggal <= hariIni.toString()
        }
        .sumOf { it.jumlah ?: 0 }
    val sisa = (totalHalaman - halamanSekarang).coerceAtLeast(0)
    val laju = (halamanJendela.toDouble() / JENDELA_LAJU_HARI).takeIf { it > 0 }
    fun selesai(perHari: Double?): LocalDate? =
        if (perHari == null || perHari <= 0 || sisa == 0) null
        else hariIni.plusDays(kotlin.math.ceil(sisa / perHari).toLong())
    val targetSah = target?.takeIf { it > 0 }
    return PerkiraanKhatam(
        halaman = halamanSekarang,
        sisaHalaman = sisa,
        lajuHarian = laju,
        selesaiMenurutLaju = selesai(laju),
        target = targetSah,
        selesaiMenurutTarget = selesai(targetSah?.toDouble())
    )
}
