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
    companion object {
        @Volatile
        private var instance: LanBridgeClient? = null

        fun getInstance(): LanBridgeClient {
            return instance ?: synchronized(this) {
                instance ?: LanBridgeClient().also { instance = it }
            }
        }
    }

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
    private var autoReconnectJob: Job? = null
    private var lastTarget: String? = null
    private var lastPort: Int = 9876
    private var lastPin: String? = null
    private var isExplicitDisconnect: Boolean = false
    val isExplicitDisconnectState: Boolean get() = isExplicitDisconnect
    private var reconnectAttempts: Int = 0
    private var connectionPreferences: com.example.data.storage.ConnectionPreferences? = null

    // Outbox queue untuk respon approval jika socket terputus/reconnecting
    private val pendingApprovalOutbox = java.util.Collections.synchronizedList(mutableListOf<JSONObject>())

    // PC Caffeine (Anti-Sleep) Workstation State
    private val _isPcCaffeineActive = MutableStateFlow(false)
    val isPcCaffeineActive: StateFlow<Boolean> = _isPcCaffeineActive.asStateFlow()
    private var isManualPcCaffeine = false

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
    private val _chatMessages = MutableStateFlow<List<AgentChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<AgentChatMessage>> = _chatMessages.asStateFlow()

    private val _isAgentStreaming = MutableStateFlow(false)
    val isAgentStreaming: StateFlow<Boolean> = _isAgentStreaming.asStateFlow()

    private val _agentToolBadge = MutableStateFlow<String?>(null)
    val agentToolBadge: StateFlow<String?> = _agentToolBadge.asStateFlow()

    private val _conversationId = MutableStateFlow<String?>(null)
    val conversationId: StateFlow<String?> = _conversationId.asStateFlow()

    private val _activeModelName = MutableStateFlow<String?>(null)
    val activeModelName: StateFlow<String?> = _activeModelName.asStateFlow()

    private val _workspaceInfo = MutableStateFlow(WorkspaceInfo(
        projectName = "Workspace",
        activeFileName = "Tidak ada berkas aktif",
        cursorLine = 1
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

    // 1. Human-in-the-Loop Approval Request State
    val resolvedApprovalIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val _pendingApproval = MutableStateFlow<AgentApprovalRequest?>(null)
    val pendingApproval: StateFlow<AgentApprovalRequest?> = _pendingApproval.asStateFlow()

    // 2. Code Review & Diffs State
    private val _workspaceDiffs = MutableStateFlow<List<FileDiff>>(createInitialDiffs())
    val workspaceDiffs: StateFlow<List<FileDiff>> = _workspaceDiffs.asStateFlow()

    // 3. Multi-Session & Conversation History State
    private val _conversationSessions = MutableStateFlow<List<ConversationSession>>(createInitialSessions())
    val conversationSessions: StateFlow<List<ConversationSession>> = _conversationSessions.asStateFlow()
    // 4. Interactive Live Terminal Output Stream
    private val _liveTerminalOutput = MutableStateFlow<String>("")
    val liveTerminalOutput: StateFlow<String> = _liveTerminalOutput.asStateFlow()

    // 5. Active Local Dev Servers & Workspace Scripts
    private val _activeDevServers = MutableStateFlow<List<ActiveDevServer>>(emptyList())
    val activeDevServers: StateFlow<List<ActiveDevServer>> = _activeDevServers.asStateFlow()

    private val _workspaceScripts = MutableStateFlow<List<WorkspaceScript>>(emptyList())
    val workspaceScripts: StateFlow<List<WorkspaceScript>> = _workspaceScripts.asStateFlow()

    // 6. IDE Layout Focus Mode State
    private val _isChatFocusMode = MutableStateFlow(false)
    val isChatFocusMode: StateFlow<Boolean> = _isChatFocusMode.asStateFlow()


    init {
        appendTerminalLog(LogLevel.INFO, "Antigravity Remote Client siap (Port Default: 9876). Scan QR atau hubungkan.")
        // Smart Auto-Caffeine: saat Agent streaming/berpikir, tahan PC agar tidak sleep
        scope.launch {
            _isAgentStreaming.collect { streaming ->
                if (streaming) {
                    if (!_isPcCaffeineActive.value) {
                        sendPcCaffeineState(true, reason = "agent_streaming")
                    }
                } else {
                    if (!isManualPcCaffeine && _isPcCaffeineActive.value) {
                        sendPcCaffeineState(false, reason = "agent_idle")
                    }
                }
            }
        }
    }

    /**
     * Connects to Workstation at ws://$ip:$port
     */
    fun initPreferences(context: android.content.Context) {
        if (connectionPreferences == null) {
            connectionPreferences = com.example.data.storage.ConnectionPreferences(context.applicationContext)
        }
    }

    fun getLastSavedConnection(): com.example.data.storage.SavedConnection? {
        return connectionPreferences?.getLastConnection()
    }

    private fun scheduleAutoReconnect() {
        if (isExplicitDisconnect || lastTarget == null || connectionPreferences?.isAutoReconnectEnabled() == false) return
        autoReconnectJob?.cancel()
        autoReconnectJob = scope.launch {
            reconnectAttempts++
            val backoffMs = when (reconnectAttempts) {
                1 -> 1500L
                2 -> 3000L
                else -> 5000L
            }
            _connectionConfig.update {
                it.copy(
                    status = ConnectionStatus.CONNECTING,
                    errorMessage = "Koneksi terputus. Menghubungkan kembali (${reconnectAttempts})..."
                )
            }
            appendTerminalLog(LogLevel.WARN, "Koneksi terputus. Mencoba reconnect otomatis dalam ${backoffMs / 1000} detik...")
            delay(backoffMs)
            val target = lastTarget ?: return@launch
            connect(target, lastPort, lastPin, isAutoReconnect = true)
        }
    }

    fun connect(target: String, port: Int = 9876, pin: String? = null, isAutoReconnect: Boolean = false) {
        simulationJob?.cancel()
        if (!isAutoReconnect) {
            isExplicitDisconnect = false
            reconnectAttempts = 0
            autoReconnectJob?.cancel()
            lastTarget = target
            lastPort = port
            lastPin = pin
            _chatMessages.value = emptyList()
            _conversationSessions.value = emptyList()
            _conversationId.value = ""
            disconnectInternal()
        } else {
            webSocket?.close(1000, null)
            webSocket = null
        }

        val trimmed = target.trim()
        val isCloud = trimmed.startsWith("wss://") || 
                      trimmed.startsWith("https://") || 
                      trimmed.contains("trycloudflare.com") ||
                      (!trimmed.contains(":") && trimmed.contains("."))

        val wsUrl = when {
            trimmed.startsWith("wss://") || trimmed.startsWith("ws://") -> trimmed
            trimmed.startsWith("https://") -> "wss://" + trimmed.removePrefix("https://")
            trimmed.startsWith("http://") -> "ws://" + trimmed.removePrefix("http://")
            isCloud -> "wss://$trimmed"
            else -> {
                val clean = trimmed.removePrefix("ws://").removePrefix("http://").removeSuffix("/")
                if (clean.contains(":")) "ws://$clean" else "ws://$clean:$port"
            }
        }

        val displayHost = wsUrl.removePrefix("wss://").removePrefix("ws://").substringBefore("/")

        _connectionConfig.update {
            it.copy(
                ipAddress = displayHost,
                port = if (isCloud) 443 else port,
                status = ConnectionStatus.CONNECTING,
                isDemoMode = false,
                errorMessage = null
            )
        }

        appendTerminalLog(LogLevel.INFO, "Menghubungkan ke ${if (isCloud) "Cloud Remote ($wsUrl)" else "Workstation ($wsUrl)"} ...")

        val request = Request.Builder().url(wsUrl).build()

        lastPingSendTime = System.currentTimeMillis()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                reconnectAttempts = 0
                autoReconnectJob?.cancel()
                val ping = (System.currentTimeMillis() - lastPingSendTime).coerceAtLeast(4L)
                _connectionConfig.update {
                    it.copy(
                        status = ConnectionStatus.CONNECTED,
                        pingMs = ping,
                        isDemoMode = false,
                        errorMessage = null
                    )
                }
                appendTerminalLog(LogLevel.SUCCESS, "Terhubung ke Antigravity Bridge [${ping}ms]")

                // Simpan profil koneksi terakhir ke SharedPreferences
                connectionPreferences?.saveConnection(target, port, pin)
                requestWorkspaceDiffs()
                requestDevServers()

                // Jika menyertakan PIN keamanan, kirim pesan otentikasi seketika
                if (!pin.isNullOrBlank()) {
                    val authPayload = JSONObject().apply {
                        put("type", "AUTH")
                        put("pin", pin)
                        put("token", pin)
                    }
                    ws.send(authPayload.toString())
                    appendTerminalLog(LogLevel.INFO, "Mengirim PIN otentikasi keamanan...")
                }

                // Kirim respon approval yang tersimpan di outbox (jika ada yang dijawab saat offline/reconnect)
                flushApprovalOutbox(ws)

                // Sinkronkan status Caffeine (Anti-Sleep) ke Workstation
                if (_isPcCaffeineActive.value || isManualPcCaffeine) {
                    sendPcCaffeineState(true, reason = "reconnected")
                } else {
                    requestPcCaffeineState()
                }

                // Auto fetch workspace info, files, chat history, scripts, and dev servers on handshake
                requestWorkspaceInfo()
                requestWorkspaceFiles()
                requestChatHistory()
                requestConversationSessions()
                requestWorkspaceScripts()
                requestDevServers()
                requestLayoutState()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingSocketMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                val errorMsg = t.localizedMessage ?: "Connection refused"
                _connectionConfig.update {
                    it.copy(
                        status = if (!isExplicitDisconnect && lastTarget != null) ConnectionStatus.CONNECTING else ConnectionStatus.ERROR,
                        isDemoMode = false,
                        errorMessage = "Gagal konek: $errorMsg"
                    )
                }
                appendTerminalLog(LogLevel.ERROR, "Gagal konek ke $displayHost: $errorMsg")
                if (!isExplicitDisconnect && lastTarget != null) {
                    scheduleAutoReconnect()
                }
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
                if (!isExplicitDisconnect && lastTarget != null) {
                    scheduleAutoReconnect()
                }
            }
        })
    }

    fun disconnect() {
        isExplicitDisconnect = true
        autoReconnectJob?.cancel()
        reconnectAttempts = 0
        disconnectInternal()
        appendTerminalLog(LogLevel.WARN, "Koneksi diputus oleh pengguna.")
    }

    private fun disconnectInternal() {
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        simulationJob?.cancel()
        agentStreamJob?.cancel()
        _isAgentStreaming.value = false
        _agentToolBadge.value = null
        _connectionConfig.update {
            it.copy(status = ConnectionStatus.DISCONNECTED, isDemoMode = false)
        }
    }


    /**
     * UPGRADED WebSocket Message Handler (onMessage)
     * Handles streaming AI Agent delta chunks, tool execution badges, workspace info, files, and telemetry
     */
    private fun handleIncomingSocketMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "AUTH_SUCCESS" -> {
                    appendTerminalLog(LogLevel.SUCCESS, "✓ Otentikasi PIN Berhasil! Akses Remote IDE Diizinkan.")
                }
                "AUTH_FAILED" -> {
                    val errMsg = json.optString("message", "PIN tidak valid atau ditolak")
                    appendTerminalLog(LogLevel.ERROR, "✕ Otentikasi Gagal: $errMsg")
                    _connectionConfig.update {
                        it.copy(status = ConnectionStatus.ERROR, errorMessage = errMsg)
                    }
                }
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
                        if (wsName.isNotEmpty() && wsName != "No Workspace") {
                            _workspaceInfo.update { current ->
                                current.copy(
                                    projectName = wsName,
                                    activeFileName = if (doc.isNotEmpty()) doc.substringAfterLast("/") else current.activeFileName
                                )
                            }
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
                    val model = json.optString("modelName", "").ifEmpty { null }
                    if (model != null) {
                        _activeModelName.value = model
                    }
                    finishChatStream(convId, model)
                }

                // 5. INFO WORKSPACE & FILE AKTIF (BARU!)
                "WORKSPACE_INFO" -> {
                    val data = json.optJSONObject("data")
                    val projectName = data?.optString("name") ?: json.optString("name", _workspaceInfo.value.projectName)
                    val activeFile = data?.optJSONObject("activeFile")
                    val fileName = activeFile?.optString("name") ?: json.optString("activeDocument", _workspaceInfo.value.activeFileName)
                    val line = activeFile?.optInt("cursorLine") ?: _workspaceInfo.value.cursorLine
                    updateWorkspaceHeader(projectName, fileName, line)

                    val scriptsArr = data?.optJSONArray("scripts")
                    if (scriptsArr != null) {
                        val sList = mutableListOf<WorkspaceScript>()
                        for (i in 0 until scriptsArr.length()) {
                            val sObj = scriptsArr.getJSONObject(i)
                            sList.add(
                                WorkspaceScript(
                                    name = sObj.optString("name", ""),
                                    command = sObj.optString("command", ""),
                                    source = sObj.optString("source", "package.json")
                                )
                            )
                        }
                        if (sList.isNotEmpty()) {
                            _workspaceScripts.value = sList
                        }
                    }
                }

                // 5b. ACTIVE DEV SERVERS (UNTUK LIVE PREVIEW DI HP)
                "DEV_SERVERS" -> {
                    val arr = json.optJSONArray("servers")
                    val sList = mutableListOf<ActiveDevServer>()
                    val currentHost = _connectionConfig.value.ipAddress
                    val commonDevPorts = setOf(3000, 3001, 3002, 3003, 4200, 5000, 5001, 5173, 5174, 5175, 8000, 8001, 8008, 8080, 8081, 8888, 9000)
                    val devKeywords = listOf("node", "vite", "next", "python", "flask", "django", "uvicorn", "go", "cargo", "deno", "bun", "php", "java")
                    val blacklist = setOf("esrv_svc", "esrv", "svchost", "system", "code", "msedge", "chrome", "brave", "antigravity", "searchhost")

                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val port = item.optInt("port", 0)
                            val proc = item.optString("processName", "").lowercase()

                            // Filter ketat: Buang service background Windows / hardware
                            if (blacklist.any { proc.contains(it) }) continue

                            // Hanya masukkan port dev umum ATAU proses dev dengan port < 10000
                            val isCommon = port in commonDevPorts
                            val isDevProc = devKeywords.any { proc.contains(it) } && port < 10000
                            if (!isCommon && !isDevProc) continue

                            val localUrl = item.optString("localUrl", "http://localhost:$port")
                            var lanUrl = item.optString("lanUrl", "")
                            val cloudUrl = item.optString("cloudUrl", "").takeIf { it.isNotBlank() }

                            // Utamakan IP Network Workstation jika lanUrl kosong atau masih localhost
                            if (lanUrl.isBlank() || lanUrl.contains("localhost") || lanUrl.contains("127.0.0.1")) {
                                val cleanHost = currentHost.removePrefix("http://")
                                    .removePrefix("https://")
                                    .removePrefix("ws://")
                                    .removePrefix("wss://")
                                    .substringBefore("/")
                                    .substringBefore(":")
                                    .trim()
                                if (cleanHost.isNotBlank() && cleanHost != "localhost" && cleanHost != "127.0.0.1" && !cleanHost.contains("trycloudflare.com")) {
                                    lanUrl = "http://$cleanHost:$port"
                                }
                            }

                            sList.add(
                                ActiveDevServer(
                                    port = port,
                                    pid = item.optInt("pid", 0),
                                    processName = item.optString("processName", ""),
                                    localUrl = localUrl,
                                    lanUrl = lanUrl,
                                    isCommonDevPort = true,
                                    cloudUrl = cloudUrl
                                )
                            )
                        }
                    }
                    _activeDevServers.value = sList
                }

                "DEV_TUNNEL_READY" -> {
                    val port = json.optInt("port", 0)
                    val url = json.optString("url", "")
                    if (port > 0 && url.isNotBlank()) {
                        _activeDevServers.update { list ->
                            list.map { if (it.port == port) it.copy(cloudUrl = url) else it }
                        }
                        appendTerminalLog(LogLevel.INFO, "[CLOUD TUNNEL] Port $port terbuka via $url")
                    }
                }

                "DEV_TUNNEL_STOPPED" -> {
                    val port = json.optInt("port", 0)
                    if (port > 0) {
                        _activeDevServers.update { list ->
                            list.map { if (it.port == port) it.copy(cloudUrl = null) else it }
                        }
                        appendTerminalLog(LogLevel.INFO, "[CLOUD TUNNEL] Tunnel port $port ditutup.")
                    }
                }

                // 5c. DAFTAR SCRIPTS PROYEK
                "WORKSPACE_SCRIPTS" -> {
                    val arr = json.optJSONArray("scripts")
                    val sList = mutableListOf<WorkspaceScript>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            sList.add(
                                WorkspaceScript(
                                    name = item.optString("name", ""),
                                    command = item.optString("command", ""),
                                    source = item.optString("source", "package.json")
                                )
                            )
                        }
                    }
                    _workspaceScripts.value = sList
                }

                // 5d. STATUS TATA LETAK FOKUS CHAT
                "CHAT_FOCUS_MODE_STATE" -> {
                    val isFocus = json.optBoolean("isFocusMode", false)
                    _isChatFocusMode.value = isFocus
                    appendTerminalLog(
                        LogLevel.INFO,
                        if (isFocus) "[IDE LAYOUT] Mode Fokus Chat (Pusat) Aktif di PC" else "[IDE LAYOUT] Mode Koding Normal Aktif di PC"
                    )
                }

                // 5e. STATUS CAFFEINE (ANTI-SLEEP) WORKSTATION
                "PC_CAFFEINE_STATUS", "CAFFEINE_STATUS" -> {
                    val active = json.optBoolean("enabled", json.optBoolean("active", false))
                    _isPcCaffeineActive.value = active
                    appendTerminalLog(
                        LogLevel.INFO,
                        if (active) "☕ [CAFFEINE] Workstation mengonfirmasi: Anti-Sleep Aktif."
                        else "💤 [CAFFEINE] Workstation mengonfirmasi: Anti-Sleep Nonaktif."
                    )
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
                    val convId = json.optString("conversationId", "")
                    if (convId.isNotBlank()) {
                        _conversationId.value = convId
                    }
                    val activeModel = json.optString("activeModel", "").ifEmpty { null }
                    if (activeModel != null) {
                        _activeModelName.value = activeModel
                    }
                    val messagesArray = json.optJSONArray("messages")
                    val historyList = mutableListOf<AgentChatMessage>()
                    if (messagesArray != null) {
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
                            val model = msgObj.optString("modelName", "").ifEmpty { null }

                            // Jangan masukkan tool execution background kosong ke riwayat chat
                            if (textContent.isBlank() && sender == ChatSender.AGENT && !msgObj.has("approvalRequest")) {
                                continue
                            }

                            var approvalReq: AgentApprovalRequest? = null
                            if (msgObj.has("approvalRequest")) {
                                val aObj = msgObj.optJSONObject("approvalRequest")
                                if (aObj != null) {
                                    approvalReq = parseApprovalRequest(aObj)
                                    if (resolvedApprovalIds.contains(approvalReq.id)) {
                                        approvalReq = approvalReq.copy(status = ApprovalStatus.APPROVED)
                                    } else if (approvalReq.status == ApprovalStatus.PENDING) {
                                        _pendingApproval.value = approvalReq
                                    }
                                }
                            }

                            historyList.add(
                                AgentChatMessage(
                                    sender = sender,
                                    text = textContent,
                                    isStreaming = false,
                                    toolBadge = badge,
                                    modelName = model,
                                    approvalRequest = approvalReq
                                )
                            )
                        }
                    }
                    _chatMessages.value = historyList
                }

                // 9. EVENT USER MENGETIK / MENGIRIM DARI IDE ANTIGRAVITY DI PC
                "USER_CHAT_MESSAGE", "USER_PROMPT" -> {
                    val prompt = json.optString("prompt", json.optString("text", ""))
                    if (prompt.isNotEmpty()) {
                        _chatMessages.update { list ->
                            // Cegah duplikasi jika bubble terakhir sudah persis pesan ini (dikirim dari HP ini)
                            if (list.isNotEmpty() && list.last().sender == ChatSender.USER && list.last().text == prompt) {
                                list
                            } else {
                                list + AgentChatMessage(
                                    sender = ChatSender.USER,
                                    text = prompt
                                )
                            }
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

                // 11. AGENT ERROR DARI WORKSTATION
                "AGENT_ERROR" -> {
                    val errorMsg = json.optString("message", "Terjadi kesalahan pada AI Agent")
                    _isAgentStreaming.value = false
                    _agentToolBadge.value = null
                    _chatMessages.update { list ->
                        if (list.isNotEmpty() && list.last().sender == ChatSender.AGENT && list.last().isStreaming) {
                            val last = list.last()
                            val prefix = if (last.text.isNotEmpty()) "\n\n" else ""
                            list.dropLast(1) + last.copy(
                                text = (last.text + prefix + "⚠️ [Agent Error]: " + errorMsg).trim(),
                                isStreaming = false,
                                toolBadge = null
                            )
                        } else {
                            list + AgentChatMessage(
                                sender = ChatSender.SYSTEM,
                                text = "⚠️ [Agent Error]: " + errorMsg
                            )
                        }
                    }
                    appendTerminalLog(LogLevel.ERROR, "[Agent Error]: " + errorMsg)
                }

                // 12. NOTIFIKASI BERKAS TERSIMPAN
                "FILE_SAVED" -> {
                    val path = json.optString("relativePath", "berkas")
                    appendTerminalLog(LogLevel.SUCCESS, "Berkas '" + path + "' berhasil disimpan di workstation.")
                }

                // 13. PERMINTAAN PERSETUJUAN DARI AGENT (HUMAN-IN-THE-LOOP)
                "AGENT_APPROVAL_REQUEST" -> {
                    val approvalObj = json.optJSONObject("approval") ?: json
                    val approval = parseApprovalRequest(approvalObj)
                    if (!resolvedApprovalIds.contains(approval.id)) {
                        _pendingApproval.value = approval
                    }
                    _chatMessages.update { list ->
                        list + AgentChatMessage(
                            sender = ChatSender.AGENT,
                            text = if (approval.actionType == "ASK_QUESTION") "❓ AI Agent mengajukan pertanyaan: ${approval.title}" else "AI Agent meminta persetujuan: ${approval.title}",
                            approvalRequest = approval
                        )
                    }
                    appendTerminalLog(LogLevel.WARN, "[APPROVAL REQUIRED] ${approval.title}: ${approval.detail}")
                }

                // 13b. APPROVAL / PERTANYAAN TERSELESAIKAN (RESOLVED)
                "AGENT_APPROVAL_RESOLVED" -> {
                    val resolvedId = json.optString("id", "")
                    val answer = json.optString("answer", "")
                    val statusStr = json.optString("status", "APPROVED")
                    val isApproved = statusStr != "REJECTED"

                    if (resolvedId.isNotBlank()) {
                        resolvedApprovalIds.add(resolvedId)
                    }
                    if (_pendingApproval.value?.id == resolvedId || resolvedId.isEmpty()) {
                        _pendingApproval.value = null
                    }

                    _chatMessages.update { list ->
                        list.map { msg ->
                            if (msg.approvalRequest?.id == resolvedId || (resolvedId.isEmpty() && msg.approvalRequest?.status == ApprovalStatus.PENDING)) {
                                val updatedReq = msg.approvalRequest.copy(
                                    status = if (isApproved) ApprovalStatus.APPROVED else ApprovalStatus.REJECTED,
                                    selectedOption = answer.ifEmpty { msg.approvalRequest.selectedOption }
                                )
                                msg.copy(approvalRequest = updatedReq)
                            } else msg
                        }
                    }
                    appendTerminalLog(LogLevel.INFO, "[APPROVAL RESOLVED] Keputusan: $answer")
                }

                // 14. DATA REVIEW & DIFFS PERUBAHAN BERKAS
                
                // Live Interactive Terminal
                "TERMINAL_OUTPUT" -> {
                    val data = json.optString("data", "")
                    if (data.isNotEmpty()) {
                        _liveTerminalOutput.update { (it + data).takeLast(100000) }
                        extractNetworkUrlFromTerminal(data)
                    }
                }

                "TERMINAL_CLEARED" -> {
                    _liveTerminalOutput.value = ""
                }

                "WORKSPACE_DIFFS", "GIT_DIFFS" -> {
                    val filesArr = json.optJSONArray("files")
                    if (filesArr != null) {
                        val diffList = mutableListOf<FileDiff>()
                        for (i in 0 until filesArr.length()) {
                            val fObj = filesArr.getJSONObject(i)
                            val fPath = fObj.optString("path", "")
                            val statusStr = fObj.optString("status", "MODIFIED").uppercase()
                            val status = when (statusStr) {
                                "ADDED" -> DiffFileStatus.ADDED
                                "DELETED" -> DiffFileStatus.DELETED
                                else -> DiffFileStatus.MODIFIED
                            }
                            val adds = fObj.optInt("additions", 0)
                            val dels = fObj.optInt("deletions", 0)
                            val raw = fObj.optString("diff", "")
                            diffList.add(
                                FileDiff(
                                    path = fPath,
                                    status = status,
                                    additions = adds,
                                    deletions = dels,
                                    hunks = parseDiffHunks(raw),
                                    rawDiff = raw
                                )
                            )
                        }
                        _workspaceDiffs.value = diffList
                    }
                }

                // 15. RIWAYAT SESI / MULTI-CONVERSATION
                "CONVERSATION_SESSIONS", "SESSIONS_LIST" -> {
                    val sessArr = json.optJSONArray("sessions")
                    val sessList = mutableListOf<ConversationSession>()
                    if (sessArr != null) {
                        for (i in 0 until sessArr.length()) {
                            val sObj = sessArr.getJSONObject(i)
                            sessList.add(
                                ConversationSession(
                                    id = sObj.optString("id", UUID.randomUUID().toString()),
                                    title = sObj.optString("title", "Percakapan"),
                                    lastMessage = sObj.optString("lastMessage", ""),
                                    updatedAt = sObj.optLong("updatedAt", System.currentTimeMillis()),
                                    isActive = sObj.optBoolean("isActive", false),
                                    messageCount = sObj.optInt("messageCount", 1)
                                )
                            )
                        }
                    }
                    _conversationSessions.value = sessList
                }

                // Fallbacks & Previous Commands
                "WELCOME" -> {
                    val server = json.optString("server", "Antigravity Bridge")
                    val version = json.optString("version", "1.0.0")
                    val wsName = json.optString("workspaceName", "")
                    val branch = json.optString("gitBranch", "main")
                    val port = if (json.has("devPort") && !json.isNull("devPort")) json.optInt("devPort") else 3000
                    _connectionConfig.update { it.copy(workspaceName = wsName.ifEmpty { "No Workspace" }) }
                    _workspaceInfo.update {
                        it.copy(
                            projectName = wsName.ifEmpty { "No Workspace" },
                            gitBranch = branch.ifEmpty { "main" },
                            devPort = port
                        )
                    }
                    appendTerminalLog(LogLevel.SUCCESS, "[$server v$version] Siap menerima perintah.")
                }

                "WORKSPACE_STATE" -> {
                    val projectName = json.optString("projectName", _workspaceInfo.value.projectName)
                    val gitBranch = json.optString("gitBranch", _workspaceInfo.value.gitBranch)
                    val devPort = if (json.has("devPort") && !json.isNull("devPort")) json.optInt("devPort") else _workspaceInfo.value.devPort
                    val isSync = json.optBoolean("isSync", true)
                    _connectionConfig.update { it.copy(workspaceName = projectName.ifEmpty { "No Workspace" }) }
                    _workspaceInfo.update {
                        it.copy(
                            projectName = projectName.ifEmpty { "No Workspace" },
                            gitBranch = gitBranch.ifEmpty { "main" },
                            devPort = devPort,
                            isSync = isSync
                        )
                    }
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
    }

    fun finishChatStream(conversationId: String?, modelName: String? = null) {
        _isAgentStreaming.value = false
        _agentToolBadge.value = null
        if (!conversationId.isNullOrEmpty()) {
            _conversationId.value = conversationId
        }
        _chatMessages.update { list ->
            if (list.isNotEmpty() && list.last().sender == ChatSender.AGENT) {
                val last = list.last()
                list.dropLast(1) + last.copy(isStreaming = false, modelName = modelName ?: last.modelName)
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
        /**
     * Kirim Gambar ke AI Agent Antigravity di Workstation
     */
    fun sendChatImage(base64Data: String, fileName: String, prompt: String, conversationId: String? = null) {
        val convId = conversationId ?: _conversationId.value
        val userCaption = if (prompt.isNotBlank()) prompt.trim() else "Mohon analisa gambar ini."

        _chatMessages.update {
            it + AgentChatMessage(
                sender = ChatSender.USER,
                text = "📷 [$fileName]\n$userCaption"
            )
        }

        if (webSocket != null && !_connectionConfig.value.isDemoMode) {
            val payload = org.json.JSONObject().apply {
                put("type", "USER_CHAT_IMAGE")
                put("image", base64Data)
                put("fileName", fileName)
                put("prompt", userCaption)
                if (convId != null) {
                    put("conversationId", convId)
                }
            }
            val sent = webSocket?.send(payload.toString()) ?: false
            if (sent) {
                _isAgentStreaming.value = true
                _agentToolBadge.value = "Mengunggah gambar ke PC..."
                appendTerminalLog(LogLevel.COMMAND, "[KIRIM GAMBAR] > $fileName ($userCaption)")
            }
        }
    }

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
            // Kirim paket AGENT_STREAM_PROMPT langsung ke bridge server
            val streamPayload = org.json.JSONObject().apply {
                put("type", "AGENT_STREAM_PROMPT")
                put("prompt", trimmed)
                if (convId != null) {
                    put("conversationId", convId)
                }
                put("effort", "high")
            }
            socketDelivered = webSocket?.send(streamPayload.toString()) ?: false
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
            val payload = org.json.JSONObject().apply {
                put("type", "AGENT_CANCEL")
            }
            webSocket?.send(payload.toString())
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
    fun requestChatHistory(convId: String? = null) {
        val payload = JSONObject().apply {
            put("type", "GET_CHAT_HISTORY")
            if (!convId.isNullOrBlank()) {
                put("conversationId", convId)
            }
        }
        webSocket?.send(payload.toString())
    }

    fun requestConversationSessions() {
        val payload = JSONObject().apply {
            put("type", "GET_CONVERSATION_SESSIONS")
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

    fun requestTerminalBuffer() {
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED) {
            val payload = JSONObject().apply {
                put("type", "GET_TERMINAL_BUFFER")
            }
            webSocket?.send(payload.toString())
        }
    }

    fun sendLiveTerminalInput(input: String) {
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED) {
            val payload = JSONObject().apply {
                put("type", "TERMINAL_INPUT")
                put("input", input)
            }
            webSocket?.send(payload.toString())
        } else if (_connectionConfig.value.isDemoMode) {
            _liveTerminalOutput.update { it + input }
            if (input.endsWith("\n") || input.endsWith("\r")) {
                val cmd = input.trim()
            _liveTerminalOutput.update { it + "simulated: " + cmd + "\n" }
            }
        }
    }

    fun sendTerminalInterrupt() {
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED) {
            val payload = JSONObject().apply {
                put("type", "TERMINAL_INTERRUPT")
            }
            webSocket?.send(payload.toString())
        }
        _liveTerminalOutput.update { it + "^C\n" }
    }

    fun clearLiveTerminal() {
        _liveTerminalOutput.value = ""
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED) {
            val payload = JSONObject().apply {
                put("type", "TERMINAL_CLEAR")
            }
            webSocket?.send(payload.toString())
        }
    }

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

    fun requestDevServers() {
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED) {
            val payload = JSONObject().apply {
                put("type", "GET_DEV_SERVERS")
            }
            webSocket?.send(payload.toString())
        }
    }

    fun requestWorkspaceScripts() {
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED) {
            val payload = JSONObject().apply {
                put("type", "GET_WORKSPACE_SCRIPTS")
            }
            webSocket?.send(payload.toString())
        }
    }

    fun killPort(port: Int) {
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED) {
            val payload = JSONObject().apply {
                put("type", "KILL_PORT")
                put("port", port)
            }
            webSocket?.send(payload.toString())
            appendTerminalLog(LogLevel.COMMAND, "Meminta penghentian proses port $port...")
        }
    }

    fun runWorkspaceScript(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return
        appendTerminalLog(LogLevel.COMMAND, "$ $trimmed")
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED) {
            val payload = JSONObject().apply {
                put("type", "RUN_WORKSPACE_SCRIPT")
                put("command", trimmed)
            }
            webSocket?.send(payload.toString())
        }
    }

    fun toggleChatFocusMode() {
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED) {
            val payload = JSONObject().apply {
                put("type", "TOGGLE_CHAT_FOCUS_MODE")
            }
            webSocket?.send(payload.toString())
            appendTerminalLog(LogLevel.COMMAND, "Mengubah layout IDE (Toggle Focus Chat)...")
        }
    }

    fun requestLayoutState() {
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED) {
            val payload = JSONObject().apply {
                put("type", "GET_LAYOUT_STATE")
            }
            webSocket?.send(payload.toString())
        }
    }

    fun requestDevTunnel(port: Int) {
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED) {
            val payload = JSONObject().apply {
                put("type", "START_DEV_TUNNEL")
                put("port", port)
            }
            webSocket?.send(payload.toString())
            appendTerminalLog(LogLevel.COMMAND, "Membuka Cloudflare Tunnel untuk port $port...")
        }
    }

    fun stopDevTunnel(port: Int) {
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED) {
            val payload = JSONObject().apply {
                put("type", "STOP_DEV_TUNNEL")
                put("port", port)
            }
            webSocket?.send(payload.toString())
            appendTerminalLog(LogLevel.COMMAND, "Menutup Cloudflare Tunnel untuk port $port...")
        }
    }

    private fun extractNetworkUrlFromTerminal(data: String) {
        val networkRegex = Regex("""(?:-\s+)?Network:\s*(https?://[a-zA-Z0-9.\-]+(?::\d+)?(?:/[^\s]*)?)""", RegexOption.IGNORE_CASE)
        val match = networkRegex.find(data) ?: return
        val rawUrl = match.groupValues[1].trim().removeSuffix("/")
        try {
            val uri = android.net.Uri.parse(rawUrl)
            val port = if (uri.port != -1) uri.port else if (rawUrl.startsWith("https")) 443 else 80
            _activeDevServers.update { current ->
                val existing = current.find { it.port == port }
                if (existing != null) {
                    current.map { if (it.port == port) it.copy(lanUrl = rawUrl) else it }
                } else {
                    current + ActiveDevServer(
                        port = port,
                        processName = "Dev ($port)",
                        localUrl = "http://localhost:$port",
                        lanUrl = rawUrl,
                        isCommonDevPort = true
                    )
                }
            }
        } catch (_: Exception) {}
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
                workspaceName = "Antiremote Demo",
                activeDocument = "MainActivity.kt",
                port = 9876
            )
        }

        updateWorkspaceHeader("Antiremote Demo", "MainActivity.kt", 45)
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
  "name": "antiremote",
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

    private fun parseApprovalRequest(jsonObj: JSONObject): AgentApprovalRequest {
        val reqId = jsonObj.optString("id", UUID.randomUUID().toString())
        val action = jsonObj.optString("actionType", jsonObj.optString("action", "TERMINAL_CMD"))
        val title = jsonObj.optString("title", "Konfirmasi Aksi")
        val detail = jsonObj.optString("detail", jsonObj.optString("question", ""))
        val statusStr = jsonObj.optString("status", "PENDING")
        val status = when (statusStr.uppercase()) {
            "APPROVED" -> ApprovalStatus.APPROVED
            "REJECTED" -> ApprovalStatus.REJECTED
            else -> ApprovalStatus.PENDING
        }
        val selectedOption = jsonObj.optString("selectedOption", "").ifEmpty { null }
        val optionsArr = jsonObj.optJSONArray("options")
        val optionsList = mutableListOf<String>()
        if (optionsArr != null) {
            for (i in 0 until optionsArr.length()) {
                optionsList.add(optionsArr.getString(i))
            }
        }
        return AgentApprovalRequest(
            id = reqId,
            actionType = action,
            title = title,
            detail = detail,
            options = if (optionsList.isNotEmpty()) optionsList else listOf("Izinkan", "Tolak"),
            status = status,
            selectedOption = selectedOption
        )
    }

    /**
     * Kirim Respons Persetujuan (Human-in-the-Loop) dengan Outbox Queue
     */
    fun sendApprovalResponse(id: String, approved: Boolean, choice: String) {
        if (id.isNotBlank()) {
            resolvedApprovalIds.add(id)
        }
        val currentReq = _pendingApproval.value
        val optionIdx = if (currentReq != null && currentReq.id == id) {
            val idx = currentReq.options.indexOfFirst { it.equals(choice, ignoreCase = true) }
            if (idx >= 0) idx + 1 else null
        } else null

        val payload = JSONObject().apply {
            put("type", "AGENT_APPROVAL_RESPONSE")
            put("id", id)
            put("approved", approved)
            put("choice", choice)
            if (optionIdx != null) {
                put("optionIndex", optionIdx)
            }
            put("timestamp", System.currentTimeMillis())
        }

        var sent = false
        val isSocketReady = webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED && !_connectionConfig.value.isDemoMode
        if (isSocketReady) {
            sent = webSocket?.send(payload.toString()) == true
        }

        if (!sent && !_connectionConfig.value.isDemoMode) {
            // Jika koneksi sedang terputus/reconnecting saat user menjawab, simpan ke antrian outbox
            pendingApprovalOutbox.add(payload)
            appendTerminalLog(LogLevel.WARN, "⚠ Jawaban untuk '$id' ($choice) disimpan ke antrian outbox (akan dikirim saat koneksi pulih).")
        } else if (sent) {
            appendTerminalLog(
                if (approved) LogLevel.SUCCESS else LogLevel.WARN,
                "[APPROVAL] Keputusan untuk '$id': $choice"
            )
        }

        // Update local state - langsung bersihkan pendingApproval
        _pendingApproval.update { current ->
            if (current == null || current.id == id || id.isBlank()) null else current
        }
        _chatMessages.update { list ->
            list.map { msg ->
                if (msg.approvalRequest?.id == id || (id.isBlank() && msg.approvalRequest?.status == ApprovalStatus.PENDING)) {
                    val updatedReq = msg.approvalRequest?.copy(
                        status = if (approved) ApprovalStatus.APPROVED else ApprovalStatus.REJECTED,
                        selectedOption = choice
                    )
                    msg.copy(approvalRequest = updatedReq)
                } else msg
            }
        }
    }

    /**
     * Mengosongkan & mengirim semua jawaban approval dari outbox saat koneksi tersambung kembali
     */
    private fun flushApprovalOutbox(ws: WebSocket) {
        synchronized(pendingApprovalOutbox) {
            if (pendingApprovalOutbox.isNotEmpty()) {
                val iterator = pendingApprovalOutbox.iterator()
                var flushedCount = 0
                while (iterator.hasNext()) {
                    val queued = iterator.next()
                    val ok = ws.send(queued.toString())
                    if (ok) {
                        flushedCount++
                        appendTerminalLog(LogLevel.SUCCESS, "✓ [OUTBOX] Berhasil mengirim jawaban tertunda (ID: ${queued.optString("id")})")
                        iterator.remove()
                    }
                }
                if (flushedCount > 0) {
                    appendTerminalLog(LogLevel.INFO, "Total $flushedCount jawaban approval tersinkronkan ke Workstation IDE.")
                }
            }
        }
    }

    /**
     * PC Caffeine (Anti-Sleep) Workstation
     * Mencegah PC tertidur atau layar mati saat proses AI Agent sedang berjalan
     */
    fun togglePcCaffeine(): Boolean {
        val newState = !_isPcCaffeineActive.value
        isManualPcCaffeine = newState
        sendPcCaffeineState(newState, reason = "manual_toggle")
        return newState
    }

    fun setPcCaffeine(enabled: Boolean, manual: Boolean = true) {
        if (manual) isManualPcCaffeine = enabled
        sendPcCaffeineState(enabled, reason = if (manual) "manual" else "auto_agent")
    }

    fun sendPcCaffeineState(enabled: Boolean, reason: String = "manual") {
        _isPcCaffeineActive.value = enabled
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED && !_connectionConfig.value.isDemoMode) {
            val payload = JSONObject().apply {
                put("type", "SET_PC_CAFFEINE")
                put("enabled", enabled)
                put("mode", "SYSTEM_AND_DISPLAY")
                put("reason", reason)
            }
            webSocket?.send(payload.toString())
        }
        appendTerminalLog(
            if (enabled) LogLevel.SUCCESS else LogLevel.INFO,
            if (enabled) "☕ [CAFFEINE] Mode PC Anti-Sleep Aktif (Layar & CPU Workstation dicegah sleep)"
            else "💤 [CAFFEINE] Mode PC Anti-Sleep Nonaktif (Kembali ke mode daya normal)"
        )
    }

    fun requestPcCaffeineState() {
        if (webSocket != null && _connectionConfig.value.status == ConnectionStatus.CONNECTED && !_connectionConfig.value.isDemoMode) {
            val payload = JSONObject().apply {
                put("type", "GET_PC_CAFFEINE")
            }
            webSocket?.send(payload.toString())
        }
    }

    /**
     * Minta Data Diffs Perubahan Berkas
     */
    fun requestWorkspaceDiffs() {
        if (webSocket != null && !_connectionConfig.value.isDemoMode) {
            val payload = JSONObject().apply {
                put("type", "GET_WORKSPACE_DIFFS")
            }
            webSocket?.send(payload.toString())
        }
    }

    /**
     * Mulai Percakapan Baru (+ Sesi Baru)
     */
    fun startNewConversation() {
        if (webSocket != null && !_connectionConfig.value.isDemoMode) {
            val payload = JSONObject().apply {
                put("type", "NEW_CONVERSATION")
            }
            webSocket?.send(payload.toString())
        }
        val newId = UUID.randomUUID().toString()
        _conversationId.value = newId
        _chatMessages.value = listOf(
            AgentChatMessage(
                sender = ChatSender.AGENT,
                text = "Percakapan baru dimulai. Apa yang ingin kita kerjakan hari ini?"
            )
        )
        _conversationSessions.update { list ->
            listOf(
                ConversationSession(
                    id = newId,
                    title = "Percakapan Baru",
                    lastMessage = "Apa yang ingin kita kerjakan hari ini?",
                    isActive = true
                )
            ) + list.map { it.copy(isActive = false) }
        }
        appendTerminalLog(LogLevel.INFO, "[SESSION] Memulai sesi baru ($newId)")
    }

    /**
     * Beralih ke Sesi Percakapan Lain
     */
    fun switchConversation(sessionId: String) {
        if (webSocket != null && !_connectionConfig.value.isDemoMode) {
            val payload = JSONObject().apply {
                put("type", "SWITCH_CONVERSATION")
                put("conversationId", sessionId)
            }
            webSocket?.send(payload.toString())
        }
        _conversationId.value = sessionId
        _conversationSessions.update { list ->
            list.map { it.copy(isActive = it.id == sessionId) }
        }
        requestChatHistory(sessionId)
    }

    private fun parseDiffHunks(rawDiff: String): List<DiffHunk> {
        if (rawDiff.isBlank()) return emptyList()
        val hunks = mutableListOf<DiffHunk>()
        val lines = rawDiff.lines()
        var currentHeader = "@@ Perubahan @@"
        val currentLines = mutableListOf<DiffLine>()

        for (line in lines) {
            when {
                line.startsWith("@@") -> {
                    if (currentLines.isNotEmpty()) {
                        hunks.add(DiffHunk(currentHeader, currentLines.toList()))
                        currentLines.clear()
                    }
                    currentHeader = line
                }
                line.startsWith("+") && !line.startsWith("+++") -> {
                    currentLines.add(DiffLine(DiffLineType.ADDED, line.removePrefix("+")))
                }
                line.startsWith("-") && !line.startsWith("---") -> {
                    currentLines.add(DiffLine(DiffLineType.DELETED, line.removePrefix("-")))
                }
                else -> {
                    currentLines.add(DiffLine(DiffLineType.UNCHANGED, line.removePrefix(" ")))
                }
            }
        }
        if (currentLines.isNotEmpty()) {
            hunks.add(DiffHunk(currentHeader, currentLines.toList()))
        }
        return hunks
    }

    private fun createInitialDiffs(): List<FileDiff> {
        return listOf(
            FileDiff(
                path = "src/components/Hero.tsx",
                status = DiffFileStatus.MODIFIED,
                additions = 14,
                deletions = 3,
                hunks = listOf(
                    DiffHunk(
                        header = "@@ -28,8 +28,19 @@",
                        lines = listOf(
                            DiffLine(DiffLineType.UNCHANGED, "export const Hero = () => {"),
                            DiffLine(DiffLineType.DELETED, "  return <h1 className=\"hero-old\">Old Title</h1>;"),
                            DiffLine(DiffLineType.ADDED, "  return ("),
                            DiffLine(DiffLineType.ADDED, "    <div className=\"hero-container-v2\">"),
                            DiffLine(DiffLineType.ADDED, "      <h1 className=\"text-4xl font-bold\">Modern AI Platform</h1>"),
                            DiffLine(DiffLineType.ADDED, "      <p className=\"text-muted\">Built with Antigravity IDE</p>"),
                            DiffLine(DiffLineType.ADDED, "    </div>"),
                            DiffLine(DiffLineType.ADDED, "  );"),
                            DiffLine(DiffLineType.UNCHANGED, "};")
                        )
                    )
                ),
                rawDiff = "@@ -28,8 +28,19 @@\nexport const Hero = () => {\n-  return <h1 className=\"hero-old\">Old Title</h1>;\n+  return (\n+    <div className=\"hero-container-v2\">\n+      <h1 className=\"text-4xl font-bold\">Modern AI Platform</h1>\n+      <p className=\"text-muted\">Built with Antigravity IDE</p>\n+    </div>\n+  );\n};"
            ),
            FileDiff(
                path = "src/styles/globals.css",
                status = DiffFileStatus.MODIFIED,
                additions = 8,
                deletions = 0,
                hunks = listOf(
                    DiffHunk(
                        header = "@@ -110,4 +110,12 @@",
                        lines = listOf(
                            DiffLine(DiffLineType.UNCHANGED, ".btn {"),
                            DiffLine(DiffLineType.ADDED, "  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);"),
                            DiffLine(DiffLineType.ADDED, "  border-radius: 8px;"),
                            DiffLine(DiffLineType.ADDED, "  box-shadow: 0 1px 2px rgba(0,0,0,0.06);"),
                            DiffLine(DiffLineType.UNCHANGED, "}")
                        )
                    )
                ),
                rawDiff = "@@ -110,4 +110,12 @@\n.btn {\n+  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);\n+  border-radius: 8px;\n+  box-shadow: 0 1px 2px rgba(0,0,0,0.06);\n}"
            )
        )
    }

    private fun createInitialSessions(): List<ConversationSession> {
        return listOf(
            ConversationSession(
                id = "sess-1",
                title = "Refactor Hero & Style Minimalist",
                lastMessage = "Hero section diperbarui sesuai standar Linear",
                updatedAt = System.currentTimeMillis() - 1000 * 60 * 15,
                isActive = true,
                messageCount = 6
            ),
            ConversationSession(
                id = "sess-2",
                title = "Integrasi WebSocket Bridge",
                lastMessage = "Port 9876 handshake sukses",
                updatedAt = System.currentTimeMillis() - 1000 * 60 * 120,
                isActive = false,
                messageCount = 14
            ),
            ConversationSession(
                id = "sess-3",
                title = "Audit Performa & Build Vite",
                lastMessage = "Semua test lulus dengan coverage 92%",
                updatedAt = System.currentTimeMillis() - 1000 * 3600 * 24,
                isActive = false,
                messageCount = 9
            )
        )
    }
}

