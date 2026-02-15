package com.islami.Aha.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object NotificationScheduler {

    const val CHANNEL_ID = "habit_reminder_channel"
    private const val CHANNEL_NAME = "Pengingat Ibadah"
    private const val TAG = "NotificationScheduler"

    /**
     * Generate a stable, positive request code from a habit ID string.
     * Uses a simple FNV-1a-inspired hash for better distribution than String.hashCode().
     */
    fun stableRequestCode(habitId: String): Int {
        var hash = 0x811c9dc5.toInt()
        for (c in habitId) {
            hash = hash xor c.code
            hash = hash * 0x01000193
        }
        return hash and 0x7FFFFFFF
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pengingat untuk ibadah harian"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }

    fun scheduleHabitReminder(
        context: Context,
        habitId: String,
        habitName: String,
        hour: Int,
        minute: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("habit_id", habitId)
            putExtra("habit_name", habitName)
            putExtra("hour", hour)
            putExtra("minute", minute)
        }

        val requestCode = stableRequestCode(habitId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val triggerTime = calendar.timeInMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Exact alarm set for '$habitName' at %02d:%02d (trigger=${triggerTime})".format(hour, minute))
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Inexact alarm set for '$habitName' at %02d:%02d (no exact alarm permission)".format(hour, minute))
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.d(TAG, "Exact alarm set for '$habitName' at %02d:%02d".format(hour, minute))
        }
    }

    fun cancelHabitReminder(context: Context, habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val requestCode = stableRequestCode(habitId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarm cancelled for habitId=$habitId")
    }
}
