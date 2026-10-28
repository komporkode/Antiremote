package com.example.data.model

data class WorkspaceInfo(
    val projectName: String = "Workspace",
    val activeFileName: String = "Tidak ada berkas aktif",
    val cursorLine: Int = 1,
    val gitBranch: String = "main",
    val devPort: Int? = 3000,
    val isSync: Boolean = true
)

data class FileContentData(
    val relativePath: String,
    val content: String
)

