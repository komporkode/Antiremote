package com.example.data.model

enum class LogLevel {
    INFO,
    WARN,
    ERROR,
    COMMAND,
    SUCCESS
}

data class TerminalLog(
    val id: String,
    val timestamp: String,
    val level: LogLevel,
    val message: String
)
