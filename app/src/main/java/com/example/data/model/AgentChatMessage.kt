package com.example.data.model

import java.util.UUID

enum class ChatSender {
    USER,
    AGENT,
    SYSTEM
}

data class AgentChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: ChatSender,
    val text: String,
    val toolBadge: String? = null,
    val isStreaming: Boolean = false,
    val approvalRequest: AgentApprovalRequest? = null,
    val timestamp: Long = System.currentTimeMillis()
)
