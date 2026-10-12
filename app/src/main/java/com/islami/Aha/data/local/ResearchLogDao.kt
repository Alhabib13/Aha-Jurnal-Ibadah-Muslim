package com.islami.Aha.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.islami.Aha.data.model.DailyCompletionCount
import com.islami.Aha.data.model.ResearchLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResearchLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: ResearchLogEntity)

    @Query("SELECT * FROM research_logs WHERE slotKey = :slotKey")
    suspend fun getBySlotKey(slotKey: String): ResearchLogEntity?

    @Query("DELETE FROM research_logs WHERE slotKey = :slotKey")
    suspend fun deleteBySlotKey(slotKey: String)

    /** Baris yang boleh diunggah oleh [uid]: miliknya sendiri atau dibuat saat tamu. */
    @Query(
        "SELECT * FROM research_logs WHERE isSynced = 0 " +
            "AND (ownerUid IS NULL OR ownerUid = :uid) ORDER BY updatedAt ASC"
    )
    suspend fun getPendingUploads(uid: String): List<ResearchLogEntity>

    /**
     * Tandai terkirim hanya jika baris tidak berubah sejak dibaca, supaya edit yang
     * terjadi selama upload tetap dikirim ulang. Baris tamu diadopsi oleh [uid].
     */
    @Query(
        "UPDATE research_logs SET isSynced = 1, ownerUid = :uid " +
            "WHERE slotKey = :slotKey AND updatedAt = :updatedAt"
    )
    suspend fun markSynced(slotKey: String, updatedAt: Long, uid: String)

    @Query("DELETE FROM research_logs WHERE ownerUid IS NOT NULL AND ownerUid != :uid")
    suspend fun deleteNotOwnedBy(uid: String)

    @Query("DELETE FROM research_logs")
    suspend fun deleteAll()

    @Query("SELECT * FROM research_logs ORDER BY tanggal ASC, slotKey ASC")
    suspend fun getAll(): List<ResearchLogEntity>

    /** Pemulihan dari cloud: jangan timpa baris lokal yang mungkin belum terkirim. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(logs: List<ResearchLogEntity>)

    @Query("SELECT COUNT(*) FROM research_logs")
    suspend fun count(): Int

    /** Baris dengan awalan slotKey tertentu (tanpa wildcard LIKE, karena '_' di slotKey). */
    @Query("SELECT * FROM research_logs WHERE substr(slotKey, 1, length(:prefix)) = :prefix")
    suspend fun getBySlotKeyPrefix(prefix: String): List<ResearchLogEntity>

    /** Sesi terbaru dengan awalan ini yang dicatat sejak [sinceMs] (jendela anti-dobel-ketuk). */
    @Query(
        "SELECT * FROM research_logs WHERE substr(slotKey, 1, length(:prefix)) = :prefix " +
            "AND jamPencatatan >= :sinceMs ORDER BY jamPencatatan DESC LIMIT 1"
    )
    suspend fun findRecentByPrefix(prefix: String, sinceMs: Long): ResearchLogEntity?

    /** Berubah setiap kali isi tabel berubah; pemicu hitung ulang statistik. */
    @Query("SELECT COUNT(*) || '-' || COALESCE(MAX(updatedAt), 0) FROM research_logs")
    fun observeChangeTick(): Flow<String>

    // ---------- UI dzikir / tilawah / iqro ----------

    @Query(
        "SELECT * FROM research_logs WHERE tanggal = :tanggal AND jenisIbadah = :jenis " +
            "AND status = 'dikerjakan' ORDER BY jamPencatatan ASC"
    )
    fun observeDikerjakan(tanggal: String, jenis: String): Flow<List<ResearchLogEntity>>

    @Query(
        "SELECT * FROM research_logs WHERE jenisIbadah = 'iqro' AND status = 'dikerjakan' " +
            "ORDER BY tanggal DESC LIMIT 1"
    )
    fun observeLatestIqro(): Flow<ResearchLogEntity?>

    @Query(
        "SELECT COUNT(*) FROM research_logs WHERE tanggal = :tanggal AND jenisIbadah = 'dzikir' " +
            "AND status = 'dikerjakan'"
    )
    suspend fun countDzikirDikerjakan(tanggal: String): Int

    // ---------- Statistik (heatmap/streak/total) ----------
    // HANYA jenis yang tidak ada di habit_completion_records. Sholat & puasa ditulis ke
    // kedua tabel, jadi memasukkannya di sini akan menghitung ganda.
    // Satuan: dzikir per slot (maks 2/hari), tilawah 1/hari (berapa pun sesinya), iqro 1/hari.

    @Query(
        "SELECT tanggal AS dateKey, " +
            "SUM(CASE WHEN jenisIbadah = 'dzikir' THEN 1 ELSE 0 END) " +
            "+ MAX(CASE WHEN jenisIbadah = 'tilawah' THEN 1 ELSE 0 END) " +
            "+ MAX(CASE WHEN jenisIbadah = 'iqro' THEN 1 ELSE 0 END) AS count " +
            "FROM research_logs WHERE status = 'dikerjakan' " +
            "AND jenisIbadah IN ('dzikir', 'tilawah', 'iqro') " +
            "AND tanggal BETWEEN :startDateKey AND :endDateKey GROUP BY tanggal"
    )
    suspend fun getActivityDailyCountsBetween(
        startDateKey: String,
        endDateKey: String
    ): List<DailyCompletionCount>

    @Query(
        "SELECT DISTINCT tanggal FROM research_logs WHERE status = 'dikerjakan' " +
            "AND jenisIbadah IN ('dzikir', 'tilawah', 'iqro') ORDER BY tanggal DESC"
    )
    suspend fun getActivityDistinctDatesDesc(): List<String>

    @Query(
        "SELECT COALESCE(SUM(c), 0) FROM (SELECT " +
            "SUM(CASE WHEN jenisIbadah = 'dzikir' THEN 1 ELSE 0 END) " +
            "+ MAX(CASE WHEN jenisIbadah = 'tilawah' THEN 1 ELSE 0 END) " +
            "+ MAX(CASE WHEN jenisIbadah = 'iqro' THEN 1 ELSE 0 END) AS c " +
            "FROM research_logs WHERE status = 'dikerjakan' " +
            "AND jenisIbadah IN ('dzikir', 'tilawah', 'iqro') GROUP BY tanggal)"
    )
    suspend fun getActivityTotalCount(): Int
}
