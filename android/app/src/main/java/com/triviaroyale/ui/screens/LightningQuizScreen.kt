package com.triviaroyale.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.triviaroyale.ads.QuizInterstitialManager
import com.triviaroyale.ads.QuizRewardedAdManager
import com.triviaroyale.ads.RewardedSkipWaitResult
import com.triviaroyale.ads.findActivity
import com.triviaroyale.data.GameState
import com.triviaroyale.data.Question
import com.triviaroyale.data.QuizRepository
import com.triviaroyale.firebase.FirebaseCloudRepository
import com.triviaroyale.firebase.GameplaySessionRequest
import com.triviaroyale.ui.components.AppProgressBar
import com.triviaroyale.ui.components.QuizLaunchCooldownBar
import com.triviaroyale.ui.components.RewardedSkipWaitButton
import com.triviaroyale.ui.theme.Amber600
import com.triviaroyale.ui.theme.Cyan400
import com.triviaroyale.ui.theme.Error
import com.triviaroyale.ui.theme.Green400
import com.triviaroyale.ui.theme.Green500
import com.triviaroyale.ui.theme.OnSurface
import com.triviaroyale.ui.theme.OnSurfaceVariant
import com.triviaroyale.ui.theme.OutlineVariant
import com.triviaroyale.ui.theme.Primary
import com.triviaroyale.ui.theme.PrimaryDim
import com.triviaroyale.ui.theme.Purple500
import com.triviaroyale.ui.theme.Red500
import com.triviaroyale.ui.theme.Secondary
import com.triviaroyale.ui.theme.Surface
import com.triviaroyale.ui.theme.SurfaceContainer
import com.triviaroyale.ui.theme.SurfaceContainerHigh
import com.triviaroyale.ui.theme.SurfaceContainerHighest
import com.triviaroyale.ui.theme.SurfaceContainerLow
import com.triviaroyale.ui.theme.Tertiary
import com.triviaroyale.ui.theme.Yellow400
import com.triviaroyale.ui.theme.Zinc800
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LIGHTNING_TOTAL_QUESTIONS = 30

private enum class LightningQuizPhase { INTRO, QUIZ, RESULTS }

