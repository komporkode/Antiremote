package com.example.data.model

data class SystemTelemetry(
    val cpuPercent: Float = 0f,
    val memoryUsedMb: Int = 0,
    val memoryTotalMb: Int = 0,
    val freeMemMb: Int = 0,
    val hostname: String = "-",
    val platform: String = "-",
    val workspaceName: String = "-",
    val activeDocument: String = "-",
    val timestamp: Long = 0L,
    val fps: Float = 60.0f,
    val networkLatencyMs: Long = 0L,
    val threadCount: Int = 0,
    val activeGoroutinesOrWorkers: Int = 0,
    val buildState: BuildState = BuildState.IDLE,
    val uptimeSeconds: Long = 0L
)

enum class BuildState {
    IDLE,
    COMPILING,
    INDEXING,
    TESTING,
    DEPLOYED,
    FAILED
}
