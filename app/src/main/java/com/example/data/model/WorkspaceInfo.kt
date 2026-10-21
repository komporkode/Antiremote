package com.example.data.model

data class WorkspaceInfo(
    val projectName: String = "Workspace",
    val activeFileName: String = "Tidak ada berkas aktif",
    val cursorLine: Int = 1
)

data class FileContentData(
    val relativePath: String,
    val content: String
)
