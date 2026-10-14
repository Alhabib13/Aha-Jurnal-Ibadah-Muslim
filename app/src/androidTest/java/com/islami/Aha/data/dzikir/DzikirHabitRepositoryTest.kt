package com.islami.Aha.data.dzikir

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.islami.Aha.data.local.AppDatabase
import com.islami.Aha.data.research.ResearchSchema
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Habit dzikir = konfigurasi: menambah / menghapus habit tidak pernah menulis research_logs. */
@RunWith(AndroidJUnit4::class)
class DzikirHabitRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: DzikirHabitRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = DzikirHabitRepository(context, db.dzikirHabitDao())
    }

    @After
    fun tearDown() = db.close()

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
        val habits = db.dzikirHabitDao().getAll()
        assertEquals(3, habits.size)
        assertEquals(ResearchSchema.WaktuDzikir.BEBAS, habits.first { it.slug == ResearchSchema.Detail.ISTIGHFAR }.waktuDzikir)
        assertEquals(0, db.researchLogDao().count())
    }

    @Test
    fun modeLayarDitolak_danTidakAdaYangTersimpanSebagian() = runBlocking {
        val err = repo.addAll(
            listOf(
                DzikirHabitDraft(ResearchSchema.Detail.SHOLAWAT),
                DzikirHabitDraft(ResearchSchema.Detail.TAHLIL, modePencatatan = ResearchSchema.ModePencatatan.LAYAR)
            )
        )
        assertTrue(err != null)
        assertTrue(db.dzikirHabitDao().getAll().isEmpty())
        assertEquals(0, db.researchLogDao().count())
    }

    @Test
    fun hapusHabit_tidakMenyentuhLog() = runBlocking {
        repo.addAll(listOf(DzikirHabitDraft(ResearchSchema.Detail.DZIKIR_PAGI)))
        val id = db.dzikirHabitDao().getAll().single().id
        repo.remove(id)
        assertTrue(db.dzikirHabitDao().getAll().isEmpty())
        assertEquals(0, db.researchLogDao().count())
    }
}
