package com.islami.Aha.util

import android.Manifest
import android.app.PendingIntent
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.islami.Aha.BuildConfig
import com.islami.Aha.R
import com.islami.Aha.MainActivity
import java.util.Locale

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
        private const val GROUP_KEY_HABIT_REMINDER = "group_habit_reminder"
        private const val SUMMARY_NOTIFICATION_ID = 777001
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("habit_id") ?: return
        val habitName = intent.getStringExtra("habit_name")
            ?: context.getString(R.string.notification_default_habit_name)
        val hour = intent.getIntExtra("hour", -1)
        val minute = intent.getIntExtra("minute", -1)

        debugLog("onReceive: habitId=$habitId, habitName=$habitName, hour=$hour, minute=$minute")

        if (!NotificationScheduler.isGlobalNotificationEnabled(context)) {
            debugLog("Global notification disabled, skipping receiver execution")
            return
        }

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping notification")
                // Still reschedule even if permission not granted
                rescheduleAlarm(context, habitId, habitName, hour, minute)
                return
            }
        }

        val capability = NotificationScheduler.getNotificationCapability(context)
        if (capability != NotificationScheduler.NotificationCapability.AVAILABLE) {
            Log.w(TAG, "Notification unavailable ($capability), skipping notification")
            rescheduleAlarm(context, habitId, habitName, hour, minute)
            return
        }

        // Show notification
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NotificationScheduler.notificationId(habitId),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channelSettings = NotificationScheduler.getChannelSettings(context)
        val notificationTitle = context.getString(R.string.notification_reminder_title)
        val notificationBuilder = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_notification)
            .setContentTitle(notificationTitle)
            .setContentText(context.getString(R.string.notification_trigger_text_format, habitName))
            .setGroup(GROUP_KEY_HABIT_REMINDER)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        when (channelSettings.soundOption) {
            NotificationScheduler.NotificationSoundOption.SYSTEM_DEFAULT -> {
                notificationBuilder
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(NotificationScheduler.getSoundUri(channelSettings.soundOption))
            }
            NotificationScheduler.NotificationSoundOption.GENTLE -> {
                notificationBuilder
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setSound(NotificationScheduler.getSoundUri(channelSettings.soundOption))
            }
            NotificationScheduler.NotificationSoundOption.SILENT -> {
                notificationBuilder
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setSilent(true)
            }
        }

        if (channelSettings.vibrationEnabled) {
            notificationBuilder.setVibrate(longArrayOf(0, 180, 80, 180))
        } else {
            notificationBuilder.setVibrate(longArrayOf(0))
        }

        val notification = notificationBuilder.build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NotificationScheduler.notificationId(habitId), notification)
        val summaryNotification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_notification)
            .setContentTitle(notificationTitle)
            .setContentText(context.getString(R.string.notification_summary_text))
            .setGroup(GROUP_KEY_HABIT_REMINDER)
            .setGroupSummary(true)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
        debugLog("Notification shown for '$habitName'")

        // Reschedule for next day
        rescheduleAlarm(context, habitId, habitName, hour, minute)
    }

    private fun rescheduleAlarm(
        context: Context,
        habitId: String,
        habitName: String,
        hour: Int,
        minute: Int
    ) {
        if (hour >= 0 && minute >= 0) {
            NotificationScheduler.scheduleHabitReminder(
                context = context,
                habitId = habitId,
                habitName = habitName,
                hour = hour,
                minute = minute
            )
            debugLog(
                String.format(
                    Locale.ROOT,
                    "Rescheduled for next day: '%s' at %02d:%02d",
                    habitName,
                    hour,
                    minute
                )
            )
        } else {
            Log.w(TAG, "Cannot reschedule: invalid hour=$hour, minute=$minute")
        }
    }
}
