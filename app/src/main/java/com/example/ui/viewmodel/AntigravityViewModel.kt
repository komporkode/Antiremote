package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.data.model.*
import com.example.data.storage.SavedConnection
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

    // Human-in-the-Loop, Diff Review, & Multi-Session flows
    val pendingApproval: StateFlow<AgentApprovalRequest?> = repository.pendingApproval
    val workspaceDiffs: StateFlow<List<FileDiff>> = repository.workspaceDiffs
    val conversationSessions: StateFlow<List<ConversationSession>> = repository.conversationSessions
    val conversationId: StateFlow<String?> = repository.conversationId

    // Active Dev Servers (In-App Preview) & Workspace Quick Scripts
    val activeDevServers: StateFlow<List<ActiveDevServer>> = repository.activeDevServers
    val workspaceScripts: StateFlow<List<WorkspaceScript>> = repository.workspaceScripts
    val isChatFocusMode: StateFlow<Boolean> = repository.isChatFocusMode

    fun selectTab(tab: DashboardTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == DashboardTab.PREVIEW) {
            requestDevServers()
        }
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

    fun initStorage(context: android.content.Context) {
        repository.initStorage(context)
        val last = repository.getLastSavedConnection()
        if (last != null && _uiState.value.targetIpInput.isEmpty()) {
            _uiState.update {
                it.copy(
                    targetIpInput = last.host,
                    targetPortInput = last.port.toString()
                )
            }
        }
    }

    fun getLastSavedConnection(): SavedConnection? = repository.getLastSavedConnection()

    fun reconnectLastKnown() {
        val last = repository.getLastSavedConnection()
        if (last != null) {
            _uiState.update {
                it.copy(
                    targetIpInput = last.host,
                    targetPortInput = last.port.toString(),
                    showConnectionDialog = false
                )
            }
            repository.connect(last.host, last.port, last.pin)
        }
    }

    fun connectToBridge(pin: String? = null) {
        val ip = _uiState.value.targetIpInput.trim()
        val port = _uiState.value.targetPortInput.toIntOrNull() ?: 9876
        repository.connect(ip, port, pin)
        _uiState.update { it.copy(showConnectionDialog = false) }
    }

    /**
     * Auto-Connect from QR Code Handshake
     * Parses JSON { "ip": "...", "port": 9876 } or Cloud { "url": "wss://...", "pin": "..." }
     */
    fun pairWithQrCode(qrPayload: String) {
        val parsed = QrHandshakeData.parse(qrPayload)
        val target = parsed?.wsUrl ?: (parsed?.ip ?: "192.168.1.10")
        val port = parsed?.port ?: 9876
        val pin = parsed?.pin ?: parsed?.authToken

        _uiState.update {
            it.copy(
                targetIpInput = target,
                targetPortInput = port.toString(),
                showQrScannerDialog = false,
                showConnectionDialog = false,
                pairingToastMessage = if (parsed?.pin != null) "Terhubung via Cloudflare (PIN: ${parsed.pin})" else "Terhubung ke Antigravity IDE di $target"
            )
        }

        repository.connect(target, port, pin)
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

        fun sendChatImage(context: android.content.Context, uri: android.net.Uri, prompt: String? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val res = uriToBase64(context, uri) ?: return@launch
            val base64 = res.first
            val fileName = res.second
            val caption = (prompt ?: _uiState.value.chatInput).trim()
            repository.sendChatImage(base64, fileName, caption)
            _uiState.update { it.copy(chatInput = "") }
        }
    }

    private fun uriToBase64(context: android.content.Context, uri: android.net.Uri): Pair<String, String>? {
        return try {
            val resolver = context.contentResolver
            var fileName = "image_${System.currentTimeMillis()}.png"
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }
            val inputStream = resolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()

            val finalBase64 = if (bytes.size > 800 * 1024) {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val maxDim = 1400
                    val ratio = (maxDim.toFloat() / Math.max(bitmap.width, bitmap.height)).coerceAtMost(1.0f)
                    val newW = (bitmap.width * ratio).toInt()
                    val newH = (bitmap.height * ratio).toInt()
                    val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, newW, newH, true)
                    val baos = java.io.ByteArrayOutputStream()
                    scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, baos)
                    android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
                } else {
                    android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                }
            } else {
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            }
            Pair(finalBase64, fileName)
        } catch (_: Exception) {
            null
        }
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

    fun requestConversationSessions() {
        repository.requestConversationSessions()
    }

    fun requestChatHistory(convId: String? = null) {
        repository.requestChatHistory(convId)
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

    // Approvals, Diffs & Sessions
    fun sendApprovalResponse(id: String, approved: Boolean, choice: String) {
        repository.sendApprovalResponse(id, approved, choice)
    }

    fun requestWorkspaceDiffs() {
        repository.requestWorkspaceDiffs()
    }

    fun updateSearchDiffQuery(query: String) {
        _uiState.update { it.copy(searchDiffQuery = query) }
    }

    fun selectDiffPath(path: String?) {
        _uiState.update { it.copy(selectedDiffPath = path) }
    }

    fun startNewConversation() {
        repository.startNewConversation()
    }

    fun switchConversation(sessionId: String) {
        repository.switchConversation(sessionId)
    }

    // Live Interactive Terminal
    val liveTerminalOutput: StateFlow<String> = repository.liveTerminalOutput
    fun requestTerminalBuffer() = repository.requestTerminalBuffer()
    fun sendLiveTerminalInput(input: String) = repository.sendLiveTerminalInput(input)
    fun sendTerminalInterrupt() = repository.sendTerminalInterrupt()
    fun clearLiveTerminal() = repository.clearLiveTerminal()

    // ==========================================
    // TAB 5: ACTIVE DEV SERVERS & LIVE PREVIEW
    // ==========================================

    fun requestDevServers() = repository.requestDevServers()
    fun requestWorkspaceScripts() = repository.requestWorkspaceScripts()
    fun killPort(port: Int) = repository.killPort(port)
    fun runWorkspaceScript(command: String) = repository.runWorkspaceScript(command)

    fun selectPreviewUrl(url: String?) {
        _uiState.update { it.copy(selectedPreviewUrl = url) }
    }

    fun triggerHotReload() {
        repository.sendLiveTerminalInput("r")
    }

    fun toggleChatFocusMode() = repository.toggleChatFocusMode()
}
