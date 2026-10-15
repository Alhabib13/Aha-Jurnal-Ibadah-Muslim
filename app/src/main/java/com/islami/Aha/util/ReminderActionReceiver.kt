package com.islami.Aha.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.islami.Aha.R
import com.islami.Aha.data.repository.DefaultHabitCompletionRepository
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.ui.widget.AhaWidgetUpdater
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Tombol di notifikasi pengingat: Sudah / Nanti / Tertinggal, dan Batalkan pada
 * konfirmasi "tercatat". Hanya "Sudah" yang menulis catatan ibadah, dengan
 * sumber_pencatatan = dari_notifikasi.
 */
class ReminderActionReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ActionEntryPoint {
        fun defaultHabitCompletion(): DefaultHabitCompletionRepository
    }

    data class Payload(
        val habitDbId: Int,
        val habitId: String,
        val habitName: String,
        val namaTampil: String,
        val hour: Int,
        val minute: Int,
        val notificationId: Int,
        val isFardhu: Boolean
    )

    companion object {
        private const val TAG = "ReminderAction"
        const val ACTION_SUDAH = "com.islami.Aha.action.SUDAH"
        const val ACTION_NANTI = "com.islami.Aha.action.NANTI"
        const val ACTION_TERTINGGAL = "com.islami.Aha.action.TERTINGGAL"
        const val ACTION_BATALKAN = "com.islami.Aha.action.BATALKAN"
        const val SNOOZE_MINUTES = 15

        /** Batalkan hanya sah sebentar setelah "Sudah" (notifikasinya hilang setelah 10 detik). */
        private const val UNDO_VISIBLE_MS = 10_000L
        private const val UNDO_GRACE_MS = 30_000L
        private const val CONFIRM_CHANNEL_ID = "aha_konfirmasi"
        private const val PREFS = "reminder_actions"

        fun pendingIntent(context: Context, action: String, p: Payload): PendingIntent {
            val intent = Intent(context, ReminderActionReceiver::class.java).apply {
                this.action = action
                putExtra("habit_db_id", p.habitDbId)
                putExtra("habit_id", p.habitId)
                putExtra("habit_name", p.habitName)
                putExtra("nama_tampil", p.namaTampil)
                putExtra("hour", p.hour)
                putExtra("minute", p.minute)
                putExtra("notification_id", p.notificationId)
                putExtra("is_fardhu", p.isFardhu)
            }
            return PendingIntent.getBroadcast(
                context,
                java.util.Objects.hash(p.notificationId, action),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun payloadOf(intent: Intent): Payload? {
            val dbId = intent.getIntExtra("habit_db_id", -1).takeIf { it >= 0 } ?: return null
            return Payload(
                habitDbId = dbId,
                habitId = intent.getStringExtra("habit_id") ?: return null,
                habitName = intent.getStringExtra("habit_name") ?: return null,
                namaTampil = intent.getStringExtra("nama_tampil") ?: "",
                hour = intent.getIntExtra("hour", -1),
                minute = intent.getIntExtra("minute", -1),
                notificationId = intent.getIntExtra("notification_id", 0),
                isFardhu = intent.getBooleanExtra("is_fardhu", false)
            )
        }

        private fun snoozeRequestCode(p: Payload) = java.util.Objects.hash(p.habitId, "snooze")

        private fun snoozeIntent(context: Context, p: Payload): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("habit_id", p.habitId)
                putExtra("habit_name", p.habitName)
                putExtra("hour", p.hour)
                putExtra("minute", p.minute)
                putExtra(NotificationReceiver.EXTRA_IS_SNOOZE, true)
            }
            return PendingIntent.getBroadcast(
                context,
                snoozeRequestCode(p),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val p = payloadOf(intent) ?: return
        val nm = context.getSystemService(NotificationManager::class.java)
        when (intent.action) {
            ACTION_NANTI -> {
                nm.cancel(p.notificationId)
                val waktu = jadwalkanUlang(context, p)
                toast(context, "Diingatkan lagi pukul $waktu")
            }
            ACTION_TERTINGGAL -> {
                nm.cancel(p.notificationId)
                batalkanPengingatUlang(context, p)
                // Hanya penanda tampilan (widget), TIDAK ditulis ke data penelitian:
                // baris status "tidak" hanya boleh berasal dari aturan slot harian.
                TertinggalStore.tandai(context, p.habitDbId)
                AhaWidgetUpdater.requestUpdate(context)
                toast(context, "${p.namaTampil} ditandai tertinggal")
            }
            ACTION_SUDAH -> jalankan(context) { repo ->
                batalkanPengingatUlang(context, p)
                TertinggalStore.hapus(context, p.habitDbId)
                val hasil = repo.setCompleted(p.habitDbId, true, ResearchSchema.Sumber.DARI_NOTIFIKASI)
                if (hasil == null) {
                    nm.cancel(p.notificationId)
                    return@jalankan
                }
                if (hasil.berubah) {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putLong("undo_${p.habitDbId}", System.currentTimeMillis()).apply()
                }
                tampilkanKonfirmasi(context, p, repo, bisaDibatalkan = hasil.berubah)
            }
            ACTION_BATALKAN -> jalankan(context) { repo ->
                nm.cancel(p.notificationId)
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val sejak = System.currentTimeMillis() - prefs.getLong("undo_${p.habitDbId}", 0L)
                prefs.edit().remove("undo_${p.habitDbId}").apply()
                if (sejak in 0..UNDO_GRACE_MS) {
                    repo.setCompleted(p.habitDbId, false)
                    withContext(Dispatchers.Main) { toast(context, "Catatan ${p.namaTampil} dibatalkan") }
                }
            }
        }
    }

    private fun jalankan(context: Context, block: suspend (DefaultHabitCompletionRepository) -> Unit) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = EntryPointAccessors.fromApplication(context.applicationContext, ActionEntryPoint::class.java)
                    .defaultHabitCompletion()
                block(repo)
            } catch (e: Exception) {
                Log.e(TAG, "Reminder action failed", e)
                logCrashlyticsSafe(e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun tampilkanKonfirmasi(
        context: Context,
        p: Payload,
        repo: DefaultHabitCompletionRepository,
        bisaDibatalkan: Boolean
    ) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CONFIRM_CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CONFIRM_CHANNEL_ID, "Konfirmasi catatan", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Konfirmasi singkat setelah mencatat dari notifikasi"
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
        val teks = if (p.isFardhu) {
            val (selesai, total) = repo.fardhuProgressToday()
            "Alhamdulillah · $selesai dari $total sholat hari ini"
        } else {
            "Alhamdulillah, sudah tercatat"
        }
        val builder = NotificationCompat.Builder(context, CONFIRM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_aha)
            .setColor(0xFF0D7C5F.toInt())
            .setContentTitle("${p.namaTampil} tercatat")
            .setContentText(teks)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setAutoCancel(true)
            .setTimeoutAfter(UNDO_VISIBLE_MS)
        if (bisaDibatalkan) {
            builder.addAction(0, "Batalkan", pendingIntent(context, ACTION_BATALKAN, p))
        }
        // Id yang sama: notifikasi pengingat berubah menjadi konfirmasi.
        nm.notify(p.notificationId, builder.build())
    }

    private fun jadwalkanUlang(context: Context, p: Payload): String {
        val trigger = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = snoozeIntent(context, p)
        val bisaTepat = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        runCatching {
            if (bisaTepat) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }.onFailure {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = trigger }
        return String.format(
            Locale.ROOT, "%02d.%02d",
            cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE)
        )
    }

    private fun batalkanPengingatUlang(context: Context, p: Payload) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(snoozeIntent(context, p))
    }

    private fun toast(context: Context, pesan: String) {
        runCatching { Toast.makeText(context.applicationContext, pesan, Toast.LENGTH_SHORT).show() }
    }
}

