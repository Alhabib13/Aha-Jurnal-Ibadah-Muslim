package com.islami.Aha.ui.addhabit

import com.islami.Aha.ui.shared.SunnahHabitSharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sharedViewModel = SunnahHabitSharedViewModel()
        viewModel = AddHabitViewModel(sharedViewModel)
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
    fun `save habit sets success and adds to shared`() {
        viewModel.saveHabit()

        assertTrue(viewModel.uiState.value.saveSuccess)
        assertEquals(1, sharedViewModel.sunnahHabits.value.size)
        assertEquals("Dhuha", sharedViewModel.sunnahHabits.value.first().name)
    }

    @Test
    fun `save custom habit uses custom name`() {
        viewModel.selectHabit(LAINNYA_ID)
        viewModel.enableCustomInput()
        viewModel.updateCustomHabitName("Sholat Hajat")
        viewModel.saveHabit()

        assertEquals("Sholat Hajat", sharedViewModel.sunnahHabits.value.first().name)
    }

    @Test
    fun `save blank custom name does not save`() {
        viewModel.selectHabit(LAINNYA_ID)
        viewModel.enableCustomInput()
        viewModel.updateCustomHabitName("")
        viewModel.saveHabit()

        assertFalse(viewModel.uiState.value.saveSuccess)
        assertTrue(sharedViewModel.sunnahHabits.value.isEmpty())
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
}
