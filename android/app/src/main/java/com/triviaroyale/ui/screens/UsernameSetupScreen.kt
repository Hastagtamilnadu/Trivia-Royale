package com.triviaroyale.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.triviaroyale.data.GameState
import com.triviaroyale.ui.theme.Cyan400
import com.triviaroyale.ui.theme.Surface
import com.triviaroyale.ui.theme.SurfaceContainerHigh
import com.triviaroyale.ui.theme.Zinc950

@Composable
fun UsernameSetupScreen(
    suggestedUsername: String,
    isWorking: Boolean,
    errorMessage: String?,
    onSaveUsername: (String) -> Unit
) {
    var username by rememberSaveable(suggestedUsername) { mutableStateOf(suggestedUsername) }
    var showValidation by remember { mutableStateOf(false) }
    val sanitizedUsername = GameState.sanitizeUsername(username)
    val usernameIsValid = sanitizedUsername.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Zinc950, Surface, Color(0xFF111827))
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.92f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Choose Your Username",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Fresh accounts need a public username before entering the arena. It shows on your profile and leaderboard.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Start
                        )
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = GameState.sanitizeUsername(it)
                        showValidation = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("Preferred Username") },
                    supportingText = {
                        Text(
                            text = "${sanitizedUsername.length}/${GameState.MAX_USERNAME_LENGTH}",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    },
                    isError = showValidation && !usernameIsValid,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    )
                )

                if (showValidation && !usernameIsValid) {
                    Text(
                        text = "Enter a username with 1 to ${GameState.MAX_USERNAME_LENGTH} letters, numbers, spaces, or underscores.",
                        color = Color(0xFFFDA4AF),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFDA4AF),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        if (!usernameIsValid) {
                            showValidation = true
                            return@Button
                        }
                        onSaveUsername(sanitizedUsername)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isWorking && usernameIsValid,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isWorking) "Saving..." else "Enter Arena",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Max ${GameState.MAX_USERNAME_LENGTH} characters.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cyan400
                )
            }
        }
    }
}
