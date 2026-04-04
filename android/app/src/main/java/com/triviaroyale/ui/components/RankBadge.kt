package com.triviaroyale.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium game-style rank badge icons drawn with Canvas.
 * Each rank has a unique shape with gradients and glow effects.
 */

// ═══════════════════════════════════════
// BRONZE — Hexagonal Shield
// ═══════════════════════════════════════
@Composable
fun BronzeRankIcon(size: Dp = 28.dp) {
    val c1 = Color(0xFFCD7F32)
    val c2 = Color(0xFFB8690E)
    val glow = Color(0xFFCD7F32)
    Canvas(modifier = Modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension * 0.42f

        // Glow
        drawCircle(glow.copy(alpha = 0.25f), radius = r * 1.3f, center = Offset(cx, cy))

        // Hexagon shield
        val hex = hexPath(cx, cy, r, 6)
        drawPath(hex, Brush.linearGradient(listOf(c1, c2)), style = Fill)
        drawPath(hex, Color.White.copy(alpha = 0.3f), style = Stroke(width = 1.5f))

        // Inner diamond
        val ir = r * 0.35f
        val diamond = hexPath(cx, cy, ir, 4)
        drawPath(diamond, Color.White.copy(alpha = 0.4f), style = Fill)
    }
}

// ═══════════════════════════════════════
// SILVER — Layered Tree / Chevron
// ═══════════════════════════════════════
@Composable
fun SilverRankIcon(size: Dp = 28.dp) {
    val c1 = Color(0xFFC0C0C0)
    val c2 = Color(0xFF8E8E8E)
    val glow = Color(0xFFC0C0C0)
    Canvas(modifier = Modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension * 0.42f

        // Glow
        drawCircle(glow.copy(alpha = 0.2f), radius = r * 1.3f, center = Offset(cx, cy))

        // Tree shape: 3 stacked triangles
        val triH = r * 0.5f
        for (i in 0..2) {
            val topY = cy - r + i * triH * 0.55f
            val spread = r * (0.55f + i * 0.18f)
            val tri = Path().apply {
                moveTo(cx, topY)
                lineTo(cx - spread, topY + triH)
                lineTo(cx + spread, topY + triH)
                close()
            }
            drawPath(tri, Brush.linearGradient(listOf(c1, c2)), style = Fill)
            drawPath(tri, Color.White.copy(alpha = 0.25f), style = Stroke(width = 1f))
        }
        // Trunk
        drawRect(
            Brush.linearGradient(listOf(c2, Color(0xFF6B6B6B))),
            topLeft = Offset(cx - r * 0.12f, cy + r * 0.5f),
            size = Size(r * 0.24f, r * 0.4f)
        )
    }
}

// ═══════════════════════════════════════
// GOLD — Crown with Jewels
// ═══════════════════════════════════════
@Composable
fun GoldRankIcon(size: Dp = 28.dp) {
    val c1 = Color(0xFFFFD700)
    val c2 = Color(0xFFDAA520)
    val glow = Color(0xFFFFD700)
    Canvas(modifier = Modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension * 0.42f

        // Glow
        drawCircle(glow.copy(alpha = 0.3f), radius = r * 1.3f, center = Offset(cx, cy))

        // Crown body
        val crown = Path().apply {
            moveTo(cx - r, cy + r * 0.3f)
            lineTo(cx - r, cy - r * 0.2f)
            lineTo(cx - r * 0.5f, cy + r * 0.1f)
            lineTo(cx, cy - r * 0.7f)
            lineTo(cx + r * 0.5f, cy + r * 0.1f)
            lineTo(cx + r, cy - r * 0.2f)
            lineTo(cx + r, cy + r * 0.3f)
            close()
        }
        drawPath(crown, Brush.linearGradient(listOf(c1, c2)), style = Fill)
        drawPath(crown, Color.White.copy(alpha = 0.3f), style = Stroke(width = 1.5f))

        // Base band
        drawRect(
            Brush.linearGradient(listOf(c2, Color(0xFFB8860B))),
            topLeft = Offset(cx - r, cy + r * 0.3f),
            size = Size(r * 2f, r * 0.25f)
        )

        // Jewels on crown tips
        val jewels = listOf(
            Offset(cx - r, cy - r * 0.2f) to Color(0xFFFF4444),
            Offset(cx, cy - r * 0.7f) to Color(0xFF44FF44),
            Offset(cx + r, cy - r * 0.2f) to Color(0xFF4444FF)
        )
        jewels.forEach { (pos, color) ->
            drawCircle(color, radius = r * 0.12f, center = pos)
            drawCircle(Color.White.copy(alpha = 0.5f), radius = r * 0.06f, center = Offset(pos.x - r * 0.03f, pos.y - r * 0.03f))
        }
    }
}

