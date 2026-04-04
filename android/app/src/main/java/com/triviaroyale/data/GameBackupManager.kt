package com.triviaroyale.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import com.triviaroyale.BuildConfig
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class BackupStatus(
    val accessGranted: Boolean,
    val permissionRequired: Boolean,
    val backupExists: Boolean,
    val backupPath: String,
    val lastModifiedAt: Long?,
    val lastErrorMessage: String?
)

data class BackupOperationResult(
    val success: Boolean,
    val message: String? = null
)

class GameBackupManager(
    private val context: Context,
    private val gson: Gson
) {
    private var lastErrorMessage: String? = null

    fun canAccessSharedBackup(): Boolean = true

    fun hasBackupFile(uid: String?, email: String?): Boolean = backupFile(uid, email).exists()

    fun getStatus(uid: String? = null, email: String? = null): BackupStatus {
        val file = backupFile(uid, email)
        return BackupStatus(
            accessGranted = true,
            permissionRequired = false,
            backupExists = file.exists(),
            backupPath = file.absolutePath,
            lastModifiedAt = file.takeIf(File::exists)?.lastModified()?.takeIf { it > 0L },
            lastErrorMessage = lastErrorMessage
        )
    }

    @Synchronized
    fun backupState(
        state: GameState.State,
        uid: String = state.uid,
        email: String? = state.email
    ): BackupOperationResult {
        return try {
            val targetFile = backupFile(uid, email)
            targetFile.parentFile?.mkdirs()
            val payload = gson.toJson(state).toByteArray(StandardCharsets.UTF_8)
            targetFile.writeBytes(encrypt(payload))
            lastErrorMessage = null
            BackupOperationResult(success = true)
        } catch (error: Exception) {
            lastErrorMessage = error.message ?: "Backup write failed."
            BackupOperationResult(success = false, message = lastErrorMessage)
        }
    }

    @Synchronized
    fun restoreState(uid: String, email: String?): GameState.State? {
        val targetFile = backupFile(uid, email)
        if (!targetFile.exists()) {
            lastErrorMessage = null
            return null
        }

        return try {
            val decrypted = decrypt(targetFile.readBytes())
            val restored = gson.fromJson(
                String(decrypted, StandardCharsets.UTF_8),
                GameState.State::class.java
            )
            checkNotNull(restored) { "Backup file did not contain a valid state." }
            lastErrorMessage = null
            restored
        } catch (error: Exception) {
            lastErrorMessage = error.message ?: "Backup restore failed."
            null
        }
    }

    fun restoreLegacyGlobalState(): GameState.State? = null

    fun deleteLegacyGlobalBackup() {
    }

    fun deleteBackupForProfile(uid: String?, email: String?) {
        runCatching {
            backupFile(uid, email).takeIf(File::exists)?.delete()
        }
    }

    fun migrateVisibleLegacyEmailFolders() {
    }

    private fun backupFile(uid: String?, email: String?): File {
        val profileId = (uid ?: email ?: "default")
            .trim()
            .ifBlank { "default" }
            .replace(Regex("[^A-Za-z0-9@._-]"), "_")
            .take(96)

        return File(context.filesDir, "backup/$profileId/data.bin")
    }

    private fun encrypt(plainBytes: ByteArray): ByteArray {
        val iv = ByteArray(GCM_IV_SIZE).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))
        val encrypted = cipher.doFinal(plainBytes)
        return ByteBuffer.allocate(8 + iv.size + encrypted.size)
            .putInt(FILE_MAGIC)
            .putInt(iv.size)
            .put(iv)
            .put(encrypted)
            .array()
    }

    private fun decrypt(cipherBytes: ByteArray): ByteArray {
        val buffer = ByteBuffer.wrap(cipherBytes)
        check(buffer.remaining() > 8) { "Backup file is too small." }
        check(buffer.int == FILE_MAGIC) { "Backup file signature does not match." }
        val ivLength = buffer.int
        check(ivLength in 12..16) { "Backup file IV is invalid." }
        check(buffer.remaining() > ivLength) { "Backup file payload is invalid." }

        val iv = ByteArray(ivLength)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    private fun secretKey(): SecretKeySpec {
        val keyMaterial = buildString {
            append(BuildConfig.APPLICATION_ID)
            append(":")
            append(BuildConfig.BACKUP_SECRET.ifBlank { DEFAULT_BACKUP_SECRET })
            append(":backup-v1")
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(keyMaterial.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }

    companion object {
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_SIZE = 12
        private const val GCM_TAG_SIZE_BITS = 128
        private const val FILE_MAGIC = 0x54525131
        private const val DEFAULT_BACKUP_SECRET = "trivia-royale-local-backup-v1"
        private val secureRandom = SecureRandom()
    }
}

fun hasSharedBackupAccess(@Suppress("UNUSED_PARAMETER") context: Context): Boolean = true

fun requiredLegacyBackupPermissions(): Array<String> = emptyArray()

fun createBackupAccessIntent(context: Context): Intent {
    return Intent().apply {
        data = Uri.parse("package:${context.packageName}")
    }
}
