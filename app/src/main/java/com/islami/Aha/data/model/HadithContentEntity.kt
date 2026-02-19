package com.islami.Aha.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hadith_contents")
data class HadithContentEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val source: String
)
