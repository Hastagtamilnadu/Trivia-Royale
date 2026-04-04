package com.triviaroyale.ui.screens

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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.triviaroyale.firebase.ClanTaskProgress
import com.triviaroyale.data.GameState
import com.triviaroyale.firebase.ClanWarState
import com.triviaroyale.firebase.ContributionLadderResult
import com.triviaroyale.firebase.ContributionLadderRow
import com.triviaroyale.firebase.FirebaseCloudRepository
import com.triviaroyale.firebase.VerifiedDailyLeaderboard
import com.triviaroyale.ui.theme.OnSurface
import com.triviaroyale.ui.theme.OnSurfaceVariant
import com.triviaroyale.ui.theme.Primary
import com.triviaroyale.ui.theme.Purple500
import com.triviaroyale.ui.theme.Secondary
import com.triviaroyale.ui.theme.Surface
import com.triviaroyale.ui.theme.SurfaceContainerHigh
import com.triviaroyale.ui.theme.SurfaceContainerHighest
import com.triviaroyale.ui.theme.SurfaceContainerLow
import com.triviaroyale.ui.theme.Tertiary
import com.triviaroyale.analytics.ClanAnalytics
import kotlinx.coroutines.launch

private enum class ClanTab(val label: String) {
    Season("Season"),
    Today("Today"),
    War("War"),
    Roster("Roster"),
}

private fun clanTabFromRoute(route: String?): ClanTab {
    return when (route?.lowercase()) {
        "today" -> ClanTab.Today
        "war" -> ClanTab.War
        "roster" -> ClanTab.Roster
        else -> ClanTab.Season
    }
}

