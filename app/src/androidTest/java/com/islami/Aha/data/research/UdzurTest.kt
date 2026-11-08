package com.islami.Aha.data.research

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.islami.Aha.data.local.AppDatabase
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.research.ResearchSchema.Detail
import com.islami.Aha.data.research.ResearchSchema.Status
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Mode Cuti (Tahap 5): baris "udzur" selalu sama dengan tanggal haid — ditulis untuk slot yang
 * kosong, tidak menimpa catatan yang ada, dan dihapus lagi bila tanggalnya dicabut.
 */
@RunWith(AndroidJUnit4::class)
class UdzurTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ResearchLogRepository
    private val hariIni = LocalDate.parse("2026-11-08")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val prefs = context.getSharedPreferences("udzur-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repo = ResearchLogRepository(
            context, db.researchLogDao(), db.deletedSyncDao(), prefs, db.habitDao(), TilawahStateRepository(prefs),
            db.dzikirHabitDao()
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun baris(tanggal: String) = db.researchLogDao().getAll().filter { it.tanggal == tanggal }
    private val sholat = { d: String -> SlotRef(IbadahSlot(ResearchSchema.Jenis.SHOLAT_WAJIB, d, null), null) }

    @Test
    fun perempuanHaid_limaSholatUdzur_tanpaJamDanSumber(): Unit = runBlocking {
        repo.sinkronUdzur(setOf("2026-11-07", "2026-11-08"), perempuan = true, hariIni = hariIni)
        for (t in listOf("2026-11-07", "2026-11-08")) {
            val b = baris(t)
            assertEquals(5, b.size)
            assertTrue(b.all { it.status == Status.UDZUR && it.jamPencatatan == null && it.sumberPencatatan == null })
            assertEquals(setOf(Detail.SUBUH, Detail.DZUHUR, Detail.ASHAR, Detail.MAGHRIB, Detail.ISYA), b.map { it.namaDetail }.toSet())
        }
    }

    @Test
    fun catatanYangAda_tidakDitimpa(): Unit = runBlocking {
        // Subuh dikerjakan sebelum haid mulai, Dzuhur ditandai Tertinggal.
        repo.recordDikerjakan(sholat(Detail.SUBUH), "2026-11-08")
        repo.recordTidakDariNotifikasi(sholat(Detail.DZUHUR), "2026-11-08", sumber = ResearchSchema.Sumber.BUKA_APLIKASI)
        repo.sinkronUdzur(setOf("2026-11-08"), perempuan = true, hariIni = hariIni)
        val status = baris("2026-11-08").associate { it.namaDetail to it.status }
        assertEquals(Status.DIKERJAKAN, status[Detail.SUBUH])
        assertEquals(Status.TIDAK, status[Detail.DZUHUR])
        assertEquals(Status.UDZUR, status[Detail.ASHAR])
        assertEquals(5, status.size)
    }

    @Test
    fun modeCutiDimatikan_udzurTanggalItuDihapus_catatanLainTetap(): Unit = runBlocking {
        repo.recordDikerjakan(sholat(Detail.SUBUH), "2026-11-08")
        repo.sinkronUdzur(setOf("2026-11-07", "2026-11-08"), perempuan = true, hariIni = hariIni)
        repo.sinkronUdzur(setOf("2026-11-07"), perempuan = true, hariIni = hariIni)
        assertEquals(5, baris("2026-11-07").size)
        assertEquals(listOf(Status.DIKERJAKAN), baris("2026-11-08").map { it.status })
    }

    @Test
    fun lakiLaki_tidakPernahUdzur_danUdzurLamaDibersihkan(): Unit = runBlocking {
        repo.sinkronUdzur(setOf("2026-11-08"), perempuan = true, hariIni = hariIni)
        repo.sinkronUdzur(setOf("2026-11-08"), perempuan = false, hariIni = hariIni)
        assertTrue(baris("2026-11-08").isEmpty())
    }

    @Test
    fun tanggalDiLuarJendelaAtauMasaDepan_diabaikan(): Unit = runBlocking {
        val lama = hariIni.minusDays(ResearchLogRepository.UDZUR_JENDELA_HARI + 1).toString()
        repo.sinkronUdzur(setOf(lama, "2026-11-09"), perempuan = true, hariIni = hariIni)
        assertEquals(0, db.researchLogDao().count())
    }

    @Test
    fun sinkronUlang_tidakMenggandakan(): Unit = runBlocking {
        repo.sinkronUdzur(setOf("2026-11-08"), perempuan = true, hariIni = hariIni)
        assertEquals(0, repo.sinkronUdzur(setOf("2026-11-08"), perempuan = true, hariIni = hariIni))
        assertEquals(5, db.researchLogDao().count())
    }

    @Test
    fun ramadan_puasaWajibIkutUdzur_bilaHabitnyaAda(): Unit = runBlocking {
        // 1 Maret 2026 jatuh di Ramadan 1447 H.
        val ramadan = LocalDate.parse("2026-03-01")
        repo.sinkronUdzur(setOf("2026-03-01"), perempuan = true, hariIni = ramadan)
        assertEquals(5, baris("2026-03-01").size)
        db.habitDao().insertHabit(Habit(name = "Puasa Ramadan", category = "Puasa Wajib", icon = "", description = ""))
        repo.sinkronUdzur(setOf("2026-03-01"), perempuan = true, hariIni = ramadan)
        val puasa = baris("2026-03-01").single { it.jenisIbadah == ResearchSchema.Jenis.PUASA }
        assertEquals(Detail.RAMADAN, puasa.namaDetail)
        assertEquals(Status.UDZUR, puasa.status)
        // Di luar Ramadan puasa wajib tidak ditandai.
        repo.sinkronUdzur(setOf("2026-11-08"), perempuan = true, hariIni = hariIni)
        assertTrue(baris("2026-11-08").none { it.jenisIbadah == ResearchSchema.Jenis.PUASA })
    }
}
