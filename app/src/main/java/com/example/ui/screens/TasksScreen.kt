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
    modifier: Modifier = Modifier
) {
    var showLogs by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
            .padding(14.dp)
            .testTag("tasks_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 1: Active Agent Task Tracker
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
                                imageVector = Icons.Outlined.CheckCircleOutline,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Tugas AI Saat Ini",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = ContentPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (agentTask.state) {
                                AgentExecutionState.COMPLETED -> AccentSuccessSoft
                                AgentExecutionState.ERROR -> AccentDangerSoft
                                AgentExecutionState.WAITING_APPROVAL -> AccentWarningSoft
                                else -> AccentPrimarySoft
                            }
                        ) {
                            Text(
                                text = when (agentTask.state) {
                                    AgentExecutionState.IDLE -> "Siap"
                                    AgentExecutionState.THINKING -> "Berpikir"
                                    AgentExecutionState.WRITING_CODE -> "Menulis Kode"
                                    AgentExecutionState.EXECUTING_COMMAND -> "Eksekusi Alat"
                                    AgentExecutionState.WAITING_APPROVAL -> "Menunggu Izin"
                                    AgentExecutionState.COMPLETED -> "Selesai"
                                    AgentExecutionState.ERROR -> "Error"
                                    else -> "Aktif"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = when (agentTask.state) {
                                        AgentExecutionState.COMPLETED -> AccentSuccess
                                        AgentExecutionState.ERROR -> AccentDanger
                                        AgentExecutionState.WAITING_APPROVAL -> AccentWarning
                                        else -> AccentPrimary
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Text(
                        text = agentTask.title.ifEmpty { "Pengembangan Fitur & Refactoring" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = ContentPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    )

                    Text(
                        text = agentTask.currentStepDescription.ifEmpty { "Menunggu instruksi tugas berikutnya..." },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ContentSecondary,
                            fontSize = 12.sp
                        )
                    )

                    // Linear Progress
                    LinearProgressIndicator(
                        progress = { agentTask.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentPrimary,
                        trackColor = CardSurfaceVariant
                    )
                }
            }
        }

        // Section 2: Conversation Sessions & Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Riwayat Sesi Percakapan",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = ContentPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )

                Button(
                    onClick = onNewSession,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Percakapan Baru", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        items(sessions, key = { it.id }) { session ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectSession(session.id) },
                shape = RoundedCornerShape(10.dp),
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (session.isActive) AccentPrimary else CardBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (session.isActive) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AccentPrimary)
                                )
                            }
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = if (session.isActive) ContentPrimary else ContentSecondary,
                                    fontWeight = if (session.isActive) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1
                            )
                        }

                        if (session.lastMessage.isNotBlank()) {
                            Text(
                                text = session.lastMessage,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ContentTertiary,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    if (session.isActive) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AccentPrimarySoft
                        ) {
                            Text(
                                text = "Aktif",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Quick Developer Actions
        item {
            Text(
                text = "Aksi Cepat Workstation",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = ContentPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    label = "Save All",
                    icon = Icons.Outlined.Save,
                    onClick = onSaveAll,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "Run Dev",
                    icon = Icons.Outlined.PlayArrow,
                    onClick = onRunDev,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "Build",
                    icon = Icons.Outlined.Build,
                    onClick = onRunBuild,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    label = "Git Pull",
                    icon = Icons.Outlined.CloudDownload,
                    onClick = onGitPull,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "Git Push",
                    icon = Icons.Outlined.CloudUpload,
                    onClick = onGitPush,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section 4: Collapsible Logs
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showLogs = !showLogs },
                shape = RoundedCornerShape(10.dp),
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.Terminal, contentDescription = null, tint = ContentSecondary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Log Eksekusi Sistem (${terminalLogs.size})",
                            style = MaterialTheme.typography.bodySmall.copy(color = ContentPrimary, fontWeight = FontWeight.Medium)
                        )
                    }

                    Icon(
                        imageVector = if (showLogs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = ContentTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (showLogs) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = AppBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(terminalLogs.takeLast(20)) { log ->
                            Text(
                                text = "[${log.timestamp}] ${log.message}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = when (log.level) {
                                        LogLevel.ERROR -> AccentDanger
                                        LogLevel.SUCCESS -> AccentSuccess
                                        LogLevel.WARN -> AccentWarning
                                        else -> ContentSecondary
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
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
