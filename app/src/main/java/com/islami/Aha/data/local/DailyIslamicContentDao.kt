package com.islami.Aha.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.islami.Aha.data.model.HadithContentEntity
import com.islami.Aha.data.model.SurahVerseEntity

@Dao
interface DailyIslamicContentDao {

    @Query("SELECT COUNT(*) FROM hadith_contents")
    suspend fun getHadithCount(): Int

    @Query("SELECT COUNT(*) FROM surah_verses")
    suspend fun getSurahVerseCount(): Int

    @Query("SELECT * FROM hadith_contents ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomHadith(): HadithContentEntity?

    @Query("SELECT * FROM surah_verses ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomSurahVerse(): SurahVerseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadith(items: List<HadithContentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahVerses(items: List<SurahVerseEntity>)

    @Query("DELETE FROM hadith_contents")
    suspend fun clearHadith()

    @Query("DELETE FROM surah_verses")
    suspend fun clearSurahVerses()

    @Transaction
    suspend fun replaceHadith(items: List<HadithContentEntity>) {
        clearHadith()
        insertHadith(items)
    }

    @Transaction
    suspend fun replaceSurahVerses(items: List<SurahVerseEntity>) {
        clearSurahVerses()
        insertSurahVerses(items)
    }
}
