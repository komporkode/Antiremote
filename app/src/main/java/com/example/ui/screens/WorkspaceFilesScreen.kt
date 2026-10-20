package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FileContentData
import com.example.ui.theme.*

/**
 * Representasi item baris pada LazyColumn Pohon Berkas
 */
data class TreeItem(
    val fullPath: String,
    val name: String,
    val isFolder: Boolean,
    val depth: Int,
    val isExpanded: Boolean = true,
    val childCount: Int = 0,
    val extension: String = ""
)

@Composable
fun WorkspaceFilesScreen(
    files: List<String>,
    activeFileContent: FileContentData?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFileClick: (String) -> Unit,
    onCloseCodeViewer: () -> Unit,
    onSaveFileEdits: (String) -> Unit,
    onRefreshFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Mode tampilan: Tree View (Pohon) vs Flat List (Daftar Rata)
    var isTreeViewMode by remember { mutableStateOf(true) }

    // Folder yang sedang dibuka/expanded (default buka root & src)
    val expandedFolders = remember {
        mutableStateListOf<String>().apply {
            add("")
            add("src")
            add("src/components")
        }
    }

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
    }

    // Bangun daftar TreeItem untuk LazyColumn berdasarkan status expanded
    val treeItems = remember(filteredFiles, expandedFolders.toList(), isTreeViewMode) {
        if (!isTreeViewMode || searchQuery.isNotBlank()) {
            // Dalam mode flat atau saat mencari, tampilkan semua berkas secara langsung
            filteredFiles.map { path ->
                val name = path.substringAfterLast("/")
                val ext = if (name.contains(".")) name.substringAfterLast(".") else ""
                TreeItem(
                    fullPath = path,
                    name = name,
                    isFolder = false,
                    depth = 0,
                    extension = ext
                )
            }
        } else {
            buildHierarchicalTree(filteredFiles, expandedFolders.toSet())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
            .padding(14.dp)
            .testTag("workspace_files_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search & Refresh Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_files_input"),
                placeholder = {
                    Text("Cari berkas project...", color = ContentTertiary, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = ContentSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = ContentTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = ContentPrimary,
                    unfocusedTextColor = ContentPrimary
                )
            )

            // Toggle Tree / Flat View Mode
            IconButton(
                onClick = { isTreeViewMode = !isTreeViewMode },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isTreeViewMode) AccentPrimarySoft else CardSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .testTag("btn_toggle_tree_view")
            ) {
                Icon(
                    imageVector = if (isTreeViewMode) Icons.Default.AccountTree else Icons.Default.List,
                    contentDescription = "Toggle View",
                    tint = if (isTreeViewMode) AccentPrimary else ContentSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Refresh Files Button (Minta ulang daftar berkas lewat WebSocket)
            IconButton(
                onClick = onRefreshFiles,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .testTag("btn_refresh_files")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Segarkan Berkas",
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Header info bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isTreeViewMode) Icons.Default.FolderOpen else Icons.Default.Description,
                    contentDescription = null,
                    tint = AccentSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isTreeViewMode && searchQuery.isBlank()) {
                        "POHON BERKAS (${filteredFiles.size} BERKAS)"
                    } else {
                        "DAFTAR BERKAS (${filteredFiles.size} BERKAS)"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ContentSecondary,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
            Text(
                text = "Klik berkas untuk READ_FILE",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ContentTertiary,
                    fontSize = 11.sp
                )
            )
        }

        // Files Tree LazyColumn
        if (treeItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOff,
                        contentDescription = null,
                        tint = ContentTertiary,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "Tidak ada berkas yang ditemukan",
                        color = ContentSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Pastikan workstation IDE terhubung dan port 9876 aktif.",
                        color = ContentTertiary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("project_file_tree_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(treeItems, key = { it.fullPath + "_" + it.isFolder }) { item ->
                    if (item.isFolder) {
                        // Baris Folder (Dapat di-expand / collapse)
                        FolderTreeRow(
                            item = item,
                            onToggleExpand = {
                                if (expandedFolders.contains(item.fullPath)) {
                                    expandedFolders.remove(item.fullPath)
                                } else {
                                    expandedFolders.add(item.fullPath)
                                }
                            }
                        )
                    } else {
                        // Baris Berkas (Klik untuk memicu READ_FILE via WebSocket)
                        FileTreeRow(
                            item = item,
                            onFileClick = { onFileClick(item.fullPath) }
                        )
                    }
                }
            }
        }
    }

    // Code Viewer Modal Dialog ketika berkas dibuka
    if (activeFileContent != null) {
        CodeViewerDialog(
            fileContent = activeFileContent,
            onDismiss = onCloseCodeViewer,
            onSave = onSaveFileEdits
        )
    }
}

/**
 * Baris Folder dalam Pohon Berkas
 */
