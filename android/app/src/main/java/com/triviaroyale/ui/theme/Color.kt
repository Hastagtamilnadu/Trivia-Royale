package com.triviaroyale.ui.theme

import androidx.compose.ui.graphics.Color

// ========== WEB APP EXACT COLOR PALETTE ==========
// Mapped 1:1 from the web app's TailwindCSS config

// Surface colors (neutral dark tones)
val Surface = Color(0xFF141218)
val SurfaceDim = Color(0xFF141218)
val SurfaceContainer = Color(0xFF211F26)
val SurfaceContainerLow = Color(0xFF1D1B20)
val SurfaceContainerHigh = Color(0xFF2B2930)
val SurfaceContainerHighest = Color(0xFF36343B)
val SurfaceContainerLowest = Color(0xFF0F0D13)
val SurfaceBright = Color(0xFF3B383E)
val SurfaceVariant = Color(0xFF49454F)

// Primary (lavender purple)
val Primary = Color(0xFFD0BCFF)
val PrimaryDim = Color(0xFFD0BCFF)
val PrimaryContainer = Color(0xFF4F378B)
val PrimaryFixed = Color(0xFFE7DEFF)
val OnPrimary = Color(0xFF381E72)
val OnPrimaryContainer = Color(0xFFE7DEFF)
val InversePrimary = Color(0xFF6750A4)

// Secondary (muted lavender)
val Secondary = Color(0xFFCCC2DC)
val SecondaryDim = Color(0xFFCCC2DC)
val SecondaryContainer = Color(0xFF484362)
val SecondaryFixed = Color(0xFFE8DEF8)
val OnSecondary = Color(0xFF332D41)

// Tertiary (rose pink)
val Tertiary = Color(0xFFEFB8C8)
val TertiaryDim = Color(0xFFEFB8C8)
val TertiaryContainer = Color(0xFF633B48)
val TertiaryFixed = Color(0xFFFFD8E4)
val OnTertiary = Color(0xFF492532)

// Error
val Error = Color(0xFFF2B8B5)
val ErrorDim = Color(0xFFF2B8B5)
val ErrorContainer = Color(0xFF8C1D18)
val OnError = Color(0xFF601410)

// On-surface
val OnSurface = Color(0xFFE6E1E5)
val OnSurfaceVariant = Color(0xFFCAC4D0)
val OnBackground = Color(0xFFE6E1E5)
val Background = Color(0xFF141218)

// Outline
val Outline = Color(0xFF938F99)
val OutlineVariant = Color(0xFF49454F)
val InverseSurface = Color(0xFFE6E1E5)

// Extra game accent colors (for rank badges, game-specific UI)
val CyanAccent = Color(0xFF22D3EE)
val Green400 = Color(0xFF4ADE80)
val Green500 = Color(0xFF22C55E)
val Red500 = Color(0xFFEF4444)
val Yellow400 = Color(0xFFFACC15)
val Amber600 = Color(0xFFD97706)
val Amber700 = Color(0xFFB45309)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Orange500 = Color(0xFFF97316)
val Purple400 = Color(0xFFC084FC)
val Purple500 = Color(0xFFA855F7)
val Cyan400 = Color(0xFF22D3EE)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc500 = Color(0xFF71717A)
val Zinc800 = Color(0xFF27272A)
val Zinc950 = Color(0xFF09090B)

// Rank colors (game-specific, independent of M3 theme)
object RankColors {
    data class RankColorSet(
        val border: Color,
        val icon: Color,
        val glow: Color,
        val bgStart: Color,
        val bgEnd: Color,
        val text: Color
    )

    val Bronze = RankColorSet(
        border = Color(0xFFB45309),
        icon = Color(0xFFD97706),
        glow = Color(0x4DB45309),
        bgStart = Color(0x33B45309),
        bgEnd = Color(0x339A3412),
        text = Color(0xFFD97706)
    )

    val Silver = RankColorSet(
        border = Color(0xFF94A3B8),
        icon = Color(0xFFCBD5E1),
        glow = Color(0x4D94A3B8),
        bgStart = Color(0x33CBD5E1),
        bgEnd = Color(0x3364748B),
        text = Color(0xFFCBD5E1)
    )

    val Gold = RankColorSet(
        border = Color(0xFFFACC15),
        icon = Color(0xFFFACC15),
        glow = Color(0x4DFACC15),
        bgStart = Color(0x33FACC15),
        bgEnd = Color(0x33D97706),
        text = Color(0xFFFACC15)
    )

    val Platinum = RankColorSet(
        border = Color(0xFFD4D4D8),
        icon = Color(0xFFD4D4D8),
        glow = Color(0x4DD4D4D8),
        bgStart = Color(0x33D4D4D8),
        bgEnd = Color(0x3371717A),
        text = Color(0xFFD4D4D8)
    )

    val Diamond = RankColorSet(
        border = Color(0xFF22D3EE),
        icon = Color(0xFF22D3EE),
        glow = Color(0x6622D3EE),
        bgStart = Color(0x3322D3EE),
        bgEnd = Color(0x333B82F6),
        text = Color(0xFF22D3EE)
    )

    val Master = RankColorSet(
        border = Color(0xFFF97316),
        icon = Color(0xFFF97316),
        glow = Color(0x66F97316),
        bgStart = Color(0x33F97316),
        bgEnd = Color(0x33DC2626),
        text = Color(0xFFF97316)
    )

    val Grandmaster = RankColorSet(
        border = Color(0xFFC084FC),
        icon = Color(0xFFC084FC),
        glow = Color(0x80C084FC),
        bgStart = Color(0x33A855F7),
        bgEnd = Color(0x336366F1),
        text = Color(0xFFC084FC)
    )

    fun forRank(name: String): RankColorSet = when (name) {
        "Bronze" -> Bronze
        "Silver" -> Silver
        "Gold" -> Gold
        "Platinum" -> Platinum
        "Diamond" -> Diamond
        "Master" -> Master
        "Grandmaster" -> Grandmaster
        else -> Bronze
    }
}
