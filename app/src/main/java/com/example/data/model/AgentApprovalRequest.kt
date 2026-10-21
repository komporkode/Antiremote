package com.example.data.model

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class AgentApprovalRequest(
    val id: String,
    val actionType: String = "TERMINAL_CMD", // TERMINAL_CMD, FILE_WRITE, PLAN_CONFIRM, ASK_QUESTION
    val title: String,
    val detail: String,
    val options: List<String> = listOf("Izinkan", "Tolak"),
    val status: ApprovalStatus = ApprovalStatus.PENDING,
    val selectedOption: String? = null
)