private fun formatLightningTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightningQuizScreen(
    gameState: GameState,
    cloudRepository: FirebaseCloudRepository,
    quizInterstitialManager: QuizInterstitialManager,
    quizRewardedAdManager: QuizRewardedAdManager,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.findActivity()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var phase by remember { mutableStateOf(LightningQuizPhase.INTRO) }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var currentQ by remember { mutableIntStateOf(0) }
    var answeredQuestions by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    val dailyHeat = remember { com.triviaroyale.data.LightningHeatSystem.getDailyHeatLevel() }
    val lightningStartTime = remember { com.triviaroyale.data.LightningHeatSystem.getTimerForHeat(dailyHeat) }
    val heatLabel = remember { com.triviaroyale.data.LightningHeatSystem.getHeatLabel(dailyHeat) }
    val heatColor = remember { com.triviaroyale.data.LightningHeatSystem.getHeatColor(dailyHeat) }
    val introDescription = remember { com.triviaroyale.data.LightningHeatSystem.getIntroDescription(dailyHeat) }
    var globalTimeLeft by remember { mutableIntStateOf(lightningStartTime) }
    var optionStates by remember { mutableStateOf(List(4) { OptionState.DEFAULT }) }
    var answered by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    var popupText by remember { mutableStateOf("") }
    var popupColor by remember { mutableStateOf(Green400) }
    var resultsRecorded by remember { mutableStateOf(false) }
    var isRewardedSkipWorking by remember { mutableStateOf(false) }
    var isLoadingQuestions by remember { mutableStateOf(false) }
    var sessionStartedAt by remember { mutableLongStateOf(0L) }
    var activeSessionId by remember { mutableStateOf("") }
    val quizCooldown = gameState.getQuizLaunchCooldownStatus(now)
    val canUseRewardedSkip = gameState.canUseRewardedSkipWait(now)

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
                gameState.consumeRewardedSkipWait()
            }
            isRewardedSkipWorking = false
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            now = System.currentTimeMillis()
        }
    }

    BackHandler(enabled = phase == LightningQuizPhase.QUIZ) {}

    fun resetOptions() {
        optionStates = List(4) { OptionState.DEFAULT }
        answered = false
    }

    fun showResults() {
        if (resultsRecorded) return

        resultsRecorded = true
        showPopup = false
        resetOptions()
        val didWin = answeredQuestions > 0 && correct.toDouble() / answeredQuestions >= 0.5
        val durationSeconds = if (sessionStartedAt > 0L) {
            ((System.currentTimeMillis() - sessionStartedAt) / 1000L).toInt().coerceAtLeast(1)
        } else {
            lightningStartTime - globalTimeLeft
        }

        gameState.recordQuizResult(
            score = score,
            questionsAnswered = answeredQuestions,
            correctCount = correct,
            isWin = didWin
        )
        gameState.addXP(score * gameState.getXpMultiplier())
        gameState.recordLightningRoundCompleted()
        if (activeSessionId.isNotBlank() && answeredQuestions > 0) {
            val session = GameplaySessionRequest(
                sessionId = activeSessionId,
                sessionType = "lightning",
                questionsAnswered = answeredQuestions,
                correctAnswers = correct,
                durationSeconds = durationSeconds.coerceAtLeast(1),
                didWin = didWin,
                genre = "Lightning Round"
            )
            scope.launch {
                runCatching { cloudRepository.recordGameplaySession(session) }
            }
        }
        activeSessionId = ""
        scope.launch {
            quizInterstitialManager.recordMatchCompleted()
            gameState.rollForBreak(
                lastInterstitialAdShownAt = quizInterstitialManager.getLastAdShownAtMillis(),
                matchesSinceLastInterruption = quizInterstitialManager.getMatchesSinceLastInterruption()
            ).also { if (it) quizInterstitialManager.recordInterruptionForBreak() }
            quizInterstitialManager.maybeShowAfterCompletedQuiz(activity) {
                phase = LightningQuizPhase.RESULTS
            }
        }
    }

    fun startQuiz() {
        if (quizCooldown.active) {
            return
        }
        isLoadingQuestions = true
        scope.launch {
            questions = QuizRepository.getLightningRoundQuestionsSuspend(LIGHTNING_TOTAL_QUESTIONS)
            currentQ = 0
            answeredQuestions = 0
            score = 0
            correct = 0
            streak = 0
            globalTimeLeft = lightningStartTime
            showPopup = false
            popupText = ""
            popupColor = Green400
            resultsRecorded = false
            sessionStartedAt = System.currentTimeMillis()
            activeSessionId = UUID.randomUUID().toString()
            resetOptions()
            isLoadingQuestions = false
            phase = LightningQuizPhase.QUIZ
        }
    }

    LaunchedEffect(phase) {
        if (phase == LightningQuizPhase.QUIZ) {
            while (phase == LightningQuizPhase.QUIZ && globalTimeLeft > 0) {
                delay(1000)
                if (phase != LightningQuizPhase.QUIZ) break
                gameState.recordRealPlaySeconds()
                quizInterstitialManager.recordActiveQuizSeconds()
                gameState.recordBreakPlaySecond()
                globalTimeLeft--
                if (globalTimeLeft <= 0) {
                    showResults()
                }
            }
        }
    }

    fun selectAnswer(index: Int) {
        if (phase != LightningQuizPhase.QUIZ || answered || questions.isEmpty() || currentQ >= questions.size) return

        answered = true
        val question = questions[currentQ]
        val isCorrect = index == question.answer

        optionStates = List(4) { optionIndex ->
            when {
                optionIndex == question.answer -> OptionState.CORRECT
                optionIndex == index && !isCorrect -> OptionState.WRONG
                else -> OptionState.DISABLED
            }
        }

        answeredQuestions = (currentQ + 1).coerceAtMost(questions.size)

        if (isCorrect) {
            correct++
            streak++
            val points = 10 + minOf(streak - 1, 5) * 2
            score += points
            globalTimeLeft = minOf(globalTimeLeft + 2, 999)
            popupText = ""
        } else {
            streak = 0
            globalTimeLeft = maxOf(globalTimeLeft - 2, 0)
            popupText = "-2s"
            popupColor = Error
        }

        showPopup = false

        scope.launch {
            delay(500)
            if (phase != LightningQuizPhase.QUIZ || resultsRecorded) return@launch

            showPopup = false

            if (globalTimeLeft <= 0 || currentQ + 1 >= questions.size) {
                showResults()
                return@launch
            }

            currentQ++
            resetOptions()
        }
    }

    val timerAccent = when {
        globalTimeLeft <= 10 -> Error
        globalTimeLeft <= 20 -> Secondary
        else -> Cyan400
    }

    Box(modifier = Modifier.fillMaxSize().background(Surface)) {
        when (phase) {
            LightningQuizPhase.INTRO -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = heatColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Bolt, null, tint = heatColor, modifier = Modifier.size(40.dp))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Lightning Round",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    // Heat badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = heatColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Bolt, null, tint = heatColor, modifier = Modifier.size(14.dp))
                            Text(
                                "Today's Heat: $heatLabel",
                                style = MaterialTheme.typography.labelMedium,
                                color = heatColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        introDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    // Heat meter
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AppProgressBar(
                            progress = dailyHeat,
                            modifier = Modifier.fillMaxWidth(),
                            height = 8.dp,
                            color = heatColor,
                            trackColor = SurfaceContainerHigh
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cool · 150s", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = OnSurfaceVariant)
                            Text("Inferno · 30s", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = OnSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Scoring", style = MaterialTheme.typography.labelLarge, color = heatColor, fontWeight = FontWeight.Bold)
                            Text("Correct answers start at 10 points and streaks add bonus points up to +10.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            Text("Wrong answers do not score and cut 2 seconds from the clock.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
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
                        Spacer(Modifier.height(16.dp))
                    }
                    Button(
                        onClick = { startQuiz() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        enabled = !quizCooldown.active,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(listOf(Primary, PrimaryDim)),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, tint = Color.White)
                                Text(
                                    "Start Lightning Round",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Back")
                    }
                }
            }

            LightningQuizPhase.QUIZ -> {
                if (questions.isNotEmpty() && currentQ < questions.size) {
                    val question = questions[currentQ]
                    val letters = listOf("A", "B", "C", "D")

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Lightning Round", style = MaterialTheme.typography.titleSmall, color = Tertiary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = RoundedCornerShape(50), color = Zinc800.copy(alpha = 0.5f)) {
                                    Text(
                                        "Score: $score",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Primary
                                    )
                                }
                                Surface(shape = RoundedCornerShape(50), color = Zinc800.copy(alpha = 0.5f)) {
                                    Text(
                                        formatLightningTime(globalTimeLeft),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = timerAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Question ${currentQ + 1} / ${questions.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant
                            )
                            AnimatedVisibility(visible = streak >= 3) {
                                Text(
                                    "$streak x Streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        AppProgressBar(
                            progress = currentQ.toFloat() / questions.size,
                            modifier = Modifier.fillMaxWidth(),
                            height = 8.dp,
                            color = Primary,
                            trackColor = SurfaceContainerHighest
                        )
                        AppProgressBar(
                            progress = (globalTimeLeft.toFloat() / lightningStartTime).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                            height = 4.dp,
                            color = timerAccent,
                            trackColor = SurfaceContainerHighest
                        )

                        // Active ability indicators
                        val abilityLabels = mutableListOf<Pair<String, Color>>()
                        if (gameState.getXpMultiplier() > 1) abilityLabels.add("⚡ 2x XP" to Yellow400)
                        if (gameState.getRpMultiplier() > 1.0) abilityLabels.add("🚀 +50% RP" to Green400)
                        if (abilityLabels.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                abilityLabels.forEach { (label, color) ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = color.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            label,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
                        ) {
                            Box(Modifier.padding(24.dp)) {
                                Text(
                                    question.question,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 32.sp
                                    )
                                )
                                if (showPopup) {
                                    Text(
                                        popupText,
                                        color = popupColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        question.options.forEachIndexed { index, option ->
                            val state = optionStates[index]
                            val borderColor = when (state) {
                                OptionState.CORRECT -> Green500
                                OptionState.WRONG -> Red500
                                OptionState.DISABLED -> OutlineVariant.copy(alpha = 0.1f)
                                OptionState.DEFAULT -> OutlineVariant.copy(alpha = 0.2f)
                            }
                            val backgroundColor = when (state) {
                                OptionState.CORRECT -> Green500.copy(alpha = 0.15f)
                                OptionState.WRONG -> Red500.copy(alpha = 0.15f)
                                else -> SurfaceContainer
                            }
                            val letterBackground = when (state) {
                                OptionState.CORRECT -> Green500
                                OptionState.WRONG -> Red500
                                else -> SurfaceContainerHighest
                            }
                            val textColor = when (state) {
                                OptionState.CORRECT -> Green500
                                OptionState.WRONG -> Red500
                                OptionState.DISABLED -> OnSurfaceVariant.copy(alpha = 0.5f)
                                OptionState.DEFAULT -> OnSurface
                            }

                            Card(
                                onClick = { if (!answered) selectAnswer(index) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                                border = BorderStroke(1.dp, borderColor),
                                enabled = state == OptionState.DEFAULT
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = letterBackground,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                letters[index],
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (state == OptionState.CORRECT || state == OptionState.WRONG) {
                                                    Color.White
                                                } else {
                                                    Primary
                                                }
                                            )
                                        }
                                    }
                                    Text(
                                        option,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = textColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LightningQuizPhase.RESULTS -> {
                val accuracy = if (answeredQuestions > 0) (correct * 100) / answeredQuestions else 0
                val resultTitle = when {
                    accuracy >= 90 -> "Legendary!"
                    accuracy >= 70 -> "Electrifying!"
                    accuracy >= 50 -> "Good Speed!"
                    else -> "Keep Training!"
                }
                val resultSubtitle = when {
                    accuracy >= 90 -> "Lightning fast and razor sharp."
                    accuracy >= 70 -> "Speed and accuracy win rounds."
                    accuracy >= 50 -> "You answered $answeredQuestions questions before time ran out."
                    else -> "Speed comes with practice."
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.height(32.dp))
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(100.dp),
                        color = Color.Transparent,
                        border = BorderStroke(2.dp, if (accuracy >= 50) Cyan400 else Error)
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            if (accuracy >= 50) Cyan400.copy(alpha = 0.2f) else Error.copy(alpha = 0.2f),
                                            Purple500.copy(alpha = 0.2f)
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (accuracy >= 50) Icons.Filled.EmojiEvents else Icons.Filled.SentimentDissatisfied,
                                null,
                                tint = if (accuracy >= 50) Cyan400 else Error,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Text(resultTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(resultSubtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, textAlign = TextAlign.Center)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("$score", "Points", Primary),
                            Triple("$correct/$answeredQuestions", "Correct", Secondary),
                            Triple("$accuracy%", "Accuracy", Tertiary)
                        ).forEach { (value, label, color) ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        value,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
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
                    Button(
                        onClick = { startQuiz() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        enabled = !quizCooldown.active,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(listOf(Primary, PrimaryDim)),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Replay, null, tint = Color.White)
                                Text(
                                    "Play Again",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Back to Home")
                    }
                }
            }
        }

        if (isLoadingQuestions) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Surface.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Amber600)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Loading Questions...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurface
                    )
                }
            }
        }
    }
}
