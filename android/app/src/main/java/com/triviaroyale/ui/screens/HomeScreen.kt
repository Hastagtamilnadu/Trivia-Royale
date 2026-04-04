package com.triviaroyale.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.triviaroyale.ads.QuizRewardedAdManager
import com.triviaroyale.ads.RewardedSkipWaitResult
import com.triviaroyale.ads.findActivity
import com.triviaroyale.data.GameState
import com.triviaroyale.data.QuizRepository
import com.triviaroyale.data.isIplSeasonActive
import com.triviaroyale.ui.components.AppProgressBar
import com.triviaroyale.ui.components.QuizLaunchCooldownBar
import com.triviaroyale.ui.components.RewardedSkipWaitButton
import java.text.SimpleDateFormat
import com.triviaroyale.ui.theme.*
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/** Staggered entrance animation wrapper for home screen sections. */
@Composable
private fun StaggeredItem(
    index: Int,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 80L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
            slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ),
    ) {
        content()
    }
}

private fun millisUntilNextMidnight(now: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return (calendar.timeInMillis - now).coerceAtLeast(0L)
}

private fun formatClockCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours >= 1L) {
        "%02dh %02dm".format(hours, minutes)
    } else {
        "%02dm %02ds".format(minutes, seconds)
    }
}

private fun formatAvailabilityTime(millis: Long): String {
    return SimpleDateFormat("EEE, d MMM h:mm a", Locale.getDefault()).format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    gameState: GameState,
    quizRewardedAdManager: QuizRewardedAdManager,
    navController: NavController
) {
    val state by gameState.state.collectAsState()
    val scrollState = rememberScrollState()
    val activity = LocalContext.current.findActivity()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var isRewardedSkipWorking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    val iplSeasonActive = isIplSeasonActive(now)
    val grandMasterStatus = gameState.getGrandMasterStatus(now)
    val grandMasterCountdown = formatClockCountdown((grandMasterStatus.nextAvailableAt - now).coerceAtLeast(0L))
    val quizCooldown = gameState.getQuizLaunchCooldownStatus(now)
    val canUseRewardedSkip = gameState.canUseRewardedSkipWait(now)

    // When the break wait timer naturally expires → reset cumulative play time
    var wasBreakActive by remember { mutableStateOf(quizCooldown.active) }
    LaunchedEffect(quizCooldown.active) {
        if (wasBreakActive && !quizCooldown.active) {
            gameState.onBreakExpired()
        }
        wasBreakActive = quizCooldown.active
    }

    LaunchedEffect(quizCooldown.active, canUseRewardedSkip) {
        if (quizCooldown.active && canUseRewardedSkip) {
            quizRewardedAdManager.preloadIfNeeded()
        }
    }

    fun watchAdToSkipWait() {
        if (!gameState.canUseRewardedSkipWait(now)) {
            return
        }
        isRewardedSkipWorking = true
        quizRewardedAdManager.showSkipWaitReward(activity) { result ->
            if (result == RewardedSkipWaitResult.EARNED) {
                // Skip the cooldown AND grant +10 coins as the ad reward
                gameState.consumeRewardedSkipWaitAndReward()
            }
            isRewardedSkipWorking = false
        }
    }

    // Pulsing glow animation for the Play Now button
    val infiniteTransition = rememberInfiniteTransition(label = "playGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ═══════════════════════════════════════
        // HERO: Daily Challenge with Glow Border
        // ═══════════════════════════════════════
        StaggeredItem(0) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Primary.copy(alpha = 0.25f),
                    spotColor = Tertiary.copy(alpha = 0.2f)
                )
                .drawBehind {
                    // Draw a gradient glow border around the card
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFB6A0FF).copy(alpha = 0.4f),
                                Color(0xFF81ECFF).copy(alpha = 0.4f)
                            )
                        ),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background decorative icon
                Icon(
                    Icons.Filled.Quiz,
                    contentDescription = null,
                    tint = Primary.copy(alpha = 0.06f),
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 20.dp)
                )
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "GRAND MASTER QUIZ",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = Primary
                        )
                    }
                    // Title
                    Text(
                        "The Grand Master Quiz",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    // Description
                    Text(
                        "One featured Grand Master run unlocks each day. Live updates can refresh the set, and the app falls back to General Knowledge when no remote bank is published.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    // Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Play Now button with glow
                        Button(
                            onClick = {
                                navController.navigate(
                                    "genreQuiz/${Uri.encode("Grand Master Quiz")}?autostart=true"
                                )
                            },
                            enabled = grandMasterStatus.available,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                            modifier = Modifier
                                .shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    ambientColor = Primary.copy(alpha = glowAlpha),
                                    spotColor = Primary.copy(alpha = glowAlpha + 0.1f)
                                )
                                .background(
                                    Brush.linearGradient(listOf(Primary, PrimaryDim)),
                                    shape = RoundedCornerShape(12.dp)
                                        )
                        ) {
                            Text(
                                if (grandMasterStatus.available) "Play Now" else "Done",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                if (grandMasterStatus.available) Icons.Filled.PlayArrow else Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!grandMasterStatus.available) {
                                Icon(Icons.Filled.Schedule, contentDescription = null, tint = Tertiary, modifier = Modifier.size(16.dp))
                                Text(
                                    grandMasterCountdown,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Tertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    if (quizCooldown.active) {
                        QuizLaunchCooldownBar(
                            remainingMillis = quizCooldown.remainingMillis,
                            totalMillis = quizCooldown.totalMillis,
                            color = Green400
                        )
                        Spacer(Modifier.height(12.dp))
                        RewardedSkipWaitButton(
                            enabled = canUseRewardedSkip,
                            isWorking = isRewardedSkipWorking,
                            onClick = ::watchAdToSkipWait
                        )
                    }
                }
            }
        }
        } // end StaggeredItem(0)

        // ═══════════════════════════════════════
        // CLAN WAR — Full width row
        // ═══════════════════════════════════════
        StaggeredItem(1) {
        Card(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            onClick = { navController.navigate("clans?tab=season") }
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Background decorative icon
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = OnSurface.copy(alpha = 0.04f),
                    modifier = Modifier.size(100.dp).align(Alignment.TopEnd)
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Groups, contentDescription = null, tint = Secondary, modifier = Modifier.size(28.dp))
                            Text(
                                "Clan War",
                                style = MaterialTheme.typography.titleLarge,
                                color = Secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Join a clan, contribute points, and climb the new public season ladder together",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                            listOf(Primary, Tertiary, Secondary).forEach { color ->
                                Surface(
                                    shape = CircleShape,
                                    color = SurfaceContainerHigh,
                                    modifier = Modifier.size(36.dp),
                                    border = BorderStroke(2.dp, Surface)
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Person, null, tint = color, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        Text(
                            "Open Clans",
                            color = Secondary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
        } // end StaggeredItem(1)

        // ═══════════════════════════════════════
        // LIGHTNING ROUND — Full width row
        // ═══════════════════════════════════════
        val dailyHeat = remember { com.triviaroyale.data.LightningHeatSystem.getDailyHeatLevel() }
        val heatColor = remember { com.triviaroyale.data.LightningHeatSystem.getHeatColor(dailyHeat) }
        val heatLabel = remember { com.triviaroyale.data.LightningHeatSystem.getHeatLabel(dailyHeat) }
        val heatSubtitle = remember { com.triviaroyale.data.LightningHeatSystem.getHomeSubtitle(dailyHeat) }
        StaggeredItem(2) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
            onClick = { navController.navigate("lightningQuiz") }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = heatColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = heatColor, modifier = Modifier.size(28.dp))
                    }
                }
                // Info
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Lightning Round", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(heatSubtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    Column {
                        AppProgressBar(
                            progress = dailyHeat,
                            modifier = Modifier.fillMaxWidth(),
                            height = 6.dp,
                            color = heatColor,
                            trackColor = SurfaceContainerLowest
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Heat Level", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = OnSurfaceVariant)
                            Text(heatLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = heatColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        } // end StaggeredItem(2)

        // ═══════════════════════════════════════
        // EXPLORE GENRES — 2x2 grid
        // ═══════════════════════════════════════
        StaggeredItem(3) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Explore Genres", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { navController.navigate("exploreGenres") }) {
                Text("View All", color = Primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
            }
        }
        } // end StaggeredItem(3)

        val displayGenres = if (iplSeasonActive) {
            QuizRepository.genres.filterNot { it.name == "Sports" }.take(4)
        } else {
            QuizRepository.genres.take(4)
        }
        fun genreColor(name: String): Color = when (name) {
            "IPL" -> Tertiary
            "Sports" -> Secondary
            "Movies" -> Primary
            "Science" -> Tertiary
            "History" -> Error
            else -> Primary
        }
        fun genreIcon(name: String) = when (name) {
            "IPL" -> Icons.Filled.EmojiEvents
            "Sports" -> Icons.Filled.SportsSoccer
            "Movies" -> Icons.Filled.Movie
            "Science" -> Icons.Filled.Science
            "History" -> Icons.Filled.HistoryEdu
            else -> Icons.Filled.Quiz
        }

        // Row 1: 2 genres
        StaggeredItem(4) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (i in 0..1) {
                if (i < displayGenres.size) {
                    val genre = displayGenres[i]
                    val accent = genreColor(genre.name)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (genre.name == "IPL") {
                                    Modifier.drawWithContent {
                                        drawContent()
                                        drawRoundRect(
                                            brush = Brush.horizontalGradient(
                                                listOf(Purple500, Cyan400)
                                            ),
                                            cornerRadius = CornerRadius(16.dp.toPx()),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                        onClick = { navController.navigate("genreQuiz/${Uri.encode(genre.name)}") }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = accent.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(genreIcon(genre.name), contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
                                }
                            }
                            Column {
                                Text(genre.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text("${genre.quizCount} Quizzes", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        } // end StaggeredItem(4)

        // Row 2: 2 genres
        StaggeredItem(5) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (i in 2..3) {
                if (i < displayGenres.size) {
                    val genre = displayGenres[i]
                    val accent = genreColor(genre.name)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (genre.name == "IPL") {
                                    Modifier.drawWithContent {
                                        drawContent()
                                        drawRoundRect(
                                            brush = Brush.horizontalGradient(
                                                listOf(Purple500, Cyan400)
                                            ),
                                            cornerRadius = CornerRadius(16.dp.toPx()),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                        onClick = { navController.navigate("genreQuiz/${Uri.encode(genre.name)}") }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = accent.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(genreIcon(genre.name), contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
                                }
                            }
                            Column {
                                Text(genre.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text("${genre.quizCount} Quizzes", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        } // end StaggeredItem(5)

        // ═══════════════════════════════════════
        // ACHIEVEMENT WIDGET
        // ═══════════════════════════════════════
        val achievements = listOf(
            Triple("First Steps", "Complete your first quiz", state.quizzesPlayed >= 1),
            Triple("Getting Hooked", "Reach a 3-day streak", state.streak >= 3),
            Triple("Sharp Mind", "Get 10 correct answers", state.correctAnswers >= 10),
            Triple("First Earnings", "Earn 100 coins", state.coins >= 100),
            Triple("Rising Star", "Reach Level 5", state.level >= 5),
        )
        val nextAch = achievements.firstOrNull { !it.third }
        val lastUnlocked = achievements.lastOrNull { it.third }
        val achToShow = nextAch ?: lastUnlocked

        if (achToShow != null) {
            StaggeredItem(6) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant.copy(alpha = 0.4f)),
                onClick = { navController.navigate("achievements") }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (achToShow.third) SecondaryContainer else SurfaceContainerHighest,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                if (achToShow.third) Icons.Filled.EmojiEvents else Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = if (achToShow.third) Secondary else OnSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (achToShow.third) "RECENTLY UNLOCKED" else "NEXT GOAL",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                            color = if (achToShow.third) Secondary else Tertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(achToShow.first, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(achToShow.second, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                }
            }
            } // end StaggeredItem(6)
        }
    }
}
