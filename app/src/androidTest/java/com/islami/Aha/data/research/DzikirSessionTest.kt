package com.islami.Aha.data.research

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.islami.Aha.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Satu baris per sesi dzikir + jendela anti-dobel-ketuk 60 detik, dengan database sungguhan. */
@RunWith(AndroidJUnit4::class)
class DzikirSessionTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ResearchLogRepository
    private val tanggal = "2026-10-12"
    private val t0 = 1_791_600_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val prefs = context.getSharedPreferences("dzikir-session-test", Context.MODE_PRIVATE)
        prefs.edit().clear().putBoolean(ResearchLogRepository.KEY_RESEARCH_NOTICE_ACK, true).commit()
        repo = ResearchLogRepository(
            context, db.researchLogDao(), db.deletedSyncDao(), prefs, db.habitDao(), TilawahStateRepository(prefs),
            db.dzikirHabitDao()
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun catat(nowMs: Long, habit: String = "habit-a", slug: String = ResearchSchema.Detail.DZIKIR_BADA_SHOLAT) =
        repo.recordDzikirSession(
            slug = slug, idHabit = habit, tanggal = tanggal,
            modePencatatan = ResearchSchema.ModePencatatan.CENTANG,
            waktuDzikir = ResearchSchema.WaktuDzikir.BADA_SHOLAT, nowMs = nowMs
        )

    @Test
    fun ketukanGandaDalam60Detik_satuBaris_jamAwalDipertahankan() = runBlocking {
        val k1 = catat(t0)
        val k2 = catat(t0 + 1_500)      // dobel ketuk
        val k3 = catat(t0 + 59_000)     // masih dalam jendela
        assertEquals(k1, k2)
        assertEquals(k1, k3)
        val rows = db.researchLogDao().getAll()
        assertEquals(1, rows.size)
        assertEquals(t0, rows.single().jamPencatatan)
        assertEquals("${tanggal}_dzikir_dzikir_bada_sholat_habit-a_$t0", rows.single().slotKey)
    }

    @Test
    fun sesiBaruSetelah60Detik_danSesiLainHariYangSama() = runBlocking {
        catat(t0)
        catat(t0 + 61_000)                  // lewat jendela -> sesi kedua
        catat(t0 + 6 * 3_600_000L)          // ba'da sholat berikutnya
        val rows = db.researchLogDao().getAll()
        assertEquals(3, rows.size)
        assertEquals(setOf(t0, t0 + 61_000, t0 + 6 * 3_600_000L), rows.mapNotNull { it.jamPencatatan }.toSet())
    }

    @Test
    fun habitAtauSlugBerbeda_tidakDigabung() = runBlocking {
        catat(t0, habit = "habit-a")
        catat(t0 + 1_000, habit = "habit-b")
        catat(t0 + 2_000, slug = ResearchSchema.Detail.ISTIGHFAR)
        assertEquals(3, db.researchLogDao().getAll().size)
    }

    @Test
    fun modeCentang_jumlahDanDurasiSelaluNull() = runBlocking {
        repo.recordDzikirSession(
            slug = ResearchSchema.Detail.ISTIGHFAR, idHabit = "habit-a", tanggal = tanggal,
            modePencatatan = ResearchSchema.ModePencatatan.CENTANG, waktuDzikir = ResearchSchema.WaktuDzikir.BEBAS,
            targetHitungan = 100, jumlah = 100, durasiDetik = 300, nowMs = t0
        )
        val row = db.researchLogDao().getAll().single()
        assertNull(row.jumlah)
        assertNull(row.durasiDetik)
        assertEquals(100, row.targetHitungan)
    }

    @Test
    fun dzikirLainnyaTanpaNamaTidakTercatat() = runBlocking {
        val key = repo.recordDzikirSession(
            slug = ResearchSchema.Detail.DZIKIR_LAINNYA, idHabit = "habit-c", tanggal = tanggal,
            modePencatatan = ResearchSchema.ModePencatatan.CENTANG, waktuDzikir = ResearchSchema.WaktuDzikir.BEBAS,
            namaDetailAsli = "  ", nowMs = t0
        )
        assertNull(key)
        assertTrue(db.researchLogDao().getAll().isEmpty())
    }

    @Test
    fun batalkanTerakhir_hanyaMenghapusSesiTerakhirHabitItu() = runBlocking {
        catat(t0)
        catat(t0 + 3_600_000L)
        catat(t0 + 1_000, habit = "habit-b")
        assertTrue(repo.removeLatestDzikirSession(ResearchSchema.Detail.DZIKIR_BADA_SHOLAT, "habit-a", tanggal))
        val sisa = db.researchLogDao().getAll().map { it.slotKey }.toSet()
        assertEquals(
            setOf(
                "${tanggal}_dzikir_dzikir_bada_sholat_habit-a_$t0",
                "${tanggal}_dzikir_dzikir_bada_sholat_habit-b_${t0 + 1_000}"
            ),
            sisa
        )
    }

    @Test
    fun sumberPencatatanSelaluTidakDiketahui() = runBlocking {
        catat(t0)
        assertEquals(ResearchSchema.Sumber.TIDAK_DIKETAHUI, db.researchLogDao().getAll().single().sumberPencatatan)
    }
}
