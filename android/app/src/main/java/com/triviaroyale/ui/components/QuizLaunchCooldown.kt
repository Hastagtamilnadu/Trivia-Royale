package com.triviaroyale.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

fun formatQuizLaunchCooldown(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) {
        "%02dm %02ds".format(minutes, seconds)
    } else {
        "%02ds".format(seconds)
    }
}

@Composable
fun RewardedSkipWaitButton(
    enabled: Boolean,
    isWorking: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled && !isWorking,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2563EB),
            disabledContainerColor = Color(0xFF1E293B),
        ),
    ) {
        Text(
            text = when {
                isWorking -> "Loading Ad…"
                enabled   -> "🎥 Watch & Earn +10 🪙 (skip wait)"
                else      -> "Skip Already Used"
            },
            modifier = Modifier.padding(horizontal = 4.dp),
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
fun QuizLaunchCooldownBar(
    remainingMillis: Long,
    totalMillis: Long,
    modifier: Modifier = Modifier,
    color: Color
) {
    val progress = if (totalMillis <= 0L) 0f else (remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Take a quick break · ${formatQuizLaunchCooldown(remainingMillis)}",
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        AppProgressBar(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            height = 5.dp,
            color = color,
            trackColor = color.copy(alpha = 0.18f)
        )
    }
}
