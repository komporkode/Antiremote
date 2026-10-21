package com.example.data.model

import org.json.JSONObject
import java.net.URI

data class QrHandshakeData(
    val app: String = "antigravity-remote",
    val ip: String,
    val port: Int = 9876,
    val wsUrl: String = "ws://$ip:$port",
    val httpUrl: String = "http://$ip:$port",
    val authToken: String? = null,
    val pin: String? = null,
    val rawPayload: String = ""
) {
    companion object {
        const val DEFAULT_PORT = 9876

        /**
         * Parses raw QR code content according to Section 4.1 of TECHNICAL_SPEC_d402e019.md
         * and Upgrade Spec 1:
         * 1. JSON Payload: {"app":"antigravity-remote","ip":"192.168.1.10","port":9876,...}
         * 2. URI Format: ws://192.168.1.10:9876 or http://192.168.1.10:9876 or antigravity://192.168.1.10:9876
         * 3. Plain text format: 192.168.1.10:9876 or 192.168.1.10
         */
        fun parse(rawContent: String): QrHandshakeData? {
            val trimmed = rawContent.trim()
            if (trimmed.isEmpty()) return null

            var targetIp = ""
            var targetPort = DEFAULT_PORT
            var token: String? = null
            var appName = "antigravity-remote"

            try {
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    try {
                        val json = JSONObject(trimmed)
                        appName = json.optString("app", "antigravity-remote")
                        val cloudUrl = json.optString("url")
                        val cloudPin = json.optString("pin").ifEmpty { json.optString("token") }
                        if (cloudUrl.isNotEmpty()) {
                            val cleanHost = cloudUrl.removePrefix("wss://").removePrefix("ws://").removePrefix("https://").removePrefix("http://").substringBefore("/")
                            return QrHandshakeData(
                                app = appName,
                                ip = cleanHost,
                                port = 443,
                                wsUrl = if (cloudUrl.startsWith("ws")) cloudUrl else "wss://$cloudUrl",
                                httpUrl = "https://$cleanHost",
                                authToken = cloudPin.ifEmpty { null },
                                pin = cloudPin.ifEmpty { null },
                                rawPayload = trimmed
                            )
                        }

                        targetIp = json.optString("ip").ifEmpty {
                            json.optString("host")
                        }
                        targetPort = json.optInt("port", DEFAULT_PORT)
                        token = cloudPin.ifEmpty {
                            json.optString("token").ifEmpty {
                                json.optString("authToken").ifEmpty { null }
                            }
                        }
                    } catch (_: Throwable) {
                        // Fallback to pattern matching if JSONObject is unmocked in JVM unit tests
                    }

                    if (targetIp.isEmpty()) {
                        val ipMatch = Regex(""""(?:ip|host)"\s*:\s*"([^"]+)"""").find(trimmed)
                        val portMatch = Regex(""""port"\s*:\s*(\d+)""").find(trimmed)
                        val tokenMatch = Regex(""""(?:token|authToken)"\s*:\s*"([^"]+)"""").find(trimmed)
                        val appMatch = Regex(""""app"\s*:\s*"([^"]+)"""").find(trimmed)

                        if (ipMatch != null) targetIp = ipMatch.groupValues[1]
                        if (portMatch != null) targetPort = portMatch.groupValues[1].toIntOrNull() ?: DEFAULT_PORT
                        if (tokenMatch != null) token = tokenMatch.groupValues[1]
                        if (appMatch != null) appName = appMatch.groupValues[1]
                    }
                } else if (trimmed.startsWith("wss://") || trimmed.startsWith("ws://") || trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("antigravity://")) {
                    val uri = URI(trimmed)
                    targetIp = uri.host ?: ""
                    targetPort = if (uri.port != -1) uri.port else DEFAULT_PORT
                    val query = uri.query
                    if (!query.isNullOrEmpty()) {
                        for (param in query.split("&")) {
                            val kv = param.split("=")
                            if (kv.size == 2) {
                                when (kv[0].lowercase()) {
                                    "token", "authtoken" -> token = kv[1]
                                    "port" -> kv[1].toIntOrNull()?.let { targetPort = it }
                                    "ip", "host" -> targetIp = kv[1]
                                }
                            }
                        }
                    }
                } else if (trimmed.contains(":")) {
                    val parts = trimmed.split(":")
                    if (parts.size >= 2) {
                        targetIp = parts[0].trim()
                        targetPort = parts[1].trim().toIntOrNull() ?: DEFAULT_PORT
                    }
                } else if (trimmed.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$"""))) {
                    targetIp = trimmed
                    targetPort = DEFAULT_PORT
                }

                if (targetIp.isNotEmpty()) {
                    return QrHandshakeData(
                        app = appName,
                        ip = targetIp,
                        port = targetPort,
                        wsUrl = "ws://$targetIp:$targetPort",
                        httpUrl = "http://$targetIp:$targetPort",
                        authToken = token,
                        rawPayload = trimmed
                    )
                }
            } catch (_: Exception) {
                // Return null if parsing fails completely
            }

            return null
        }
    }
}
