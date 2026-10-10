package com.islami.Aha.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.islami.Aha.data.model.ResearchLogEntity

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
}
