package com.triviaroyale.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.triviaroyale.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "AdDebug"

class QuizInterstitialManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var isLoading = false
    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoadedAtElapsedRealtime: Long = 0L

    // Exponential backoff for failed loads
    private var consecutiveFailures = 0
    private var lastFailedAtElapsedRealtime: Long = 0L

    // ── Ad Loading ────────────────────────────────────────────────

    fun preloadIfNeeded() {
        if (!isEnabled()) return
        QuizAdsBootstrap.initialize(appContext) {
            preloadIfNeededInternal()
        }
    }

    private fun preloadIfNeededInternal() {
        if (isLoading) return
        if (interstitialAd != null && !isInterstitialExpired()) return

        // Exponential backoff: don't retry too quickly after failures
        if (consecutiveFailures > 0) {
            val backoffMs = backoffDelayMs(consecutiveFailures)
            val elapsed = SystemClock.elapsedRealtime() - lastFailedAtElapsedRealtime
            if (elapsed < backoffMs) {
                // Log backoff only every 10s to avoid spam
                if (elapsed % 10000 < 1100) {
                    Log.d(TAG, "⏳ Interstitial backoff: ${elapsed}ms / ${backoffMs}ms (failures=$consecutiveFailures)")
                }
                return
            }
        }

        val adUnitId = interstitialAdUnitId()
        Log.d(TAG, "preload: loading ad with unit=$adUnitId")
        isLoading = true
        InterstitialAd.load(
            appContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loadedAd: InterstitialAd) {
                    isLoading = false
                    interstitialAd = loadedAd
                    interstitialLoadedAtElapsedRealtime = SystemClock.elapsedRealtime()
                    consecutiveFailures = 0  // Reset backoff on success
                    Log.d(TAG, "✅ Ad LOADED successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                    consecutiveFailures++
                    lastFailedAtElapsedRealtime = SystemClock.elapsedRealtime()
                    Log.e(TAG, "❌ Ad FAILED to load: code=${loadAdError.code}, msg=${loadAdError.message}, nextBackoff=${backoffDelayMs(consecutiveFailures)}ms")
                }
            }
        )
    }

    // ── Progress Tracking ─────────────────────────────────────────

    /**
     * Called every active quiz second to accumulate play time.
     * Also triggers preload at the 3-minute mark.
     */
    fun recordActiveQuizSeconds(seconds: Int = 1) {
        if (!isEnabled()) return
        val current = readProgress()
        val next = advanceQuizInterstitialProgress(current, seconds)
        writeProgress(next)

        if (next.activeQuizSeconds % 30 == 0) {
            Log.d(TAG, "⏱ activeQuizSeconds=${next.activeQuizSeconds}, matches=${next.matchesCompletedSinceLastAd}, pending=${next.pendingInterstitial}, shouldPreload=${next.shouldPreload}")
        }

        if (next.shouldPreload) {
            preloadIfNeeded()
        }
    }

    /**
     * Called once when a quiz match is completed (regardless of outcome).
     * Increments match counters for both the interstitial gate and the
     * unified "1-clean-match-after-interruption" rule.
     */
    fun recordMatchCompleted() {
        if (!isEnabled()) return
        val current = readProgress()
        val newMatchCount = current.matchesCompletedSinceLastAd + 1
        writeProgress(current.copy(
            matchesCompletedSinceLastAd = newMatchCount
        ))
        // Unified interruption counter
        val newInterruptionCount = prefs.getInt(KEY_MATCHES_SINCE_INTERRUPTION, 1) + 1
        prefs.edit().putInt(KEY_MATCHES_SINCE_INTERRUPTION, newInterruptionCount).apply()
        Log.d(TAG, "🏁 Match completed: totalMatches=$newMatchCount, matchesSinceInterruption=$newInterruptionCount, activeSeconds=${current.activeQuizSeconds}, pendingAd=${current.pendingInterstitial}")
    }

    // ── Show ──────────────────────────────────────────────────────

    /**
     * Called after a quiz is completed. Shows an interstitial only if
     * all 3 gates are satisfied: 4 min playtime, 2+ matches, 90s gap.
     */
    suspend fun maybeShowAfterCompletedQuiz(activity: Activity?, onComplete: () -> Unit) {
        val progress = readProgress()
        val now = System.currentTimeMillis()

        // Gate 4: at least 1 clean match since any interruption (ad or break)
        val matchesSinceInterruption = prefs.getInt(KEY_MATCHES_SINCE_INTERRUPTION, 1)
        val shouldShow = shouldShowInterstitial(progress, now)
        val enabled = isEnabled()

        Log.d(TAG, "🔍 maybeShow: enabled=$enabled, shouldShow=$shouldShow, matchesSinceInterruption=$matchesSinceInterruption")
        Log.d(TAG, "   gates → pending=${progress.pendingInterstitial}, matches=${progress.matchesCompletedSinceLastAd}>=2?, seconds=${progress.activeQuizSeconds}>=240?, gap=${if (progress.lastAdShownAtMillis > 0) (now - progress.lastAdShownAtMillis) else "never"}ms")
        Log.d(TAG, "   adReady=${interstitialAd != null}, isLoading=$isLoading, expired=${isInterstitialExpired()}")

        if (!enabled || !shouldShow || matchesSinceInterruption < 1) {
            if (!enabled) Log.d(TAG, "❌ BLOCKED: not enabled")
            if (!shouldShow) Log.d(TAG, "❌ BLOCKED: gates not satisfied")
            if (matchesSinceInterruption < 1) Log.d(TAG, "❌ BLOCKED: matchesSinceInterruption=$matchesSinceInterruption (need >=1)")
            preloadIfNeeded()
            onComplete()
            return
        }
        Log.d(TAG, "✅ All gates PASSED — attempting to show ad")

        // If the ad is still loading (quiz ended right at the 4-min threshold),
        // wait up to 3 seconds for it to become ready before giving up.
        if (interstitialAd == null && isLoading) {
            withContext(Dispatchers.IO) {
                val deadlineMs = SystemClock.elapsedRealtime() + 3_000L
                while (interstitialAd == null && SystemClock.elapsedRealtime() < deadlineMs) {
                    delay(200)
                }
            }
        }

        val readyAd = interstitialAd?.takeIf { !isInterstitialExpired() }
        if (activity == null || readyAd == null) {
            Log.w(TAG, "❌ Ad not ready to show: activity=${activity != null}, readyAd=${readyAd != null}")
            preloadIfNeeded()
            onComplete()
            return
        }
        Log.d(TAG, "📺 SHOWING ad now!")

        interstitialAd = null
        var continued = false
        fun continueOnce() {
            if (!continued) {
                continued = true
                onComplete()
            }
        }

        readyAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                // Ad shown — reset all counters and record timestamp
                clearProgress()
            }

            override fun onAdDismissedFullScreenContent() {
                preloadIfNeeded()
                continueOnce()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                preloadIfNeeded()
                continueOnce()
            }
        }

        readyAd.show(activity)
    }

    // ── SharedPreferences ─────────────────────────────────────────

    private fun readProgress(): QuizInterstitialProgress {
        return QuizInterstitialProgress(
            activeQuizSeconds = prefs.getInt(KEY_ACTIVE_QUIZ_SECONDS, 0),
            matchesCompletedSinceLastAd = prefs.getInt(KEY_MATCHES_COMPLETED, 0),
            lastAdShownAtMillis = prefs.getLong(KEY_LAST_AD_SHOWN_AT, 0L),
            pendingInterstitial = prefs.getBoolean(KEY_PENDING_INTERSTITIAL, false),
            shouldPreload = prefs.getBoolean(KEY_SHOULD_PRELOAD, false)
        )
    }

    private fun writeProgress(p: QuizInterstitialProgress) {
        prefs.edit()
            .putInt(KEY_ACTIVE_QUIZ_SECONDS, p.activeQuizSeconds)
            .putInt(KEY_MATCHES_COMPLETED, p.matchesCompletedSinceLastAd)
            .putLong(KEY_LAST_AD_SHOWN_AT, p.lastAdShownAtMillis)
            .putBoolean(KEY_PENDING_INTERSTITIAL, p.pendingInterstitial)
            .putBoolean(KEY_SHOULD_PRELOAD, p.shouldPreload)
            .apply()
    }

    /**
     * Resets play time + match counter; records the time the ad was shown
     * so the 90-second gap guard works correctly.
     */
    private fun clearProgress() {
        prefs.edit()
            .putInt(KEY_ACTIVE_QUIZ_SECONDS, 0)
            .putInt(KEY_MATCHES_COMPLETED, 0)
            .putLong(KEY_LAST_AD_SHOWN_AT, System.currentTimeMillis())
            .putBoolean(KEY_PENDING_INTERSTITIAL, false)
            .putBoolean(KEY_SHOULD_PRELOAD, false)
            .putInt(KEY_MATCHES_SINCE_INTERRUPTION, 0) // reset: next match is clean
            .apply()
    }

    /**
     * Called by the break system when a break is triggered.
     * Resets the unified interruption counter so the next match is clean.
     */
    fun recordInterruptionForBreak() {
        prefs.edit().putInt(KEY_MATCHES_SINCE_INTERRUPTION, 0).apply()
    }

    /**
     * Returns how many matches have been played since the last interruption.
     * Used by the break system to enforce the 1-clean-match rule.
     */
    fun getMatchesSinceLastInterruption(): Int {
        return prefs.getInt(KEY_MATCHES_SINCE_INTERRUPTION, 1)
    }

    // ── Helpers ───────────────────────────────────────────────────

    private fun isEnabled(): Boolean {
        return BuildConfig.DEBUG || BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID.isNotBlank()
    }

    private fun interstitialAdUnitId(): String {
        return if (BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID.isNotBlank()) {
            BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID
        } else {
            SAMPLE_INTERSTITIAL_AD_UNIT_ID
        }
    }

    private fun isInterstitialExpired(): Boolean {
        return interstitialAd == null ||
            SystemClock.elapsedRealtime() - interstitialLoadedAtElapsedRealtime > INTERSTITIAL_EXPIRY_MILLIS
    }

    /**
     * Returns when the last interstitial ad was shown (epoch millis), or 0 if never.
     * Used by the break system to avoid ad → break collision.
     */
    fun getLastAdShownAtMillis(): Long = prefs.getLong(KEY_LAST_AD_SHOWN_AT, 0L)

    companion object {
        private const val PREFS_NAME = "quiz_interstitial_ads"
        private const val KEY_ACTIVE_QUIZ_SECONDS = "active_quiz_seconds"
        private const val KEY_MATCHES_COMPLETED = "matches_completed_since_last_ad"
        private const val KEY_LAST_AD_SHOWN_AT = "last_ad_shown_at_millis"
        private const val KEY_PENDING_INTERSTITIAL = "pending_interstitial"
        private const val KEY_SHOULD_PRELOAD = "should_preload"
        private const val KEY_MATCHES_SINCE_INTERRUPTION = "matches_since_last_interruption"
        private const val SAMPLE_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val INTERSTITIAL_EXPIRY_MILLIS = 55L * 60L * 1000L

        /** Exponential backoff: 5s, 15s, 45s, 120s cap */
        private fun backoffDelayMs(failures: Int): Long {
            val delaySeconds = minOf(120L, 5L * (1L shl minOf(failures - 1, 4).coerceAtLeast(0)))
            return delaySeconds * 1000L
        }
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
