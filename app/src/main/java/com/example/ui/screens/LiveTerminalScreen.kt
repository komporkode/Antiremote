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

        // Terminal Console Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TerminalDarkBg)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            val cleanOutput = remember(output) {
                // Strip ANSI escape codes for clean mobile rendering
                output.replace(Regex("\u001B\\[[;?0-9]*[a-zA-Z]"), "")
            }

            if (cleanOutput.isBlank()) {
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
                    text = cleanOutput,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFD4D4D4),
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                )
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
