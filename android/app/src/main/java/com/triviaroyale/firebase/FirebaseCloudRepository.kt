package com.triviaroyale.firebase

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.gson.Gson
import com.triviaroyale.data.DailyTaskProgress
import com.triviaroyale.data.GameState
import com.triviaroyale.data.Question
import com.triviaroyale.data.QuizBankOverride
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class CloudProfile(
    val uid: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String?,
    val updatedAt: Long
)

data class CloudGameBackup(
    val stateJson: String,
    val updatedAt: Long
)

data class CoinSyncResult(
    val acceptedCoins: Int,
    val suspicious: Boolean
)

data class CosmeticPurchaseResult(
    val cosmeticId: String,
    val remainingCoins: Int
)

data class DeviceRegistrationResult(
    val blocked: Boolean,
    val accountCount: Int,
    val maxAccounts: Int
)

data class QuizCategoryMetadata(
    val bankId: String,
    val genre: String,
    val category: String,
    val version: Int,
    val updatedAt: Long,
    val questionCount: Int
)

data class VerifiedDailyQuestion(
    val prompt: String,
    val options: List<String>
)

data class VerifiedDailyChallengeSession(
    val sessionId: String,
    val title: String,
    val dateKey: String,
    val questions: List<VerifiedDailyQuestion>
)

data class VerifiedDailyLeaderboardEntry(
    val uid: String?,
    val displayName: String,
    val clanTag: String,
    val equippedTitleId: String,
    val equippedFrameId: String,
    val equippedNameplateId: String,
    val correctAnswers: Int,
    val elapsedMillis: Long,
    val accuracy: Int,
    val rp: Int
)

data class VerifiedDailyLeaderboard(
    val title: String,
    val dateKey: String,
    val entries: List<VerifiedDailyLeaderboardEntry>
)

data class VerifiedDailySubmissionResult(
    val correctAnswers: Int,
    val elapsedMillis: Long,
    val bestCorrectStreak: Int,
    val leaderboardRank: Int?
)

data class OwnedCosmetics(
    val cosmeticIds: List<String>
)

data class CloudWallet(
    val uid: String,
    val coins: Int,
    val lastValidatedDayKey: String,
    val validatedEarnedToday: Int,
    val suspiciousCount: Int
)

data class GameplaySessionRequest(
    val sessionId: String,
    val sessionType: String,
    val questionsAnswered: Int,
    val correctAnswers: Int,
    val durationSeconds: Int,
    val bestCorrectStreak: Int = 0,
    val didWin: Boolean,
    val genre: String,
)

data class DailyTaskClaimResult(
    val taskId: String,
    val taskTitle: String,
    val rewardCoins: Int,
    val baseReward: Int,
    val adBoosted: Boolean,
    val walletCoins: Int
)

data class AbilityPurchaseResult(
    val abilityId: String,
    val abilityName: String,
    val effect: String,
    val expiresAt: Long,
    val remainingCoins: Int
)

data class ActiveAbilityInfo(
    val id: String,
    val effect: String,
    val name: String,
    val activatedAt: Long,
    val expiresAt: Long
)

data class ClanSearchResult(
    val id: String,
    val name: String,
    val tag: String,
    val description: String,
    val emblemId: String,
    val memberCount: Int,
    val activeMemberCount7d: Int,
    val activeBand: String,
    val openSlots: Int,
    val battleJoinRecommended: Boolean,
)

data class ContributionLadderRow(
    val rank: Int?,
    val uid: String,
    val displayName: String,
    val clanId: String,
    val clanTag: String,
    val equippedTitleId: String,
    val equippedFrameId: String,
    val equippedNameplateId: String,
    val dailyCp: Int,
    val warCp: Int,
    val taskCp: Int,
    val totalCp: Int,
    val soloCapApplied: Boolean,
)

data class ContributionLadderMetadata(
    val dateKey: String?,
    val seasonId: String?,
    val currentWarIndex: Int?,
    val cap: Int?,
)

data class ContributionLadderResult(
    val tab: String,
    val rows: List<ContributionLadderRow>,
    val currentUserRow: ContributionLadderRow?,
    val metadata: ContributionLadderMetadata,
)

data class ClanWarMemberRow(
    val uid: String,
    val displayName: String,
    val equippedTitleId: String,
    val equippedFrameId: String,
    val equippedNameplateId: String,
    val countedRuns: Int,
    val countedScore: Int,
)

data class ClanWarState(
    val warId: String,
    val clanId: String,
    val clanName: String,
    val clanTag: String,
    val phase: String,
    val countdownMillis: Long,
    val contributorCap: Int,
    val clanScore: Int,
    val opponentScore: Int,
    val opponentName: String,
    val result: String,
    val canClaimChest: Boolean,
    val chestClaimed: Boolean,
    val isMvp: Boolean,
    val members: List<ClanWarMemberRow>,
    val currentPlayerRow: ClanWarMemberRow,
)

data class ClanTaskProgress(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val rewardCp: Int,
    val progress: Int,
    val target: Int,
    val complete: Boolean,
    val claimed: Boolean,
)

data class ClanTaskClaimResult(
    val taskId: String,
    val rewardCoins: Int,
    val rewardCp: Int,
    val walletCoins: Int,
    val totalCp: Int,
)

data class WarChestClaimResult(
    val warId: String,
    val coinReward: Int,
    val fragmentReward: Int,
    val isWinner: Boolean,
    val isMvp: Boolean,
    val walletCoins: Int,
    val warFragments: Int,
    val seasonBadges: List<String>,
)

