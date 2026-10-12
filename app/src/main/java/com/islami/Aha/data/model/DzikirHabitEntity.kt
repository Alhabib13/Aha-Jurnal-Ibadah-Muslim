package com.islami.Aha.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Habit dzikir = KONFIGURASI yang dipilih pengguna di Add Habit, bukan kejadian.
 * Tidak pernah menulis ke `research_logs` / `logs`; log hanya dibuat saat pengguna
 * menekan "Catat" (lihat ResearchLogRepository.recordDzikirSession).
 *
 * Sengaja terpisah dari `user_habits` (sunnah): habit sunnah menulis
 * habit_completion_records, sedangkan dzikir hanya boleh lewat research_logs.
 */
@Entity(tableName = "dzikir_habits")
data class DzikirHabitEntity(
    /** Dipakai sebagai idHabit di ID log dzikir; pola [A-Za-z0-9-]{1,64}. */
    @PrimaryKey
    val id: String,
    /** Slug katalog = nama_detail di logs. */
    val slug: String,
    /** Hanya untuk dzikir_lainnya (wajib); null untuk slug lain. */
    val namaDetailAsli: String?,
    val modePencatatan: String,
    val waktuDzikir: String,
    /** Opsional; null = tanpa target (tidak ada nilai anjuran bawaan). */
    val targetHitungan: Int?,
    val createdAt: Long,
    /** null = dibuat saat tamu; diadopsi akun yang login berikutnya. */
    val ownerUid: String?
)
