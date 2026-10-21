package com.example.ui.viewmodel

enum class DashboardTab(val label: String) {
    CHAT("Chat"),
    REVIEW("Review"),
    TASKS("Tasks"),
    TERMINAL("Terminal")
}

enum class LogFilter(val label: String) {
    ALL("All"),
    COMMANDS("Commands"),
    ERRORS("Errors"),
    SUCCESS("Success")
}

data class AntigravityUiState(
    val selectedTab: DashboardTab = DashboardTab.CHAT,
    val showConnectionDialog: Boolean = false,
    val showQrScannerDialog: Boolean = false,
    val showVsCodeGuideDialog: Boolean = false,
    val targetIpInput: String = "192.168.1.10",
    val targetPortInput: String = "9876",
    val chatInput: String = "",
    val searchDiffQuery: String = "",
    val searchFileQuery: String = "",
    val selectedDiffPath: String? = null,
    val terminalInput: String = "",
    val logFilter: LogFilter = LogFilter.ALL,
    val autoScrollTerminal: Boolean = true,
    val selectedFilePath: String = "src/App.tsx",
    val isEditingCurrentFile: Boolean = false,
    val currentFileDraftContent: String = "",
    val agentPromptInput: String = "",
    val pairingToastMessage: String? = null,
    val connectionHistory: List<String> = listOf(
        "192.168.1.10:9876 (VS Code Antigravity)",
        "192.168.1.55:9876 (MacBook Pro M3)",
        "10.0.0.15:9876 (Dev Workstation)"
    )
)
