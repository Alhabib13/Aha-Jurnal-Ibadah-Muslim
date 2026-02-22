package com.islami.Aha.ui.settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object SettingsBackupCrypto {
    private const val BACKUP_FORMAT = "AHA_BACKUP_ENC_V1"
    private const val BACKUP_KEY_ALIAS = "aha_backup_key_alias"

    fun encode(plainJson: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateBackupKey())
        val encryptedBytes = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv

        return JSONObject()
            .put("format", BACKUP_FORMAT)
            .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
            .put("checksumSha256", sha256Hex(plainJson))
            .toString()
    }

    fun decode(payload: String): String {
        val trimmed = payload.trim()
        if (!trimmed.startsWith("{")) return payload

        val root = runCatching { JSONObject(trimmed) }.getOrElse { return payload }
        if (root.optString("format") != BACKUP_FORMAT) return payload

        val ivBase64 = root.optString("iv")
        val ciphertextBase64 = root.optString("ciphertext")
        if (ivBase64.isBlank() || ciphertextBase64.isBlank()) {
            throw IllegalStateException("Format backup terenkripsi tidak valid")
        }

        val plainJson = runCatching {
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateBackupKey(),
                GCMParameterSpec(128, iv)
            )
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrElse {
            throw IllegalStateException("Backup terenkripsi tidak dapat dibuka pada perangkat ini")
        }

        val expectedChecksum = root.optString("checksumSha256")
        if (expectedChecksum.isNotBlank() && expectedChecksum != sha256Hex(plainJson)) {
            throw IllegalStateException("Integritas file backup tidak valid")
        }

        return plainJson
    }

    private fun getOrCreateBackupKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(BACKUP_KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                BACKUP_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun sha256Hex(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }
}
