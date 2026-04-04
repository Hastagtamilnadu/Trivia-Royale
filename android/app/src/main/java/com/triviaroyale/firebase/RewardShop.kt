package com.triviaroyale.firebase

/**
 * Cosmetic & Ability Reward Shop — Policy-compliant reward system.
 *
 * Players spend earned coins on profile titles, profile frames, badge skins,
 * and timed gameplay abilities that provide real in-game advantages.
 * No real-world value is exchanged, fully compliant with Google Play and AdMob policies.
 */

enum class CosmeticType { TITLE, FRAME, BADGE_SKIN, ABILITY }

data class CosmeticReward(
    val id: String,
    val name: String,
    val description: String,
    val type: CosmeticType,
    val coinsRequired: Int,
    val rarity: String = "Common",      // Common, Rare, Epic, Legendary
    val durationDays: Int = 0,          // 0 = permanent, >0 = timed ability
    val effect: String = ""             // ability effect key (e.g., "rp_boost_50")
)

// ═══════════════════════════════════════════════════════════
//  Cosmetic Catalog — server also has a copy for validation
// ═══════════════════════════════════════════════════════════

val COSMETIC_CATALOG: List<CosmeticReward> = listOf(
    // ── Titles ─────────────────────────────────────────────
    CosmeticReward(
        id = "title_quiz_rookie",
        name = "Quiz Rookie",
        description = "A humble beginning for every champion.",
        type = CosmeticType.TITLE,
        coinsRequired = 1_000,
        rarity = "Common"
    ),
    CosmeticReward(
        id = "title_trivia_addict",
        name = "Trivia Addict",
        description = "You can't stop, won't stop.",
        type = CosmeticType.TITLE,
        coinsRequired = 3_000,
        rarity = "Common"
    ),
    CosmeticReward(
        id = "title_knowledge_seeker",
        name = "Knowledge Seeker",
        description = "Always curious, always learning.",
        type = CosmeticType.TITLE,
        coinsRequired = 5_000,
        rarity = "Rare"
    ),
    CosmeticReward(
        id = "title_quiz_legend",
        name = "Quiz Legend",
        description = "Your name echoes in the halls of trivia.",
        type = CosmeticType.TITLE,
        coinsRequired = 15_000,
        rarity = "Epic"
    ),
    CosmeticReward(
        id = "title_trivia_master",
        name = "Trivia Master",
        description = "The ultimate trivia authority.",
        type = CosmeticType.TITLE,
        coinsRequired = 30_000,
        rarity = "Epic"
    ),
    CosmeticReward(
        id = "title_brain_royale",
        name = "Brain Royale",
        description = "The undisputed monarch of knowledge.",
        type = CosmeticType.TITLE,
        coinsRequired = 100_000,
        rarity = "Legendary"
    ),

    // ── Profile Frames ────────────────────────────────────
    CosmeticReward(
        id = "frame_neon_purple",
        name = "Neon Purple",
        description = "A pulsing neon glow around your profile.",
        type = CosmeticType.FRAME,
        coinsRequired = 4_000,
        rarity = "Common"
    ),
    CosmeticReward(
        id = "frame_cyan_wave",
        name = "Cyan Wave",
        description = "Cool ocean vibes for your profile.",
        type = CosmeticType.FRAME,
        coinsRequired = 4_000,
        rarity = "Common"
    ),
    CosmeticReward(
        id = "frame_golden_crown",
        name = "Golden Crown",
        description = "A regal golden border fit for royalty.",
        type = CosmeticType.FRAME,
        coinsRequired = 12_000,
        rarity = "Rare"
    ),
    CosmeticReward(
        id = "frame_diamond_edge",
        name = "Diamond Edge",
        description = "Faceted diamond sparkle around your avatar.",
        type = CosmeticType.FRAME,
        coinsRequired = 25_000,
        rarity = "Epic"
    ),
    CosmeticReward(
        id = "frame_inferno",
        name = "Inferno",
        description = "Blazing flames that light up your profile.",
        type = CosmeticType.FRAME,
        coinsRequired = 40_000,
        rarity = "Epic"
    ),
    CosmeticReward(
        id = "frame_aurora",
        name = "Aurora Borealis",
        description = "The northern lights dance around your avatar.",
        type = CosmeticType.FRAME,
        coinsRequired = 100_000,
        rarity = "Legendary"
    ),

    // ── Badge Skins ───────────────────────────────────────
    CosmeticReward(
        id = "badge_chrome",
        name = "Chrome Finish",
        description = "A sleek chrome overlay on your rank badge.",
        type = CosmeticType.BADGE_SKIN,
        coinsRequired = 6_000,
        rarity = "Rare"
    ),
    CosmeticReward(
        id = "badge_holographic",
        name = "Holographic",
        description = "Prismatic holographic shimmer on your badge.",
        type = CosmeticType.BADGE_SKIN,
        coinsRequired = 16_000,
        rarity = "Epic"
    ),
    CosmeticReward(
        id = "badge_obsidian",
        name = "Obsidian",
        description = "Dark volcanic glass with red ember accents.",
        type = CosmeticType.BADGE_SKIN,
        coinsRequired = 30_000,
        rarity = "Epic"
    ),
    CosmeticReward(
        id = "badge_celestial",
        name = "Celestial",
        description = "Stars and galaxies swirl within your badge.",
        type = CosmeticType.BADGE_SKIN,
        coinsRequired = 80_000,
        rarity = "Legendary"
    ),

    // ── Abilities (Timed Gameplay Boosts) ─────────────────
    CosmeticReward(
        id = "ability_rp_boost",
        name = "RP Surge",
        description = "+50% RP earned from all quizzes for 3 days.",
        type = CosmeticType.ABILITY,
        coinsRequired = 2_000,
        rarity = "Epic",
        durationDays = 3,
        effect = "rp_boost_50"
    ),
    CosmeticReward(
        id = "ability_time_extend",
        name = "Time Warp",
        description = "+5 seconds per question for 3 days.",
        type = CosmeticType.ABILITY,
        coinsRequired = 1_500,
        rarity = "Rare",
        durationDays = 3,
        effect = "time_extend_5s"
    ),
    CosmeticReward(
        id = "ability_streak_shield",
        name = "Streak Shield",
        description = "Protects your daily streak once within 3 days.",
        type = CosmeticType.ABILITY,
        coinsRequired = 1_800,
        rarity = "Epic",
        durationDays = 3,
        effect = "streak_shield"
    ),
    CosmeticReward(
        id = "ability_hint_unlock",
        name = "Hint Master",
        description = "50/50 hint on every question for 3 days.",
        type = CosmeticType.ABILITY,
        coinsRequired = 2_500,
        rarity = "Legendary",
        durationDays = 3,
        effect = "hint_unlock"
    ),
    CosmeticReward(
        id = "ability_double_xp",
        name = "XP Overdrive",
        description = "2x XP earned from all quizzes for 3 days.",
        type = CosmeticType.ABILITY,
        coinsRequired = 2_000,
        rarity = "Epic",
        durationDays = 3,
        effect = "double_xp"
    )
)

val COSMETIC_TYPES_DISPLAY = listOf(
    CosmeticType.ABILITY to "⚡ Abilities",
    CosmeticType.TITLE to "Titles",
    CosmeticType.FRAME to "Profile Frames",
    CosmeticType.BADGE_SKIN to "Badge Skins"
)

fun cosmeticById(id: String): CosmeticReward? =
    COSMETIC_CATALOG.firstOrNull { it.id == id }

fun cosmeticsForType(type: CosmeticType): List<CosmeticReward> =
    COSMETIC_CATALOG.filter { it.type == type }

fun rarityColor(rarity: String): Long = when (rarity) {
    "Common" -> 0xFF94A3B8     // Slate
    "Rare" -> 0xFF3B82F6       // Blue
    "Epic" -> 0xFFA855F7       // Purple
    "Legendary" -> 0xFFF59E0B  // Amber
    else -> 0xFF94A3B8
}
