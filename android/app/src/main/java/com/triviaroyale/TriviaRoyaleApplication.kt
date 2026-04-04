package com.triviaroyale

import android.app.Application
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.triviaroyale.data.DynamicQuizRepository

/**
 * Custom Application class that initializes the AdMob SDK as early as possible.
 *
 * Calling MobileAds.initialize() here (in Application.onCreate, before any Activity
 * starts) ensures that the SDK has time to fetch ad settings before the first ad
 * request at the 3-minute gameplay mark. This prevents the "Not retrying to fetch
 * app settings" race condition seen in Logcat.
 *
 * QuizAdsBootstrap guards against double-initialization, so the call inside
 * QuizInterstitialManager / QuizRewardedAdManager is a safe no-op after this.
 */
class TriviaRoyaleApplication : Application() {

    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        // Initialize AdMob SDK at process start. The completion callback is
        // intentionally left empty — QuizAdsBootstrap tracks its own state.
        MobileAds.initialize(this) {}

        // Initialize dynamic quiz repository (loads cached questions from disk)
        DynamicQuizRepository.initialize(this)
    }
}
