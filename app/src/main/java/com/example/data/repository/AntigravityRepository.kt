package com.example.data.repository

import com.example.data.model.*
import com.example.data.network.LanBridgeClient
import kotlinx.coroutines.flow.StateFlow

class AntigravityRepository(
    private val client: LanBridgeClient = LanBridgeClient()
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

    fun connect(ip: String, port: Int = 9876) = client.connect(ip, port)
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
}
