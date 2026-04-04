package com.triviaroyale.firebase

import android.content.Context
import android.provider.Settings
import com.google.firebase.installations.FirebaseInstallations
import java.security.MessageDigest

class DeviceIdentityManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("device_identity", Context.MODE_PRIVATE)

    suspend fun getDeviceId(): String {
        val cached = prefs.getString(KEY_DEVICE_ID, null)
        if (!cached.isNullOrBlank()) {
            return cached
        }

        val installationId = FirebaseInstallations.getInstance().id.awaitTask()
        val androidId = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty()
        val deviceId = buildString {
            if (androidId.isNotBlank()) {
                append("aid:")
                append(hashSegment(androidId))
                append(":")
            }
            append("fis:")
            append(hashSegment(installationId))
        }
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        return deviceId
    }

    private fun hashSegment(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(24)
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id_v1"
    }
}
