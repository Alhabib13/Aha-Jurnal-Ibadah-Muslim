package com.islami.Aha.util

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
    fun `kode rentang usia sama persis dengan firestore rules`() {
        assertEquals(listOf("<13", "13-17", "18+"), AgeRange.entries.map { it.code })
        assertEquals(AgeRange.TEEN, AgeRange.fromCode("13-17"))
        assertNull(AgeRange.fromCode("20"))
    }

    @Test
    fun `izin wali hanya berlaku untuk di bawah 18 tahun`() {
        UserPreferencesManager.setAgeRange(AgeRange.TEEN, guardianOk = true)
        assertTrue(UserPreferencesManager.guardianOk.value)
        UserPreferencesManager.setAgeRange(AgeRange.ADULT, guardianOk = true)
        assertFalse(UserPreferencesManager.guardianOk.value)
        UserPreferencesManager.clearAll()
        assertNull(UserPreferencesManager.ageRange.value)
    }
}
