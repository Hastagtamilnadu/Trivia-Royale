package com.triviaroyale.data

import android.content.Context
import com.google.gson.Gson
import com.triviaroyale.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Trivia Royale - Global State Manager
 * Keeps the public API stable while persisting state in Room and mirroring it
 * to an encrypted local backup file for cheap offline recovery.
 */
class GameState(context: Context) {
    private val appContext = context.applicationContext
    private val legacyPrefs =
        appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val database = GameDatabase.getInstance(appContext)
    private val dao = database.gameStateDao()
    private val backupManager = GameBackupManager(appContext, gson)
    private val dailyTasksManager = DailyTasksManager(appContext)

    /** In-memory counter: how many breaks triggered this app session. Resets on restart. */
    private var sessionBreakCount = 0

    companion object {
        const val DEFAULT_USERNAME = "New Player"
        const val MAX_USERNAME_LENGTH = 12
        const val MAX_DAILY_TOTAL_COINS = 1500
        const val MAX_DAILY_TASK_COINS = 1000
        const val MAX_DAILY_QUIZ_COINS = 0
        const val MAX_DAILY_TASK_CLAIMS = 10
        const val MAX_COINS_PER_CORRECT_ANSWER = 0

        // ── Break system constants ────────────────────────────────
        const val BREAK_WAIT_MILLIS = 15_000L                    // 15 second wait
        const val BREAK_MIN_CUMULATIVE_SECONDS = 6 * 60          // earliest break at 6 min
        const val BREAK_MAX_CUMULATIVE_SECONDS = 10 * 60         // guaranteed break at 10 min
        const val BREAK_ROLL_CHANCE = 0.35                       // 35% per quiz after min
        const val BREAK_AD_COLLISION_GUARD_MILLIS = 90_000L      // no break within 90s of ad

        private const val LEGACY_PREFS_NAME = "trivia_royale_state"
        private const val LEGACY_STATE_KEY = "state"

        fun sanitizeUsername(raw: String): String {
            return raw
                .replace(Regex("[^A-Za-z0-9 _]"), "")
                .trim()
                .replace(Regex("\\s+"), " ")
                .take(MAX_USERNAME_LENGTH)
        }
    }

    data class State(
        var username: String = DEFAULT_USERNAME,
        var uid: String = "U${(Math.random() * 100000).toInt()}",
        var email: String? = null,
        var photoUrl: String? = null,
        var coins: Int = 0,
        var xp: Int = 0,
        var level: Int = 1,
        var quizzesPlayed: Int = 0,
        var quizzesWon: Int = 0,
        var totalQuestionsAnswered: Int = 0,
        var correctAnswers: Int = 0,
        var streak: Int = 0,
        var lastPlayedDate: String? = null,
        var achievements: MutableList<String> = mutableListOf(),
        var lastPointsResetMonth: String? = null,
        var transactions: MutableList<Transaction> = mutableListOf(),
        var dailyTaskState: DailyTaskState? = DailyTaskState(),
        var grandMasterLastPlayedAt: Long = 0L,
        var grandMasterLastPlayedDayKey: String? = null,
        var quizLaunchCooldownUntil: Long = 0L,           // reused as break wait timer
        var quizCooldownRewardSkipUsedForUntil: Long = 0L, // legacy, kept for migration compat
        var breakCumulativeSeconds: Int = 0,               // play seconds since last break
        var breakRewardSkipUsedForUntil: Long = 0L,        // dedup guard for ad-skip
        var lastLocalSaveAt: Long = System.currentTimeMillis(),
        var lastCloudSyncAt: Long = 0L,
        var lastWithdrawalRequestedAt: Long = 0L,
        var dailyCoinDate: String? = null,
        var dailyCoinsEarned: Int = 0,
        var dailyQuizCoinsEarned: Int = 0,
        var dailyTaskCoinsEarned: Int = 0,
        var dailyTaskClaims: Int = 0,
        var localIntegrityHash: String = "",
        var ownedCosmetics: MutableList<String> = mutableListOf(),
        var equippedTitle: String? = null,
        var equippedFrame: String? = null,
        var equippedBadgeSkin: String? = null,
        var activeAbilities: MutableList<ActiveAbility> = mutableListOf()
    )

    data class ActiveAbility(
        val id: String = "",
        val effect: String = "",
        val name: String = "",
        val activatedAt: Long = 0L,
        val expiresAt: Long = 0L
    )

    data class DailyTaskState(
        var dayKey: String? = null,
        var baseQuizzesPlayed: Int = 0,
        var baseQuestionsAnswered: Int = 0,
        var baseCorrectAnswers: Int = 0,
        var baseWins: Int = 0,
        var dailyChallengePlayed: Boolean = false,
        var lightningRounds: Int = 0,
        var iplQuizzes: Int = 0,
        var iplCorrectAnswers: Int = 0,
        var playSeconds: Int = 0,
        var claimedTaskIds: MutableList<String>? = mutableListOf()
    )

