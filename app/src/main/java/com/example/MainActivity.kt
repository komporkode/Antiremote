package com.example

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ConnectionStatus
import com.example.data.storage.ConnectionPreferences
import com.example.service.BridgeNotificationService
import com.example.ui.components.ConnectionDialog
import com.example.ui.components.QrScannerDialog
import com.example.ui.components.SettingsDialog
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

private fun triggerHapticFeedback(context: android.content.Context, preferences: ConnectionPreferences, pattern: LongArray) {
    if (!preferences.isHapticEnabled()) return
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
        }
    } catch (_: Exception) {}
}

@Composable
fun AntigravityApp(
    viewModel: AntigravityViewModel = viewModel()
) {
    val context = LocalContext.current
    val connectionPreferences = remember { ConnectionPreferences(context) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config by viewModel.connectionConfig.collectAsStateWithLifecycle()
    val agentTask by viewModel.agentTask.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    val workspaceInfo by viewModel.workspaceInfo.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAgentStreaming by viewModel.isAgentStreaming.collectAsStateWithLifecycle()
    val agentToolBadge by viewModel.agentToolBadge.collectAsStateWithLifecycle()

    // New Linear Minimalist features
    val pendingApproval by viewModel.pendingApproval.collectAsStateWithLifecycle()
    val workspaceDiffs by viewModel.workspaceDiffs.collectAsStateWithLifecycle()
    val conversationSessions by viewModel.conversationSessions.collectAsStateWithLifecycle()
    val liveTerminalOutput by viewModel.liveTerminalOutput.collectAsStateWithLifecycle()

    var isKeepScreenOn by remember { mutableStateOf(false) }

    // Mode Meja: Layar Selalu Aktif (Keep Screen On)
    DisposableEffect(isKeepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (isKeepScreenOn) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Haptic Alert saat Agent selesai streaming (1x getar halus)
    var wasStreaming by remember { mutableStateOf(false) }
    LaunchedEffect(isAgentStreaming) {
        if (wasStreaming && !isAgentStreaming) {
            triggerHapticFeedback(context, connectionPreferences, longArrayOf(0, 50))
        }
        wasStreaming = isAgentStreaming
    }

    // Haptic Alert saat ada Approval Request yang pending (2x getar)
    LaunchedEffect(pendingApproval) {
        if (pendingApproval != null && pendingApproval?.status == com.example.data.model.ApprovalStatus.PENDING) {
            triggerHapticFeedback(context, connectionPreferences, longArrayOf(0, 100, 100, 100))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        viewModel.initStorage(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    android.Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    LaunchedEffect(config.status) {
        if (config.status == ConnectionStatus.CONNECTED) {
            try {
                BridgeNotificationService.start(context)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to start BridgeNotificationService", e)
            }
        } else if (config.status == ConnectionStatus.DISCONNECTED || config.status == ConnectionStatus.ERROR) {
            try {
                BridgeNotificationService.stop(context)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to stop BridgeNotificationService", e)
            }
        }
    }

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
                isKeepScreenOn = isKeepScreenOn,
                onToggleKeepScreenOn = { isKeepScreenOn = !isKeepScreenOn },
                onOpenSettingsClick = { showSettingsDialog = true },
                onOpenConnectionDialog = { viewModel.showConnectionDialog(true) },
                onScanQrClick = { viewModel.showQrScanner(true) },
                onSaveAllClick = {
                    viewModel.triggerSaveAll()
                    Toast.makeText(context, "Menyimpan semua berkas di PC...", Toast.LENGTH_SHORT).show()
                }
            )
        },
        bottomBar = {
            // 4-Tab Bottom Navigation Bar (Linear Style)
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
                        DashboardTab.REVIEW -> (if (isSelected) Icons.Filled.Code else Icons.Outlined.Code) to "Review"
                        DashboardTab.TASKS -> (if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle) to "Tasks"
                        DashboardTab.TERMINAL -> (if (isSelected) Icons.Filled.Terminal else Icons.Outlined.Terminal) to "Terminal"
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                        icon = {
                            if (tab == DashboardTab.REVIEW && workspaceDiffs.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = AccentPrimary,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = if (workspaceDiffs.size > 99) "99+" else workspaceDiffs.size.toString(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label
                                )
                            }
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
                // Tab 1: 💬 AI Agent Live Chat & Human-in-the-Loop Approvals
                DashboardTab.CHAT -> {
                    AgentChatScreen(
                        messages = chatMessages,
                        isStreaming = isAgentStreaming,
                        activeToolBadge = agentToolBadge,
                        pendingApproval = pendingApproval,
                        inputPrompt = uiState.chatInput,
                        onInputChange = { viewModel.updateChatInput(it) },
                        onSendPrompt = { viewModel.sendChatPrompt(it) },
                        onCancelAgent = { viewModel.cancelAgentTask() },
                        onApprovalResponse = { id, approved, choice ->
                            viewModel.sendApprovalResponse(id, approved, choice)
                        },
                        onSlashCommandClick = { viewModel.insertSlashCommand(it) }
                    )
                }

                // Tab 2: 🔍 Code Review & Unified Diff Viewer
                DashboardTab.REVIEW -> {
                    ReviewDiffScreen(
                        diffs = workspaceDiffs,
                        selectedDiffPath = uiState.selectedDiffPath,
                        searchQuery = uiState.searchDiffQuery,
                        onSearchQueryChange = { viewModel.updateSearchDiffQuery(it) },
                        onSelectDiff = { viewModel.selectDiffPath(it) },
                        onRefreshDiffs = { viewModel.requestWorkspaceDiffs() },
                        onRequestAiRevision = { path ->
                            viewModel.updateChatInput("Tolong periksa dan revisi berkas $path: ")
                            viewModel.selectTab(DashboardTab.CHAT)
                        }
                    )
                }

                // Tab 3: 📋 Tasks, Multi-Session & Developer Shortcuts
                DashboardTab.TASKS -> {
                    TasksScreen(
                        agentTask = agentTask,
                        sessions = conversationSessions,
                        onSelectSession = { sessionId ->
                            viewModel.switchConversation(sessionId)
                            viewModel.selectTab(DashboardTab.CHAT)
                        },
                        onNewSession = {
                            viewModel.startNewConversation()
                            viewModel.selectTab(DashboardTab.CHAT)
                        },
                        onSaveAll = {
                            viewModel.triggerSaveAll()
                            Toast.makeText(context, "Save All berhasil terkirim", Toast.LENGTH_SHORT).show()
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
                        },
                        terminalLogs = terminalLogs
                    )
                }

                // Tab 4: 💻 Interactive Live Terminal (Bidirectional Shell)
                DashboardTab.TERMINAL -> {
                    LaunchedEffect(Unit) {
                        viewModel.requestTerminalBuffer()
                    }
                    LiveTerminalScreen(
                        output = liveTerminalOutput,
                        onSendInput = { viewModel.sendLiveTerminalInput(it) },
                        onInterrupt = { viewModel.sendTerminalInterrupt() },
                        onClear = { viewModel.clearLiveTerminal() },
                        onRequestAiFix = { prompt ->
                            viewModel.updateChatInput(prompt)
                            viewModel.selectTab(DashboardTab.CHAT)
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
            lastSavedConnection = viewModel.getLastSavedConnection(),
            onReconnectLast = { viewModel.reconnectLastKnown() },
            onIpChange = { viewModel.updateTargetIpInput(it) },
            onPortChange = { viewModel.updateTargetPortInput(it) },
            onConnect = { pin -> viewModel.connectToBridge(pin) },
            onScanQrClick = { viewModel.showQrScanner(true) },
            onDisconnect = { viewModel.disconnect() },
            onDemoMode = { viewModel.startDemoSimulation() },
            onQuickSelectHistory = { viewModel.setQuickIp(it) },
            onDismiss = { viewModel.showConnectionDialog(false) }
        )
    }

    // QR Code Scanner Dialog
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

    // App Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            preferences = connectionPreferences,
            isKeepScreenOn = isKeepScreenOn,
            onToggleKeepScreenOn = { isKeepScreenOn = !isKeepScreenOn },
            onDismiss = { showSettingsDialog = false }
        )
    }
}
