package com.triviaroyale.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.triviaroyale.data.GameState
import com.triviaroyale.ui.components.AppProgressBar
import com.triviaroyale.ui.theme.*
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(gameState: GameState, navController: NavController) {
    val state by gameState.state.collectAsState()

    // === Achievement Data (matching web app) ===
    data class Achievement(val name: String, val desc: String, val icon: String, val metric: Int, val target: Int, val gold: Boolean)
    data class Category(val name: String, val icon: String, val catColor: Color, val items: List<Achievement>)

    val categories = listOf(
        Category("Gameplay", "sports_esports", Primary, listOf(
            Achievement("First Steps", "Complete your first quiz", "play_circle", state.quizzesPlayed, 1, false),
            Achievement("Quiz Enthusiast", "Complete 5 quizzes", "bolt", state.quizzesPlayed, 5, false),
            Achievement("Quiz Veteran", "Complete 10 quizzes", "psychology", state.quizzesPlayed, 10, false),
            Achievement("Quiz Master", "Complete 25 quizzes", "emoji_events", state.quizzesPlayed, 25, true),
            Achievement("Legend", "Complete 100 quizzes", "workspace_premium", state.quizzesPlayed, 100, true),
        )),
        Category("Knowledge", "psychology", Tertiary, listOf(
            Achievement("Sharp Mind", "Get 10 correct answers", "lightbulb", state.correctAnswers, 10, false),
            Achievement("Brainy", "Get 50 correct answers", "school", state.correctAnswers, 50, false),
            Achievement("Genius", "Get 200 correct answers", "neurology", state.correctAnswers, 200, true),
            Achievement("Knowledge Ace", "Get 1000 correct answers", "psychology", state.correctAnswers, 1000, true),
        )),
        Category("Consistency", "event_repeat", Secondary, listOf(
            Achievement("Getting Hooked", "Reach a 3-day streak", "local_fire_department", state.streak, 3, false),
            Achievement("Weekly Warrior", "7-day streak", "whatshot", state.streak, 7, false),
            Achievement("Unstoppable", "30-day streak", "local_fire_department", state.streak, 30, true),
        )),
        Category("Milestones", "flag", Primary, listOf(
            Achievement("First Earnings", "Earn 100 coins", "paid", state.coins, 100, true),
            Achievement("Coin Collector", "Earn 500 coins", "savings", state.coins, 500, true),
            Achievement("Treasure Stack", "Earn 1000 coins", "paid", state.coins, 1000, true),
            Achievement("Vault Breaker", "Earn 10000 coins", "paid", state.coins, 10000, true),
            Achievement("Rising Star", "Reach Level 5", "star", state.level, 5, true),
            Achievement("Dedicated", "Reach Level 10", "military_tech", state.level, 10, true),
            Achievement("Grandmaster", "Reach Level 20", "diamond", state.level, 20, true),
        ))
    )

    val allAch = categories.flatMap { it.items }
    val unlocked = allAch.count { it.metric >= it.target }
    val goldCount = allAch.count { it.metric >= it.target && it.gold }
    val silverCount = allAch.count { it.metric >= it.target && !it.gold }
    val progressPct = if (allAch.isNotEmpty()) (unlocked.toFloat() / allAch.size) else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // === Back button + Title ===
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OnSurface)
            }
            Text("Achievements", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // === Summary Stats ===
        // Overall Progress - Full Width
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceContainerHigh.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Overall Progress", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp), color = OnSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$unlocked",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)
                    )
                    Text(
                        "/${allAch.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                AppProgressBar(
                    progress = progressPct,
                    modifier = Modifier.fillMaxWidth(),
                    height = 6.dp,
                    color = Primary,
                    trackColor = SurfaceContainerHighest,
                    cornerRadius = 3.dp
                )
            }
        }

        // Gold & Silver Badge Counts - Side by Side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gold Badges
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceContainerHigh.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.15f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Canvas gold medal icon
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFFFFD700).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AchievementIcon(iconName = "military_tech", tint = Color(0xFFFFD700), size = 20.dp)
                    }
                    Column {
                        Text("$goldCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = Color(0xFFFFD700))
                        Text("GOLD BADGES", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 1.5.sp), color = OnSurfaceVariant)
                    }
                }
            }
            // Silver Badges
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceContainerHigh.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color(0xFFC0C0C0).copy(alpha = 0.15f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Canvas silver star icon
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFFC0C0C0).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AchievementIcon(iconName = "workspace_premium", tint = Color(0xFFC0C0C0), size = 20.dp)
                    }
                    Column {
                        Text("$silverCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = Color(0xFFC0C0C0))
                        Text("SILVER BADGES", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 1.5.sp), color = OnSurfaceVariant)
                    }
                }
            }
        }

        // === Category Sections ===
        categories.forEach { cat ->
            val catUnlocked = cat.items.count { it.metric >= it.target }

            Spacer(Modifier.height(4.dp))

            // Category Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AchievementIcon(iconName = cat.icon, tint = cat.catColor, size = 20.dp)
                    Text(
                        cat.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "$catUnlocked of ${cat.items.size} Unlocked",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            // Achievement Grid (2 columns)
            val chunkedItems = cat.items.chunked(2)
            chunkedItems.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { ach ->
                        val done = ach.metric >= ach.target
                        val progress = if (ach.target > 0) minOf(1f, ach.metric.toFloat() / ach.target) else 0f

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceContainerHigh.copy(alpha = 0.4f),
                            border = BorderStroke(
                                1.dp,
                                if (done && ach.gold) Color(0xFFFFD700).copy(alpha = 0.3f)
                                else if (done) cat.catColor.copy(alpha = 0.3f)
                                else OutlineVariant.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Icon circle
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(
                                            if (done && ach.gold) Color(0xFFFFD700).copy(alpha = 0.15f)
                                            else if (done) cat.catColor.copy(alpha = 0.15f)
                                            else SurfaceContainerHighest,
                                            CircleShape
                                        )
                                        .then(
                                            if (done && ach.gold) Modifier.border(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.4f), CircleShape)
                                            else if (done) Modifier.border(1.5.dp, cat.catColor.copy(alpha = 0.4f), CircleShape)
                                            else Modifier
                                        )
                                        .then(
                                            if (done && ach.gold) {
                                                Modifier.shadow(
                                                    elevation = 12.dp,
                                                    shape = CircleShape,
                                                    ambientColor = Color(0xFFFFD700).copy(alpha = 0.45f),
                                                    spotColor = Color(0xFFFFD700).copy(alpha = 0.45f)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Custom Canvas icon
                                    AchievementIcon(
                                        iconName = ach.icon,
                                        tint = if (done && ach.gold) Color(0xFFFFD700)
                                               else if (done) cat.catColor
                                               else OnSurfaceVariant.copy(alpha = 0.5f),
                                        size = 26.dp
                                    )

                                    // Check badge
                                    if (done) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .offset(x = 2.dp, y = 2.dp)
                                                .size(16.dp)
                                                .background(
                                                    if (ach.gold) Color(0xFFFFD700) else cat.catColor,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("✓", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // Name
                                Text(
                                    ach.name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center,
                                    color = if (done && ach.gold) Color(0xFFFFD700)
                                            else if (done) OnSurface
                                            else OnSurface,
                                    maxLines = 1
                                )

                                // Description
                                Text(
                                    ach.desc,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = OnSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )

                                // Progress bar for locked
                                if (!done) {
                                    Spacer(Modifier.height(6.dp))
                                    AppProgressBar(
                                        progress = progress,
                                        modifier = Modifier.fillMaxWidth(),
                                        height = 3.dp,
                                        color = Primary,
                                        trackColor = SurfaceContainerHighest,
                                        cornerRadius = 2.dp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${ach.metric}/${ach.target}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    // Fill empty space if odd number of items
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// === Custom Canvas Achievement Icons ===
@Composable
internal fun AchievementIcon(iconName: String, tint: Color, size: Dp = 26.dp) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension * 0.4f

        when (iconName) {
            "play_circle" -> drawPlayIcon(cx, cy, r, tint)
            "bolt" -> drawBoltIcon(cx, cy, r, tint)
            "psychology" -> drawBrainIcon(cx, cy, r, tint)
            "emoji_events" -> drawTrophyIcon(cx, cy, r, tint)
            "workspace_premium" -> drawMedalIcon(cx, cy, r, tint)
            "lightbulb" -> drawBulbIcon(cx, cy, r, tint)
            "school" -> drawCapIcon(cx, cy, r, tint)
            "neurology" -> drawBrainIcon(cx, cy, r, tint)
            "local_fire_department", "whatshot" -> drawFlameIcon(cx, cy, r, tint)
            "event_repeat" -> drawRepeatIcon(cx, cy, r, tint)
            "paid" -> drawCoinIcon(cx, cy, r, tint)
            "savings" -> drawPiggyIcon(cx, cy, r, tint)
            "star" -> drawStarIcon(cx, cy, r, tint)
            "military_tech" -> drawMedalIcon(cx, cy, r, tint)
            "diamond" -> drawDiamondIcon(cx, cy, r, tint)
            "flag" -> drawFlagIcon(cx, cy, r, tint)
            "sports_esports" -> drawControllerIcon(cx, cy, r, tint)
            "lock" -> drawLockIcon(cx, cy, r, tint)
            else -> drawStarIcon(cx, cy, r, tint)
        }
    }
}

// Public alias for use in other screens
@Composable
fun AchievementIconPublic(iconName: String, tint: Color, size: Dp = 26.dp) {
    AchievementIcon(iconName = iconName, tint = tint, size = size)
}

// --- Individual icon draw functions ---

private fun DrawScope.drawPlayIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    drawCircle(tint, radius = r, center = Offset(cx, cy), style = Stroke(width = 2f))
    val tri = Path().apply {
        moveTo(cx - r * 0.3f, cy - r * 0.5f)
        lineTo(cx + r * 0.6f, cy)
        lineTo(cx - r * 0.3f, cy + r * 0.5f)
        close()
    }
    drawPath(tri, tint, style = Fill)
}

private fun DrawScope.drawBoltIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    val bolt = Path().apply {
        moveTo(cx + r * 0.1f, cy - r * 1.0f)
        lineTo(cx - r * 0.5f, cy + r * 0.1f)
        lineTo(cx + r * 0.05f, cy + r * 0.1f)
        lineTo(cx - r * 0.1f, cy + r * 1.0f)
        lineTo(cx + r * 0.5f, cy - r * 0.1f)
        lineTo(cx - r * 0.05f, cy - r * 0.1f)
        close()
    }
    drawPath(bolt, tint, style = Fill)
}

private fun DrawScope.drawBrainIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    // Simplified brain: two overlapping circles
    drawCircle(tint, radius = r * 0.6f, center = Offset(cx - r * 0.25f, cy - r * 0.1f), style = Stroke(width = 2f))
    drawCircle(tint, radius = r * 0.6f, center = Offset(cx + r * 0.25f, cy - r * 0.1f), style = Stroke(width = 2f))
    drawCircle(tint, radius = r * 0.4f, center = Offset(cx, cy + r * 0.3f), style = Stroke(width = 2f))
    // Center line
    drawLine(tint, Offset(cx, cy - r * 0.7f), Offset(cx, cy + r * 0.7f), strokeWidth = 1.5f)
}

private fun DrawScope.drawTrophyIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    // Cup body
    val cup = Path().apply {
        moveTo(cx - r * 0.6f, cy - r * 0.7f)
        lineTo(cx + r * 0.6f, cy - r * 0.7f)
        lineTo(cx + r * 0.4f, cy + r * 0.2f)
        lineTo(cx - r * 0.4f, cy + r * 0.2f)
        close()
    }
    drawPath(cup, tint, style = Fill)
    // Handles
    drawArc(tint, startAngle = -90f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx + r * 0.5f, cy - r * 0.5f), size = Size(r * 0.45f, r * 0.7f),
        style = Stroke(width = 2f))
    drawArc(tint, startAngle = 90f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - r * 0.95f, cy - r * 0.5f), size = Size(r * 0.45f, r * 0.7f),
        style = Stroke(width = 2f))
    // Base
    drawRect(tint, topLeft = Offset(cx - r * 0.3f, cy + r * 0.2f), size = Size(r * 0.6f, r * 0.15f))
    drawRect(tint, topLeft = Offset(cx - r * 0.5f, cy + r * 0.35f), size = Size(r * 1.0f, r * 0.2f))
}

private fun DrawScope.drawMedalIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    // Ribbon
    val ribbon = Path().apply {
        moveTo(cx - r * 0.5f, cy - r * 1.0f)
        lineTo(cx - r * 0.2f, cy - r * 0.1f)
        lineTo(cx, cy - r * 0.4f)
        lineTo(cx + r * 0.2f, cy - r * 0.1f)
        lineTo(cx + r * 0.5f, cy - r * 1.0f)
        close()
    }
    drawPath(ribbon, tint.copy(alpha = 0.5f), style = Fill)
    // Medal circle
    drawCircle(tint, radius = r * 0.55f, center = Offset(cx, cy + r * 0.2f), style = Fill)
    drawCircle(tint.copy(alpha = 0.3f), radius = r * 0.35f, center = Offset(cx, cy + r * 0.2f), style = Stroke(width = 1.5f))
}

private fun DrawScope.drawBulbIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    // Bulb
    drawCircle(tint, radius = r * 0.6f, center = Offset(cx, cy - r * 0.2f), style = Stroke(width = 2f))
    // Filament lines
    drawLine(tint, Offset(cx, cy - r * 0.6f), Offset(cx, cy + r * 0.1f), strokeWidth = 1.5f)
    // Base
    drawRect(tint, topLeft = Offset(cx - r * 0.3f, cy + r * 0.4f), size = Size(r * 0.6f, r * 0.15f))
    drawRect(tint, topLeft = Offset(cx - r * 0.25f, cy + r * 0.55f), size = Size(r * 0.5f, r * 0.1f))
    // Rays
    for (i in 0..3) {
        val angle = -45f + i * 30f
        val rad = angle * PI.toFloat() / 180f
        drawLine(tint,
            Offset(cx + r * 0.7f * cos(rad), cy - r * 0.2f + r * 0.7f * sin(rad)),
            Offset(cx + r * 1.0f * cos(rad), cy - r * 0.2f + r * 1.0f * sin(rad)),
            strokeWidth = 1.5f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawCapIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    // Graduation cap
    val cap = Path().apply {
        moveTo(cx, cy - r * 0.6f)
        lineTo(cx + r * 1.0f, cy)
        lineTo(cx, cy + r * 0.3f)
        lineTo(cx - r * 1.0f, cy)
        close()
    }
    drawPath(cap, tint, style = Fill)
    // Tassel
    drawLine(tint, Offset(cx + r * 0.7f, cy), Offset(cx + r * 0.7f, cy + r * 0.7f), strokeWidth = 2f)
    drawCircle(tint, radius = r * 0.1f, center = Offset(cx + r * 0.7f, cy + r * 0.8f))
}

private fun DrawScope.drawFlameIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    val flame = Path().apply {
        moveTo(cx, cy - r * 1.0f)
        cubicTo(cx + r * 0.5f, cy - r * 0.4f, cx + r * 0.8f, cy + r * 0.2f, cx + r * 0.4f, cy + r * 0.8f)
        quadraticBezierTo(cx, cy + r * 1.0f, cx - r * 0.4f, cy + r * 0.8f)
        cubicTo(cx - r * 0.8f, cy + r * 0.2f, cx - r * 0.5f, cy - r * 0.4f, cx, cy - r * 1.0f)
        close()
    }
    drawPath(flame, tint, style = Fill)
}

private fun DrawScope.drawRepeatIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    drawArc(tint, startAngle = 0f, sweepAngle = 270f, useCenter = false,
        topLeft = Offset(cx - r * 0.6f, cy - r * 0.6f), size = Size(r * 1.2f, r * 1.2f),
        style = Stroke(width = 2f, cap = StrokeCap.Round))
    // Arrow
    val arrowTip = Offset(cx + r * 0.6f, cy)
    drawLine(tint, arrowTip, Offset(arrowTip.x - r * 0.3f, arrowTip.y - r * 0.3f), strokeWidth = 2f, cap = StrokeCap.Round)
    drawLine(tint, arrowTip, Offset(arrowTip.x + r * 0.1f, arrowTip.y - r * 0.35f), strokeWidth = 2f, cap = StrokeCap.Round)
}