    data class Transaction(
        val amount: String,
        val reason: String,
        val date: String
    )

    data class GrandMasterStatus(
        val available: Boolean,
        val lastPlayedAt: Long?,
        val nextAvailableAt: Long
    )

    data class QuizLaunchCooldownStatus(
        val active: Boolean,
        val remainingMillis: Long,
        val totalMillis: Long
    ) {
        val progress: Float
            get() = if (totalMillis <= 0L) 0f else (remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    }

    data class CoinSecuritySnapshot(
        val totalCoins: Int,
        val dayKey: String,
        val dailyCoinsEarned: Int,
        val dailyQuizCoinsEarned: Int,
        val dailyTaskCoinsEarned: Int,
        val dailyTaskClaims: Int,
        val playSecondsToday: Int,
        val quizzesPlayedToday: Int,
        val questionsAnsweredToday: Int,
        val correctAnswersToday: Int
    )

    private val _backupStatus = MutableStateFlow(backupManager.getStatus())
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refreshBackupStatus()
    }

    val currentState: State
        get() = _state.value

    private fun loadState(): State {
        val loadedState = runBlocking(Dispatchers.IO) {
            if (backupManager.canAccessSharedBackup()) {
                backupManager.migrateVisibleLegacyEmailFolders()
            }
            val roomState = dao.getState()?.toState(gson)
            val legacyState = loadLegacyState()
            val roomBackupState = if (backupManager.canAccessSharedBackup()) {
                roomState?.let { backupManager.restoreState(it.uid, it.email) }
            } else {
                null
            }
            val legacyBackupState = if (backupManager.canAccessSharedBackup()) {
                legacyState?.let { backupManager.restoreState(it.uid, it.email) }
            } else {
                null
            }
            val legacyGlobalBackupState = if (backupManager.canAccessSharedBackup()) {
                backupManager.restoreLegacyGlobalState()
            } else {
                null
            }
            val selected = selectNewestState(
                roomState,
                roomBackupState,
                legacyState,
                legacyBackupState,
                legacyGlobalBackupState
            )
            val normalized = normalizeLoadedState(selected ?: State())

            dao.upsert(GameStateEntity.fromState(normalized, gson))

            if (legacyState != null) {
                clearLegacyState()
            }

            if (backupManager.canAccessSharedBackup() && hasStableProfileIdentity(normalized)) {
                backupManager.backupState(normalized)
            }

            normalized
        }

        return loadedState
    }

    private fun selectNewestState(vararg candidates: State?): State? {
        return candidates
            .filterNotNull()
            .maxByOrNull { it.lastLocalSaveAt }
    }

    private fun loadLegacyState(): State? {
        val json = legacyPrefs.getString(LEGACY_STATE_KEY, null) ?: return null
        return runCatching { gson.fromJson(json, State::class.java) }.getOrNull()
    }

    private fun clearLegacyState() {
        legacyPrefs.edit().remove(LEGACY_STATE_KEY).apply()
    }

    private fun todayCoinKey(): String = IndiaTime.formatDateKey()

    private fun ensureCoinDay(state: State) {
        val today = todayCoinKey()
        if (state.dailyCoinDate == today) {
            return
        }
        state.dailyCoinDate = today
        state.dailyCoinsEarned = 0
        state.dailyQuizCoinsEarned = 0
        state.dailyTaskCoinsEarned = 0
        state.dailyTaskClaims = 0
    }