/**
 * Penanda lokal "tertinggal" dari tombol notifikasi, hanya untuk tampilan widget hari ini.
 * Tidak dikirim ke mana pun dan bukan bagian data penelitian.
 */
object TertinggalStore {
    private const val PREFS = "tertinggal_hari_ini"

    private fun kunci(habitDbId: Int) = "${DateUtils.getTodayKey()}|$habitDbId"

    fun tandai(context: Context, habitDbId: Int) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val hariIni = DateUtils.getTodayKey()
        val sisa = p.getStringSet("daftar", emptySet()).orEmpty().filter { it.startsWith("$hariIni|") }.toMutableSet()
        sisa += kunci(habitDbId)
        p.edit().putStringSet("daftar", sisa).apply()
    }

    fun hapus(context: Context, habitDbId: Int) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val sisa = p.getStringSet("daftar", emptySet()).orEmpty() - kunci(habitDbId)
        p.edit().putStringSet("daftar", sisa).apply()
    }

    fun hariIni(context: Context): Set<Int> {
        val awalan = "${DateUtils.getTodayKey()}|"
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet("daftar", emptySet()).orEmpty()
            .filter { it.startsWith(awalan) }
            .mapNotNull { it.removePrefix(awalan).toIntOrNull() }
            .toSet()
    }
}
