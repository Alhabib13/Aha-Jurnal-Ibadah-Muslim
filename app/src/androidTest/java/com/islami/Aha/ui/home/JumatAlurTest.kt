package com.islami.Aha.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.islami.Aha.data.local.AppDatabase
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.repository.CompletionSyncRepository
import com.islami.Aha.data.repository.DefaultHabitCompletionRepository
import com.islami.Aha.data.research.ResearchLogRepository
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.data.research.ResearchSchema.Detail
import com.islami.Aha.data.research.TilawahStateRepository
import com.islami.Aha.ui.home.components.HomeHabitItem
import com.islami.Aha.util.DateUtils
import com.islami.Aha.util.UserPreferencesManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Bukti alur Sholat Jumat di Beranda dengan tanggal dipalsukan ke Jumat 6 November 2026, 13.00 WIB.
 * Kartu dirender dari HomeUiState yang slot tengah harinya dihitung oleh repository yang sama
 * dengan Beranda; tombol di kartu benar-benar diketuk, lalu aksinya dijalankan seperti
 * HomeViewModel (tandaiTertinggal / gantiTengahHari / toggleHabitCompletion).
 */
@RunWith(AndroidJUnit4::class)
class JumatAlurTest {
    @get:Rule val compose = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repo: DefaultHabitCompletionRepository
    private val jumat = "2026-11-06"
    private val jamSemula = DateUtils.jam
    private var jumatSemula = false
    private var dzuhurId = 0

    @Before
    fun setUp(): Unit = runBlocking {
        DateUtils.jam = Clock.fixed(Instant.parse("2026-11-06T06:00:00Z"), ZoneId.of("Asia/Jakarta"))
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val prefs = context.getSharedPreferences("jumat-alur-test", Context.MODE_PRIVATE)
        prefs.edit().clear().putString(ResearchLogRepository.KEY_RESEARCH_CONSENT, ResearchLogRepository.CONSENT_SETUJU).commit()
        val research = ResearchLogRepository(
            context, db.researchLogDao(), db.deletedSyncDao(), prefs, db.habitDao(), TilawahStateRepository(prefs),
            db.dzikirHabitDao()
        )
        repo = DefaultHabitCompletionRepository(
            context, db, db.habitDao(), db.habitCompletionDao(), research,
            CompletionSyncRepository(db.habitCompletionDao(), db.deletedSyncDao(), prefs), prefs
        )
        listOf("Sholat Subuh" to "04:30", "Sholat Dzuhur" to "11:55", "Sholat Ashar" to "15:10",
            "Sholat Maghrib" to "18:00", "Sholat Isya" to "19:15").forEach { (nama, jam) ->
            db.habitDao().insertHabit(Habit(name = nama, category = "Sholat Fardhu", icon = "", description = "", time = jam))
        }
        dzuhurId = db.habitDao().getFardhuHabits().single { it.name == "Sholat Dzuhur" }.id
        jumatSemula = UserPreferencesManager.isJumatEnabled.value
    }

    @After
    fun tearDown() {
        DateUtils.jam = jamSemula
        UserPreferencesManager.setJumatEnabled(jumatSemula)
        db.close()
    }

    /** HomeUiState Beranda (tab Sholat › Fardhu) untuk pengguna laki-laki / perempuan. */
    private suspend fun stateBeranda(lakiLaki: Boolean): HomeUiState {
        UserPreferencesManager.setJumatEnabled(lakiLaki)
        return HomeUiState(
            allHabits = db.habitDao().getFardhuHabits(),
            isJumatEnabled = lakiLaki,
            detailTengahHariIni = repo.detailTengahHari(DateUtils.getTodayKey())
        )
    }

    /** Merender kartu sholat seperti HomeScreen; aksi tombol dicatat untuk dijalankan tes. */
    private fun render(state: HomeUiState, aksi: MutableList<String>) {
        compose.setContent {
            Column {
                state.filteredHabits.forEach { habit ->
                    HomeHabitItem(
                        habit = habit,
                        onCheckedChange = { aksi += "centang:${habit.id}" },
                        onToggleReminder = {},
                        currentTime = "13:00",
                        tertinggal = state.tertinggal(habit),
                        onTertinggal = if (state.sholatWajib(habit)) ({ aksi += "tertinggal:${habit.id}" }) else null,
                        onBatalTertinggal = {},
                        opsiJumat = state.opsiJumat(habit),
                        onGantiTengahHari = { keDzuhur -> aksi += "ganti:$keDzuhur" }
                    )
                }
            }
        }
    }

