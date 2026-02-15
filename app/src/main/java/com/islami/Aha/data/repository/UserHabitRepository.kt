package com.islami.Aha.data.repository

import com.islami.Aha.data.local.UserHabitDao
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
    private val userHabitDao: UserHabitDao
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
        val entity = habit.toEntity()
        userHabitDao.insertHabit(entity)
        syncUpsertToCloud(entity)
    }

    suspend fun updateHabit(habit: SunnahHabit) {
        val entity = habit.toEntity()
        userHabitDao.updateHabit(entity)
        syncUpsertToCloud(entity)
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
                    UserHabitEntity(
                        id = doc.id,
                        name = doc.getString("name").orEmpty(),
                        category = doc.getString("category").orEmpty(),
                        frequencyLabel = doc.getString("frequencyLabel").orEmpty(),
                        rakaat = doc.getLong("rakaat")?.toInt(),
                        reminderEnabled = doc.getBoolean("reminderEnabled") ?: false,
                        reminderTime = doc.getString("reminderTime"),
                        completedDateKey = doc.getString("completedDateKey"),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }.getOrNull()
            }.filter { it.id.isNotBlank() && it.name.isNotBlank() && it.category.isNotBlank() }

            if (entities.isNotEmpty()) {
                userHabitDao.insertAllHabits(entities)
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
                        "createdAt" to entity.createdAt
                    )
                )
                .await()
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

private fun SunnahHabit.toEntity(): UserHabitEntity {
    return UserHabitEntity(
        id = id,
        name = name,
        category = category.name,
        frequencyLabel = frequencyLabel,
        rakaat = rakaat,
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime,
        completedDateKey = if (isCompletedToday) DateUtils.getTodayKey() else null
    )
}
