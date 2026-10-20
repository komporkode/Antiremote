package com.example.data.model

data class WorkspaceInfo(
    val projectName: String = "cms-landing-page",
    val activeFileName: String = "App.tsx",
    val cursorLine: Int = 45
)

data class FileContentData(
    val relativePath: String,
    val content: String
)
