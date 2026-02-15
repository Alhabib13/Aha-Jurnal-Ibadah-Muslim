package com.islami.Aha.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.islami.Aha.data.model.HabitCompletionRecord
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.model.UserHabitEntity

@Database(
    entities = [Habit::class, UserHabitEntity::class, HabitCompletionRecord::class],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun userHabitDao(): UserHabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_habits ADD COLUMN rakaat INTEGER")
                db.execSQL("ALTER TABLE user_habits ADD COLUMN completedDateKey TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS habit_completion_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        habitKey TEXT NOT NULL,
                        dateKey TEXT NOT NULL,
                        category TEXT NOT NULL,
                        source TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_habit_completion_records_habitKey_dateKey ON habit_completion_records(habitKey, dateKey)"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!columnExists(db, "user_habits", "rakaat")) {
                    db.execSQL("ALTER TABLE user_habits ADD COLUMN rakaat INTEGER")
                }
                if (!columnExists(db, "user_habits", "completedDateKey")) {
                    db.execSQL("ALTER TABLE user_habits ADD COLUMN completedDateKey TEXT")
                }
            }
        }

        // Rescue migration for devices that already ended up on schema version 8
        // without the two new columns due to previous inconsistent state.
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!columnExists(db, "user_habits", "rakaat")) {
                    db.execSQL("ALTER TABLE user_habits ADD COLUMN rakaat INTEGER")
                }
                if (!columnExists(db, "user_habits", "completedDateKey")) {
                    db.execSQL("ALTER TABLE user_habits ADD COLUMN completedDateKey TEXT")
                }
            }
        }

        private fun columnExists(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String
        ): Boolean {
            db.query("PRAGMA table_info($tableName)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) {
                        return true
                    }
                }
            }
            return false
        }
    }
}
