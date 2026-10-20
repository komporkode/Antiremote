package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun AgentCommanderScreen(
    task: AgentTask,
    promptInput: String,
    onPromptInputChange: (String) -> Unit,
    onDispatchPrompt: (String?) -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "agent_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(14.dp)
            .testTag("agent_commander_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live Agent Status Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, LaserPurple.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header row
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
                                .background(
                                    if (task.state == AgentExecutionState.THINKING || task.state == AgentExecutionState.WRITING_CODE)
                                        LaserPurple.copy(alpha = pulseAlpha)
                                    else NeonMint
                                )
                        )
                        Text(
                            text = "AGENTIC ENGINE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = LaserPurple,
                                letterSpacing = 1.sp
                            )
                        )

                        // antigravity.google tag
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonCyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, NeonCyan)
                        ) {
                            Text(
                                text = task.platformUrl,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = NeonCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Pause / Resume Toggle
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            .testTag("btn_toggle_agent_pause")
                    ) {
                        Icon(
                            imageVector = if (task.state == AgentExecutionState.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Toggle Pause",
                            tint = if (task.state == AgentExecutionState.PAUSED) NeonMint else CyberAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Sub-header tags: Model & Autonomy
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LaserPurple.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, LaserPurple)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = LaserPurple,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = task.aiModel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = LaserPurple,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceVariantDark,
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, SurfaceBorder)
                    ) {
                        Text(
                            text = "Multi-Interface (Editor • Terminal • Browser)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Goal Prompt Title
                Text(
                    text = task.goalPrompt,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                // Current Step Sub-text
                Text(
                    text = "» ${task.currentStepDescription}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                )

                // Progress Bar
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = LaserPurple,
                    trackColor = SurfaceBorder
                )

                // Metrics row: Tokens & Step Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tokens: ${task.tokenUsage} (Gemini 3.0)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                    )

                    val stateLabel = when (task.state) {
                        AgentExecutionState.THINKING -> "THINKING..."
                        AgentExecutionState.WRITING_CODE -> "EDITING WORKSPACE"
                        AgentExecutionState.EXECUTING_COMMAND -> "RUNNING TERMINAL"
                        AgentExecutionState.BROWSER_TESTING -> "BROWSER VALIDATION"
                        AgentExecutionState.WAITING_APPROVAL -> "AWAITING APPROVAL"
                        AgentExecutionState.COMPLETED -> "TASK COMPLETE"
                        AgentExecutionState.PAUSED -> "PAUSED"
                        AgentExecutionState.ERROR -> "AGENT HALTED"
                        AgentExecutionState.IDLE -> "IDLE"
                    }

                    Text(
                        text = stateLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = LaserPurple,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        // Section: Step Execution Timeline
        Text(
            text = "AUTONOMOUS EXECUTION TRACE",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(task.steps, key = { it.stepNumber }) { step ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceVariantDark,
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        when (step.status) {
                            StepStatus.SUCCESS -> NeonMint.copy(alpha = 0.4f)
                            StepStatus.IN_PROGRESS -> LaserPurple.copy(alpha = 0.8f)
                            StepStatus.FAILED -> NeonRed.copy(alpha = 0.6f)
                            StepStatus.PENDING -> SurfaceBorder
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Status Icon
                        when (step.status) {
                            StepStatus.SUCCESS -> Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = NeonMint,
                                modifier = Modifier.size(18.dp)
                            )
                            StepStatus.IN_PROGRESS -> CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = LaserPurple
                            )
                            StepStatus.FAILED -> Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Failed",
                                tint = NeonRed,
                                modifier = Modifier.size(18.dp)
                            )
                            StepStatus.PENDING -> Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceBorder)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Step ${step.stepNumber}: ${step.toolName}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = LaserPurple,
                                        fontSize = 11.sp
                                    )
                                )

                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = SurfaceBorder
                                ) {
                                    Text(
                                        text = step.targetInterface,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = NeonCyan
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }

                                if (step.durationMs > 0) {
                                    Text(
                                        text = "${step.durationMs}ms",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = TextTertiary,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                            Text(
                                text = step.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Official Antigravity Agent Slash Commands (TECHNICAL_SPEC Section 5.2)
        val presets = listOf(
            "/goal selesaikan unit test yang gagal",
            "/plan arsitektur sistem real-time",
            "/grill-me uji asumsi teknis ini",
            "/learn simpan preferensi clean code",
            "audit performa dan keamanan aplikasi"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { preset ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDispatchPrompt(preset) },
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceVariantDark,
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, SurfaceBorder)
                ) {
                    Text(
                        text = preset,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Prompt Dispatcher Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, LaserPurple.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = LaserPurple,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp)
            )

            TextField(
                value = promptInput,
                onValueChange = onPromptInputChange,
                placeholder = {
                    Text(
                        "Instruct Antigravity Agent (Gemini 3.0)...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("agent_prompt_input"),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onDispatchPrompt(null) })
            )

            IconButton(
                onClick = { onDispatchPrompt(null) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LaserPurple)
                    .testTag("btn_dispatch_agent_prompt")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Dispatch Prompt",
                    tint = DarkBg,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
