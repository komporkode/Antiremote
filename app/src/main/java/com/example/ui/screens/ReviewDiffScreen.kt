package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun ReviewDiffScreen(
    diffs: List<FileDiff>,
    selectedDiffPath: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectDiff: (String?) -> Unit,
    onRefreshDiffs: () -> Unit,
    onRequestAiRevision: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAdditions = diffs.sumOf { it.additions }
    val totalDeletions = diffs.sumOf { it.deletions }

    val filteredDiffs = remember(diffs, searchQuery) {
        if (searchQuery.isBlank()) diffs
        else diffs.filter { it.path.contains(searchQuery, ignoreCase = true) }
    }

    val activeDiff = remember(diffs, selectedDiffPath) {
        diffs.find { it.path == selectedDiffPath }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
            .testTag("review_diff_screen")
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Review Perubahan Kode",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = ContentPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = "${diffs.size} berkas • +$totalAdditions / -$totalDeletions baris",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ContentSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                FilledTonalButton(
                    onClick = onRefreshDiffs,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CardSurfaceVariant,
                        contentColor = ContentPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sinkronkan", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (activeDiff != null) {
            // Detail Unified Diff Viewer
            UnifiedDiffDetailViewer(
                diff = activeDiff,
                onBack = { onSelectDiff(null) },
                onRequestRevision = { onRequestAiRevision(activeDiff.path) }
            )
        } else {
            // List of Changed Files
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari berkas yang diubah...", color = ContentTertiary, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = ContentTertiary, modifier = Modifier.size(16.dp))
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = ContentPrimary,
                        unfocusedTextColor = ContentPrimary
                    ),
                    singleLine = true
                )

                if (filteredDiffs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = AccentSuccess,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Tidak ada perubahan berkas",
                                style = MaterialTheme.typography.titleSmall.copy(color = ContentPrimary, fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Semua kode bersih atau perubahan sudah di-commit.",
                                style = MaterialTheme.typography.bodySmall.copy(color = ContentTertiary, fontSize = 12.sp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredDiffs, key = { it.path }) { diff ->
                            FileDiffItemCard(
                                diff = diff,
                                onClick = { onSelectDiff(diff.path) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileDiffItemCard(
    diff: FileDiff,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag("diff_card_${diff.path}"),
        shape = RoundedCornerShape(10.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = when (diff.status) {
                        DiffFileStatus.ADDED -> Icons.Outlined.NoteAdd
                        DiffFileStatus.DELETED -> Icons.Outlined.Delete
                        DiffFileStatus.MODIFIED -> Icons.Outlined.EditNote
                    },
                    contentDescription = null,
                    tint = when (diff.status) {
                        DiffFileStatus.ADDED -> AccentSuccess
                        DiffFileStatus.DELETED -> AccentDanger
                        DiffFileStatus.MODIFIED -> AccentWarning
                    },
                    modifier = Modifier.size(20.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = diff.path.substringAfterLast("/"),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = ContentPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )
                    Text(
                        text = diff.path,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ContentTertiary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        maxLines = 1
                    )
                }
            }

            // Line changes count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (diff.additions > 0) {
                    Text(
                        text = "+${diff.additions}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DiffAddedText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                if (diff.deletions > 0) {
                    Text(
                        text = "-${diff.deletions}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DiffDeletedText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = ContentTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun UnifiedDiffDetailViewer(
    diff: FileDiff,
    onBack: () -> Unit,
    onRequestRevision: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
    ) {
        // Diff Navigation Sub-bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali", tint = ContentPrimary, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = diff.path,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ContentPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "+${diff.additions}",
                        color = DiffAddedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "-${diff.deletions}",
                        color = DiffDeletedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Diff Content Lines
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp)
        ) {
            if (diff.hunks.isEmpty()) {
                item {
                    Text(
                        text = diff.rawDiff.ifEmpty { "Tidak ada rincian baris diff." },
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ContentSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )
                }
            } else {
                diff.hunks.forEach { hunk ->
                    item {
                        // Hunk Header (@@ ... @@)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = DiffHeaderBg
                        ) {
                            Text(
                                text = hunk.header,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DiffHeaderText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }

                    items(hunk.lines) { line ->
                        val (bgColor, textColor, prefix) = when (line.type) {
                            DiffLineType.ADDED -> Triple(DiffAddedBg, DiffAddedText, "+ ")
                            DiffLineType.DELETED -> Triple(DiffDeletedBg, DiffDeletedText, "- ")
                            DiffLineType.UNCHANGED -> Triple(Color.Transparent, ContentSecondary, "  ")
                            DiffLineType.HEADER -> Triple(DiffHeaderBg, DiffHeaderText, "  ")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor)
                                .padding(horizontal = 8.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = prefix + line.content,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = textColor,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Bar: Request Revision
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Perlu perbaikan pada berkas ini?",
                    style = MaterialTheme.typography.bodySmall.copy(color = ContentSecondary, fontSize = 12.sp)
                )

                Button(
                    onClick = onRequestRevision,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Minta AI Revisi", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
