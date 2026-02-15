package com.islami.Aha

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.repository.UserHabitRepository
import com.islami.Aha.ui.theme.ThemeManager
import com.islami.Aha.util.NotificationScheduler
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class AhaApplication : Application() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppEntryPoint {
        fun userHabitRepository(): UserHabitRepository
        fun habitDao(): HabitDao
    }

    override fun onCreate() {
        super.onCreate()
        val isDebugBuild = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!isDebugBuild)
        FirebaseCrashlytics.getInstance().log("AhaApplication started")
        val prefs = getSharedPreferences("aha_prefs", Context.MODE_PRIVATE)
        ThemeManager.init(prefs)
        NotificationScheduler.createNotificationChannel(this)
        restoreAlarms()
    }

    private fun restoreAlarms() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    this@AhaApplication,
                    AppEntryPoint::class.java
                )
                val repository = entryPoint.userHabitRepository()
                val habitDao = entryPoint.habitDao()
                val activeHabits = repository.getActiveReminderHabits()
                val activeDefaultHabits = habitDao.getActiveReminderHabits()

                activeHabits.forEach { habit ->
                    val parts = habit.reminderTime?.split(":") ?: return@forEach
                    if (parts.size == 2) {
                        val hour = parts[0].toIntOrNull() ?: return@forEach
                        val minute = parts[1].toIntOrNull() ?: return@forEach
                        NotificationScheduler.scheduleHabitReminder(
                            context = this@AhaApplication,
                            habitId = habit.id,
                            habitName = habit.name,
                            hour = hour,
                            minute = minute
                        )
                    }
                }

                activeDefaultHabits.forEach { habit ->
                    val parts = habit.time.split(":")
                    if (parts.size == 2) {
                        val hour = parts[0].toIntOrNull() ?: return@forEach
                        val minute = parts[1].toIntOrNull() ?: return@forEach
                        NotificationScheduler.scheduleHabitReminder(
                            context = this@AhaApplication,
                            habitId = "default_${habit.id}",
                            habitName = habit.name,
                            hour = hour,
                            minute = minute
                        )
                    }
                }
                Log.d("AhaApplication", "Restored ${activeHabits.size + activeDefaultHabits.size} alarms")
            } catch (e: Exception) {
                Log.e("AhaApplication", "Failed to restore alarms", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }
}
