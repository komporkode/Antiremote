package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
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
import com.example.ui.components.ChatInputBar
import com.example.ui.components.StreamingChatBubble
import com.example.ui.theme.*
import kotlinx.coroutines.launch

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
    onSendImage: ((android.net.Uri, String) -> Unit)? = null,
    onCancelAgent: () -> Unit,
    onApprovalResponse: (id: String, approved: Boolean, choice: String) -> Unit = { _, _, _ -> },
    onSlashCommandClick: (String) -> Unit,
    isChatFocusMode: Boolean = false,
    onToggleChatFocusMode: () -> Unit = {},
    onRefreshChat: () -> Unit = {},
    conversationId: String? = null,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var attachedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        attachedImageUri = uri
    }

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

    // Filter out empty background tool calls from chat history
    val displayMessages = remember(messages, isStreaming) {
        messages.filter { msg ->
            msg.text.isNotBlank() || msg.approvalRequest != null || (msg.isStreaming && isStreaming)
        }
    }

        var hasInitiallyScrolledForConv by remember(conversationId) { mutableStateOf(false) }
    var previousMessageCount by remember { mutableIntStateOf(0) }

    // 1. Instant scroll ke paling bawah seketika saat chat dibuka atau saat berganti percakapan
    LaunchedEffect(conversationId, displayMessages.size) {
        if (displayMessages.isNotEmpty() && !hasInitiallyScrolledForConv) {
            listState.scrollToItem(displayMessages.size - 1)
            hasInitiallyScrolledForConv = true
            previousMessageCount = displayMessages.size
        }
    }

    // 2. Auto-scroll saat pesan baru masuk atau saat AI sedang streaming token
    LaunchedEffect(displayMessages.size, displayMessages.lastOrNull()?.text?.length, activeToolBadge) {
        if (displayMessages.isNotEmpty() && hasInitiallyScrolledForConv) {
            val isNewMessage = displayMessages.size > previousMessageCount
            previousMessageCount = displayMessages.size

            val isNearBottom = !listState.canScrollForward ||
                (listState.layoutInfo.totalItemsCount - 1 - listState.firstVisibleItemIndex) <= 2

            if (isNewMessage || isStreaming || isNearBottom) {
                listState.scrollToItem(displayMessages.size - 1)
            }
        }
    }

    val quickActionMacros = listOf(
        Triple("Test & Fix", Icons.Outlined.PlayArrow, "Jalankan unit test dan perbaiki jika ada error"),
        Triple("Git Diff", Icons.Outlined.Description, "Cek git status dan jelaskan ringkasan perubahan file"),
        Triple("Commit & Push", Icons.Outlined.CloudUpload, "Buat git commit dengan pesan yang deskriptif dan push"),
        Triple("Auto-fix", Icons.Outlined.AutoFixHigh, "Analisis error yang ada dan perbaiki kodenya secara otomatis")
    )

    val slashCommands = listOf(
        "/goal" to "/goal ",
        "/plan" to "/plan ",
        "/grill-me" to "/grill-me ",
        "/learn" to "/learn ",
        "/fix" to "/fix ",
        "/review" to "/review "
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
            .testTag("agent_chat_screen")
    ) {
        // Top Sub-Header: Focus Mode & Session Status
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
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
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (isStreaming) AccentWarning else AccentSuccess)
                    )
                    Text(
                        text = if (isStreaming) "Agent Merespons..." else "Agent Siap",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ContentSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                    if (!conversationId.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CardSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(
                                text = "#${conversationId.take(8)}",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ContentTertiary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                    IconButton(
                        onClick = onRefreshChat,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sinkronkan Chat dari IDE",
                            tint = AccentPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Focus Chat Mode Toggle Chip for PC IDE
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToggleChatFocusMode() },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isChatFocusMode) AccentPrimarySoft else CardSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isChatFocusMode) AccentPrimary else CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isChatFocusMode) Icons.Default.CloseFullscreen else Icons.Outlined.CenterFocusStrong,
                            contentDescription = null,
                            tint = if (isChatFocusMode) AccentPrimary else ContentSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isChatFocusMode) "PC: Fokus Chat" else "Fokuskan Chat di PC",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isChatFocusMode) AccentPrimary else ContentSecondary,
                                fontWeight = if (isChatFocusMode) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }


        // Chat Messages List (LazyColumn inside Box for Floating Action Button)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("agent_chat_messages_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (displayMessages.isEmpty()) {
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
                    items(displayMessages, key = { it.id }) { msg ->
                        StreamingChatBubble(
                            message = msg,
                            isCurrentlyStreaming = msg.isStreaming && isStreaming,
                            currentToolBadge = if (msg.isStreaming) activeToolBadge else msg.toolBadge,
                            onApprovalResponse = onApprovalResponse,
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }

            // Floating Down Arrow Button (Scroll to Bottom)
            val showScrollToBottom by remember {
                derivedStateOf {
                    listState.canScrollForward
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showScrollToBottom,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 12.dp, end = 16.dp)
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .clickable {
                            coroutineScope.launch {
                                if (displayMessages.isNotEmpty()) {
                                    listState.animateScrollToItem(displayMessages.size - 1)
                                }
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll ke Bawah",
                            tint = AccentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
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
            // Quick Action Macros with Tactile Press Micro-Animation
            quickActionMacros.forEach { (label, icon, prompt) ->
                var isPressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.90f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "ChipPressScale",
                    finishedListener = { isPressed = false }
                )

                Surface(
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(16.dp))
                        .combinedClickable(
                            onClick = {
                                isPressed = true
                                onSendPrompt(prompt)
                            },
                            onLongClick = {
                                onInputChange(prompt)
                                Toast.makeText(context, "Prompt disalin ke input untuk diedit", Toast.LENGTH_SHORT).show()
                            }
                        )
                        .testTag("macro_${label.filter { it.isLetter() }}"),
                    shape = RoundedCornerShape(16.dp),
                    color = CardSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = ContentPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
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

        // Bottom Chat Input Bar (WhatsApp Style - Spacious Capsule + Circular FAB)
        ChatInputBar(
            inputPrompt = inputPrompt,
            onInputChange = onInputChange,
            attachedImageUri = attachedImageUri,
            onAttachImageClick = { imagePickerLauncher.launch("image/*") },
            onRemoveAttachedImage = { attachedImageUri = null },
            onVoiceInputClick = startVoiceInput,
            onSendPrompt = onSendPrompt,
            onSendImage = onSendImage,
            isStreaming = isStreaming,
            onCancelAgent = onCancelAgent
        )
    }
}
