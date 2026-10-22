package com.example.data.model

data class ActiveDevServer(
    val port: Int,
    val pid: Int = 0,
    val processName: String = "",
    val localUrl: String = "",
    val lanUrl: String = "",
    val isCommonDevPort: Boolean = true,
    val cloudUrl: String? = null
) {
    val name: String
        get() = processName.ifEmpty { "Port $port" }

    fun getBestUrl(isCloud: Boolean, bridgeHost: String): String {
        if (isCloud && !cloudUrl.isNullOrBlank()) return cloudUrl
        if (lanUrl.isNotBlank() && !lanUrl.contains("localhost") && !lanUrl.contains("127.0.0.1")) {
            return lanUrl
        }
        if (bridgeHost.isNotBlank() && bridgeHost != "localhost" && bridgeHost != "127.0.0.1") {
            return "http://$bridgeHost:$port"
        }
        return localUrl.ifBlank { "http://127.0.0.1:$port" }
    }
}

data class WorkspaceScript(
    val name: String,
    val command: String,
    val source: String = "package.json"
)

enum class ConsoleLogLevel {
    DEBUG, LOG, INFO, WARN, ERROR
}

data class ConsoleLogItem(
    val level: ConsoleLogLevel,
    val message: String,
    val lineNumber: Int = 0,
    val sourceId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
