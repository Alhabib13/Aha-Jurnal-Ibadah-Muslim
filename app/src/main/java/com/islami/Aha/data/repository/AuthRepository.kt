package com.islami.Aha.data.repository

import android.content.SharedPreferences
import android.net.Uri
import com.islami.Aha.BuildConfig
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.delay
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
        private const val KEY_USER_AVATAR_URI = "user_avatar_uri"
        private const val RECENT_LOGIN_MAX_AGE_MS = 10 * 60 * 1000L
    }

    private data class CloudSunnahBackup(
        val id: String,
        val data: Map<String, Any>
    )

    private data class CloudDeleteBackup(
        val sunnahHabits: List<CloudSunnahBackup>,
        val profileData: Map<String, Any>?
    )

    val currentUser: FirebaseUser? get() = firebaseAuth.currentUser

    val isLoggedIn: Boolean get() = firebaseAuth.currentUser != null
    private val firestore: FirebaseFirestore? by lazy {
        runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }
    private val storage: FirebaseStorage? by lazy {
        runCatching { FirebaseStorage.getInstance() }.getOrNull()
    }

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error("Login gagal")
            syncToPreferences(user)
            runCatching { syncProfileFromCloud(user.uid) }
                .onFailure { FirebaseCrashlytics.getInstance().recordException(it) }
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
            runCatching { upsertProfileBasics(user.uid, name, email) }
                .onFailure { FirebaseCrashlytics.getInstance().recordException(it) }
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    suspend fun loginWithGoogleIdToken(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: return AuthResult.Error("Login gagal")
            syncToPreferences(user)
            runCatching {
                upsertProfileBasics(
                    uid = user.uid,
                    name = user.displayName ?: user.email?.substringBefore("@") ?: "Pengguna",
                    email = user.email ?: ""
                )
                syncProfileFromCloud(user.uid)
            }.onFailure { FirebaseCrashlytics.getInstance().recordException(it) }
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    suspend fun updateAvatar(avatarUri: String): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.failure(
            IllegalStateException("User tidak login")
        )
        if (avatarUri.isBlank()) {
            return Result.failure(IllegalStateException("Foto profil tidak valid"))
        }

        return runCatching {
            val parsedUri = Uri.parse(avatarUri)
            val downloadUrl = uploadAvatarAndGetUrl(user.uid, parsedUri)

            firestore
                ?.collection("users")
                ?.document(user.uid)
                ?.collection("meta")
                ?.document("profile")
                ?.set(
                    mapOf(
                        "avatarUrl" to downloadUrl,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                ?.await()

            sharedPreferences.edit().putString(KEY_USER_AVATAR_URI, downloadUrl).apply()
            Unit
        }.recoverCatching { error ->
            throw IllegalStateException(mapAvatarStorageError(error))
        }.onFailure { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }

    suspend fun clearAvatar(): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.failure(
            IllegalStateException("User tidak login")
        )

        return runCatching {
            deleteAvatarFromKnownBuckets(user.uid)

            firestore
                ?.collection("users")
                ?.document(user.uid)
                ?.collection("meta")
                ?.document("profile")
                ?.set(
                    mapOf(
                        "avatarUrl" to FieldValue.delete(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                ?.await()

            sharedPreferences.edit().remove(KEY_USER_AVATAR_URI).apply()
            Unit
        }.recoverCatching { error ->
            throw IllegalStateException(mapAvatarStorageError(error))
        }.onFailure { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }

    private suspend fun fetchDownloadUrlWithRetry(
        reference: StorageReference,
        attempts: Int = 8
    ): String {
        var lastError: Exception? = null
        var delayMs = 400L
        repeat(attempts) { index ->
            try {
                // Force metadata read first; in some cases token generation lags shortly after upload.
                reference.metadata.await()
                return reference.downloadUrl.await().toString()
            } catch (error: Exception) {
                val storageError = error as? StorageException
                val isLastAttempt = index == attempts - 1
                if (storageError?.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND || isLastAttempt) {
                    throw error
                }
                lastError = error
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(3000L)
            }
        }
        throw lastError ?: IllegalStateException("Gagal mengambil URL foto profil")
    }

    private suspend fun uploadAvatarAndGetUrl(
        userId: String,
        sourceUri: Uri
    ): String {
        var lastError: Exception? = null
        val targetPath = "users/$userId/profile/avatar.jpg"

        for (bucket in resolveStorageBucketCandidates()) {
            val attempt = runCatching {
                val storageInstance = if (bucket.isNullOrBlank()) {
                    storage ?: throw IllegalStateException("Firebase Storage belum siap")
                } else {
                    FirebaseStorage.getInstance("gs://$bucket")
                }
                val storageRef = storageInstance.reference.child(targetPath)
                val uploadSnapshot = storageRef.putFile(sourceUri).await()
                fetchDownloadUrlWithRetry(uploadSnapshot.storage)
            }
            if (attempt.isSuccess) {
                return attempt.getOrThrow()
            }
            val error = attempt.exceptionOrNull() as? Exception
                ?: IllegalStateException("Upload foto profil gagal")
            lastError = error
            if (!shouldRetryWithAlternativeBucket(error)) {
                throw error
            }
        }
        throw lastError ?: IllegalStateException("Upload foto profil gagal")
    }

    private suspend fun deleteAvatarFromKnownBuckets(userId: String) {
        val attemptedBuckets = linkedSetOf<String?>()
        suspend fun deleteFrom(bucketOverride: String?) {
            if (!attemptedBuckets.add(bucketOverride)) return
            runCatching {
                val storageInstance = if (bucketOverride.isNullOrBlank()) {
                    storage
                } else {
                    FirebaseStorage.getInstance("gs://$bucketOverride")
                } ?: return@runCatching

                storageInstance.reference
                    .child("users/$userId/profile/avatar.jpg")
                    .delete()
                    .await()
            }.onFailure { error ->
                if (!isStorageNotFoundLike(error)) {
                    throw error
                }
            }
        }

        resolveStorageBucketCandidates().forEach { bucket ->
            deleteFrom(bucket)
        }
    }

    private fun resolveStorageBucketCandidates(): List<String?> {
        val buckets = linkedSetOf<String?>()
        buckets += null

        val configuredBucket = storage?.app?.options?.storageBucket
            ?.removePrefix("gs://")
            ?.trim()
            .orEmpty()
        if (configuredBucket.isNotBlank()) {
            buckets += configuredBucket
            resolveAlternateBucket(configuredBucket)?.let { buckets += it }
        }

        val projectId = BuildConfig.FIREBASE_PROJECT_ID.trim()
        if (projectId.isNotBlank()) {
            buckets += "$projectId.firebasestorage.app"
            buckets += "$projectId.appspot.com"
        }
        return buckets.toList()
    }

    private fun resolveAlternateBucket(bucket: String): String? {
        return when {
            bucket.endsWith(".firebasestorage.app") ->
                "${bucket.removeSuffix(".firebasestorage.app")}.appspot.com"
            bucket.endsWith(".appspot.com") ->
                "${bucket.removeSuffix(".appspot.com")}.firebasestorage.app"
            else -> null
        }
    }

    private fun shouldRetryWithAlternativeBucket(error: Throwable): Boolean {
        return isStorageNotFoundLike(error)
    }

    private fun isStorageNotFoundLike(error: Throwable): Boolean {
        val storageError = error as? StorageException ?: return false
        return storageError.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND ||
            storageError.httpResultCode == 404 ||
            (storageError.message?.contains("Not Found", ignoreCase = true) == true)
    }

    private fun mapAvatarStorageError(error: Throwable): String {
        val storageError = error as? StorageException
        return when (storageError?.errorCode) {
            StorageException.ERROR_NOT_AUTHENTICATED,
            StorageException.ERROR_NOT_AUTHORIZED -> "Akses upload foto ditolak. Coba login ulang"
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED,
            StorageException.ERROR_QUOTA_EXCEEDED -> "Upload foto gagal. Coba lagi nanti"
            else -> if (storageError != null && isStorageNotFoundLike(storageError)) {
                val knownBuckets = resolveStorageBucketCandidates()
                    .filterNotNull()
                    .distinct()
                    .joinToString(", ")
                "Bucket Firebase Storage tidak ditemukan/aktif. Buka Firebase Console > Storage > Get Started, lalu pastikan bucket cocok: $knownBuckets"
            } else {
                error.message ?: "Gagal menyimpan foto profil"
            }
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

    suspend fun reloadAndGetEmailVerificationStatus(): Result<Boolean> {
        val user = firebaseAuth.currentUser ?: return Result.failure(
            IllegalStateException("User tidak login")
        )
        return runCatching {
            user.reload().await()
            user.isEmailVerified
        }.recoverCatching { error ->
            throw IllegalStateException(mapFirebaseError(error as? Exception ?: Exception(error)))
        }.onFailure { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }

    suspend fun sendEmailVerificationToCurrentUser(): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.failure(
            IllegalStateException("User tidak login")
        )
        return runCatching {
            user.sendEmailVerification().await()
            Unit
        }.recoverCatching { error ->
            throw IllegalStateException(mapFirebaseError(error as? Exception ?: Exception(error)))
        }.onFailure { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        sendSecurityEmail: Boolean = true
    ): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.failure(
            IllegalStateException("User tidak login")
        )
        val email = user.email ?: return Result.failure(
            IllegalStateException("Email tidak ditemukan")
        )

        return runCatching {
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()

            if (sendSecurityEmail) {
                runCatching {
                    firebaseAuth.sendPasswordResetEmail(email).await()
                }.onFailure { error ->
                    FirebaseCrashlytics.getInstance().recordException(error)
                    throw IllegalStateException("Password berhasil diubah, tapi email konfirmasi gagal dikirim")
                }
            }
            Unit
        }.recoverCatching { error ->
            if (error is IllegalStateException && !error.message.isNullOrBlank()) {
                throw error
            }
            throw IllegalStateException(mapFirebaseError(error as? Exception ?: Exception(error)))
        }.onFailure { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.failure(
            IllegalStateException("User tidak login")
        )
        return runCatching {
            ensureRecentLoginForDelete(user)
            deleteAccountSafely(user)
        }.recoverCatching { error ->
            if (error is FirebaseAuthRecentLoginRequiredException || error is ReAuthRequiredException) {
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
            deleteAccountSafely(user)
        }.onFailure { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }

    private suspend fun ensureRecentLoginForDelete(user: FirebaseUser) {
        user.reload().await()
        val lastSignInAt = user.metadata?.lastSignInTimestamp ?: 0L
        if (lastSignInAt <= 0L) return
        if (System.currentTimeMillis() - lastSignInAt > RECENT_LOGIN_MAX_AGE_MS) {
            throw ReAuthRequiredException()
        }
    }

    private suspend fun deleteAccountSafely(user: FirebaseUser) {
        val backup = deleteCloudDataWithBackup(user.uid)
        runCatching {
            user.delete().await()
        }.onFailure { error ->
            runCatching { restoreCloudData(user.uid, backup) }
                .onFailure { restoreError ->
                    FirebaseCrashlytics.getInstance().recordException(restoreError)
                }
            throw error
        }
        clearSession()
    }

    private suspend fun deleteCloudDataWithBackup(uid: String): CloudDeleteBackup {
        val db = firestore ?: return CloudDeleteBackup(
            sunnahHabits = emptyList(),
            profileData = null
        )

        val sunnahSnapshot = db.collection("users")
            .document(uid)
            .collection("sunnah_habits")
            .get()
            .await()
        val sunnahDocs = sunnahSnapshot.documents
        val sunnahBackup = sunnahDocs.map { doc ->
            CloudSunnahBackup(
                id = doc.id,
                data = doc.data.orEmpty().mapNotNull { (key, value) ->
                    if (value == null) null else key to value
                }.toMap()
            )
        }

        val profileSnapshot = db.collection("users")
            .document(uid)
            .collection("meta")
            .document("profile")
            .get()
            .await()
        val profileBackup = profileSnapshot.data?.mapNotNull { (key, value) ->
            if (value == null) null else key to value
        }?.toMap()

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

        return CloudDeleteBackup(
            sunnahHabits = sunnahBackup,
            profileData = profileBackup
        )
    }

    private suspend fun restoreCloudData(uid: String, backup: CloudDeleteBackup) {
        val db = firestore ?: return

        backup.sunnahHabits.chunked(500).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { item ->
                batch.set(
                    db.collection("users")
                        .document(uid)
                        .collection("sunnah_habits")
                        .document(item.id),
                    item.data
                )
            }
            batch.commit().await()
        }

        val profileData = backup.profileData
        if (!profileData.isNullOrEmpty()) {
            db.collection("users")
                .document(uid)
                .collection("meta")
                .document("profile")
                .set(profileData, SetOptions.merge())
                .await()
        }
    }

    private fun clearSession() {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_AVATAR_URI)
            .apply()
    }

    fun logout() {
        firebaseAuth.signOut()
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_AVATAR_URI)
            .apply()
    }

    private fun syncToPreferences(user: FirebaseUser, displayName: String? = null) {
        val name = displayName ?: user.displayName ?: user.email?.substringBefore("@") ?: "Pengguna"
        val editor = sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, user.email ?: "")
        val photoUrl = user.photoUrl?.toString()
        if (!photoUrl.isNullOrBlank()) {
            editor.putString(KEY_USER_AVATAR_URI, photoUrl)
        }
        editor.apply()
    }

    private suspend fun upsertProfileBasics(uid: String, name: String, email: String) {
        firestore
            ?.collection("users")
            ?.document(uid)
            ?.collection("meta")
            ?.document("profile")
            ?.set(
                mapOf(
                    "name" to name,
                    "email" to email,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            ?.await()
    }

    private suspend fun syncProfileFromCloud(uid: String) {
        val snapshot = firestore
            ?.collection("users")
            ?.document(uid)
            ?.collection("meta")
            ?.document("profile")
            ?.get()
            ?.await()
            ?: return

        if (!snapshot.exists()) return

        val cloudName = snapshot.getString("name")
        val cloudEmail = snapshot.getString("email")
        val cloudAvatar = snapshot.getString("avatarUrl")
        val editor = sharedPreferences.edit()
        if (!cloudName.isNullOrBlank()) editor.putString(KEY_USER_NAME, cloudName)
        if (!cloudEmail.isNullOrBlank()) editor.putString(KEY_USER_EMAIL, cloudEmail)
        if (!cloudAvatar.isNullOrBlank()) {
            editor.putString(KEY_USER_AVATAR_URI, cloudAvatar)
        } else {
            editor.remove(KEY_USER_AVATAR_URI)
        }
        editor.apply()
    }

    private fun mapFirebaseError(e: Exception): String {
        val rawMessage = e.localizedMessage ?: e.message.orEmpty()
        val lowerMessage = rawMessage.lowercase()
        if (
            "app check token is invalid" in lowerMessage ||
            "app attestation failed" in lowerMessage ||
            "appcheck" in lowerMessage && "invalid" in lowerMessage
        ) {
            return appCheckFailureMessage()
        }
        if (
            "supplied auth credential is incorrect" in lowerMessage ||
            "there is no user record corresponding" in lowerMessage ||
            "password is invalid" in lowerMessage
        ) {
            return "Email atau password salah"
        }

        if (e is FirebaseAuthException) {
            return when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Format email tidak valid"
                "ERROR_USER_NOT_FOUND",
                "ERROR_INVALID_CREDENTIAL",
                "ERROR_INVALID_LOGIN_CREDENTIAL",
                "ERROR_WRONG_PASSWORD" -> "Email atau password salah"
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Email sudah terdaftar"
                "ERROR_WEAK_PASSWORD" -> "Password minimal 6 karakter"
                "ERROR_TOO_MANY_REQUESTS" -> "Terlalu banyak percobaan, coba lagi nanti"
                "ERROR_NETWORK_REQUEST_FAILED" -> "Tidak ada koneksi internet"
                "ERROR_USER_DISABLED" -> "Akun ini telah dinonaktifkan"
                "ERROR_OPERATION_NOT_ALLOWED" -> "Metode login ini belum diaktifkan"
                "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "Akun sudah terdaftar dengan metode login lain"
                "ERROR_WEB_CONTEXT_CANCELED",
                "ERROR_CANCELED" -> "Login dibatalkan"
                "ERROR_INTERNAL_ERROR" ->
                    if ("app check" in lowerMessage || "attestation" in lowerMessage) {
                        appCheckFailureMessage()
                    } else {
                        "Terjadi gangguan internal Firebase. Coba lagi sebentar."
                    }
                else -> "Terjadi kesalahan: ${e.localizedMessage}"
            }
        }
        val msg = e.message?.lowercase() ?: return "Terjadi kesalahan"
        return when {
            "network" in msg -> "Tidak ada koneksi internet"
            "developer_error" in msg || "unknown calling package name" in msg ->
                "Konfigurasi Google Sign-In tidak valid. Cek SHA-1/SHA-256 dan OAuth client di Firebase."
            else -> "Terjadi kesalahan: ${e.localizedMessage}"
        }
    }

    private fun appCheckFailureMessage(): String {
        return when (BuildConfig.APP_CHECK_MODE.uppercase()) {
            "DEBUG" -> {
                "Validasi keamanan Firebase gagal (App Check DEBUG). Khusus build debug: ambil token dari Logcat tag DebugAppCheckProvider lalu daftarkan di Firebase Console > App Check > Manage debug tokens."
            }
            "PLAY_INTEGRITY" -> {
                "Validasi keamanan Firebase gagal (App Check Play Integrity). Pastikan SHA-256 signing key sudah didaftarkan, Play Integrity API aktif, dan aplikasi dijalankan di device fisik dengan Google Play Services."
            }
            "AUTO" -> {
                if (BuildConfig.DEBUG) {
                    "Validasi keamanan Firebase gagal (App Check AUTO). Di emulator gunakan debug token; di device fisik pastikan Play Integrity + SHA-256 sudah benar."
                } else {
                    "Validasi keamanan Firebase gagal (App Check). Cek konfigurasi App Check di Firebase Console."
                }
            }
            else -> "Validasi keamanan Firebase gagal (App Check). Cek konfigurasi App Check di Firebase Console."
        }
    }
}
