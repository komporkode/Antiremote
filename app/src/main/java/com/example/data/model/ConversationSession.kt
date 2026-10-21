package com.example.data.model

data class ConversationSession(
    val id: String,
    val title: String,
    val lastMessage: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = false,
    val messageCount: Int = 0
)
