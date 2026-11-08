package com.islami.Aha.ui.profile

import com.islami.Aha.data.model.HabitCompletionRecord
import com.islami.Aha.data.model.ResearchLogEntity
import com.islami.Aha.data.research.ResearchSchema
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// "Jejak ibadah" menggantikan lencana: hanya momen yang SUDAH terjadi, bertanggal,
// tanpa yang dikunci atau dinilai. Logika murni supaya bisa dites.

enum class JenisJejak { PERTAMA, SUNNAH, PUASA, DZIKIR, TILAWAH, IQRO, KELIPATAN, REKOR }

data class Jejak(val judul: String, val keterangan: String, val tanggal: String, val jenis: JenisJejak)

internal val KELIPATAN_CATATAN = listOf(10, 50, 100, 250, 500, 1000)
private const val REKOR_MINIMAL = 3

private val idLocale = Locale.forLanguageTag("id-ID")

/** "2026-10-13" -> "13 Okt" (tahun ditambahkan bila bukan tahun [tahunIni]). */
fun tanggalJejak(tanggal: String, tahunIni: Int = LocalDate.now().year): String = runCatching {
    val t = LocalDate.parse(tanggal)
    t.format(DateTimeFormatter.ofPattern(if (t.year == tahunIni) "d MMM" else "d MMM yyyy", idLocale))
}.getOrDefault(tanggal)

private fun labelWaktuDzikir(waktu: String?) = when (waktu) {
    ResearchSchema.WaktuDzikir.BADA_SHOLAT -> "Dzikir setelah sholat"
    ResearchSchema.WaktuDzikir.PAGI -> "Dzikir pagi"
    ResearchSchema.WaktuDzikir.PETANG -> "Dzikir petang"
    ResearchSchema.WaktuDzikir.SEBELUM_TIDUR -> "Dzikir sebelum tidur"
    else -> "Dzikir"
}

private fun labelJenis(jenis: String) = when (jenis) {
    ResearchSchema.Jenis.DZIKIR -> "Dzikir"
    ResearchSchema.Jenis.TILAWAH -> "Tilawah"
    ResearchSchema.Jenis.IQRO -> "Iqro"
    else -> jenis
}

private fun isSunnahSholat(kategori: String) = kategori == "SHOLAT" || kategori == "Sholat Tarawih"
private fun isPuasa(kategori: String) = kategori == "PUASA" || kategori == "Puasa Wajib"

/**
 * Satuan "catatan" sama dengan Total dicatat di header: tiap baris sholat/puasa,
 * dzikir 1 per habit per hari, tilawah 1 per hari, iqro 1 per hari.
 */
internal fun tanggalSatuanCatatan(records: List<HabitCompletionRecord>, aktivitas: List<ResearchLogEntity>): List<String> {
    val dzikir = aktivitas.filter { it.jenisIbadah == ResearchSchema.Jenis.DZIKIR }
        .distinctBy { it.slotKey.dropLast(14) }.map { it.tanggal }
    val harian = aktivitas.filter { it.jenisIbadah == ResearchSchema.Jenis.TILAWAH || it.jenisIbadah == ResearchSchema.Jenis.IQRO }
        .distinctBy { it.jenisIbadah to it.tanggal }.map { it.tanggal }
    return (records.map { it.dateKey } + dzikir + harian).sorted()
}

/** Rentang hari berturut terpanjang (hari Mode Cuti menyambung tapi tidak dihitung): (panjang, tanggal akhir). */
internal fun rekorBerturut(tanggalAktif: Set<String>, tanggalCuti: Set<String>): Pair<Int, String>? {
    val hari = tanggalAktif.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.sorted()
    if (hari.isEmpty()) return null
    var terbaik = 0 to ""
    var panjang = 0
    var sebelumnya: LocalDate? = null
    for (d in hari) {
        val sambung = sebelumnya?.let { prev ->
            var c = prev.plusDays(1)
            while (c < d && c.toString() in tanggalCuti) c = c.plusDays(1)
            c == d
        } ?: false
        panjang = if (sambung) panjang + 1 else 1
        if (panjang > terbaik.first) terbaik = panjang to d.toString()
        sebelumnya = d
    }
    return terbaik
}