    private suspend fun barisTengahHari() =
        db.researchLogDao().getAll().filter { it.tanggal == jumat && it.namaDetail in setOf(Detail.JUMAT, Detail.DZUHUR) }

    @Test
    fun tanggalBenarBenarDipalsukanKeJumat() {
        assertEquals(jumat, DateUtils.getTodayKey())
        assertTrue(HomeUiState().isFriday)
    }

    @Test
    fun lakiLaki_melihatKartuJumat(): Unit = runBlocking {
        val state = stateBeranda(lakiLaki = true)
        assertEquals(Detail.JUMAT, state.detailTengahHariIni)
        render(state, mutableListOf())
        compose.onNodeWithText("Jumat").assertExists()
        compose.onNodeWithText("Tidak Jumat? Ganti Dzuhur").assertExists()
        compose.onAllNodesWithText("Dzuhur").assertCountEquals(0)
    }

    @Test
    fun perempuan_tidakMelihatKartuJumat(): Unit = runBlocking {
        val state = stateBeranda(lakiLaki = false)
        assertEquals(Detail.DZUHUR, state.detailTengahHariIni)
        render(state, mutableListOf())
        compose.onNodeWithText("Dzuhur").assertExists()
        compose.onAllNodesWithText("Jumat").assertCountEquals(0)
        compose.onAllNodesWithText("Tidak Jumat? Ganti Dzuhur").assertCountEquals(0)
    }

    @Test
    fun gantiDzuhur_menghasilkanLogDzuhur(): Unit = runBlocking {
        val aksi = mutableListOf<String>()
        render(stateBeranda(lakiLaki = true), aksi)
        compose.onNodeWithText("Tidak Jumat? Ganti Dzuhur").performClick()
        compose.waitForIdle()
        assertEquals(listOf("ganti:true"), aksi)
        // Seperti HomeViewModel.gantiTengahHari, lalu pengguna mencentang kartu Dzuhur.
        repo.gantiTengahHari(dzuhurId, DateUtils.getTodayKey(), keDzuhur = true)
        assertEquals(Detail.DZUHUR, repo.detailTengahHari(DateUtils.getTodayKey()))
        repo.setCompleted(dzuhurId, true, dateKey = DateUtils.getTodayKey())
        val log = barisTengahHari().single()
        assertEquals(Detail.DZUHUR, log.namaDetail)
        assertEquals(ResearchSchema.Status.DIKERJAKAN, log.status)
    }

    @Test
    fun tertinggalDiKartuJumat_menghasilkanLogJumatTidak(): Unit = runBlocking {
        val aksi = mutableListOf<String>()
        val state = stateBeranda(lakiLaki = true)
        // Hanya kartu tengah hari yang dirender, supaya tombol Tertinggal yang diketuk pasti milik kartu Jumat.
        val kartuJumat = state.copy(allHabits = state.allHabits.filter { it.id == dzuhurId })
        render(kartuJumat, aksi)
        compose.onNodeWithText("Jumat").assertExists()
        compose.onNodeWithText("Tertinggal").performClick()
        compose.waitForIdle()
        assertEquals(listOf("tertinggal:$dzuhurId"), aksi)
        // Seperti HomeViewModel.tandaiTertinggal.
        assertTrue(repo.markTertinggal(dzuhurId, DateUtils.getTodayKey(), ResearchSchema.Sumber.BUKA_APLIKASI))
        val log = barisTengahHari().single()
        assertEquals(Detail.JUMAT, log.namaDetail)
        assertEquals(ResearchSchema.Status.TIDAK, log.status)
        assertEquals(ResearchSchema.Sumber.BUKA_APLIKASI, log.sumberPencatatan)
    }
}
