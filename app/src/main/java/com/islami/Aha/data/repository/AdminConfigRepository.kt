package com.islami.Aha.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class AppFeatureConfig(
    val puasaWajibRamadanEnabled: Boolean = true
)

@Singleton
class AdminConfigRepository @Inject constructor() {

    companion object {
        private const val COLLECTION_APP_CONFIG = "app_config"
        private const val DOC_FEATURE_FLAGS = "feature_flags"
        private const val FIELD_PUASA_WAJIB_RAMADAN_ENABLED = "puasaWajibRamadanEnabled"
    }

    private val auth: FirebaseAuth? by lazy {
        runCatching { FirebaseAuth.getInstance() }.getOrNull()
    }
    private val firestore: FirebaseFirestore? by lazy {
        runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }

    private val _featureConfig = MutableStateFlow(AppFeatureConfig())
    val featureConfig: StateFlow<AppFeatureConfig> = _featureConfig.asStateFlow()

    private var configListener: ListenerRegistration? = null

    init {
        startFeatureConfigListener()
    }

    private fun startFeatureConfigListener() {
        val db = firestore ?: return
        configListener = db.collection(COLLECTION_APP_CONFIG)
            .document(DOC_FEATURE_FLAGS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirebaseCrashlytics.getInstance().recordException(error)
                    return@addSnapshotListener
                }
                val enabled = snapshot?.getBoolean(FIELD_PUASA_WAJIB_RAMADAN_ENABLED) ?: true
                _featureConfig.value = AppFeatureConfig(
                    puasaWajibRamadanEnabled = enabled
                )
            }
    }

    suspend fun isCurrentUserAdmin(): Boolean {
        val user = auth?.currentUser ?: return false

        val claimAdmin = runCatching {
            val token = user.getIdToken(true).await()
            val claims = token.claims
            (claims["admin"] as? Boolean) == true || (claims["role"] as? String) == "admin"
        }.getOrElse {
            FirebaseCrashlytics.getInstance().recordException(it)
            false
        }
        if (claimAdmin) return true

        val db = firestore ?: return false
        val roleDoc = runCatching {
            db.collection("users")
                .document(user.uid)
                .collection("meta")
                .document("profile")
                .get()
                .await()
        }.getOrElse {
            FirebaseCrashlytics.getInstance().recordException(it)
            return false
        }

        return roleDoc.getString("role") == "admin"
    }

    suspend fun setPuasaWajibRamadanEnabled(enabled: Boolean): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore unavailable"))
        return runCatching {
            db.collection(COLLECTION_APP_CONFIG)
                .document(DOC_FEATURE_FLAGS)
                .set(
                    mapOf(
                        FIELD_PUASA_WAJIB_RAMADAN_ENABLED to enabled,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            Unit
        }.onFailure { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }
}
