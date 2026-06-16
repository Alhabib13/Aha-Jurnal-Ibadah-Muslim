package com.islami.Aha.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

object NotificationScheduler {
    enum class NotificationCapability {
        AVAILABLE,
        PERMISSION_DENIED,
        SYSTEM_DISABLED,
        CHANNEL_DISABLED
    }

    enum class NotificationSoundOption(
        val value: String,
        val displayName: String,
        val description: String
    ) {
        SYSTEM_DEFAULT(
            value = "system_default",
            displayName = "Default Sistem",
            description = "Gunakan suara notifikasi bawaan perangkat"
        ),
        GENTLE(
            value = "gentle",
            displayName = "Lembut",
            description = "Tetap berbunyi dengan prioritas lebih rendah"
        ),
        SILENT(
            value = "silent",
            displayName = "Hening",
            description = "Tanpa suara notifikasi"
        );

        companion object {
            fun fromValue(value: String?): NotificationSoundOption {
                return values().firstOrNull { it.value == value } ?: SYSTEM_DEFAULT
            }
        }
    }

    data class NotificationChannelSettings(
        val soundOption: NotificationSoundOption,
        val vibrationEnabled: Boolean
    )

    const val CHANNEL_ID = "habit_reminder_channel"
    private const val CHANNEL_NAME = "Pengingat Ibadah"
    private const val TAG = "NotificationScheduler"
    const val KEY_GLOBAL_NOTIFICATION_ENABLED = "global_notification_enabled"
    const val KEY_NOTIFICATION_SOUND = "notification_sound_option"
    const val KEY_NOTIFICATION_VIBRATION = "notification_vibration_enabled"

    // SharedPreferences registry: habitId → unique sequential requestCode.
    // Collision-free karena setiap habitId dapat integer unik yang di-increment.
    private const val PREFS_REQUEST_CODES = "notification_request_codes"
    private const val KEY_NEXT_CODE = "_next_code"
    private const val NEXT_CODE_INITIAL = 1

