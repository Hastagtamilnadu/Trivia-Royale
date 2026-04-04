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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.triviaroyale.data.GameState
import com.triviaroyale.firebase.FirebaseCloudRepository
import com.triviaroyale.firebase.PremiumCatalog
import com.triviaroyale.firebase.PremiumCatalogItem
import com.triviaroyale.analytics.ClanAnalytics
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

private enum class ShopTab(val label: String) {
    Style("Style"),
    Abilities("Abilities"),
    Earned("Earned"),
}

private data class AbilityItem(
    val id: String,
    val name: String,
    val description: String,
    val costCoins: Int,
)

private val nonCompetitiveAbilities = listOf(
    AbilityItem("ability_double_xp", "XP Overdrive", "2x XP for 3 days.", 2000),
    AbilityItem("ability_streak_shield", "Streak Shield", "Protects your daily streak once within 3 days.", 1800),
)

private fun displayItemName(id: String): String {
    if (id.isBlank()) return ""
    return id
        .substringAfterLast("_")
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }
}

@Composable
private fun BalanceCard(
    title: String,
    value: String,
    accent: Color,
    icon: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.55f))),
                    RoundedCornerShape(18.dp)
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(title, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelMedium)
                    Text(value, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }
                icon()
            }
        }
    }
}

@Composable
private fun PremiumItemCard(
    item: PremiumCatalogItem,
    owned: Boolean,
    equipped: Boolean,
    purchaseLabel: String,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A171E)),
        border = BorderStroke(1.dp, if (equipped) Color(0xFF8B5CF6) else Color(0xFF2D2832)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${item.type} · ${item.rarity}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFA1A1AA))
                }
                when {
                    equipped -> Text("Equipped", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                    owned -> Text("Owned", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                    else -> Text(purchaseLabel, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
                }
            }

            if (item.contents.isNotEmpty()) {
                Text(
                    "Includes: ${item.contents.joinToString { displayItemName(it) }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA1A1AA),
                )
            }

            Button(
                onClick = onAction,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            ) {
                Text(
                    when {
                        equipped -> "Unequip"
                        owned && item.type in setOf("TITLE", "FRAME", "NAMEPLATE") -> "Equip"
                        owned -> "Owned"
                        else -> "Unlock"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    gameState: GameState,
    cloudRepository: FirebaseCloudRepository,
    navController: NavController,
) {
    val gameStateSnapshot by gameState.state.collectAsState()
    val scope = rememberCoroutineScope()
    val numberFormatter = remember { NumberFormat.getIntegerInstance(Locale("en", "IN")) }

    var catalog by remember { mutableStateOf<PremiumCatalog?>(null) }
    var selectedTab by remember { mutableStateOf(ShopTab.Style) }
    var isLoading by remember { mutableStateOf(true) }
    var isWorking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun refreshCatalog() {
        isLoading = true
        runCatching {
            catalog = cloudRepository.fetchPremiumCatalog()
            message = null
        }.onFailure { error ->
            message = error.message ?: "Could not load the clan shop."
        }
        isLoading = false
    }

    LaunchedEffect(gameStateSnapshot.uid) {
        if (gameStateSnapshot.uid.isBlank()) return@LaunchedEffect
        refreshCatalog()
    }

    val catalogState = catalog
    val crowns = catalogState?.crowns ?: 0
    val fragments = catalogState?.warFragments ?: 0
    val coins = gameStateSnapshot.coins

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0A10))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
            }
            Icon(Icons.Filled.LocalMall, contentDescription = null, tint = Color(0xFFFBBF24))
            Text("Clan Shop", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
        }

        BalanceCard(
            title = "Crowns",
            value = numberFormatter.format(crowns),
            accent = Color(0xFF8B5CF6),
            icon = { Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        )
        BalanceCard(
            title = "War Fragments",
            value = numberFormatter.format(fragments),
            accent = Color(0xFF0EA5E9),
            icon = { Icon(Icons.Filled.Diamond, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        )
        BalanceCard(
            title = "Coins",
            value = numberFormatter.format(coins),
            accent = Color(0xFF10B981),
            icon = { Icon(Icons.Filled.Payments, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        )

        TabRow(selectedTabIndex = ShopTab.entries.indexOf(selectedTab)) {
            ShopTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.label, fontWeight = FontWeight.Bold) },
                    icon = {
                        Icon(
                            when (tab) {
                                ShopTab.Style -> Icons.Filled.Style
                                ShopTab.Abilities -> Icons.Filled.Bolt
                                ShopTab.Earned -> Icons.Filled.Diamond
                            },
                            contentDescription = null
                        )
                    }
                )
            }
        }

        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Loading shop...", color = Color(0xFFA1A1AA))
            }
        }

        message?.let { info ->
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF27272A)) {
                Text(info, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
        }

        when (selectedTab) {
            ShopTab.Style -> {
                catalogState?.crownPacks?.forEach { pack ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF17131D))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(pack.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("₹${pack.priceInr}", color = Color(0xFFFBBF24))
                            }
                            Button(
                                onClick = {
                                    if (isWorking) return@Button
                                    scope.launch {
                                        isWorking = true
                                        runCatching {
                                            cloudRepository.purchaseCrowns(pack.id, UUID.randomUUID().toString())
                                        }.onSuccess { newBalance ->
                                            ClanAnalytics.logCrownPackPurchased(pack.id, pack.crowns)
                                            message = "Added ${pack.crowns} Crowns. New balance: ${numberFormatter.format(newBalance)}."
                                            refreshCatalog()
                                        }.onFailure { error ->
                                            message = error.message ?: "Could not purchase Crowns."
                                        }
                                        isWorking = false
                                    }
                                },
                                enabled = !isWorking,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            ) {
                                Text("Buy", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                catalogState?.premiumItems?.forEach { item ->
                    val owned = catalogState.ownedPremiumIds.contains(item.id) || item.contents.any { catalogState.ownedPremiumIds.contains(it) }
                    val equipped = when (item.type) {
                        "TITLE" -> catalogState.equippedTitleId == item.id
                        "FRAME" -> catalogState.equippedFrameId == item.id
                        "NAMEPLATE" -> catalogState.equippedNameplateId == item.id
                        else -> false
                    }
                    PremiumItemCard(
                        item = item,
                        owned = owned,
                        equipped = equipped,
                        purchaseLabel = "${item.priceCrowns} Crowns",
                        enabled = !isWorking && (owned || crowns >= item.priceCrowns),
                        onAction = {
                            if (isWorking) return@PremiumItemCard
                            scope.launch {
                                isWorking = true
                                val isEquippable = item.type in setOf("TITLE", "FRAME", "NAMEPLATE")
                                runCatching {
                                    if (owned && isEquippable) {
                                        val slot = when (item.type) {
                                            "TITLE" -> "title"
                                            "FRAME" -> "frame"
                                            else -> "nameplate"
                                        }
                                        val target = if (equipped) null else item.id
                                        cloudRepository.equipPremiumCosmetic(slot, target)
                                        null
                                    } else {
                                        cloudRepository.purchasePremiumCosmetic(item.id)
                                    }
                                }.onSuccess {
                                    if (isEquippable) {
                                        when (item.type) {
                                            "TITLE" -> {
                                                if (equipped) gameState.unequipCosmetic(com.triviaroyale.firebase.CosmeticType.TITLE)
                                                else gameState.equipCosmetic(item.id, com.triviaroyale.firebase.CosmeticType.TITLE)
                                            }
                                            "FRAME" -> {
                                                if (equipped) gameState.unequipCosmetic(com.triviaroyale.firebase.CosmeticType.FRAME)
                                                else gameState.equipCosmetic(item.id, com.triviaroyale.firebase.CosmeticType.FRAME)
                                            }
                                        }
                                    }
                                    message = if (owned && isEquippable) {
                                        ClanAnalytics.logCosmeticEquipped(
                                            itemId = item.id,
                                            slot = when (item.type) { "TITLE" -> "title"; "FRAME" -> "frame"; else -> "nameplate" },
                                            equipped = !equipped,
                                        )
                                        if (equipped) "${item.name} unequipped." else "${item.name} equipped."
                                    } else {
                                        ClanAnalytics.logCosmeticPurchased(item.id, item.priceCrowns)
                                        "${item.name} unlocked."
                                    }
                                    refreshCatalog()
                                }.onFailure { error ->
                                    message = error.message ?: "Could not update ${item.name}."
                                }
                                isWorking = false
                            }
                        }
                    )
                }
            }

            ShopTab.Abilities -> {
                nonCompetitiveAbilities.forEach { ability ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF17131D)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(ability.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(ability.description, color = Color(0xFFA1A1AA), style = MaterialTheme.typography.bodySmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${numberFormatter.format(ability.costCoins)} coins", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = {
                                        if (isWorking) return@Button
                                        scope.launch {
                                            isWorking = true
                                            runCatching { cloudRepository.purchaseAbility(ability.id) }
                                                .onSuccess { result ->
                                                    gameState.applyValidatedCoinBalance(result.remainingCoins, false)
                                                    message = "${ability.name} activated."
                                                }
                                                .onFailure { error ->
                                                    message = error.message ?: "Could not activate ${ability.name}."
                                                }
                                            isWorking = false
                                        }
                                    },
                                    enabled = !isWorking && coins >= ability.costCoins,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                ) {
                                    Text("Activate", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            ShopTab.Earned -> {
                catalogState?.earnedItems?.forEach { item ->
                    val owned = catalogState.ownedEarnedIds.contains(item.id)
                    PremiumItemCard(
                        item = item,
                        owned = owned,
                        equipped = false,
                        purchaseLabel = "${item.priceCrowns} Fragments",
                        enabled = !isWorking && (owned || fragments >= item.priceCrowns),
                        onAction = {
                            if (owned || isWorking) return@PremiumItemCard
                            scope.launch {
                                isWorking = true
                                runCatching { cloudRepository.purchasePremiumCosmetic(item.id) }
                                    .onSuccess {
                                        message = "${item.name} redeemed from war fragments."
                                        refreshCatalog()
                                    }
                                    .onFailure { error ->
                                        message = error.message ?: "Could not redeem ${item.name}."
                                    }
                                isWorking = false
                            }
                        }
                    )
                }
                if (catalogState?.earnedItems.isNullOrEmpty()) {
                    Text("Earned cosmetics will appear here after you start claiming war chests.", color = Color(0xFFA1A1AA))
                }
            }
        }
    }
}
