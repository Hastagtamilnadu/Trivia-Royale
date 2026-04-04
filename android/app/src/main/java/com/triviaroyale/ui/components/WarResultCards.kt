package com.triviaroyale.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.triviaroyale.ui.theme.Green400
import com.triviaroyale.ui.theme.OnSurface
import com.triviaroyale.ui.theme.OnSurfaceVariant
import com.triviaroyale.ui.theme.OutlineVariant
import com.triviaroyale.ui.theme.Primary
import com.triviaroyale.ui.theme.Purple500
import com.triviaroyale.ui.theme.Secondary
import com.triviaroyale.ui.theme.SurfaceContainerHigh
import com.triviaroyale.ui.theme.Tertiary
import kotlinx.coroutines.delay

/**
 * Shown after a war contribution quiz completes.
 * Displays the player's score, position, and whether it was counted.
 */
@Composable
fun ContributionResultCard(
    score: Int,
    correctAnswers: Int,
    totalQuestions: Int,
    clanName: String,
    counted: Boolean,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) + scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        ),
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (counted) Primary.copy(alpha = 0.18f) else OnSurfaceVariant.copy(alpha = 0.12f),
                                if (counted) Purple500.copy(alpha = 0.12f) else OnSurfaceVariant.copy(alpha = 0.06f),
                            ),
                        ),
                        RoundedCornerShape(22.dp),
                    )
                    .padding(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            if (counted) Icons.Filled.Shield else Icons.Filled.Groups,
                            contentDescription = null,
                            tint = if (counted) Primary else OnSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                        Column {
                            Text(
                                if (counted) "Contribution Counted!" else "Not Counted",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (counted) Primary else OnSurfaceVariant,
                            )
                            Text(
                                "for $clanName",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatChip(label = "Score", value = "$score", color = Primary, modifier = Modifier.weight(1f))
                        StatChip(label = "Correct", value = "$correctAnswers/$totalQuestions", color = Secondary, modifier = Modifier.weight(1f))
                    }

                    if (counted) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Green400.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Green400.copy(alpha = 0.22f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = Green400, modifier = Modifier.size(18.dp))
                                Text(
                                    "Your score has been added to the clan war total.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shown after a war settles when the player is the MVP.
 * Displays the MVP title, war result, and bonus rewards.
 */
@Composable
fun MvpResultCard(
    playerName: String,
    clanName: String,
    warResult: String,
    coinReward: Int,
    fragmentReward: Int,
    isWinner: Boolean,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) + scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        ),
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, Tertiary.copy(alpha = 0.3f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Tertiary.copy(alpha = 0.18f), Purple500.copy(alpha = 0.12f)),
                        ),
                        RoundedCornerShape(22.dp),
                    )
                    .padding(24.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = Tertiary,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        "MVP",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Tertiary,
                    )
                    Text(
                        playerName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                    )
                    Text(
                        "Top contributor for $clanName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                    )

                    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.15f))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isWinner) Green400.copy(alpha = 0.14f) else Secondary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, if (isWinner) Green400.copy(alpha = 0.22f) else Secondary.copy(alpha = 0.18f)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                if (isWinner) Icons.Filled.MilitaryTech else Icons.Filled.Shield,
                                contentDescription = null,
                                tint = if (isWinner) Green400 else Secondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                if (isWinner) "Victory! $warResult" else "Defeat. $warResult",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatChip(label = "Coins", value = "+$coinReward", color = Secondary, modifier = Modifier.weight(1f))
                        StatChip(label = "Fragments", value = "+$fragmentReward", color = Tertiary, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
