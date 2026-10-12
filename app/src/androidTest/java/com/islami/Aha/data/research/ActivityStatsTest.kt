package com.islami.Aha.data.research

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.islami.Aha.data.local.AppDatabase
import com.islami.Aha.data.model.DzikirHabitEntity
import com.islami.Aha.data.model.HabitCompletionRecord
import com.islami.Aha.data.model.ResearchLogEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Heatmap/streak/total membaca dua tabel tanpa menghitung ganda. */
@RunWith(AndroidJUnit4::class)
class ActivityStatsTest {

    private lateinit var db: AppDatabase
    private lateinit var stats: ActivityStatsRepository
    private val today = LocalDate.now()
    private fun day(ago: Long) = today.minusDays(ago).toString()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        stats = ActivityStatsRepository(db.habitCompletionDao(), db.researchLogDao(), db.dzikirHabitDao())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun completion(habitKey: String, date: String) =
        db.habitCompletionDao().insert(HabitCompletionRecord(habitKey = habitKey, dateKey = date, category = "Sholat Fardhu", source = "DEFAULT"))

    private suspend fun log(
        slotKey: String, date: String, jenis: String, detail: String?,
        status: String = "dikerjakan", jumlah: Int? = null, jilid: Int? = null
    ) = db.researchLogDao().upsert(
        ResearchLogEntity(
            slotKey = slotKey, tanggal = date, jenisIbadah = jenis, namaDetail = detail, namaDetailAsli = null,
            status = status, jumlah = jumlah, jilid = jilid,
            jamPencatatan = if (status == "dikerjakan") 1L else null,
            sumberPencatatan = if (status == "dikerjakan") "tidak_diketahui" else null,
            ownerUid = null, updatedAt = 1L, offsetWaktu = "+07:00"
        )
    )

    private fun habit(id: String, slug: String, waktu: String) = DzikirHabitEntity(
        id = id, slug = slug, namaDetailAsli = null, modePencatatan = "centang",
        waktuDzikir = waktu, targetHitungan = null, createdAt = 1L, ownerUid = null
    )

    @Test
    fun withoutNewIbadah_numbersAreIdenticalToOldDao() = runBlocking {
        listOf(0L, 1L, 2L, 5L).forEach { ago -> (1..3).forEach { completion("default_$it", day(ago)) } }
        // Sholat juga tercatat di research_logs (penulisan ganda) -> tidak boleh ikut terhitung.
        log("${day(0)}_sholat_wajib_subuh", day(0), "sholat_wajib", "subuh")
        log("${day(1)}_sholat_wajib_isya", day(1), "sholat_wajib", "isya", status = "tidak")
        val old = db.habitCompletionDao()
        assertEquals(old.getDailyCountsBetween(day(6), day(0)).sortedBy { it.dateKey }, stats.getDailyCountsBetween(day(6), day(0)))
        assertEquals(old.getDistinctCompletionDatesDesc(), stats.getDistinctCompletionDatesDesc())
        assertEquals(old.getTotalCompletionCount(), stats.getTotalCompletionCount())
    }

    @Test
    fun dzikirTilawahIqro_countedOncePerUnit_andStatusFiltered() = runBlocking {
        completion("default_1", day(0))
        log("${day(0)}_sholat_wajib_subuh", day(0), "sholat_wajib", "subuh") // ganda, diabaikan
        db.dzikirHabitDao().upsert(habit("h-pagi", "dzikir_pagi", "pagi"))
        db.dzikirHabitDao().upsert(habit("h-bada", "dzikir_bada_sholat", "bada_sholat"))
        db.dzikirHabitDao().upsert(habit("h-istighfar", "istighfar", "bebas"))
        log("${day(0)}_dzikir_dzikir_pagi_h-pagi_1791600000000", day(0), "dzikir", "dzikir_pagi")
        // Tiga sesi ba'da sholat di hari yang sama = tetap SATU habit dikerjakan.
        log("${day(0)}_dzikir_dzikir_bada_sholat_h-bada_1791600000000", day(0), "dzikir", "dzikir_bada_sholat")
        log("${day(0)}_dzikir_dzikir_bada_sholat_h-bada_1791610000000", day(0), "dzikir", "dzikir_bada_sholat")
        log("${day(0)}_dzikir_dzikir_bada_sholat_h-bada_1791620000000", day(0), "dzikir", "dzikir_bada_sholat")
        log("${day(0)}_tilawah_bacaan_harian_a", day(0), "tilawah", "bacaan_harian", jumlah = 3)
        log("${day(0)}_tilawah_hafalan_b", day(0), "tilawah", "hafalan", jumlah = 2)
        log("${day(0)}_tilawah_murajaah_c", day(0), "tilawah", "murajaah", jumlah = 5)
        log("${day(0)}_iqro", day(0), "iqro", null, jumlah = 12, jilid = 3)
        log("${day(1)}_dzikir_dzikir_pagi_h-pagi_1791500000000", day(1), "dzikir", "dzikir_pagi", status = "tidak")   // bukan dikerjakan
        log("${day(1)}_puasa_ramadan", day(1), "puasa", "ramadan", status = "udzur")

        // hari ini: 1 sholat + 2 habit dzikir (4 sesi) + 1 tilawah (3 sesi) + 1 iqro = 5
        assertEquals(listOf(day(0) to 5), stats.getDailyCountsBetween(day(6), day(0)).map { it.dateKey to it.count })
        assertEquals(5, stats.getTotalCompletionCount())
        assertEquals(listOf(day(0)), stats.getDistinctCompletionDatesDesc())
        // Rasio: 2 dari 3 habit (istighfar ber-waktu bebas tetap masuk penyebut).
        assertEquals(ActivityStatsRepository.DzikirProgress(2, 3), stats.getDzikirProgress(day(0), modeCutiAktif = false))
        assertEquals(ActivityStatsRepository.DzikirProgress(0, 3), stats.getDzikirProgress(day(1), modeCutiAktif = false))
        // Bawaan ModeCutiPolicy: dzikir tetap aktif saat Mode Cuti.
        assertEquals(ActivityStatsRepository.DzikirProgress(2, 3), stats.getDzikirProgress(day(0), modeCutiAktif = true))
    }

    @Test
    fun dzikirOnlyDay_keepsStreak() = runBlocking {
        completion("default_1", day(0))
        log("${day(1)}_dzikir_dzikir_pagi_h-pagi_1791500000000", day(1), "dzikir", "dzikir_pagi")   // hanya dzikir
        log("${day(2)}_tilawah_hafalan_x", day(2), "tilawah", "hafalan", jumlah = 1)  // hanya tilawah
        completion("default_2", day(3))
        assertEquals(listOf(day(0), day(1), day(2), day(3)), stats.getDistinctCompletionDatesDesc())
        assertEquals(4, stats.getTotalCompletionCount())
    }
}
