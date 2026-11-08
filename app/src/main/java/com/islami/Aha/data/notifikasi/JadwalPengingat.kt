package com.islami.Aha.data.notifikasi

import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.research.ResearchMapping
import com.islami.Aha.domain.model.SunnahHabit
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Penyusun jadwal pengingat 7 hari ke depan yang diunggah ke `users/{uid}/jadwal_pengingat`.
 *
 * Jadwal ini MENIRU alarm lokal, bukan menghitung ulang waktu sholat: alarm lokal berbunyi tiap
 * hari pada jam:menit yang tersimpan di habit (jam sholat diperbarui dari API saat aplikasi
 * dibuka), jadi tiap pengingat menghasilkan satu entri per hari pada jam:menit yang sama.
 * Aturan saring sama dengan AhaApplication.restoreAlarms / NotificationReceiver:
 * notifikasi global mati = kosong; Mode Cuti = nama yang memuat "Sholat"/"Puasa" dilewati.
 * Pengingat yang tidak bisa dipetakan ke jenis_ibadah/nama_detail tidak diunggah
 * (data hilang lebih baik daripada data salah).
 */
object JadwalPengingat {
    const val KOLEKSI = "jadwal_pengingat"
    const val WAKTU_KIRIM = "waktu_kirim"
    const val JENIS_IBADAH = "jenis_ibadah"
    const val NAMA_DETAIL = "nama_detail"
    const val ID_PENGINGAT = "id_pengingat"

    const val JUMLAH_HARI = 7L
    /** Saat aplikasi dibuka, unggah ulang bila sisa jadwal di server kurang dari ini. */
    val SISA_MINIMUM: Duration = Duration.ofDays(2)

    private val FORMAT_TANGGAL_ID = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT)

    /** Satu pengingat lokal yang aktif. [idPengingat] = habitId di AlarmManager ("default_3" / uuid sunnah). */
    data class Sumber(
        val idPengingat: String,
        val jenisIbadah: String,
        val namaDetail: String?,
        val jam: LocalTime
    )

    /** Satu dokumen jadwal_pengingat. */
    data class Entri(
        val docId: String,
        val idPengingat: String,
        val waktuKirimMs: Long,
        val jenisIbadah: String,
        val namaDetail: String?
    ) {
        fun keFirestore(): Map<String, Any?> = mapOf(
            WAKTU_KIRIM to com.google.firebase.Timestamp(java.util.Date(waktuKirimMs)),
            JENIS_IBADAH to jenisIbadah,
            NAMA_DETAIL to namaDetail,
            ID_PENGINGAT to idPengingat
        )
    }

    /** Daftar pengingat aktif, disaring dengan aturan yang sama dengan penjadwalan alarm lokal. */
    fun sumberDari(
        habitBawaan: List<Habit>,
        habitSunnah: List<SunnahHabit>,
        notifikasiGlobalAktif: Boolean,
        modeCuti: Boolean
    ): List<Sumber> {
        if (!notifikasiGlobalAktif) return emptyList()
        fun dilewatiCuti(nama: String) =
            modeCuti && (nama.contains("Sholat", ignoreCase = true) || nama.contains("Puasa", ignoreCase = true))

        val bawaan = habitBawaan
            .filter { it.isReminderEnabled && it.time.isNotBlank() && !dilewatiCuti(it.name) }
            .mapNotNull { h ->
                val jam = parseJam(h.time) ?: return@mapNotNull null
                val slot = ResearchMapping.fromDefaultHabit(h.name, h.category) ?: return@mapNotNull null
                Sumber("default_${h.id}", slot.jenisIbadah, slot.namaDetail, jam)
            }
        val sunnah = habitSunnah
            .filter { it.reminderEnabled && !dilewatiCuti(it.name) }
            .mapNotNull { h ->
                val jam = parseJam(h.reminderTime) ?: return@mapNotNull null
                val slot = ResearchMapping.fromSunnahHabit(h.name, h.category.name) ?: return@mapNotNull null
                Sumber(h.id, slot.jenisIbadah, slot.namaDetail, jam)
            }
        return (bawaan + sunnah).sortedWith(compareBy({ it.jam }, { it.idPengingat }))
    }

    /**
     * Semua waktu kirim dalam (sekarang, sekarang + 7 hari]: tepat 7 entri per pengingat,
     * mulai hari ini bila jamnya belum lewat (sama seperti alarm lokal), urut waktu.
     */
    fun susun(sumber: List<Sumber>, sekarang: ZonedDateTime): List<Entri> {
        val batas = sekarang.plusDays(JUMLAH_HARI)
        val hariIni = sekarang.toLocalDate()
        return sumber.flatMap { s ->
            (0..JUMLAH_HARI).mapNotNull { d ->
                val tanggal = hariIni.plusDays(d)
                val waktu = ZonedDateTime.of(tanggal, s.jam, sekarang.zone)
                if (!waktu.isAfter(sekarang) || waktu.isAfter(batas)) return@mapNotNull null
                Entri(
                    docId = "${s.idPengingat}_${tanggal.format(FORMAT_TANGGAL_ID)}",
                    idPengingat = s.idPengingat,
                    waktuKirimMs = waktu.toInstant().toEpochMilli(),
                    jenisIbadah = s.jenisIbadah,
                    namaDetail = s.namaDetail
                )
            }
        }.sortedWith(compareBy({ it.waktuKirimMs }, { it.docId }))
    }

    /**
     * Sidik isi jadwal (bukan tanggalnya): berubah bila pengingat ditambah/diubah/dihapus,
     * jam sholat berubah (lokasi), Mode Cuti atau notifikasi global berubah.
     */
    fun sidik(sumber: List<Sumber>): String =
        sumber.joinToString(";") { "${it.idPengingat}|${it.jenisIbadah}|${it.namaDetail}|${it.jam}" }

    /**
     * Perlu unggah ulang? Ya bila isi berubah, atau bila entri terakhir yang sudah diunggah
     * jatuh kurang dari [SISA_MINIMUM] dari sekarang (jadwal kosong tidak perlu diperpanjang).
     */
    fun perluUnggah(
        sidikBaru: String,
        sidikTerkirim: String?,
        akhirTerkirimMs: Long?,
        sekarangMs: Long
    ): Boolean {
        if (sidikTerkirim == null || sidikBaru != sidikTerkirim) return true
        if (sidikBaru.isEmpty()) return false
        val akhir = akhirTerkirimMs ?: return true
        return akhir - sekarangMs < SISA_MINIMUM.toMillis()
    }

    private fun parseJam(teks: String?): LocalTime? {
        val bagian = teks?.trim()?.split(":") ?: return null
        if (bagian.size != 2) return null
        val jam = bagian[0].toIntOrNull() ?: return null
        val menit = bagian[1].toIntOrNull() ?: return null
        if (jam !in 0..23 || menit !in 0..59) return null
        return LocalTime.of(jam, menit)
    }
}
