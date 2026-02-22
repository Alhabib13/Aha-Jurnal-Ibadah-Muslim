package com.islami.Aha.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surah_verses")
data class SurahVerseEntity(
    @PrimaryKey
    val id: String,
    val surahName: String,
    val ayahNumber: Int,
    val translation: String
)
