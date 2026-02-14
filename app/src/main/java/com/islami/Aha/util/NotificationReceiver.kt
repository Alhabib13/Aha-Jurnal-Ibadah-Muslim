package com.islami.Aha.util

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.islami.Aha.R

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("habit_id") ?: return
        val habitName = intent.getStringExtra("habit_name") ?: "Ibadah"
        val hour = intent.getIntExtra("hour", -1)
        val minute = intent.getIntExtra("minute", -1)

        Log.d(TAG, "onReceive: habitId=$habitId, habitName=$habitName, hour=$hour, minute=$minute")

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

        // Show notification
        val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_notification)
            .setContentTitle("Pengingat Ibadah")
            .setContentText("Waktunya $habitName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(habitId.hashCode(), notification)
        Log.d(TAG, "Notification shown for '$habitName'")

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
            Log.d(TAG, "Rescheduled for next day: '$habitName' at %02d:%02d".format(hour, minute))
        } else {
            Log.w(TAG, "Cannot reschedule: invalid hour=$hour, minute=$minute")
        }
    }
}
