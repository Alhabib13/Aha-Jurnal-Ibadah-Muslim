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
        fun userHabitDao(): com.islami.Aha.data.local.UserHabitDao
    }

    companion object {
        private const val TAG = "NotificationReceiver"
        private const val GROUP_KEY_HABIT_REMINDER = "group_habit_reminder"
        private const val SUMMARY_NOTIFICATION_ID = 777001
        const val EXTRA_IS_SNOOZE = "is_snooze"
        /** Hari ibadah pengingat ulang (yyyy-MM-dd); Isya yang ditunda lewat tengah malam tetap milik kemarin. */
        const val EXTRA_TANGGAL = "tanggal"
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
        val tanggal = intent.getStringExtra(EXTRA_TANGGAL) ?: DateUtils.getTodayKey()

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
                        showActionableReminder(context, defaultHabitDbId, habitId, habitName, hour, minute, isSnooze, tanggal, prefs)
                    }.onFailure { e ->
                        Log.e(TAG, "Actionable reminder failed, falling back to plain notification", e)
                        showPlainReminder(context, habitId, habitName, prefs, KanalNotifikasi.Jenis.SHOLAT_SUNNAH)
                    }
                    reschedule()
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // Habit buatan pengguna / pengingat Mode Cuti: channel menurut jenisnya (perlu baca database).
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jenis = if (habitId == "haidh_reminder") {
                    KanalNotifikasi.Jenis.MODE_CUTI
                } else {
                    val kategori = runCatching {
                        EntryPointAccessors.fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
                            .userHabitDao().getHabitById(habitId)?.category
                    }.getOrNull()
                    KanalNotifikasi.jenisHabitSunnah(kategori)
                }
                runCatching { showPlainReminder(context, habitId, habitName, prefs, jenis) }
                    .onFailure { Log.e(TAG, "Plain reminder failed", it) }
                reschedule()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun showActionableReminder(
        context: Context,
        habitDbId: Int,
        habitId: String,
        habitName: String,
        hour: Int,
        minute: Int,
        isSnooze: Boolean,
        tanggal: String,
        prefs: android.content.SharedPreferences
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
        val habit: Habit = entryPoint.habitDao().getHabitById(habitDbId) ?: run {
            showPlainReminder(context, habitId, habitName, prefs, KanalNotifikasi.Jenis.SHOLAT_SUNNAH)
            return
        }
        if (entryPoint.defaultHabitCompletion().isCompletedOn(habitDbId, tanggal)) {
            debugLog("'${habit.name}' already recorded for $tanggal, no reminder needed")
            return
        }

        val isFardhu = habit.category == "Sholat Fardhu"
        val slug = if (isFardhu) SuaraSholat.slugDariNama(habit.name) else null
        val namaTampil = namaTampil(habit.name, prefs, tanggal)
        val notificationId = NotificationScheduler.notificationId(habitId)
        val payload = ReminderActionReceiver.Payload(
            habitDbId = habitDbId,
            habitId = habitId,
            habitName = habit.name,
            namaTampil = namaTampil,
            hour = hour,
            minute = minute,
            notificationId = notificationId,
            isFardhu = isFardhu,
            tanggal = tanggal
        )

        val jenis = KanalNotifikasi.jenisHabitBawaan(habit.category)
        val channelId = KanalNotifikasi.idUntuk(jenis, slug)
        val alarm = KanalNotifikasi.berkarakterAlarm(jenis)

        val jam = if (hour >= 0 && minute >= 0) String.format(Locale.ROOT, "%02d.%02d", hour, minute) else null
        val title = when {
            isSnooze && tanggal == java.time.LocalDate.now().minusDays(1).toString() -> "Pengingat $namaTampil kemarin"
            isSnooze && tanggal != DateUtils.getTodayKey() -> "Pengingat $namaTampil ($tanggal)"
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
            // Sahur berkarakter alarm: kategori alarm ikut dihormati mode Jangan Ganggu
            // (bersama jalur volume alarm di channelnya).
            .setCategory(if (alarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent(context, notificationId, habitId, tanggal))
            .setAutoCancel(true)
            .setPriority(if (alarm) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Sudah", ReminderActionReceiver.pendingIntent(context, ReminderActionReceiver.ACTION_SUDAH, payload))
        // Penundaan cukup sekali: pengingat ulang hanya menawarkan Sudah / Tertinggal.
        if (!isSnooze) {
            builder.addAction(0, "Nanti ${ReminderActionReceiver.SNOOZE_MINUTES} mnt", ReminderActionReceiver.pendingIntent(context, ReminderActionReceiver.ACTION_NANTI, payload))
        }
        if (isFardhu) {
            builder.addAction(0, "Tertinggal", ReminderActionReceiver.pendingIntent(context, ReminderActionReceiver.ACTION_TERTINGGAL, payload))
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Tanpa channel: suara & getar diatur di notifikasinya.
            val uri = when {
                slug != null -> KanalNotifikasi.suaraSholat(context, slug)
                alarm -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                else -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            }
            @Suppress("DEPRECATION")
            if (uri != null) {
                builder.setSound(uri, if (slug != null || alarm) android.media.AudioManager.STREAM_ALARM else android.media.AudioManager.STREAM_NOTIFICATION)
            } else {
                builder.setSilent(true)
            }
            builder.setVibrate(longArrayOf(0, 180, 80, 180))
        }

        context.getSystemService(NotificationManager::class.java).notify(notificationId, builder.build())
        debugLog("Actionable notification shown for '${habit.name}'")
    }

    private fun namaTampil(habitName: String, prefs: android.content.SharedPreferences, tanggal: String): String {
        // Laki-laki di hari Jumat: Dzuhur tampil sebagai Jumat, kecuali hari itu diganti Dzuhur.
        val isJumatEnabled = prefs.getBoolean("jumat_enabled", false)
        val gantiDzuhur = com.islami.Aha.data.research.PilihanJumat(prefs).gantiDzuhur(tanggal)
        val jumat = habitName == com.islami.Aha.data.research.SholatJumat.NAMA_DZUHUR &&
            com.islami.Aha.data.research.SholatJumat.detailTengahHari(tanggal, isJumatEnabled, gantiDzuhur) ==
            com.islami.Aha.data.research.ResearchSchema.Detail.JUMAT
        val nama = if (jumat) com.islami.Aha.data.research.SholatJumat.NAMA_JUMAT else habitName
        return nama.removePrefix("Sholat ").ifBlank { nama }
    }

    /**
     * Mengetuk badan notifikasi membuka aplikasi dengan penanda habit + tanggal pengingat,
     * supaya pencatatan berikutnya untuk habit itu berstatus "dari_notifikasi_dibuka".
     */
    private fun openAppIntent(context: Context, requestCode: Int, habitId: String?, tanggal: String): PendingIntent {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (habitId != null) {
                putExtra(NotifikasiSesi.EXTRA_HABIT_ID, habitId)
                putExtra(NotifikasiSesi.EXTRA_TANGGAL, tanggal)
            }
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
        prefs: android.content.SharedPreferences,
        jenis: KanalNotifikasi.Jenis
    ) {
        val contentPendingIntent = openAppIntent(
            context,
            NotificationScheduler.notificationId(habitId),
            habitId.takeIf { it != "haidh_reminder" },
            DateUtils.getTodayKey()
        )
        val channelId = KanalNotifikasi.idUntuk(jenis)
        val alarm = KanalNotifikasi.berkarakterAlarm(jenis)

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

        // Android 8+: suara & getar dari channel (diatur pengguna di Pengaturan Android).
        // Android 7 ke bawah: nilai bawaan per jenis.
        notificationBuilder
            .setCategory(if (alarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (alarm) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 180, 80, 180))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            notificationBuilder.setSound(
                android.media.RingtoneManager.getDefaultUri(
                    if (alarm) android.media.RingtoneManager.TYPE_ALARM else android.media.RingtoneManager.TYPE_NOTIFICATION
                ),
                if (alarm) android.media.AudioManager.STREAM_ALARM else android.media.AudioManager.STREAM_NOTIFICATION
            )
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
