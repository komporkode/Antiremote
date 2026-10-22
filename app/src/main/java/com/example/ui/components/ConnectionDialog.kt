package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.ConnectionConfig
import com.example.data.model.ConnectionStatus
import com.example.data.storage.SavedConnection
import com.example.ui.theme.*

@Composable
fun ConnectionDialog(
    config: ConnectionConfig,
    ipInput: String,
    portInput: String,
    history: List<String>,
    lastSavedConnection: SavedConnection? = null,
    onReconnectLast: () -> Unit = {},
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnect: (String?) -> Unit,
    onScanQrClick: () -> Unit,
    onDisconnect: () -> Unit,
    onDemoMode: () -> Unit,
    onQuickSelectHistory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        var pinInput by remember { mutableStateOf("") }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .testTag("connection_dialog"),
            color = CardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Router,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Koneksi VS Code",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ContentPrimary
                            )
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ContentSecondary
                        )
                    }
                }

                // 1-Tap Reconnect Card
                if (lastSavedConnection != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onReconnectLast()
                                onDismiss()
                            }
                            .testTag("btn_reconnect_last"),
                        shape = RoundedCornerShape(10.dp),
                        color = AccentPrimarySoft,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Sambungkan Kembali (1-Tap)",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AccentPrimary,
                                            fontSize = 12.sp
                                        )
                                    )
                                    Text(
                                        text = "${lastSavedConnection.host}:${lastSavedConnection.port}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = ContentSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Scan QR Fast Action Button
                Button(
                    onClick = {
                        onDismiss()
                        onScanQrClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_dialog_scan_qr"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentSuccess,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan QR Code dari VS Code",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = CardBorder)
                    Text(
                        text = " ATAU MANUAL ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ContentTertiary,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = CardBorder)
                }

                // IP Address and Port Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = onIpChange,
                        label = { Text("IP / Domain Cloud", fontSize = 12.sp) },
                        placeholder = { Text("192.168.1.x atau domain cloud") },
                        modifier = Modifier
                            .weight(1.8f)
                            .testTag("ip_input_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = ContentPrimary,
                            unfocusedTextColor = ContentPrimary
                        )
                    )

                    OutlinedTextField(
                        value = portInput,
                        onValueChange = onPortChange,
                        label = { Text("Port", fontSize = 12.sp) },
                        placeholder = { Text("9876") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("port_input_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = ContentPrimary,
                            unfocusedTextColor = ContentPrimary
                        )
                    )
                }

                // Optional Cloud Security PIN
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it },
                    label = { Text("PIN Keamanan Cloud (Opsional)", fontSize = 12.sp) },
                    placeholder = { Text("6 Digit PIN (contoh: 739201)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = ContentPrimary,
                        unfocusedTextColor = ContentPrimary
                    )
                )

                // Quick Connect Presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Riwayat Koneksi:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ContentTertiary
                        )
                    )
                    history.forEach { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onQuickSelectHistory(item) }
                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp)),
                            color = CardSurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = ContentSecondary
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = ContentTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onConnect(pinInput.ifBlank { null }) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_connect_lan"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cable,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hubungkan Manual",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onDemoMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("btn_launch_demo"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ContentPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AccentPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mode Simulasi Offline",
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }

                    if (config.status == ConnectionStatus.CONNECTED) {
                        TextButton(
                            onClick = onDisconnect,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Putuskan Koneksi",
                                color = AccentDanger,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
