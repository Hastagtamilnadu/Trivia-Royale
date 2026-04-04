package com.triviaroyale.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.triviaroyale.BuildConfig

object FirebaseBootstrap {
    private var appCheckInstalled = false

    private data class ConfigValue(
        val label: String,
        val value: String
    )

    private fun requiredValues(): List<ConfigValue> = listOf(
        ConfigValue("firebaseApiKey", BuildConfig.FIREBASE_API_KEY),
        ConfigValue("firebaseAppId", BuildConfig.FIREBASE_APP_ID),
        ConfigValue("firebaseProjectId", BuildConfig.FIREBASE_PROJECT_ID),
        ConfigValue("firebaseSenderId", BuildConfig.FIREBASE_SENDER_ID)
    )

    fun isConfigured(): Boolean = requiredValues().all { it.value.isNotBlank() }

    fun missingKeys(): List<String> = requiredValues()
        .filter { it.value.isBlank() }
        .map { it.label }

    fun isGoogleSignInConfigured(): Boolean = BuildConfig.FIREBASE_WEB_CLIENT_ID.isNotBlank()

    /** Returns true when running a debug build. */
    fun isDebugBuild(): Boolean = BuildConfig.DEBUG

    /** Returns true when App Check has been installed (both debug and release). */
    fun isAppCheckActive(): Boolean = appCheckInstalled

    fun ensureInitialized(context: Context): Boolean {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            installAppCheckIfNeeded()
            return true
        }
        if (!isConfigured()) {
            return false
        }

        val options = FirebaseOptions.Builder()
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
            .apply {
                if (BuildConfig.FIREBASE_STORAGE_BUCKET.isNotBlank()) {
                    setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
                }
            }
            .build()

        FirebaseApp.initializeApp(context, options)
        installAppCheckIfNeeded()
        return FirebaseApp.getApps(context).isNotEmpty()
    }

    private fun installAppCheckIfNeeded() {
        if (appCheckInstalled) {
            return
        }
        val appCheck = FirebaseAppCheck.getInstance()
        if (isDebugBuild()) {
            // Debug builds use DebugAppCheckProviderFactory.
            // On first run, logcat will print a debug token — register it in
            // Firebase Console → App Check → Manage debug tokens.
            try {
                val debugFactory = Class.forName(
                    "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory"
                )
                val getInstance = debugFactory.getMethod("getInstance")
                val factory = getInstance.invoke(null)
                    as com.google.firebase.appcheck.AppCheckProviderFactory
                appCheck.installAppCheckProviderFactory(factory)
                Log.d("FirebaseBootstrap", "Debug build: App Check installed with DEBUG token provider")
            } catch (e: Exception) {
                Log.w("FirebaseBootstrap", "Debug App Check provider not available, skipping", e)
                return
            }
        } else {
            // Release builds use Play Integrity for real device attestation.
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            Log.d("FirebaseBootstrap", "Release build: App Check installed with Play Integrity")
        }
        appCheckInstalled = true
    }
}
