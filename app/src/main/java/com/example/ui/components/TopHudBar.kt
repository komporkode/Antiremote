package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionConfig
import com.example.data.model.ConnectionStatus
import com.example.data.model.WorkspaceInfo
import com.example.ui.theme.*

@Composable
fun TopHudBar(
    config: ConnectionConfig,
    workspaceInfo: WorkspaceInfo,
    isKeepScreenOn: Boolean = false,
    onToggleKeepScreenOn: () -> Unit = {},
    onOpenSettingsClick: () -> Unit = {},
    onOpenConnectionDialog: () -> Unit,
    onScanQrClick: () -> Unit,
    onSaveAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = config.status == ConnectionStatus.CONNECTED
    val (statusColor, statusTitle) = when (config.status) {
        ConnectionStatus.CONNECTED -> AccentSuccess to "Online"
        ConnectionStatus.CONNECTING -> AccentWarning to "Menghubungkan"
        ConnectionStatus.ERROR -> AccentDanger to "Error"
        ConnectionStatus.DISCONNECTED -> ContentTertiary to "Offline"
    }

    // Breathing pulse animation for live connection indicator
    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isConnected) 0.35f else 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_hud_bar"),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Header Title "Chat" + Subtitle "• Project • :Port"
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "Chat",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = ContentPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .alpha(if (isConnected) pulseAlpha else 1.0f)
                            .background(statusColor)
                    )

                    val dispProject = workspaceInfo.projectName.ifBlank { "Dengar" }
                    Text(
                        text = dispProject,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ContentSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ContentTertiary,
                            fontSize = 11.sp
                        )
                    )

                    Text(
                        text = ":${workspaceInfo.devPort ?: 3000}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AccentPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            // Right: Git Branch Capsule Chip + Connection Pill + Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Git Branch Status Capsule
                val dispBranch = workspaceInfo.gitBranch.ifBlank { "main" }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AltRoute,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "$dispBranch sync",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ContentSecondary,
                                fontWeight = FontWeight.Normal,
                                fontSize = 10.sp
                            ),
                            maxLines = 1
                        )
                    }
                }

                // Connection Manager Pill
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenConnectionDialog() }
                        .testTag("connection_pill"),
                    shape = RoundedCornerShape(12.dp),
                    color = CardSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ContentPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Settings Button
                IconButton(
                    onClick = onOpenSettingsClick,
                    modifier = Modifier
                        .size(26.dp)
                        .testTag("btn_open_settings")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Pengaturan",
                        tint = ContentSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
