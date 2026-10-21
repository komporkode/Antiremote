package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionConfig
import com.example.data.model.ConnectionStatus
import com.example.data.model.LogLevel
import com.example.data.model.SystemTelemetry
import com.example.data.model.TerminalLog
import com.example.ui.theme.*

@Composable
fun TelemetryScreen(
    telemetry: SystemTelemetry,
    connectionConfig: ConnectionConfig,
    terminalLogs: List<TerminalLog>,
    terminalInput: String,
    onTerminalInputChange: (String) -> Unit,
    onSendTerminalCommand: (String) -> Unit,
    onOpenConnectionDialog: () -> Unit,
    onScanQrClick: () -> Unit,
    onSaveAll: () -> Unit,
    onRunDev: () -> Unit,
    onRunBuild: () -> Unit,
    onGitPull: () -> Unit,
    onGitPush: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("telemetry_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Connection Status Card
        item {
            LanConnectionCard(
                config = connectionConfig,
                onScanQrClick = onScanQrClick,
                onConfigureClick = onOpenConnectionDialog
            )
        }

        // 2. Real-time CPU & RAM Gauge Meters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // CPU Usage Card
                val cpuColor = when {
                    telemetry.cpuPercent > 80f -> AccentDanger
                    telemetry.cpuPercent > 50f -> AccentWarning
                    else -> AccentPrimary
                }
                CleanMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Beban CPU",
                    value = String.format("%.1f%%", telemetry.cpuPercent),
                    subtitle = if (telemetry.cpuPercent > 70f) "Beban tinggi" else "Performa lancar",
                    icon = Icons.Outlined.Memory,
                    accentColor = cpuColor,
                    progress = (telemetry.cpuPercent / 100f).coerceIn(0f, 1f)
                )

                // RAM Usage Card
                val ramPercent = if (telemetry.memoryTotalMb > 0)
                    (telemetry.memoryUsedMb.toFloat() / telemetry.memoryTotalMb.toFloat())
                else 0f
                val ramColor = when {
                    ramPercent > 0.85f -> AccentDanger
                    ramPercent > 0.65f -> AccentWarning
                    else -> AccentSecondary
                }
                CleanMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Memori RAM",
                    value = String.format("%.1f GB", telemetry.memoryUsedMb / 1024f),
                    subtitle = "dari ${String.format("%.1f GB", telemetry.memoryTotalMb / 1024f)} total",
                    icon = Icons.Outlined.Storage,
                    accentColor = ramColor,
                    progress = ramPercent.coerceIn(0f, 1f)
                )
            }
        }

        // Latency & Host Details
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CleanMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Latensi",
                    value = "${connectionConfig.pingMs} ms",
                    subtitle = if (connectionConfig.status == ConnectionStatus.CONNECTED) "Koneksi stabil" else "Offline",
                    icon = Icons.Outlined.Wifi,
                    accentColor = AccentSuccess
                )

                val hours = telemetry.uptimeSeconds / 3600
                val mins = (telemetry.uptimeSeconds % 3600) / 60
                val uptimeStr = if (hours > 0) "${hours}j ${mins}m" else "${mins}m ${telemetry.uptimeSeconds % 60}d"
                CleanMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Waktu Aktif",
                    value = uptimeStr,
                    subtitle = "Port: ${connectionConfig.port}",
                    icon = Icons.Outlined.Schedule,
                    accentColor = AccentPrimary
                )
            }
        }

        // 3. Tombol Aksi Cepat (Quick Actions)
        item {
            Text(
                text = "Aksi Cepat",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ContentPrimary,
                    fontSize = 15.sp
                )
            )
        }

        // Row 1: Simpan Semua & Jalankan Dev
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 💾 Simpan Semua
                FilledTonalButton(
                    onClick = onSaveAll,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_quick_save_all"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentSuccessSoft,
                        contentColor = AccentSuccess
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan Semua", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                // 🚀 Jalankan Dev
                FilledTonalButton(
                    onClick = onRunDev,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_quick_npm_dev"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentPrimarySoft,
                        contentColor = AccentPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Jalankan Dev", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }

        // Row 2: npm run build & Git actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 📦 Build
                FilledTonalButton(
                    onClick = onRunBuild,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(42.dp)
                        .testTag("btn_quick_npm_build"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CardSurfaceVariant,
                        contentColor = ContentPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AccentWarning
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Build", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }

                // 🔄 Git Pull
                OutlinedButton(
                    onClick = onGitPull,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_quick_git_pull"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ContentSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pull", fontSize = 11.sp)
                }

                // 🔄 Git Push
                OutlinedButton(
                    onClick = onGitPush,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_quick_git_push"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ContentSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Push", fontSize = 11.sp)
                }
            }
        }

        // 4. Log & Aktivitas Sistem
        item {
            Text(
                text = "Log & Aktivitas Sistem",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ContentPrimary,
                    fontSize = 15.sp
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Terminal Log Output Box
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                color = CardSurfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STDOUT / STDERR (${terminalLogs.size} lines)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ContentTertiary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AccentSuccess)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        reverseLayout = false
                    ) {
                        items(terminalLogs.takeLast(50)) { log ->
                            val color = when (log.level) {
                                LogLevel.ERROR -> AccentDanger
                                LogLevel.SUCCESS -> AccentSuccess
                                LogLevel.WARN -> AccentWarning
                                LogLevel.COMMAND -> AccentPrimary
                                LogLevel.INFO -> ContentSecondary
                            }
                            Text(
                                text = "[${log.timestamp}] ${log.message}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = color,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Terminal Command Prompt Input
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = terminalInput,
                    onValueChange = onTerminalInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_command_input"),
                    placeholder = {
                        Text("$ perintah shell...", color = ContentTertiary, fontSize = 12.sp)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = ContentPrimary,
                        unfocusedTextColor = ContentPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (terminalInput.isNotBlank()) onSendTerminalCommand(terminalInput)
                    })
                )

                IconButton(
                    onClick = {
                        if (terminalInput.isNotBlank()) onSendTerminalCommand(terminalInput)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentPrimary)
                        .testTag("btn_send_terminal_cmd")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Command",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * High-UX LAN Connection Card with integrated "Scan QR"
 */
@Composable
fun LanConnectionCard(
    config: ConnectionConfig,
    onScanQrClick: () -> Unit,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText) = when (config.status) {
        ConnectionStatus.CONNECTED -> AccentSuccess to "Connected"
        ConnectionStatus.CONNECTING -> AccentWarning to "Connecting..."
        ConnectionStatus.ERROR -> AccentDanger to "Error"
        ConnectionStatus.DISCONNECTED -> ContentTertiary to "Offline"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .testTag("lan_connection_card"),
        color = CardSurface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )

                    Column {
                        Text(
                            text = if (config.status == ConnectionStatus.CONNECTED) config.hostname else "Antigravity Bridge",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ContentPrimary,
                                fontSize = 14.sp
                            )
                        )
                        Text(
                            text = "${config.ipAddress}:${config.port}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = ContentSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (config.isDemoMode) "Demo" else statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = CardBorder.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onScanQrClick,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(38.dp)
                        .testTag("btn_scan_qr_connect"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = "Scan QR",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scan QR",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }

                FilledTonalButton(
                    onClick = onConfigureClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_configure_lan"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CardSurfaceVariant,
                        contentColor = ContentSecondary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Manual IP",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Manual IP",
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Metric Card
 */
@Composable
fun CleanMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
        color = CardSurface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ContentSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ContentPrimary,
                    fontSize = 18.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = accentColor,
                    trackColor = CardBorder
                )
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ContentTertiary,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