// ═══════════════════════════════════════
// PLATINUM — 5-pointed Star with shine
// ═══════════════════════════════════════
@Composable
fun PlatinumRankIcon(size: Dp = 28.dp) {
    val c1 = Color(0xFFE5E4E2)
    val c2 = Color(0xFF9E9E9E)
    val glow = Color(0xFFE5E4E2)
    Canvas(modifier = Modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension * 0.44f

        // Glow
        drawCircle(glow.copy(alpha = 0.25f), radius = r * 1.35f, center = Offset(cx, cy))

        // Star
        val star = starPath(cx, cy, r, r * 0.45f, 5)
        drawPath(star, Brush.linearGradient(listOf(c1, c2, c1)), style = Fill)
        drawPath(star, Color.White.copy(alpha = 0.4f), style = Stroke(width = 1.5f))

        // Inner circle
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)), radius = r * 0.3f, center = Offset(cx, cy))
    }
}

// ═══════════════════════════════════════
// DIAMOND — Faceted gem shape
// ═══════════════════════════════════════
@Composable
fun DiamondRankIcon(size: Dp = 28.dp) {
    val c1 = Color(0xFF00E5FF)
    val c2 = Color(0xFF0097A7)
    val c3 = Color(0xFF00BCD4)
    val glow = Color(0xFF00E5FF)
    Canvas(modifier = Modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension * 0.42f

        // Glow
        drawCircle(glow.copy(alpha = 0.3f), radius = r * 1.4f, center = Offset(cx, cy))

        // Top facet
        val top = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r, cy - r * 0.1f)
            lineTo(cx, cy + r)
            close()
        }
        drawPath(top, Brush.linearGradient(listOf(c1, c3)), style = Fill)

        // Left facet
        val left = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx - r, cy - r * 0.1f)
            lineTo(cx, cy + r)
            close()
        }
        drawPath(left, Brush.linearGradient(listOf(c2, c3)), style = Fill)

        // Top band
        val band = Path().apply {
            moveTo(cx - r, cy - r * 0.1f)
            lineTo(cx, cy - r)
            lineTo(cx + r, cy - r * 0.1f)
            lineTo(cx, cy * 0.1f + cy - r * 0.1f)
            close()
        }
        drawPath(band, Color.White.copy(alpha = 0.15f), style = Fill)

        // Outline
        val outline = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r, cy - r * 0.1f)
            lineTo(cx, cy + r)
            lineTo(cx - r, cy - r * 0.1f)
            close()
        }
        drawPath(outline, Color.White.copy(alpha = 0.4f), style = Stroke(width = 1.5f))

        // Sparkle highlight
        drawCircle(Color.White.copy(alpha = 0.6f), radius = r * 0.08f, center = Offset(cx - r * 0.25f, cy - r * 0.4f))
    }
}

