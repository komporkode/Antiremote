package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ConnectionDialog
import com.example.ui.components.QrScannerDialog
import com.example.ui.components.TopHudBar
import com.example.ui.components.VsCodeGuideDialog
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AntigravityViewModel
import com.example.ui.viewmodel.DashboardTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AntigravityApp()
            }
        }
    }
}

@Composable
fun AntigravityApp(
    viewModel: AntigravityViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config by viewModel.connectionConfig.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    val workspaceInfo by viewModel.workspaceInfo.collectAsStateWithLifecycle()
    val workspaceFiles by viewModel.workspaceFileList.collectAsStateWithLifecycle()
    val activeFileContent by viewModel.activeFileContent.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAgentStreaming by viewModel.isAgentStreaming.collectAsStateWithLifecycle()
    val agentToolBadge by viewModel.agentToolBadge.collectAsStateWithLifecycle()

    // Show toast on pairing success
    LaunchedEffect(uiState.pairingToastMessage) {
        uiState.pairingToastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearPairingToast()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopHudBar(
                config = config,
                workspaceInfo = workspaceInfo,
                onOpenConnectionDialog = { viewModel.showConnectionDialog(true) },
                onScanQrClick = { viewModel.showQrScanner(true) },
                onSaveAllClick = {
                    viewModel.triggerSaveAll()
                    Toast.makeText(context, "Menyimpan semua berkas di VS Code...", Toast.LENGTH_SHORT).show()
                }
            )
        },
        bottomBar = {
            // 3-Tab Bottom Navigation Bar
            NavigationBar(
                modifier = Modifier
                    .testTag("bottom_navigation_bar")
                    .border(androidx.compose.foundation.BorderStroke(1.dp, CardBorder)),
                containerColor = CardSurface,
                windowInsets = WindowInsets.navigationBars
            ) {
                DashboardTab.values().forEach { tab ->
                    val isSelected = uiState.selectedTab == tab
                    val (icon, label) = when (tab) {
                        DashboardTab.CHAT -> (if (isSelected) Icons.Filled.Forum else Icons.Outlined.Forum) to "Chat"
                        DashboardTab.FILES -> (if (isSelected) Icons.Filled.Folder else Icons.Outlined.Folder) to "Files"
                        DashboardTab.TELEMETRY -> (if (isSelected) Icons.Filled.Dashboard else Icons.Outlined.Dashboard) to "Dashboard"
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentPrimary,
                            selectedTextColor = AccentPrimary,
                            unselectedIconColor = ContentSecondary,
                            unselectedTextColor = ContentSecondary,
                            indicatorColor = AccentPrimarySoft
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AppBg)
        ) {
            when (uiState.selectedTab) {
                // Tab 1: 💬 AI Agent Live Chat (Fitur Utama)
                DashboardTab.CHAT -> {
                    AgentChatScreen(
                        messages = chatMessages,
                        isStreaming = isAgentStreaming,
                        activeToolBadge = agentToolBadge,
                        inputPrompt = uiState.chatInput,
                        onInputChange = { viewModel.updateChatInput(it) },
                        onSendPrompt = { viewModel.sendChatPrompt(it) },
                        onCancelAgent = { viewModel.cancelAgentTask() },
                        onSlashCommandClick = { viewModel.insertSlashCommand(it) }
                    )
                }

                // Tab 2: 📁 Workspace Files & Code Viewer
                DashboardTab.FILES -> {
                    WorkspaceFilesScreen(
                        files = workspaceFiles,
                        activeFileContent = activeFileContent,
                        searchQuery = uiState.searchFileQuery,
                        onSearchQueryChange = { viewModel.updateSearchFileQuery(it) },
                        onFileClick = { viewModel.openFile(it) },
                        onCloseCodeViewer = { viewModel.closeCodeViewer() },
                        onSaveFileEdits = {
                            viewModel.saveActiveFileEdits(it)
                            Toast.makeText(context, "Perubahan berkas disimpan", Toast.LENGTH_SHORT).show()
                        },
                        onRefreshFiles = {
                            viewModel.requestWorkspaceFiles()
                            viewModel.requestWorkspaceInfo()
                        }
                    )
                }

                // Tab 3: ⚡ Telemetri CPU/RAM & Terminal Remote
                DashboardTab.TELEMETRY -> {
                    TelemetryScreen(
                        telemetry = telemetry,
                        connectionConfig = config,
                        terminalLogs = terminalLogs,
                        terminalInput = uiState.terminalInput,
                        onTerminalInputChange = { viewModel.updateTerminalInput(it) },
                        onSendTerminalCommand = { viewModel.executeTerminalCommand(it) },
                        onOpenConnectionDialog = { viewModel.showConnectionDialog(true) },
                        onScanQrClick = { viewModel.showQrScanner(true) },
                        onSaveAll = {
                            viewModel.triggerSaveAll()
                            Toast.makeText(context, "Save All terkirim ke VS Code", Toast.LENGTH_SHORT).show()
                        },
                        onRunDev = {
                            viewModel.triggerRunDev()
                            Toast.makeText(context, "Menjalankan npm run dev di Workstation", Toast.LENGTH_SHORT).show()
                        },
                        onRunBuild = {
                            viewModel.triggerRunBuild()
                            Toast.makeText(context, "Menjalankan npm run build di Workstation", Toast.LENGTH_SHORT).show()
                        },
                        onGitPull = {
                            viewModel.triggerGitPull()
                            Toast.makeText(context, "Menjalankan git pull di Workstation", Toast.LENGTH_SHORT).show()
                        },
                        onGitPush = {
                            viewModel.triggerGitPush()
                            Toast.makeText(context, "Menjalankan git push di Workstation", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Manual Connection Dialog
    if (uiState.showConnectionDialog) {
        ConnectionDialog(
            config = config,
            ipInput = uiState.targetIpInput,
            portInput = uiState.targetPortInput,
            history = uiState.connectionHistory,
            onIpChange = { viewModel.updateTargetIpInput(it) },
            onPortChange = { viewModel.updateTargetPortInput(it) },
            onConnect = { viewModel.connectToBridge() },
            onScanQrClick = { viewModel.showQrScanner(true) },
            onDisconnect = { viewModel.disconnect() },
            onDemoMode = { viewModel.startDemoSimulation() },
            onQuickSelectHistory = { viewModel.setQuickIp(it) },
            onDismiss = { viewModel.showConnectionDialog(false) }
        )
    }

    // QR Code Scanner Dialog (CameraX + ML Kit with Auto-Connect)
    if (uiState.showQrScannerDialog) {
        QrScannerDialog(
            onDismiss = { viewModel.showQrScanner(false) },
            onQrScanned = { payload -> viewModel.pairWithQrCode(payload) },
            onShowVsCodeGuide = {
                viewModel.showQrScanner(false)
                viewModel.showVsCodeGuide(true)
            }
        )
    }

    // VS Code Setup Guide Dialog
    if (uiState.showVsCodeGuideDialog) {
        VsCodeGuideDialog(
            onDismiss = { viewModel.showVsCodeGuide(false) }
        )
    }
}