    private fun debugLog(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message)
        }
    }

    /**
     * Kembalikan requestCode yang sudah tersimpan untuk [habitId], atau buat baru
     * secara sequential jika belum ada. Dijamin collision-free karena setiap
     * habitId mendapat integer unik yang di-increment — tidak ada dua habitId
     * yang berbagi requestCode yang sama.
     */
    @Synchronized
    private fun getOrAssignRequestCode(context: Context, habitId: String): Int {
        val prefs = context.getSharedPreferences(PREFS_REQUEST_CODES, Context.MODE_PRIVATE)
        val existing = prefs.getInt(habitId, -1)
        if (existing != -1) return existing
        val next = prefs.getInt(KEY_NEXT_CODE, NEXT_CODE_INITIAL)
        prefs.edit()
            .putInt(habitId, next)
            .putInt(KEY_NEXT_CODE, next + 1)
            .apply()
        debugLog("Assigned new requestCode=$next for habitId=$habitId")
        return next
    }

    /**
     * Stable notification ID untuk ditampilkan di status bar.
     * Consistent per habitId — tidak berubah antar sesi.
     */
    fun notificationId(habitId: String): Int = legacyHashCode(habitId)

    /**
     * FNV-1a hash — hanya dipakai sebagai fallback untuk cancel alarm lama
     * yang dijadwalkan sebelum migrasi ke registry SharedPreferences.
     */
    private fun legacyHashCode(habitId: String): Int {
        var hash = 0x811c9dc5.toInt()
        for (c in habitId) {
            hash = hash xor c.code
            hash = hash * 0x01000193
        }
        return hash and 0x7FFFFFFF
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
                return
            }

            val channel = buildNotificationChannel(getChannelSettings(context))
            notificationManager.createNotificationChannel(channel)
            debugLog("Notification channel created: $CHANNEL_ID")
        }
    }

    fun applyChannelSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.deleteNotificationChannel(CHANNEL_ID)
            notificationManager.createNotificationChannel(buildNotificationChannel(getChannelSettings(context)))
            debugLog("Notification channel recreated with latest settings")
        }
    }

    fun getChannelSettings(context: Context): NotificationChannelSettings {
        val prefs = SecurePrefsProvider.get(context)
        return NotificationChannelSettings(
            soundOption = NotificationSoundOption.fromValue(
                prefs.getString(KEY_NOTIFICATION_SOUND, NotificationSoundOption.SYSTEM_DEFAULT.value)
            ),
            vibrationEnabled = prefs.getBoolean(KEY_NOTIFICATION_VIBRATION, true)
        )
    }

    fun getSoundUri(option: NotificationSoundOption): Uri? {
        return when (option) {
            NotificationSoundOption.SYSTEM_DEFAULT,
            NotificationSoundOption.GENTLE -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            NotificationSoundOption.SILENT -> null
        }
    }

    fun isGlobalNotificationEnabled(context: Context): Boolean {
        return runCatching {
            SecurePrefsProvider.get(context)
                .getBoolean(KEY_GLOBAL_NOTIFICATION_ENABLED, true)
        }.getOrDefault(true)
    }

    fun getNotificationCapability(context: Context): NotificationCapability {
        return runCatching {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                return@runCatching NotificationCapability.SYSTEM_DISABLED
            }
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@runCatching NotificationCapability.PERMISSION_DENIED
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                    return@runCatching NotificationCapability.CHANNEL_DISABLED
                }
            }
            NotificationCapability.AVAILABLE
        }.getOrDefault(NotificationCapability.AVAILABLE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildNotificationChannel(
        settings: NotificationChannelSettings
    ): NotificationChannel {
        val importance = when (settings.soundOption) {
            NotificationSoundOption.SYSTEM_DEFAULT -> NotificationManager.IMPORTANCE_HIGH
            NotificationSoundOption.GENTLE -> NotificationManager.IMPORTANCE_DEFAULT
            NotificationSoundOption.SILENT -> NotificationManager.IMPORTANCE_LOW
        }

        return NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = "Pengingat untuk ibadah harian"
            setSound(
                getSoundUri(settings.soundOption),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(settings.vibrationEnabled)
            if (settings.vibrationEnabled) {
                vibrationPattern = longArrayOf(0, 180, 80, 180)
            } else {
                vibrationPattern = longArrayOf(0)
            }
        }
    }

    fun scheduleHabitReminder(
        context: Context,
        habitId: String,
        habitName: String,
        hour: Int,
        minute: Int
    ) {
        if (!isGlobalNotificationEnabled(context)) {
            debugLog("Skip scheduling '$habitName' because global notification is disabled")
            cancelHabitReminder(context, habitId)
            return
        }
        val capability = getNotificationCapability(context)
        if (capability != NotificationCapability.AVAILABLE) {
            debugLog(
                "Scheduling '$habitName' while notifications are not fully available: $capability"
            )
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("habit_id", habitId)
            putExtra("habit_name", habitName)
            putExtra("hour", hour)
            putExtra("minute", minute)
        }

        val requestCode = getOrAssignRequestCode(context, habitId)
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
                debugLog("Exact alarm set for '$habitName' at %02d:%02d (trigger=${triggerTime})".format(hour, minute))
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                debugLog("Inexact alarm set for '$habitName' at %02d:%02d (no exact alarm permission)".format(hour, minute))
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            debugLog("Exact alarm set for '$habitName' at %02d:%02d".format(hour, minute))
        }
    }

    fun cancelHabitReminder(context: Context, habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = context.getSharedPreferences(PREFS_REQUEST_CODES, Context.MODE_PRIVATE)
        val storedCode = prefs.getInt(habitId, -1)

        fun cancelCode(code: Int) {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pi)
        }

        if (storedCode != -1) {
            // Kode baru dari registry — path normal
            cancelCode(storedCode)
        } else {
            // Migrasi: alarm lama dijadwalkan dengan kode hash sebelum registry ada
            cancelCode(legacyHashCode(habitId))
        }

        // Hapus dari registry sehingga habitId yang sama bisa mendapat kode baru jika dijadwal ulang
        prefs.edit().remove(habitId).apply()
        debugLog("Alarm cancelled for habitId=$habitId (code=${if (storedCode != -1) storedCode else "legacy"})")
    }
}
