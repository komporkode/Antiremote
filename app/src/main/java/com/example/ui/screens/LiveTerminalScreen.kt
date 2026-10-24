package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*


import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

fun parseAnsiTerminalOutput(raw: String): AnnotatedString {
    return buildAnnotatedString {
        val pattern = Regex("\u001B\\[([0-9;]*)m")
        var cursor = 0
        var currentColor = Color(0xFFD4D4D4)
        var isBold = false

        for (match in pattern.findAll(raw)) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > cursor) {
                val text = raw.substring(cursor, start)
                val s = length
                append(text)
                addStyle(
                    SpanStyle(
                        color = currentColor,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
                    ),
                    s,
                    length
                )
            }

            val codes = match.groups[1]?.value?.split(";")?.mapNotNull { it.toIntOrNull() } ?: listOf(0)
            for (code in codes) {
                when (code) {
                    0 -> { currentColor = Color(0xFFD4D4D4); isBold = false }
                    1 -> isBold = true
                    31, 91 -> currentColor = Color(0xFFF87171) // Red
                    32, 92 -> currentColor = Color(0xFF4ADE80) // Green
                    33, 93 -> currentColor = Color(0xFFFBBF24) // Yellow
                    34, 94 -> currentColor = Color(0xFF60A5FA) // Blue
                    35, 95 -> currentColor = Color(0xFFC084FC) // Magenta
                    36, 96 -> currentColor = Color(0xFF38BDF8) // Cyan
                    37, 97 -> currentColor = Color(0xFFF1F5F9) // White
                    else -> {}
                }
            }
            cursor = end
        }
        if (cursor < raw.length) {
            val s = length
            append(raw.substring(cursor))
            addStyle(SpanStyle(color = currentColor, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal), s, length)
        }
    }
}

@Composable
fun LiveTerminalScreen(
    output: String,
    onSendInput: (String) -> Unit,
    onInterrupt: () -> Unit,
    onClear: () -> Unit,
    onRequestAiFix: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var fontSizeIndex by remember { mutableStateOf(1) } // 0: 10sp, 1: 12sp, 2: 14sp
    val fontSizes = remember { listOf(10f, 12f, 14f) }
    val currentFontSize = fontSizes[fontSizeIndex]

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // Auto-scroll to bottom whenever new output arrives
    LaunchedEffect(output) {
        if (output.isNotEmpty()) {
            verticalScrollState.animateScrollTo(verticalScrollState.maxValue)
        }
    }

    val quickCommands = listOf(
        "git status",
        "npm run dev",
        "npm run build",
        "ls",
        "npm test",
        "git diff"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentSuccess)
                )
                Text(
                    text = "LIVE TERMINAL (WORKSPACE)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = ContentPrimary,
                        letterSpacing = 0.8.sp
                    )
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tanya AI Button (Forward tail output ke Chat)
                if (onRequestAiFix != null && output.isNotBlank()) {
                    FilledTonalButton(
                        onClick = {
                            val tailLines = output.lines().takeLast(30).joinToString("\n").trim()
                            val prompt = "Tolong periksa dan perbaiki error berikut dari terminal:\n```\n$tailLines\n```"
                            onRequestAiFix(prompt)
                        },
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_terminal_ai_fix"),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AccentPrimarySoft,
                            contentColor = AccentPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tanya AI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Ctrl+C Interrupt Button
                OutlinedButton(
                    onClick = onInterrupt,
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("btn_terminal_interrupt"),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentWarning.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AccentWarning
                    )
                ) {
                    Text(
                        text = "Ctrl + C",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                // Font Size Toggle [A 12sp]
                OutlinedButton(
                    onClick = { fontSizeIndex = (fontSizeIndex + 1) % fontSizes.size },
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("btn_terminal_font_size"),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ContentSecondary
                    )
                ) {
                    Text(
                        text = "A ${currentFontSize.toInt()}sp",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // Clear Button
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("btn_terminal_clear")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = "Clear",
                        tint = ContentSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Terminal Console Viewport (Pro Dark + ANSI Colors + Smart Error AI)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TerminalDarkBg)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            val styledOutput = remember(output) {
                parseAnsiTerminalOutput(output)
            }

            val hasError = remember(output) {
                output.contains("error", ignoreCase = true) ||
                output.contains("failed", ignoreCase = true) ||
                output.contains("exception", ignoreCase = true) ||
                output.contains("fatal", ignoreCase = true)
            }

            if (output.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Menunggu output terminal dari workstation...\nKetik perintah di bawah untuk mulai.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = ContentTertiary,
                            fontSize = 12.sp
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = styledOutput,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = currentFontSize.sp,
                        lineHeight = (currentFontSize * 1.4f).sp
                    )
                )
            }

            // Smart Floating Banner: Tanya AI untuk Perbaiki Error
            if (hasError && onRequestAiFix != null) {
                Surface(
                    onClick = {
                        val clean = output.replace(Regex("\u001B\\[[;?0-9]*[a-zA-Z]"), "")
                        val tailLines = clean.lines().takeLast(30).joinToString("\n").trim()
                        onRequestAiFix("Tolong periksa dan perbaiki error terminal berikut:\n```\n$tailLines\n```")
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF7F1D1D),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Error terdeteksi • Tanya AI untuk Perbaiki",
                            color = Color(0xFFFEE2E2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Quick Command Chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickCommands) { cmd ->
                Surface(
                    onClick = { onSendInput(cmd + "\n") },
                    shape = RoundedCornerShape(6.dp),
                    color = CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Text(
                        text = cmd,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = ContentSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Command Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_terminal_command"),
                placeholder = {
                    Text(
                        text = "$ Masukkan perintah shell...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = ContentTertiary,
                            fontSize = 12.sp
                        )
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = ContentPrimary,
                    fontSize = 12.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) {
                        onSendInput(inputText + "\n")
                        inputText = ""
                    }
                })
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendInput(inputText + "\n")
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentPrimary)
                    .testTag("btn_send_terminal")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
