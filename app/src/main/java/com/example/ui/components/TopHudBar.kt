package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    onOpenConnectionDialog: () -> Unit,
    onScanQrClick: () -> Unit,
    onSaveAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_hud_bar"),
        color = CardSurface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: App Title, Connection Status Pill & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val (statusColor, statusTitle) = when (config.status) {
                    ConnectionStatus.CONNECTED -> AccentSuccess to "Tersambung"
                    ConnectionStatus.CONNECTING -> AccentWarning to "Menghubungkan..."
                    ConnectionStatus.ERROR -> AccentDanger to "Koneksi Terputus"
                    ConnectionStatus.DISCONNECTED -> ContentTertiary to "Offline"
                }

                // Left: App Name & Clickable Status Pill
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Antiremote",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = ContentPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    )

                    // Interactive Status Chip
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenConnectionDialog() }
                            .testTag("connection_pill"),
                        shape = RoundedCornerShape(6.dp),
                        color = CardSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = "$statusTitle • ${config.ipAddress}:${config.port}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ContentSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            if (config.isDemoMode) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AccentWarning.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "DEMO",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AccentWarning,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Right: Scan QR & Save Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save Button
                    if (config.status == ConnectionStatus.CONNECTED) {
                        FilledTonalButton(
                            onClick = onSaveAllClick,
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("btn_top_save_all"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = AccentSuccessSoft,
                                contentColor = AccentSuccess
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Simpan",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Simpan",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Scan QR Button
                    FilledTonalButton(
                        onClick = onScanQrClick,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_top_scan_qr"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AccentPrimarySoft,
                            contentColor = AccentPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = "Scan QR",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Scan QR",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Row 2: Clean breadcrumb showing active workspace & file
            val projectName = workspaceInfo.projectName.ifEmpty { config.workspaceName.ifEmpty { "Workstation" } }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = CardSurfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AccentPrimary
                    )
                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ContentPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )

                    if (workspaceInfo.activeFileName.isNotEmpty()) {
                        Text(
                            text = "›",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ContentTertiary,
                                fontSize = 11.sp
                            )
                        )
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = ContentSecondary
                        )
                        Text(
                            text = "${workspaceInfo.activeFileName} (L:${workspaceInfo.cursorLine})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ContentSecondary,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
