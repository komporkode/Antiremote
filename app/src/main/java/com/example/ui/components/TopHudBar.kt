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
        tonalElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Connection Status & Scan QR Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val (statusColor, statusTitle) = when (config.status) {
                    ConnectionStatus.CONNECTED -> AccentSuccess to "Connected"
                    ConnectionStatus.CONNECTING -> AccentWarning to "Connecting"
                    ConnectionStatus.ERROR -> AccentDanger to "Error"
                    ConnectionStatus.DISCONNECTED -> ContentTertiary to "Disconnected"
                }

                // Status chip with IP and Port
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenConnectionDialog() }
                        .testTag("connection_pill"),
                    shape = RoundedCornerShape(8.dp),
                    color = CardSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = "$statusTitle (${config.ipAddress}:${config.port})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = ContentPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        if (config.isDemoMode) {
                            Text(
                                text = "DEMO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentWarning,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // 📷 Scan QR Button
                Button(
                    onClick = onScanQrClick,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_top_scan_qr"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary.copy(alpha = 0.15f),
                        contentColor = AccentPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.35f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = "Scan QR",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scan QR",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            HorizontalDivider(
                color = CardBorder.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            // Row 2: WORKSPACE & EDITING status with [💾 Save] button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "WORKSPACE: ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ContentTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = workspaceInfo.projectName.ifEmpty { config.workspaceName },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ContentPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "EDITING  : ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ContentTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = "${workspaceInfo.activeFileName} (Baris ${workspaceInfo.cursorLine})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                // 💾 Save Button
                Button(
                    onClick = onSaveAllClick,
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_top_save_all"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentSuccess.copy(alpha = 0.15f),
                        contentColor = AccentSuccess
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentSuccess.copy(alpha = 0.35f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save All",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
