package com.islami.Aha.data.repository

import com.islami.Aha.data.local.UserHabitDao
import com.islami.Aha.data.model.UserHabitEntity
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserHabitRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: UserHabitRepository
    private lateinit var mockDao: UserHabitDao

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockDao = mockk(relaxed = true)
        repository = UserHabitRepository(mockDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getAllHabits maps entities to domain models`() = runTest {
        val entities = listOf(
            UserHabitEntity(
                id = "1",
                name = "Dhuha",
                category = "SHOLAT",
                frequencyLabel = "Setiap hari",
                reminderEnabled = true,
                reminderTime = "06:00"
            ),
            UserHabitEntity(
                id = "2",
                name = "Senin Kamis",
                category = "PUASA",
                frequencyLabel = "Senin & Kamis"
            )
        )
        every { mockDao.getAllHabits() } returns flowOf(entities)

        val habits = repository.getAllHabits().first()

        assertEquals(2, habits.size)
        assertEquals("Dhuha", habits[0].name)
        assertEquals(SunnahCategoryType.SHOLAT, habits[0].category)
        assertTrue(habits[0].reminderEnabled)
        assertEquals("06:00", habits[0].reminderTime)
        assertEquals("Senin Kamis", habits[1].name)
        assertEquals(SunnahCategoryType.PUASA, habits[1].category)
    }

    @Test
    fun `getAllHabits handles invalid category gracefully`() = runTest {
        val entities = listOf(
            UserHabitEntity(id = "1", name = "Test", category = "INVALID_CATEGORY")
        )
        every { mockDao.getAllHabits() } returns flowOf(entities)

        val habits = repository.getAllHabits().first()

        assertEquals(1, habits.size)
        assertEquals(SunnahCategoryType.SHOLAT, habits[0].category) // fallback
    }

    @Test
    fun `getHabitById returns mapped domain model`() = runTest {
        val entity = UserHabitEntity(
            id = "1",
            name = "Tahajud",
            category = "SHOLAT",
            reminderEnabled = false
        )
        coEvery { mockDao.getHabitById("1") } returns entity

        val habit = repository.getHabitById("1")

        assertEquals("Tahajud", habit?.name)
        assertEquals(SunnahCategoryType.SHOLAT, habit?.category)
    }

    @Test
    fun `getHabitById returns null when not found`() = runTest {
        coEvery { mockDao.getHabitById("nonexistent") } returns null

        val habit = repository.getHabitById("nonexistent")
        assertNull(habit)
    }

    @Test
    fun `insertHabit converts domain to entity and calls dao`() = runTest {
        val habit = SunnahHabit(
            id = "1",
            name = "Istikharah",
            category = SunnahCategoryType.SHOLAT,
            frequencyLabel = "Setiap hari",
            reminderEnabled = true,
            reminderTime = "21:00"
        )

        repository.insertHabit(habit)

        coVerify {
            mockDao.insertHabit(match {
                it.id == "1" &&
                it.name == "Istikharah" &&
                it.category == "SHOLAT" &&
                it.reminderEnabled &&
                it.reminderTime == "21:00"
            })
        }
    }

    @Test
    fun `deleteHabit delegates to dao`() = runTest {
        repository.deleteHabit("123")
        coVerify { mockDao.deleteHabit("123") }
    }

    @Test
    fun `getHabitCount returns flow from dao`() = runTest {
        every { mockDao.getHabitCount() } returns flowOf(5)

        val count = repository.getHabitCount().first()
        assertEquals(5, count)
    }

    @Test
    fun `getHabitCountByCategory passes category name to dao`() = runTest {
        every { mockDao.getHabitCountByCategory("SHOLAT") } returns flowOf(3)

        val count = repository.getHabitCountByCategory(SunnahCategoryType.SHOLAT).first()
        assertEquals(3, count)
    }

    @Test
    fun `getActiveReminderHabits maps entities`() = runTest {
        val entities = listOf(
            UserHabitEntity(
                id = "1",
                name = "Dhuha",
                category = "SHOLAT",
                reminderEnabled = true,
                reminderTime = "06:00"
            )
        )
        coEvery { mockDao.getActiveReminderHabits() } returns entities

        val habits = repository.getActiveReminderHabits()

        assertEquals(1, habits.size)
        assertEquals("Dhuha", habits[0].name)
        assertTrue(habits[0].reminderEnabled)
    }
}
