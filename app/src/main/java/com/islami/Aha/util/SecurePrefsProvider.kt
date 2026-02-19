package com.islami.Aha.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefsProvider {
    private const val LEGACY_PREFS_NAME = "aha_prefs"
    private const val SECURE_PREFS_NAME = "aha_secure_prefs"
    private const val KEY_MIGRATED = "__secure_prefs_migrated_v1"

    fun get(context: Context): SharedPreferences {
        return runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val securePrefs = EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            migrateLegacyIfNeeded(context, securePrefs)
            securePrefs
        }.getOrElse {
            context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun migrateLegacyIfNeeded(
        context: Context,
        securePrefs: SharedPreferences
    ) {
        if (securePrefs.getBoolean(KEY_MIGRATED, false)) return

        val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val allEntries = legacyPrefs.all

        if (allEntries.isNotEmpty()) {
            val secureEditor = securePrefs.edit()
            allEntries.forEach { (key, value) ->
                when (value) {
                    is String -> secureEditor.putString(key, value)
                    is Int -> secureEditor.putInt(key, value)
                    is Long -> secureEditor.putLong(key, value)
                    is Float -> secureEditor.putFloat(key, value)
                    is Boolean -> secureEditor.putBoolean(key, value)
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        secureEditor.putStringSet(key, value.filterIsInstance<String>().toSet())
                    }
                }
            }
            secureEditor.putBoolean(KEY_MIGRATED, true).apply()

            // Remove plain-text values after successful migration.
            legacyPrefs.edit().clear().apply()
        } else {
            securePrefs.edit().putBoolean(KEY_MIGRATED, true).apply()
        }
    }
}
