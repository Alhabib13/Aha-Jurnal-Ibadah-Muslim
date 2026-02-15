package com.islami.Aha.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.model.HabitCompletionRecord
import com.islami.Aha.data.model.UserHabitEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseDaoIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var userHabitDao: UserHabitDao
    private lateinit var habitCompletionDao: HabitCompletionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitDao = db.habitDao()
        userHabitDao = db.userHabitDao()
        habitCompletionDao = db.habitCompletionDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun userHabitDao_insertAndReadById_works() = runBlocking {
        val entity = UserHabitEntity(
            id = "habit_1",
            name = "Tahajud",
            category = "SHOLAT",
            frequencyLabel = "Setiap hari",
            reminderEnabled = true,
            reminderTime = "03:30",
            completedDateKey = "2026-02-15"
        )

        userHabitDao.insertHabit(entity)
        val loaded = userHabitDao.getHabitById("habit_1")

        assertNotNull(loaded)
        assertEquals("Tahajud", loaded?.name)
        assertEquals("03:30", loaded?.reminderTime)
    }

    @Test
    fun habitCompletionDao_ignoreDuplicateRecord_works() = runBlocking {
        val record = HabitCompletionRecord(
            habitKey = "sunnah_habit_1",
            dateKey = "2026-02-15",
            category = "Sholat Sunnah",
            source = "SUNNAH"
        )

        habitCompletionDao.insert(record)
        habitCompletionDao.insert(record)

        val totalCount = habitCompletionDao.getTotalCompletionCount()
        assertEquals(1, totalCount)
    }

    @Test
    fun habitDao_deleteAllHabits_clearsTable() = runBlocking {
        habitDao.insertHabit(
            Habit(
                name = "Sholat Subuh",
                category = "Sholat Fardhu",
                icon = "sunrise",
                description = "",
                time = "04:30"
            )
        )
        habitDao.insertHabit(
            Habit(
                name = "Sholat Isya",
                category = "Sholat Fardhu",
                icon = "moon",
                description = "",
                time = "19:15"
            )
        )

        assertEquals(2, habitDao.getHabits().first().size)
        habitDao.deleteAllHabits()
        assertEquals(0, habitDao.getHabits().first().size)
    }
}
