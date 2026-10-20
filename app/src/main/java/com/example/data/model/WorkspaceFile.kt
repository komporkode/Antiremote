package com.example.data.model

data class WorkspaceFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean = false,
    val extension: String = "",
    val content: String = "",
    val language: String = "kotlin",
    val isModifiedLocally: Boolean = false,
    val lineCount: Int = 0
)
