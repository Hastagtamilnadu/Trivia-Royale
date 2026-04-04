package com.triviaroyale.ui.screens

import android.util.Log

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.triviaroyale.ads.QuizRewardedAdManager
import com.triviaroyale.ads.RewardedSkipWaitResult
import com.triviaroyale.ads.findActivity
import com.triviaroyale.data.DailyTaskProgress
import com.triviaroyale.data.GameState
import com.triviaroyale.firebase.FirebaseCloudRepository
import com.triviaroyale.ui.components.AppProgressBar
import com.triviaroyale.ui.theme.OnSurface
import com.triviaroyale.ui.theme.OnSurfaceVariant
import com.triviaroyale.ui.theme.OutlineVariant
import com.triviaroyale.ui.theme.Primary
import com.triviaroyale.ui.theme.Secondary
import com.triviaroyale.ui.theme.Surface
import com.triviaroyale.ui.theme.SurfaceContainer
import com.triviaroyale.ui.theme.SurfaceContainerHigh
import com.triviaroyale.ui.theme.SurfaceContainerHighest
import com.triviaroyale.ui.theme.Tertiary
import java.text.NumberFormat
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private fun taskProgressLabel(task: DailyTaskProgress): String {
    return if (task.id == "play-30-mins") {
        val minutesDone = task.progress / 60
        val minutesTarget = task.target / 60
        "$minutesDone/$minutesTarget min"
    } else {
        "${task.progress}/${task.target}"
    }
}

private fun sortDailyTasks(tasks: List<DailyTaskProgress>): List<DailyTaskProgress> {
    return tasks.sortedWith(
        compareBy<DailyTaskProgress>(
            {
                when {
                    it.complete && !it.claimed -> 0
                    !it.claimed -> 1
                    else -> 2
                }
            },
            { !it.fixed },
            { -it.progress },
            { it.title },
        ),
    )
}

private fun boostedRewardCoins(baseCoins: Int): Int = baseCoins * 3 / 2

