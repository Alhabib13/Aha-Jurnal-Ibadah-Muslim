package com.islami.Aha.data.research

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.islami.Aha.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Persetujuan penelitian hanya mengatur pengunggahan. Menolak tidak boleh menghilangkan
 * fitur: dzikir/tilawah/iqro tetap tercatat di HP.
 */
@RunWith(AndroidJUnit4::class)
class ResearchConsentTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ResearchLogRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val prefs = context.getSharedPreferences("research-consent-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repo = ResearchLogRepository(
            context, db.researchLogDao(), db.deletedSyncDao(), prefs, db.habitDao(), TilawahStateRepository(prefs),
            db.dzikirHabitDao()
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun catatDzikir() = repo.recordDzikirSession(
        slug = ResearchSchema.Detail.ISTIGHFAR, idHabit = "habit-a", tanggal = "2026-10-14",
        modePencatatan = ResearchSchema.ModePencatatan.CENTANG, waktuDzikir = ResearchSchema.WaktuDzikir.BEBAS,
        nowMs = 1_791_968_735_434L
    )

    @Test
    fun belumMemutuskan_tetapBisaMencatat() = runBlocking {
        assertNull(repo.researchConsent())
        assertFalse(repo.hasDecidedConsent())
        catatDzikir()
        assertEquals(1, db.researchLogDao().count())
    }

    @Test
    fun menolak_fiturTetapJalan() = runBlocking {
        repo.setResearchConsent(false)
        assertEquals(false, repo.researchConsent())
        assertTrue(repo.hasDecidedConsent())
        catatDzikir()
        repo.addTilawahSession(
            ResearchSchema.Detail.HAFALAN,
            com.islami.Aha.data.quran.QuranCalculator.hitung(
                com.islami.Aha.data.quran.AyatPosition(1, 1), com.islami.Aha.data.quran.AyatPosition(1, 7)
            )!!,
            "2026-10-14"
        )
        assertEquals(2, db.researchLogDao().count())
    }

    private val prefsTes get() = context.getSharedPreferences("research-consent-test", Context.MODE_PRIVATE)

    @Test
    fun mengertiLama_bukanPersetujuan_layarBaruMunculSekali() {
        prefsTes.edit().putBoolean(ResearchLogRepository.KEY_RESEARCH_NOTICE_ACK, true).commit()
        // Belum memutuskan: Beranda menampilkan layar persetujuan baru, tidak ada yang diunggah.
        assertNull(repo.researchConsent())
        assertFalse(repo.hasDecidedConsent())
        // Setelah menjawab di layar itu keputusannya tersimpan, kunci lama hilang, layar tidak muncul lagi.
        repo.setResearchConsent(true, null)
        assertEquals(true, repo.researchConsent())
        assertTrue(repo.hasDecidedConsent())
        assertFalse(prefsTes.contains(ResearchLogRepository.KEY_RESEARCH_NOTICE_ACK))
    }

    @Test
    fun mengertiLama_laluMenolak_tetapTidakDitanyaLagi() {
        prefsTes.edit().putBoolean(ResearchLogRepository.KEY_RESEARCH_NOTICE_ACK, true).commit()
        repo.setResearchConsent(false, null)
        assertEquals(false, repo.researchConsent())
        assertTrue(repo.hasDecidedConsent())
        assertFalse(prefsTes.contains(ResearchLogRepository.KEY_RESEARCH_NOTICE_ACK))
    }

    @Test
    fun berhentiIkut_menandaiPenarikan_setujuLagiMembatalkannya() {
        // Penandaan butuh akun yang sedang masuk (UID); tanpa akun tidak ada dokumen yang ditandai.
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        org.junit.Assume.assumeTrue("Dilewati: tidak ada akun yang masuk", uid != null)
        repo.setResearchConsent(true)
        assertNull(repo.penarikanTertunda())
        repo.setResearchConsent(false)
        assertEquals(uid, repo.penarikanTertunda())
        repo.setResearchConsent(true)
        assertNull(repo.penarikanTertunda())
    }

    @Test
    fun tanpaAkun_menolakTidakMenyisakanPenarikanTertunda() {
        org.junit.Assume.assumeTrue(
            "Dilewati: perangkat sedang memakai akun",
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null
        )
        repo.setResearchConsent(false)
        assertNull(repo.penarikanTertunda())
    }

    @Test
    fun hapusDataTanpaLogin_berhentiIkut_catatanHpTetapAda() = runBlocking {
        // Tes ini memanggil penghapusan data penelitian DI SERVER untuk akun yang sedang masuk.
        // Di perangkat yang sedang memakai akun (termasuk anonim) tes dilewati, supaya data asli
        // di Firestore produksi tidak ikut terhapus.
        org.junit.Assume.assumeTrue(
            "Dilewati: perangkat sedang memakai akun, penghapusan akan mengenai data asli di server",
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null
        )
        repo.setResearchConsent(true)
        catatDzikir()
        repo.deleteMyResearchData()
        assertEquals(false, repo.researchConsent())
        assertEquals(1, db.researchLogDao().count())
        assertTrue(db.researchLogDao().getAll().all { !it.isSynced })
    }

    @Test
    fun logoutMenghapusKeputusan() = runBlocking {
        repo.setResearchConsent(true)
        repo.clearLocal()
        assertNull(repo.researchConsent())
    }
}
