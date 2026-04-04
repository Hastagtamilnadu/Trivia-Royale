package com.triviaroyale.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.triviaroyale.data.GenreLeaderboardSessionStore
import com.triviaroyale.data.Question
import com.triviaroyale.data.QuizContentManager
import com.triviaroyale.data.QuizRepository
import com.triviaroyale.firebase.FirebaseCloudRepository
import com.triviaroyale.firebase.GameplaySessionRequest
import com.triviaroyale.ui.components.AppProgressBar
import com.triviaroyale.ui.components.QuizLaunchCooldownBar
import com.triviaroyale.ui.components.RewardedSkipWaitButton
import com.triviaroyale.ui.theme.Amber700
import com.triviaroyale.ui.theme.Cyan400
import com.triviaroyale.ui.theme.Error
import com.triviaroyale.ui.theme.Green400
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
import com.triviaroyale.ui.theme.Surface
import com.triviaroyale.ui.theme.SurfaceContainer
import com.triviaroyale.ui.theme.SurfaceContainerHigh
import com.triviaroyale.ui.theme.SurfaceContainerHighest
import com.triviaroyale.ui.theme.SurfaceContainerLow
import com.triviaroyale.ui.theme.Tertiary
import com.triviaroyale.ui.theme.Yellow400
import com.triviaroyale.analytics.ClanAnalytics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class GenreQuizPhase { INTRO, QUIZ, RESULTS }

private data class GenreLandingSpec(
    val icon: ImageVector,
    val accent: Color,
    val description: String
)

private data class FeaturedQuizSpec(
    val title: String,
    val difficulty: String,
    val questionCount: Int
)

private const val GRAND_MASTER_DAILY_COUNT = 10

private fun formatGrandMasterTime(millis: Long): String {
    return SimpleDateFormat("EEE, d MMM h:mm a", Locale.getDefault()).format(Date(millis))
}

