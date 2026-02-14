package com.islami.Aha.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.model.UserHabitEntity

@Database(
    entities = [Habit::class, UserHabitEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun userHabitDao(): UserHabitDao
}
