package com.islami.Aha.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.islami.Aha.util.NotificationReceiver

/**
 * Build debug saja. Contoh:
 * adb shell am broadcast -a com.islami.Aha.debug.TRIGGER_REMINDER -p com.Islamic.Aha
 *   --es habit_id default_2 --es habit_name "Sholat Dzuhur" --ei hour 12 --ei minute 1 --ez is_snooze true
 * is_snooze=true supaya jadwal harian yang asli tidak ikut diubah.
 */
class DebugReminderTrigger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.sendBroadcast(
            Intent(context, NotificationReceiver::class.java).putExtras(intent.extras ?: return)
        )
    }
}
