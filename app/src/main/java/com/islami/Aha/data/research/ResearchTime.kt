package com.islami.Aha.data.research

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Perhitungan waktu untuk log penelitian. Semua dihitung DI HP saat pencatatan, sehingga
 * tidak ada lokasi maupun jadwal sholat yang dikirim ke server.
 */
object ResearchTime {
    private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

    /** Batas usia jadwal hasil lokasi; lewat dari ini pengguna mungkin sudah pindah kota. */
    const val MAX_UMUR_JADWAL_MS = 3L * 24 * 60 * 60 * 1000

    /** Offset zona waktu pada saat [epochMs], format "+07:00" / "-03:30" / "+00:00". */
    fun offsetString(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val offset: ZoneOffset = zone.rules.getOffset(Instant.ofEpochMilli(epochMs))
        val totalMinutes = offset.totalSeconds / 60
        val sign = if (totalMinutes < 0) "-" else "+"
        val abs = kotlin.math.abs(totalMinutes)
        return "%s%02d:%02d".format(sign, abs / 60, abs % 60)
    }

    /**
     * Menit dari adzan ke saat pencatatan (negatif = dicatat sebelum adzan).
     * Null bila: jadwal tidak dari lokasi / terlalu lama, [adzanHHmm] tidak valid,
     * atau pencatatan susulan (tanggal ibadah ≠ tanggal lokal saat mencatat).
     */
    fun selisihMenitDariAdzan(
        jamPencatatanMs: Long,
        tanggalIbadah: String,
        adzanHHmm: String?,
        jadwalDariLokasi: Boolean,
        jadwalDisinkronMs: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): Int? {
        if (!jadwalDariLokasi || adzanHHmm == null) return null
        if (jamPencatatanMs - jadwalDisinkronMs > MAX_UMUR_JADWAL_MS) return null
        val dicatat = Instant.ofEpochMilli(jamPencatatanMs).atZone(zone)
        if (dicatat.toLocalDate() != runCatching { LocalDate.parse(tanggalIbadah) }.getOrNull()) return null
        val adzan = runCatching { LocalTime.parse(adzanHHmm, HHMM) }.getOrNull() ?: return null
        val menit = ChronoUnit.MINUTES.between(dicatat.toLocalDate().atTime(adzan).atZone(zone), dicatat)
        return menit.toInt().takeIf { it in ResearchSchema.SELISIH_MENIT }
    }

    /** Nama habit fardhu yang jadwalnya dipakai sebagai acuan adzan untuk slot ini. */
    fun habitAcuanAdzan(jenis: String, detail: String?): String? = when (jenis) {
        ResearchSchema.Jenis.SHOLAT_WAJIB -> when (detail) {
            ResearchSchema.Detail.SUBUH -> "Sholat Subuh"
            ResearchSchema.Detail.DZUHUR -> "Sholat Dzuhur"
            ResearchSchema.Detail.ASHAR -> "Sholat Ashar"
            ResearchSchema.Detail.MAGHRIB -> "Sholat Maghrib"
            ResearchSchema.Detail.ISYA -> "Sholat Isya"
            else -> null
        }
        // Dzikir pagi diukur dari Subuh, dzikir petang dari Ashar.
        ResearchSchema.Jenis.DZIKIR -> when (detail) {
            ResearchSchema.Detail.PAGI -> "Sholat Subuh"
            ResearchSchema.Detail.PETANG -> "Sholat Ashar"
            else -> null
        }
        else -> null
    }
}
