package com.triviaroyale.ads

// ── System 1: Interstitial Ad Policy ──────────────────────────────────────
//
//  Three gates must ALL be satisfied before an interstitial is shown:
//    1. ≥ 4 minutes of cumulative active quiz time
//    2. ≥ 2 completed matches since the last ad
//    3. ≥ 90 seconds since the last ad was shown
//
//  This prevents:
//    • Ad on the very first quiz
//    • Back-to-back ads
//    • Ad every match
// ──────────────────────────────────────────────────────────────────────────

const val ACTIVE_QUIZ_SECONDS_PER_INTERSTITIAL = 4 * 60   // 4 min play time gate
const val ACTIVE_QUIZ_SECONDS_PRELOAD_TRIGGER  = 3 * 60   // preload 1 min early
const val MIN_MATCHES_PER_INTERSTITIAL         = 2         // at least 2 quizzes completed
const val MIN_GAP_BETWEEN_ADS_MILLIS           = 90_000L   // 90 seconds between ads

data class QuizInterstitialProgress(
    val activeQuizSeconds: Int = 0,
    val matchesCompletedSinceLastAd: Int = 0,
    val lastAdShownAtMillis: Long = 0L,
    val pendingInterstitial: Boolean = false,
    val shouldPreload: Boolean = false
)

/**
 * Advance the play-time counter. Called every active quiz second.
 * Only touches time + preload/pending flags; match count is updated separately.
 */
fun advanceQuizInterstitialProgress(
    current: QuizInterstitialProgress,
    additionalSeconds: Int
): QuizInterstitialProgress {
    if (additionalSeconds <= 0) return current
    if (current.pendingInterstitial) return current

    val nextActiveSeconds = (current.activeQuizSeconds + additionalSeconds)
        .coerceAtMost(ACTIVE_QUIZ_SECONDS_PER_INTERSTITIAL)
    val hitPreload = nextActiveSeconds >= ACTIVE_QUIZ_SECONDS_PRELOAD_TRIGGER
    val hitInterstitial = nextActiveSeconds >= ACTIVE_QUIZ_SECONDS_PER_INTERSTITIAL
    return current.copy(
        activeQuizSeconds = nextActiveSeconds,
        pendingInterstitial = hitInterstitial,
        shouldPreload = hitPreload || hitInterstitial
    )
}

/**
 * Check whether all 3 gates are satisfied to show an interstitial.
 */
fun shouldShowInterstitial(
    progress: QuizInterstitialProgress,
    nowMillis: Long
): Boolean {
    // Gate 1: play time
    if (!progress.pendingInterstitial) return false
    // Gate 2: match count
    if (progress.matchesCompletedSinceLastAd < MIN_MATCHES_PER_INTERSTITIAL) return false
    // Gate 3: gap since last ad (skip check if no ad has ever been shown)
    if (progress.lastAdShownAtMillis > 0L) {
        val gap = nowMillis - progress.lastAdShownAtMillis
        if (gap < MIN_GAP_BETWEEN_ADS_MILLIS) return false
    }
    return true
}
