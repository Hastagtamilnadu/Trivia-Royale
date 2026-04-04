package com.triviaroyale.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.triviaroyale.data.GameState
import com.triviaroyale.firebase.FirebaseCloudRepository
import com.triviaroyale.firebase.SeasonHistoryResult
import com.triviaroyale.analytics.ClanAnalytics
import com.triviaroyale.ui.theme.OnSurface
import com.triviaroyale.ui.theme.OnSurfaceVariant
import com.triviaroyale.ui.theme.OutlineVariant
import com.triviaroyale.ui.theme.Primary
import com.triviaroyale.ui.theme.Purple500
import com.triviaroyale.ui.theme.Secondary
import com.triviaroyale.ui.theme.Surface
import com.triviaroyale.ui.theme.SurfaceContainerHigh
import com.triviaroyale.ui.theme.SurfaceContainerHighest
import com.triviaroyale.ui.theme.SurfaceContainerLow
import com.triviaroyale.ui.theme.Tertiary
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(gameState: GameState, cloudRepository: FirebaseCloudRepository, navController: NavController) {
    val state by gameState.state.collectAsState()
    val formatter = NumberFormat.getIntegerInstance()
    var seasonHistory by remember { mutableStateOf<SeasonHistoryResult?>(null) }
    var historyLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        runCatching { seasonHistory = cloudRepository.fetchSeasonHistory() }
        historyLoading = false
        ClanAnalytics.logSeasonHistoryViewed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Primary, Purple500)),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.size(112.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        state.username,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        "Level ${state.level} · Clan identity is now the main progression path",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                    if (!state.equippedTitle.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                        ) {
                            Text(
                                state.equippedTitle!!.substringAfterLast("_").replace('_', ' ').replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("TOTAL XP", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Text(
                        formatter.format(state.xp),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("STREAK", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Text(
                        "${state.streak} days",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Secondary
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = Tertiary)
                    Text("Season History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (historyLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Loading season history...", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                } else if (seasonHistory == null) {
                    Text("Could not load season history.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                } else {
                    val history = seasonHistory!!
                    if (history.lastSeasonFinish > 0) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Primary.copy(alpha = 0.22f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Primary)
                                Text(
                                    "Last Season: Rank #${history.lastSeasonFinish}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface,
                                )
                            }
                        }
                    }
                    if (history.seasonBadges.isNotEmpty()) {
                        Text("Earned Badges", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            history.seasonBadges.take(6).forEach { badge ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Tertiary.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, Tertiary.copy(alpha = 0.25f)),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = Tertiary, modifier = Modifier.size(14.dp))
                                        Text(
                                            badge.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Tertiary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.1f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            Triple("Wars", "${history.warParticipationCount}", Primary),
                            Triple("Wins", "${history.warWinCount}", Secondary),
                            Triple("MVPs", "${history.mvpCount}", Tertiary),
                        ).forEach { (label, value, color) ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            Triple("CP Streak", "${history.contributionStreak}", Secondary),
                            Triple("Lifetime CP", formatter.format(history.lifetimeContribution), Primary),
                            Triple("Chests", "${history.warChestClaims}", Tertiary),
                        ).forEach { (label, value, color) ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("My Achievements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = { navController.navigate("achievements") }) {
                    Text("View All", color = Primary, fontWeight = FontWeight.Bold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple("Quizzes", formatter.format(state.quizzesPlayed), Primary),
                    Triple("Correct", formatter.format(state.correctAnswers), Secondary),
                    Triple("Coins", formatter.format(state.coins), Tertiary),
                ).forEach { (label, value, color) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(
                "Account & Support",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow)
            ) {
                Column {
                    Surface(onClick = { navController.navigate("settings") }, color = Color.Transparent) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(shape = RoundedCornerShape(10.dp), color = SurfaceContainerHighest, modifier = Modifier.size(40.dp)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Settings, null, tint = Primary)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Account Settings", fontWeight = FontWeight.Bold)
                                Text("Manage your display name and app preferences", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                            Icon(Icons.Filled.ChevronRight, null, tint = OnSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.1f))

                    Surface(onClick = { navController.navigate("help") }, color = Color.Transparent) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(shape = RoundedCornerShape(10.dp), color = SurfaceContainerHighest, modifier = Modifier.size(40.dp)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.LocalFireDepartment, null, tint = Tertiary)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Help & Support", fontWeight = FontWeight.Bold)
                                Text("FAQs, privacy tools, and account management", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                            Icon(Icons.Filled.ChevronRight, null, tint = OnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
