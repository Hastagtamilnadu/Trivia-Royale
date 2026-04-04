package com.triviaroyale.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizInterstitialPolicyTest {
    @Test
    fun `active quiz time below threshold does not trigger interstitial`() {
        val result = advanceQuizInterstitialProgress(
            QuizInterstitialProgress(activeQuizSeconds = 120, pendingInterstitial = false),
            60
        )

        assertEquals(180, result.activeQuizSeconds)
        assertFalse(result.pendingInterstitial)
    }

    @Test
    fun `active quiz time reaches threshold and triggers pending interstitial`() {
        val result = advanceQuizInterstitialProgress(
            QuizInterstitialProgress(activeQuizSeconds = 200, pendingInterstitial = false),
            40
        )

        assertEquals(ACTIVE_QUIZ_SECONDS_PER_INTERSTITIAL, result.activeQuizSeconds)
        assertTrue(result.pendingInterstitial)
    }

    @Test
    fun `pending interstitial state does not keep increasing`() {
        val result = advanceQuizInterstitialProgress(
            QuizInterstitialProgress(
                activeQuizSeconds = ACTIVE_QUIZ_SECONDS_PER_INTERSTITIAL,
                pendingInterstitial = true
            ),
            30
        )

        assertEquals(ACTIVE_QUIZ_SECONDS_PER_INTERSTITIAL, result.activeQuizSeconds)
        assertTrue(result.pendingInterstitial)
    }
}
