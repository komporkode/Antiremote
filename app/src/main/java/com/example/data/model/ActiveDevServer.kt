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

        val cleanBridgeHost = sanitizeHost(bridgeHost)

        if (lanUrl.isNotBlank() && !lanUrl.contains("localhost") && !lanUrl.contains("127.0.0.1")) {
            return fixDoublePort(lanUrl, port)
        }
        if (cleanBridgeHost.isNotBlank() && cleanBridgeHost != "localhost" && cleanBridgeHost != "127.0.0.1") {
            return "http://$cleanBridgeHost:$port"
        }
        return localUrl.ifBlank { "http://127.0.0.1:$port" }
    }

    private fun sanitizeHost(raw: String): String {
        return raw.removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("ws://")
            .removePrefix("wss://")
            .substringBefore("/")
            .substringBefore(":")
            .trim()
    }

    private fun fixDoublePort(url: String, targetPort: Int): String {
        val scheme = if (url.startsWith("https://")) "https://" else "http://"
        val body = url.removePrefix("https://").removePrefix("http://")
        val hostPart = body.substringBefore("/")
        val pathPart = if (body.contains("/")) "/" + body.substringAfter("/") else ""
        val hostOnly = hostPart.substringBefore(":")
        return "$scheme$hostOnly:$targetPort$pathPart"
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