@Composable
private fun TaskCard(
    task: DailyTaskProgress,
    isWorking: Boolean,
    onClaim: (DailyTaskProgress) -> Unit,
) {
    val progressFraction = (task.progress.toFloat() / task.target.toFloat()).coerceIn(0f, 1f)
    val accent = if (task.fixed) Primary else Tertiary

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.18f)),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (task.fixed) Icons.Filled.Flag else Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = Int.MAX_VALUE)
                    }
                    Text(task.description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
                Text(
                    "+${task.rewardCoins}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Secondary,
                )
            }

            Text(taskProgressLabel(task), style = MaterialTheme.typography.labelLarge, color = accent)
            AppProgressBar(
                progress = progressFraction,
                modifier = Modifier.fillMaxWidth(),
                height = 8.dp,
                color = accent,
                trackColor = SurfaceContainerHighest,
            )

            if (task.claimed) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Secondary, modifier = Modifier.size(18.dp))
                    Text("Claimed", color = Secondary, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { onClaim(task) },
                    enabled = task.complete && !isWorking,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        disabledContainerColor = SurfaceContainer,
                    ),
                ) {
                    Text(
                        if (isWorking) "Claiming..." else "Claim Reward",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdBoostClaimDialog(
    task: DailyTaskProgress,
    isWorking: Boolean,
    onClaimBase: () -> Unit,
    onClaimBoosted: () -> Unit,
    onDismiss: () -> Unit,
) {
    val baseCoins = task.rewardCoins
    val boostedCoins = boostedRewardCoins(baseCoins)
    val coinFormatter = remember { NumberFormat.getIntegerInstance() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Claim Reward",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary,
                )
                Text(
                    "Choose how to claim your reward:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Ad-boosted claim button (primary action)
                Button(
                    onClick = onClaimBoosted,
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                    ),
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "50% Bonus → ${coinFormatter.format(boostedCoins)} Coins",
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                    )
                }
                Text(
                    "Watch a short ad",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Base claim button (secondary)
                OutlinedButton(
                    onClick = onClaimBase,
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OutlineVariant),
                ) {
                    Text(
                        "Claim ${coinFormatter.format(baseCoins)} Coins",
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) {
                Text("Cancel", color = OnSurfaceVariant)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    gameState: GameState,
    cloudRepository: FirebaseCloudRepository,
    quizRewardedAdManager: QuizRewardedAdManager,
    navController: NavController,
) {
    val state by gameState.state.collectAsState()
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.findActivity()
    val coinFormatter = remember { NumberFormat.getIntegerInstance() }

    var tasks by remember { mutableStateOf<List<DailyTaskProgress>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var activeClaimTaskId by remember { mutableStateOf<String?>(null) }
    var isBoostClaimWorking by remember { mutableStateOf(false) }
    var showClaimDialog by remember { mutableStateOf<DailyTaskProgress?>(null) }

    suspend fun loadTasks() {
        if (state.uid.isBlank()) {
            tasks = emptyList()
            return
        }
        isLoading = true
        try {
            // Local task computation is the source of truth for ALL task metrics:
            // quizzes played, Grand Master completion, lightning rounds, play seconds, etc.
            // Cloud is used only for claiming rewards (server-authoritative coin balance).
            val currentTasks = gameState.getDailyTasks()
            if (currentTasks.isNotEmpty()) {
                tasks = sortDailyTasks(currentTasks)
                statusMessage = null
            } else {
                tasks = emptyList()
                statusMessage = "No daily tasks available right now."
            }
        } finally {
            isLoading = false
        }
    }

    fun performClaim(task: DailyTaskProgress, adBoosted: Boolean) {
        scope.launch {
            activeClaimTaskId = task.id
            showClaimDialog = null
            Log.d("TaskClaim", "⏳ Claiming task=${task.id} adBoosted=$adBoosted")

            // Server-authoritative coin balance — cloud claim required
            // Timeout after 10s to avoid freezing the UI
            try {
                val cloudResult = withTimeoutOrNull(10_000L) {
                    cloudRepository.claimDailyTaskReward(task.id, adBoosted)
                }
                if (cloudResult != null) {
                    // Server already added coins — set balance to server-authoritative total.
                    // Pass rewardCoins=0 so claimDailyTask only marks the task as claimed
                    // without adding coins again (avoids double-counting).
                    gameState.applyValidatedCoinBalance(cloudResult.walletCoins, false)
                    gameState.claimDailyTask(task.id, 0)
                    val boostLabel = if (cloudResult.adBoosted) " (50% Bonus!)" else ""
                    statusMessage = "${cloudResult.taskTitle} claimed! +${coinFormatter.format(cloudResult.rewardCoins)} coins$boostLabel"
                    Log.d("TaskClaim", "✅ Claimed! reward=${cloudResult.rewardCoins} wallet=${cloudResult.walletCoins} boosted=${cloudResult.adBoosted}")
                } else {
                    // Cloud call timed out after 10s
                    Log.w("TaskClaim", "⏱ Cloud claim timed out after 10s for task=${task.id}")
                    statusMessage = "Claim timed out. Please check your internet and try again."
                }
            } catch (e: Exception) {
                // Log the ACTUAL error so we can diagnose failures from Logcat
                Log.e("TaskClaim", "❌ Claim FAILED for task=${task.id}: ${e.message}", e)
                val userMessage = when {
                    e.message?.contains("already claimed", ignoreCase = true) == true ||
                        e.message?.contains("already-exists", ignoreCase = true) == true ->
                        "This reward was already claimed."
                    e.message?.contains("not complete", ignoreCase = true) == true ||
                        e.message?.contains("failed-precondition", ignoreCase = true) == true ->
                        "Task not complete yet. Keep playing!"
                    e.message?.contains("App verification", ignoreCase = true) == true ||
                        e.message?.contains("attestation", ignoreCase = true) == true ||
                        e.message?.contains("App Check", ignoreCase = true) == true ->
                        "Connecting to server… Please check your internet and try again."
                    e.message?.contains("session expired", ignoreCase = true) == true ||
                        e.message?.contains("Sign in", ignoreCase = true) == true ->
                        "Session expired. Please restart the app."
                    else -> "Could not claim reward: ${e.message ?: "Unknown error"}"
                }
                statusMessage = userMessage
            }
            loadTasks()
            activeClaimTaskId = null
        }
    }

    val hasClaimableRewards = tasks.any { it.complete && !it.claimed }

    // Reload tasks whenever game stats change (quiz completed, Grand Master played, etc.).
    LaunchedEffect(
        state.uid,
        state.quizzesPlayed,
        state.quizzesWon,
        state.correctAnswers,
        state.totalQuestionsAnswered,
        state.grandMasterLastPlayedDayKey,
        state.dailyTaskState
    ) {
        loadTasks()
    }

    LaunchedEffect(hasClaimableRewards) {
        if (hasClaimableRewards) {
            quizRewardedAdManager.preloadIfNeeded()
        }
    }

    fun claimWithBoost(task: DailyTaskProgress) {
        if (isBoostClaimWorking || activeClaimTaskId != null) {
            return
        }

        isBoostClaimWorking = true
        quizRewardedAdManager.showSkipWaitReward(activity) { result ->
            when (result) {
                RewardedSkipWaitResult.EARNED -> performClaim(task, adBoosted = true)
                RewardedSkipWaitResult.DISMISSED -> {
                    statusMessage = "Ad closed before the bonus was earned."
                }
                RewardedSkipWaitResult.NOT_READY -> {
                    statusMessage = "Bonus ad isn't ready yet. Try again in a moment."
                }
                RewardedSkipWaitResult.FAILED -> {
                    statusMessage = "Bonus ad couldn't be shown. Try again."
                }
            }
            isBoostClaimWorking = false
        }
    }

    // Ad-boost claim dialog
    showClaimDialog?.let { task ->
        AdBoostClaimDialog(
            task = task,
            isWorking = isBoostClaimWorking || activeClaimTaskId == task.id,
            onClaimBase = { performClaim(task, adBoosted = false) },
            onClaimBoosted = { claimWithBoost(task) },
            onDismiss = { showClaimDialog = null },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, null, tint = OnSurface)
            }
            Text("Daily Tasks", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // Daily coin cap info card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
        ) {
            Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Daily Coin Limit", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                    Text(
                        "Earn up to 1,000 coins",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF3B82F6),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    "with 50% bonus!",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (isLoading) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.18f)),
            ) {
                Text(
                    "Refreshing daily tasks...",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
            }
        }

        statusMessage?.let { message ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.18f)),
            ) {
                Text(
                    message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurface,
                )
            }
        }

        if (!isLoading && tasks.isEmpty() && statusMessage == null) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.18f)),
            ) {
                Text(
                    "No server-backed daily tasks are available right now.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
            }
        }

        tasks.forEachIndexed { index, task ->
            TaskCard(
                task = task,
                isWorking = activeClaimTaskId == task.id,
                onClaim = { progress ->
                    // Show ad-boost dialog instead of directly claiming
                    showClaimDialog = progress
                },
            )
            if (index < tasks.lastIndex) {
                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.08f))
            }
        }
    }
}
