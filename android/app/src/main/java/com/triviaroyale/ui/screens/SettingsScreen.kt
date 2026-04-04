package com.triviaroyale.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.triviaroyale.data.GameState
import com.triviaroyale.ui.theme.OnSurface
import com.triviaroyale.ui.theme.OnSurfaceVariant
import com.triviaroyale.ui.theme.OutlineVariant
import com.triviaroyale.ui.theme.Primary
import com.triviaroyale.ui.theme.Surface
import com.triviaroyale.ui.theme.SurfaceContainerHigh
import com.triviaroyale.ui.theme.SurfaceContainerHighest
import com.triviaroyale.ui.theme.SurfaceContainerLow

private data class SettingsRow(
    val title: String,
    val subtitle: String,
    val icon: @Composable () -> Unit,
    val titleColor: Color = OnSurface,
    val onClick: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    gameState: GameState,
    navController: NavController,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    onLogOut: () -> Unit
) {
    val state by gameState.state.collectAsState()

    val rows = buildList {
        add(
        SettingsRow(
            title = "Display Name",
            subtitle = state.username,
            icon = { Icon(Icons.Outlined.PersonOutline, null, tint = Primary) }
        )
        )
        add(
        SettingsRow(
            title = "Privacy Policy",
            subtitle = "Open the full policy online",
            icon = { Icon(Icons.Outlined.Description, null, tint = OnSurface) },
            onClick = onOpenPrivacyPolicy
        )
        )
        add(
        SettingsRow(
            title = "Terms & Conditions",
            subtitle = "Skill-based rules, rewards, and anti-abuse terms",
            icon = { Icon(Icons.Outlined.Description, null, tint = OnSurface) },
            onClick = onOpenTerms
        )
        )
        add(
            SettingsRow(
                title = "Log Out",
                subtitle = "Sign out of this account",
                icon = { Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = Color(0xFFF59E0B)) },
                titleColor = Color(0xFFF59E0B),
                onClick = onLogOut
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, null, tint = OnSurface)
            }
            Text(
                "Account Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow)
        ) {
            Column {
                rows.forEachIndexed { index, row ->
                    Surface(
                        onClick = row.onClick ?: {},
                        enabled = row.onClick != null,
                        color = Color.Transparent
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceContainerHighest,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    row.icon()
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(row.title, fontWeight = FontWeight.Bold, color = row.titleColor)
                                Text(
                                    row.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                            if (row.onClick != null) {
                                Icon(Icons.Filled.ChevronRight, null, tint = OnSurfaceVariant)
                            }
                        }
                    }
                    if (index < rows.lastIndex) {
                        HorizontalDivider(color = OutlineVariant.copy(alpha = 0.1f))
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Arena Profile", fontWeight = FontWeight.Bold)
                Text(
                    "Keep your name sharp and your profile ready for the season standings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}