private fun DrawScope.drawCoinIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    drawCircle(tint, radius = r * 0.8f, center = Offset(cx, cy), style = Fill)
    drawCircle(tint.copy(alpha = 0.3f), radius = r * 0.55f, center = Offset(cx, cy), style = Stroke(width = 1.5f))
    // Dollar sign
    drawLine(tint.copy(alpha = 0.4f), Offset(cx, cy - r * 0.35f), Offset(cx, cy + r * 0.35f), strokeWidth = 2f)
    drawLine(tint.copy(alpha = 0.4f), Offset(cx - r * 0.15f, cy - r * 0.1f), Offset(cx + r * 0.15f, cy - r * 0.1f), strokeWidth = 1.5f)
    drawLine(tint.copy(alpha = 0.4f), Offset(cx - r * 0.15f, cy + r * 0.1f), Offset(cx + r * 0.15f, cy + r * 0.1f), strokeWidth = 1.5f)
}

private fun DrawScope.drawPiggyIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    // Body
    drawOval(tint, topLeft = Offset(cx - r * 0.7f, cy - r * 0.5f), size = Size(r * 1.4f, r * 1.0f), style = Fill)
    // Snout
    drawCircle(tint.copy(alpha = 0.7f), radius = r * 0.25f, center = Offset(cx + r * 0.7f, cy))
    // Ears
    drawCircle(tint, radius = r * 0.2f, center = Offset(cx - r * 0.2f, cy - r * 0.55f))
    drawCircle(tint, radius = r * 0.2f, center = Offset(cx + r * 0.2f, cy - r * 0.55f))
    // Legs
    drawRect(tint, topLeft = Offset(cx - r * 0.45f, cy + r * 0.35f), size = Size(r * 0.2f, r * 0.35f))
    drawRect(tint, topLeft = Offset(cx + r * 0.25f, cy + r * 0.35f), size = Size(r * 0.2f, r * 0.35f))
}

