package com.islami.Aha.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.islami.Aha.data.local.AppDatabase
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.model.ResearchLogEntity
import com.islami.Aha.data.research.ResearchLogRepository
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.data.research.ResearchSchema.Detail
import com.islami.Aha.data.research.TilawahStateRepository
import com.islami.Aha.util.UserPreferencesManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Slot tengah hari di hari Jumat: Jumat (laki-laki) atau Dzuhur, tidak pernah keduanya. */
@RunWith(AndroidJUnit4::class)
class SholatJumatRepoTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: DefaultHabitCompletionRepository
    private val jumat = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY)).toString()
    private val kamis = LocalDate.parse(jumat).minusDays(1).toString()
    private var dzuhurId = 0
    private var jumatSemula = false

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val prefs = context.getSharedPreferences("sholat-jumat-test", Context.MODE_PRIVATE)
        prefs.edit().clear().putString(ResearchLogRepository.KEY_RESEARCH_CONSENT, ResearchLogRepository.CONSENT_SETUJU).commit()
        val research = ResearchLogRepository(
            context, db.researchLogDao(), db.deletedSyncDao(), prefs, db.habitDao(), TilawahStateRepository(prefs),
            db.dzikirHabitDao()
        )
        repo = DefaultHabitCompletionRepository(
            context, db, db.habitDao(), db.habitCompletionDao(), research,
            CompletionSyncRepository(db.habitCompletionDao(), db.deletedSyncDao(), prefs), prefs
        )
        listOf("Sholat Subuh", "Sholat Dzuhur", "Sholat Ashar", "Sholat Maghrib", "Sholat Isya").forEach {
            db.habitDao().insertHabit(Habit(name = it, category = "Sholat Fardhu", icon = "", description = ""))
        }
        dzuhurId = db.habitDao().getFardhuHabits().single { it.name == "Sholat Dzuhur" }.id
        jumatSemula = UserPreferencesManager.isJumatEnabled.value
        UserPreferencesManager.setJumatEnabled(true) // laki-laki
    }

    @After
    fun tearDown() {
        UserPreferencesManager.setJumatEnabled(jumatSemula)
        db.close()
    }

    private suspend fun barisTengahHari(tanggal: String) =
        db.researchLogDao().getAll().filter { it.tanggal == tanggal && it.namaDetail in setOf(Detail.JUMAT, Detail.DZUHUR) }

    @Test
    fun lakiLakiHariJumat_centangDzuhurTercatatJumat() = runBlocking {
        repo.setCompleted(dzuhurId, true, dateKey = jumat)
        val log = barisTengahHari(jumat).single()
        assertEquals(Detail.JUMAT, log.namaDetail)
        assertEquals("Sholat Jumat", log.namaDetailAsli)
        assertEquals(ResearchSchema.Sumber.BUKA_APLIKASI, log.sumberPencatatan)
    }

    @Test
    fun perempuanDanHariBiasa_tetapDzuhur() = runBlocking {
        repo.setCompleted(dzuhurId, true, dateKey = kamis)
        assertEquals(Detail.DZUHUR, barisTengahHari(kamis).single().namaDetail)
        UserPreferencesManager.setJumatEnabled(false)
        repo.setCompleted(dzuhurId, true, dateKey = jumat)
        assertEquals(Detail.DZUHUR, barisTengahHari(jumat).single().namaDetail)
    }

    @Test
    fun gantiKeDzuhur_menghapusCatatanJumat_laluCentangDzuhur() = runBlocking {
        repo.setCompleted(dzuhurId, true, dateKey = jumat)
        assertTrue(repo.gantiTengahHari(dzuhurId, jumat, keDzuhur = true))
        assertFalse(repo.isCompletedOn(dzuhurId, jumat))
        assertTrue(barisTengahHari(jumat).isEmpty())
        assertEquals(Detail.DZUHUR, repo.detailTengahHari(jumat))

        repo.setCompleted(dzuhurId, true, dateKey = jumat)
        assertEquals(Detail.DZUHUR, barisTengahHari(jumat).single().namaDetail)

        // Kembali ke Jumat: catatan Dzuhur ikut hilang.
        assertTrue(repo.gantiTengahHari(dzuhurId, jumat, keDzuhur = false))
        assertTrue(barisTengahHari(jumat).isEmpty())
        assertEquals(Detail.JUMAT, repo.detailTengahHari(jumat))
    }

    @Test
    fun saling_meniadakan_barisLawanTerhapusSaatMenulis() = runBlocking {
        // Pilihan hari itu tegas Jumat, tapi ada baris dzuhur basi (mis. dari pemulihan).
        repo.gantiTengahHari(dzuhurId, jumat, keDzuhur = false)
        db.researchLogDao().upsert(
            ResearchLogEntity(
                slotKey = "${jumat}_sholat_wajib_dzuhur", tanggal = jumat, jenisIbadah = ResearchSchema.Jenis.SHOLAT_WAJIB,
                namaDetail = Detail.DZUHUR, namaDetailAsli = null, status = ResearchSchema.Status.DIKERJAKAN,
                jumlah = null, jilid = null, jamPencatatan = 1L, sumberPencatatan = ResearchSchema.Sumber.BUKA_APLIKASI,
                ownerUid = null, updatedAt = 1L, offsetWaktu = "+07:00"
            )
        )
        repo.setCompleted(dzuhurId, true, dateKey = jumat)
        assertEquals(listOf(Detail.JUMAT), barisTengahHari(jumat).map { it.namaDetail })
    }

    @Test
    fun tertinggalDariBeranda_sumberBukaAplikasi_dapatDibatalkan() = runBlocking {
        assertTrue(repo.markTertinggal(dzuhurId, jumat, ResearchSchema.Sumber.BUKA_APLIKASI))
        val log = barisTengahHari(jumat).single()
        assertEquals(Detail.JUMAT, log.namaDetail)
        assertEquals(ResearchSchema.Status.TIDAK, log.status)
        assertEquals(ResearchSchema.Sumber.BUKA_APLIKASI, log.sumberPencatatan)

        assertTrue(repo.batalkanTertinggal(dzuhurId, jumat))
        assertTrue(barisTengahHari(jumat).isEmpty())
        // Batal tidak menyentuh baris "dikerjakan".
        repo.setCompleted(dzuhurId, true, dateKey = jumat)
        assertFalse(repo.batalkanTertinggal(dzuhurId, jumat))
        assertEquals(ResearchSchema.Status.DIKERJAKAN, barisTengahHari(jumat).single().status)
    }

    @Test
    fun tertinggalJumat_laluGantiDzuhurDanCentang_hanyaDzuhur() = runBlocking {
        repo.markTertinggal(dzuhurId, jumat, ResearchSchema.Sumber.DARI_NOTIFIKASI)
        repo.gantiTengahHari(dzuhurId, jumat, keDzuhur = true)
        repo.setCompleted(dzuhurId, true, dateKey = jumat)
        val log = barisTengahHari(jumat).single()
        assertEquals(Detail.DZUHUR, log.namaDetail)
        assertEquals(ResearchSchema.Status.DIKERJAKAN, log.status)
    }

    @Test
    fun sumberTertinggalTakSah_ditolak() = runBlocking {
        assertFalse(repo.markTertinggal(dzuhurId, jumat, ResearchSchema.Sumber.DARI_NOTIFIKASI_DIBUKA))
        assertTrue(barisTengahHari(jumat).isEmpty())
    }

    @Test
    fun kelengkapanHariJumat_tetapLimaSlot() = runBlocking {
        db.habitDao().getFardhuHabits().forEach { repo.setCompleted(it.id, true, dateKey = jumat) }
        assertEquals(5 to 5, repo.fardhuProgressOn(jumat))
        val detail = db.researchLogDao().getAll().filter { it.tanggal == jumat }.map { it.namaDetail }.toSet()
        assertEquals(setOf(Detail.SUBUH, Detail.JUMAT, Detail.ASHAR, Detail.MAGHRIB, Detail.ISYA), detail)
    }

    @Test
    fun habitBernamaSholatJumat_diHariBiasa_tercatatDzuhur_bukanDitolak() = runBlocking {
        // Habit bawaan yang terlanjur bernama "Sholat Jumat" (bug lama sebelum dirapikan Beranda).
        db.habitDao().updateHabit(db.habitDao().getHabitById(dzuhurId)!!.copy(name = "Sholat Jumat"))
        repo.setCompleted(dzuhurId, true, dateKey = kamis)
        val log = barisTengahHari(kamis).single()
        assertEquals(ResearchSchema.Jenis.SHOLAT_WAJIB, log.jenisIbadah)
        assertEquals(Detail.DZUHUR, log.namaDetail)
        repo.setCompleted(dzuhurId, true, dateKey = jumat)
        assertEquals(Detail.JUMAT, barisTengahHari(jumat).single().namaDetail)
    }

    @Test
    fun gantiTidakBerlaku_untukPerempuanAtauHariBiasa() = runBlocking {
        assertFalse(repo.gantiTengahHari(dzuhurId, kamis, keDzuhur = true))
        UserPreferencesManager.setJumatEnabled(false)
        assertFalse(repo.gantiTengahHari(dzuhurId, jumat, keDzuhur = true))
    }
}
