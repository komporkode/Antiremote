package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun StreamingChatBubble(
    message: AgentChatMessage,
    isCurrentlyStreaming: Boolean,
    currentToolBadge: String?,
    onApprovalResponse: (id: String, approved: Boolean, choice: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == ChatSender.USER
    val isSystem = message.sender == ChatSender.SYSTEM
    val textContent = message.text
    val hasText = textContent.isNotBlank()
    val hasApproval = message.approvalRequest != null
    val shouldShowBubble = hasText || hasApproval || (isCurrentlyStreaming && !isUser)

    // Jangan render apa pun jika tidak ada teks dan tidak sedang aktif streaming
    if (!shouldShowBubble) return

    // Animasi blinking kursor saat streaming aktif
    val infiniteTransition = rememberInfiniteTransition(label = "CursorPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CursorAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender Header
        Row(
            modifier = Modifier.padding(bottom = 4.dp, start = if (isUser) 0.dp else 4.dp, end = if (isUser) 4.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = when {
                    isUser -> Icons.Default.Person
                    isSystem -> Icons.Default.Info
                    else -> Icons.Outlined.SmartToy
                },
                contentDescription = null,
                tint = if (isUser) AccentPrimary else ContentSecondary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = when {
                    isUser -> "You"
                    isSystem -> "System"
                    else -> "Antigravity Agent"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ContentSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            )

            if (!isUser && currentToolBadge != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isCurrentlyStreaming) AccentPrimarySoft else CardSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrentlyStreaming) AccentPrimary.copy(alpha = 0.3f) else CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isCurrentlyStreaming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(8.dp),
                                color = AccentPrimary,
                                strokeWidth = 1.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selesai",
                                tint = AccentSuccess,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        Text(
                            text = currentToolBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isCurrentlyStreaming) AccentPrimary else ContentSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }

        // Bubble Surface (Hanya render jika ada teks, request approval, atau sedang streaming aktif)
        if (shouldShowBubble) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp,
                            bottomStart = if (isUser) 14.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 14.dp
                        )
                    ),
                color = when {
                    isUser -> AccentPrimary
                    isSystem -> AccentDangerSoft
                    else -> CardSurface
                },
                border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (hasText) {
                        if (isUser) {
                            Text(
                                text = textContent,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            )
                        } else {
                            RichMarkdownText(
                                content = textContent,
                                textColor = ContentPrimary
                            )
                        }
                    } else if (isCurrentlyStreaming) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Sedang bekerja...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ContentTertiary,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = 6.dp, height = 12.dp)
                                    .background(AccentPrimary.copy(alpha = pulseAlpha))
                            )
                        }
                    }

                    if (isCurrentlyStreaming && hasText) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 7.dp, height = 14.dp)
                                .background(AccentPrimary.copy(alpha = pulseAlpha))
                        )
                    }

                    // Interactive Approval Card (Human-in-the-Loop)
                    if (message.approvalRequest != null) {
                        if (hasText) Spacer(modifier = Modifier.height(10.dp))
                        InteractiveApprovalCard(
                            request = message.approvalRequest,
                            onResponse = onApprovalResponse
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveApprovalCard(
    request: AgentApprovalRequest,
    onResponse: (id: String, approved: Boolean, choice: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isQuestion = request.actionType.equals("ASK_QUESTION", ignoreCase = true)
    var isCustomMode by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = CardSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (request.status == ApprovalStatus.PENDING) {
                if (isQuestion) AccentPrimary.copy(alpha = 0.6f) else AccentWarning.copy(alpha = 0.5f)
            } else CardBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isQuestion) {
                        AccentPrimarySoft
                    } else if (request.status == ApprovalStatus.PENDING) {
                        AccentWarningSoft
                    } else {
                        AccentPrimarySoft
                    }
                ) {
                    Text(
                        text = if (isQuestion) "PERTANYAAN AI" else request.actionType,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isQuestion) AccentPrimary else if (request.status == ApprovalStatus.PENDING) AccentWarning else AccentPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Text(
                    text = when (request.status) {
                        ApprovalStatus.PENDING -> if (isQuestion) "Menunggu Jawaban" else "Menunggu Izin"
                        ApprovalStatus.APPROVED -> if (isQuestion) "✓ Terjawab" else "✓ Disetujui"
                        ApprovalStatus.REJECTED -> if (isQuestion) "✕ Dilewati" else "✕ Ditolak"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = when (request.status) {
                            ApprovalStatus.PENDING -> if (isQuestion) AccentPrimary else AccentWarning
                            ApprovalStatus.APPROVED -> AccentSuccess
                            ApprovalStatus.REJECTED -> AccentDanger
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                )
            }

            Text(
                text = request.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = ContentPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            )

            if (request.detail.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    color = AppBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Text(
                        text = request.detail,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ContentPrimary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    )
                }
            }

            if (request.status == ApprovalStatus.PENDING) {
                if (isQuestion) {
                    // Tampilan Opsi Bertingkat Vertikal (Pilihan Ganda Rapi)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        request.options.forEachIndexed { index, option ->
                            val isRecommended = option.contains("Recommended", ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onResponse(request.id, true, option)
                                    },
                                color = if (isRecommended) AccentPrimarySoft.copy(alpha = 0.25f) else CardSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isRecommended) AccentPrimary.copy(alpha = 0.7f) else CardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isRecommended) AccentPrimary else CardSurfaceVariant,
                                        border = if (!isRecommended) androidx.compose.foundation.BorderStroke(1.dp, CardBorder) else null
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isRecommended) Color.White else ContentPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = ContentPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            fontWeight = if (isRecommended) FontWeight.Medium else FontWeight.Normal
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Input Jawaban Kustom (Other)
                        if (isCustomMode) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = customText,
                                    onValueChange = { customText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            text = "Tulis jawaban kustom atau instruksi Anda...",
                                            fontSize = 12.sp,
                                            color = ContentTertiary
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        color = ContentPrimary,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 3,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentPrimary,
                                        unfocusedBorderColor = CardBorder,
                                        focusedContainerColor = AppBg,
                                        unfocusedContainerColor = AppBg
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (customText.isNotBlank()) {
                                                onResponse(request.id, true, customText)
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                        enabled = customText.isNotBlank()
                                    ) {
                                        Text("Kirim Jawaban", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = { isCustomMode = false },
                                        modifier = Modifier.height(34.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                                    ) {
                                        Text("Batal", fontSize = 11.sp, color = ContentSecondary)
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { isCustomMode = true },
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("✍ Tulis Jawaban Lain", fontSize = 11.sp, color = ContentSecondary)
                                }
                                OutlinedButton(
                                    onClick = { onResponse(request.id, false, "Skip") },
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("Lewati (Skip)", fontSize = 11.sp, color = ContentTertiary)
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        request.options.forEach { option ->
                            val isPositive = option.contains("izinkan", ignoreCase = true) ||
                                    option.contains("ya", ignoreCase = true) ||
                                    option.contains("approve", ignoreCase = true)

                            Button(
                                onClick = { onResponse(request.id, isPositive, option) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPositive) AccentPrimary else CardSurface,
                                    contentColor = if (isPositive) Color.White else ContentPrimary
                                ),
                                border = if (!isPositive) androidx.compose.foundation.BorderStroke(1.dp, CardBorder) else null,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    color = if (request.status == ApprovalStatus.APPROVED) AccentSuccessSoft else AccentDangerSoft
                ) {
                    Text(
                        text = if (request.status == ApprovalStatus.APPROVED) {
                            "✓ Terpilih: ${request.selectedOption ?: "Disetujui"}"
                        } else {
                            "✕ Keputusan: ${request.selectedOption ?: "Ditolak / Dilewati"}"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (request.status == ApprovalStatus.APPROVED) AccentSuccess else AccentDanger,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

