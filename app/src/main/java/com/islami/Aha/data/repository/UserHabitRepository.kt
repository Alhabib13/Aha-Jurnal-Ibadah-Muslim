package com.islami.Aha.data.repository

import androidx.room.withTransaction
import com.islami.Aha.data.local.AppDatabase
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.UserHabitDao
import com.islami.Aha.data.model.HabitCompletionRecord
import com.islami.Aha.data.model.UserHabitEntity
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import com.islami.Aha.util.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserHabitRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val userHabitDao: UserHabitDao,
    private val habitCompletionDao: HabitCompletionDao
) {
    companion object {
        private const val MIN_SYNC_INTERVAL_MS = 30_000L
    }

    private val firebaseAuth: FirebaseAuth? by lazy {
        runCatching { FirebaseAuth.getInstance() }.getOrNull()
    }
    private val firestore: FirebaseFirestore? by lazy {
        runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }
    private val syncMutex = Mutex()
    private var lastSyncAtMs: Long = 0L

    fun getAllHabits(): Flow<List<SunnahHabit>> {
        return userHabitDao.getAllHabits().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getHabitCount(): Flow<Int> = userHabitDao.getHabitCount()

    fun getHabitCountByCategory(category: SunnahCategoryType): Flow<Int> {
        return userHabitDao.getHabitCountByCategory(category.name)
    }

    suspend fun insertHabit(habit: SunnahHabit) {
        val now = System.currentTimeMillis()
        val existing = userHabitDao.getHabitById(habit.id)
        val entity = habit.toEntity(existing = existing, nowMs = now)
        userHabitDao.insertHabit(entity)
        syncUpsertToCloud(entity)
    }

    suspend fun updateHabit(habit: SunnahHabit) {
        val now = System.currentTimeMillis()
        val existing = userHabitDao.getHabitById(habit.id)
        val entity = habit.toEntity(existing = existing, nowMs = now)
        userHabitDao.updateHabit(entity)
        syncUpsertToCloud(entity)
    }

    suspend fun toggleHabitCompletion(id: String): SunnahHabit? {
        val todayKey = DateUtils.getTodayKey()
        val habitKey = "sunnah_$id"
        var updatedEntity: UserHabitEntity? = null
        appDatabase.withTransaction {
            val current = userHabitDao.getHabitById(id) ?: return@withTransaction
            val wasCompletedToday = DateUtils.isToday(current.completedDateKey)
            updatedEntity = current.copy(
                completedDateKey = if (wasCompletedToday) null else todayKey,
                updatedAt = System.currentTimeMillis()
            )
            userHabitDao.updateHabit(updatedEntity!!)
            if (wasCompletedToday) {
                habitCompletionDao.deleteByHabitAndDate(habitKey, todayKey)
            } else {
                habitCompletionDao.insert(
                    HabitCompletionRecord(
                        habitKey = habitKey,
                        dateKey = todayKey,
                        category = toSunnahCompletionCategory(current.category),
                        source = "SUNNAH"
                    )
                )
            }
        }
        val entity = updatedEntity ?: return null
        syncUpsertToCloud(entity)
        return entity.toDomain()
    }

    suspend fun deleteHabit(id: String) {
        userHabitDao.deleteHabit(id)
        syncDeleteFromCloud(id)
    }

    suspend fun getHabitById(id: String): SunnahHabit? {
        return userHabitDao.getHabitById(id)?.toDomain()
    }

    suspend fun getActiveReminderHabits(): List<SunnahHabit> {
        return userHabitDao.getActiveReminderHabits().map { it.toDomain() }
    }

    suspend fun syncFromCloudIfLoggedIn() {
        syncMutex.withLock {
            val now = System.currentTimeMillis()
            if (now - lastSyncAtMs < MIN_SYNC_INTERVAL_MS) return

            val uid = firebaseAuth?.currentUser?.uid ?: return
            val cloud = firestore ?: return
            lastSyncAtMs = now

            val snapshot = runCatching {
                cloud.collection("users")
                    .document(uid)
                    .collection("sunnah_habits")
                    .get()
                    .await()
            }.getOrElse { error ->
                lastSyncAtMs = 0L
                FirebaseCrashlytics.getInstance().recordException(error)
                return
            }

            val entities = snapshot.documents.mapNotNull { doc ->
                runCatching {
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = doc.getLong("updatedAt") ?: createdAt
                    UserHabitEntity(
                        id = doc.id,
                        name = doc.getString("name").orEmpty(),
                        category = doc.getString("category").orEmpty(),
                        frequencyLabel = doc.getString("frequencyLabel").orEmpty(),
                        rakaat = doc.getLong("rakaat")?.toInt(),
                        reminderEnabled = doc.getBoolean("reminderEnabled") ?: false,
                        reminderTime = doc.getString("reminderTime"),
                        completedDateKey = doc.getString("completedDateKey"),
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        lastSyncedAt = updatedAt
                    )
                }.getOrNull()
            }.filter { it.id.isNotBlank() && it.name.isNotBlank() && it.category.isNotBlank() }

            val localSnapshot = userHabitDao.getAllHabitsSnapshot()
            val localById = localSnapshot.associateBy { it.id }
            val remoteById = entities.associateBy { it.id }

            val remoteToApplyLocally = entities.mapNotNull { remote ->
                val local = localById[remote.id]
                if (local == null || remote.updatedAt >= local.updatedAt) {
                    remote.copy(lastSyncedAt = maxOf(local?.lastSyncedAt ?: 0L, remote.updatedAt))
                } else {
                    null
                }
            }

            if (remoteToApplyLocally.isNotEmpty()) {
                userHabitDao.insertAllHabits(remoteToApplyLocally)
            }

            if (!snapshot.metadata.isFromCache) {
                val idsToDelete = mutableListOf<String>()
                val pendingUpload = linkedMapOf<String, UserHabitEntity>()

                localSnapshot.forEach { local ->
                    val remote = remoteById[local.id]
                    when {
                        remote == null -> {
                            // Delete local only when the record is already fully synced.
                            if (local.lastSyncedAt > 0L && local.updatedAt <= local.lastSyncedAt) {
                                idsToDelete += local.id
                            } else {
                                pendingUpload[local.id] = local
                            }
                        }
                        local.updatedAt > remote.updatedAt -> {
                            pendingUpload[local.id] = local
                        }
                    }
                }

                if (idsToDelete.isNotEmpty()) {
                    userHabitDao.deleteHabitsByIds(idsToDelete)
                }

                pendingUpload.values.forEach { local ->
                    syncUpsertToCloud(local)
                }
            }
        }
    }

    private suspend fun syncUpsertToCloud(entity: UserHabitEntity) {
        val uid = firebaseAuth?.currentUser?.uid ?: return
        val cloud = firestore ?: return
        runCatching {
            cloud.collection("users")
                .document(uid)
                .collection("sunnah_habits")
                .document(entity.id)
                .set(
                    mapOf(
                        "name" to entity.name,
                        "category" to entity.category,
                        "frequencyLabel" to entity.frequencyLabel,
                        "rakaat" to entity.rakaat,
                        "reminderEnabled" to entity.reminderEnabled,
                        "reminderTime" to entity.reminderTime,
                        "completedDateKey" to entity.completedDateKey,
                        "createdAt" to entity.createdAt,
                        "updatedAt" to entity.updatedAt
                    )
                )
                .await()
            userHabitDao.markHabitSynced(entity.id, entity.updatedAt)
        }.onFailure { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }

    private suspend fun syncDeleteFromCloud(id: String) {
        val uid = firebaseAuth?.currentUser?.uid ?: return
        val cloud = firestore ?: return
        runCatching {
            cloud.collection("users")
                .document(uid)
                .collection("sunnah_habits")
                .document(id)
                .delete()
                .await()
        }.onFailure { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }

    private fun toSunnahCompletionCategory(rawCategory: String): String {
        return when (rawCategory.uppercase()) {
            SunnahCategoryType.SHOLAT.name -> "Sholat Sunnah"
            SunnahCategoryType.PUASA.name -> "Puasa Sunnah"
            else -> "Sunnah"
        }
    }
}

private fun UserHabitEntity.toDomain(): SunnahHabit {
    return SunnahHabit(
        id = id,
        name = name,
        category = try {
            SunnahCategoryType.valueOf(category)
        } catch (_: IllegalArgumentException) {
            SunnahCategoryType.SHOLAT
        },
        frequencyLabel = frequencyLabel,
        rakaat = rakaat,
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime,
        isCompletedToday = DateUtils.isToday(completedDateKey)
    )
}

private fun SunnahHabit.toEntity(
    existing: UserHabitEntity? = null,
    nowMs: Long = System.currentTimeMillis()
): UserHabitEntity {
    return UserHabitEntity(
        id = id,
        name = name,
        category = category.name,
        frequencyLabel = frequencyLabel,
        rakaat = rakaat,
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime,
        completedDateKey = if (isCompletedToday) DateUtils.getTodayKey() else null,
        createdAt = existing?.createdAt ?: nowMs,
        updatedAt = nowMs,
        lastSyncedAt = existing?.lastSyncedAt ?: 0L
    )
}
