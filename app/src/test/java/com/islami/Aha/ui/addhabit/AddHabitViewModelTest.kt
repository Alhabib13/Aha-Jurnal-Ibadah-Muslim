package com.islami.Aha.ui.addhabit

import android.content.Context
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.UserHabitDao
import com.islami.Aha.data.model.UserHabitEntity
import com.islami.Aha.data.repository.UserHabitRepository
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.shared.SunnahHabitSharedViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddHabitViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AddHabitViewModel
    private lateinit var sharedViewModel: SunnahHabitSharedViewModel
    private lateinit var mockContext: Context
    private lateinit var mockDao: UserHabitDao
    private lateinit var mockHabitCompletionDao: HabitCompletionDao

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockContext = mockk(relaxed = true)
        mockDao = mockk(relaxed = true)
        mockHabitCompletionDao = mockk(relaxed = true)
        every { mockDao.getAllHabits() } returns flowOf(emptyList())
        every { mockDao.getHabitCount() } returns flowOf(0)
        val repository = UserHabitRepository(mockDao)
        sharedViewModel = SunnahHabitSharedViewModel(mockHabitCompletionDao, repository)
        viewModel = AddHabitViewModel(mockContext, sharedViewModel)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state uses expected defaults`() {
        val state = viewModel.uiState.value
        assertEquals(SunnahCategoryType.SHOLAT, state.selectedCategory)
        assertEquals(FrequencyType.EVERY_DAY, state.frequencyType)
        assertFalse(state.isReminderEnabled)
        assertFalse(state.isCustomHabit)
        assertNull(state.selectedRakaat)
    }

    @Test
    fun `select category updates available habits and resets state`() {
        viewModel.selectCategory(SunnahCategoryType.PUASA)
        val state = viewModel.uiState.value

        assertEquals(SunnahCategoryType.PUASA, state.selectedCategory)
        assertEquals("senin_kamis", state.selectedHabitId)
        assertNull(state.selectedRakaat)
        assertFalse(state.isCustomHabit)
    }

    @Test
    fun `select lainnya shows extra habits`() {
        viewModel.selectHabit(LAINNYA_ID)
        val state = viewModel.uiState.value

        assertTrue(state.isLainnya)
        assertTrue(state.extraHabits.isNotEmpty())
    }

    @Test
    fun `select extra habit sets name`() {
        viewModel.selectHabit(LAINNYA_ID)
        viewModel.selectExtraHabit("taubat")
        val state = viewModel.uiState.value

        assertEquals("Taubat", state.selectedHabitName)
        assertFalse(state.isCustomHabit)
    }

    @Test
    fun `enable custom input and type name`() {
        viewModel.selectHabit(LAINNYA_ID)
        viewModel.enableCustomInput()
        viewModel.updateCustomHabitName("Sholat Hajat")
        val state = viewModel.uiState.value

        assertTrue(state.isCustomHabit)
        assertEquals("Sholat Hajat", state.selectedHabitName)
    }

    @Test
    fun `select rakaat works for sholat`() {
        viewModel.selectRakaat(4)
        assertEquals(4, viewModel.uiState.value.selectedRakaat)
    }

    @Test
    fun `switching to puasa clears rakaat`() {
        viewModel.selectRakaat(4)
        viewModel.selectCategory(SunnahCategoryType.PUASA)
        assertNull(viewModel.uiState.value.selectedRakaat)
    }

    @Test
    fun `save habit sets success`() {
        viewModel.saveHabit()
        assertTrue(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun `save blank custom name does not save`() {
        viewModel.selectHabit(LAINNYA_ID)
        viewModel.enableCustomInput()
        viewModel.updateCustomHabitName("")
        viewModel.saveHabit()

        assertFalse(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun `reset state returns to defaults`() {
        viewModel.selectCategory(SunnahCategoryType.PUASA)
        viewModel.toggleReminder()
        viewModel.selectRakaat(6)
        viewModel.resetState()

        val state = viewModel.uiState.value
        assertEquals(SunnahCategoryType.SHOLAT, state.selectedCategory)
        assertFalse(state.isReminderEnabled)
        assertNull(state.selectedRakaat)
    }

    @Test
    fun `frequency label for specific days`() {
        viewModel.selectFrequency(FrequencyType.SPECIFIC_DAYS)
        viewModel.toggleDay(1) // Senin
        viewModel.toggleDay(4) // Kamis
        val label = viewModel.getFormattedReminderTime()
        // Just verify it doesn't crash - the label format is internal
        assertTrue(label.isNotBlank())
    }

    @Test
    fun `toggle reminder flips state`() {
        assertFalse(viewModel.uiState.value.isReminderEnabled)
        viewModel.toggleReminder()
        assertTrue(viewModel.uiState.value.isReminderEnabled)
        viewModel.toggleReminder()
        assertFalse(viewModel.uiState.value.isReminderEnabled)
    }

    @Test
    fun `show and hide time picker`() {
        assertFalse(viewModel.uiState.value.showTimePicker)
        viewModel.showTimePicker()
        assertTrue(viewModel.uiState.value.showTimePicker)
        viewModel.hideTimePicker()
        assertFalse(viewModel.uiState.value.showTimePicker)
    }

    @Test
    fun `onReminderTimeChange updates time and hides picker`() {
        viewModel.showTimePicker()
        viewModel.onReminderTimeChange(14, 30)
        val state = viewModel.uiState.value
        assertEquals(14, state.reminderHour)
        assertEquals(30, state.reminderMinute)
        assertFalse(state.showTimePicker)
    }
}
