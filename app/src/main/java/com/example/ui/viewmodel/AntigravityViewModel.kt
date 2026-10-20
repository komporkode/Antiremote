package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.model.*
import com.example.data.repository.AntigravityRepository
import kotlinx.coroutines.flow.*

class AntigravityViewModel(
    private val repository: AntigravityRepository = AntigravityRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AntigravityUiState())
    val uiState: StateFlow<AntigravityUiState> = _uiState.asStateFlow()

    val connectionConfig: StateFlow<ConnectionConfig> = repository.connectionConfig
    val telemetry: StateFlow<SystemTelemetry> = repository.telemetry
    val terminalLogs: StateFlow<List<TerminalLog>> = repository.terminalLogs
    val agentTask: StateFlow<AgentTask> = repository.agentTask
    val files: StateFlow<List<WorkspaceFile>> = repository.files

    // Upgraded flows for Remote Control AI Agent & Workspace
    val chatMessages: StateFlow<List<AgentChatMessage>> = repository.chatMessages
    val isAgentStreaming: StateFlow<Boolean> = repository.isAgentStreaming
    val agentToolBadge: StateFlow<String?> = repository.agentToolBadge
    val workspaceInfo: StateFlow<WorkspaceInfo> = repository.workspaceInfo
    val workspaceFileList: StateFlow<List<String>> = repository.workspaceFileList
    val activeFileContent: StateFlow<FileContentData?> = repository.activeFileContent

    fun selectTab(tab: DashboardTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun showConnectionDialog(show: Boolean) {
        _uiState.update { it.copy(showConnectionDialog = show) }
    }

    fun showQrScanner(show: Boolean) {
        _uiState.update { it.copy(showQrScannerDialog = show) }
    }

    fun showVsCodeGuide(show: Boolean) {
        _uiState.update { it.copy(showVsCodeGuideDialog = show) }
    }

    fun updateTargetIpInput(ip: String) {
        _uiState.update { it.copy(targetIpInput = ip) }
    }

    fun updateTargetPortInput(port: String) {
        _uiState.update { it.copy(targetPortInput = port) }
    }

    fun connectToBridge() {
        val ip = _uiState.value.targetIpInput.trim()
        val port = _uiState.value.targetPortInput.toIntOrNull() ?: 9876
        repository.connect(ip, port)
        _uiState.update { it.copy(showConnectionDialog = false) }
    }

    /**
     * Auto-Connect from QR Code Handshake
     * Parses JSON { "ip": "...", "port": 9876 } or URI, connects immediately without extra clicks
     */
    fun pairWithQrCode(qrPayload: String) {
        val parsed = QrHandshakeData.parse(qrPayload)
        val host = parsed?.ip ?: "192.168.1.10"
        val port = parsed?.port ?: 9876

        _uiState.update {
            it.copy(
                targetIpInput = host,
                targetPortInput = port.toString(),
                showQrScannerDialog = false,
                showConnectionDialog = false,
                pairingToastMessage = "Terhubung ke Antigravity IDE di $host:$port"
            )
        }

        repository.connect(host, port)
    }

    fun clearPairingToast() {
        _uiState.update { it.copy(pairingToastMessage = null) }
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun startDemoSimulation() {
        repository.startDemoSimulation()
        _uiState.update { it.copy(showConnectionDialog = false) }
    }

    fun setQuickIp(historyEntry: String) {
        val hostPort = historyEntry.substringBefore(" ").trim()
        val parts = hostPort.split(":")
        if (parts.size == 2) {
            _uiState.update {
                it.copy(
                    targetIpInput = parts[0],
                    targetPortInput = parts[1]
                )
            }
        }
    }

    // ==========================================
    // TAB 1: AI AGENT LIVE CHAT ACTIONS
    // ==========================================

    fun updateChatInput(text: String) {
        _uiState.update { it.copy(chatInput = text) }
    }

    fun sendChatPrompt(prompt: String? = null) {
        val textToSend = (prompt ?: _uiState.value.chatInput).trim()
        if (textToSend.isNotEmpty()) {
            repository.sendAgentStreamPrompt(textToSend)
            _uiState.update { it.copy(chatInput = "") }
        }
    }

    fun insertSlashCommand(command: String) {
        _uiState.update {
            val current = it.chatInput.trim()
            val newText = if (current.isEmpty()) "$command " else "$current $command "
            it.copy(chatInput = newText)
        }
    }

    fun cancelAgentTask() {
        repository.cancelAgentTask()
    }

    fun requestChatHistory() {
        repository.requestChatHistory()
    }

    // ==========================================
    // TAB 2: WORKSPACE & FILE EXPLORER ACTIONS
    // ==========================================

    fun updateSearchFileQuery(query: String) {
        _uiState.update { it.copy(searchFileQuery = query) }
    }

    fun requestWorkspaceFiles() {
        repository.requestWorkspaceFiles()
    }

    fun requestWorkspaceInfo() {
        repository.requestWorkspaceInfo()
    }

    fun openFile(relativePath: String) {
        _uiState.update { it.copy(selectedFilePath = relativePath) }
        repository.requestReadFile(relativePath)
    }

    fun closeCodeViewer() {
        repository.closeCodeViewer()
    }

    fun saveActiveFileEdits(content: String) {
        val path = activeFileContent.value?.relativePath ?: _uiState.value.selectedFilePath
        repository.saveFile(path, content)
    }

    // ==========================================
    // TAB 3: TELEMETRY & QUICK ACTIONS
    // ==========================================

    fun triggerSaveAll() {
        repository.triggerSaveAll()
    }

    fun triggerRunDev() {
        repository.sendCommand("npm run dev")
    }

    fun triggerRunBuild() {
        repository.sendCommand("npm run build")
    }

    fun triggerGitPull() {
        repository.sendCommand("git pull")
    }

    fun triggerGitPush() {
        repository.sendCommand("git push")
    }

    // Terminal command execution
    fun updateTerminalInput(text: String) {
        _uiState.update { it.copy(terminalInput = text) }
    }

    fun executeTerminalCommand(command: String? = null) {
        val cmd = command ?: _uiState.value.terminalInput
        if (cmd.isNotBlank()) {
            repository.sendCommand(cmd)
            _uiState.update { it.copy(terminalInput = "") }
        }
    }

    fun setLogFilter(filter: LogFilter) {
        _uiState.update { it.copy(logFilter = filter) }
    }

    fun toggleAutoScroll() {
        _uiState.update { it.copy(autoScrollTerminal = !it.autoScrollTerminal) }
    }

    fun clearTerminal() {
        repository.clearLogs()
    }
}