fun susunJejak(
    records: List<HabitCompletionRecord>,
    namaHabit: Map<String, String>,
    aktivitas: List<ResearchLogEntity>,
    tanggalCuti: Set<String> = emptySet(),
    tahunIni: Int = LocalDate.now().year
): List<Jejak> {
    val hasil = mutableListOf<Jejak>()
    val recUrut = records.sortedBy { it.dateKey }
    val aktUrut = aktivitas.sortedWith(compareBy({ it.tanggal }, { it.jamPencatatan ?: 0L }))
    fun tgl(t: String) = tanggalJejak(t, tahunIni)

    // Catatan pertama dari mana pun.
    val recPertama = recUrut.firstOrNull()
    val aktPertama = aktUrut.firstOrNull()
    when {
        recPertama != null && (aktPertama == null || recPertama.dateKey <= aktPertama.tanggal) -> {
            val nama = namaHabit[recPertama.habitKey] ?: recPertama.category
            hasil += Jejak("Catatan ibadah pertama", "$nama · ${tgl(recPertama.dateKey)}", recPertama.dateKey, JenisJejak.PERTAMA)
        }
        aktPertama != null ->
            hasil += Jejak("Catatan ibadah pertama", "${labelJenis(aktPertama.jenisIbadah)} · ${tgl(aktPertama.tanggal)}", aktPertama.tanggal, JenisJejak.PERTAMA)
    }

    recUrut.firstOrNull { isSunnahSholat(it.category) }?.let {
        hasil += Jejak("Sholat sunnah pertama dicatat", "${namaHabit[it.habitKey] ?: "Sholat sunnah"} · ${tgl(it.dateKey)}", it.dateKey, JenisJejak.SUNNAH)
    }
    recUrut.firstOrNull { isPuasa(it.category) }?.let {
        hasil += Jejak("Puasa pertama dicatat", "${namaHabit[it.habitKey] ?: "Puasa"} · ${tgl(it.dateKey)}", it.dateKey, JenisJejak.PUASA)
    }
    aktUrut.firstOrNull { it.jenisIbadah == ResearchSchema.Jenis.DZIKIR }?.let {
        hasil += Jejak("Mulai mencatat dzikir", "${labelWaktuDzikir(it.waktuDzikir)} · ${tgl(it.tanggal)}", it.tanggal, JenisJejak.DZIKIR)
    }

    // Tilawah: sesi pertama, lalu setiap juz baru yang dimulai dalam bacaan harian.
    val tilawah = aktUrut.filter { it.jenisIbadah == ResearchSchema.Jenis.TILAWAH }
    tilawah.firstOrNull()?.let {
        hasil += Jejak("Mulai mencatat tilawah", "Al-Qur'an · ${tgl(it.tanggal)}", it.tanggal, JenisJejak.TILAWAH)
    }
    val harian = tilawah.filter { it.namaDetail == ResearchSchema.Detail.BACAAN_HARIAN && it.juz != null }
    val juzAwal = harian.firstOrNull()?.juz
    if (juzAwal != null) {
        harian.filter { it.juz!! > juzAwal }.distinctBy { it.juz }.forEach {
            hasil += Jejak("Sampai di juz ${it.juz}", "Bacaan harian · ${tgl(it.tanggal)}", it.tanggal, JenisJejak.TILAWAH)
        }
    }

    // Iqro: mulai, lalu setiap kali naik ke jilid yang lebih tinggi.
    val iqro = aktUrut.filter { it.jenisIbadah == ResearchSchema.Jenis.IQRO && it.jilid != null }
    iqro.firstOrNull()?.let { awal ->
        hasil += Jejak("Mulai belajar Iqro", "Jilid ${awal.jilid} · ${tgl(awal.tanggal)}", awal.tanggal, JenisJejak.IQRO)
        var tertinggi = awal.jilid!!
        iqro.forEach {
            if (it.jilid!! > tertinggi) {
                tertinggi = it.jilid
                hasil += Jejak("Naik ke jilid ${it.jilid}", "Iqro · ${tgl(it.tanggal)}", it.tanggal, JenisJejak.IQRO)
            }
        }
    }

    // Catatan ke-10, ke-50, ...
    val satuan = tanggalSatuanCatatan(records, aktivitas)
    KELIPATAN_CATATAN.filter { it <= satuan.size }.forEach { n ->
        val t = satuan[n - 1]
        hasil += Jejak("Catatan ke-$n", "Semua ibadah yang tercatat · ${tgl(t)}", t, JenisJejak.KELIPATAN)
    }

    rekorBerturut(satuan.toSet(), tanggalCuti)?.takeIf { it.first >= REKOR_MINIMAL }?.let { (n, akhir) ->
        hasil += Jejak("$n hari berturut", "Rekor terpanjang, sampai ${tgl(akhir)}", akhir, JenisJejak.REKOR)
    }

    // Terbaru di atas; pada tanggal yang sama urutan penyusunan dibalik (yang terakhir dicapai di atas).
    return hasil.withIndex().sortedWith(compareByDescending<IndexedValue<Jejak>> { it.value.tanggal }.thenByDescending { it.index })
        .map { it.value }
}
