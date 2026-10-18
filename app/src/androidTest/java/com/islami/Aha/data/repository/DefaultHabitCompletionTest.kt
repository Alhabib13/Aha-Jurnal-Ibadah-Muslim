package com.islami.Aha.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.islami.Aha.data.local.AppDatabase
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.research.ResearchLogRepository
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.data.research.TilawahStateRepository
import com.islami.Aha.util.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** "Sudah" dari notifikasi mencatat ke hari ibadah pengingatnya, bukan hari saat tombol ditekan. */
@RunWith(AndroidJUnit4::class)
class DefaultHabitCompletionTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: DefaultHabitCompletionRepository
    private val hariIni = DateUtils.getTodayKey()
    private val kemarin = LocalDate.now().minusDays(1).toString()
    private var isyaId = 0

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val prefs = context.getSharedPreferences("default-habit-completion-test", Context.MODE_PRIVATE)
        prefs.edit().clear().putBoolean(ResearchLogRepository.KEY_RESEARCH_NOTICE_ACK, true).commit()
        val research = ResearchLogRepository(
            context, db.researchLogDao(), db.deletedSyncDao(), prefs, db.habitDao(), TilawahStateRepository(prefs),
            db.dzikirHabitDao()
        )
        repo = DefaultHabitCompletionRepository(
            context, db, db.habitDao(), db.habitCompletionDao(), research,
            CompletionSyncRepository(db.habitCompletionDao(), db.deletedSyncDao(), prefs), prefs
        )
        db.habitDao().insertHabit(Habit(name = "Sholat Isya", category = "Sholat Fardhu", icon = "", description = ""))
        isyaId = db.habitDao().getFardhuHabits().single().id
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun sudahUntukKemarin_tercatatKemarin_tampilanHariIniTidakBerubah() = runBlocking {
        val hasil = repo.setCompleted(isyaId, true, ResearchSchema.Sumber.DARI_NOTIFIKASI, dateKey = kemarin)!!
        assertTrue(hasil.berubah)
        assertTrue(repo.isCompletedOn(isyaId, kemarin))
        assertFalse(repo.isCompletedToday(isyaId))
        assertFalse(db.habitDao().getHabitById(isyaId)!!.isCompleted)

        val log = db.researchLogDao().getAll().single()
        assertEquals(kemarin, log.tanggal)
        assertEquals(ResearchSchema.Sumber.DARI_NOTIFIKASI, log.sumberPencatatan)
        // Pencatatan susulan: jam = saat menekan (hari ini), tanggal = hari ibadah.
        assertTrue(log.jamPencatatan!! > 0)
    }

    @Test
    fun sudahHariIni_tetapSepertiSebelumnya() = runBlocking {
        repo.setCompleted(isyaId, true, ResearchSchema.Sumber.DARI_NOTIFIKASI)
        assertTrue(repo.isCompletedToday(isyaId))
        assertTrue(db.habitDao().getHabitById(isyaId)!!.isCompleted)
        assertEquals(hariIni, db.researchLogDao().getAll().single().tanggal)
    }

    @Test
    fun centangDariAplikasi_sumberBukaAplikasi() = runBlocking {
        repo.setCompleted(isyaId, true)
        assertEquals(ResearchSchema.Sumber.BUKA_APLIKASI, db.researchLogDao().getAll().single().sumberPencatatan)
    }

    @Test
    fun batalkanKemarin_menghapusCatatanKemarinSaja() = runBlocking {
        repo.setCompleted(isyaId, true, dateKey = hariIni)
        repo.setCompleted(isyaId, true, dateKey = kemarin)
        repo.setCompleted(isyaId, false, dateKey = kemarin)
        assertFalse(repo.isCompletedOn(isyaId, kemarin))
        assertTrue(repo.isCompletedToday(isyaId))
        assertEquals(listOf(hariIni), db.researchLogDao().getAll().map { it.tanggal })
    }

    @Test
    fun tanggalMasaDepan_ditolak() = runBlocking {
        val besok = LocalDate.now().plusDays(1).toString()
        assertNull(repo.setCompleted(isyaId, true, dateKey = besok))
        assertTrue(db.researchLogDao().getAll().isEmpty())
    }

    @Test
    fun tertinggal_menulisTidakDariNotifikasi_tanpaMenyentuhCentang() = runBlocking {
        assertTrue(repo.markTertinggal(isyaId, kemarin))
        val log = db.researchLogDao().getAll().single()
        assertEquals(kemarin, log.tanggal)
        assertEquals(ResearchSchema.Status.TIDAK, log.status)
        assertEquals(ResearchSchema.Sumber.DARI_NOTIFIKASI, log.sumberPencatatan)
        assertTrue(log.jamPencatatan!! > 0)
        assertNull(log.selisihMenitDariAdzan)
        assertFalse(repo.isCompletedOn(isyaId, kemarin))
    }

    @Test
    fun tertinggalLaluDicentang_tertimpaJadiDikerjakan() = runBlocking {
        repo.markTertinggal(isyaId, kemarin)
        repo.setCompleted(isyaId, true, ResearchSchema.Sumber.BUKA_APLIKASI, dateKey = kemarin)
        val log = db.researchLogDao().getAll().single()
        assertEquals(ResearchSchema.Status.DIKERJAKAN, log.status)
        assertEquals(ResearchSchema.Sumber.BUKA_APLIKASI, log.sumberPencatatan)
    }

    @Test
    fun tertinggalSetelahSudah_diabaikan() = runBlocking {
        repo.setCompleted(isyaId, true, ResearchSchema.Sumber.DARI_NOTIFIKASI, dateKey = kemarin)
        assertFalse(repo.markTertinggal(isyaId, kemarin))
        assertEquals(ResearchSchema.Status.DIKERJAKAN, db.researchLogDao().getAll().single().status)
    }

    @Test
    fun tertinggalMasaDepan_ditolak() = runBlocking {
        assertFalse(repo.markTertinggal(isyaId, LocalDate.now().plusDays(1).toString()))
        assertTrue(db.researchLogDao().getAll().isEmpty())
    }

    @Test
    fun progresFardhu_perTanggal() = runBlocking {
        repo.setCompleted(isyaId, true, dateKey = kemarin)
        assertEquals(1 to 1, repo.fardhuProgressOn(kemarin))
        assertEquals(0 to 1, repo.fardhuProgressOn(hariIni))
    }
}
