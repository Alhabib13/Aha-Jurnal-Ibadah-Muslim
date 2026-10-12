package com.islami.Aha.ui.addhabit

import android.content.Context
import com.islami.Aha.data.dzikir.DzikirHabitDraft
import com.islami.Aha.data.dzikir.DzikirHabitRepository
import com.islami.Aha.data.dzikir.DzikirKatalog
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.ui.shared.SunnahHabitSharedViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AddHabitDzikirTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: DzikirHabitRepository
    private lateinit var vm: AddHabitViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val f = listOf("src/main/assets/dzikir_katalog.json", "app/src/main/assets/dzikir_katalog.json")
            .map(::File).first { it.exists() }
        repo = mockk(relaxed = true)
        every { repo.katalog } returns DzikirKatalog.parse(f.readText(Charsets.UTF_8))
        coEvery { repo.getAll() } returns emptyList()
        coEvery { repo.addAll(any(), any()) } returns null
        vm = AddHabitViewModel(mockk<Context>(relaxed = true), mockk<SunnahHabitSharedViewModel>(relaxed = true), repo)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `kategori dzikir memuat 9 entri, target kosong secara bawaan`() = runTest(dispatcher) {
        vm.selectDzikirCategory()
        advanceUntilIdle()
        val s = vm.uiState.value
        assertTrue(s.isDzikir)
        assertEquals(9, s.dzikirKatalog.size)
        vm.toggleDzikirEntry(ResearchSchema.Detail.ISTIGHFAR)
        with(vm.uiState.value.dzikirPilihan.single()) {
            assertEquals("", targetText)
            assertEquals(ResearchSchema.ModePencatatan.CENTANG, mode)
        }
    }

    @Test
    fun `mode layar tidak bisa dipilih selama bacaan kosong`() = runTest(dispatcher) {
        vm.selectDzikirCategory()
        advanceUntilIdle()
        vm.toggleDzikirEntry(ResearchSchema.Detail.DZIKIR_PAGI)
        vm.selectDzikirMode(ResearchSchema.Detail.DZIKIR_PAGI, ResearchSchema.ModePencatatan.LAYAR)
        assertEquals(ResearchSchema.ModePencatatan.CENTANG, vm.uiState.value.dzikirPilihan.single().mode)
    }

    @Test
    fun `beberapa dzikir sekaligus termasuk dua dzikir_lainnya`() = runTest(dispatcher) {
        vm.selectDzikirCategory()
        advanceUntilIdle()
        vm.toggleDzikirEntry(ResearchSchema.Detail.DZIKIR_BADA_SHOLAT)
        vm.toggleDzikirEntry(ResearchSchema.Detail.DZIKIR_LAINNYA)
        vm.addDzikirLainnya()
        val keys = vm.uiState.value.dzikirPilihan.filter { it.slug == ResearchSchema.Detail.DZIKIR_LAINNYA }.map { it.key }
        assertEquals(2, keys.size)
        vm.updateDzikirNama(keys[0], "Ratib")
        vm.updateDzikirNama(keys[1], "Hizib")
        vm.updateDzikirTarget(keys[1], "33a")
        vm.saveHabit()
        advanceUntilIdle()

        val drafts = slot<List<DzikirHabitDraft>>()
        coVerify { repo.addAll(capture(drafts), any()) }
        assertEquals(
            listOf(
                DzikirHabitDraft(ResearchSchema.Detail.DZIKIR_BADA_SHOLAT),
                DzikirHabitDraft(ResearchSchema.Detail.DZIKIR_LAINNYA, namaDetailAsli = "Ratib"),
                DzikirHabitDraft(ResearchSchema.Detail.DZIKIR_LAINNYA, namaDetailAsli = "Hizib", targetHitungan = 33)
            ),
            drafts.captured
        )
        assertTrue(vm.uiState.value.saveSuccess)
    }

    @Test
    fun `tanpa pilihan tidak menyimpan`() = runTest(dispatcher) {
        vm.selectDzikirCategory()
        advanceUntilIdle()
        vm.saveHabit()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.saveSuccess)
        assertNotNull(vm.uiState.value.dzikirError)
        coVerify(exactly = 0) { repo.addAll(any(), any()) }
    }
}
