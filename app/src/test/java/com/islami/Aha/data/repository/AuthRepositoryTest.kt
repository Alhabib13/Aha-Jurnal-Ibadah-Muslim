package com.islami.Aha.data.repository

import android.content.SharedPreferences
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AuthRepository
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockAuth = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
        repository = AuthRepository(mockAuth, mockPrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isLoggedIn returns true when currentUser exists`() {
        val mockUser = mockk<FirebaseUser>()
        // Akun sungguhan (bukan anonim).
        every { mockUser.isAnonymous } returns false
        every { mockAuth.currentUser } returns mockUser
        assertTrue(repository.isLoggedIn)
    }

    @Test
    fun `isLoggedIn returns false when no currentUser`() {
        every { mockAuth.currentUser } returns null
        assertFalse(repository.isLoggedIn)
    }

    @Test
    fun `currentUser delegates to firebaseAuth`() {
        val mockUser = mockk<FirebaseUser>()
        every { mockAuth.currentUser } returns mockUser
        assertEquals(mockUser, repository.currentUser)
    }

    @Test
    fun `logout clears preferences and signs out`() {
        repository.logout()

        verify { mockAuth.signOut() }
        verify { mockEditor.putBoolean("is_logged_in", false) }
        verify { mockEditor.remove("user_name") }
        verify { mockEditor.remove("user_email") }
        verify { mockEditor.remove("user_avatar_uri") }
        verify { mockEditor.apply() }
    }

    // ---------- Anonymous Auth ----------

    private fun userAnonim(): FirebaseUser = mockk<FirebaseUser>().also {
        every { it.isAnonymous } returns true
        every { it.uid } returns "anon-1"
    }

    @Test
    fun `tamu anonim bukan login, tapi tercatat sebagai anonim`() {
        every { mockAuth.currentUser } returns userAnonim()
        assertFalse(repository.isLoggedIn)
        assertTrue(repository.isAnonim)
    }

    @Test
    fun `pernah login - dari tanda baru, sesi lama, atau jejak kunci per akun versi lama`() {
        every { mockPrefs.all } returns emptyMap<String, Any>()
        assertFalse(repository.pernahLogin())
        every { mockPrefs.getBoolean("pernah_login_akun", false) } returns true
        assertTrue(repository.pernahLogin())
        every { mockPrefs.getBoolean("pernah_login_akun", false) } returns false
        every { mockPrefs.all } returns mapOf("research_profil_terkirim_uidLama" to "x")
        assertTrue(repository.pernahLogin())
    }

    @Test
    fun `tidak membuat akun anonim bila sudah ada sesi atau pernah login`() = runTest {
        every { mockAuth.currentUser } returns userAnonim()
        assertFalse(repository.masukAnonimBilaPerlu())
        every { mockAuth.currentUser } returns null
        every { mockPrefs.getBoolean("pernah_login_akun", false) } returns true
        assertFalse(repository.masukAnonimBilaPerlu())
        verify(exactly = 0) { mockAuth.signInAnonymously() }
    }

    @Test
    fun `tamu anonim login email ke akun lain - tidak dicoba tanpa penegasan`() = runTest {
        every { mockAuth.currentUser } returns userAnonim()
        assertEquals(AuthResult.PerluKonfirmasiPindahAkun, repository.login("a@b.c", "rahasia1"))
        verify(exactly = 0) { mockAuth.signInWithEmailAndPassword(any(), any()) }
    }

    @Test
    fun `hapus akun anonim ditolak bila tidak ada akun atau akunnya bukan anonim`() = runTest {
        every { mockAuth.currentUser } returns null
        assertTrue(repository.hapusAkunAnonim().isFailure)
        val akunSungguhan = mockk<FirebaseUser>()
        every { akunSungguhan.isAnonymous } returns false
        every { akunSungguhan.uid } returns "uid-asli"
        every { mockAuth.currentUser } returns akunSungguhan
        assertTrue(repository.hapusAkunAnonim().isFailure)
        verify(exactly = 0) { akunSungguhan.delete() }
    }
}
