package com.example.data.model

enum class AgentExecutionState {
    IDLE,
    THINKING,
    WRITING_CODE,
    EXECUTING_COMMAND,
    BROWSER_TESTING,
    WAITING_APPROVAL,
    COMPLETED,
    PAUSED,
    ERROR
}

data class AgentStep(
    val stepNumber: Int,
    val toolName: String,
    val description: String,
    val status: StepStatus,
    val durationMs: Long = 0L,
    val targetInterface: String = "Editor" // Editor, Terminal, Browser, Reasoning
)

enum class StepStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILED
}

data class AgentTask(
    val id: String,
    val title: String,
    val goalPrompt: String,
    val state: AgentExecutionState,
    val progress: Float, // 0.0f - 1.0f
    val currentStepDescription: String,
    val tokenUsage: Int,
    val steps: List<AgentStep> = emptyList(),
    val startedAt: String,
    val aiModel: String = "Gemini 3.0",
    val platformUrl: String = "antigravity.google",
    val autonomyLevel: String = "Full Autonomous"
)
