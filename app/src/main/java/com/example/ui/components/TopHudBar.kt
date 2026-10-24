package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
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
    val (statusColor, statusTitle) = when (config.status) {
        ConnectionStatus.CONNECTED -> AccentSuccess to "Online"
        ConnectionStatus.CONNECTING -> AccentWarning to "Menghubungkan"
        ConnectionStatus.ERROR -> AccentDanger to "Error"
        ConnectionStatus.DISCONNECTED -> ContentTertiary to "Offline"
    }

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
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Breadcrumb (Project › Active File)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = workspaceInfo.projectName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ContentPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )

                Text(
                    text = "›",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ContentTertiary,
                        fontSize = 12.sp
                    )
                )

                Text(
                    text = "${workspaceInfo.activeFileName}:${workspaceInfo.cursorLine}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ContentSecondary,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right: High-Signal Actions (Connection Status Pill + Settings)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Sleek Connection Status Pill (1-Tap Connection Manager)
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
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
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Settings Button
                IconButton(
                    onClick = onOpenSettingsClick,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("btn_open_settings")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Pengaturan",
                        tint = ContentSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
