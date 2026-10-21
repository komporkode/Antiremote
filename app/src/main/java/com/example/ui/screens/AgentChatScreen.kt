package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.data.model.AgentChatMessage
import com.example.data.model.ChatSender
import com.example.ui.theme.*

@Composable
fun AgentChatScreen(
    messages: List<AgentChatMessage>,
    isStreaming: Boolean,
    activeToolBadge: String?,
    inputPrompt: String,
    onInputChange: (String) -> Unit,
    onSendPrompt: (String) -> Unit,
    onCancelAgent: () -> Unit,
    onSlashCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom saat potongan teks baru masuk (streaming chunk)
    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length, activeToolBadge) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val slashCommands = listOf(
        "🎯 /goal" to "/goal ",
        "📋 /plan" to "/plan ",
        "🔥 /grill-me" to "/grill-me ",
        "💡 /learn" to "/learn ",
        "🛠️ /fix" to "/fix ",
        "🔍 /review" to "/review "
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
            .testTag("agent_chat_screen")
    ) {
        // Antigravity IDE Realtime Status Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(AccentSuccess)
                    )
                    Text(
                        text = "Sinkronisasi Antigravity IDE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ContentSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Text(
                    text = "${messages.size} pesan",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ContentTertiary,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Chat Messages List (LazyColumn)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("agent_chat_messages_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = ContentTertiary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Belum ada pesan aktif",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = ContentSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "Pesan yang diketik di Antigravity IDE desktop maupun dari HP akan otomatis tersinkronisasi di sini secara realtime.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ContentTertiary,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                ),
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    StreamingChatBubble(
                        message = msg,
                        isCurrentlyStreaming = msg.isStreaming && isStreaming,
                        currentToolBadge = if (msg.isStreaming) activeToolBadge else msg.toolBadge
                    )
                }
            }
        }

        // Active Agent Status & Prominent Red Stop / Cancel Button (When agent is thinking / streaming)
        AnimatedVisibility(
            visible = isStreaming || activeToolBadge != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = AccentDanger.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentDanger.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AccentDanger,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = activeToolBadge ?: "AI sedang merespons...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ContentPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }

                    // Stop Streaming Button
                    FilledTonalButton(
                        onClick = onCancelAgent,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_agent_cancel_emergency"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AccentDangerSoft,
                            contentColor = AccentDanger
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Hentikan",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Berhenti",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        // Slash Command Chips Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("slash_command_chips_bar"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slashCommands.forEach { (label, command) ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSlashCommandClick(command) }
                        .testTag("chip_slash_${command.trim().removePrefix("/")}"),
                    shape = RoundedCornerShape(20.dp),
                    color = CardSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ContentPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }

        // Bottom Prompt Bar (Input Field + Send Button)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardSurface,
            tonalElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.key == Key.Enter && !keyEvent.isShiftPressed && keyEvent.type == KeyEventType.KeyUp) {
                                if (inputPrompt.isNotBlank()) {
                                    onSendPrompt(inputPrompt)
                                    true
                                } else false
                            } else false
                        }
                        .testTag("agent_chat_input"),
                    placeholder = {
                        Text(
                            text = "Ketik prompt untuk AI Agent...",
                            color = ContentTertiary,
                            fontSize = 13.sp
                        )
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurfaceVariant,
                        unfocusedContainerColor = CardSurfaceVariant,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = ContentPrimary,
                        unfocusedTextColor = ContentPrimary
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputPrompt.isNotBlank()) {
                            onSendPrompt(inputPrompt)
                        }
                    })
                )

                FilledIconButton(
                    onClick = {
                        if (inputPrompt.isNotBlank()) {
                            onSendPrompt(inputPrompt)
                        }
                    },
                    enabled = inputPrompt.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_send_agent_prompt"),
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AccentPrimary,
                        contentColor = Color.White,
                        disabledContainerColor = CardSurfaceVariant,
                        disabledContentColor = ContentTertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Kirim Prompt ke AI Agent",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Komponen Bubble Chat Streaming Real-time yang menampilkan potongan teks (chunks) via WebSocket
 */
@Composable
private fun StreamingChatBubble(
    message: AgentChatMessage,
    isCurrentlyStreaming: Boolean,
    currentToolBadge: String?
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isUser = message.sender == ChatSender.USER
    val isSystem = message.sender == ChatSender.SYSTEM

    // Pesan Sistem (Tersinkron dengan workstation)
    if (isSystem) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = AccentSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ContentSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
        return
    }

    // Animasi denyut kursor streaming
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_cursor")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isUser) "user_chat_bubble" else "agent_streaming_bubble"),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Label Pengirim & Tool execution badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = if (isUser) Icons.Default.Person else Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = if (isUser) AccentPrimary else AccentSecondary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isUser) "You" else "Antigravity Agent",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ContentSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            )

            // 3. AGENT TOOL EXECUTION BADGE (Badge kecil realtime)
            if (!isUser && currentToolBadge != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = AccentSecondary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentSecondary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(8.dp),
                            color = AccentSecondary,
                            strokeWidth = 1.5.dp
                        )
                        Text(
                            text = currentToolBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }

        // Bubble Surface
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
            color = if (isUser) AccentPrimary else CardSurface,
            border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Parse teks jika mengandung markdown code block ``` ... ```
                val textContent = message.text
                if (!isUser && textContent.contains("```")) {
                    MarkdownCodeSegments(
                        content = textContent,
                        isCurrentlyStreaming = isCurrentlyStreaming,
                        pulseAlpha = pulseAlpha
                    )
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = textContent.ifEmpty { if (isCurrentlyStreaming) "..." else "" },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isUser) Color.White else ContentPrimary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        )

                        // Streaming cursor indicator
                        if (isCurrentlyStreaming) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = 7.dp, height = 14.dp)
                                    .background(AccentPrimary.copy(alpha = pulseAlpha))
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Merender potongan teks yang mengandung Markdown code blocks
 */
@Composable
private fun MarkdownCodeSegments(
    content: String,
    isCurrentlyStreaming: Boolean,
    pulseAlpha: Float
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val parts = content.split("```")

    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) {
            // Ini adalah blok kode
            val firstLine = part.substringBefore("\n").trim()
            val codeBody = if (part.contains("\n")) part.substringAfter("\n") else part

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = CardSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = firstLine.ifEmpty { "code" },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(codeBody))
                                Toast.makeText(context, "Kode disalin", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Salin Kode",
                                tint = ContentSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Text(
                        text = codeBody,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = ContentPrimary,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        } else {
            // Teks biasa
            if (part.isNotEmpty()) {
                Text(
                    text = part,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ContentPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                )
            }
        }
    }

    if (isCurrentlyStreaming) {
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .size(width = 7.dp, height = 14.dp)
                .background(AccentPrimary.copy(alpha = pulseAlpha))
        )
    }
}
