package com.example.data.model

enum class DiffFileStatus {
    MODIFIED,
    ADDED,
    DELETED
}

enum class DiffLineType {
    ADDED,
    DELETED,
    UNCHANGED,
    HEADER
}

data class DiffLine(
    val type: DiffLineType,
    val content: String,
    val oldLineNumber: Int? = null,
    val newLineNumber: Int? = null
)

data class DiffHunk(
    val header: String,
    val lines: List<DiffLine>
)

data class FileDiff(
    val path: String,
    val status: DiffFileStatus = DiffFileStatus.MODIFIED,
    val additions: Int = 0,
    val deletions: Int = 0,
    val hunks: List<DiffHunk> = emptyList(),
    val rawDiff: String = ""
)
