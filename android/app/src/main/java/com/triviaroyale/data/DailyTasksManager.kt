package com.triviaroyale.data

import android.content.Context
import kotlin.random.Random

private enum class DailyTaskMetric {
    QUIZZES_PLAYED,
    QUESTIONS_ANSWERED,
    CORRECT_ANSWERS,
    QUIZZES_WON,
    LIGHTNING_ROUNDS,
    IPL_QUIZZES,
    IPL_CORRECT_ANSWERS,
    PLAY_SECONDS,
    GRAND_MASTER_COMPLETED
}

private data class DailyTaskDefinition(
    val id: String,
    val title: String,
    val description: String,
    val metric: DailyTaskMetric,
    val target: Int,
    val rewardCoins: Int,
    val fixed: Boolean
)

data class DailyTaskProgress(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val progress: Int,
    val target: Int,
    val complete: Boolean,
    val claimed: Boolean,
    val fixed: Boolean
)

data class DailyTaskSecuritySnapshot(
    val quizzesPlayedToday: Int,
    val questionsAnsweredToday: Int,
    val correctAnswersToday: Int,
    val playSecondsToday: Int
)

class DailyTasksManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun bootstrap(state: GameState.State, allowPrefsMigration: Boolean = true) {
        val persisted = state.dailyTaskState
        val prefSnapshot = if (allowPrefsMigration) readSnapshotFromPrefs(state) else null
        val resolved = resolveSnapshot(state, persisted, prefSnapshot)
        writeSnapshot(resolved, state)
        syncSnapshotIntoState(state)
    }

    fun reset(state: GameState.State) {
        writeSnapshot(freshSnapshotForToday(state), state)
        syncSnapshotIntoState(state)
    }

    fun syncSnapshotIntoState(state: GameState.State) {
        ensureToday(state)
        state.dailyTaskState = GameState.DailyTaskState(
            dayKey = prefs.getString(KEY_DAY_KEY, todayKey()),
            baseQuizzesPlayed = prefs.getInt(KEY_BASE_QUIZZES_PLAYED, state.quizzesPlayed),
            baseQuestionsAnswered = prefs.getInt(KEY_BASE_QUESTIONS, state.totalQuestionsAnswered),
            baseCorrectAnswers = prefs.getInt(KEY_BASE_CORRECT, state.correctAnswers),
            baseWins = prefs.getInt(KEY_BASE_WINS, state.quizzesWon),
            dailyChallengePlayed = prefs.getBoolean(KEY_DAILY_CHALLENGE_PLAYED, false),
            lightningRounds = prefs.getInt(KEY_LIGHTNING_ROUNDS, 0),
            iplQuizzes = prefs.getInt(KEY_IPL_QUIZZES, 0),
            iplCorrectAnswers = prefs.getInt(KEY_IPL_CORRECT, 0),
            playSeconds = prefs.getInt(KEY_PLAY_SECONDS, 0),
            claimedTaskIds = claimedTaskIds().toMutableList()
        )
    }

    fun canPlayDailyChallenge(state: GameState.State): Boolean {
        ensureToday(state)
        return !prefs.getBoolean(KEY_DAILY_CHALLENGE_PLAYED, false)
    }

    fun markDailyChallengePlayed(state: GameState.State): Boolean {
        ensureToday(state)
        if (prefs.getBoolean(KEY_DAILY_CHALLENGE_PLAYED, false)) {
            return false
        }
        prefs.edit().putBoolean(KEY_DAILY_CHALLENGE_PLAYED, true).apply()
        return true
    }

    fun recordLightningRoundCompleted(state: GameState.State) {
        ensureToday(state)
        val current = prefs.getInt(KEY_LIGHTNING_ROUNDS, 0)
        prefs.edit().putInt(KEY_LIGHTNING_ROUNDS, current + 1).apply()
    }

    fun recordGenreQuizCompleted(state: GameState.State, genre: String, correctAnswers: Int) {
        ensureToday(state)
        if (genre != "IPL") {
            return
        }
        val currentQuizzes = prefs.getInt(KEY_IPL_QUIZZES, 0)
        val currentCorrect = prefs.getInt(KEY_IPL_CORRECT, 0)
        prefs.edit()
            .putInt(KEY_IPL_QUIZZES, currentQuizzes + 1)
            .putInt(KEY_IPL_CORRECT, currentCorrect + correctAnswers)
            .apply()
    }

    fun recordPlaySeconds(state: GameState.State, seconds: Int = 1) {
        if (seconds <= 0) {
            return
        }
        ensureToday(state)
        val current = prefs.getInt(KEY_PLAY_SECONDS, 0)
        prefs.edit().putInt(KEY_PLAY_SECONDS, current + seconds).apply()
    }

    fun getTasks(state: GameState.State): List<DailyTaskProgress> {
        ensureToday(state)
        val claimed = claimedTaskIds()
        return definitionsForToday().map { definition ->
            val progress = progressFor(definition.metric, state).coerceAtLeast(0)
            DailyTaskProgress(
                id = definition.id,
                title = definition.title,
                description = definition.description,
                rewardCoins = definition.rewardCoins,
                progress = progress.coerceAtMost(definition.target),
                target = definition.target,
                complete = progress >= definition.target,
                claimed = definition.id in claimed,
                fixed = definition.fixed
            )
        }
    }

    fun claimTask(
        state: GameState.State,
        taskId: String,
        rewardCoins: Int? = null,
        onReward: (Int, String) -> Unit
    ): Boolean {
        ensureToday(state)
        val task = getTasks(state).firstOrNull { it.id == taskId } ?: return false
        if (!task.complete || task.claimed) {
            return false
        }

        val claimed = claimedTaskIds().toMutableSet()
        claimed += taskId
        prefs.edit().putString(KEY_CLAIMED_TASK_IDS, claimed.joinToString(",")).apply()
        val resolvedReward = rewardCoins ?: task.rewardCoins
        val rewardReason = if (resolvedReward > task.rewardCoins) {
            "Task Reward (50% Bonus): ${task.title}"
        } else {
            "Task Reward: ${task.title}"
        }
        onReward(resolvedReward, rewardReason)
        return true
    }

    fun buildSecuritySnapshot(state: GameState.State): DailyTaskSecuritySnapshot {
        ensureToday(state)
        return DailyTaskSecuritySnapshot(
            quizzesPlayedToday = progressFor(DailyTaskMetric.QUIZZES_PLAYED, state).coerceAtLeast(0),
            questionsAnsweredToday = progressFor(DailyTaskMetric.QUESTIONS_ANSWERED, state).coerceAtLeast(0),
            correctAnswersToday = progressFor(DailyTaskMetric.CORRECT_ANSWERS, state).coerceAtLeast(0),
            playSecondsToday = progressFor(DailyTaskMetric.PLAY_SECONDS, state).coerceAtLeast(0)
        )
    }

    private fun ensureToday(state: GameState.State) {
        val today = todayKey()
        if (prefs.getString(KEY_DAY_KEY, null) == today) {
            return
        }

        prefs.edit()
            .putString(KEY_DAY_KEY, today)
            .putInt(KEY_BASE_QUIZZES_PLAYED, state.quizzesPlayed)
            .putInt(KEY_BASE_QUESTIONS, state.totalQuestionsAnswered)
            .putInt(KEY_BASE_CORRECT, state.correctAnswers)
            .putInt(KEY_BASE_WINS, state.quizzesWon)
            .putBoolean(KEY_DAILY_CHALLENGE_PLAYED, false)
            .putInt(KEY_LIGHTNING_ROUNDS, 0)
            .putInt(KEY_IPL_QUIZZES, 0)
            .putInt(KEY_IPL_CORRECT, 0)
            .putInt(KEY_PLAY_SECONDS, 0)
            .putString(KEY_CLAIMED_TASK_IDS, "")
            .apply()
    }

    private fun progressFor(metric: DailyTaskMetric, state: GameState.State): Int {
        return when (metric) {
            DailyTaskMetric.QUIZZES_PLAYED ->
                state.quizzesPlayed - prefs.getInt(KEY_BASE_QUIZZES_PLAYED, state.quizzesPlayed)
            DailyTaskMetric.QUESTIONS_ANSWERED ->
                state.totalQuestionsAnswered - prefs.getInt(KEY_BASE_QUESTIONS, state.totalQuestionsAnswered)
            DailyTaskMetric.CORRECT_ANSWERS ->
                state.correctAnswers - prefs.getInt(KEY_BASE_CORRECT, state.correctAnswers)
            DailyTaskMetric.QUIZZES_WON ->
                state.quizzesWon - prefs.getInt(KEY_BASE_WINS, state.quizzesWon)
            DailyTaskMetric.LIGHTNING_ROUNDS -> prefs.getInt(KEY_LIGHTNING_ROUNDS, 0)
            DailyTaskMetric.IPL_QUIZZES -> prefs.getInt(KEY_IPL_QUIZZES, 0)
            DailyTaskMetric.IPL_CORRECT_ANSWERS -> prefs.getInt(KEY_IPL_CORRECT, 0)
            DailyTaskMetric.PLAY_SECONDS -> prefs.getInt(KEY_PLAY_SECONDS, 0)
            DailyTaskMetric.GRAND_MASTER_COMPLETED -> if (state.grandMasterLastPlayedDayKey == todayKey()) 1 else 0
        }
    }

    private fun claimedTaskIds(): Set<String> {
        return prefs.getString(KEY_CLAIMED_TASK_IDS, "")
            .orEmpty()
            .split(",")
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
    }

    private fun definitionsForToday(): List<DailyTaskDefinition> {
        val fixedTasks = listOf(
            DailyTaskDefinition(
                id = "grand-master-daily",
                title = "Grand Master Run",
                description = "Play today's Grand Master quiz once.",
                metric = DailyTaskMetric.GRAND_MASTER_COMPLETED,
                target = 1,
                rewardCoins = 150,
                fixed = true
            ),
            DailyTaskDefinition(
                id = "play-30-mins",
                title = "Thirty-Minute Focus",
                description = "Spend 30 minutes actively answering questions today.",
                metric = DailyTaskMetric.PLAY_SECONDS,
                target = 1_800,
                rewardCoins = 200,
                fixed = true
            )
        )

        val randomPool = mutableListOf(
            DailyTaskDefinition("quiz-sprint", "Warm-Up Circuit", "Finish 25 quizzes today.", DailyTaskMetric.QUIZZES_PLAYED, 25, 120, false),
            DailyTaskDefinition("answer-storm", "Answer Storm", "Answer 25 questions today.", DailyTaskMetric.QUESTIONS_ANSWERED, 25, 100, false),
            DailyTaskDefinition("precision-pass", "Precision Pass", "Get 100 answers correct today.", DailyTaskMetric.CORRECT_ANSWERS, 100, 150, false),
            DailyTaskDefinition("steady-hand", "Steady Hand", "Win 5 quizzes today.", DailyTaskMetric.QUIZZES_WON, 5, 130, false),
            DailyTaskDefinition("flash-tap", "Flash Tap", "Finish 1 Lightning Round today.", DailyTaskMetric.LIGHTNING_ROUNDS, 1, 90, false),
            DailyTaskDefinition("hour-grind", "Hour Grind", "Answer 100 questions today.", DailyTaskMetric.QUESTIONS_ANSWERED, 100, 100, false)
        )

        val seed = todayKey().hashCode().toLong()
        val seasonalTasks = if (isIplSeasonActive()) {
            listOf(
                DailyTaskDefinition("ipl-double-header", "Powerplay Double Header", "Finish 20 IPL quizzes today.", DailyTaskMetric.IPL_QUIZZES, 20, 100, false),
                DailyTaskDefinition("ipl-scoreboard", "Boundary Hunter", "Get 30 correct answers in IPL quizzes today.", DailyTaskMetric.IPL_CORRECT_ANSWERS, 30, 110, false)
            ).shuffled(Random(seed + 77L)).take(2)
        } else {
            emptyList()
        }
        val generalRandomCount = if (isIplSeasonActive()) 2 else 4
        val randomTasks = randomPool.shuffled(Random(seed)).take(generalRandomCount) + seasonalTasks
        return fixedTasks + randomTasks
    }

    private fun todayKey(): String = IndiaTime.formatDateKey()

    private fun resolveSnapshot(
        state: GameState.State,
        persisted: GameState.DailyTaskState?,
        prefSnapshot: GameState.DailyTaskState?
    ): GameState.DailyTaskState {
        val today = todayKey()
        val normalizedPersisted = persisted?.takeIf(::hasSnapshot)
        val normalizedPrefs = prefSnapshot?.takeIf(::hasSnapshot)

        return when {
            normalizedPersisted?.dayKey == today && normalizedPrefs?.dayKey == today ->
                mergeSnapshots(normalizedPersisted, normalizedPrefs)

            normalizedPersisted?.dayKey == today -> normalizedPersisted.copy(
                claimedTaskIds = normalizedPersisted.claimedTaskIds.orEmpty().toMutableList()
            )

            normalizedPrefs?.dayKey == today -> normalizedPrefs.copy(
                claimedTaskIds = normalizedPrefs.claimedTaskIds.orEmpty().toMutableList()
            )

            else -> freshSnapshotForToday(state)
        }
    }

    private fun hasSnapshot(snapshot: GameState.DailyTaskState): Boolean {
        return !snapshot.dayKey.isNullOrBlank() ||
            snapshot.baseQuizzesPlayed > 0 ||
            snapshot.baseQuestionsAnswered > 0 ||
            snapshot.baseCorrectAnswers > 0 ||
            snapshot.baseWins > 0 ||
            snapshot.dailyChallengePlayed ||
            snapshot.lightningRounds > 0 ||
            snapshot.iplQuizzes > 0 ||
            snapshot.iplCorrectAnswers > 0 ||
            snapshot.playSeconds > 0 ||
            snapshot.claimedTaskIds.orEmpty().isNotEmpty()
    }

    private fun readSnapshotFromPrefs(state: GameState.State): GameState.DailyTaskState? {
        if (!prefs.contains(KEY_DAY_KEY)) {
            return null
        }
        return GameState.DailyTaskState(
            dayKey = prefs.getString(KEY_DAY_KEY, null),
            baseQuizzesPlayed = prefs.getInt(KEY_BASE_QUIZZES_PLAYED, state.quizzesPlayed),
            baseQuestionsAnswered = prefs.getInt(KEY_BASE_QUESTIONS, state.totalQuestionsAnswered),
            baseCorrectAnswers = prefs.getInt(KEY_BASE_CORRECT, state.correctAnswers),
            baseWins = prefs.getInt(KEY_BASE_WINS, state.quizzesWon),
            dailyChallengePlayed = prefs.getBoolean(KEY_DAILY_CHALLENGE_PLAYED, false),
            lightningRounds = prefs.getInt(KEY_LIGHTNING_ROUNDS, 0),
            iplQuizzes = prefs.getInt(KEY_IPL_QUIZZES, 0),
            iplCorrectAnswers = prefs.getInt(KEY_IPL_CORRECT, 0),
            playSeconds = prefs.getInt(KEY_PLAY_SECONDS, 0),
            claimedTaskIds = claimedTaskIds().toMutableList()
        )
    }

    private fun mergeSnapshots(
        primary: GameState.DailyTaskState,
        secondary: GameState.DailyTaskState
    ): GameState.DailyTaskState {
        return GameState.DailyTaskState(
            dayKey = primary.dayKey ?: secondary.dayKey,
            baseQuizzesPlayed = minOf(primary.baseQuizzesPlayed, secondary.baseQuizzesPlayed),
            baseQuestionsAnswered = minOf(primary.baseQuestionsAnswered, secondary.baseQuestionsAnswered),
            baseCorrectAnswers = minOf(primary.baseCorrectAnswers, secondary.baseCorrectAnswers),
            baseWins = minOf(primary.baseWins, secondary.baseWins),
            dailyChallengePlayed = primary.dailyChallengePlayed || secondary.dailyChallengePlayed,
            lightningRounds = maxOf(primary.lightningRounds, secondary.lightningRounds),
            iplQuizzes = maxOf(primary.iplQuizzes, secondary.iplQuizzes),
            iplCorrectAnswers = maxOf(primary.iplCorrectAnswers, secondary.iplCorrectAnswers),
            playSeconds = maxOf(primary.playSeconds, secondary.playSeconds),
            claimedTaskIds = (primary.claimedTaskIds.orEmpty() + secondary.claimedTaskIds.orEmpty())
                .distinct()
                .toMutableList()
        )
    }

    private fun freshSnapshotForToday(state: GameState.State): GameState.DailyTaskState {
        return GameState.DailyTaskState(
            dayKey = todayKey(),
            baseQuizzesPlayed = state.quizzesPlayed,
            baseQuestionsAnswered = state.totalQuestionsAnswered,
            baseCorrectAnswers = state.correctAnswers,
            baseWins = state.quizzesWon,
            claimedTaskIds = mutableListOf()
        )
    }

    private fun writeSnapshot(snapshot: GameState.DailyTaskState, state: GameState.State) {
        val resolved = if (snapshot.dayKey == todayKey()) {
            snapshot
        } else {
            freshSnapshotForToday(state)
        }

        prefs.edit()
            .putString(KEY_DAY_KEY, resolved.dayKey ?: todayKey())
            .putInt(KEY_BASE_QUIZZES_PLAYED, resolved.baseQuizzesPlayed)
            .putInt(KEY_BASE_QUESTIONS, resolved.baseQuestionsAnswered)
            .putInt(KEY_BASE_CORRECT, resolved.baseCorrectAnswers)
            .putInt(KEY_BASE_WINS, resolved.baseWins)
            .putBoolean(KEY_DAILY_CHALLENGE_PLAYED, resolved.dailyChallengePlayed)
            .putInt(KEY_LIGHTNING_ROUNDS, resolved.lightningRounds)
            .putInt(KEY_IPL_QUIZZES, resolved.iplQuizzes)
            .putInt(KEY_IPL_CORRECT, resolved.iplCorrectAnswers)
            .putInt(KEY_PLAY_SECONDS, resolved.playSeconds)
            .putString(KEY_CLAIMED_TASK_IDS, resolved.claimedTaskIds.orEmpty().joinToString(","))
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "daily_tasks"
        const val KEY_DAY_KEY = "day_key"
        const val KEY_BASE_QUIZZES_PLAYED = "base_quizzes_played"
        const val KEY_BASE_QUESTIONS = "base_questions"
        const val KEY_BASE_CORRECT = "base_correct"
        const val KEY_BASE_WINS = "base_wins"
        const val KEY_DAILY_CHALLENGE_PLAYED = "daily_challenge_played"
        const val KEY_LIGHTNING_ROUNDS = "lightning_rounds"
        const val KEY_IPL_QUIZZES = "ipl_quizzes"
        const val KEY_IPL_CORRECT = "ipl_correct"
        const val KEY_PLAY_SECONDS = "play_seconds"
        const val KEY_CLAIMED_TASK_IDS = "claimed_task_ids"
    }
}