private fun DrawScope.drawStarIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    val star = starPath(cx, cy, r * 0.9f, r * 0.4f, 5)
    drawPath(star, tint, style = Fill)
}

private fun DrawScope.drawDiamondIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    val diamond = Path().apply {
        moveTo(cx, cy - r * 0.9f)
        lineTo(cx + r * 0.8f, cy - r * 0.1f)
        lineTo(cx, cy + r * 0.9f)
        lineTo(cx - r * 0.8f, cy - r * 0.1f)
        close()
    }
    drawPath(diamond, tint, style = Fill)
    // Facet line
    drawLine(tint.copy(alpha = 0.3f), Offset(cx - r * 0.8f, cy - r * 0.1f), Offset(cx + r * 0.8f, cy - r * 0.1f), strokeWidth = 1f)
}

private fun DrawScope.drawFlagIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    // Pole
    drawLine(tint, Offset(cx - r * 0.5f, cy - r * 0.9f), Offset(cx - r * 0.5f, cy + r * 0.9f), strokeWidth = 2f, cap = StrokeCap.Round)
    // Flag
    val flag = Path().apply {
        moveTo(cx - r * 0.5f, cy - r * 0.9f)
        lineTo(cx + r * 0.7f, cy - r * 0.5f)
        lineTo(cx - r * 0.5f, cy - r * 0.1f)
        close()
    }
    drawPath(flag, tint, style = Fill)
}

