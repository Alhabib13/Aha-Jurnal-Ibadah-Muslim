package com.islami.Aha.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class AppFeatureConfig(
    val puasaWajibRamadanEnabled: Boolean = true,
    val ramadanScheduleByLocationEnabled: Boolean = true,
    val sholatTarawihEnabled: Boolean = true,
    val fardhuScheduleByLocationEnabled: Boolean = true
)

data class AdminAccessDebug(
    val isAuthenticated: Boolean = false,
    val uid: String = "",
    val email: String = "",
    val projectId: String = "",
    val hasAdminClaim: Boolean = false,
    val hasRoleClaim: Boolean = false,
    val hasProfileRole: Boolean = false,
    val message: String = ""
) {
    val isAdmin: Boolean
        get() = hasAdminClaim || hasRoleClaim || hasProfileRole
}

@Singleton
class AdminConfigRepository @Inject constructor() {

    companion object {
        private const val COLLECTION_APP_CONFIG = "app_config"
        private const val DOC_FEATURE_FLAGS = "feature_flags"
        private const val FIELD_PUASA_WAJIB_RAMADAN_ENABLED = "puasaWajibRamadanEnabled"
        private const val FIELD_RAMADAN_SCHEDULE_BY_LOCATION_ENABLED = "ramadanScheduleByLocationEnabled"
        private const val FIELD_SHOLAT_TARAWIH_ENABLED = "sholatTarawihEnabled"
        private const val FIELD_FARDHU_SCHEDULE_BY_LOCATION_ENABLED = "fardhuScheduleByLocationEnabled"
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
                val puasaWajibEnabled =
                    snapshot?.getBoolean(FIELD_PUASA_WAJIB_RAMADAN_ENABLED) ?: true
                val ramadanScheduleEnabled =
                    snapshot?.getBoolean(FIELD_RAMADAN_SCHEDULE_BY_LOCATION_ENABLED) ?: true
                val sholatTarawihEnabled =
                    snapshot?.getBoolean(FIELD_SHOLAT_TARAWIH_ENABLED) ?: true
                val fardhuScheduleEnabled =
                    snapshot?.getBoolean(FIELD_FARDHU_SCHEDULE_BY_LOCATION_ENABLED) ?: true
                _featureConfig.value = AppFeatureConfig(
                    puasaWajibRamadanEnabled = puasaWajibEnabled,
                    ramadanScheduleByLocationEnabled = ramadanScheduleEnabled,
                    sholatTarawihEnabled = sholatTarawihEnabled,
                    fardhuScheduleByLocationEnabled = fardhuScheduleEnabled
                )
            }
    }

    suspend fun isCurrentUserAdmin(): Boolean {
        return getAdminAccessDebug().isAdmin
    }

    suspend fun getAdminAccessDebug(): AdminAccessDebug {
        val projectId = runCatching { firestore?.app?.options?.projectId.orEmpty() }
            .getOrDefault("")
        val user = auth?.currentUser ?: return AdminAccessDebug(
            isAuthenticated = false,
            projectId = projectId,
            message = "Belum login"
        )

        val uid = user.uid
        val email = user.email.orEmpty()
        val tokenResult = runCatching { user.getIdToken(true).await() }
        val token = tokenResult.getOrElse {
            FirebaseCrashlytics.getInstance().recordException(it)
            return AdminAccessDebug(
                isAuthenticated = true,
                uid = uid,
                email = email,
                projectId = projectId,
                message = "Gagal baca custom claim admin"
            )
        }
        val claims = token.claims
        val hasAdminClaim = (claims["admin"] as? Boolean) == true
        val hasRoleClaim = (claims["role"] as? String) == "admin"

        if (hasAdminClaim || hasRoleClaim) {
            return AdminAccessDebug(
                isAuthenticated = true,
                uid = uid,
                email = email,
                projectId = projectId,
                hasAdminClaim = hasAdminClaim,
                hasRoleClaim = hasRoleClaim,
                hasProfileRole = false,
                message = "Admin via custom claim"
            )
        }

        val db = firestore
        if (db == null) {
            return AdminAccessDebug(
                isAuthenticated = true,
                uid = uid,
                email = email,
                projectId = projectId,
                hasAdminClaim = hasAdminClaim,
                hasRoleClaim = hasRoleClaim,
                hasProfileRole = false,
                message = "Firestore tidak tersedia"
            )
        }

        val roleDoc = runCatching {
            db.collection("users")
                .document(uid)
                .collection("meta")
                .document("profile")
                .get(Source.SERVER)
                .await()
        }.getOrElse {
            FirebaseCrashlytics.getInstance().recordException(it)
            return AdminAccessDebug(
                isAuthenticated = true,
                uid = uid,
                email = email,
                projectId = projectId,
                hasAdminClaim = hasAdminClaim,
                hasRoleClaim = hasRoleClaim,
                hasProfileRole = false,
                message = "Gagal baca profile admin dari SERVER"
            )
        }

        val hasProfileRole = roleDoc.getString("role") == "admin"
        if (hasProfileRole) {
            return AdminAccessDebug(
                isAuthenticated = true,
                uid = uid,
                email = email,
                projectId = projectId,
                hasAdminClaim = hasAdminClaim,
                hasRoleClaim = hasRoleClaim,
                hasProfileRole = true,
                message = "Admin via profile.role (SERVER)"
            )
        }

        val roleDocCache = runCatching {
            db.collection("users")
                .document(uid)
                .collection("meta")
                .document("profile")
                .get(Source.CACHE)
                .await()
        }.getOrElse {
            FirebaseCrashlytics.getInstance().recordException(it)
            return AdminAccessDebug(
                isAuthenticated = true,
                uid = uid,
                email = email,
                projectId = projectId,
                hasAdminClaim = hasAdminClaim,
                hasRoleClaim = hasRoleClaim,
                hasProfileRole = false,
                message = "Akun belum terdeteksi admin"
            )
        }

        val hasProfileRoleCache = roleDocCache.getString("role") == "admin"
        return AdminAccessDebug(
            isAuthenticated = true,
            uid = uid,
            email = email,
            projectId = projectId,
            hasAdminClaim = hasAdminClaim,
            hasRoleClaim = hasRoleClaim,
            hasProfileRole = hasProfileRoleCache,
            message = if (hasProfileRoleCache) {
                "Admin via profile.role (CACHE only)"
            } else {
                "Akun belum terdeteksi admin"
            }
        )
    }

    suspend fun setPuasaWajibRamadanEnabled(enabled: Boolean): Result<Unit> {
        return updateFeatureFlag(FIELD_PUASA_WAJIB_RAMADAN_ENABLED, enabled)
    }

    suspend fun setRamadanScheduleByLocationEnabled(enabled: Boolean): Result<Unit> {
        return updateFeatureFlag(FIELD_RAMADAN_SCHEDULE_BY_LOCATION_ENABLED, enabled)
    }

    suspend fun setSholatTarawihEnabled(enabled: Boolean): Result<Unit> {
        return updateFeatureFlag(FIELD_SHOLAT_TARAWIH_ENABLED, enabled)
    }

    suspend fun setFardhuScheduleByLocationEnabled(enabled: Boolean): Result<Unit> {
        return updateFeatureFlag(FIELD_FARDHU_SCHEDULE_BY_LOCATION_ENABLED, enabled)
    }

    private suspend fun updateFeatureFlag(fieldName: String, enabled: Boolean): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore unavailable"))
        return runCatching {
            db.collection(COLLECTION_APP_CONFIG)
                .document(DOC_FEATURE_FLAGS)
                .set(
                    mapOf(
                        fieldName to enabled,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()
            Unit
        }.onFailure { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }
}
