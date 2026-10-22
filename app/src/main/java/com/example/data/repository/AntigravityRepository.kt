package com.example.data.repository

import com.example.data.model.*
import com.example.data.network.LanBridgeClient
import kotlinx.coroutines.flow.StateFlow

class AntigravityRepository(
    private val client: LanBridgeClient = LanBridgeClient.getInstance()
) {
    val connectionConfig: StateFlow<ConnectionConfig> = client.connectionConfig
    val telemetry: StateFlow<SystemTelemetry> = client.telemetry
    val terminalLogs: StateFlow<List<TerminalLog>> = client.terminalLogs
    val agentTask: StateFlow<AgentTask> = client.agentTask
    val files: StateFlow<List<WorkspaceFile>> = client.files

    // Upgraded AI Agent & Workspace flows
    val chatMessages: StateFlow<List<AgentChatMessage>> = client.chatMessages
    val isAgentStreaming: StateFlow<Boolean> = client.isAgentStreaming
    val agentToolBadge: StateFlow<String?> = client.agentToolBadge
    val conversationId: StateFlow<String?> = client.conversationId
    val workspaceInfo: StateFlow<WorkspaceInfo> = client.workspaceInfo
    val workspaceFileList: StateFlow<List<String>> = client.workspaceFileList
    val activeFileContent: StateFlow<FileContentData?> = client.activeFileContent

    // Human-in-the-Loop, Diff Review, & Multi-Session flows
    val pendingApproval: StateFlow<AgentApprovalRequest?> = client.pendingApproval
    val workspaceDiffs: StateFlow<List<FileDiff>> = client.workspaceDiffs
    val conversationSessions: StateFlow<List<ConversationSession>> = client.conversationSessions

    // Live Interactive Terminal
    val liveTerminalOutput: StateFlow<String> = client.liveTerminalOutput

    // Active Dev Servers & Workspace Scripts
    val activeDevServers: StateFlow<List<ActiveDevServer>> = client.activeDevServers
    val workspaceScripts: StateFlow<List<WorkspaceScript>> = client.workspaceScripts
    val isChatFocusMode: StateFlow<Boolean> = client.isChatFocusMode

    fun connect(ip: String, port: Int = 9876, pin: String? = null) = client.connect(ip, port, pin)
    fun initStorage(context: android.content.Context) = client.initPreferences(context)
    fun getLastSavedConnection() = client.getLastSavedConnection()
    fun disconnect() = client.disconnect()
    fun startDemoSimulation() = client.startDemoSimulation()
    fun sendCommand(command: String) = client.sendTerminalCommand(command)
    fun dispatchAgentPrompt(prompt: String) = client.dispatchAgentPrompt(prompt)
    fun toggleAgentPause() = client.toggleAgentPause()
    fun saveFile(path: String, content: String) = client.saveFileContent(path, content)
    fun triggerBuild() = client.sendTerminalCommand("npm run build")
    fun clearLogs() = client.clearLogs()

    // Upgraded commands for AI Agent & Workspace
    fun sendAgentStreamPrompt(prompt: String, convId: String? = null) = client.sendAgentStreamPrompt(prompt, convId)
    fun cancelAgentTask() = client.cancelAgentTask()
    fun requestWorkspaceInfo() = client.requestWorkspaceInfo()
    fun requestWorkspaceFiles() = client.requestWorkspaceFiles()
    fun requestChatHistory() = client.requestChatHistory()
    fun requestReadFile(relativePath: String) = client.requestReadFile(relativePath)
    fun triggerSaveAll() = client.triggerSaveAll()
    fun closeCodeViewer() = client.closeCodeViewer()

    // Approvals, Diffs & Sessions
    fun sendApprovalResponse(id: String, approved: Boolean, choice: String) = client.sendApprovalResponse(id, approved, choice)
    fun requestWorkspaceDiffs() = client.requestWorkspaceDiffs()
    fun startNewConversation() = client.startNewConversation()
    fun switchConversation(sessionId: String) = client.switchConversation(sessionId)

    // Interactive Terminal
    fun requestTerminalBuffer() = client.requestTerminalBuffer()
    fun sendLiveTerminalInput(input: String) = client.sendLiveTerminalInput(input)
    fun sendTerminalInterrupt() = client.sendTerminalInterrupt()
    fun clearLiveTerminal() = client.clearLiveTerminal()

    // Dev Servers & Scripts
    fun requestDevServers() = client.requestDevServers()
    fun requestWorkspaceScripts() = client.requestWorkspaceScripts()
    fun killPort(port: Int) = client.killPort(port)
    fun runWorkspaceScript(command: String) = client.runWorkspaceScript(command)
    fun toggleChatFocusMode() = client.toggleChatFocusMode()
    fun requestLayoutState() = client.requestLayoutState()
}
