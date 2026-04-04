package com.triviaroyale.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

/**
 * Centralised analytics helper for all 10 clan-related events.
 * Each method wraps a single Firebase Analytics event with structured parameters.
 */
object ClanAnalytics {

    private val analytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(com.triviaroyale.TriviaRoyaleApplication.appContext)
    }

    /** 1. User creates a clan */
    fun logClanCreated(clanId: String, clanTag: String) {
        analytics.logEvent("clan_created") {
            param("clan_id", clanId)
            param("clan_tag", clanTag)
        }
    }

    /** 2. User joins an existing clan */
    fun logClanJoined(clanId: String, clanTag: String) {
        analytics.logEvent("clan_joined") {
            param("clan_id", clanId)
            param("clan_tag", clanTag)
        }
    }

    /** 3. War contribution quiz is completed */
    fun logWarContributionCompleted(warId: String, score: Int, counted: Boolean) {
        analytics.logEvent("war_contribution_completed") {
            param("war_id", warId)
            param("score", score.toLong())
            param("counted", if (counted) "true" else "false")
        }
    }

    /** 4. War chest is claimed */
    fun logWarChestClaimed(warId: String, coinReward: Int, isWinner: Boolean, isMvp: Boolean) {
        analytics.logEvent("war_chest_claimed") {
            param("war_id", warId)
            param("coin_reward", coinReward.toLong())
            param("is_winner", if (isWinner) "true" else "false")
            param("is_mvp", if (isMvp) "true" else "false")
        }
    }

    /** 5. Clan task is claimed */
    fun logClanTaskClaimed(taskId: String, rewardCp: Int) {
        analytics.logEvent("clan_task_claimed") {
            param("task_id", taskId)
            param("reward_cp", rewardCp.toLong())
        }
    }

    /** 6. Premium cosmetic purchased with crowns */
    fun logCosmeticPurchased(itemId: String, crownCost: Int) {
        analytics.logEvent("cosmetic_purchased") {
            param("item_id", itemId)
            param("crown_cost", crownCost.toLong())
        }
    }

    /** 7. Premium cosmetic equipped or unequipped */
    fun logCosmeticEquipped(itemId: String, slot: String, equipped: Boolean) {
        analytics.logEvent("cosmetic_equipped") {
            param("item_id", itemId)
            param("slot", slot)
            param("action", if (equipped) "equip" else "unequip")
        }
    }

    /** 8. Crown pack purchased via Play Billing */
    fun logCrownPackPurchased(productId: String, crowns: Int) {
        analytics.logEvent("crown_pack_purchased") {
            param("product_id", productId)
            param("crowns", crowns.toLong())
        }
    }

    /** 9. Season history opened on profile */
    fun logSeasonHistoryViewed() {
        analytics.logEvent("season_history_viewed") {}
    }

    /** 10. War board tab opened */
    fun logWarBoardViewed(warId: String, phase: String) {
        analytics.logEvent("war_board_viewed") {
            param("war_id", warId)
            param("phase", phase)
        }
    }
}
