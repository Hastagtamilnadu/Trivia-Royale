package com.triviaroyale.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.triviaroyale.BuildConfig

internal object QuizAdsBootstrap {
    private var initialized = false
    private var initializationStarted = false
    private val pendingCallbacks = mutableListOf<() -> Unit>()

    fun initialize(context: Context, onInitialized: () -> Unit) {
        var shouldStartInitialization = false
        synchronized(this) {
            if (initialized) {
                onInitialized()
                return
            }
            pendingCallbacks += onInitialized
            if (!initializationStarted) {
                initializationStarted = true
                shouldStartInitialization = true
            }
        }

        if (!shouldStartInitialization) {
            return
        }

        // Register test devices so real ad unit IDs serve test ads during development.
        // This prevents accidental policy violations (tapping your own real ads = ban).
        if (BuildConfig.DEBUG) {
            val testDeviceIds = listOf(
                "YOUR_TEST_DEVICE_ID"  // Add your test device ID here
            )
            val requestConfig = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(requestConfig)
        }

        MobileAds.initialize(context.applicationContext) {
            val callbacks = synchronized(this) {
                initialized = true
                pendingCallbacks.toList().also { pendingCallbacks.clear() }
            }
            callbacks.forEach { callback -> callback() }
        }
    }
}