private fun cosmeticLabel(raw: String): String {
    if (raw.isBlank()) return ""
    return raw.substringAfterLast("_").replace('_', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SeasonRowCard(
    row: ContributionLadderRow,
    isCurrentUser: Boolean,
    cap: Int?,
    onJoinClan: (() -> Unit)? = null,
) {
    val capped = cap != null && row.totalCp >= cap && row.clanId.isBlank()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) SurfaceContainerHighest else SurfaceContainerLow
        ),
        border = if (isCurrentUser) BorderStroke(1.dp, Primary.copy(alpha = 0.3f)) else null,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        buildString {
                            if (row.rank != null) append("#${row.rank} ")
                            append(row.displayName)
                            if (row.clanTag.isNotBlank()) append(" · ${row.clanTag}")
                            if (row.equippedTitleId.isNotBlank()) append(" · ${cosmeticLabel(row.equippedTitleId)}")
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Daily ${row.dailyCp} · War ${row.warCp} · Tasks ${row.taskCp}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                    )
                }
                Text(
                    "${row.totalCp} CP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Secondary,
                )
            }

            if (isCurrentUser && row.clanId.isBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Tertiary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Tertiary.copy(alpha = 0.2f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = Tertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (capped) "Solo cap reached. Join a clan to climb." else "Join a clan to unlock the full ladder.",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurface,
                        )
                        if (onJoinClan != null) {
                            Button(
                                onClick = onJoinClan,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            ) {
                                Text("Join", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WarBoardCard(
    warState: ClanWarState?,
    isLoading: Boolean,
    onPlayContribution: () -> Unit,
) {
    when {
        isLoading -> {
            SectionCard(title = "War") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Loading war board...", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
            }
        }
        warState == null -> {
            SectionCard(title = "War") {
                Text(
                    "Join a clan to unlock your war board, contribution score, and the main progression path.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
            }
        }
        else -> {
            SectionCard(title = "${warState.clanName} vs ${warState.opponentName}") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Phase", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        Text(warState.phase.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Countdown", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        Text("${warState.countdownMillis / 1000}s", fontWeight = FontWeight.Bold, color = Secondary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${warState.clanScore} · ${warState.clanTag}", fontWeight = FontWeight.Bold)
                    Text("${warState.opponentScore} · ${warState.opponentName}", fontWeight = FontWeight.Bold, color = Tertiary)
                }

                Text(
                    "Contributor cap: ${warState.contributorCap} · Your counted runs: ${warState.currentPlayerRow.countedRuns}/3",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    warState.members.take(6).forEachIndexed { index, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                buildString {
                                    append("${index + 1}. ${row.displayName}")
                                    if (row.equippedTitleId.isNotBlank()) append(" · ${cosmeticLabel(row.equippedTitleId)}")
                                },
                                fontWeight = FontWeight.Medium
                            )
                            Text("${row.countedScore} CP", color = Secondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = onPlayContribution,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Play First Contributing Quiz", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClansScreen(
    gameState: GameState,
    cloudRepository: FirebaseCloudRepository,
    navController: NavController,
    initialTab: String,
) {
    val scope = rememberCoroutineScope()
    var seasonData by remember { mutableStateOf<ContributionLadderResult?>(null) }
    var todayData by remember { mutableStateOf<VerifiedDailyLeaderboard?>(null) }
    var warState by remember { mutableStateOf<ClanWarState?>(null) }
    var clanTasks by remember { mutableStateOf<List<ClanTaskProgress>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isWarLoading by remember { mutableStateOf(false) }
    var isClaimWorking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var mvpClaimResult by remember { mutableStateOf<com.triviaroyale.firebase.WarChestClaimResult?>(null) }
    var selectedTab by remember(initialTab) { mutableStateOf(clanTabFromRoute(initialTab)) }

    suspend fun refreshSeasonAndToday() {
        isLoading = true
        errorMessage = null
        runCatching {
            val season = cloudRepository.fetchContributionLadder("season")
            val today = cloudRepository.fetchVerifiedDailyLeaderboard()
            seasonData = season
            todayData = today
            val inClan = !season.currentUserRow?.clanId.isNullOrBlank()
            if (!inClan && selectedTab == ClanTab.War) {
                selectedTab = ClanTab.Season
            }
            if (inClan && initialTab.equals("war", ignoreCase = true)) {
                selectedTab = ClanTab.War
            }
        }.onFailure { error ->
            errorMessage = error.message ?: "Could not load clan data."
        }
        isLoading = false
    }

    suspend fun refreshWar() {
        val inClan = !seasonData?.currentUserRow?.clanId.isNullOrBlank()
        if (!inClan) {
            warState = null
            clanTasks = emptyList()
            return
        }
        isWarLoading = true
        runCatching {
            warState = cloudRepository.fetchClanWarState()
            clanTasks = cloudRepository.fetchClanTasks()
        }.onFailure { error ->
            errorMessage = error.message ?: "Could not load the war board."
        }
        isWarLoading = false
    }

    LaunchedEffect(Unit) {
        refreshSeasonAndToday()
        if (initialTab.equals("war", ignoreCase = true)) {
            refreshWar()
        }
    }

    val inClan = !seasonData?.currentUserRow?.clanId.isNullOrBlank()
    val availableTabs = if (inClan) {
        listOf(ClanTab.War, ClanTab.Today, ClanTab.Season, ClanTab.Roster)
    } else {
        listOf(ClanTab.Season, ClanTab.Today)
    }
    if (selectedTab !in availableTabs) {
        selectedTab = availableTabs.first()
    }

    LaunchedEffect(selectedTab, inClan) {
        if (selectedTab == ClanTab.War && inClan) {
            refreshWar()
            warState?.let { w -> ClanAnalytics.logWarBoardViewed(w.warId, w.phase) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    .padding(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Groups, contentDescription = null, tint = Color.White)
                        Text(
                            if (inClan) "Clan HQ" else "Clans",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                        )
                    }
                    Text(
                        if (inClan) {
                            "War, Today, Season, and Roster now drive progression."
                        } else {
                            "Season is public for everyone. Join a clan to unlock the full ladder and war progression."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.86f),
                    )
                }
            }
        }

        TabRow(selectedTabIndex = availableTabs.indexOf(selectedTab)) {
            availableTabs.forEach { tab ->
                Tab(
                    selected = tab == selectedTab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.label, fontWeight = FontWeight.Bold) },
                    icon = {
                        Icon(
                            when (tab) {
                                ClanTab.Season -> Icons.Filled.Leaderboard
                                ClanTab.Today -> Icons.Filled.Whatshot
                                ClanTab.War -> Icons.Filled.Groups
                                ClanTab.Roster -> Icons.Filled.Schedule
                            },
                            contentDescription = null
                        )
                    }
                )
            }
        }

        if (!inClan && !isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF0ea5e9), Color(0xFF6366f1))),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Column {
                                Text(
                                    "Create or Join a Clan",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                )
                                Text(
                                    "Unlock wars, clan tasks, and full ladder progression.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                )
                            }
                        }
                        Button(
                            onClick = { navController.navigate("clanDiscovery") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Find or Create Clan", color = Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (isLoading) {
            SectionCard(title = "Loading") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Refreshing clan data...", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
            }
        }

        errorMessage?.let { message ->
            SectionCard(title = "Notice") {
                Text(message, style = MaterialTheme.typography.bodySmall, color = OnSurface)
            }
        }

        successMessage?.let { message ->
            SectionCard(title = "Updated") {
                Text(message, style = MaterialTheme.typography.bodySmall, color = Secondary)
            }
        }

        mvpClaimResult?.let { mvp ->
            val ws = warState
            com.triviaroyale.ui.components.MvpResultCard(
                playerName = "You",
                clanName = ws?.clanName ?: ws?.clanTag ?: "Your Clan",
                warResult = if (mvp.isWinner) "Your clan won!" else "Better luck next war.",
                coinReward = mvp.coinReward,
                fragmentReward = mvp.fragmentReward,
                isWinner = mvp.isWinner,
            )
        }

        when (selectedTab) {
            ClanTab.Season -> {
                val season = seasonData
                if (season != null) {
                    season.currentUserRow?.let { row ->
                        SectionCard(title = "Your Season Row") {
                            SeasonRowCard(
                                row = row,
                                isCurrentUser = true,
                                cap = season.metadata.cap,
                                onJoinClan = if (row.clanId.isBlank()) {
                                    { navController.navigate("clanDiscovery") }
                                } else null,
                            )
                        }
                    }
                    SectionCard(title = "Season Ladder") {
                        season.rows.forEach { row ->
                            SeasonRowCard(
                                row = row,
                                isCurrentUser = season.currentUserRow?.uid == row.uid,
                                cap = season.metadata.cap,
                                onJoinClan = null,
                            )
                        }
                    }
                }
            }

            ClanTab.Today -> {
                SectionCard(title = todayData?.title ?: "Today") {
                    val entries = todayData?.entries.orEmpty()
                    if (entries.isEmpty()) {
                        Text("No verified daily entries yet today.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    } else {
                        entries.take(20).forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        buildString {
                                            append("#${index + 1} ${entry.displayName}")
                                            if (entry.clanTag.isNotBlank()) append(" · ${entry.clanTag}")
                                            if (entry.equippedTitleId.isNotBlank()) append(" · ${cosmeticLabel(entry.equippedTitleId)}")
                                        },
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "${entry.correctAnswers} correct · ${entry.accuracy}% accuracy",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant,
                                    )
                                }
                                Text("${entry.elapsedMillis / 1000}s", color = Secondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            ClanTab.War -> {
                WarBoardCard(
                    warState = warState,
                    isLoading = isWarLoading,
                    onPlayContribution = {
                        navController.navigate("genreQuiz/General%20Knowledge?autostart=true&returnToWar=true")
                    },
                )
                if (warState != null) {
                    SectionCard(title = "Clan Tasks") {
                        if (clanTasks.isEmpty()) {
                            Text("No clan tasks available yet.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        } else {
                            clanTasks.forEach { task ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                                    border = BorderStroke(1.dp, if (task.complete && !task.claimed) Primary.copy(alpha = 0.25f) else Color.Transparent)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(task.title, fontWeight = FontWeight.Bold)
                                                Text(task.description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                            }
                                            Text("+${task.rewardCoins} / +${task.rewardCp} CP", color = Secondary, fontWeight = FontWeight.Bold)
                                        }
                                        Text("${task.progress}/${task.target}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                        if (task.claimed) {
                                            Text("Claimed", color = Secondary, fontWeight = FontWeight.Bold)
                                        } else {
                                            Button(
                                                onClick = {
                                                    if (isClaimWorking) return@Button
                                                    scope.launch {
                                                        isClaimWorking = true
                                                        errorMessage = null
                                                        successMessage = null
                                                        runCatching {
                                                            cloudRepository.claimClanTaskReward(task.id)
                                                        }.onSuccess { result ->
                                                            ClanAnalytics.logClanTaskClaimed(task.id, result.rewardCp)
                                                            successMessage = "Claimed ${task.title}: +${result.rewardCoins} coins and +${result.rewardCp} CP."
                                                            refreshSeasonAndToday()
                                                            refreshWar()
                                                        }.onFailure { error ->
                                                            errorMessage = error.message ?: "Could not claim clan task."
                                                        }
                                                        isClaimWorking = false
                                                    }
                                                },
                                                enabled = task.complete && !isClaimWorking,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                            ) {
                                                Text("Claim Task", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    SectionCard(title = "War Chest") {
                        val currentWar = warState
                        if (currentWar == null) {
                            Text("War chest is unavailable.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        } else {
                            Text(
                                when {
                                    currentWar.chestClaimed -> "Chest already claimed for this war."
                                    currentWar.canClaimChest -> "Settlement chest is ready. Claim your personal war rewards now."
                                    currentWar.result == "pending" -> "Complete a counted contribution and wait for settlement to unlock the chest."
                                    else -> "Chest not available yet."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant,
                            )
                            if (currentWar.canClaimChest) {
                                Button(
                                    onClick = {
                                        val currentWar = warState ?: return@Button
                                        if (isClaimWorking || currentWar.warId.isBlank()) return@Button
                                        scope.launch {
                                            isClaimWorking = true
                                            errorMessage = null
                                            successMessage = null
                                            runCatching {
                                                cloudRepository.claimWarChest(currentWar.warId)
                                            }.onSuccess { result ->
                                                ClanAnalytics.logWarChestClaimed(currentWar.warId, result.coinReward, result.isWinner, result.isMvp)
                                                if (result.isMvp) {
                                                    mvpClaimResult = result
                                                }
                                                successMessage = buildString {
                                                    append("War chest claimed: +${result.coinReward} coins, +${result.fragmentReward} fragments")
                                                    if (result.isWinner) append(", victory bonus included")
                                                    if (result.isMvp) append(", MVP bonus included")
                                                }
                                                refreshSeasonAndToday()
                                                refreshWar()
                                            }.onFailure { error ->
                                                errorMessage = error.message ?: "Could not claim war chest."
                                            }
                                            isClaimWorking = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                                    enabled = !isClaimWorking,
                                ) {
                                    Text("Claim War Chest", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            ClanTab.Roster -> {
                val ws = warState
                if (ws != null) {
                    SectionCard(title = "${ws.clanName} · ${ws.clanTag}") {
                        Text(
                            "Your clan has ${ws.members.size} members tracked in the current war.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        ws.members.forEachIndexed { index, member ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        buildString {
                                            append("${index + 1}. ${member.displayName}")
                                            if (member.equippedTitleId.isNotBlank()) append(" · ${cosmeticLabel(member.equippedTitleId)}")
                                        },
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                Text("${member.countedScore} CP", color = Secondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    SectionCard(title = "Roster") {
                        Text(
                            "Play a contributing quiz to see roster data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                        )
                    }
                }

                // ── Clan Management ──────────────────────────
                var showLeaveConfirm by remember { mutableStateOf(false) }
                SectionCard(title = "Clan Management") {
                    if (!showLeaveConfirm) {
                        Text(
                            "You can leave this clan at any time. If you are the only member, the clan will be disbanded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { showLeaveConfirm = true },
                            enabled = !isClaimWorking,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFef4444)),
                        ) {
                            Text("Leave Clan", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            "Are you sure? You'll have a 24-hour cooldown before you can join another clan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFef4444),
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { showLeaveConfirm = false },
                                enabled = !isClaimWorking,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighest),
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        isClaimWorking = true
                                        errorMessage = null
                                        successMessage = null
                                        runCatching { cloudRepository.leaveClan() }
                                            .onSuccess {
                                                successMessage = "You left the clan. Find or create a new one from Clan Discovery."
                                                showLeaveConfirm = false
                                                // Refresh all data — user is now clanless
                                                refreshSeasonAndToday()
                                                warState = null
                                                clanTasks = emptyList()
                                            }
                                            .onFailure { error ->
                                                errorMessage = error.message ?: "Could not leave clan."
                                            }
                                        isClaimWorking = false
                                    }
                                },
                                enabled = !isClaimWorking,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFef4444)),
                            ) {
                                if (isClaimWorking) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("Leave Clan", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