data class SeasonHistoryResult(
    val lastSeasonFinish: Int,
    val seasonBadges: List<String>,
    val warParticipationCount: Int,
    val warWinCount: Int,
    val mvpCount: Int,
    val contributionStreak: Int,
    val lifetimeContribution: Int,
    val warChestClaims: Int,
)

data class CrownPack(
    val id: String,
    val name: String,
    val crowns: Int,
    val priceInr: Int,
)

data class PremiumCatalogItem(
    val id: String,
    val type: String,
    val name: String,
    val priceCrowns: Int,
    val rarity: String,
    val contents: List<String>,
)

data class PremiumCatalog(
    val crowns: Int,
    val warFragments: Int,
    val crownPacks: List<CrownPack>,
    val premiumItems: List<PremiumCatalogItem>,
    val earnedItems: List<PremiumCatalogItem>,
    val ownedPremiumIds: List<String>,
    val ownedEarnedIds: List<String>,
    val equippedTitleId: String,
    val equippedFrameId: String,
    val equippedNameplateId: String,
)

private data class CachedVerifiedLeaderboard(
    val boardDateKey: String,
    val board: VerifiedDailyLeaderboard
)

class FirebaseCloudRepository(context: Context) {
    private val appContext = context.applicationContext
    private val gson = Gson()
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val appCheck: FirebaseAppCheck by lazy { FirebaseAppCheck.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance("asia-south1") }
    private val prefs by lazy {
        appContext.getSharedPreferences("firebase_cloud_repository_cache", Context.MODE_PRIVATE)
    }

    suspend fun loadProfile(uid: String): CloudProfile? {
        ensureAuthenticatedUser(uid)
        val snapshot = firestore.collection("users").document(uid).get().awaitTask()
        if (!snapshot.exists()) {
            return null
        }
        return CloudProfile(
            uid = snapshot.getString("uid").orEmpty().ifBlank { uid },
            displayName = snapshot.getString("displayName")
                ?: snapshot.getString("username")
                .orEmpty(),
            email = snapshot.getString("email"),
            photoUrl = snapshot.getString("photoUrl"),
            updatedAt = snapshot.getLong("updatedAt") ?: 0L
        )
    }

    suspend fun syncProfile(user: FirebaseUser, state: GameState.State): Long {
        ensureAuthenticatedUser(user.uid)
        val displayName = GameState.sanitizeUsername(state.username)
            .ifBlank { GameState.sanitizeUsername(user.displayName.orEmpty()) }
        require(displayName.isNotBlank()) { "Display name is required before syncing." }

        val updatedAt = System.currentTimeMillis()
        val payload = hashMapOf<String, Any>(
            "uid" to user.uid,
            "username" to displayName,
            "displayName" to displayName,
            "updatedAt" to updatedAt
        )
        user.email?.takeIf { it.isNotBlank() }?.let { payload["email"] = it }
        user.photoUrl?.toString()?.takeIf { it.isNotBlank() }?.let { payload["photoUrl"] = it }

        firestore.collection("users").document(user.uid).set(payload, SetOptions.merge()).awaitTask()
        return updatedAt
    }

