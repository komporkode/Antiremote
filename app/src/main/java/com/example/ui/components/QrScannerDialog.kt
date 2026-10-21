package com.example.ui.components

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.model.QrHandshakeData
import com.example.ui.theme.*

@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onQrScanned: (String) -> Unit,
    onShowVsCodeGuide: () -> Unit
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var scannedDataFeedback by remember { mutableStateOf<String?>(null) }
    var manualPayloadInput by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Izin kamera diperlukan untuk scan fisik. Anda juga dapat menggunakan Auto-Pair.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    fun handleDetectedQr(rawPayload: String) {
        if (scannedDataFeedback != null) return // debounce
        scannedDataFeedback = rawPayload

        // Haptic feedback
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(70)
            }
        } catch (_: Exception) {}

        val parsed = QrHandshakeData.parse(rawPayload)
        val targetText = if (parsed != null) "${parsed.ip}:${parsed.port}" else rawPayload.take(24)
        Toast.makeText(context, "QR Terdeteksi: $targetText", Toast.LENGTH_SHORT).show()

        onQrScanned(rawPayload)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .testTag("qr_scanner_dialog"),
            color = CardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
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
                                imageVector = Icons.Outlined.QrCodeScanner,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Scan QR Handshake",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = ContentPrimary
                                )
                            )
                            Text(
                                text = "Google Antigravity ML Kit Engine",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentSuccess,
                                    fontSize = 10.sp
                                )
                            )
                        }
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
                    text = "Arahkan kamera ke QR Code yang ditampilkan oleh ekstensi Antigravity di VS Code atau terminal daemon.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ContentSecondary,
                        fontSize = 12.sp
                    )
                )

                // Viewfinder Box containing CameraX + ML Kit View
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TerminalDarkBg)
                        .border(1.dp, if (scannedDataFeedback != null) AccentSuccess else CardBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        // Live CameraX with ML Kit Barcode Analyzer
                        CameraXQrScannerView(
                            modifier = Modifier.fillMaxSize(),
                            isTorchEnabled = isFlashlightOn,
                            isScanningActive = scannedDataFeedback == null,
                            onQrCodeDetected = { raw ->
                                handleDetectedQr(raw)
                            }
                        )
                    } else {
                        // Fallback UI when camera permission is not granted
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CameraAlt,
                                contentDescription = null,
                                tint = ContentTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Kamera Belum Diizinkan",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = ContentPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Berikan izin untuk scan QR langsung dari layar PC",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ContentTertiary,
                                    fontSize = 11.sp
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Izinkan Kamera", fontSize = 12.sp)
                            }
                        }
                    }

                    // Reticle overlay corners & dynamic Laser
                    Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        val w = size.width
                        val h = size.height
                        val cornerLen = 26.dp.toPx()
                        val strokeW = 3.dp.toPx()
                        val reticleColor = if (scannedDataFeedback != null) AccentSuccess else AccentPrimary

                        // Top-Left
                        drawLine(reticleColor, Offset(0f, 0f), Offset(cornerLen, 0f), strokeW)
                        drawLine(reticleColor, Offset(0f, 0f), Offset(0f, cornerLen), strokeW)

                        // Top-Right
                        drawLine(reticleColor, Offset(w, 0f), Offset(w - cornerLen, 0f), strokeW)
                        drawLine(reticleColor, Offset(w, 0f), Offset(w, cornerLen), strokeW)

                        // Bottom-Left
                        drawLine(reticleColor, Offset(0f, h), Offset(cornerLen, h), strokeW)
                        drawLine(reticleColor, Offset(0f, h), Offset(0f, h - cornerLen), strokeW)

                        // Bottom-Right
                        drawLine(reticleColor, Offset(w, h), Offset(w - cornerLen, h), strokeW)
                        drawLine(reticleColor, Offset(w, h), Offset(w, h - cornerLen), strokeW)

                        // Animated Laser Line (only when actively scanning)
                        if (scannedDataFeedback == null) {
                            val currentY = h * laserY
                            drawLine(
                                color = AccentSuccess,
                                start = Offset(6.dp.toPx(), currentY),
                                end = Offset(w - 6.dp.toPx(), currentY),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }

                    // Success Feedback Checkmark overlay
                    if (scannedDataFeedback != null) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(AccentSuccess.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Scanned",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Flashlight toggle button
                    if (hasCameraPermission) {
                        IconButton(
                            onClick = { isFlashlightOn = !isFlashlightOn },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CardSurface.copy(alpha = 0.85f))
                        ) {
                            Icon(
                                imageVector = if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flashlight",
                                tint = if (isFlashlightOn) AccentWarning else ContentSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Live Status Chip at bottom of viewfinder
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (scannedDataFeedback != null) "✓ HANDSHAKE CAPTURED" else if (hasCameraPermission) "ML KIT REAL-TIME SCAN" else "CAMERA STANDBY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (scannedDataFeedback != null) AccentSuccess else AccentPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Instant Auto-Pair Button (ideal for emulator testing and rapid connection)
                Button(
                    onClick = {
                        val sampleVsCodeQr = """{"app":"antigravity-remote","ip":"192.168.1.10","port":9876,"wsUrl":"ws://192.168.1.10:9876","httpUrl":"http://192.168.1.10:9876"}"""
                        handleDetectedQr(sampleVsCodeQr)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_auto_pair_qr_test"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentSuccess,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auto-Pair dari VS Code (Scan Test)",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }

                // Action Row: Paste pairing link & Setup Guide
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!clipText.isNullOrBlank()) {
                                handleDetectedQr(clipText)
                            } else {
                                showManualInput = !showManualInput
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AccentPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Paste Pairing Link",
                            color = AccentPrimary,
                            fontSize = 12.sp
                        )
                    }

                    TextButton(onClick = onShowVsCodeGuide) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ContentSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Petunjuk VS Code",
                            color = ContentSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Optional Manual Payload input field
                if (showManualInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualPayloadInput,
                            onValueChange = { manualPayloadInput = it },
                            placeholder = { Text("192.168.1.10:9876 atau JSON...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = ContentPrimary,
                                unfocusedTextColor = ContentPrimary
                            )
                        )

                        Button(
                            onClick = {
                                if (manualPayloadInput.isNotBlank()) {
                                    handleDetectedQr(manualPayloadInput)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                        ) {
                            Text("Pair", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
