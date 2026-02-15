package com.islami.Aha.data.local

import androidx.room.TypeConverter
import com.islami.Aha.ui.addhabit.SunnahCategoryType

class Converters {

    @TypeConverter
    fun fromCategory(value: SunnahCategoryType): String = value.name

    @TypeConverter
    fun toCategory(value: String): SunnahCategoryType = try {
        SunnahCategoryType.valueOf(value)
    } catch (_: IllegalArgumentException) {
        SunnahCategoryType.SHOLAT
    }
}
