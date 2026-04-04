package com.triviaroyale.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.triviaroyale.ads.QuizRewardedAdManager
import com.triviaroyale.ads.RewardedSkipWaitResult
import com.triviaroyale.ads.findActivity
import com.triviaroyale.data.GameState
import com.triviaroyale.data.QuizRepository
import com.triviaroyale.ui.components.QuizLaunchCooldownBar
import com.triviaroyale.ui.components.RewardedSkipWaitButton
import com.triviaroyale.ui.theme.Amber700
import com.triviaroyale.ui.theme.Cyan400
import com.triviaroyale.ui.theme.Green500
import com.triviaroyale.ui.theme.OnSurface
import com.triviaroyale.ui.theme.OnSurfaceVariant
import com.triviaroyale.ui.theme.Orange500
import com.triviaroyale.ui.theme.OutlineVariant
import com.triviaroyale.ui.theme.Primary
import com.triviaroyale.ui.theme.Purple400
import com.triviaroyale.ui.theme.Purple500
import com.triviaroyale.ui.theme.Red500
import com.triviaroyale.ui.theme.Secondary
import com.triviaroyale.ui.theme.Slate300
import com.triviaroyale.ui.theme.Surface
import com.triviaroyale.ui.theme.SurfaceContainerHigh
import com.triviaroyale.ui.theme.SurfaceContainerHighest
import com.triviaroyale.ui.theme.Tertiary
import com.triviaroyale.ui.theme.Yellow400
import kotlinx.coroutines.delay

private data class GenrePresentation(
    val icon: ImageVector,
    val color: Color,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreGenresScreen(
    gameState: GameState,
    quizRewardedAdManager: QuizRewardedAdManager,
    navController: NavController,
) {
    val activity = LocalContext.current.findActivity()
    val gameSnapshot by gameState.state.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isRewardedSkipWorking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            now = System.currentTimeMillis()
        }
    }
    val quizCooldown = remember(now, gameSnapshot.quizLaunchCooldownUntil) {
        gameState.getQuizLaunchCooldownStatus(now)
    }
    val canUseRewardedSkip = gameState.canUseRewardedSkipWait(now)

    // When the break timer naturally expires → reset cumulative play time
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
                gameState.consumeRewardedSkipWaitAndReward()
            }
            isRewardedSkipWorking = false
        }
    }
    val genrePresentation = mapOf(
        "IPL" to GenrePresentation(Icons.Filled.EmojiEvents, Tertiary, "Seasonal cricket specials with catalog-based updates"),
        "Sports" to GenrePresentation(Icons.Filled.SportsSoccer, Orange500, "Athletes, records and leagues"),
        "Movies" to GenrePresentation(Icons.Filled.Movie, Red500, "Cinema, actors and directors"),
        "Science" to GenrePresentation(Icons.Filled.Science, Cyan400, "Space, biology and chemistry"),
        "History" to GenrePresentation(Icons.Filled.HistoryEdu, Amber700, "World events and civilizations"),
        "Music" to GenrePresentation(Icons.Filled.MusicNote, Color(0xFFF472B6), "Artists, albums and genres"),
        "Tech" to GenrePresentation(Icons.Filled.Computer, Color(0xFF818CF8), "Innovation and silicon valley"),
        "Entertainment" to GenrePresentation(Icons.Filled.TheaterComedy, Yellow400, "Celebrities, shows and trends"),
        "Geography" to GenrePresentation(Icons.Filled.Public, Green500, "Countries, capitals and maps"),
        "Art" to GenrePresentation(Icons.Filled.Palette, Color(0xFFFB7185), "Painting, design and creativity"),
        "Literature" to GenrePresentation(Icons.Filled.MenuBook, Color(0xFF34D399), "Classic books and authors"),
        "General Knowledge" to GenrePresentation(Icons.Filled.Quiz, Purple400, "A bit of everything"),
        "Pop Culture" to GenrePresentation(Icons.Filled.TrendingUp, Secondary, "Internet moments and fandoms"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, null, tint = OnSurface)
            }
            Column {
                Text("Explore Genres", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Choose a genre to play", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }

        Spacer(Modifier.height(4.dp))

        if (quizCooldown.active) {
            QuizLaunchCooldownBar(
                remainingMillis = quizCooldown.remainingMillis,
                totalMillis = quizCooldown.totalMillis,
                color = Green500
            )
            Spacer(Modifier.height(8.dp))
            RewardedSkipWaitButton(
                enabled = canUseRewardedSkip,
                isWorking = isRewardedSkipWorking,
                onClick = ::watchAdToSkipWait
            )
        }

        QuizRepository.genres.forEach { genre ->
            val presentation = genrePresentation[genre.name]
                ?: GenrePresentation(Icons.Filled.School, Primary, "Challenge yourself with curated trivia")
            val destination = "genreQuiz/${Uri.encode(genre.name)}"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 232.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
                onClick = { navController.navigate(destination) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Icon(
                        presentation.icon,
                        contentDescription = null,
                        tint = OnSurface.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-8).dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = presentation.color.copy(alpha = 0.18f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        presentation.icon,
                                        contentDescription = null,
                                        tint = presentation.color,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            Text(
                                genre.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                presentation.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant,
                                maxLines = 2
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${genre.quizCount} quizzes",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                color = SurfaceContainerHighest.copy(alpha = 0.95f)
                            )
                        }

                        Button(
                            onClick = { navController.navigate(destination) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = presentation.color,
                                contentColor = Color.White
                            ),
                            enabled = true,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "Play",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
