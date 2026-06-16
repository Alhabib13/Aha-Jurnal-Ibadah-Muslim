package com.islami.Aha

import android.app.Application
import android.os.Build
import android.util.Log
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.repository.CompletionSyncRepository
import com.islami.Aha.data.repository.UserHabitRepository
import com.islami.Aha.ui.theme.ThemeManager
import com.islami.Aha.util.NotificationScheduler
import com.islami.Aha.util.SecurePrefsProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
    companion object {
        private const val TAG = "AhaApplication"
        private const val APP_CHECK_LOG_TAG = "AhaAppCheck"
    }
    private enum class AppCheckMode {
        NONE,
        DEBUG,
        PLAY_INTEGRITY,
        AUTO
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppEntryPoint {
        fun userHabitRepository(): UserHabitRepository
        fun habitDao(): HabitDao
        fun completionSyncRepository(): CompletionSyncRepository
    }

    override fun onCreate() {
        super.onCreate()
        val appCheckMode = resolveAppCheckMode()
        Log.i(
            TAG,
            "App start env=${BuildConfig.APP_ENV}, debug=${BuildConfig.DEBUG}, appCheckMode=$appCheckMode, firebaseProject=${BuildConfig.FIREBASE_PROJECT_ID}, firebaseAppId=${BuildConfig.FIREBASE_MOBILE_SDK_APP_ID}"
        )
        if (BuildConfig.FIREBASE_PROJECT_ID.isBlank() || BuildConfig.FIREBASE_MOBILE_SDK_APP_ID.isBlank()) {
            Log.e(TAG, "Firebase config is blank. Check app/google-services.json.")
        }
        initializeFirebaseAppCheck(appCheckMode)
        val enableCrashlytics = BuildConfig.APP_ENV == "prod" && !BuildConfig.DEBUG
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enableCrashlytics)
        FirebaseCrashlytics.getInstance().log("AhaApplication started")
        val prefs = SecurePrefsProvider.get(this)
        ThemeManager.init(prefs)
        NotificationScheduler.createNotificationChannel(this)
        restoreAlarms()
    }

    private fun resolveAppCheckMode(): AppCheckMode {
        if (BuildConfig.FORCE_APPCHECK_DEBUG) return AppCheckMode.DEBUG
        return when (BuildConfig.APP_CHECK_MODE.uppercase()) {
            "DEBUG" -> AppCheckMode.DEBUG
            "PLAY_INTEGRITY" -> AppCheckMode.PLAY_INTEGRITY
            "AUTO" -> AppCheckMode.AUTO
            else -> AppCheckMode.NONE
        }
    }

    private fun initializeFirebaseAppCheck(mode: AppCheckMode) {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        val appCheck = FirebaseAppCheck.getInstance()
        val resolvedMode = if (mode == AppCheckMode.AUTO) {
            if (isProbablyEmulator()) AppCheckMode.DEBUG else AppCheckMode.PLAY_INTEGRITY
        } else {
            mode
        }
        when (resolvedMode) {
            AppCheckMode.NONE -> {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(
                        TAG,
                        "Firebase App Check provider: NONE (dev mode). Keep App Check enforcement disabled in Firebase dev project."
                    )
                }
            }
            AppCheckMode.DEBUG -> {
                appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(
                        TAG,
                        "Firebase App Check provider: DEBUG. If login still fails, register debug token from Logcat tag DebugAppCheckProvider."
                    )
                }
                appCheck.getAppCheckToken(true)
                    .addOnSuccessListener {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(
                                TAG,
                                "Firebase App Check token fetched with DEBUG provider. Register debug token from Logcat tag DebugAppCheckProvider if required."
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            TAG,
                            "Firebase App Check debug token fetch failed. Register debug token in Firebase Console > App Check > Manage debug tokens.",
                            e
                        )
                    }
            }
            AppCheckMode.PLAY_INTEGRITY -> {
                appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Firebase App Check provider: PLAY_INTEGRITY.")
                }
            }
            AppCheckMode.AUTO -> Unit
        }
        if (mode == AppCheckMode.AUTO && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(
                APP_CHECK_LOG_TAG,
                "App Check AUTO resolved to $resolvedMode (emulator=${isProbablyEmulator()})."
            )
        }
        appCheck.setTokenAutoRefreshEnabled(resolvedMode != AppCheckMode.NONE)
    }

    private fun isProbablyEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.lowercase().contains("emulator") ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            "google_sdk".equals(Build.PRODUCT, ignoreCase = true)
    }

    private fun restoreAlarms() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    this@AhaApplication,
                    AppEntryPoint::class.java
                )
                val repository = entryPoint.userHabitRepository()
                val habitDao = entryPoint.habitDao()
                val activeHabits = repository.getActiveReminderHabits()
                val activeDefaultHabits = habitDao.getActiveReminderHabits()

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

                activeDefaultHabits.forEach { habit ->
                    val parts = habit.time.split(":")
                    if (parts.size == 2) {
                        val hour = parts[0].toIntOrNull() ?: return@forEach
                        val minute = parts[1].toIntOrNull() ?: return@forEach
                        NotificationScheduler.scheduleHabitReminder(
                            context = this@AhaApplication,
                            habitId = "default_${habit.id}",
                            habitName = habit.name,
                            hour = hour,
                            minute = minute
                        )
                    }
                }
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Restored ${activeHabits.size + activeDefaultHabits.size} alarms")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore alarms", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }
}