    private fun computeLocalIntegrityHash(state: State): String {
        val payload = buildString {
            append(state.uid); append('|')
            append(state.coins); append('|')
            append(state.dailyCoinDate.orEmpty()); append('|')
            append(state.dailyCoinsEarned); append('|')
            append(state.dailyQuizCoinsEarned); append('|')
            append(state.dailyTaskCoinsEarned); append('|')
            append(state.dailyTaskClaims); append('|')
            append(state.totalQuestionsAnswered); append('|')
            append(state.correctAnswers); append('|')
            append(state.quizzesPlayed); append('|')
            append(state.quizzesWon)
        }
        return MessageDigest.getInstance("SHA-256")
            .digest((payload + "|" + BuildConfig.BACKUP_SECRET).toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun isCoinStatePlausible(state: State): Boolean {
        val playSnapshot = dailyTasksManager.buildSecuritySnapshot(state)
        return state.coins >= 0 &&
            state.dailyCoinsEarned >= 0 &&
            state.dailyQuizCoinsEarned >= 0 &&
            state.dailyTaskCoinsEarned >= 0 &&
            state.dailyTaskClaims >= 0 &&
            state.dailyCoinsEarned == state.dailyQuizCoinsEarned + state.dailyTaskCoinsEarned &&
            state.dailyTaskClaims <= MAX_DAILY_TASK_CLAIMS &&
            state.dailyTaskCoinsEarned <= MAX_DAILY_TASK_COINS &&
            state.dailyQuizCoinsEarned <= MAX_DAILY_QUIZ_COINS &&
            state.dailyCoinsEarned <= MAX_DAILY_TOTAL_COINS &&
            playSnapshot.correctAnswersToday >= 0 &&
            playSnapshot.questionsAnsweredToday >= playSnapshot.correctAnswersToday &&
            state.dailyQuizCoinsEarned <= playSnapshot.correctAnswersToday * MAX_COINS_PER_CORRECT_ANSWER
    }

    private fun sanitizeCoinState(state: State) {
        ensureCoinDay(state)
        val expectedHash = computeLocalIntegrityHash(state.copy(localIntegrityHash = ""))
        val hasStoredHash = state.localIntegrityHash.isNotBlank()
        val hashMismatch = hasStoredHash && state.localIntegrityHash != expectedHash
        if (hashMismatch || !isCoinStatePlausible(state)) {
            state.coins = 0
            state.dailyCoinsEarned = 0
            state.dailyQuizCoinsEarned = 0
            state.dailyTaskCoinsEarned = 0
            state.dailyTaskClaims = 0
        }
        state.localIntegrityHash = computeLocalIntegrityHash(state.copy(localIntegrityHash = ""))
    }

    private fun saveState() {
        sanitizeCoinState(_state.value)
        persistState(_state.value)
    }

    private fun normalizeLoadedState(
        state: State,
        allowPrefsMigration: Boolean = true
    ): State {
        val normalized = normalizeMonthlyPointsState(state)
        dailyTasksManager.bootstrap(normalized, allowPrefsMigration)
        sanitizeCoinState(normalized)
        return normalized
    }

    private fun persistState(state: State) {
        dailyTasksManager.syncSnapshotIntoState(state)
        sanitizeCoinState(state)
        runBlocking(Dispatchers.IO) {
            dao.upsert(GameStateEntity.fromState(state, gson))
            clearLegacyState()
            if (backupManager.canAccessSharedBackup() && hasStableProfileIdentity(state)) {
                backupManager.backupState(state)
            }
        }
        refreshBackupStatus()
    }

    fun refreshBackupStatus() {
        _backupStatus.value = backupManager.getStatus(_state.value.uid, _state.value.email)
    }

    suspend fun createBackupNow(): Boolean {
        val success = withContext(Dispatchers.IO) {
            backupManager.backupState(_state.value).success
        }
        refreshBackupStatus()
        return success
    }

    suspend fun restoreFromExternalBackup(): Boolean {
        val current = _state.value
        if (!hasStableProfileIdentity(current)) {
            refreshBackupStatus()
            return false
        }

        val restoredState = withContext(Dispatchers.IO) {
            backupManager.restoreState(current.uid, current.email)
        } ?: run {
            refreshBackupStatus()
            return false
        }

        val normalized = normalizeLoadedState(restoredState, allowPrefsMigration = false)
        withContext(Dispatchers.IO) {
            dao.upsert(GameStateEntity.fromState(normalized, gson))
            clearLegacyState()
            if (backupManager.canAccessSharedBackup() && hasStableProfileIdentity(normalized)) {
                backupManager.backupState(normalized)
            }
        }
        _state.value = normalized
        refreshBackupStatus()
        return true
    }

    suspend fun activateAuthenticatedProfile(
        uid: String,
        email: String?,
        remoteDisplayName: String?,
        photoUrl: String?
    ): State {
        val currentState = _state.value.deepCopy()
        val activatedState = withContext(Dispatchers.IO) {
            val currentStateIsGuestBackup = isGuestGeneratedUid(currentState.uid) &&
                currentState.email.isNullOrBlank()

            if (!isSameProfile(currentState, uid, email) &&
                hasStableProfileIdentity(currentState) &&
                hasMeaningfulProgress(currentState) &&
                backupManager.canAccessSharedBackup()
            ) {
                backupManager.backupState(currentState)
            }

            val roomState = dao.getState()?.toState(gson)?.takeIf { isSameProfile(it, uid, email) }
            val backupState = if (backupManager.canAccessSharedBackup()) {
                backupManager.restoreState(uid, email)
            } else {
                null
            }
            val migratedLegacyGlobal = if (backupManager.canAccessSharedBackup() && backupState == null) {
                backupManager.restoreLegacyGlobalState()?.takeIf {
                    isSameProfile(it, uid, email) || !hasStableProfileIdentity(it)
                }
            } else {
                null
            }

            val selected = when {
                isSameProfile(currentState, uid, email) -> currentState
                else -> selectNewestState(backupState, roomState, migratedLegacyGlobal)
            }

            val normalized = normalizeLoadedState(
                selected?.deepCopy() ?: freshStateForProfile(uid, email, photoUrl),
                allowPrefsMigration = false
            ).apply {
                this.uid = uid
                this.email = email
                this.photoUrl = photoUrl
                if (!remoteDisplayName.isNullOrBlank()) {
                    val sanitizedRemoteName = sanitizeUsername(remoteDisplayName)
                    if (sanitizedRemoteName.isNotBlank() && needsUsernameSetup(this)) {
                        username = sanitizedRemoteName
                    }
                }
            }
            dao.upsert(GameStateEntity.fromState(normalized, gson))
            clearLegacyState()
            if (backupManager.canAccessSharedBackup()) {
                backupManager.backupState(normalized)
                if (migratedLegacyGlobal != null) {
                    backupManager.deleteLegacyGlobalBackup()
                }
                if (currentStateIsGuestBackup) {
                    backupManager.deleteBackupForProfile(currentState.uid, currentState.email)
                }
            }
            normalized
        }

        _state.value = activatedState
        refreshBackupStatus()
        return activatedState
    }

    private fun update(block: State.() -> Unit) {
        val updatedState = _state.value.deepCopy()
        applyMonthlyPointsResetIfNeeded(updatedState)
        ensureCoinDay(updatedState)
        updatedState.block()
        updatedState.localIntegrityHash = ""
        sanitizeCoinState(updatedState)
        updatedState.lastLocalSaveAt = System.currentTimeMillis()
        _state.value = updatedState
        saveState()
    }

    private enum class CoinSource {
        QUIZ,
        TASK
    }

    private fun addCoinsValidated(amount: Int, reason: String, source: CoinSource) {
        if (amount <= 0) {
            return
        }
        update {
            ensureCoinDay(this)
            val nextDailyCoins = dailyCoinsEarned + amount
            val nextQuizCoins = if (source == CoinSource.QUIZ) dailyQuizCoinsEarned + amount else dailyQuizCoinsEarned
            val nextTaskCoins = if (source == CoinSource.TASK) dailyTaskCoinsEarned + amount else dailyTaskCoinsEarned
            if (
                nextDailyCoins > MAX_DAILY_TOTAL_COINS ||
                nextQuizCoins > MAX_DAILY_QUIZ_COINS ||
                nextTaskCoins > MAX_DAILY_TASK_COINS
            ) {
                return@update
            }
            coins += amount
            dailyCoinsEarned = nextDailyCoins
            dailyQuizCoinsEarned = nextQuizCoins
            dailyTaskCoinsEarned = nextTaskCoins
            if (source == CoinSource.TASK) {
                dailyTaskClaims += 1
            }
            transactions.add(0, Transaction("+$amount", reason, todayDateString()))
            if (transactions.size > 30) {
                transactions.removeAt(transactions.lastIndex)
            }
        }
    }

    fun addCoins(amount: Int, reason: String = "Quiz Reward") {
        addCoinsValidated(amount, reason, CoinSource.QUIZ)
    }

    fun addTaskCoins(amount: Int, reason: String) {
        addCoinsValidated(amount, reason, CoinSource.TASK)
    }

    fun deductCoins(amount: Int, reason: String = "Purchase"): Boolean {
        if (_state.value.coins < amount) {
            return false
        }

        update {
            coins -= amount
            transactions.add(0, Transaction("-$amount", reason, todayDateString()))
            if (transactions.size > 30) {
                transactions.removeAt(transactions.lastIndex)
            }
        }
        return true
    }

    fun recordWithdrawalRequest(
        amount: Int,
        reason: String,
        requestedAt: Long = System.currentTimeMillis()
    ): Boolean {
        if (_state.value.coins < amount) {
            return false
        }

        update {
            coins -= amount
            lastWithdrawalRequestedAt = requestedAt
            transactions.add(0, Transaction("-$amount", reason, todayDateString()))
            if (transactions.size > 30) {
                transactions.removeAt(transactions.lastIndex)
            }
        }
        return true
    }

    fun addXP(amount: Int) {
        update {
            xp += amount
            recalculateLevel(this)
        }
    }

    private fun recalculateLevel(s: State) {
        val xp = s.xp
        var newLevel = 1
        if (s.quizzesPlayed >= 1) {
            newLevel = 2
            for (level in 3..12) {
                if (xp >= (level - 2) * 1000) {
                    newLevel = level
                } else {
                    break
                }
            }
            if (newLevel >= 12) {
                for (level in 13..20) {
                    val required = 10000 + (level - 12) * 10000
                    if (xp >= required) {
                        newLevel = level
                    } else {
                        break
                    }
                }
            }
            if (newLevel >= 20) {
                val excess = xp - 90000
                if (excess > 0) {
                    newLevel = 20 + (excess / 100000)
                }
            }
        }
        if (newLevel > s.level) {
            s.level = newLevel
        }
    }

    fun recordQuizResult(score: Int, questionsAnswered: Int, correctCount: Int, isWin: Boolean) {
        update {
            quizzesPlayed += 1
            if (isWin) {
                quizzesWon += 1
            }
            totalQuestionsAnswered += questionsAnswered
            correctAnswers += correctCount

            val today = todayString()
            if (lastPlayedDate != null) {
                val lastDate = parseDate(lastPlayedDate!!)
                val now = Calendar.getInstance()
                val diffDays = daysBetween(lastDate, now)
                if (diffDays == 1L) {
                    streak += 1
                } else if (diffDays > 1L) {
                    // Streak Shield: protect streak from reset (one-time use)
                    val shieldIdx = activeAbilities.indexOfFirst {
                        it.effect == "streak_shield" && it.expiresAt > System.currentTimeMillis()
                    }
                    if (shieldIdx >= 0) {
                        // Shield absorbs the break — streak preserved, shield consumed
                        activeAbilities.removeAt(shieldIdx)
                    } else {
                        streak = 1
                    }
                }
            } else {
                streak = 1
            }
            lastPlayedDate = today
            // NOTE: cooldown is no longer triggered per-quiz.
            // Breaks are triggered by rollForBreak() in the quiz screens.
            recalculateLevel(this)
        }
    }

    fun getAccuracy(): Int {
        val s = _state.value
        if (s.totalQuestionsAnswered == 0) {
            return 0
        }
        return ((s.correctAnswers.toDouble() / s.totalQuestionsAnswered) * 100).roundToInt()
    }

    fun getWinRate(): Int {
        val s = _state.value
        if (s.quizzesPlayed == 0) {
            return 0
        }
        return ((s.quizzesWon.toDouble() / s.quizzesPlayed) * 100).roundToInt()
    }

    private val namePool = listOf(
        "Arjun", "Priya", "Ravi", "Sneha", "Kiran", "Ananya", "Vikram", "Meera",
        "Aditya", "Kavya", "Rohan", "Ishita", "Dev", "Nisha", "Siddharth", "Pooja",
        "Rahul", "Divya", "Amit", "Shreya", "Varun", "Tanvi", "Nikhil", "Ritika",
        "Harsh", "Simran", "Kunal", "Jaya", "Akash", "Swati", "Manish", "Neha"
    )

    fun getMatchNames(count: Int): List<String> {
        val username = _state.value.username
        return namePool
            .filterNot { it.equals(username, ignoreCase = true) }
            .shuffled()
            .take(count)
    }

    fun canPlayDailyChallenge(): Boolean {
        return dailyTasksManager.canPlayDailyChallenge(_state.value)
    }

    fun recordDailyChallengePlayed(): Boolean {
        return dailyTasksManager.markDailyChallengePlayed(_state.value)
    }

    fun canPlayGrandMasterQuiz(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return getGrandMasterStatus(nowMillis).available
    }

    // ══════════════════════════════════════════════════════════════
    // BREAK SYSTEM  (replaces the old per-quiz cooldown)
    //
    //  • After 6 min cumulative play: 35% chance per quiz completion
    //  • After 10 min: guaranteed break
    //  • Break = 15 second wait OR watch ad to skip + earn 10 coins
    // ══════════════════════════════════════════════════════════════

    /**
     * Record 1 second of active quiz play toward the break timer.
     * Call this every active quiz second alongside recordActiveQuizSeconds().
     */
    fun recordBreakPlaySecond() {
        update {
            breakCumulativeSeconds += 1
        }
    }

    /**
     * Roll for a break at quiz completion. Returns true if a break was triggered.
     *
     * IMPORTANT: Only call this after a match ends, never mid-gameplay.
     *
     * Rules:
     *  1. First eligible break per session → 100% guaranteed
     *  2. Subsequent → 35% random roll after minimum threshold
     *  3. Guaranteed by 10 min regardless
     *  4. Never within 90s of an interstitial ad (collision guard)
     *  5. Never if user hasn't had 1 clean match since the last interruption
     *
     * @param lastInterstitialAdShownAt epoch millis when last interstitial was shown (0 = never).
     * @param matchesSinceLastInterruption how many matches since the last ad or break.
     */
    fun rollForBreak(
        nowMillis: Long = System.currentTimeMillis(),
        lastInterstitialAdShownAt: Long = 0L,
        matchesSinceLastInterruption: Int = 1
    ): Boolean {
        val current = _state.value
        // Already in a break? Don't stack.
        if (getBreakStatus(nowMillis).active) return false

        // 1-clean-match rule: at least 1 match must pass since any interruption
        if (matchesSinceLastInterruption < 1) return false

        // Anti-collision: suppress break if an interstitial ad was just shown
        if (lastInterstitialAdShownAt > 0L) {
            val sinceLastAd = nowMillis - lastInterstitialAdShownAt
            if (sinceLastAd < BREAK_AD_COLLISION_GUARD_MILLIS) return false
        }

        val cumSecs = current.breakCumulativeSeconds
        val shouldBreak = when {
            cumSecs >= BREAK_MAX_CUMULATIVE_SECONDS -> true            // guaranteed at 10 min
            cumSecs >= BREAK_MIN_CUMULATIVE_SECONDS -> {
                if (sessionBreakCount == 0) {
                    true                                               // first eligible → 100%
                } else {
                    Math.random() < BREAK_ROLL_CHANCE                  // subsequent → 35% roll
                }
            }
            else -> false
        }

        if (shouldBreak) {
            sessionBreakCount++
            update {
                quizLaunchCooldownUntil = nowMillis + BREAK_WAIT_MILLIS
            }
        }
        return shouldBreak
    }

    /** Whether the break wait timer is currently active. */
    fun getBreakStatus(nowMillis: Long = System.currentTimeMillis()): QuizLaunchCooldownStatus {
        val remaining = (_state.value.quizLaunchCooldownUntil - nowMillis).coerceAtLeast(0L)
        return QuizLaunchCooldownStatus(
            active = remaining > 0L,
            remainingMillis = remaining,
            totalMillis = BREAK_WAIT_MILLIS
        )
    }

    fun canLaunchQuiz(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return !getBreakStatus(nowMillis).active
    }

    // Keep the old name as an alias so callers still compile
    fun getQuizLaunchCooldownStatus(nowMillis: Long = System.currentTimeMillis()) = getBreakStatus(nowMillis)

    /** Whether the user can watch a rewarded ad to skip this break. */
    fun canUseRewardedSkipWait(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val current = _state.value
        val breakStatus = getBreakStatus(nowMillis)
        return breakStatus.active &&
            current.breakRewardSkipUsedForUntil != current.quizLaunchCooldownUntil
    }

    /** Skip the break wait (no coin reward). */
    fun consumeRewardedSkipWait(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!canUseRewardedSkipWait(nowMillis)) return false
        update {
            breakRewardSkipUsedForUntil = quizLaunchCooldownUntil
            quizLaunchCooldownUntil = nowMillis   // break ends now
            breakCumulativeSeconds = 0             // reset timer
        }
        return true
    }

    /** Skip break AND earn +10 coins. Called when rewarded ad completes. */
    fun consumeRewardedSkipWaitAndReward(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val skipped = consumeRewardedSkipWait(nowMillis)
        if (skipped) {
            addCoinsValidated(10, "Watch Ad Reward", CoinSource.TASK)
        }
        return skipped
    }

    /** Called when the 15-second break timer naturally expires. Resets the cumulative counter. */
    fun onBreakExpired() {
        update {
            breakCumulativeSeconds = 0
        }
    }

    fun getGrandMasterStatus(nowMillis: Long = System.currentTimeMillis()): GrandMasterStatus {
        val state = _state.value
        val todayKey = dayKeyFor(nowMillis)
        return GrandMasterStatus(
            available = state.grandMasterLastPlayedDayKey != todayKey,
            lastPlayedAt = state.grandMasterLastPlayedAt.takeIf { it > 0L },
            nextAvailableAt = nextMidnightMillis(nowMillis)
        )
    }

    fun recordGrandMasterQuizCompleted(completedAt: Long = System.currentTimeMillis()) {
        update {
            grandMasterLastPlayedAt = completedAt
            grandMasterLastPlayedDayKey = dayKeyFor(completedAt)
        }
    }

    fun recordLightningRoundCompleted() {
        dailyTasksManager.recordLightningRoundCompleted(_state.value)
    }

    fun recordRealPlaySeconds(seconds: Int = 1) {
        dailyTasksManager.recordPlaySeconds(_state.value, seconds)
    }

    fun recordGenreQuizCompleted(genre: String, correctAnswers: Int) {
        dailyTasksManager.recordGenreQuizCompleted(_state.value, genre, correctAnswers)
    }

    fun getDailyTasks(): List<DailyTaskProgress> {
        return dailyTasksManager.getTasks(_state.value)
    }

    fun claimDailyTask(taskId: String, rewardCoins: Int? = null): Boolean {
        return dailyTasksManager.claimTask(_state.value, taskId, rewardCoins) { amount, reason ->
            addTaskCoins(amount, reason)
        }
    }

    fun buildCoinSecuritySnapshot(): CoinSecuritySnapshot {
        val state = _state.value
        val todayMetrics = dailyTasksManager.buildSecuritySnapshot(state)
        return CoinSecuritySnapshot(
            totalCoins = state.coins,
            dayKey = state.dailyCoinDate ?: todayCoinKey(),
            dailyCoinsEarned = state.dailyCoinsEarned,
            dailyQuizCoinsEarned = state.dailyQuizCoinsEarned,
            dailyTaskCoinsEarned = state.dailyTaskCoinsEarned,
            dailyTaskClaims = state.dailyTaskClaims,
            playSecondsToday = todayMetrics.playSecondsToday,
            quizzesPlayedToday = todayMetrics.quizzesPlayedToday,
            questionsAnsweredToday = todayMetrics.questionsAnsweredToday,
            correctAnswersToday = todayMetrics.correctAnswersToday
        )
    }

    fun applyValidatedCoinBalance(coins: Int, suspicious: Boolean) {
        val normalizedCoins = coins.coerceAtLeast(0)
        if (!suspicious && _state.value.coins == normalizedCoins) {
            return
        }
        update {
            this.coins = normalizedCoins
            if (suspicious) {
                dailyCoinsEarned = 0
                dailyQuizCoinsEarned = 0
                dailyTaskCoinsEarned = 0
                dailyTaskClaims = 0
                transactions.clear()
            }
        }
    }

    fun resetCompletely() {
        val resetState = normalizeMonthlyPointsState(State())
        dailyTasksManager.reset(resetState)
        _state.value = resetState
        saveState()
    }

    fun syncVolatileProgress() {
        val current = _state.value
        val synced = current.deepCopy()
        dailyTasksManager.syncSnapshotIntoState(synced)
        if (synced.dailyTaskState != current.dailyTaskState) {
            synced.lastLocalSaveAt = System.currentTimeMillis()
            _state.value = synced
            saveState()
        }
    }

    fun setUsername(username: String) {
        val sanitized = sanitizeUsername(username)
        if (sanitized.isBlank()) {
            return
        }
        update {
            this.username = sanitized
        }
    }

    fun needsUsernameSetup(): Boolean {
        return needsUsernameSetup(_state.value)
    }

    private fun needsUsernameSetup(state: State): Boolean {
        val sanitized = sanitizeUsername(state.username)
        val placeholderNames = setOf(
            sanitizeUsername(DEFAULT_USERNAME),
            sanitizeUsername("Rookie Player")
        )
        return sanitized.isBlank() || sanitized in placeholderNames
    }

    fun applyAuthenticatedIdentity(
        uid: String,
        displayName: String?,
        email: String?,
        photoUrl: String?
    ) {
        update {
            this.uid = uid
            if (!displayName.isNullOrBlank()) {
                sanitizeUsername(displayName).takeIf { it.isNotBlank() }?.let {
                    username = it
                }
            }
            this.email = email
            this.photoUrl = photoUrl
        }
    }

    fun exportStateJson(): String = gson.toJson(_state.value)

    fun importCloudStateJson(
        stateJson: String,
        updatedAt: Long,
        uid: String,
        displayName: String?,
        email: String?,
        photoUrl: String?
    ): Boolean {
        if (stateJson.isBlank()) {
            return false
        }

        return try {
            val restoredState = gson.fromJson(stateJson, State::class.java) ?: return false
            if (!displayName.isNullOrBlank()) {
                sanitizeUsername(displayName).takeIf { it.isNotBlank() }?.let {
                    restoredState.username = it
                }
            }
            restoredState.uid = uid
            restoredState.email = email
            restoredState.photoUrl = photoUrl
            restoredState.lastCloudSyncAt = updatedAt
            if (restoredState.lastLocalSaveAt < updatedAt) {
                restoredState.lastLocalSaveAt = updatedAt
            }
            _state.value = normalizeLoadedState(restoredState, allowPrefsMigration = false)
            saveState()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun markCloudSync(updatedAt: Long) {
        update {
            lastCloudSyncAt = updatedAt
        }
    }

    fun hasMeaningfulLocalProgress(): Boolean {
        return hasMeaningfulProgress(_state.value)
    }

    private fun todayString(): String =
        SimpleDateFormat("EEE MMM dd yyyy", Locale.US).format(Date())

    private fun todayDateString(): String =
        SimpleDateFormat("M/d/yyyy", Locale.US).format(Date())

    private fun dayKeyFor(timeMillis: Long): String = IndiaTime.formatDateKey(timeMillis)

    private fun nextMidnightMillis(nowMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun currentMonthKey(): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    private fun normalizeMonthlyPointsState(state: State): State {
        if (state.lastPointsResetMonth == null) {
            state.lastPointsResetMonth = currentMonthKey()
            return state
        }
        applyMonthlyPointsResetIfNeeded(state)
        return state
    }

    private fun applyMonthlyPointsResetIfNeeded(state: State) {
        val currentMonth = currentMonthKey()
        val lastResetMonth = state.lastPointsResetMonth
        if (lastResetMonth == null) {
            state.lastPointsResetMonth = currentMonth
            return
        }
        if (lastResetMonth == currentMonth) {
            return
        }

        state.lastPointsResetMonth = currentMonth
    }

    private fun parseDate(dateStr: String): Calendar {
        val cal = Calendar.getInstance()
        try {
            cal.time =
                SimpleDateFormat("EEE MMM dd yyyy", Locale.US).parse(dateStr) ?: Date()
        } catch (_: Exception) {
            // Keep today.
        }
        return cal
    }

    private fun daysBetween(start: Calendar, end: Calendar): Long {
        val s = start.clone() as Calendar
        val e = end.clone() as Calendar
        s.set(Calendar.HOUR_OF_DAY, 0)
        s.set(Calendar.MINUTE, 0)
        s.set(Calendar.SECOND, 0)
        s.set(Calendar.MILLISECOND, 0)
        e.set(Calendar.HOUR_OF_DAY, 0)
        e.set(Calendar.MINUTE, 0)
        e.set(Calendar.SECOND, 0)
        e.set(Calendar.MILLISECOND, 0)
        return (e.timeInMillis - s.timeInMillis) / (1000 * 60 * 60 * 24)
    }

    private fun State.deepCopy(): State {
        return copy(
            achievements = achievements.toMutableList(),
            transactions = transactions.toMutableList(),
            dailyTaskState = dailyTaskState?.copy(
                claimedTaskIds = dailyTaskState?.claimedTaskIds.orEmpty().toMutableList()
            ) ?: DailyTaskState()
        )
    }

    private fun hasMeaningfulProgress(state: State): Boolean {
        return state.quizzesPlayed > 0 ||
            state.coins > 0 ||
            state.xp > 0 ||
            state.correctAnswers > 0 ||
            state.achievements.isNotEmpty() ||
            state.grandMasterLastPlayedAt > 0L
    }

    private fun hasStableProfileIdentity(state: State): Boolean {
        return !state.email.isNullOrBlank() ||
            (state.uid.isNotBlank() && !isGuestGeneratedUid(state.uid))
    }

    private fun isSameProfile(state: State, uid: String, email: String?): Boolean {
        if (state.uid.isNotBlank() && state.uid == uid) {
            return true
        }
        return !state.email.isNullOrBlank() &&
            !email.isNullOrBlank() &&
            state.email.equals(email, ignoreCase = true)
    }

    private fun freshStateForProfile(uid: String, email: String?, photoUrl: String?): State {
        return State(
            username = DEFAULT_USERNAME,
            uid = uid,
            email = email,
            photoUrl = photoUrl,
            lastLocalSaveAt = 0L
        )
    }

    private fun isGuestGeneratedUid(uid: String?): Boolean {
        return uid != null && uid.matches(Regex("^U\\d{1,5}$"))
    }

    // ── Cosmetic Reward Shop ────────────────────────────────
    fun ownsCosmetic(cosmeticId: String): Boolean {
        return currentState.ownedCosmetics.contains(cosmeticId)
    }

    fun purchaseCosmetic(cosmeticId: String, serverCoinsAfter: Int) {
        update {
            if (!ownedCosmetics.contains(cosmeticId)) {
                ownedCosmetics.add(cosmeticId)
            }
            coins = serverCoinsAfter
        }
    }

    fun equipCosmetic(cosmeticId: String, type: com.triviaroyale.firebase.CosmeticType) {
        update {
            when (type) {
                com.triviaroyale.firebase.CosmeticType.TITLE -> equippedTitle = cosmeticId
                com.triviaroyale.firebase.CosmeticType.FRAME -> equippedFrame = cosmeticId
                com.triviaroyale.firebase.CosmeticType.BADGE_SKIN -> equippedBadgeSkin = cosmeticId
                com.triviaroyale.firebase.CosmeticType.ABILITY -> { /* abilities are not equipped */ }
            }
        }
    }

    fun unequipCosmetic(type: com.triviaroyale.firebase.CosmeticType) {
        update {
            when (type) {
                com.triviaroyale.firebase.CosmeticType.TITLE -> equippedTitle = null
                com.triviaroyale.firebase.CosmeticType.FRAME -> equippedFrame = null
                com.triviaroyale.firebase.CosmeticType.BADGE_SKIN -> equippedBadgeSkin = null
                com.triviaroyale.firebase.CosmeticType.ABILITY -> { /* abilities are not equipped */ }
            }
        }
    }

    fun importOwnedCosmetics(cosmeticIds: List<String>) {
        update {
            ownedCosmetics.clear()
            ownedCosmetics.addAll(cosmeticIds)
        }
    }

    // ── Ability System ──────────────────────────────────────
    fun hasActiveAbility(effect: String): Boolean {
        val now = System.currentTimeMillis()
        return currentState.activeAbilities.any { it.effect == effect && it.expiresAt > now }
    }

    fun getRpMultiplier(): Double {
        return if (hasActiveAbility("rp_boost_50")) 1.5 else 1.0
    }

    fun getTimeExtensionSeconds(): Int {
        return if (hasActiveAbility("time_extend_5s")) 5 else 0
    }

    fun hasStreakShield(): Boolean = hasActiveAbility("streak_shield")

    fun hasHintUnlock(): Boolean = hasActiveAbility("hint_unlock")

    fun hasDoubleXp(): Boolean = hasActiveAbility("double_xp")

    fun getXpMultiplier(): Int = if (hasDoubleXp()) 2 else 1

    fun importActiveAbilities(abilities: List<ActiveAbility>) {
        update {
            activeAbilities.clear()
            activeAbilities.addAll(abilities.filter { it.expiresAt > System.currentTimeMillis() })
        }
    }

    fun addAbility(ability: ActiveAbility) {
        update {
            activeAbilities.removeAll { it.effect == ability.effect }
            activeAbilities.add(ability)
        }
    }
}
