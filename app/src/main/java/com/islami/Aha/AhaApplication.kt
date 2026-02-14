package com.islami.Aha

import android.app.Application
import android.util.Log
import com.islami.Aha.data.repository.UserHabitRepository
import com.islami.Aha.util.NotificationScheduler
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
    }

    override fun onCreate() {
        super.onCreate()
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
                val activeHabits = repository.getActiveReminderHabits()

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
                Log.d("AhaApplication", "Restored ${activeHabits.size} alarms")
            } catch (e: Exception) {
                Log.e("AhaApplication", "Failed to restore alarms", e)
            }
        }
    }
}
