package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.RichMarkdownText
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AgentChatScreen(
    messages: List<AgentChatMessage>,
    isStreaming: Boolean,
    activeToolBadge: String?,
    pendingApproval: AgentApprovalRequest? = null,
    inputPrompt: String,
    onInputChange: (String) -> Unit,
    onSendPrompt: (String) -> Unit,
    onCancelAgent: () -> Unit,
    onApprovalResponse: (id: String, approved: Boolean, choice: String) -> Unit = { _, _, _ -> },
    onSlashCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val updated = if (inputPrompt.isBlank()) spokenText else "$inputPrompt $spokenText"
                onInputChange(updated)
            }
        }
    }

    val startVoiceInput = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara instruksi untuk AI Agent...")
            }
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice input tidak didukung di perangkat ini", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-scroll to bottom saat potongan teks baru masuk (streaming chunk)
    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length, activeToolBadge) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickActionMacros = listOf(
        "🧪 Test & Fix" to "Jalankan unit test dan perbaiki jika ada error",
        "📝 Git Diff" to "Cek git status dan jelaskan ringkasan perubahan file",
        "🚀 Commit & Push" to "Buat git commit dengan pesan yang deskriptif dan push",
        "🧹 Lint & Clean" to "Jalankan lint/format code dan rapikan kode yang tidak terpakai"
    )

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
        // Chat Sync Status Sub-bar
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
                            .size(6.dp)
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
                                imageVector = Icons.Outlined.Forum,
                                contentDescription = null,
                                tint = ContentTertiary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Belum ada pesan",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = ContentSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "Ketik instruksi di bawah untuk mulai berkolaborasi dengan AI Agent di Antigravity IDE.",
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
                        currentToolBadge = if (msg.isStreaming) activeToolBadge else msg.toolBadge,
                        onApprovalResponse = onApprovalResponse
                    )
                }
            }
        }

        // Sticky Approval Prompt Banner (Jika ada approval yang pending)
        if (pendingApproval != null && pendingApproval.status == ApprovalStatus.PENDING) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = AccentWarningSoft,
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentWarning.copy(alpha = 0.4f))
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
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = AccentWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Izin Diperlukan: ${pendingApproval.title}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ContentPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { onApprovalResponse(pendingApproval.id, true, "Izinkan") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Izinkan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { onApprovalResponse(pendingApproval.id, false, "Tolak") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Tolak", fontSize = 11.sp, color = ContentSecondary)
                        }
                    }
                }
            }
        }

        // Active Agent Status & Gentle Stop Button
        AnimatedVisibility(
            visible = isStreaming || activeToolBadge != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = CardSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
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
                            modifier = Modifier.size(14.dp),
                            color = AccentPrimary,
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

                    FilledTonalButton(
                        onClick = onCancelAgent,
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_agent_cancel"),
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
                            modifier = Modifier.size(13.dp)
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

        // Action & Slash Command Chips Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("action_chips_bar"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick Action Macros (1-Tap Send, Long-Press Edit)
            quickActionMacros.forEach { (label, prompt) ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .combinedClickable(
                            onClick = { onSendPrompt(prompt) },
                            onLongClick = {
                                onInputChange(prompt)
                                Toast.makeText(context, "Prompt disalin ke input untuk diedit", Toast.LENGTH_SHORT).show()
                            }
                        )
                        .testTag("macro_${label.filter { it.isLetter() }}"),
                    shape = RoundedCornerShape(20.dp),
                    color = AccentPrimarySoft,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = AccentPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Slash Commands
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ContentPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
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
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
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
                            text = "Ketik instruksi untuk AI Agent...",
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

                FilledTonalIconButton(
                    onClick = startVoiceInput,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("btn_voice_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = CardSurfaceVariant,
                        contentColor = AccentPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Dikte Suara",
                        modifier = Modifier.size(20.dp)
                    )
                }

                FilledIconButton(
                    onClick = {
                        if (inputPrompt.isNotBlank()) {
                            onSendPrompt(inputPrompt)
                        }
                    },
                    enabled = inputPrompt.isNotBlank(),
                    modifier = Modifier
                        .size(42.dp)
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
                        contentDescription = "Kirim Prompt",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = CardSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (request.status == ApprovalStatus.PENDING) AccentWarning.copy(alpha = 0.5f) else CardBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (request.status == ApprovalStatus.PENDING) AccentWarningSoft else AccentPrimarySoft
                ) {
                    Text(
                        text = request.actionType,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (request.status == ApprovalStatus.PENDING) AccentWarning else AccentPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Text(
                    text = when (request.status) {
                        ApprovalStatus.PENDING -> "Menunggu Izin"
                        ApprovalStatus.APPROVED -> "✓ Disetujui"
                        ApprovalStatus.REJECTED -> "✕ Ditolak"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = when (request.status) {
                            ApprovalStatus.PENDING -> AccentWarning
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
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ContentPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            if (request.status == ApprovalStatus.PENDING) {
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
            } else {
                Text(
                    text = "Respons tersimpan: ${request.selectedOption ?: "Selesai"}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ContentTertiary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