private fun formatGrandMasterCountdown(millis: Long): String {
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

private fun genreLandingSpec(genre: String): GenreLandingSpec {
    return when (genre) {
        "Grand Master Quiz" -> GenreLandingSpec(Icons.Filled.Whatshot, Primary, "A daily featured set with one attempt and optional live refreshes.")
        "Daily Challenge" -> GenreLandingSpec(Icons.Filled.Whatshot, Primary, "Today's verified skill challenge with one protected submission window.")
        "IPL" -> GenreLandingSpec(Icons.Filled.EmojiEvents, Tertiary, "Seasonal IPL quizzes with lightweight catalog-based refreshes.")
        "Sports" -> GenreLandingSpec(Icons.Filled.SportsSoccer, Orange500, "Athletes, records and iconic sporting moments.")
        "Movies" -> GenreLandingSpec(Icons.Filled.Movie, Red500, "Cinema, actors, directors and blockbusters.")
        "Science" -> GenreLandingSpec(Icons.Filled.Science, Cyan400, "Space, biology, physics and chemistry.")
        "History" -> GenreLandingSpec(Icons.Filled.HistoryEdu, Amber700, "World events, empires and turning points.")
        "Music" -> GenreLandingSpec(Icons.Filled.MusicNote, Color(0xFFF472B6), "Artists, albums, songs and genres.")
        "Tech" -> GenreLandingSpec(Icons.Filled.Computer, Color(0xFF818CF8), "Innovation, gadgets, coding and the web.")
        "Entertainment" -> GenreLandingSpec(Icons.Filled.TheaterComedy, Yellow400, "Celebrities, shows and pop moments.")
        "Geography" -> GenreLandingSpec(Icons.Filled.Public, Color(0xFF34D399), "Countries, capitals, maps and landmarks.")
        "Art" -> GenreLandingSpec(Icons.Filled.Palette, Color(0xFFFB7185), "Painting, design, sculpture and creators.")
        "Literature" -> GenreLandingSpec(Icons.Filled.MenuBook, Color(0xFF4ADE80), "Books, authors, stories and classics.")
        "General Knowledge" -> GenreLandingSpec(Icons.Filled.Quiz, Purple400, "A little bit of everything.")
        "Pop Culture" -> GenreLandingSpec(Icons.Filled.TrendingUp, Secondary, "Internet moments, fandoms and trends.")
        else -> GenreLandingSpec(Icons.Filled.School, Primary, "Challenge yourself with curated trivia.")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreQuizScreen(
    genre: String,
    gameState: GameState,
    cloudRepository: FirebaseCloudRepository,
    quizInterstitialManager: QuizInterstitialManager,
    quizRewardedAdManager: QuizRewardedAdManager,
    navController: NavController,
    autoStart: Boolean = false,
    returnToWar: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.findActivity()
    val gameSnapshot by gameState.state.collectAsState()
    val isGrandMasterQuiz = genre == "Grand Master Quiz"
    val isDailyChallenge = genre == "Daily Challenge"
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val grandMasterStatus = if (isGrandMasterQuiz) gameState.getGrandMasterStatus(now) else null
    val quizCooldown = remember(now, gameSnapshot.quizLaunchCooldownUntil) {
        gameState.getQuizLaunchCooldownStatus(now)
    }
    val questionCount = when {
        isDailyChallenge -> 8
        isGrandMasterQuiz -> GRAND_MASTER_DAILY_COUNT
        else -> 10
    }
    val contentGenre = if (isDailyChallenge || isGrandMasterQuiz) "General Knowledge" else genre
    val spec = remember(genre) { genreLandingSpec(genre) }
    var categoryNames by remember(contentGenre) {
        mutableStateOf(QuizRepository.getCategoryNames(contentGenre).take(4))
    }
    val featuredQuizzes = remember(contentGenre, questionCount, categoryNames) {
        val difficulties = listOf("Casual", "Pro", "Elite", "Master")
        if (isGrandMasterQuiz) {
            listOf(
                FeaturedQuizSpec(
                    title = "Grand Master Set",
                    difficulty = "Daily Refresh",
                    questionCount = questionCount
                )
            )
        } else if (contentGenre == "IPL") {
            listOf(
                FeaturedQuizSpec(
                    title = "Featured Quiz",
                    difficulty = "Seasonal",
                    questionCount = QuizRepository.getTopPlayerCategoryQuestionCount(contentGenre)
                )
            )
        } else {
            categoryNames.mapIndexed { index, category ->
                FeaturedQuizSpec(
                    title = category,
                    difficulty = difficulties.getOrElse(index) { "Master" },
                    questionCount = QuizRepository.getQuestionCountForCategory(contentGenre, category)
                )
            }
        }
    }
    val topPlayer = remember(contentGenre) { GenreLeaderboardSessionStore.getTopPlayer(contentGenre) }

    var phase by remember(genre, autoStart) {
        mutableStateOf(if (autoStart) GenreQuizPhase.QUIZ else GenreQuizPhase.INTRO)
    }
    var selectedFeaturedQuiz by remember(genre, autoStart) { mutableStateOf<String?>(null) }
    var questions by remember(genre, autoStart) { mutableStateOf<List<Question>>(emptyList()) }
    var currentQ by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    val baseTime = 15 + gameState.getTimeExtensionSeconds()
    var timeLeft by remember { mutableIntStateOf(baseTime) }
    var optionStates by remember { mutableStateOf(List(4) { OptionState.DEFAULT }) }
    var answered by remember { mutableStateOf(false) }
    var hintUsedThisQuestion by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    var lastPts by remember { mutableIntStateOf(0) }
    var verifiedDailySessionId by remember { mutableStateOf<String?>(null) }
    var verifiedDailyAnswerLog by remember { mutableStateOf<List<Int>>(emptyList()) }
    var verifiedDailyMessage by remember { mutableStateOf<String?>(null) }
    var verifiedDailySubmissionFailed by remember { mutableStateOf(false) }
    var isSubmittingVerifiedDaily by remember { mutableStateOf(false) }
    var isRewardedSkipWorking by remember { mutableStateOf(false) }
    var isLoadingQuestions by remember { mutableStateOf(false) }
    var sessionStartedAt by remember { mutableLongStateOf(0L) }
    var activeSessionId by remember { mutableStateOf("") }
    val dailyChallengeAvailable = !isDailyChallenge || gameState.canPlayDailyChallenge()
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

    fun handleBackPress() {
        when (phase) {
            GenreQuizPhase.INTRO -> navController.popBackStack()
            GenreQuizPhase.QUIZ -> Unit
            GenreQuizPhase.RESULTS -> {
                if (autoStart) {
                    navController.popBackStack()
                } else {
                    phase = GenreQuizPhase.INTRO
                }
            }
        }
    }

    suspend fun loadQuestions(featuredQuiz: String? = selectedFeaturedQuiz): List<Question> {
        return if (isDailyChallenge) {
            QuizRepository.getDailyChallengeQuestionsSuspend(questionCount)
        } else if (isGrandMasterQuiz) {
            val latestQuestions = runCatching {
                cloudRepository.fetchDynamicQuizQuestions(
                    genre = "General Knowledge",
                    count = questionCount,
                    recentFirst = true
                ).questions
            }.getOrDefault(emptyList())
            if (latestQuestions.isNotEmpty()) {
                latestQuestions.map { question ->
                    Question(
                        question = question.question,
                        options = question.options,
                        answer = question.answer,
                        difficulty = question.difficulty
                    )
                }
            } else {
                QuizRepository.getQuestionsSuspend("General Knowledge", questionCount)
            }
        } else if (featuredQuiz != null) {
            QuizRepository.getQuestionsForCategorySuspend(genre, featuredQuiz, questionCount)
        } else {
            QuizRepository.getQuestionsSuspend(genre, questionCount)
        }
    }

    fun startQuiz(featuredQuiz: String? = selectedFeaturedQuiz) {
        if (isDailyChallenge && !dailyChallengeAvailable) {
            return
        }
        if (quizCooldown.active) {
            phase = GenreQuizPhase.INTRO
            return
        }
        if (isGrandMasterQuiz && grandMasterStatus?.available == false) {
            phase = GenreQuizPhase.INTRO
            return
        }
        if (isGrandMasterQuiz) {
            scope.launch {
                val resolvedQuestions = loadQuestions(featuredQuiz)

                selectedFeaturedQuiz = featuredQuiz
                questions = resolvedQuestions
                verifiedDailySessionId = null
                verifiedDailyAnswerLog = emptyList()
                verifiedDailyMessage = null
                verifiedDailySubmissionFailed = false
                isSubmittingVerifiedDaily = false
                currentQ = 0
                score = 0
                correct = 0
                timeLeft = baseTime
                optionStates = List(4) { OptionState.DEFAULT }
                answered = false
                showPopup = false
                lastPts = 0
                sessionStartedAt = System.currentTimeMillis()
                activeSessionId = UUID.randomUUID().toString()
                phase = GenreQuizPhase.QUIZ
            }
            return
        }
        if (isDailyChallenge) {
            scope.launch {
                runCatching { cloudRepository.startVerifiedDailyChallenge() }
                    .onSuccess { session ->
                        selectedFeaturedQuiz = featuredQuiz
                        gameState.recordDailyChallengePlayed()
                        questions = session.questions.map { remote ->
                            Question(
                                question = remote.prompt,
                                options = remote.options,
                                answer = -1,
                                difficulty = 0.5
                            )
                        }
                        verifiedDailySessionId = session.sessionId
                        verifiedDailyAnswerLog = emptyList()
                        verifiedDailyMessage = null
                        verifiedDailySubmissionFailed = false
                        isSubmittingVerifiedDaily = false
                        currentQ = 0
                        score = 0
                        correct = 0
                        timeLeft = baseTime
                        optionStates = List(4) { OptionState.DEFAULT }
                        answered = false
                        showPopup = false
                        lastPts = 0
                        sessionStartedAt = System.currentTimeMillis()
                        activeSessionId = UUID.randomUUID().toString()
                        phase = GenreQuizPhase.QUIZ
                    }
                    .onFailure {
                        verifiedDailySessionId = null
                        verifiedDailyAnswerLog = emptyList()
                        verifiedDailyMessage = "Verified daily challenge is unavailable right now."
                        verifiedDailySubmissionFailed = true
                        isSubmittingVerifiedDaily = false
                        phase = GenreQuizPhase.INTRO
                    }
            }
            return
        }
        isLoadingQuestions = true
        scope.launch {
            selectedFeaturedQuiz = featuredQuiz
            questions = loadQuestions(featuredQuiz)
            currentQ = 0
            score = 0
            correct = 0
            timeLeft = baseTime
            optionStates = List(4) { OptionState.DEFAULT }
            answered = false
            showPopup = false
            lastPts = 0
            verifiedDailySessionId = null
            verifiedDailyAnswerLog = emptyList()
            verifiedDailyMessage = null
            verifiedDailySubmissionFailed = false
            isSubmittingVerifiedDaily = false
            sessionStartedAt = System.currentTimeMillis()
            activeSessionId = UUID.randomUUID().toString()
            isLoadingQuestions = false
            phase = GenreQuizPhase.QUIZ
        }
    }

    LaunchedEffect(genre, autoStart) {
        if (autoStart && questions.isEmpty()) {
            startQuiz(null)
        }
    }

    LaunchedEffect(isGrandMasterQuiz) {
        if (isGrandMasterQuiz) {
            while (true) {
                delay(1000)
                now = System.currentTimeMillis()
            }
        }
    }

    LaunchedEffect(contentGenre) {
        if (!isDailyChallenge && !isGrandMasterQuiz) {
            runCatching {
                QuizContentManager.syncCatalog(cloudRepository)
                if (contentGenre == "IPL") {
                    val known = QuizContentManager.getKnownCategoriesForGenre(contentGenre).take(4)
                    if (known.isNotEmpty()) {
                        categoryNames = known
                        QuizContentManager.ensureGenreFresh(cloudRepository, contentGenre, known)
                    }
                } else {
                    QuizContentManager.ensureGenreFresh(cloudRepository, contentGenre, categoryNames)
                }
                Unit
            }
        }
    }

    fun recordGenreQuizOutcome(finalScore: Int, finalCorrect: Int) {
        val didWin = questions.isNotEmpty() && finalCorrect.toDouble() / questions.size >= 0.5
        gameState.recordQuizResult(
            score = finalScore,
            questionsAnswered = questions.size,
            correctCount = finalCorrect,
            isWin = didWin
        )
        gameState.addXP(finalScore * gameState.getXpMultiplier())
        gameState.recordGenreQuizCompleted(contentGenre, finalCorrect)
        if (isGrandMasterQuiz) {
            gameState.recordGrandMasterQuizCompleted()
        }
        if (!isDailyChallenge && activeSessionId.isNotBlank() && questions.isNotEmpty()) {
            val durationSeconds = if (sessionStartedAt > 0L) {
                ((System.currentTimeMillis() - sessionStartedAt) / 1000L).toInt().coerceAtLeast(1)
            } else {
                questions.size * 15
            }
            val session = GameplaySessionRequest(
                sessionId = activeSessionId,
                sessionType = if (isGrandMasterQuiz) "grand_master" else "genre",
                questionsAnswered = questions.size,
                correctAnswers = finalCorrect,
                durationSeconds = durationSeconds,
                bestCorrectStreak = finalCorrect,
                didWin = didWin,
                genre = if (isGrandMasterQuiz) "Grand Master Quiz" else genre
            )
            scope.launch {
                runCatching { cloudRepository.recordGameplaySession(session) }
            }
        }
        activeSessionId = ""
    }

    fun finishAfterQuiz() {
        if (returnToWar) {
            ClanAnalytics.logWarContributionCompleted(
                warId = "current",
                score = score,
                counted = true,
            )
        }
        phase = GenreQuizPhase.RESULTS
    }

    // Auto-navigate back to war tab after results are shown
    if (returnToWar && phase == GenreQuizPhase.RESULTS) {
        LaunchedEffect(Unit) {
            delay(3500)
            navController.navigate("clans?tab=war") {
                popUpTo("clans?tab={tab}") {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    fun submitVerifiedDailyEntryIfNeeded() {
        if (!isDailyChallenge) {
            return
        }
        val sessionId = verifiedDailySessionId ?: return
        isSubmittingVerifiedDaily = true
        verifiedDailySubmissionFailed = false
        verifiedDailyMessage = null
        scope.launch {
            runCatching {
                cloudRepository.submitVerifiedDailyChallenge(sessionId, verifiedDailyAnswerLog)
            }.onSuccess { result ->
                correct = result.correctAnswers
                score = result.correctAnswers * 10
                recordGenreQuizOutcome(score, result.correctAnswers)
                verifiedDailyMessage = result.leaderboardRank?.let { "Verified rank #$it" }
                verifiedDailySubmissionFailed = false
                isSubmittingVerifiedDaily = false
                scope.launch {
                    quizInterstitialManager.recordMatchCompleted()
                    gameState.rollForBreak(
                        lastInterstitialAdShownAt = quizInterstitialManager.getLastAdShownAtMillis(),
                        matchesSinceLastInterruption = quizInterstitialManager.getMatchesSinceLastInterruption()
                    ).also { if (it) quizInterstitialManager.recordInterruptionForBreak() }
                    quizInterstitialManager.maybeShowAfterCompletedQuiz(activity) {
                        finishAfterQuiz()
                    }
                }
            }.onFailure { error ->
                verifiedDailyMessage = error.message ?: "Verified submission could not be completed."
                verifiedDailySubmissionFailed = true
                isSubmittingVerifiedDaily = false
                scope.launch {
                    quizInterstitialManager.recordMatchCompleted()
                    gameState.rollForBreak(
                        lastInterstitialAdShownAt = quizInterstitialManager.getLastAdShownAtMillis(),
                        matchesSinceLastInterruption = quizInterstitialManager.getMatchesSinceLastInterruption()
                    ).also { if (it) quizInterstitialManager.recordInterruptionForBreak() }
                    quizInterstitialManager.maybeShowAfterCompletedQuiz(activity) {
                        finishAfterQuiz()
                    }
                }
            }
        }
    }

    BackHandler {
        handleBackPress()
    }

    LaunchedEffect(phase, currentQ, questions.size) {
        if (phase == GenreQuizPhase.QUIZ && questions.isNotEmpty() && currentQ < questions.size && !answered) {
            val questionIndex = currentQ
            timeLeft = baseTime
            while (phase == GenreQuizPhase.QUIZ && currentQ == questionIndex && timeLeft > 0 && !answered) {
                delay(1000)
                if (phase == GenreQuizPhase.QUIZ && currentQ == questionIndex && timeLeft > 0 && !answered) {
                    gameState.recordRealPlaySeconds()
                    quizInterstitialManager.recordActiveQuizSeconds()
                    gameState.recordBreakPlaySecond()
                    timeLeft--
                }
            }

            if (phase == GenreQuizPhase.QUIZ && currentQ == questionIndex && timeLeft == 0 && !answered) {
                optionStates = List(4) { OptionState.DISABLED }
                answered = true
                showPopup = false
                if (isDailyChallenge) {
                    verifiedDailyAnswerLog = verifiedDailyAnswerLog + (-1)
                }
                delay(1500)

                if (phase != GenreQuizPhase.QUIZ || currentQ != questionIndex) return@LaunchedEffect

                answered = false
                optionStates = List(4) { OptionState.DEFAULT }
                if (questionIndex + 1 < questions.size) {
                    currentQ = questionIndex + 1
                } else {
                    if (isDailyChallenge) {
                        phase = GenreQuizPhase.RESULTS
                        submitVerifiedDailyEntryIfNeeded()
                    } else {
                        recordGenreQuizOutcome(score, correct)
                        quizInterstitialManager.recordMatchCompleted()
                        gameState.rollForBreak(
                        lastInterstitialAdShownAt = quizInterstitialManager.getLastAdShownAtMillis(),
                        matchesSinceLastInterruption = quizInterstitialManager.getMatchesSinceLastInterruption()
                    ).also { if (it) quizInterstitialManager.recordInterruptionForBreak() }
                        quizInterstitialManager.maybeShowAfterCompletedQuiz(activity) {
                            finishAfterQuiz()
                        }
                    }
                }
            }
        }
    }

    fun selectAnswer(index: Int) {
        if (answered || questions.isEmpty() || currentQ >= questions.size) return
        answered = true
        val q = questions[currentQ]
        if (isDailyChallenge) {
            optionStates = List(4) { OptionState.DISABLED }
            verifiedDailyAnswerLog = verifiedDailyAnswerLog + index
            showPopup = false
        } else {
            val isCorrect = index == q.answer
            optionStates = List(4) { i ->
                when {
                    i == q.answer -> OptionState.CORRECT
                    i == index && !isCorrect -> OptionState.WRONG
                    else -> OptionState.DISABLED
                }
            }

            if (isCorrect) {
                correct++
                val timeBonus = (timeLeft.toDouble() / baseTime * 5).toInt()
                val pts = 10 + timeBonus
                score += pts
                lastPts = pts
                showPopup = true
            } else {
                showPopup = false
            }
        }

        scope.launch {
            delay(1200)
            showPopup = false
            answered = false
            hintUsedThisQuestion = false
            optionStates = List(4) { OptionState.DEFAULT }
            if (currentQ + 1 < questions.size) {
                currentQ++
            } else {
                if (isDailyChallenge) {
                    phase = GenreQuizPhase.RESULTS
                    submitVerifiedDailyEntryIfNeeded()
                } else {
                    recordGenreQuizOutcome(score, correct)
                    quizInterstitialManager.recordMatchCompleted()
                    gameState.rollForBreak(
                        lastInterstitialAdShownAt = quizInterstitialManager.getLastAdShownAtMillis(),
                        matchesSinceLastInterruption = quizInterstitialManager.getMatchesSinceLastInterruption()
                    ).also { if (it) quizInterstitialManager.recordInterruptionForBreak() }
                    quizInterstitialManager.maybeShowAfterCompletedQuiz(activity) {
                        finishAfterQuiz()
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Surface)) {
        when (phase) {
            GenreQuizPhase.INTRO -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { handleBackPress() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = OnSurface)
                        }
                        Text(
                            "Pick a featured quiz to start",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(48.dp))
                    }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .padding(24.dp)
                        ) {
                            Icon(
                                spec.icon,
                                contentDescription = null,
                                tint = spec.accent.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .size(144.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 18.dp, y = (-8).dp)
                            )

                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = spec.accent.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            when {
                                                isGrandMasterQuiz -> "DAILY FEATURED SET"
                                                isDailyChallenge -> "DAILY CHALLENGE"
                                                else -> "CATEGORY"
                                            },
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.6.sp
                                            ),
                                            color = spec.accent
                                        )
                                    }
                                    Text(
                                        genre.uppercase(),
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        spec.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = SurfaceContainer,
                                    border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.18f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = spec.accent, modifier = Modifier.size(18.dp))
                                        Text(
                                            when {
                                                isGrandMasterQuiz && grandMasterStatus?.available == false ->
                                                    "Today\'s run is complete. Come back when the timer resets."
                                                isGrandMasterQuiz ->
                                                    "Today\'s featured run starts instantly."
                                                else ->
                                                    "Featured quizzes below start instantly."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnSurfaceVariant
                                        )
                                    }
                                }
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

                    if (isGrandMasterQuiz && grandMasterStatus?.available == false) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.18f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    formatGrandMasterCountdown((grandMasterStatus.nextAvailableAt - now).coerceAtLeast(0L)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Tertiary
                                )
                                Text(
                                    "Unlocks at ${formatGrandMasterTime(grandMasterStatus.nextAvailableAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }

                    topPlayer?.let {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Top Player", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                Text(
                                    it.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    "${NumberFormat.getIntegerInstance().format(it.bestScore)} PTS | ${it.accuracy}% ACC",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = spec.accent,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Text("Featured Quizzes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    featuredQuizzes.forEachIndexed { index, quiz ->
                        val cardAccent = if (index % 2 == 0) spec.accent else Purple500
                        Card(
                            onClick = {
                                if (!isGrandMasterQuiz || grandMasterStatus?.available != false) {
                                    startQuiz(if (contentGenre == "IPL") null else quiz.title)
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.18f))
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = cardAccent.copy(alpha = 0.14f),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(spec.icon, contentDescription = null, tint = cardAccent, modifier = Modifier.size(34.dp))
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(quiz.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = cardAccent.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                quiz.difficulty,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = cardAccent
                                            )
                                        }
                                        Text("${quiz.questionCount} Qs", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                    }
                                }

                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = cardAccent)
                            }
                        }
                    }
                }
            }

            GenreQuizPhase.QUIZ -> {
                if (questions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = CircleShape, color = spec.accent.copy(alpha = 0.12f), modifier = Modifier.size(72.dp)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(spec.icon, contentDescription = null, tint = spec.accent, modifier = Modifier.size(34.dp))
                                }
                            }
                            Text(
                                if (isDailyChallenge) {
                                    "Loading daily challenge..."
                                } else {
                                    "Preparing ${selectedFeaturedQuiz ?: genre} quiz..."
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (currentQ < questions.size) {
                    val q = questions[currentQ]
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
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(genre, style = MaterialTheme.typography.titleSmall, color = spec.accent)
                                if (!selectedFeaturedQuiz.isNullOrBlank()) {
                                    Text(selectedFeaturedQuiz ?: "", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = RoundedCornerShape(50), color = SurfaceContainerHighest) {
                                    Text(
                                        if (isDailyChallenge) "Verified Run" else "Score: $score",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = spec.accent
                                    )
                                }
                                Surface(shape = RoundedCornerShape(50), color = SurfaceContainerHighest) {
                                    Text(
                                        "Time ${timeLeft}s",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (timeLeft <= 3) Error else Cyan400
                                    )
                                }
                            }
                        }

                        Text("Question ${currentQ + 1} / ${questions.size}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)

                        AppProgressBar(
                            progress = currentQ.toFloat() / questions.size,
                            modifier = Modifier.fillMaxWidth(),
                            height = 8.dp,
                            color = spec.accent,
                            trackColor = SurfaceContainerHighest
                        )

                        AppProgressBar(
                            progress = timeLeft / baseTime.toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                            height = 4.dp,
                            color = if (timeLeft <= 3) Error else Green400,
                            trackColor = SurfaceContainerHighest
                        )

                        // Active ability indicators
                        val abilityLabels = mutableListOf<Pair<String, Color>>()
                        if (gameState.getTimeExtensionSeconds() > 0) abilityLabels.add("⏱ +5s" to Cyan400)
                        if (gameState.getXpMultiplier() > 1) abilityLabels.add("⚡ 2x XP" to Yellow400)
                        if (gameState.getRpMultiplier() > 1.0) abilityLabels.add("🚀 +50% RP" to Green400)
                        if (gameState.hasHintUnlock()) abilityLabels.add("💡 Hint" to Purple400)
                        if (gameState.hasStreakShield()) abilityLabels.add("🛡 Shield" to Orange500)
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

                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (showPopup) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Green400.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Green400.copy(alpha = 0.35f)),
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                ) {
                                    Text(
                                        "+$lastPts",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = Green400,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
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
                                    q.question,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 32.sp
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // 50/50 Hint button (Hint Master ability)
                        if (gameState.hasHintUnlock() && !hintUsedThisQuestion && !answered) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        hintUsedThisQuestion = true
                                        val correctIdx = q.answer
                                        // Find wrong indices and pick 2 to eliminate
                                        val wrongIndices = (0 until q.options.size)
                                            .filter { it != correctIdx }
                                            .shuffled()
                                            .take(2)
                                        optionStates = optionStates.mapIndexed { i, state ->
                                            if (i in wrongIndices) OptionState.DISABLED else state
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = spec.accent.copy(alpha = 0.15f),
                                        contentColor = spec.accent
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "50/50 Hint",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        val letters = listOf("A", "B", "C", "D")
                        q.options.forEachIndexed { index, option ->
                            val state = optionStates[index]
                            val borderColor = when (state) {
                                OptionState.CORRECT -> Green500
                                OptionState.WRONG -> Red500
                                OptionState.DISABLED -> OutlineVariant.copy(alpha = 0.1f)
                                OptionState.DEFAULT -> OutlineVariant.copy(alpha = 0.2f)
                            }
                            val bgColor = when (state) {
                                OptionState.CORRECT -> Green500.copy(alpha = 0.15f)
                                OptionState.WRONG -> Red500.copy(alpha = 0.15f)
                                else -> SurfaceContainer
                            }
                            val letterBg = when (state) {
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
                                colors = CardDefaults.cardColors(containerColor = bgColor),
                                border = BorderStroke(1.dp, borderColor),
                                enabled = state == OptionState.DEFAULT
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(10.dp), color = letterBg, modifier = Modifier.size(40.dp)) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                letters[index],
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (state == OptionState.CORRECT || state == OptionState.WRONG) Color.White else spec.accent
                                            )
                                        }
                                    }
                                    Text(option, style = MaterialTheme.typography.bodyLarge, color = textColor, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            GenreQuizPhase.RESULTS -> {
                val pct = if (questions.isNotEmpty()) (correct * 100) / questions.size else 0
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.height(32.dp))

                    if (returnToWar) {
                        com.triviaroyale.ui.components.ContributionResultCard(
                            score = score,
                            correctAnswers = correct,
                            totalQuestions = questions.size,
                            clanName = "Your Clan",
                            counted = true,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Returning to war board…",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(100.dp),
                        color = Color.Transparent,
                        border = BorderStroke(2.dp, if (pct >= 50) Cyan400 else Error)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            if (pct >= 50) Cyan400.copy(alpha = 0.2f) else Error.copy(alpha = 0.2f),
                                            Purple500.copy(alpha = 0.2f)
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (pct >= 50) Icons.Filled.EmojiEvents else Icons.Filled.SentimentDissatisfied,
                                contentDescription = null,
                                tint = if (pct >= 50) Cyan400 else Error,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Text(
                        if (pct >= 70) "Excellent!" else if (pct >= 50) "Good Job!" else "Keep Trying!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isDailyChallenge) {
                            "Daily Challenge"
                        } else if (isGrandMasterQuiz) {
                            "Grand Master Quiz"
                        } else {
                            buildString {
                                append(genre)
                                if (!selectedFeaturedQuiz.isNullOrBlank()) {
                                    append(" - ")
                                    append(selectedFeaturedQuiz)
                                }
                                append(" Quiz")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )

                    if (isDailyChallenge && isSubmittingVerifiedDaily) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
                        ) {
                            Text(
                                "Submitting your verified run...",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface
                            )
                        }
                    }

                    if (isDailyChallenge && verifiedDailySubmissionFailed && !verifiedDailyMessage.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                            border = BorderStroke(1.dp, Error.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    verifiedDailyMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Error
                                )
                                Button(
                                    onClick = { submitVerifiedDailyEntryIfNeeded() },
                                    enabled = !isSubmittingVerifiedDaily,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Retry Submission")
                                }
                            }
                        }
                    } else if (!verifiedDailyMessage.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
                        ) {
                            Text(
                                verifiedDailyMessage.orEmpty(),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("$score", "Points", spec.accent),
                            Triple("$correct/${questions.size}", "Correct", Secondary),
                            Triple("${pct}%", "Accuracy", Tertiary)
                        ).forEach { (value, label, color) ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
                                    Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = OnSurfaceVariant)
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

                    if (!isDailyChallenge && !isGrandMasterQuiz) {
                        Button(
                            onClick = { startQuiz(selectedFeaturedQuiz) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            enabled = !quizCooldown.active,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(listOf(spec.accent, Purple500)),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Replay, contentDescription = null, tint = Color.White)
                                    Text("Play Again", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { handleBackPress() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (autoStart) "Back to Home" else "Back to Quiz List")
                    }
                }
            }
        }

        // Loading overlay while fetching dynamic questions
        if (isLoadingQuestions) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Surface.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = spec.accent)
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
