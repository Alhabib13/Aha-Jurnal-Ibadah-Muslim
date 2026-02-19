package com.islami.Aha.ui.home

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.islami.Aha.data.model.Habit
import kotlinx.coroutines.tasks.await

private val validTimeRegex = Regex("^\\d{2}:\\d{2}$")
private val canonicalFardhuOrder = listOf(
    "Sholat Subuh",
    "Sholat Dzuhur",
    "Sholat Ashar",
    "Sholat Maghrib",
    "Sholat Isya"
)

internal suspend fun fetchCompleteFardhuHabitsFromFirestore(
    firestore: FirebaseFirestore,
    onError: (Throwable) -> Unit
): List<Habit> {
    val snapshot = runCatching {
        firestore.collection("fardhu_defaults")
            .orderBy("order")
            .get()
            .await()
    }.getOrElse {
        onError(it)
        return emptyList()
    }
    return mapFardhuSnapshotToHabits(snapshot)
}

private fun mapFardhuSnapshotToHabits(snapshot: QuerySnapshot): List<Habit> {
    val canonicalSet = canonicalFardhuOrder.toSet()
    val byName = snapshot.documents.mapNotNull { doc ->
        val name = doc.getString("name").orEmpty()
        val time = doc.getString("defaultTime").orEmpty()
        val icon = doc.getString("icon").orEmpty().ifBlank { "sun" }
        if (name !in canonicalSet || !time.matches(validTimeRegex)) return@mapNotNull null
        name to Habit(
            name = name,
            category = "Sholat Fardhu",
            icon = icon,
            description = "",
            time = time
        )
    }.toMap()

    return canonicalFardhuOrder.mapNotNull { byName[it] }
        .takeIf { it.size == canonicalFardhuOrder.size }
        ?: emptyList()
}

internal fun fallbackFardhuHabits(): List<Habit> = listOf(
    Habit(name = "Sholat Subuh", category = "Sholat Fardhu", icon = "sunrise", description = "", time = "04:30"),
    Habit(name = "Sholat Dzuhur", category = "Sholat Fardhu", icon = "sun", description = "", time = "11:55"),
    Habit(name = "Sholat Ashar", category = "Sholat Fardhu", icon = "cloud", description = "", time = "15:10"),
    Habit(name = "Sholat Maghrib", category = "Sholat Fardhu", icon = "moon", description = "", time = "18:00"),
    Habit(name = "Sholat Isya", category = "Sholat Fardhu", icon = "moon", description = "", time = "19:15")
)