@Composable
private fun FolderTreeRow(
    item: TreeItem,
    onToggleExpand: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (item.isExpanded) 90f else 0f,
        label = "folder_arrow_rotate"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (item.depth * 14).dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggleExpand() }
            .testTag("folder_node_${item.name}"),
        shape = RoundedCornerShape(8.dp),
        color = CardSurfaceVariant.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = ContentSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotation)
                )

                Icon(
                    imageVector = if (item.isExpanded) Icons.Filled.FolderOpen else Icons.Filled.Folder,
                    contentDescription = null,
                    tint = AccentSecondary,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ContentPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            if (item.childCount > 0) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Text(
                        text = "${item.childCount}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ContentTertiary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Baris Berkas dalam Pohon Berkas (Klik untuk memicu READ_FILE via WebSocket)
 */
@Composable
private fun FileTreeRow(
    item: TreeItem,
    onFileClick: () -> Unit
) {
    val (icon, iconTint, badgeBg) = when (item.extension.lowercase()) {
        "tsx", "jsx" -> Triple(Icons.Default.Code, AccentPrimary, AccentPrimary.copy(alpha = 0.12f))
        "ts", "js" -> Triple(Icons.Default.Javascript, AccentSecondary, AccentSecondary.copy(alpha = 0.12f))
        "json" -> Triple(Icons.Default.DataObject, AccentWarning, AccentWarning.copy(alpha = 0.12f))
        "css", "scss" -> Triple(Icons.Default.Palette, Color(0xFF38BDF8), Color(0xFF38BDF8).copy(alpha = 0.12f))
        "html" -> Triple(Icons.Default.Language, Color(0xFFF97316), Color(0xFFF97316).copy(alpha = 0.12f))
        "md" -> Triple(Icons.Default.Description, Color(0xFFA855F7), Color(0xFFA855F7).copy(alpha = 0.12f))
        else -> Triple(Icons.Default.InsertDriveFile, ContentSecondary, CardSurfaceVariant)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (item.depth * 14).dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onFileClick() }
            .testTag("file_item_${item.name.replace(".", "_")}"),
        shape = RoundedCornerShape(10.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = ContentPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = item.fullPath,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ContentTertiary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        maxLines = 1
                    )
                }
            }

            // Extension badge & Arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.extension.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CardSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Text(
                            text = item.extension.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ContentSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Buka",
                    tint = ContentTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Membangun struktur hierarki pohon berkas dari daftar string path
 */
private fun buildHierarchicalTree(
    paths: List<String>,
    expandedFolders: Set<String>
): List<TreeItem> {
    // Kumpulkan semua direktori dan berkas
    val result = mutableListOf<TreeItem>()

    // Dapatkan struktur grup berdasarkan direktori
    val dirMap = mutableMapOf<String, MutableList<String>>() // dir -> direct files
    val subDirMap = mutableMapOf<String, MutableSet<String>>() // dir -> subdirs

    paths.forEach { path ->
        val parts = path.split("/")
        if (parts.size == 1) {
            dirMap.getOrPut("") { mutableListOf() }.add(path)
        } else {
            for (i in 0 until parts.size - 1) {
                val parent = if (i == 0) "" else parts.subList(0, i).joinToString("/")
                val current = parts.subList(0, i + 1).joinToString("/")
                subDirMap.getOrPut(parent) { mutableSetOf() }.add(current)
            }
            val parentDir = parts.dropLast(1).joinToString("/")
            dirMap.getOrPut(parentDir) { mutableListOf() }.add(path)
        }
    }

    fun traverse(dir: String, depth: Int) {
        // Tampilkan subdirektori terlebih dahulu
        val subDirs = subDirMap[dir]?.sorted() ?: emptyList()
        for (subDir in subDirs) {
            val dirName = subDir.substringAfterLast("/")
            val isExpanded = expandedFolders.contains(subDir)
            val childCount = (subDirMap[subDir]?.size ?: 0) + (dirMap[subDir]?.size ?: 0)

            result.add(
                TreeItem(
                    fullPath = subDir,
                    name = dirName,
                    isFolder = true,
                    depth = depth,
                    isExpanded = isExpanded,
                    childCount = childCount
                )
            )

            if (isExpanded) {
                traverse(subDir, depth + 1)
            }
        }

        // Kemudian tampilkan berkas di direktori ini
        val filesInDir = dirMap[dir]?.sorted() ?: emptyList()
        for (file in filesInDir) {
            val fileName = file.substringAfterLast("/")
            val ext = if (fileName.contains(".")) fileName.substringAfterLast(".") else ""
            result.add(
                TreeItem(
                    fullPath = file,
                    name = fileName,
                    isFolder = false,
                    depth = depth,
                    extension = ext
                )
            )
        }
    }

    traverse("", 0)
    return result
}

/**
 * Modal Dialog Pembaca & Editor Kode (Respons dari perintah READ_FILE WebSocket)
 */
@Composable
private fun CodeViewerDialog(
    fileContent: FileContentData,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var editableCode by remember(fileContent.relativePath) {
        mutableStateOf(fileContent.content)
    }
    var isEditingMode by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .testTag("code_viewer_dialog"),
            color = CardSurface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = fileContent.relativePath.substringAfterLast("/"),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ContentPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = fileContent.relativePath,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ContentTertiary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Salin Kode Button
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(editableCode))
                                Toast.makeText(context, "Kode disalin ke clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Salin Kode",
                                tint = ContentSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Toggle Edit / View
                        IconButton(
                            onClick = { isEditingMode = !isEditingMode },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isEditingMode) Icons.Default.Visibility else Icons.Default.Edit,
                                contentDescription = if (isEditingMode) "View" else "Edit",
                                tint = AccentPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Save Button
                        Button(
                            onClick = {
                                onSave(editableCode)
                                isEditingMode = false
                            },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_save_code_viewer"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentSuccess,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Simpan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = ContentSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = CardBorder)

                // Code Editor / Monospace Viewer Area
                val lines = remember(editableCode) { editableCode.lines() }

                if (isEditingMode) {
                    OutlinedTextField(
                        value = editableCode,
                        onValueChange = { editableCode = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .testTag("code_edit_text_field"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = ContentPrimary,
                            lineHeight = 18.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardSurfaceVariant,
                            unfocusedContainerColor = CardSurfaceVariant,
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = CardBorder
                        )
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CardSurfaceVariant)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        // Line numbers
                        Column(
                            modifier = Modifier.padding(end = 12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            lines.indices.forEach { index ->
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = ContentTertiary,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }

                        // Monospace Code Lines
                        Column {
                            lines.forEach { line ->
                                Text(
                                    text = if (line.isEmpty()) " " else line,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = ContentPrimary,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
