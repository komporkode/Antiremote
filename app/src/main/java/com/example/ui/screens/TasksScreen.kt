package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun TasksScreen(
    agentTask: AgentTask,
    sessions: List<ConversationSession>,
    onSelectSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onSaveAll: () -> Unit,
    onRunDev: () -> Unit,
    onRunBuild: () -> Unit,
    onGitPull: () -> Unit,
    onGitPush: () -> Unit,
    terminalLogs: List<TerminalLog>,
    workspaceScripts: List<WorkspaceScript> = emptyList(),
    onRunScript: (String) -> Unit = {},
    isChatFocusMode: Boolean = false,
    onToggleChatFocusMode: () -> Unit = {},
    activeServers: List<ActiveDevServer> = emptyList(),
    onTriggerHotReload: () -> Unit = {},
    onKillPort: (Int) -> Unit = {},
    onOpenPreview: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
            .padding(14.dp)
            .testTag("tasks_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 1: AI Agent Plan & Subtask Tracker
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (agentTask.state) {
                                            AgentExecutionState.IDLE -> AccentSuccess
                                            AgentExecutionState.COMPLETED -> AccentSuccess
                                            AgentExecutionState.ERROR -> AccentDanger
                                            else -> AccentPrimary
                                        }
                                    )
                            )
                            Text(
                                text = "RENCANA KERJA AI AGENT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ContentPrimary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (agentTask.state) {
                                AgentExecutionState.IDLE -> AccentSuccessSoft
                                AgentExecutionState.COMPLETED -> AccentSuccessSoft
                                AgentExecutionState.ERROR -> AccentDangerSoft
                                AgentExecutionState.WAITING_APPROVAL -> AccentWarningSoft
                                else -> AccentPrimarySoft
                            }
                        ) {
                            Text(
                                text = when (agentTask.state) {
                                    AgentExecutionState.IDLE -> "SIAP"
                                    AgentExecutionState.THINKING -> "BERPIKIR"
                                    AgentExecutionState.WRITING_CODE -> "MENULIS KODE"
                                    AgentExecutionState.EXECUTING_COMMAND -> "EKSEKUSI ALAT"
                                    AgentExecutionState.WAITING_APPROVAL -> "MENUNGGU IZIN"
                                    AgentExecutionState.COMPLETED -> "SELESAI"
                                    AgentExecutionState.ERROR -> "ERROR"
                                    else -> "AKTIF"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = when (agentTask.state) {
                                        AgentExecutionState.IDLE -> AccentSuccess
                                        AgentExecutionState.COMPLETED -> AccentSuccess
                                        AgentExecutionState.ERROR -> AccentDanger
                                        AgentExecutionState.WAITING_APPROVAL -> AccentWarning
                                        else -> AccentPrimary
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Text(
                        text = agentTask.title.ifEmpty { "Pengembangan & Refactoring Workstation" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = ContentPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )

                    Text(
                        text = agentTask.currentStepDescription.ifEmpty { "Siap menerima instruksi dari Chat." },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ContentSecondary,
                            fontSize = 12.sp
                        )
                    )

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { agentTask.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentPrimary,
                        trackColor = CardSurfaceVariant
                    )

                    // Subtask / Checklist Items
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SubtaskCheckItem(
                            title = "Analisis Workspace & Rencana",
                            isDone = agentTask.state != AgentExecutionState.IDLE
                        )
                        SubtaskCheckItem(
                            title = "Eksekusi Alat & Modifikasi Berkas",
                            isDone = agentTask.state == AgentExecutionState.COMPLETED,
                            isInProgress = agentTask.state in listOf(AgentExecutionState.WRITING_CODE, AgentExecutionState.EXECUTING_COMMAND)
                        )
                        SubtaskCheckItem(
                            title = "Verifikasi & Selesai",
                            isDone = agentTask.state == AgentExecutionState.COMPLETED
                        )
                    }
                }
            }
        }

        // Section 2: Dev Servers & Background Processes Monitor
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Dns,
                                contentDescription = null,
                                tint = if (activeServers.isNotEmpty()) AccentSuccess else ContentTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "DEV SERVERS & PROSES BACKGROUND",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ContentPrimary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (activeServers.isNotEmpty()) AccentSuccessSoft else CardSurfaceVariant
                        ) {
                            Text(
                                text = if (activeServers.isNotEmpty()) "${activeServers.size} Aktif" else "Offline",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (activeServers.isNotEmpty()) AccentSuccess else ContentTertiary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }

                    if (activeServers.isNotEmpty()) {
                        activeServers.forEach { server ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardSurfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(AccentSuccess)
                                    )
                                    Column {
                                        Text(
                                            text = "${server.name} (: ${server.port})",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = ContentPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            )
                                        )
                                        Text(
                                            text = server.lanUrl.ifEmpty { server.localUrl },
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = ContentTertiary,
                                                fontSize = 9.5.sp
                                            )
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Hot Reload ('r')
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onTriggerHotReload() },
                                        shape = RoundedCornerShape(4.dp),
                                        color = AccentPrimarySoft
                                    ) {
                                        Text(
                                            text = "Reload",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = AccentPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }

                                    // Buka Preview
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onOpenPreview() },
                                        shape = RoundedCornerShape(4.dp),
                                        color = AccentSuccessSoft
                                    ) {
                                        Text(
                                            text = "Preview",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = AccentSuccess,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }

                                    // Kill Port
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onKillPort(server.port) },
                                        shape = RoundedCornerShape(4.dp),
                                        color = AccentDangerSoft
                                    ) {
                                        Text(
                                            text = "Kill",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = AccentDanger,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardSurfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tidak ada dev server aktif",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ContentTertiary,
                                    fontSize = 11.sp
                                )
                            )
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onRunDev() },
                                shape = RoundedCornerShape(4.dp),
                                color = AccentPrimarySoft
                            ) {
                                Text(
                                    text = "Run Dev",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AccentPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Quick Developer Actions Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AKSI CEPAT WORKSTATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ContentPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 11.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton("Save All", Icons.Outlined.Save, onSaveAll, Modifier.weight(1f))
                    QuickActionButton("Git Pull", Icons.Outlined.CloudDownload, onGitPull, Modifier.weight(1f))
                    QuickActionButton("Git Push", Icons.Outlined.CloudUpload, onGitPush, Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton("Run Dev", Icons.Outlined.PlayArrow, onRunDev, Modifier.weight(1f))
                    QuickActionButton("Build", Icons.Outlined.Build, onRunBuild, Modifier.weight(1f))
                    QuickActionButton(
                        if (isChatFocusMode) "Reset PC" else "Fokus PC",
                        Icons.Outlined.FitScreen,
                        onToggleChatFocusMode,
                        Modifier.weight(1f)
                    )
                }
            }
        }

        // Section 4: Workspace Scripts (package.json / tasks.json)
        if (workspaceScripts.isNotEmpty()) {
            item {
                Text(
                    text = "SKRIP WORKSPACE (${workspaceScripts.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ContentPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 11.sp
                    )
                )
            }

            items(workspaceScripts, key = { it.name }) { script ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onRunScript(script.command) },
                    shape = RoundedCornerShape(8.dp),
                    color = CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (script.source == "package.json") Icons.Default.Terminal else Icons.Default.Task,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Column {
                                Text(
                                    text = script.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = ContentPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                                Text(
                                    text = script.command,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = ContentTertiary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CardSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Jalankan",
                                    tint = AccentSuccess,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Run",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AccentSuccess,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtaskCheckItem(
    title: String,
    isDone: Boolean = false,
    isInProgress: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = when {
                isDone -> Icons.Default.CheckCircle
                isInProgress -> Icons.Default.Pending
                else -> Icons.Outlined.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = when {
                isDone -> AccentSuccess
                isInProgress -> AccentPrimary
                else -> ContentTertiary
            },
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (isDone) ContentPrimary else ContentSecondary,
                fontSize = 12.sp,
                fontWeight = if (isInProgress) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = AccentPrimary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ContentPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            )
        }
    }
}
