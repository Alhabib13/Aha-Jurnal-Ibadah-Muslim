package com.islami.Aha.util

import com.islami.Aha.data.research.ResearchSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilPermanenTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        UserPreferencesManager.clearAll()
    }

    @After
    fun tearDown() {
        UserPreferencesManager.clearAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `profil sekali diisi tidak bisa diganti dari aplikasi`() {
        assertFalse(UserPreferencesManager.isGenderLocked)
        assertTrue(UserPreferencesManager.setGender(GenderProfile.FEMALE))
        assertTrue(UserPreferencesManager.isGenderLocked)
        assertFalse(UserPreferencesManager.setGender(GenderProfile.MALE))
        assertFalse(UserPreferencesManager.setGender(GenderProfile.UNSPECIFIED))
        assertEquals(GenderProfile.FEMALE, UserPreferencesManager.gender.value)
        // Memilih nilai yang sama tetap dianggap berhasil.
        assertTrue(UserPreferencesManager.setGender(GenderProfile.FEMALE))
    }

    @Test
    fun `nilai dari server menang, UNSPECIFIED dari server diabaikan`() {
        UserPreferencesManager.setGender(GenderProfile.FEMALE)
        UserPreferencesManager.setGenderFromCloud(GenderProfile.MALE)
        assertEquals(GenderProfile.MALE, UserPreferencesManager.gender.value)
        UserPreferencesManager.setGenderFromCloud(GenderProfile.UNSPECIFIED)
        assertEquals(GenderProfile.MALE, UserPreferencesManager.gender.value)
    }

    @Test
    fun `pengguna dari v1-1-1 - jenis kelamin dari server belum dikunci sampai dikonfirmasi`() {
        // v1.1.1: jenis kelamin ada (bisa nilai lama dari server), usia belum pernah ditanyakan.
        UserPreferencesManager.setGenderFromCloud(GenderProfile.MALE)
        assertFalse(UserPreferencesManager.isGenderLocked)
        assertTrue(UserPreferencesManager.perluKonfirmasiGender)

        // Membetulkan lewat kartu konfirmasi, lalu terkunci.
        UserPreferencesManager.konfirmasiGender(GenderProfile.FEMALE, belumTerkirim = true)
        assertEquals(GenderProfile.FEMALE, UserPreferencesManager.gender.value)
        assertTrue(UserPreferencesManager.isGenderLocked)
        assertFalse(UserPreferencesManager.perluKonfirmasiGender)
        assertFalse(UserPreferencesManager.setGender(GenderProfile.MALE))
    }

    @Test
    fun `konfirmasi yang belum terkirim tidak ditimpa nilai lama dari server`() {
        UserPreferencesManager.setGenderFromCloud(GenderProfile.MALE)
        UserPreferencesManager.konfirmasiGender(GenderProfile.FEMALE, belumTerkirim = true)
        UserPreferencesManager.setGenderFromCloud(GenderProfile.MALE)
        assertEquals(GenderProfile.FEMALE, UserPreferencesManager.gender.value)
        assertTrue(UserPreferencesManager.isGenderBelumTerkirim)
        // Setelah server menerima, nilai server kembali menang (dan sudah sama).
        UserPreferencesManager.genderTerkirim()
        assertFalse(UserPreferencesManager.isGenderBelumTerkirim)
    }

    @Test
    fun `tamu yang mengonfirmasi tetap kalah dari server saat login`() {
        UserPreferencesManager.setGender(GenderProfile.UNSPECIFIED)
        UserPreferencesManager.setGenderFromCloud(GenderProfile.FEMALE)
        UserPreferencesManager.konfirmasiGender(GenderProfile.MALE, belumTerkirim = false)
        UserPreferencesManager.setGenderFromCloud(GenderProfile.FEMALE)
        assertEquals(GenderProfile.FEMALE, UserPreferencesManager.gender.value)
    }

    @Test
    fun `usia terisi (onboarding v1-2-0) = sudah ditegaskan, tidak ditanya lagi`() {
        UserPreferencesManager.setGenderFromCloud(GenderProfile.FEMALE)
        UserPreferencesManager.setAgeRange(AgeRange.AGE_19_25, guardianOk = false)
        assertTrue(UserPreferencesManager.isGenderLocked)
        assertFalse(UserPreferencesManager.perluKonfirmasiGender)
    }

    @Test
    fun `belum memilih jenis kelamin tidak perlu konfirmasi`() {
        assertFalse(UserPreferencesManager.perluKonfirmasiGender)
        UserPreferencesManager.konfirmasiGender(GenderProfile.UNSPECIFIED, belumTerkirim = true)
        assertFalse(UserPreferencesManager.isGenderLocked)
    }

    @Test
    fun `kode rentang usia sama persis dengan skema penelitian`() {
        assertEquals(ResearchSchema.Profil.RENTANG_UMUR_VALUES, AgeRange.entries.map { it.code })
        assertEquals(AgeRange.AGE_15_18, AgeRange.fromCode("15-18"))
        assertNull(AgeRange.fromCode("20"))
    }

    @Test
    fun `kode lama hanya dipetakan bila pasti masuk rentang baru`() {
        assertEquals(AgeRange.UNDER_15, AgeRange.fromCode("<13"))
        assertNull(AgeRange.fromCode("13-17"))
        assertNull(AgeRange.fromCode("18+"))
    }

    @Test
    fun `kode kesibukan sama persis dengan skema penelitian`() {
        assertEquals(ResearchSchema.Profil.STATUS_AKTIVITAS_VALUES, StatusAktivitas.entries.map { it.code })
    }

    @Test
    fun `izin wali hanya berlaku untuk di bawah 18 tahun`() {
        UserPreferencesManager.setAgeRange(AgeRange.AGE_15_18, guardianOk = true)
        assertTrue(UserPreferencesManager.guardianOk.value)
        UserPreferencesManager.setAgeRange(AgeRange.AGE_19_25, guardianOk = true)
        assertFalse(UserPreferencesManager.guardianOk.value)
        UserPreferencesManager.clearAll()
        assertNull(UserPreferencesManager.ageRange.value)
    }
}