private fun DrawScope.drawControllerIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    // Body
    drawRoundRect(tint, topLeft = Offset(cx - r * 0.8f, cy - r * 0.4f), size = Size(r * 1.6f, r * 0.8f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.3f), style = Fill)
    // D-pad
    drawRect(Color.Black.copy(alpha = 0.3f), Offset(cx - r * 0.55f, cy - r * 0.1f), Size(r * 0.3f, r * 0.05f))
    drawRect(Color.Black.copy(alpha = 0.3f), Offset(cx - r * 0.47f, cy - r * 0.2f), Size(r * 0.05f, r * 0.3f))
    // Buttons
    drawCircle(Color.Black.copy(alpha = 0.3f), r * 0.06f, Offset(cx + r * 0.35f, cy - r * 0.1f))
    drawCircle(Color.Black.copy(alpha = 0.3f), r * 0.06f, Offset(cx + r * 0.55f, cy))
}

private fun DrawScope.drawLockIcon(cx: Float, cy: Float, r: Float, tint: Color) {
    // Lock body
    drawRoundRect(tint, topLeft = Offset(cx - r * 0.55f, cy - r * 0.1f), size = Size(r * 1.1f, r * 0.9f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.15f), style = Fill)
    // Lock shackle (arc)
    drawArc(tint, startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - r * 0.35f, cy - r * 0.8f), size = Size(r * 0.7f, r * 0.8f),
        style = Stroke(width = 2.5f))
    // Keyhole
    drawCircle(Color.Black.copy(alpha = 0.4f), radius = r * 0.12f, center = Offset(cx, cy + r * 0.2f))
}

// Helper
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