// ═══════════════════════════════════════
// MASTER — Flame emblem
// ═══════════════════════════════════════
@Composable
fun MasterRankIcon(size: Dp = 28.dp) {
    val c1 = Color(0xFFFF6D00)
    val c2 = Color(0xFFFF1744)
    val c3 = Color(0xFFFFAB00)
    val glow = Color(0xFFFF6D00)
    Canvas(modifier = Modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension * 0.42f

        // Glow
        drawCircle(glow.copy(alpha = 0.3f), radius = r * 1.4f, center = Offset(cx, cy))

        // Outer flame
        val flame = Path().apply {
            moveTo(cx, cy - r * 1.1f)
            cubicTo(cx + r * 0.6f, cy - r * 0.5f, cx + r * 1.0f, cy + r * 0.2f, cx + r * 0.5f, cy + r * 0.9f)
            quadraticBezierTo(cx, cy + r * 1.1f, cx - r * 0.5f, cy + r * 0.9f)
            cubicTo(cx - r * 1.0f, cy + r * 0.2f, cx - r * 0.6f, cy - r * 0.5f, cx, cy - r * 1.1f)
            close()
        }
        drawPath(flame, Brush.linearGradient(listOf(c3, c1, c2)), style = Fill)

        // Inner flame
        val inner = Path().apply {
            moveTo(cx, cy - r * 0.4f)
            cubicTo(cx + r * 0.3f, cy, cx + r * 0.4f, cy + r * 0.4f, cx + r * 0.2f, cy + r * 0.7f)
            quadraticBezierTo(cx, cy + r * 0.85f, cx - r * 0.2f, cy + r * 0.7f)
            cubicTo(cx - r * 0.4f, cy + r * 0.4f, cx - r * 0.3f, cy, cx, cy - r * 0.4f)
            close()
        }
        drawPath(inner, Brush.linearGradient(listOf(Color(0xFFFFFF00), c3)), style = Fill)
    }
}

// ═══════════════════════════════════════
// GRANDMASTER — Radiant starburst
// ═══════════════════════════════════════
@Composable
fun GrandmasterRankIcon(size: Dp = 28.dp) {
    val c1 = Color(0xFFFF4081)
    val c2 = Color(0xFFE040FB)
    val c3 = Color(0xFFFFAB40)
    val glow = Color(0xFFFF4081)
    Canvas(modifier = Modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension * 0.44f

        // Outer glow
        drawCircle(glow.copy(alpha = 0.35f), radius = r * 1.5f, center = Offset(cx, cy))

        // 8-point starburst
        val burst = starPath(cx, cy, r, r * 0.5f, 8)
        drawPath(burst, Brush.sweepGradient(listOf(c1, c2, c3, c1)), style = Fill)
        drawPath(burst, Color.White.copy(alpha = 0.35f), style = Stroke(width = 1.5f))

        // Inner circle with gradient
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.7f), Color.Transparent)), radius = r * 0.35f, center = Offset(cx, cy))

        // Center dot
        drawCircle(Color.White, radius = r * 0.1f, center = Offset(cx, cy))
    }
}

// ═══════════════════════════════════════
// Helper: get rank badge composable by RP
// ═══════════════════════════════════════
@Composable
fun RankBadgeForRP(rp: Int, size: Dp = 28.dp) {
    when {
        rp >= 6500 -> GrandmasterRankIcon(size)
        rp >= 5000 -> MasterRankIcon(size)
        rp >= 3500 -> DiamondRankIcon(size)
        rp >= 2200 -> PlatinumRankIcon(size)
        rp >= 1200 -> GoldRankIcon(size)
        rp >= 500  -> SilverRankIcon(size)
        else       -> BronzeRankIcon(size)
    }
}

@Composable
fun RankBadgeByName(name: String, size: Dp = 28.dp) {
    when (name) {
        "Grandmaster" -> GrandmasterRankIcon(size)
        "Master"      -> MasterRankIcon(size)
        "Diamond"     -> DiamondRankIcon(size)
        "Platinum"    -> PlatinumRankIcon(size)
        "Gold"        -> GoldRankIcon(size)
        "Silver"      -> SilverRankIcon(size)
        else          -> BronzeRankIcon(size)
    }
}

// ═══════════════════════════════════════
// Helper functions
// ═══════════════════════════════════════
private fun hexPath(cx: Float, cy: Float, radius: Float, sides: Int): Path {
    val path = Path()
    val angleStep = (2 * PI / sides).toFloat()
    val startAngle = -PI.toFloat() / 2
    for (i in 0 until sides) {
        val angle = startAngle + i * angleStep
        val x = cx + radius * cos(angle)
        val y = cy + radius * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun starPath(cx: Float, cy: Float, outerR: Float, innerR: Float, points: Int): Path {
    val path = Path()
    val step = PI.toFloat() / points
    val start = -PI.toFloat() / 2
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerR else innerR
        val angle = start + i * step
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