    suspend fun syncCoinBalance(snapshot: GameState.CoinSecuritySnapshot): CoinSyncResult {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "syncCoinBalance",
            data = mapOf(
                "totalCoins" to snapshot.totalCoins,
                "dayKey" to snapshot.dayKey,
                "dailyCoinsEarned" to snapshot.dailyCoinsEarned,
                "dailyQuizCoinsEarned" to snapshot.dailyQuizCoinsEarned,
                "dailyTaskCoinsEarned" to snapshot.dailyTaskCoinsEarned,
                "dailyTaskClaims" to snapshot.dailyTaskClaims,
                "playSecondsToday" to snapshot.playSecondsToday,
                "quizzesPlayedToday" to snapshot.quizzesPlayedToday,
                "questionsAnsweredToday" to snapshot.questionsAnsweredToday,
                "correctAnswersToday" to snapshot.correctAnswersToday
            )
        )
        return CoinSyncResult(
            acceptedCoins = numberToInt(response["acceptedCoins"]),
            suspicious = response["suspicious"] as? Boolean ?: false
        )
    }

    suspend fun loadGameBackup(): CloudGameBackup? {
        ensureAuthenticatedUser()
        val response = callFunction(name = "loadGameBackup")
        val exists = response["exists"] as? Boolean ?: false
        if (!exists) {
            return null
        }
        val stateJson = response["stateJson"] as? String ?: return null
        if (stateJson.isBlank()) {
            return null
        }
        return CloudGameBackup(
            stateJson = stateJson,
            updatedAt = when (val value = response["updatedAt"]) {
                is Long -> value
                is Int -> value.toLong()
                is Double -> value.toLong()
                else -> 0L
            }
        )
    }

    suspend fun saveGameBackup(state: GameState.State): Long {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "saveGameBackup",
            data = mapOf("stateJson" to gson.toJson(state))
        )
        return when (val value = response["updatedAt"]) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            else -> 0L
        }
    }

    suspend fun fetchWallet(uid: String): CloudWallet {
        ensureAuthenticatedUser(uid)
        val snapshot = firestore.collection("wallets").document(uid).get().awaitTask()
        return CloudWallet(
            uid = uid,
            coins = numberToInt(snapshot.getLong("coins")),
            lastValidatedDayKey = snapshot.getString("lastValidatedDayKey").orEmpty(),
            validatedEarnedToday = numberToInt(snapshot.getLong("validatedEarnedToday")),
            suspiciousCount = numberToInt(snapshot.getLong("suspiciousCount"))
        )
    }

    suspend fun fetchOwnedCosmetics(): OwnedCosmetics {
        ensureAuthenticatedUser()
        val response = callFunction(name = "fetchOwnedCosmetics")
        val ids = (response["cosmeticIds"] as? List<*>).orEmpty().mapNotNull { it as? String }
        return OwnedCosmetics(cosmeticIds = ids)
    }

    suspend fun purchaseCosmeticReward(cosmeticId: String): CosmeticPurchaseResult {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "purchaseCosmeticReward",
            data = mapOf("cosmeticId" to cosmeticId)
        )
        return CosmeticPurchaseResult(
            cosmeticId = response["cosmeticId"] as? String ?: cosmeticId,
            remainingCoins = numberToInt(response["remainingCoins"])
        )
    }

    suspend fun registerDevice(deviceId: String): DeviceRegistrationResult {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "registerDevice",
            data = mapOf("deviceId" to deviceId)
        )
        return DeviceRegistrationResult(
            blocked = response["blocked"] as? Boolean ?: false,
            accountCount = numberToInt(response["accountCount"]),
            maxAccounts = numberToInt(response["maxAccounts"]).coerceAtLeast(2)
        )
    }

    suspend fun recordGameplaySession(session: GameplaySessionRequest): Boolean {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "recordGameplaySession",
            data = mapOf(
                "sessionId" to session.sessionId,
                "sessionType" to session.sessionType,
                "questionsAnswered" to session.questionsAnswered,
                "correctAnswers" to session.correctAnswers,
                "durationSeconds" to session.durationSeconds,
                "bestCorrectStreak" to session.bestCorrectStreak,
                "didWin" to session.didWin,
                "genre" to session.genre,
            )
        )
        return response["accepted"] as? Boolean ?: false
    }

    suspend fun fetchDailyTaskStatus(): List<DailyTaskProgress> {
        ensureAuthenticatedUser()
        val response = callFunction(name = "fetchDailyTaskStatus")
        return (response["tasks"] as? List<*>).orEmpty().mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            val id = map["id"] as? String ?: return@mapNotNull null
            val title = map["title"] as? String ?: return@mapNotNull null
            val description = map["description"] as? String ?: return@mapNotNull null
            DailyTaskProgress(
                id = id,
                title = title,
                description = description,
                rewardCoins = numberToInt(map["rewardCoins"]),
                progress = numberToInt(map["progress"]),
                target = numberToInt(map["target"]),
                complete = map["complete"] as? Boolean ?: false,
                claimed = map["claimed"] as? Boolean ?: false,
                fixed = map["fixed"] as? Boolean ?: false
            )
        }
    }

    suspend fun claimDailyTaskReward(taskId: String, adBoosted: Boolean = false): DailyTaskClaimResult {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "claimDailyTaskReward",
            data = mapOf("taskId" to taskId, "adBoosted" to adBoosted)
        )
        val wallet = response["wallet"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return DailyTaskClaimResult(
            taskId = response["taskId"] as? String ?: taskId,
            taskTitle = response["taskTitle"] as? String ?: "Daily Task",
            rewardCoins = numberToInt(response["rewardCoins"]),
            baseReward = numberToInt(response["baseReward"]),
            adBoosted = response["adBoosted"] as? Boolean ?: false,
            walletCoins = numberToInt(wallet["coins"])
        )
    }

    suspend fun fetchContributionLadder(tab: String): ContributionLadderResult {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "fetchContributionLadder",
            data = mapOf("tab" to tab)
        )
        val rows = (response["rows"] as? List<*>).orEmpty().mapNotNull(::parseContributionRow)
        val currentUserRow = parseContributionRow(response["currentUserRow"])
        val metadata = (response["metadata"] as? Map<*, *>).orEmpty()
        return ContributionLadderResult(
            tab = response["tab"] as? String ?: tab,
            rows = rows,
            currentUserRow = currentUserRow,
            metadata = ContributionLadderMetadata(
                dateKey = metadata["dateKey"] as? String,
                seasonId = metadata["seasonId"] as? String,
                currentWarIndex = (metadata["currentWarIndex"] as? Number)?.toInt(),
                cap = (metadata["cap"] as? Number)?.toInt(),
            )
        )
    }

    suspend fun searchClans(query: String = "", limit: Int = 20): List<ClanSearchResult> {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "searchClans",
            data = mapOf("query" to query, "limit" to limit)
        )
        return (response["clans"] as? List<*>).orEmpty().mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            ClanSearchResult(
                id = map["id"] as? String ?: return@mapNotNull null,
                name = map["name"] as? String ?: return@mapNotNull null,
                tag = map["tag"] as? String ?: "",
                description = map["description"] as? String ?: "",
                emblemId = map["emblemId"] as? String ?: "shield",
                memberCount = numberToInt(map["memberCount"]),
                activeMemberCount7d = numberToInt(map["activeMemberCount7d"]),
                activeBand = map["activeBand"] as? String ?: "inactive",
                openSlots = numberToInt(map["openSlots"]),
                battleJoinRecommended = map["battleJoinRecommended"] as? Boolean ?: false,
            )
        }
    }

    suspend fun createClan(
        name: String,
        tag: String,
        description: String,
        emblemId: String,
    ): String {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "createClan",
            data = mapOf(
                "name" to name,
                "tag" to tag,
                "description" to description,
                "emblemId" to emblemId,
            )
        )
        return response["clanId"] as? String ?: error("Missing clan id.")
    }

    suspend fun joinClan(clanId: String): String {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "joinClan",
            data = mapOf("clanId" to clanId)
        )
        return response["clanId"] as? String ?: clanId
    }

    suspend fun leaveClan(): Long {
        ensureAuthenticatedUser()
        val response = callFunction(name = "leaveClan")
        return numberToLong(response["contributionCooldownEndsAt"])
    }

    suspend fun fetchClanWarState(clanId: String? = null): ClanWarState {
        ensureAuthenticatedUser()
        val payload = if (clanId.isNullOrBlank()) null else mapOf("clanId" to clanId)
        val response = callFunction(name = "fetchClanWarState", data = payload)
        return ClanWarState(
            warId = response["warId"] as? String ?: "",
            clanId = response["clanId"] as? String ?: error("Missing clan id."),
            clanName = response["clanName"] as? String ?: "Clan",
            clanTag = response["clanTag"] as? String ?: "",
            phase = response["phase"] as? String ?: "prep",
            countdownMillis = numberToLong(response["countdownMillis"]),
            contributorCap = numberToInt(response["contributorCap"]),
            clanScore = numberToInt(response["clanScore"]),
            opponentScore = numberToInt(response["opponentScore"]),
            opponentName = response["opponentName"] as? String ?: "Waiting for match",
            result = response["result"] as? String ?: "pending",
            canClaimChest = response["canClaimChest"] as? Boolean ?: false,
            chestClaimed = response["chestClaimed"] as? Boolean ?: false,
            isMvp = response["isMvp"] as? Boolean ?: false,
            members = (response["members"] as? List<*>).orEmpty().mapNotNull(::parseWarMemberRow),
            currentPlayerRow = parseWarMemberRow(response["currentPlayerRow"])
                ?: ClanWarMemberRow("", "Player", "", "", "", 0, 0),
        )
    }

    suspend fun fetchClanTasks(): List<ClanTaskProgress> {
        ensureAuthenticatedUser()
        val response = callFunction(name = "fetchClanTasks")
        return (response["tasks"] as? List<*>).orEmpty().mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            ClanTaskProgress(
                id = map["id"] as? String ?: return@mapNotNull null,
                title = map["title"] as? String ?: return@mapNotNull null,
                description = map["description"] as? String ?: "",
                rewardCoins = numberToInt(map["rewardCoins"]),
                rewardCp = numberToInt(map["rewardCp"]),
                progress = numberToInt(map["progress"]),
                target = numberToInt(map["target"]),
                complete = map["complete"] as? Boolean ?: false,
                claimed = map["claimed"] as? Boolean ?: false,
            )
        }
    }

    suspend fun claimClanTaskReward(taskId: String): ClanTaskClaimResult {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "claimClanTaskReward",
            data = mapOf("taskId" to taskId)
        )
        val wallet = response["wallet"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return ClanTaskClaimResult(
            taskId = response["taskId"] as? String ?: taskId,
            rewardCoins = numberToInt(response["rewardCoins"]),
            rewardCp = numberToInt(response["rewardCp"]),
            walletCoins = numberToInt(wallet["coins"]),
            totalCp = numberToInt(response["totalCp"]),
        )
    }

    suspend fun claimWarChest(warId: String): WarChestClaimResult {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "claimWarChest",
            data = mapOf("warId" to warId)
        )
        val wallet = response["wallet"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return WarChestClaimResult(
            warId = response["warId"] as? String ?: warId,
            coinReward = numberToInt(response["coinReward"]),
            fragmentReward = numberToInt(response["fragmentReward"]),
            isWinner = response["isWinner"] as? Boolean ?: false,
            isMvp = response["isMvp"] as? Boolean ?: false,
            walletCoins = numberToInt(wallet["coins"]),
            warFragments = numberToInt(response["warFragments"]),
            seasonBadges = (response["seasonBadges"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        )
    }

    suspend fun fetchPremiumCatalog(): PremiumCatalog {
        ensureAuthenticatedUser()
        val response = callFunction(name = "fetchPremiumCatalog")
        val crownPacks = (response["crownPacks"] as? List<*>).orEmpty().mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            CrownPack(
                id = map["id"] as? String ?: return@mapNotNull null,
                name = map["name"] as? String ?: "",
                crowns = numberToInt(map["crowns"]),
                priceInr = numberToInt(map["priceInr"]),
            )
        }
        val premiumItems = (response["premiumItems"] as? List<*>).orEmpty().mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            PremiumCatalogItem(
                id = map["id"] as? String ?: return@mapNotNull null,
                type = map["type"] as? String ?: "",
                name = map["name"] as? String ?: "",
                priceCrowns = numberToInt(map["priceCrowns"]),
                rarity = map["rarity"] as? String ?: "",
                contents = (map["contents"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
            )
        }
        val earnedItems = (response["earnedItems"] as? List<*>).orEmpty().mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            PremiumCatalogItem(
                id = map["id"] as? String ?: return@mapNotNull null,
                type = map["type"] as? String ?: "",
                name = map["name"] as? String ?: "",
                priceCrowns = numberToInt(map["fragmentsRequired"]),
                rarity = map["rarity"] as? String ?: "",
                contents = emptyList(),
            )
        }
        val equipped = response["equipped"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return PremiumCatalog(
            crowns = numberToInt(response["crowns"]),
            warFragments = numberToInt(response["warFragments"]),
            crownPacks = crownPacks,
            premiumItems = premiumItems,
            earnedItems = earnedItems,
            ownedPremiumIds = (response["ownedPremiumIds"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
            ownedEarnedIds = (response["ownedEarnedIds"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
            equippedTitleId = equipped["titleId"] as? String ?: "",
            equippedFrameId = equipped["frameId"] as? String ?: "",
            equippedNameplateId = equipped["nameplateId"] as? String ?: "",
        )
    }

    suspend fun fetchSeasonHistory(): SeasonHistoryResult {
        ensureAuthenticatedUser()
        val response = callFunction(name = "fetchSeasonHistory")
        return SeasonHistoryResult(
            lastSeasonFinish = numberToInt(response["lastSeasonFinish"]),
            seasonBadges = (response["seasonBadges"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
            warParticipationCount = numberToInt(response["warParticipationCount"]),
            warWinCount = numberToInt(response["warWinCount"]),
            mvpCount = numberToInt(response["mvpCount"]),
            contributionStreak = numberToInt(response["contributionStreak"]),
            lifetimeContribution = numberToInt(response["lifetimeContribution"]),
            warChestClaims = numberToInt(response["warChestClaims"]),
        )
    }

    suspend fun purchaseCrowns(productId: String, purchaseToken: String): Int {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "purchaseCrowns",
            data = mapOf(
                "productId" to productId,
                "purchaseToken" to purchaseToken,
            )
        )
        val wallet = response["wallet"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return numberToInt(wallet["crowns"])
    }

    suspend fun purchasePremiumCosmetic(itemId: String): Triple<Int, List<String>, List<String>> {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "purchasePremiumCosmetic",
            data = mapOf("itemId" to itemId)
        )
        val wallet = response["wallet"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        return Triple(
            numberToInt(wallet["crowns"]),
            (response["ownedPremiumIds"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
            (response["ownedEarnedIds"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        )
    }

    suspend fun equipPremiumCosmetic(slot: String, itemIdOrNull: String?) {
        ensureAuthenticatedUser()
        callFunction(
            name = "equipPremiumCosmetic",
            data = mapOf(
                "slot" to slot,
                "itemIdOrNull" to (itemIdOrNull ?: "")
            )
        )
    }

    suspend fun purchaseAbility(abilityId: String): AbilityPurchaseResult {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "purchaseAbility",
            data = mapOf("abilityId" to abilityId)
        )
        return AbilityPurchaseResult(
            abilityId = response["abilityId"] as? String ?: abilityId,
            abilityName = response["abilityName"] as? String ?: "Ability",
            effect = response["effect"] as? String ?: "",
            expiresAt = numberToLong(response["expiresAt"]),
            remainingCoins = numberToInt(response["remainingCoins"])
        )
    }

    suspend fun fetchActiveAbilities(): List<ActiveAbilityInfo> {
        ensureAuthenticatedUser()
        val response = callFunction(name = "fetchActiveAbilities")
        val abilities = response["abilities"] as? List<*> ?: emptyList<Any?>()
        return abilities.mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            ActiveAbilityInfo(
                id = map["id"] as? String ?: return@mapNotNull null,
                effect = map["effect"] as? String ?: "",
                name = map["name"] as? String ?: "",
                activatedAt = numberToLong(map["activatedAt"]),
                expiresAt = numberToLong(map["expiresAt"])
            )
        }
    }

    suspend fun fetchUpdatedQuizCategoryMetadata(sinceMillis: Long): List<QuizCategoryMetadata> {
        val snapshot = firestore
            .collection("public")
            .document("quizCatalog")
            .collection("categories")
            .whereGreaterThan("updatedAt", sinceMillis)
            .get()
            .awaitTask()

        return snapshot.documents.mapNotNull(::parseQuizCategoryMetadata)
    }

    suspend fun fetchQuizBankOverride(bankId: String): QuizBankOverride? {
        val snapshot = firestore
            .collection("public")
            .document("quizContent")
            .collection("banks")
            .document(bankId)
            .get()
            .awaitTask()
        if (!snapshot.exists()) {
            return null
        }
        return parseQuizBankOverride(snapshot)
    }

    suspend fun startVerifiedDailyChallenge(): VerifiedDailyChallengeSession {
        ensureAuthenticatedUser()
        val response = callFunction(name = "startVerifiedDailyChallenge")
        val rawQuestions = response["questions"] as? List<*> ?: emptyList<Any?>()
        val questions = rawQuestions.mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            val prompt = map["question"] as? String ?: return@mapNotNull null
            val options = (map["options"] as? List<*>)?.mapNotNull { it as? String } ?: return@mapNotNull null
            if (options.size != 4) return@mapNotNull null
            VerifiedDailyQuestion(
                prompt = prompt,
                options = options
            )
        }
        return VerifiedDailyChallengeSession(
            sessionId = response["sessionId"] as? String ?: error("Missing daily session id."),
            title = response["title"] as? String ?: "Ranked Leaderboard",
            dateKey = response["dateKey"] as? String ?: "",
            questions = questions
        )
    }

    suspend fun submitVerifiedDailyChallenge(
        sessionId: String,
        answers: List<Int>
    ): VerifiedDailySubmissionResult {
        ensureAuthenticatedUser()
        val response = callFunction(
            name = "submitVerifiedDailyChallenge",
            data = mapOf(
                "sessionId" to sessionId,
                "answers" to answers
            )
        )
        return VerifiedDailySubmissionResult(
            correctAnswers = numberToInt(response["correctAnswers"]),
            elapsedMillis = when (val value = response["elapsedMillis"]) {
                is Long -> value
                is Int -> value.toLong()
                is Double -> value.toLong()
                else -> 0L
            },
            bestCorrectStreak = numberToInt(response["bestCorrectStreak"]),
            leaderboardRank = (response["leaderboardRank"] as? Number)?.toInt()
        )
    }

    suspend fun fetchVerifiedDailyLeaderboard(): VerifiedDailyLeaderboard? {
        ensureAuthenticatedUser()

        val expectedDateKey = currentIndiaDayKey()

        // Cache-first: if we already have data for yesterday, return it immediately.
        // This guarantees at most 1 Cloud Function call per user per day.
        readCachedLeaderboardForDate(expectedDateKey)?.let { return it }

        // Cache miss — call the Cloud Function and persist the result.
        val response = callFunction(name = "fetchVerifiedDailyLeaderboard")
        val dateKey = (response["dateKey"] as? String).orEmpty().ifBlank { expectedDateKey }

        val entries = (response["entries"] as? List<*>).orEmpty().mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            val displayName = map["displayName"] as? String ?: return@mapNotNull null
            VerifiedDailyLeaderboardEntry(
                uid = map["uid"] as? String,
                displayName = displayName,
                clanTag = map["clanTag"] as? String ?: "",
                equippedTitleId = map["equippedTitleId"] as? String ?: "",
                equippedFrameId = map["equippedFrameId"] as? String ?: "",
                equippedNameplateId = map["equippedNameplateId"] as? String ?: "",
                correctAnswers = numberToInt(map["correctAnswers"]),
                elapsedMillis = when (val value = map["elapsedMillis"]) {
                    is Long -> value
                    is Int -> value.toLong()
                    is Double -> value.toLong()
                    else -> 0L
                },
                accuracy = numberToInt(map["accuracy"]),
                rp = numberToInt(map["rp"])
            )
        }

        return VerifiedDailyLeaderboard(
            title = response["title"] as? String ?: "Verified Daily Challenge",
            dateKey = dateKey,
            entries = entries
        ).also(::cacheLeaderboard)
    }

    data class DynamicQuestion(
        val hash: String,
        val question: String,
        val options: List<String>,
        val answer: Int,
        val difficulty: Double,
        val genre: String,
        val category: String
    )

    data class DynamicQuizResult(
        val questions: List<DynamicQuestion>,
        val genre: String,
        val category: String,
        val count: Int
    )

    data class GenreMetadataEntry(
        val genre: String,
        val totalQuestions: Int,
        val categories: List<String>,
        val lastSeedAt: Long
    )

    suspend fun fetchDynamicQuizQuestions(
        genre: String,
        category: String? = null,
        count: Int = 10,
        excludeHashes: List<String> = emptyList(),
        recentFirst: Boolean = false
    ): DynamicQuizResult {
        ensureAuthenticatedUser()
        val data = mutableMapOf<String, Any>(
            "genre" to genre,
            "count" to count
        )
        if (!category.isNullOrBlank()) data["category"] = category
        if (excludeHashes.isNotEmpty()) data["excludeHashes"] = excludeHashes.take(200)
        if (recentFirst) data["recentFirst"] = true

        val response = callFunction(name = "getQuizQuestions", data = data)
        val rawQuestions = (response["questions"] as? List<*>).orEmpty()
        val questions = rawQuestions.mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            val q = map["question"] as? String ?: return@mapNotNull null
            val opts = (map["options"] as? List<*>)?.mapNotNull { it as? String } ?: return@mapNotNull null
            if (opts.size != 4) return@mapNotNull null
            val answer = numberToInt(map["answer"])
            if (answer !in 0..3) return@mapNotNull null
            DynamicQuestion(
                hash = (map["hash"] as? String).orEmpty(),
                question = q,
                options = opts,
                answer = answer,
                difficulty = when (val d = map["difficulty"]) {
                    is Double -> d; is Int -> d.toDouble(); is Float -> d.toDouble(); else -> 0.5
                },
                genre = (map["genre"] as? String).orEmpty(),
                category = (map["category"] as? String).orEmpty()
            )
        }
        return DynamicQuizResult(
            questions = questions,
            genre = response["genre"] as? String ?: genre,
            category = response["category"] as? String ?: "",
            count = questions.size
        )
    }

    suspend fun fetchDynamicQuizMetadata(): List<GenreMetadataEntry> {
        ensureAuthenticatedUser()
        val response = callFunction(name = "getQuizMetadata", data = mapOf("genre" to ""))
        val rawMetadata = (response["metadata"] as? List<*>).orEmpty()
        return rawMetadata.mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            val genre = map["genre"] as? String ?: map["id"] as? String ?: return@mapNotNull null
            GenreMetadataEntry(
                genre = genre,
                totalQuestions = numberToInt(map["totalQuestions"]),
                categories = (map["categories"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                lastSeedAt = numberToLong(map["lastSeedAt"])
            )
        }
    }

    suspend fun deleteUserAccount() {
        ensureAuthenticatedUser()
        callFunction(name = "deleteAccount")
    }

    suspend fun ensureAuthenticatedSession(expectedUid: String? = null): Boolean {
        return runCatching {
            ensureAuthenticatedUser(expectedUid)
            true
        }.getOrDefault(false)
    }

    fun isProtectedCloudFailure(error: Throwable?): Boolean {
        // In debug builds, never treat cloud errors as protected failures
        // that would trigger a sign-out. App Check is disabled in debug.
        if (FirebaseBootstrap.isDebugBuild()) {
            return false
        }
        val message = error?.message.orEmpty()
        return message.contains("session expired", ignoreCase = true) ||
            message.contains("sign in again", ignoreCase = true) ||
            message.contains("protected cloud features", ignoreCase = true) ||
            message.contains("build is not allowed", ignoreCase = true) ||
            message.contains("app verification failed", ignoreCase = true) ||
            message.contains("verify this app build", ignoreCase = true) ||
            message.contains("app attestation failed", ignoreCase = true) ||
            message.contains("placeholder token", ignoreCase = true) ||
            message.contains("too many attempts", ignoreCase = true) ||
            message.contains("app check", ignoreCase = true) ||
            message.contains("appcheck", ignoreCase = true)
    }



    private fun parseQuizCategoryMetadata(
        snapshot: com.google.firebase.firestore.DocumentSnapshot
    ): QuizCategoryMetadata? {
        val bankId = snapshot.getString("bankId") ?: snapshot.id
        val genre = snapshot.getString("genre") ?: return null
        val category = snapshot.getString("category") ?: return null
        return QuizCategoryMetadata(
            bankId = bankId,
            genre = genre,
            category = category,
            version = numberToInt(snapshot.getLong("version")).coerceAtLeast(1),
            updatedAt = snapshot.getLong("updatedAt")
                ?: snapshot.getTimestamp("updatedAt")?.toDate()?.time
                ?: 0L,
            questionCount = numberToInt(snapshot.getLong("questionCount"))
        )
    }

    private fun parseQuizBankOverride(
        snapshot: com.google.firebase.firestore.DocumentSnapshot
    ): QuizBankOverride? {
        val genre = snapshot.getString("genre") ?: return null
        val category = snapshot.getString("category") ?: return null
        val rawQuestions = snapshot.get("questions") as? List<*> ?: return null
        val questions = rawQuestions.mapNotNull(::parseQuestion)
        if (questions.isEmpty()) {
            return null
        }
        val updatedAt = snapshot.getLong("updatedAt")
            ?: snapshot.getTimestamp("updatedAt")?.toDate()?.time
            ?: 0L
        return QuizBankOverride(
            genre = genre,
            category = category,
            questions = questions,
            updatedAt = updatedAt
        )
    }

    private fun parseQuestion(raw: Any?): Question? {
        val map = raw as? Map<*, *> ?: return null
        val question = map["question"] as? String ?: return null
        val options = (map["options"] as? List<*>)?.mapNotNull { it as? String } ?: return null
        if (options.size != 4) {
            return null
        }
        val answer = numberToInt(map["answer"])
        if (answer !in 0..3) {
            return null
        }
        val difficulty = when (val value = map["difficulty"]) {
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is Double -> value
            is Float -> value.toDouble()
            else -> 0.5
        }
        return Question(
            question = question,
            options = options,
            answer = answer,
            difficulty = difficulty
        )
    }

    private fun parseContributionRow(raw: Any?): ContributionLadderRow? {
        val map = raw as? Map<*, *> ?: return null
        val uid = map["uid"] as? String ?: return null
        return ContributionLadderRow(
            rank = (map["rank"] as? Number)?.toInt(),
            uid = uid,
            displayName = map["displayName"] as? String ?: "Player",
            clanId = map["clanId"] as? String ?: "",
            clanTag = map["clanTag"] as? String ?: "",
            equippedTitleId = map["equippedTitleId"] as? String ?: "",
            equippedFrameId = map["equippedFrameId"] as? String ?: "",
            equippedNameplateId = map["equippedNameplateId"] as? String ?: "",
            dailyCp = numberToInt(map["dailyCp"]),
            warCp = numberToInt(map["warCp"]),
            taskCp = numberToInt(map["taskCp"]),
            totalCp = numberToInt(map["totalCp"]),
            soloCapApplied = map["soloCapApplied"] as? Boolean ?: false,
        )
    }

    private fun parseWarMemberRow(raw: Any?): ClanWarMemberRow? {
        val map = raw as? Map<*, *> ?: return null
        val uid = map["uid"] as? String ?: return null
        return ClanWarMemberRow(
            uid = uid,
            displayName = map["displayName"] as? String ?: "Player",
            equippedTitleId = map["equippedTitleId"] as? String ?: "",
            equippedFrameId = map["equippedFrameId"] as? String ?: "",
            equippedNameplateId = map["equippedNameplateId"] as? String ?: "",
            countedRuns = numberToInt(map["countedRuns"]),
            countedScore = numberToInt(map["countedScore"]),
        )
    }

    private fun numberToInt(value: Any?): Int {
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Double -> value.toInt()
            is Float -> value.toInt()
            else -> 0
        }
    }

    private fun numberToLong(value: Any?): Long {
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is Float -> value.toLong()
            else -> 0L
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun callFunction(
        name: String,
        data: Any? = null
    ): Map<String, Any?> {
        ensureValidAppCheckToken()
        return try {
            val result = invokeCallable(name, data)
            result.getData() as? Map<String, Any?>
                ?: error("$name returned an invalid response.")
        } catch (error: Exception) {
            if (error is FirebaseFunctionsException && error.code == FirebaseFunctionsException.Code.UNAUTHENTICATED) {
                try {
                    auth.currentUser?.getIdToken(true)?.awaitTask()
                    ensureValidAppCheckToken(forceRefresh = true)
                    val retryResult = invokeCallable(name, data)
                    return retryResult.getData() as? Map<String, Any?>
                        ?: error("$name returned an invalid response.")
                } catch (retryError: Exception) {
                    throw mapCloudError(retryError, name)
                }
            }
            throw mapCloudError(error, name)
        }
    }

    private suspend fun ensureValidAppCheckToken(forceRefresh: Boolean = false) {
        if (!FirebaseBootstrap.isAppCheckActive()) {
            // App Check provider failed to install — skip silently.
            return
        }
        if (FirebaseBootstrap.isDebugBuild()) {
            // Debug builds: attempt to get a token but silently ignore failures.
            // The debug token may not be registered in Firebase Console yet,
            // and App Check enforcement is not active on the backend for debug.
            try {
                appCheck.getAppCheckToken(forceRefresh).awaitTask()
            } catch (e: Exception) {
                android.util.Log.w("CloudRepo", "Debug build: App Check token failed (non-fatal): ${e.message}")
            }
            return
        }
        try {
            appCheck.getAppCheckToken(forceRefresh).awaitTask()
        } catch (error: Exception) {
            throw mapCloudError(error, "App verification")
        }
    }

    private suspend fun invokeCallable(
        name: String,
        data: Any? = null
    ): com.google.firebase.functions.HttpsCallableResult {
        val callable = functions.getHttpsCallable(name)
        return if (data == null) {
            callable.call().awaitTask()
        } else {
            callable.call(data).awaitTask()
        }
    }

    private fun cacheLeaderboard(board: VerifiedDailyLeaderboard) {
        // Key the cache by the server's dateKey, not the device's local date.
        // This ensures stale data from a previous challenge day is never served.
        val payload = CachedVerifiedLeaderboard(
            boardDateKey = board.dateKey,
            board = board
        )
        prefs.edit().putString(KEY_VERIFIED_LEADERBOARD_CACHE, gson.toJson(payload)).apply()
    }

    /** Returns cached leaderboard only if it matches the given expected dateKey. */
    private fun readCachedLeaderboardForDate(expectedDateKey: String): VerifiedDailyLeaderboard? {
        val raw = prefs.getString(KEY_VERIFIED_LEADERBOARD_CACHE, null) ?: return null
        val cached = runCatching {
            gson.fromJson(raw, CachedVerifiedLeaderboard::class.java)
        }.getOrNull() ?: return null
        return if (cached.boardDateKey == expectedDateKey) cached.board else null
    }

    /** Returns the most recently cached leaderboard without date validation (for pre-population). */
    fun readCachedLeaderboard(): VerifiedDailyLeaderboard? {
        val raw = prefs.getString(KEY_VERIFIED_LEADERBOARD_CACHE, null) ?: return null
        return runCatching {
            gson.fromJson(raw, CachedVerifiedLeaderboard::class.java)
        }.getOrNull()?.board
    }

    private fun currentIndiaDayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }.format(Date())
    }

    private suspend fun ensureAuthenticatedUser(expectedUid: String? = null): FirebaseUser {
        val user = auth.currentUser ?: error("Sign in again to continue.")
        if (!expectedUid.isNullOrBlank() && user.uid != expectedUid) {
            error("Your session changed. Sign in again to continue.")
        }
        try {
            user.getIdToken(false).awaitTask()
        } catch (error: Exception) {
            throw mapCloudError(error, "Authentication")
        }
        return user
    }

    private fun mapCloudError(error: Exception, operation: String): IllegalStateException {
        val message = error.message.orEmpty()

        // In debug builds, suppress App Check / attestation errors entirely.
        // These are expected in unsigned debug APKs and should NOT scare the user.
        if (FirebaseBootstrap.isDebugBuild() && isAppCheckRelatedMessage(message)) {
            android.util.Log.w("CloudRepo", "Debug build: suppressing App Check error: $message")
            return IllegalStateException("Connecting to server… Please check your internet and try again.", error)
        }

        val normalizedMessage = when {
            message.contains("App attestation failed", ignoreCase = true) ||
                message.contains("appcheck", ignoreCase = true) ||
                message.contains("App Check", ignoreCase = true) ||
                (operation.equals("App verification", ignoreCase = true) &&
                    message.contains("Too many attempts", ignoreCase = true)) ->
                "App verification failed for this build. Reinstall a properly signed build after registering the SHA fingerprints in Firebase."

            message.contains("Unknown calling package name", ignoreCase = true) ->
                "Google Play services could not verify this app build. Register the app's SHA fingerprints in Firebase and reinstall the signed app."

            error is FirebaseFunctionsException && error.code == FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                "This build is not allowed to use protected cloud features."

            error is FirebaseFunctionsException && error.code == FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                "Your session expired. Sign in again."

            message.isNotBlank() -> message

            else -> "$operation failed. Try again."
        }
        return IllegalStateException(normalizedMessage, error)
    }

    private fun isAppCheckRelatedMessage(message: String): Boolean {
        return message.contains("app attestation", ignoreCase = true) ||
            message.contains("appcheck", ignoreCase = true) ||
            message.contains("app check", ignoreCase = true) ||
            message.contains("Unknown calling package", ignoreCase = true) ||
            message.contains("placeholder token", ignoreCase = true) ||
            message.contains("too many attempts", ignoreCase = true)
    }

    private companion object {
        const val KEY_VERIFIED_LEADERBOARD_CACHE = "verified_leaderboard_cache_v1"
    }
}
