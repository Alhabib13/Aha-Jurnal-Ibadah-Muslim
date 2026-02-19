package com.islami.Aha.ui.settings

import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.local.UserHabitDao
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.model.HabitCompletionRecord
import com.islami.Aha.data.model.UserHabitEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal suspend fun buildSettingsExportJson(
    habitDao: HabitDao,
    userHabitDao: UserHabitDao,
    habitCompletionDao: HabitCompletionDao,
    isLoggedIn: Boolean,
    userEmail: String
): String {
    val defaultHabits = habitDao.getHabitsSnapshot()
    val sunnahHabits = userHabitDao.getAllHabitsSnapshot()
    val completionRecords = habitCompletionDao.getAllRecords()

    val root = JSONObject()
        .put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT).format(Date()))
        .put("appVersion", "1.0.0")
        .put("isLoggedIn", isLoggedIn)
        .put("userEmail", userEmail)

    val defaultHabitsArray = JSONArray()
    defaultHabits.forEach { habit ->
        defaultHabitsArray.put(
            JSONObject()
                .put("id", habit.id)
                .put("name", habit.name)
                .put("category", habit.category)
                .put("icon", habit.icon)
                .put("description", habit.description)
                .put("isCompleted", habit.isCompleted)
                .put("streak", habit.streak)
                .put("time", habit.time)
                .put("isReminderEnabled", habit.isReminderEnabled)
                .put("isCustom", habit.isCustom)
        )
    }

    val sunnahHabitsArray = JSONArray()
    sunnahHabits.forEach { habit ->
        val habitJson = JSONObject()
            .put("id", habit.id)
            .put("name", habit.name)
            .put("category", habit.category)
            .put("frequencyLabel", habit.frequencyLabel)
            .put("reminderEnabled", habit.reminderEnabled)
            .put("createdAt", habit.createdAt)
        if (habit.rakaat != null) habitJson.put("rakaat", habit.rakaat) else habitJson.put("rakaat", JSONObject.NULL)
        if (habit.reminderTime != null) habitJson.put("reminderTime", habit.reminderTime) else habitJson.put("reminderTime", JSONObject.NULL)
        if (habit.completedDateKey != null) habitJson.put("completedDateKey", habit.completedDateKey) else habitJson.put("completedDateKey", JSONObject.NULL)
        sunnahHabitsArray.put(habitJson)
    }

    val completionArray = JSONArray()
    completionRecords.forEach { record ->
        completionArray.put(
            JSONObject()
                .put("id", record.id)
                .put("habitKey", record.habitKey)
                .put("dateKey", record.dateKey)
                .put("category", record.category)
                .put("source", record.source)
        )
    }

    root.put("defaultHabits", defaultHabitsArray)
    root.put("sunnahHabits", sunnahHabitsArray)
    root.put("completionRecords", completionArray)
    return root.toString(2)
}

internal fun parseSettingsImportJson(rawJson: String): ParsedImport {
    val root = JSONObject(rawJson)
    val defaultHabitsArray = root.optJSONArray("defaultHabits") ?: JSONArray()
    val sunnahHabitsArray = root.optJSONArray("sunnahHabits") ?: JSONArray()
    val completionArray = root.optJSONArray("completionRecords") ?: JSONArray()

    val defaultHabits = mutableListOf<Habit>()
    for (i in 0 until defaultHabitsArray.length()) {
        val obj = defaultHabitsArray.optJSONObject(i) ?: continue
        val name = obj.optString("name").trim()
        val category = obj.optString("category").trim()
        if (name.isBlank() || category.isBlank()) continue
        defaultHabits += Habit(
            id = obj.optInt("id", 0).coerceAtLeast(0),
            name = name,
            category = category,
            icon = obj.optString("icon").ifBlank { "sun" },
            description = obj.optString("description"),
            isCompleted = obj.optBoolean("isCompleted", false),
            streak = obj.optInt("streak", 0).coerceAtLeast(0),
            time = obj.optString("time"),
            isReminderEnabled = obj.optBoolean("isReminderEnabled", false),
            isCustom = obj.optBoolean("isCustom", false)
        )
    }

    val sunnahHabits = mutableListOf<UserHabitEntity>()
    for (i in 0 until sunnahHabitsArray.length()) {
        val obj = sunnahHabitsArray.optJSONObject(i) ?: continue
        val id = obj.optString("id").trim()
        val name = obj.optString("name").trim()
        if (id.isBlank() || name.isBlank()) continue
        sunnahHabits += UserHabitEntity(
            id = id,
            name = name,
            category = obj.optString("category").ifBlank { "SHOLAT" },
            frequencyLabel = obj.optString("frequencyLabel"),
            rakaat = if (obj.isNull("rakaat")) null else obj.optInt("rakaat"),
            reminderEnabled = obj.optBoolean("reminderEnabled", false),
            reminderTime = if (obj.isNull("reminderTime")) null else obj.optString("reminderTime"),
            completedDateKey = if (obj.isNull("completedDateKey")) null else obj.optString("completedDateKey"),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }

    val completionRecords = mutableListOf<HabitCompletionRecord>()
    for (i in 0 until completionArray.length()) {
        val obj = completionArray.optJSONObject(i) ?: continue
        val habitKey = obj.optString("habitKey").trim()
        val dateKey = obj.optString("dateKey").trim()
        val category = obj.optString("category").trim()
        if (habitKey.isBlank() || dateKey.isBlank() || category.isBlank()) continue
        completionRecords += HabitCompletionRecord(
            id = 0L,
            habitKey = habitKey,
            dateKey = dateKey,
            category = category,
            source = obj.optString("source").ifBlank { "DEFAULT" }
        )
    }

    if (defaultHabits.isEmpty() && sunnahHabits.isEmpty() && completionRecords.isEmpty()) {
        throw IllegalStateException("File backup tidak valid atau kosong")
    }

    return ParsedImport(
        defaultHabits = defaultHabits,
        sunnahHabits = sunnahHabits,
        completionRecords = completionRecords
    )
}

internal fun parseHourMinute(raw: String?): Pair<Int, Int>? {
    if (raw.isNullOrBlank()) return null
    val parts = raw.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour to minute
}
