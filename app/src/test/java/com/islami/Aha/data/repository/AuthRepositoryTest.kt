package com.islami.Aha.data.repository

import android.content.SharedPreferences
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
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
}
