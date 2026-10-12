package com.islami.Aha.data.dzikir

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.islami.Aha.data.local.AppDatabase
import com.islami.Aha.data.model.ResearchLogEntity
import com.islami.Aha.data.research.ResearchSchema
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migrasi pengguna kartu dzikir lama -> HABIT dzikir. Bukti utama: isi research_logs
 * (jumlah baris DAN isi tiap baris) identik sebelum dan sesudah; tidak ada log baru.
 */
@RunWith(AndroidJUnit4::class)
class DzikirHabitMigrationTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: DzikirHabitRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val prefs = context.getSharedPreferences("dzikir-habit-migration-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repo = DzikirHabitRepository(context, db.dzikirHabitDao(), db.researchLogDao(), prefs)
    }

    @After
    fun tearDown() = db.close()

    private fun row(slotKey: String, slug: String, waktu: String, tanggal: String = "2026-10-01") = ResearchLogEntity(
        slotKey = slotKey, tanggal = tanggal, jenisIbadah = "dzikir", namaDetail = slug, namaDetailAsli = null,
        status = "dikerjakan", jumlah = null, jilid = null, jamPencatatan = 1_791_600_000_000L,
        sumberPencatatan = "tidak_diketahui", ownerUid = "uidA", isSynced = true, updatedAt = 1L,
        offsetWaktu = "+07:00", modePencatatan = "centang", waktuDzikir = waktu
    )

    @Test
    fun penggunaKartuPagiSaja_mendapatHabitPagiSaja_tanpaLogBaru() = runBlocking {
        db.researchLogDao().upsert(row("2026-10-01_dzikir_dzikir_pagi_kartu-pagi_1791600000000", "dzikir_pagi", "pagi"))
        db.researchLogDao().upsert(row("2026-10-02_dzikir_dzikir_pagi_kartu-pagi_1791690000000", "dzikir_pagi", "pagi", "2026-10-02"))
        val logSebelum = db.researchLogDao().getAll()

        assertEquals(1, repo.migrateLegacyCardsIfNeeded())

        assertEquals(logSebelum, db.researchLogDao().getAll()) // isi identik, tidak ada baris baru
        val habits = db.dzikirHabitDao().getAll()
        assertEquals(1, habits.size)
        with(habits.single()) {
            assertEquals("kartu-pagi", id)
            assertEquals(ResearchSchema.Detail.DZIKIR_PAGI, slug)
            assertEquals(ResearchSchema.ModePencatatan.CENTANG, modePencatatan)
            assertEquals(ResearchSchema.WaktuDzikir.PAGI, waktuDzikir)
            assertNull(targetHitungan)
            assertNull(namaDetailAsli)
        }
    }

    @Test
    fun keduaKartu_duaHabit_danHanyaSekali() = runBlocking {
        db.researchLogDao().upsert(row("2026-10-01_dzikir_dzikir_pagi_kartu-pagi_1791600000000", "dzikir_pagi", "pagi"))
        db.researchLogDao().upsert(row("2026-10-01_dzikir_dzikir_petang_kartu-petang_1791620000000", "dzikir_petang", "petang"))
        val logSebelum = db.researchLogDao().getAll()

        assertEquals(2, repo.migrateLegacyCardsIfNeeded())
        // Pengguna menghapus satu habit hasil migrasi; peluncuran berikutnya TIDAK membuatnya lagi.
        db.dzikirHabitDao().deleteById("kartu-petang")
        assertEquals(0, repo.migrateLegacyCardsIfNeeded())

        assertEquals(listOf("kartu-pagi"), db.dzikirHabitDao().getAll().map { it.id })
        assertEquals(logSebelum, db.researchLogDao().getAll())
    }

    @Test
    fun penggunaTanpaKartuLama_tidakMendapatApaPun() = runBlocking {
        // Baris dzikir dari habit lain (bukan kartu lama) tidak memicu migrasi.
        db.researchLogDao().upsert(row("2026-10-01_dzikir_dzikir_pagi_habit-x_1791600000000", "dzikir_pagi", "pagi"))
        assertEquals(0, repo.migrateLegacyCardsIfNeeded())
        assertTrue(db.dzikirHabitDao().getAll().isEmpty())
        assertEquals(1, db.researchLogDao().count())
    }

    @Test
    fun tambahHabitDariAddHabit_tidakMenulisLog() = runBlocking {
        val err = repo.addAll(
            listOf(
                DzikirHabitDraft(ResearchSchema.Detail.ISTIGHFAR),
                DzikirHabitDraft(ResearchSchema.Detail.DZIKIR_LAINNYA, namaDetailAsli = "Hizib A"),
                DzikirHabitDraft(ResearchSchema.Detail.DZIKIR_LAINNYA, namaDetailAsli = "Hizib B", targetHitungan = 7)
            )
        )
        assertNull(err)
        assertEquals(3, db.dzikirHabitDao().getAll().size)
        assertEquals(0, db.researchLogDao().count())
        // Mode layar ditolak selama katalog tanpa bacaan.
        val layar = repo.addAll(listOf(DzikirHabitDraft(ResearchSchema.Detail.TAHLIL, modePencatatan = "layar")))
        assertTrue(layar != null)
        assertEquals(3, db.dzikirHabitDao().getAll().size)
    }
}
