package com.islami.Aha.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.islami.Aha.data.local.AppDatabase
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.model.UserHabitEntity
import com.islami.Aha.data.research.ResearchLogRepository
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.data.research.TilawahStateRepository
import com.islami.Aha.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tombol Tertinggal di kartu puasa & habit sunnah buatan pengguna, plus kode responden. */
@RunWith(AndroidJUnit4::class)
class TertinggalSunnahTest {
    private lateinit var db: AppDatabase
    private lateinit var research: ResearchLogRepository
    private lateinit var sunnah: UserHabitRepository
    private lateinit var bawaan: DefaultHabitCompletionRepository
    private lateinit var prefs: android.content.SharedPreferences
    private val hariIni = DateUtils.getTodayKey()

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        prefs = context.getSharedPreferences("tertinggal-sunnah-test", Context.MODE_PRIVATE)
        prefs.edit().clear().putString(ResearchLogRepository.KEY_RESEARCH_CONSENT, ResearchLogRepository.CONSENT_SETUJU).commit()
        research = ResearchLogRepository(
            context, db.researchLogDao(), db.deletedSyncDao(), prefs, db.habitDao(), TilawahStateRepository(prefs),
            db.dzikirHabitDao()
        )
        val sync = CompletionSyncRepository(db.habitCompletionDao(), db.deletedSyncDao(), prefs)
        sunnah = UserHabitRepository(db, db.userHabitDao(), db.habitCompletionDao(), sync, db.deletedSyncDao(), research)
        bawaan = DefaultHabitCompletionRepository(context, db, db.habitDao(), db.habitCompletionDao(), research, sync, prefs)
        listOf(
            UserHabitEntity(id = "dhuha", name = "Sholat Dhuha", category = "SHOLAT", createdAt = 1L, updatedAt = 1L),
            UserHabitEntity(id = "senkam", name = "Puasa Senin Kamis", category = "PUASA", createdAt = 1L, updatedAt = 1L),
            UserHabitEntity(id = "awwabin", name = "Sholat Awwabin", category = "SHOLAT", createdAt = 1L, updatedAt = 1L)
        ).forEach { db.userHabitDao().insertHabit(it) }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun puasaSunnah_tertinggal_bukaAplikasi_laluBatal() = runBlocking {
        assertTrue(sunnah.markTertinggal("senkam"))
        val log = db.researchLogDao().getAll().single()
        assertEquals(ResearchSchema.Jenis.PUASA, log.jenisIbadah)
        assertEquals(ResearchSchema.Detail.SENIN_KAMIS, log.namaDetail)
        assertEquals(ResearchSchema.Status.TIDAK, log.status)
        assertEquals(ResearchSchema.Sumber.BUKA_APLIKASI, log.sumberPencatatan)
        assertTrue(sunnah.batalkanTertinggal("senkam"))
        assertTrue(db.researchLogDao().getAll().isEmpty())
    }

    @Test
    fun sunnahTertinggal_laluDicentang_tertimpaJadiDikerjakan() = runBlocking {
        sunnah.markTertinggal("dhuha")
        sunnah.toggleHabitCompletion("dhuha")
        val log = db.researchLogDao().getAll().single()
        assertEquals(ResearchSchema.Status.DIKERJAKAN, log.status)
        // Sudah dicentang: Tertinggal diabaikan, batal tidak menyentuh baris dikerjakan.
        assertFalse(sunnah.markTertinggal("dhuha"))
        assertFalse(sunnah.batalkanTertinggal("dhuha"))
        assertEquals(ResearchSchema.Status.DIKERJAKAN, db.researchLogDao().getAll().single().status)
    }

    @Test
    fun habitBuatanSendiri_tertinggalDenganIdLokal() = runBlocking {
        assertTrue(sunnah.markTertinggal("awwabin"))
        val log = db.researchLogDao().getAll().single()
        assertEquals(ResearchSchema.Detail.SUNNAH_LAINNYA, log.namaDetail)
        assertTrue(log.slotKey.endsWith("_awwabin"))
    }

    @Test
    fun puasaRamadan_tertinggalLewatJalurBawaan() = runBlocking {
        db.habitDao().insertHabit(Habit(name = "Puasa Ramadhan", category = "Puasa Wajib", icon = "", description = ""))
        val id = db.habitDao().getHabits().first().single().id
        assertTrue(bawaan.markTertinggal(id, hariIni, ResearchSchema.Sumber.BUKA_APLIKASI))
        val log = db.researchLogDao().getAll().single()
        assertEquals(ResearchSchema.Jenis.PUASA, log.jenisIbadah)
        assertEquals(ResearchSchema.Detail.RAMADAN, log.namaDetail)
        assertEquals(ResearchSchema.Status.TIDAK, log.status)
    }

    @Test
    fun kodeResponden_disimpanHanyaBilaSetujuDanSah() {
        research.setResearchConsent(true, " UNPAM-017 ")
        assertEquals("UNPAM-017", research.kodeResponden())
        research.setResearchConsent(true, "pe-01-unp ")
        assertEquals("PE-01-UNP", research.kodeResponden())
        assertEquals("PE-01-UNP", prefs.getString(ResearchLogRepository.KEY_KODE_RESPONDEN, null))
        // Kode lama yang tersimpan sebelum dibakukan tetap dikirim dalam bentuk baku.
        prefs.edit().putString(ResearchLogRepository.KEY_KODE_RESPONDEN, "Ti-03-Unp").commit()
        assertEquals("TI-03-UNP", research.kodeResponden())
        research.setResearchConsent(true, "x")
        assertNull(research.kodeResponden())
        research.setResearchConsent(true, "R-0001")
        research.setResearchConsent(false, "R-0001")
        assertNull(research.kodeResponden())
        assertEquals(false, research.researchConsent())
    }
}
