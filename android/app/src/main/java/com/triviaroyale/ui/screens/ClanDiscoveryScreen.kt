package com.triviaroyale.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.triviaroyale.firebase.ClanSearchResult
import com.triviaroyale.firebase.FirebaseCloudRepository
import com.triviaroyale.ui.theme.OnSurface
import com.triviaroyale.ui.theme.OnSurfaceVariant
import com.triviaroyale.ui.theme.Primary
import com.triviaroyale.ui.theme.Purple500
import com.triviaroyale.ui.theme.Secondary
import com.triviaroyale.ui.theme.Surface
import com.triviaroyale.ui.theme.SurfaceContainerHigh
import com.triviaroyale.analytics.ClanAnalytics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClanDiscoveryScreen(
    cloudRepository: FirebaseCloudRepository,
    navController: NavController,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var clans by remember { mutableStateOf<List<ClanSearchResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isWorking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    var createName by remember { mutableStateOf("") }
    var createTag by remember { mutableStateOf("") }
    var createDescription by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
        focusedBorderColor = Color.White,
        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
        focusedLabelColor = Color.White,
        cursorColor = Color.White,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White.copy(alpha = 0.9f),
    )

    suspend fun refresh() {
        isLoading = true
        runCatching {
            clans = cloudRepository.searchClans(query)
            errorMessage = null
        }.onFailure { error ->
            errorMessage = error.message ?: "Could not load clans."
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        refresh()
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
                Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = OnSurface)
            }
            Text("Clan Discovery", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        }

        // ── Success Banner ──────────────────────────────────
        AnimatedVisibility(
            visible = successMessage != null,
            enter = fadeIn() + slideInVertically { -it }
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF22c55e).copy(alpha = 0.15f),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF22c55e), modifier = Modifier.size(20.dp))
                    Text(
                        successMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // ── Create Clan (Primary CTA) ─────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF0ea5e9), Color(0xFF6366f1))),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Column {
                            Text("Create Your Clan", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
                            Text("Build a team, compete in wars, and climb the ladder.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                    OutlinedTextField(
                        value = createName,
                        onValueChange = { createName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Clan Name (min 3 characters)") },
                        colors = textFieldColors,
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = createTag,
                        onValueChange = { createTag = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tag (e.g. TRV, min 2 characters)") },
                        colors = textFieldColors,
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = createDescription,
                        onValueChange = { createDescription = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Description (optional)") },
                        colors = textFieldColors,
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                isWorking = true
                                errorMessage = null
                                successMessage = null
                                runCatching {
                                    cloudRepository.createClan(createName, createTag, createDescription, "shield")
                                }.onSuccess { clanId ->
                                    ClanAnalytics.logClanCreated(createName, createTag)
                                    successMessage = "Clan created! Opening your War HQ..."
                                    // Small delay so the user sees the success message
                                    delay(800)
                                    navController.navigate("clans?tab=war") {
                                        popUpTo("clans?tab={tab}") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }.onFailure { error ->
                                    errorMessage = error.message ?: "Could not create clan."
                                }
                                isWorking = false
                            }
                        },
                        enabled = !isWorking && createName.isNotBlank() && createTag.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    ) {
                        if (isWorking) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Primary, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Creating...", color = Primary, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Create and Open War", color = Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Join Existing Clan ───────────────────────────────
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
                    .padding(22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Groups, contentDescription = null, tint = Color.White)
                        Text("Join Existing Clan", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Battle-phase clans are listed first so the fastest path lands you in a live war board.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                }
            }
        }

        // ── Search Bar ───────────────────────────────────────
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                scope.launch { refresh() }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search clans by name or tag") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = OnSurfaceVariant) },
            singleLine = true,
        )

        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Loading clans...", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }

        errorMessage?.let { message ->
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFef4444).copy(alpha = 0.12f)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurface,
                    )
                    // If user sees "Leave your current clan first", offer a leave button
                    if (message.contains("Leave your current clan", ignoreCase = true) ||
                        message.contains("already in", ignoreCase = true)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isWorking = true
                                    errorMessage = null
                                    runCatching { cloudRepository.leaveClan() }
                                        .onSuccess {
                                            successMessage = "You left your previous clan. You can now create a new one!"
                                        }
                                        .onFailure { error ->
                                            errorMessage = error.message ?: "Could not leave clan."
                                        }
                                    isWorking = false
                                }
                            },
                            enabled = !isWorking,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFef4444)),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Leave Current Clan", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Clan List ────────────────────────────────────────
        if (clans.isEmpty() && !isLoading) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(40.dp))
                    Text("No clans found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Be the first! Create a clan above and start competing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                    )
                }
            }
        }

        clans.forEach { clan ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("${clan.name} · ${clan.tag}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${clan.memberCount}/30 members · ${clan.activeMemberCount7d} active · ${clan.activeBand}",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant,
                            )
                        }
                        if (clan.battleJoinRecommended) {
                            Surface(shape = RoundedCornerShape(12.dp), color = Primary.copy(alpha = 0.16f)) {
                                Text(
                                    "Battle Ready",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Text(clan.description.ifBlank { "A fresh clan looking for contributors." }, style = MaterialTheme.typography.bodySmall, color = OnSurface)
                    Button(
                        onClick = {
                            scope.launch {
                                isWorking = true
                                errorMessage = null
                                successMessage = null
                                runCatching { cloudRepository.joinClan(clan.id) }
                                    .onSuccess {
                                        ClanAnalytics.logClanJoined(clan.id, clan.tag)
                                        successMessage = "Joined ${clan.name}! Opening War HQ..."
                                        delay(800)
                                        navController.navigate("clans?tab=war") {
                                            popUpTo("clans?tab={tab}") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                    .onFailure { error ->
                                        errorMessage = error.message ?: "Could not join clan."
                                    }
                                isWorking = false
                            }
                        },
                        enabled = !isWorking && clan.openSlots > 0,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    ) {
                        Text("Join and Open War", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
