package com.example.data.model

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class ConnectionConfig(
    val ipAddress: String = "192.168.1.10",
    val port: Int = 9876,
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val pingMs: Long = 0L,
    val isDemoMode: Boolean = false,
    val errorMessage: String? = null,
    val serverVersion: String = "1.0.0",
    val hostname: String = "Workstation",
    val platform: String = "Unknown",
    val workspaceName: String = "Workspace",
    val activeDocument: String = "",
    val authRequired: Boolean = false,
    val authToken: String = "",
    val activeProject: String = "Workspace"
)
