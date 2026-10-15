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
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.model.Habit
import com.islami.Aha.data.repository.DefaultHabitCompletionRepository
import com.islami.Aha.ui.widget.AhaWidgetUpdater
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class NotificationReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun habitDao(): HabitDao
        fun defaultHabitCompletion(): DefaultHabitCompletionRepository
    }

    companion object {
        private const val TAG = "NotificationReceiver"
        private const val GROUP_KEY_HABIT_REMINDER = "group_habit_reminder"
        private const val SUMMARY_NOTIFICATION_ID = 777001
        const val EXTRA_IS_SNOOZE = "is_snooze"
        private const val DEFAULT_PREFIX = "default_"
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
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)

        debugLog("onReceive: habitId=$habitId, habitName=$habitName, hour=$hour, minute=$minute, snooze=$isSnooze")

        // Widget ikut berganti "waktu sekarang" di setiap pergantian waktu sholat.
        AhaWidgetUpdater.requestUpdate(context)

        // Pengingat ulang ("Nanti") hanya sekali; jadwal hariannya tetap berjalan sendiri.
        val reschedule: () -> Unit = {
            if (!isSnooze && habitId != "haidh_reminder") rescheduleAlarm(context, habitId, habitName, hour, minute)
        }

        if (!NotificationScheduler.isGlobalNotificationEnabled(context)) {
            debugLog("Global notification disabled, skipping receiver execution")
            return
        }

        val prefs = com.islami.Aha.util.SecurePrefsProvider.get(context)
        val isHaidhMode = prefs.getBoolean("haidh_mode", false)
        if (isHaidhMode && (habitName.contains("Sholat", ignoreCase = true) || habitName.contains("Puasa", ignoreCase = true))) {
            debugLog("Haidh mode is active, skipping notification for: $habitName")
            reschedule()
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
                reschedule()
                return
            }
        }

        val capability = NotificationScheduler.getNotificationCapability(context)
        if (capability != NotificationScheduler.NotificationCapability.AVAILABLE) {
            Log.w(TAG, "Notification unavailable ($capability), skipping notification")
            reschedule()
            return
        }

        val defaultHabitDbId = habitId.removePrefix(DEFAULT_PREFIX).toIntOrNull()
            ?.takeIf { habitId.startsWith(DEFAULT_PREFIX) }
        if (defaultHabitDbId != null) {
            // Habit bawaan: tampilkan tombol Sudah / Nanti / Tertinggal (perlu baca database).
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    runCatching {
                        showActionableReminder(context, defaultHabitDbId, habitId, habitName, hour, minute, isSnooze, prefs)
                    }.onFailure { e ->
                        Log.e(TAG, "Actionable reminder failed, falling back to plain notification", e)
                        showPlainReminder(context, habitId, habitName, prefs)
                    }
                    reschedule()
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        showPlainReminder(context, habitId, habitName, prefs)
        reschedule()
    }

    private suspend fun showActionableReminder(
        context: Context,
        habitDbId: Int,
        habitId: String,
        habitName: String,
        hour: Int,
        minute: Int,
        isSnooze: Boolean,
        prefs: android.content.SharedPreferences
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
        val habit: Habit = entryPoint.habitDao().getHabitById(habitDbId) ?: run {
            showPlainReminder(context, habitId, habitName, prefs)
            return
        }
        if (entryPoint.defaultHabitCompletion().isCompletedToday(habitDbId)) {
            debugLog("'${habit.name}' already recorded today, no reminder needed")
            return
        }

        val isFardhu = habit.category == "Sholat Fardhu"
        val slug = if (isFardhu) SuaraSholat.slugDariNama(habit.name) else null
        val namaTampil = namaTampil(habit.name, prefs)
        val notificationId = NotificationScheduler.notificationId(habitId)
        val payload = ReminderActionReceiver.Payload(
            habitDbId = habitDbId,
            habitId = habitId,
            habitName = habit.name,
            namaTampil = namaTampil,
            hour = hour,
            minute = minute,
            notificationId = notificationId,
            isFardhu = isFardhu
        )

        val channelId = if (slug != null) {
            SuaraSholat.pastikanFileMasihAda(context, slug)
            SuaraSholat.channelId(context, slug)
        } else {
            NotificationScheduler.channelId(context)
        }

        val jam = if (hour >= 0 && minute >= 0) String.format(Locale.ROOT, "%02d.%02d", hour, minute) else null
        val title = when {
            isSnooze -> "Pengingat $namaTampil"
            isFardhu && jam != null -> "Waktunya $namaTampil · $jam"
            else -> context.getString(R.string.notification_trigger_text_format, namaTampil)
        }
        val text = if (isFardhu) "Sudah sholat $namaTampil? Ketuk tombol di bawah untuk mencatat."
        else "Sudah mengerjakan $namaTampil? Ketuk Sudah untuk mencatat."

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_aha)
            .setColor(0xFF0D7C5F.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent(context, notificationId))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Sudah", ReminderActionReceiver.pendingIntent(context, ReminderActionReceiver.ACTION_SUDAH, payload))
            .addAction(0, "Nanti ${ReminderActionReceiver.SNOOZE_MINUTES} mnt", ReminderActionReceiver.pendingIntent(context, ReminderActionReceiver.ACTION_NANTI, payload))
        if (isFardhu) {
            builder.addAction(0, "Tertinggal", ReminderActionReceiver.pendingIntent(context, ReminderActionReceiver.ACTION_TERTINGGAL, payload))
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Tanpa channel: suara & getar diatur di notifikasinya.
            val uri = if (slug != null) SuaraSholat.soundUri(context, slug)
            else NotificationScheduler.getSoundUri(NotificationScheduler.getChannelSettings(context).soundOption)
            @Suppress("DEPRECATION")
            if (uri != null) builder.setSound(uri, android.media.AudioManager.STREAM_ALARM) else builder.setSilent(true)
            builder.setVibrate(
                if (NotificationScheduler.getChannelSettings(context).vibrationEnabled) longArrayOf(0, 180, 80, 180)
                else longArrayOf(0)
            )
        }

        context.getSystemService(NotificationManager::class.java).notify(notificationId, builder.build())
        debugLog("Actionable notification shown for '${habit.name}'")
    }

    private fun namaTampil(habitName: String, prefs: android.content.SharedPreferences): String {
        // Cek apakah hari ini Jumat dan mode Jumat aktif (untuk Laki-laki)
        val isJumatEnabled = prefs.getBoolean("jumat_enabled", false)
        val isFriday = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY
        val nama = if (isJumatEnabled && isFriday && habitName == "Sholat Dzuhur") "Sholat Jumat" else habitName
        return nama.removePrefix("Sholat ").ifBlank { nama }
    }

    private fun openAppIntent(context: Context, requestCode: Int): PendingIntent {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showPlainReminder(
        context: Context,
        habitId: String,
        habitName: String,
        prefs: android.content.SharedPreferences
    ) {
        val contentPendingIntent = openAppIntent(context, NotificationScheduler.notificationId(habitId))
        val channelSettings = NotificationScheduler.getChannelSettings(context)
        val channelId = NotificationScheduler.channelId(context)

        val isJumatEnabled = prefs.getBoolean("jumat_enabled", false)
        val isFriday = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY
        var finalHabitName = habitName
        if (isJumatEnabled && isFriday && habitName == "Sholat Dzuhur") {
            finalHabitName = "Sholat Jumat"
        }

        val notificationTitle = if (habitId == "haidh_reminder") "Peringatan Cuti Ibadah" else context.getString(R.string.notification_reminder_title)
        val notificationText = if (habitId == "haidh_reminder") "Apakah masa cuti ibadah Anda sudah selesai? Jangan lupa matikan mode cuti di aplikasi Aha." else context.getString(R.string.notification_trigger_text_format, finalHabitName)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_aha)
            .setColor(0xFF0D7C5F.toInt())
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
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
        val summaryNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_aha)
            .setColor(0xFF0D7C5F.toInt())
            .setContentTitle(notificationTitle)
            .setContentText(context.getString(R.string.notification_summary_text))
            .setGroup(GROUP_KEY_HABIT_REMINDER)
            .setGroupSummary(true)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
        debugLog("Notification shown for '$habitName'")
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
