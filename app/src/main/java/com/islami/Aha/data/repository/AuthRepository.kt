package com.islami.Aha.data.repository

import android.content.SharedPreferences
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class ReAuthRequiredException : Exception("Re-authentication required")

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
    private val firestore: FirebaseFirestore? by lazy {
        runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }

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

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return runCatching {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Unit
        }.onFailure { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.failure(
            IllegalStateException("User tidak login")
        )
        return runCatching {
            deleteCloudData(user.uid)
            user.delete().await()
            clearSession()
        }.recoverCatching { error ->
            if (error is FirebaseAuthRecentLoginRequiredException) {
                throw ReAuthRequiredException()
            }
            throw error
        }.onFailure { error ->
            if (error !is ReAuthRequiredException) {
                FirebaseCrashlytics.getInstance().recordException(error)
            }
        }
    }

    suspend fun reAuthenticateAndDelete(password: String): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.failure(
            IllegalStateException("User tidak login")
        )
        val email = user.email ?: return Result.failure(
            IllegalStateException("Email tidak ditemukan")
        )
        return runCatching {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            deleteCloudData(user.uid)
            user.delete().await()
            clearSession()
        }.onFailure { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }

    private suspend fun deleteCloudData(uid: String) {
        val db = firestore ?: return
        val sunnahDocs = db.collection("users")
            .document(uid)
            .collection("sunnah_habits")
            .get()
            .await()
            .documents

        sunnahDocs.chunked(500).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }

        db.collection("users")
            .document(uid)
            .collection("meta")
            .document("profile")
            .delete()
            .await()
    }

    private fun clearSession() {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove("user_avatar_uri")
            .apply()
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
        if (e is FirebaseAuthException) {
            return when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Format email tidak valid"
                "ERROR_USER_NOT_FOUND",
                "ERROR_INVALID_CREDENTIAL" -> "Email atau password salah"
                "ERROR_WRONG_PASSWORD" -> "Password salah"
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Email sudah terdaftar"
                "ERROR_WEAK_PASSWORD" -> "Password minimal 6 karakter"
                "ERROR_TOO_MANY_REQUESTS" -> "Terlalu banyak percobaan, coba lagi nanti"
                "ERROR_NETWORK_REQUEST_FAILED" -> "Tidak ada koneksi internet"
                "ERROR_USER_DISABLED" -> "Akun ini telah dinonaktifkan"
                else -> "Terjadi kesalahan: ${e.localizedMessage}"
            }
        }
        val msg = e.message?.lowercase() ?: return "Terjadi kesalahan"
        return when {
            "network" in msg -> "Tidak ada koneksi internet"
            else -> "Terjadi kesalahan: ${e.localizedMessage}"
        }
    }
}
