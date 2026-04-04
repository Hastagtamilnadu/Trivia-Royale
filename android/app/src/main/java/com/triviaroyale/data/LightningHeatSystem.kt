package com.triviaroyale.data

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Dynamic daily heat system for Lightning Round.
 *
 * Each day gets a deterministic heat level (0.0 → 1.0) derived from
 * the IST date key. The heat affects:
 *   • Timer duration: 150s (cool, heat≈0) → 30s (inferno, heat≈1)
 *   • Visual colour: blue → cyan → yellow → orange → red
 *   • Label: Cool → Warm → Hot → Blazing → Inferno
 */
object LightningHeatSystem {

    // Timer bounds
    private const val MIN_TIMER_SECONDS = 30   // at heat = 1.0 (Inferno)
    private const val MAX_TIMER_SECONDS = 150  // at heat = 0.0 (Cool)

    // ── Daily heat level (deterministic from IST date) ──────────────────────

    fun getDailyHeatLevel(): Float {
        val dayKey = currentIstDateKey()
        // Use a deterministic hash of the date string to produce 0..1
        val hash = dayKey.hashCode().toLong() and 0xFFFFFFFFL // unsigned
        return (hash % 1000) / 999f
    }

    // ── Timer seconds based on heat ─────────────────────────────────────────

    fun getTimerForHeat(heat: Float = getDailyHeatLevel()): Int {
        // Linear interpolation: high heat = less time
        val clamped = heat.coerceIn(0f, 1f)
        return (MAX_TIMER_SECONDS - (clamped * (MAX_TIMER_SECONDS - MIN_TIMER_SECONDS))).toInt()
    }

    // ── Heat label ──────────────────────────────────────────────────────────

    fun getHeatLabel(heat: Float = getDailyHeatLevel()): String {
        return when {
            heat >= 0.85f -> "Inferno"
            heat >= 0.65f -> "Blazing"
            heat >= 0.45f -> "Hot"
            heat >= 0.25f -> "Warm"
            else -> "Cool"
        }
    }

    // ── Heat colour (for progress bar + accent) ─────────────────────────────

    fun getHeatColor(heat: Float = getDailyHeatLevel()): Color {
        return when {
            heat >= 0.85f -> Color(0xFFEF4444) // Red
            heat >= 0.65f -> Color(0xFFF97316) // Orange
            heat >= 0.45f -> Color(0xFFEAB308) // Yellow / Amber
            heat >= 0.25f -> Color(0xFF22D3EE) // Cyan
            else -> Color(0xFF3B82F6)          // Blue
        }
    }

    // ── Description for intro screen ────────────────────────────────────────

    fun getIntroDescription(heat: Float = getDailyHeatLevel()): String {
        val timer = getTimerForHeat(heat)
        val minutes = timer / 60
        val seconds = timer % 60
        val timeStr = if (minutes > 0 && seconds > 0) {
            "${minutes}m ${seconds}s"
        } else if (minutes > 0) {
            "${minutes}m"
        } else {
            "${seconds}s"
        }
        return "$timeStr · 30 questions · +2s correct · -2s wrong"
    }

    fun getHomeSubtitle(heat: Float = getDailyHeatLevel()): String {
        val timer = getTimerForHeat(heat)
        val label = getHeatLabel(heat)
        return when {
            heat >= 0.65f -> "$timer sec. Pure chaos. 🔥"
            heat >= 0.45f -> "$timer sec. Stay sharp. ⚡"
            heat >= 0.25f -> "$timer sec. A decent warm-up. 🌤"
            else -> "$timer sec. Take it easy. ❄\uFE0F"
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun currentIstDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }.format(Date())
    }
}
