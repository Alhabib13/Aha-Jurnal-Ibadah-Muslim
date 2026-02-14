package com.islami.Aha.data.repository

import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val sharedPreferences: SharedPreferences
) {

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
    }

    val currentUser: FirebaseUser? get() = firebaseAuth.currentUser

    val isLoggedIn: Boolean get() = firebaseAuth.currentUser != null

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error("Login gagal")
            syncToPreferences(user)
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    suspend fun register(name: String, email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error("Registrasi gagal")

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdates).await()

            syncToPreferences(user.apply { /* displayName updated */ }, name)
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    fun logout() {
        firebaseAuth.signOut()
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove("user_avatar_uri")
            .apply()
    }

    private fun syncToPreferences(user: FirebaseUser, displayName: String? = null) {
        val name = displayName ?: user.displayName ?: user.email?.substringBefore("@") ?: "Pengguna"
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, user.email ?: "")
            .apply()
    }

    private fun mapFirebaseError(e: Exception): String {
        val msg = e.message ?: return "Terjadi kesalahan"
        return when {
            "email address is badly formatted" in msg -> "Format email tidak valid"
            "no user record" in msg || "INVALID_LOGIN_CREDENTIALS" in msg -> "Email atau password salah"
            "password is invalid" in msg -> "Password salah"
            "email address is already in use" in msg -> "Email sudah terdaftar"
            "password should be at least 6 characters" in msg -> "Password minimal 6 karakter"
            "network error" in msg.lowercase() -> "Tidak ada koneksi internet"
            "too many requests" in msg.lowercase() -> "Terlalu banyak percobaan, coba lagi nanti"
            else -> "Terjadi kesalahan: ${e.localizedMessage}"
        }
    }
}
