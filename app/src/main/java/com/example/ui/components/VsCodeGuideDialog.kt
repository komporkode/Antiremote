package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun VsCodeGuideDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bridgeCommand = "npx @google/antigravity-bridge (atau via Extension Antigravity Remote)"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .testTag("vscode_guide_dialog"),
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
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.IntegrationInstructions,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

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

                Text(
                    text = "Ikuti 3 langkah cepat ini untuk menghubungkan VS Code ke Antigravity Remote:",
                    style = MaterialTheme.typography.bodySmall.copy(color = ContentSecondary)
                )

                // Step 1
                GuideStepItem(
                    stepNumber = "1",
                    title = "Buka Terminal di VS Code / Antigravity",
                    description = "Buka proyek Anda di VS Code, lalu buka tab Terminal terintegrasi."
                )

                // Step 2
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GuideStepItem(
                        stepNumber = "2",
                        title = "Jalankan Antigravity Bridge CLI",
                        description = "Ketik perintah berikut untuk mengaktifkan socket lokal:"
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TerminalDarkBg)
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        color = TerminalDarkBg
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bridgeCommand,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = AccentSuccess,
                                    fontSize = 12.sp
                                )
                            )

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Bridge Command", bridgeCommand))
                                    Toast.makeText(context, "Perintah disalin ke clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy command",
                                    tint = ContentSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Step 3
                GuideStepItem(
                    stepNumber = "3",
                    title = "Scan QR Code Pairing di Layar",
                    description = "Klik tombol 'Antigravity Remote' di Status Bar IDE atau jalankan command palette (Ctrl+Shift+P > 'Buka QR Code Pairing'), lalu scan."
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text("Saya Mengerti", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun GuideStepItem(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AccentPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary,
                    fontSize = 12.sp
                )
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = ContentPrimary,
                    fontSize = 13.sp
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ContentSecondary,
                    fontSize = 11.sp
                )
            )
        }
    }
}
