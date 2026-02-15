package com.islami.Aha.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.repository.UserHabitRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootEntryPoint {
        fun userHabitRepository(): UserHabitRepository
        fun habitDao(): HabitDao
    }

    companion object {
        private const val ACTION_QUICKBOOT = "android.intent.action.QUICKBOOT_POWERON"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != ACTION_QUICKBOOT) return

        Log.d("BootCompletedReceiver", "Device reboot detected, restoring alarms")
        NotificationScheduler.createNotificationChannel(context)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        BootEntryPoint::class.java
                    )
                    val userRepository = entryPoint.userHabitRepository()
                    val habitDao = entryPoint.habitDao()

                    userRepository.getActiveReminderHabits().forEach { habit ->
                        val parts = habit.reminderTime?.split(":") ?: return@forEach
                        if (parts.size == 2) {
                            val hour = parts[0].toIntOrNull() ?: return@forEach
                            val minute = parts[1].toIntOrNull() ?: return@forEach
                            NotificationScheduler.scheduleHabitReminder(
                                context = context,
                                habitId = habit.id,
                                habitName = habit.name,
                                hour = hour,
                                minute = minute
                            )
                        }
                    }

                    habitDao.getActiveReminderHabits().forEach { habit ->
                        val parts = habit.time.split(":")
                        if (parts.size == 2) {
                            val hour = parts[0].toIntOrNull() ?: return@forEach
                            val minute = parts[1].toIntOrNull() ?: return@forEach
                            NotificationScheduler.scheduleHabitReminder(
                                context = context,
                                habitId = "default_${habit.id}",
                                habitName = habit.name,
                                hour = hour,
                                minute = minute
                            )
                        }
                    }
                }.onFailure { e ->
                    Log.e("BootCompletedReceiver", "Failed to restore alarms", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
