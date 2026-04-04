package com.triviaroyale.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.triviaroyale.BuildConfig
import java.lang.ref.WeakReference

enum class RewardedSkipWaitResult {
    EARNED,
    DISMISSED,
    NOT_READY,
    FAILED,
}

class QuizRewardedAdManager(context: Context) {
    private val appContext = context.applicationContext

    private var isLoading = false
    private var rewardedAd: RewardedAd? = null
    private var rewardedLoadedAtElapsedRealtime: Long = 0L
    private var pendingShowRequest: PendingShowRequest? = null

    // Exponential backoff for failed loads
    private var consecutiveFailures = 0
    private var lastFailedAtElapsedRealtime: Long = 0L

    private data class PendingShowRequest(
        val activityRef: WeakReference<Activity>,
        val onResult: (RewardedSkipWaitResult) -> Unit,
    )

    fun preloadIfNeeded() {
        if (!isEnabled()) {
            return
        }
        QuizAdsBootstrap.initialize(appContext) {
            preloadIfNeededInternal()
        }
    }

    private fun preloadIfNeededInternal() {
        if (isLoading) {
            return
        }
        if (rewardedAd != null && !isRewardedExpired()) {
            return
        }

        // Exponential backoff: don't retry too quickly after failures
        if (consecutiveFailures > 0) {
            val backoffMs = backoffDelayMs(consecutiveFailures)
            val elapsed = SystemClock.elapsedRealtime() - lastFailedAtElapsedRealtime
            if (elapsed < backoffMs) {
                android.util.Log.d("AdDebug", "⏳ Rewarded backoff: ${elapsed}ms / ${backoffMs}ms (failures=$consecutiveFailures)")
                return
            }
        }

        isLoading = true
        RewardedAd.load(
            appContext,
            rewardedAdUnitId(),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loadedAd: RewardedAd) {
                    isLoading = false
                    rewardedAd = loadedAd
                    rewardedLoadedAtElapsedRealtime = SystemClock.elapsedRealtime()
                    consecutiveFailures = 0  // Reset backoff on success
                    flushPendingShowRequest(loadedAd)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoading = false
                    rewardedAd = null
                    consecutiveFailures++
                    lastFailedAtElapsedRealtime = SystemClock.elapsedRealtime()
                    android.util.Log.e("AdDebug", "❌ Rewarded FAILED: code=${loadAdError.code}, msg=${loadAdError.message}, nextBackoff=${backoffDelayMs(consecutiveFailures)}ms")
                    pendingShowRequest?.let { pending ->
                        pendingShowRequest = null
                        pending.onResult(RewardedSkipWaitResult.FAILED)
                    }
                }
            }
        )
    }

    fun showSkipWaitReward(
        activity: Activity?,
        onResult: (RewardedSkipWaitResult) -> Unit,
    ) {
        if (!isEnabled()) {
            onResult(RewardedSkipWaitResult.NOT_READY)
            return
        }

        if (activity == null) {
            onResult(RewardedSkipWaitResult.NOT_READY)
            return
        }

        val readyAd = rewardedAd?.takeIf { !isRewardedExpired() }
        if (readyAd == null) {
            pendingShowRequest?.let { pending ->
                pendingShowRequest = null
                pending.onResult(RewardedSkipWaitResult.NOT_READY)
            }
            pendingShowRequest = PendingShowRequest(
                activityRef = WeakReference(activity),
                onResult = onResult,
            )
            preloadIfNeeded()
            return
        }

        showRewardedAd(activity, readyAd, onResult)
    }

    private fun flushPendingShowRequest(loadedAd: RewardedAd) {
        val pending = pendingShowRequest ?: return
        pendingShowRequest = null
        val activity = pending.activityRef.get()
        if (activity == null) {
            pending.onResult(RewardedSkipWaitResult.NOT_READY)
            return
        }
        showRewardedAd(activity, loadedAd, pending.onResult)
    }

    private fun showRewardedAd(
        activity: Activity,
        ad: RewardedAd,
        onResult: (RewardedSkipWaitResult) -> Unit,
    ) {
        rewardedAd = null
        var earnedReward = false
        var callbackSent = false

        fun finish(result: RewardedSkipWaitResult) {
            if (!callbackSent) {
                callbackSent = true
                onResult(result)
            }
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preloadIfNeeded()
                finish(if (earnedReward) RewardedSkipWaitResult.EARNED else RewardedSkipWaitResult.DISMISSED)
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                preloadIfNeeded()
                finish(RewardedSkipWaitResult.FAILED)
            }
        }

        ad.show(activity) { rewardItem: RewardItem ->
            earnedReward = rewardItem.amount > 0
        }
    }

    private fun isEnabled(): Boolean {
        return BuildConfig.DEBUG || BuildConfig.ADMOB_REWARDED_AD_UNIT_ID.isNotBlank()
    }

    private fun rewardedAdUnitId(): String {
        return if (BuildConfig.ADMOB_REWARDED_AD_UNIT_ID.isNotBlank()) {
            BuildConfig.ADMOB_REWARDED_AD_UNIT_ID
        } else {
            SAMPLE_REWARDED_AD_UNIT_ID
        }
    }

    private fun isRewardedExpired(): Boolean {
        return rewardedAd == null ||
            SystemClock.elapsedRealtime() - rewardedLoadedAtElapsedRealtime > REWARDED_EXPIRY_MILLIS
    }

    companion object {
        private const val SAMPLE_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val REWARDED_EXPIRY_MILLIS = 55L * 60L * 1000L

        /** Exponential backoff: 5s, 15s, 45s, 120s cap */
        private fun backoffDelayMs(failures: Int): Long {
            val delaySeconds = minOf(120L, 5L * (1L shl minOf(failures - 1, 4).coerceAtLeast(0)))
            return delaySeconds * 1000L
        }
    }
}
