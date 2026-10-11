package com.islami.Aha.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Antrean lokal untuk koleksi Firestore `logs` (instrumen penelitian).
 *
 * Sengaja terpisah dari `habit_completion_records`: tabel itu dibaca heatmap & streak
 * yang menghitung setiap baris sebagai "dikerjakan", sehingga baris "tidak"/"udzur"
 * akan merusaknya. Nilai kolom mengikuti ResearchSchema.
 */
@Entity(
    tableName = "research_logs",
    indices = [Index(value = ["tanggal"]), Index(value = ["isSynced"])]
)
data class ResearchLogEntity(
    /** ResearchSchema.slotKey: ID log tanpa uid. */
    @PrimaryKey
    val slotKey: String,
    val tanggal: String,
    val jenisIbadah: String,
    val namaDetail: String?,
    val namaDetailAsli: String?,
    val status: String,
    val jumlah: Int?,
    val jilid: Int?,
    /** Epoch ms dari jam HP; null untuk baris "tidak"/"udzur". */
    val jamPencatatan: Long?,
    val sumberPencatatan: String?,
    /**
     * uid yang login saat baris dibuat; null = tamu. Baris milik uid lain tidak
     * pernah diunggah, sehingga log tidak bisa masuk ke akun yang salah.
     */
    val ownerUid: String?,
    val isSynced: Boolean = false,
    val updatedAt: Long,
    /** Tilawah: jumlah ayat (ukuran halus untuk model); jenis lain null. */
    val jumlahAyat: Int? = null,
    /** Tilawah: juz awal sesi; jenis lain null. */
    val juz: Int? = null,
    /** Zona waktu HP saat baris dibuat, mis. "+07:00". Wajib untuk semua jenis. */
    val offsetWaktu: String,
    /** Menit dari adzan (sholat_wajib & dzikir), dihitung di HP; null bila tak bisa dihitung. */
    val selisihMenitDariAdzan: Int? = null
)
