package com.example.data.network

import com.example.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class LanBridgeClient(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val okHttpClient = OkHttpClient.Builder()
        .pingInterval(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var simulationJob: Job? = null
    private var agentStreamJob: Job? = null
    private var lastPingSendTime: Long = 0L

    private val _connectionConfig = MutableStateFlow(ConnectionConfig())
    val connectionConfig: StateFlow<ConnectionConfig> = _connectionConfig.asStateFlow()

    private val _telemetry = MutableStateFlow(SystemTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<TerminalLog>>(emptyList())
    val terminalLogs: StateFlow<List<TerminalLog>> = _terminalLogs.asStateFlow()

    private val _agentTask = MutableStateFlow(createInitialAgentTask())
    val agentTask: StateFlow<AgentTask> = _agentTask.asStateFlow()

    private val _files = MutableStateFlow(createInitialFiles())
    val files: StateFlow<List<WorkspaceFile>> = _files.asStateFlow()

    // Upgraded Remote Control AI Agent & Workspace states
    private val _chatMessages = MutableStateFlow<List<AgentChatMessage>>(listOf(
        AgentChatMessage(
            sender = ChatSender.SYSTEM,
            text = "Tersambung ke Antigravity IDE (Port 9876). Chat dari PC & HP tersinkronisasi dua arah secara realtime."
        ),
        AgentChatMessage(
            sender = ChatSender.AGENT,
            text = "Halo! Saya AI Agent Antigravity. Chat ini terhubung langsung dengan IDE Antigravity di komputer Anda. Anda dapat mengetik instruksi dari sini atau langsung di PC!"
        )
    ))
    val chatMessages: StateFlow<List<AgentChatMessage>> = _chatMessages.asStateFlow()

    private val _isAgentStreaming = MutableStateFlow(false)
    val isAgentStreaming: StateFlow<Boolean> = _isAgentStreaming.asStateFlow()

    private val _agentToolBadge = MutableStateFlow<String?>(null)
    val agentToolBadge: StateFlow<String?> = _agentToolBadge.asStateFlow()

    private val _conversationId = MutableStateFlow<String?>(null)
    val conversationId: StateFlow<String?> = _conversationId.asStateFlow()

    private val _workspaceInfo = MutableStateFlow(WorkspaceInfo(
        projectName = "cms-landing-page",
        activeFileName = "App.tsx",
        cursorLine = 45
    ))
    val workspaceInfo: StateFlow<WorkspaceInfo> = _workspaceInfo.asStateFlow()

    private val _workspaceFileList = MutableStateFlow<List<String>>(listOf(
        "src/App.tsx",
        "src/components/Navbar.tsx",
        "src/components/Hero.tsx",
        "src/components/Features.tsx",
        "src/styles/globals.css",
        "package.json",
        "tsconfig.json",
        "vite.config.ts",
        "README.md"
    ))
    val workspaceFileList: StateFlow<List<String>> = _workspaceFileList.asStateFlow()

    private val _activeFileContent = MutableStateFlow<FileContentData?>(null)
    val activeFileContent: StateFlow<FileContentData?> = _activeFileContent.asStateFlow()

    init {
        appendTerminalLog(LogLevel.INFO, "Antigravity Remote Client siap (Port Default: 9876). Scan QR atau hubungkan.")
    }

    /**
     * Connects to Workstation at ws://$ip:$port
     */
    fun connect(ip: String, port: Int = 9876) {
        simulationJob?.cancel()
        disconnect()

        val cleanIp = ip.trim().removePrefix("http://").removePrefix("ws://").removeSuffix("/")
        _connectionConfig.update {
            it.copy(
                ipAddress = cleanIp,
                port = port,
                status = ConnectionStatus.CONNECTING,
                isDemoMode = false,
                errorMessage = null
            )
        }

        appendTerminalLog(LogLevel.INFO, "Menghubungkan ke Workstation ws://$cleanIp:$port ...")

        val wsUrl = "ws://$cleanIp:$port"
        val request = Request.Builder().url(wsUrl).build()

        lastPingSendTime = System.currentTimeMillis()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                val ping = (System.currentTimeMillis() - lastPingSendTime).coerceAtLeast(4L)
                _connectionConfig.update {
                    it.copy(
                        status = ConnectionStatus.CONNECTED,
                        pingMs = ping,
                        isDemoMode = false,
                        errorMessage = null
                    )
                }
                appendTerminalLog(LogLevel.SUCCESS, "Terhubung ke Antigravity Bridge ($cleanIp:$port) [${ping}ms]")

                // Auto fetch workspace info, files, and chat history on handshake
                requestWorkspaceInfo()
                requestWorkspaceFiles()
                requestChatHistory()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingSocketMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                val errorMsg = t.localizedMessage ?: "Connection refused"
                _connectionConfig.update {
                    it.copy(
                        status = ConnectionStatus.ERROR,
                        isDemoMode = false,
                        errorMessage = "Gagal konek: $errorMsg"
                    )
                }
                appendTerminalLog(LogLevel.ERROR, "Gagal konek ke $cleanIp:$port: $errorMsg")
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
                _connectionConfig.update {
                    it.copy(status = ConnectionStatus.DISCONNECTED)
                }
                appendTerminalLog(LogLevel.WARN, "Koneksi ditutup oleh server: $reason")
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _connectionConfig.update {
                    it.copy(status = ConnectionStatus.DISCONNECTED)
                }
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        simulationJob?.cancel()
        agentStreamJob?.cancel()
        _isAgentStreaming.value = false
        _agentToolBadge.value = null
        _connectionConfig.update {
            it.copy(status = ConnectionStatus.DISCONNECTED, isDemoMode = false)
        }
        appendTerminalLog(LogLevel.WARN, "Koneksi diputus oleh pengguna.")
    }

    /**
     * UPGRADED WebSocket Message Handler (onMessage)
     * Handles streaming AI Agent delta chunks, tool execution badges, workspace info, files, and telemetry
     */
    private fun handleIncomingSocketMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                // 1. Telemetri CPU & RAM
                "TELEMETRY" -> {
                    val cpu = if (json.has("cpuPercent")) json.getInt("cpuPercent") else (json.optDouble("cpuPercent", 0.0)).toInt()
                    val usedRam = json.optInt("memoryUsedMb", 0)
                    val totalRam = json.optInt("memoryTotalMb", 0)
                    updateTelemetryUi(cpu, usedRam, totalRam)

                    val host = json.optString("hostname", "")
                    val wsName = json.optString("workspaceName", "")
                    val doc = json.optString("activeDocument", "")
                    if (host.isNotEmpty() || wsName.isNotEmpty() || doc.isNotEmpty()) {
                        _connectionConfig.update {
                            it.copy(
                                hostname = host.ifEmpty { it.hostname },
                                workspaceName = wsName.ifEmpty { it.workspaceName },
                                activeDocument = doc.ifEmpty { it.activeDocument }
                            )
                        }
                    }
                }

                // 2. LIVE STREAM CHAT AGENT (BARU!)
                "AGENT_STREAM_CHUNK", "STREAM_CHUNK", "CHUNK", "DELTA", "AGENT_CHUNK" -> {
                    val deltaToken = json.optString("delta", json.optString("chunk", json.optString("token", json.optString("text", ""))))
                    if (deltaToken.isNotEmpty()) {
                        appendChatStream(deltaToken)
                    }
                }

                // 3. AGENT TOOL EXECUTION BADGE (BARU!)
                "AGENT_TOOL_CALL", "TOOL_CALL", "TOOL_START", "AGENT_TOOL" -> {
                    val toolName = json.optString("toolName", json.optString("tool", json.optString("name", "code_action")))
                    showAgentToolBadge("Sedang mengeksekusi: $toolName...")
                }

                // 4. AGENT SELESAI (BARU!)
                "AGENT_DONE", "AGENT_FINISHED", "STREAM_DONE", "DONE" -> {
                    val convId = json.optString("conversationId", json.optString("sessionId", "")).ifEmpty { null }
                    finishChatStream(convId)
                }

                // 5. INFO WORKSPACE & FILE AKTIF (BARU!)
                "WORKSPACE_INFO" -> {
                    val data = json.optJSONObject("data")
                    val projectName = data?.optString("name") ?: json.optString("name", "cms-landing-page")
                    val activeFile = data?.optJSONObject("activeFile")
                    val fileName = activeFile?.optString("name") ?: "Tidak ada file aktif"
                    val line = activeFile?.optInt("cursorLine") ?: 0
                    updateWorkspaceHeader(projectName, fileName, line)
                }

                // 6. DAFTAR FILE PROJECT (BARU!)
                "WORKSPACE_FILES" -> {
                    val filesArray = json.optJSONArray("files")
                    val fileList = mutableListOf<String>()
                    if (filesArray != null) {
                        for (i in 0 until filesArray.length()) {
                            fileList.add(filesArray.getString(i))
                        }
                    }
                    updateFileExplorerList(fileList)
                }

                // 7. ISI FILE KODE (BARU!)
                "FILE_CONTENT" -> {
                    val content = json.optString("content", "")
                    val path = json.optString("relativePath", "")
                    openCodeViewer(path, content)
                }

                // 8. RIWAYAT CHAT LENGKAP DARI ANTIGRAVITY IDE (BARU!)
                "CHAT_HISTORY", "CONVERSATION_HISTORY" -> {
                    val messagesArray = json.optJSONArray("messages")
                    if (messagesArray != null && messagesArray.length() > 0) {
                        val historyList = mutableListOf<AgentChatMessage>()
                        for (i in 0 until messagesArray.length()) {
                            val msgObj = messagesArray.getJSONObject(i)
                            val senderStr = msgObj.optString("sender", msgObj.optString("role", "agent")).lowercase()
                            val sender = when (senderStr) {
                                "user", "human" -> ChatSender.USER
                                "system" -> ChatSender.SYSTEM
                                else -> ChatSender.AGENT
                            }
                            val textContent = msgObj.optString("text", msgObj.optString("content", msgObj.optString("message", "")))
                            val badge = msgObj.optString("toolBadge", "").ifEmpty { null }
                            historyList.add(
                                AgentChatMessage(
                                    sender = sender,
                                    text = textContent,
                                    isStreaming = false,
                                    toolBadge = badge
                                )
                            )
                        }
                        if (historyList.isNotEmpty()) {
                            _chatMessages.value = historyList
                        }
                    }
                }

                // 9. EVENT USER MENGETIK / MENGIRIM DARI IDE ANTIGRAVITY DI PC
                "USER_CHAT_MESSAGE", "USER_PROMPT" -> {
                    val prompt = json.optString("prompt", json.optString("text", ""))
                    if (prompt.isNotEmpty()) {
                        _chatMessages.update {
                            it + AgentChatMessage(
                                sender = ChatSender.USER,
                                text = prompt
                            )
                        }
                    }
                }

                // 10. PESAN AGENT LENGKAP DARI ANTIGRAVITY IDE DI PC
                "AGENT_MESSAGE" -> {
                    val content = json.optString("content", json.optString("text", ""))
                    val badge = json.optString("toolBadge", "").ifEmpty { null }
                    if (content.isNotEmpty()) {
                        _chatMessages.update {
                            it + AgentChatMessage(
                                sender = ChatSender.AGENT,
                                text = content,
                                isStreaming = false,
                                toolBadge = badge
                            )
                        }
                    }
                }

                // Fallbacks & Previous Commands
                "WELCOME" -> {
                    val server = json.optString("server", "Antigravity Bridge")
                    val version = json.optString("version", "1.0.0")
                    appendTerminalLog(LogLevel.SUCCESS, "[$server v$version] Siap menerima perintah.")
                }

                "COMMAND_RESULT" -> {
                    val success = json.optBoolean("success", true)
                    val cmd = json.optString("command", "")
                    val msg = json.optString("message", "")
                    val level = if (success) LogLevel.SUCCESS else LogLevel.ERROR
                    appendTerminalLog(level, msg.ifEmpty { "Eksekusi '$cmd': ${if (success) "Sukses" else "Gagal"}" })
                }

                "PONG" -> {
                    val ping = (System.currentTimeMillis() - lastPingSendTime).coerceAtLeast(2L)
                    _connectionConfig.update { it.copy(pingMs = ping) }
                }

                "ERROR" -> {
                    val msg = json.optString("message", "Error from server")
                    appendTerminalLog(LogLevel.ERROR, "[SERVER ERROR] $msg")
                }

                else -> {
                    appendTerminalLog(LogLevel.INFO, text)
                }
            }
        } catch (_: Exception) {
            appendTerminalLog(LogLevel.INFO, text)
        }
    }

    // Helper functions for state mutations
    fun appendChatStream(deltaToken: String) {
        _isAgentStreaming.value = true
        _chatMessages.update { list ->
            if (list.isNotEmpty() && list.last().sender == ChatSender.AGENT && list.last().isStreaming) {
                val last = list.last()
                list.dropLast(1) + last.copy(text = last.text + deltaToken)
            } else {
                list + AgentChatMessage(
                    sender = ChatSender.AGENT,
                    text = deltaToken,
                    isStreaming = true,
                    toolBadge = _agentToolBadge.value
                )
            }
        }
    }

    fun showAgentToolBadge(badgeText: String) {
        _agentToolBadge.value = badgeText
        _chatMessages.update { list ->
            if (list.isNotEmpty() && list.last().sender == ChatSender.AGENT) {
                val last = list.last()
                list.dropLast(1) + last.copy(toolBadge = badgeText)
            } else {
                list
            }
        }
    }

    fun finishChatStream(conversationId: String?) {
        _isAgentStreaming.value = false
        _agentToolBadge.value = null
        if (!conversationId.isNullOrEmpty()) {
            _conversationId.value = conversationId
        }
        _chatMessages.update { list ->
            if (list.isNotEmpty() && list.last().sender == ChatSender.AGENT) {
                val last = list.last()
                list.dropLast(1) + last.copy(isStreaming = false)
            } else {
                list
            }
        }
    }

    fun updateWorkspaceHeader(projectName: String, fileName: String, line: Int) {
        _workspaceInfo.value = WorkspaceInfo(
            projectName = projectName,
            activeFileName = fileName,
            cursorLine = line
        )
        _connectionConfig.update {
            it.copy(
                workspaceName = projectName,
                activeProject = projectName,
                activeDocument = fileName
            )
        }
    }

    fun updateFileExplorerList(fileList: List<String>) {
        _workspaceFileList.value = fileList
        _files.update {
            fileList.map { path ->
                WorkspaceFile(
                    path = path,
                    name = path.substringAfterLast("/"),
                    language = when {
                        path.endsWith(".tsx") || path.endsWith(".ts") -> "typescript"
                        path.endsWith(".jsx") || path.endsWith(".js") -> "javascript"
                        path.endsWith(".json") -> "json"
                        path.endsWith(".css") -> "css"
                        path.endsWith(".kt") -> "kotlin"
                        else -> "text"
                    },
                    content = "// Konten berkas $path\n",
                    lineCount = 10
                )
            }
        }
    }

    fun openCodeViewer(path: String, content: String) {
        _activeFileContent.value = FileContentData(relativePath = path, content = content)
    }

    fun closeCodeViewer() {
        _activeFileContent.value = null
    }

    fun updateTelemetryUi(cpu: Int, usedRam: Int, totalRam: Int) {
        _telemetry.update { current ->
            current.copy(
                cpuPercent = cpu.toFloat(),
                memoryUsedMb = usedRam,
                memoryTotalMb = if (totalRam > 0) totalRam else 16384,
                freeMemMb = (if (totalRam > 0) totalRam else 16384) - usedRam
            )
        }
    }

    // ==========================================
    // BAGIAN 3: FUNGSI PENGIRIM PERINTAH WEBSOCKET
    // ==========================================

    /**
     * A. Kirim Prompt Chat ke Agent (dengan Streaming & Dukungan Kompatibilitas Lengkap)
     * Mengirimkan paket perintah ke workstation via WebSocket menggunakan format:
     * 1. AGENT_STREAM_PROMPT (Streaming token realtime)
     * 2. ANTIGRAVITY_COMMAND (Format resmi bridge workstation 'agy' dari spesifikasi teknis)
     * 3. USER_CHAT_MESSAGE (Sinkronisasi dua arah chat Antigravity IDE)
     */
    fun sendAgentStreamPrompt(prompt: String, conversationId: String? = null) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        // 1. Tampilkan chat bubble pengguna langsung di UI layar HP
        _chatMessages.update {
            it + AgentChatMessage(
                sender = ChatSender.USER,
                text = trimmed
            )
        }

        val convId = conversationId ?: _conversationId.value
        var socketDelivered = false

        if (webSocket != null && !_connectionConfig.value.isDemoMode) {
            // A. AGENT_STREAM_PROMPT: Format streaming modern (User Prompt #1)
            // Dilengkapi seluruh variasi field (prompt, command, message, text, content, data)
            val streamPayload = JSONObject().apply {
                put("type", "AGENT_STREAM_PROMPT")
                put("action", "AGENT_STREAM_PROMPT")
                put("prompt", trimmed)
                put("command", trimmed)
                put("message", trimmed)
                put("text", trimmed)
                put("content", trimmed)
                if (convId != null) {
                    put("conversationId", convId)
                    put("sessionId", convId)
                }
                put("effort", "high")
                put("data", JSONObject().apply {
                    put("prompt", trimmed)
                    put("command", trimmed)
                    put("message", trimmed)
                    put("text", trimmed)
                    if (convId != null) put("conversationId", convId)
                    put("effort", "high")
                })
            }
            val res1 = webSocket?.send(streamPayload.toString()) ?: false

            // B. ANTIGRAVITY_COMMAND: Format resmi spesifikasi Antigravity Bridge (TECHNICAL_SPEC_d402e019.md)
            // Backend Antigravity Bridge di PC mengeksekusi 'agy <command>' di terminal khusus
            val agyPayload = JSONObject().apply {
                put("type", "ANTIGRAVITY_COMMAND")
                put("command", trimmed)
                put("prompt", trimmed)
                put("message", trimmed)
                if (convId != null) put("conversationId", convId)
            }
            val res2 = webSocket?.send(agyPayload.toString()) ?: false

            // C. USER_CHAT_MESSAGE: Format sinkronisasi dua arah chat Antigravity IDE
            val userChatPayload = JSONObject().apply {
                put("type", "USER_CHAT_MESSAGE")
                put("prompt", trimmed)
                put("text", trimmed)
                put("message", trimmed)
                put("content", trimmed)
                if (convId != null) put("conversationId", convId)
            }
            val res3 = webSocket?.send(userChatPayload.toString()) ?: false

            // D. AGENT_PROMPT: Alternatif format perintah agent
            val agentPromptPayload = JSONObject().apply {
                put("type", "AGENT_PROMPT")
                put("prompt", trimmed)
                put("command", trimmed)
                put("text", trimmed)
                if (convId != null) put("conversationId", convId)
            }
            val res4 = webSocket?.send(agentPromptPayload.toString()) ?: false

            socketDelivered = res1 || res2 || res3 || res4
            if (socketDelivered) {
                _isAgentStreaming.value = true
                _agentToolBadge.value = "Menghubungkan ke Agent..."
                appendTerminalLog(LogLevel.COMMAND, "[CHAT AGENT] > $trimmed")
            }
        }

        if (!socketDelivered) {
            // Simulasi Offline Streaming untuk preview interaktif jika tidak terhubung ke socket
            simulateAgentStreamingResponse(trimmed)
        }
    }

    /**
     * B. Tombol Stop / Cancel Agent (Emergency Switch)
     */
    fun cancelAgentTask() {
        if (webSocket != null && !_connectionConfig.value.isDemoMode) {
            val payload = JSONObject().apply {
                put("type", "AGENT_CANCEL")
            }
            webSocket?.send(payload.toString())

            val agyCancel = JSONObject().apply {
                put("type", "ANTIGRAVITY_COMMAND")
                put("command", "cancel")
            }
            webSocket?.send(agyCancel.toString())

            val cancelPayload = JSONObject().apply {
                put("type", "CANCEL_AGENT")
            }
            webSocket?.send(cancelPayload.toString())
        }
        agentStreamJob?.cancel()
        _isAgentStreaming.value = false
        _agentToolBadge.value = null
        _chatMessages.update { list ->
            if (list.isNotEmpty() && list.last().sender == ChatSender.AGENT && list.last().isStreaming) {
                val last = list.last()
                list.dropLast(1) + last.copy(
                    text = last.text + "\n[Tugas dibatalkan oleh pengguna]",
                    isStreaming = false,
                    toolBadge = null
                )
            } else list
        }
        appendTerminalLog(LogLevel.WARN, "[AGENT CANCEL] Proses AI Agent dihentikan.")
    }

    /**
     * C. Minta Info Workspace & File yang sedang aktif dibuka
     */
    fun requestWorkspaceInfo() {
        val payload = JSONObject().apply {
            put("type", "GET_WORKSPACE_INFO")
        }
        webSocket?.send(payload.toString())
    }

    /**
     * D. Minta Daftar Seluruh File Project
     */
    fun requestWorkspaceFiles() {
        val payload = JSONObject().apply {
            put("type", "GET_WORKSPACE_FILES")
            put("maxResults", 300)
        }
        webSocket?.send(payload.toString())
    }

    /**
     * Minta Riwayat Chat Lengkap dari Antigravity IDE di PC
     */
    fun requestChatHistory() {
        val payload = JSONObject().apply {
            put("type", "GET_CHAT_HISTORY")
        }
        webSocket?.send(payload.toString())
    }

    /**
     * E. Minta Buka dan Baca File Kode tertentu
     */
    fun requestReadFile(relativePath: String) {
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED && !_connectionConfig.value.isDemoMode) {
            val payload = JSONObject().apply {
                put("type", "READ_FILE")
                put("relativePath", relativePath)
            }
            webSocket?.send(payload.toString())
        } else {
            // Fallback content in simulator
            val sampleContent = generateSampleCode(relativePath)
            openCodeViewer(relativePath, sampleContent)
        }
    }

    /**
     * F. Simpan Semua File di Editor PC (1-Click)
     */
    fun triggerSaveAll() {
        val payload = JSONObject().apply {
            put("type", "VSCODE_COMMAND")
            put("command", "workbench.action.files.saveAll")
        }
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED && !_connectionConfig.value.isDemoMode) {
            webSocket?.send(payload.toString())
        }
        appendTerminalLog(LogLevel.SUCCESS, "[VSCODE] workbench.action.files.saveAll terkirim.")
    }

    /**
     * Sends raw terminal shell command
     */
    fun sendTerminalCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        appendTerminalLog(LogLevel.COMMAND, "$ $trimmed")

        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED && !_connectionConfig.value.isDemoMode) {
            val payload = JSONObject().apply {
                put("type", "TERMINAL_COMMAND")
                put("command", trimmed)
            }
            webSocket?.send(payload.toString())
        } else if (_connectionConfig.value.isDemoMode) {
            processSimulatedCommand(trimmed)
        } else {
            appendTerminalLog(LogLevel.INFO, "Perintah '$trimmed' dicatat. Hubungkan ke PC untuk eksekusi langsung.")
        }
    }

    fun dispatchAgentPrompt(prompt: String) {
        sendAgentStreamPrompt(prompt)
    }

    fun toggleAgentPause() {
        cancelAgentTask()
    }

    fun clearLogs() {
        _terminalLogs.value = emptyList()
    }

    fun saveFileContent(path: String, newContent: String) {
        _activeFileContent.value = FileContentData(path, newContent)
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED && !_connectionConfig.value.isDemoMode) {
            val payload = JSONObject().apply {
                put("type", "WRITE_FILE")
                put("relativePath", path)
                put("content", newContent)
            }
            webSocket?.send(payload.toString())
        }
        triggerSaveAll()
    }

    /**
     * Interactive streaming simulation when offline or demoing
     */
    private fun simulateAgentStreamingResponse(userPrompt: String) {
        agentStreamJob?.cancel()
        agentStreamJob = scope.launch {
            _isAgentStreaming.value = true
            showAgentToolBadge("Menganalisis workspace...")
            delay(400)

            val isGoal = userPrompt.startsWith("/goal")
            val isPlan = userPrompt.startsWith("/plan")
            val isGrill = userPrompt.startsWith("/grill-me")
            val isLearn = userPrompt.startsWith("/learn")

            val chunks = when {
                isGoal -> listOf(
                    "Target proyek berhasil dipetakan: ",
                    "\n1. Membangun remote controller real-time untuk Google Antigravity.",
                    "\n2. Mengintegrasikan live streaming token chat dan status tools.",
                    "\n3. Menampilkan status file yang sedang diedit (App.tsx:45)."
                )
                isPlan -> listOf(
                    "Rencana Eksekusi Antigravity Agent:\n",
                    "- [✓] Validasi WebSocket bridge port 9876\n",
                    "- [✓] Subscribe event AGENT_STREAM_CHUNK & TELEMETRY\n",
                    "- [→] Eksekusi pemeriksaan unit test dan build artifact"
                )
                isGrill -> listOf(
                    "Evaluasi arsitektur Antigravity Remote:\n",
                    "• Latensi koneksi saat ini sangat responsif.\n",
                    "• Format payload WebSocket mengikuti protokol JSON dua arah.\n",
                    "• Pastikan port 9876 terbuka di firewall Windows/macOS/Linux Anda."
                )
                isLearn -> listOf(
                    "Penjelasan Komponen Antigravity:\n",
                    "Antigravity Remote menghubungkan perangkat seluler langsung ke IDE workstation melalui WebSocket port 9876. ",
                    "Semua event agent, file explorer, dan telemetri CPU/RAM dikirim secara real-time tanpa server perantara."
                )
                else -> listOf(
                    "Menerima instruksi: '$userPrompt'\n\n",
                    "Sedang membaca struktur direktori proyek... ",
                    "Memverifikasi perubahan pada ",
                    "App.tsx",
                    " dan menyiapkan instruksi build. ",
                    "Semua operasi berjalan dengan sukses!"
                )
            }

            // Tambahkan bubble awal
            _chatMessages.update {
                it + AgentChatMessage(
                    sender = ChatSender.AGENT,
                    text = "",
                    isStreaming = true,
                    toolBadge = "Membaca berkas..."
                )
            }

            for ((index, chunk) in chunks.withIndex()) {
                if (!isActive) break
                delay(120)
                if (index == 2) {
                    showAgentToolBadge("Sedang mengeksekusi: replace_file_content...")
                }
                appendChatStream(chunk)
            }

            delay(200)
            finishChatStream("conv-" + UUID.randomUUID().toString().take(8))
        }
    }

    /**
     * Offline Simulation Mode
     */
    fun startDemoSimulation() {
        simulationJob?.cancel()
        disconnect()

        _connectionConfig.update {
            it.copy(
                status = ConnectionStatus.CONNECTED,
                isDemoMode = true,
                pingMs = Random.nextLong(6, 12),
                errorMessage = null,
                serverVersion = "1.0.0 (Simulasi)",
                hostname = "ANTIGRAVITY-PC",
                workspaceName = "cms-landing-page",
                activeDocument = "App.tsx",
                port = 9876
            )
        }

        updateWorkspaceHeader("cms-landing-page", "App.tsx", 45)
        appendTerminalLog(LogLevel.INFO, "[DEMO] Mode Simulasi Aktif (Port: 9876).")

        simulationJob = scope.launch {
            while (isActive) {
                delay(1500)
                val cpu = Random.nextInt(12, 38)
                val mem = Random.nextInt(4200, 6800)
                updateTelemetryUi(cpu, mem, 16384)
            }
        }
    }

    private fun processSimulatedCommand(command: String) {
        scope.launch {
            delay(300)
            when {
                command.contains("build") -> {
                    appendTerminalLog(LogLevel.INFO, "> npm run build ...")
                    delay(1000)
                    appendTerminalLog(LogLevel.SUCCESS, "✓ built in 1.28s (dist/index.html 42.1 kB)")
                }
                command.contains("dev") -> {
                    appendTerminalLog(LogLevel.INFO, "> vite dev --port 5173")
                    appendTerminalLog(LogLevel.SUCCESS, "  ➜  Local:   http://localhost:5173/")
                }
                command.startsWith("git") -> {
                    appendTerminalLog(LogLevel.INFO, "On branch main. Everything up to date.")
                }
                else -> {
                    appendTerminalLog(LogLevel.SUCCESS, "Perintah berhasil dijalankan.")
                }
            }
        }
    }

    private fun appendTerminalLog(level: LogLevel, message: String) {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = formatter.format(Date())
        val newLog = TerminalLog(
            id = UUID.randomUUID().toString(),
            timestamp = time,
            level = level,
            message = message
        )
        _terminalLogs.update { (it + newLog).takeLast(250) }
    }

    private fun createInitialAgentTask(): AgentTask {
        return AgentTask(
            id = "task-agy-1",
            title = "Antigravity Autonomous Agent",
            goalPrompt = "Siap menerima instruksi dari Remote",
            state = AgentExecutionState.IDLE,
            progress = 0f,
            currentStepDescription = "Menunggu perintah / prompt dari ponsel...",
            tokenUsage = 0,
            startedAt = "-",
            aiModel = "Gemini 3.0",
            platformUrl = "antigravity.google",
            autonomyLevel = "Full Autonomous"
        )
    }

    private fun createInitialFiles(): List<WorkspaceFile> {
        return listOf(
            WorkspaceFile(
                path = "src/App.tsx",
                name = "App.tsx",
                language = "typescript",
                content = generateSampleCode("src/App.tsx"),
                lineCount = 45
            )
        )
    }

    private fun generateSampleCode(path: String): String {
        return when {
            path.endsWith("App.tsx") -> """
import React, { useState } from 'react';
import Navbar from './components/Navbar';
import Hero from './components/Hero';
import Features from './components/Features';

export default function App() {
  const [activeTab, setActiveTab] = useState('overview');

  return (
    <div className="min-h-screen bg-slate-950 text-white">
      <Navbar onTabChange={setActiveTab} />
      <main className="container mx-auto px-4 py-8">
        <Hero />
        <Features />
      </main>
    </div>
  );
}
""".trimIndent()
            path.endsWith("package.json") -> """
{
  "name": "cms-landing-page",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "lucide-react": "^0.383.0"
  }
}
""".trimIndent()
            else -> "// Berkas $path\n// Disinkronkan langsung dari Google Antigravity Workstation.\n"
        }
    }
}
