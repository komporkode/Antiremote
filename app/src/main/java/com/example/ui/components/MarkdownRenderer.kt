package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Parses inline markdown: **bold**, *italic*, `code`, and [text](link)
 */
fun parseInlineMarkdown(
    raw: String,
    normalColor: Color = ContentPrimary,
    accentColor: Color = AccentPrimary
): AnnotatedString {
    return buildAnnotatedString {
        // Regex pattern for bold (**text**), italic (*text*), inline code (`text`), link ([text](url))
        val pattern = Regex("""(\*\*([^*]+)\*\*)|(\*([^*]+)\*)|(`([^`]+)`)|(\[([^\]]+)\]\(([^)]+)\))""")
        var cursor = 0

        for (match in pattern.findAll(raw)) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > cursor) {
                append(raw.substring(cursor, start))
            }

            when {
                // Bold: **text**
                match.groups[2] != null -> {
                    val text = match.groups[2]!!.value
                    val s = length
                    append(text)
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), s, length)
                }
                // Italic: *text*
                match.groups[4] != null -> {
                    val text = match.groups[4]!!.value
                    val s = length
                    append(text)
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), s, length)
                }
                // Inline Code: `text`
                match.groups[6] != null -> {
                    val text = match.groups[6]!!.value
                    val s = length
                    append(text)
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = accentColor,
                            background = CardBorder.copy(alpha = 0.5f)
                        ),
                        s,
                        length
                    )
                }
                // Link: [text](url)
                match.groups[8] != null -> {
                    val text = match.groups[8]!!.value
                    val s = length
                    append(text)
                    addStyle(
                        SpanStyle(
                            color = accentColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        ),
                        s,
                        length
                    )
                }
            }
            cursor = end
        }

        if (cursor < raw.length) {
            append(raw.substring(cursor))
        }
    }
}

/**
 * Native Jetpack Compose Rich Markdown Renderer
 * Renders headings, lists, inline formatting, and code blocks cleanly
 */

fun isTableSeparator(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.contains("|") && trimmed.replace("|", "").replace("-", "").replace(":", "").replace(" ", "").isEmpty()
}

@Composable
fun RenderMarkdownTable(
    tableLines: List<String>,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    if (tableLines.size < 2) return

    val headers = tableLines[0].trim().removeSurrounding("|", "|").split("|").map { it.trim() }
    val dataRows = tableLines.drop(2).map { rowLine ->
        rowLine.trim().removeSurrounding("|", "|").split("|").map { it.trim() }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Column {
                // Table Header
                Row(
                    modifier = Modifier
                        .background(CardSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 7.dp)
                ) {
                    headers.forEach { header ->
                        Text(
                            text = parseInlineMarkdown(header, normalColor = ContentPrimary),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AccentPrimary,
                                fontSize = 11.5.sp
                            ),
                            modifier = Modifier
                                .widthIn(min = 90.dp, max = 240.dp)
                                .padding(horizontal = 6.dp)
                        )
                    }
                }
                HorizontalDivider(color = CardBorder, thickness = 1.dp)

                // Table Rows
                dataRows.forEachIndexed { rowIdx, rowCells ->
                    val bg = if (rowIdx % 2 == 1) CardSurfaceVariant.copy(alpha = 0.45f) else Color.Transparent
                    Row(
                        modifier = Modifier
                            .background(bg)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        headers.indices.forEach { colIdx ->
                            val cellText = rowCells.getOrNull(colIdx) ?: ""
                            Text(
                                text = parseInlineMarkdown(cellText, normalColor = textColor),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = textColor,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                ),
                                modifier = Modifier
                                    .widthIn(min = 90.dp, max = 240.dp)
                                    .padding(horizontal = 6.dp)
                            )
                        }
                    }
                    if (rowIdx < dataRows.size - 1) {
                        HorizontalDivider(color = CardBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderTextSegment(part: String, textColor: Color) {
    val lines = part.split("\n")
    var lineIdx = 0
    while (lineIdx < lines.size) {
        val rawLine = lines[lineIdx]
        val line = rawLine.trimEnd()

        if (line.trim().startsWith("|") && lineIdx + 1 < lines.size && isTableSeparator(lines[lineIdx + 1])) {
            val tableLines = mutableListOf<String>()
            while (lineIdx < lines.size && lines[lineIdx].trim().contains("|")) {
                tableLines.add(lines[lineIdx].trim())
                lineIdx++
            }
            RenderMarkdownTable(tableLines, textColor)
            continue
        }

        if (line.isBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
        } else if (line.startsWith("### ")) {
            Text(
                text = parseInlineMarkdown(line.removePrefix("### ").trim(), textColor),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 13.5.sp
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        } else if (line.startsWith("## ")) {
            Text(
                text = parseInlineMarkdown(line.removePrefix("## ").trim(), textColor),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 14.5.sp
                ),
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
            )
        } else if (line.startsWith("# ")) {
            Text(
                text = parseInlineMarkdown(line.removePrefix("# ").trim(), textColor),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 15.5.sp
                ),
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
        } else if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
            val trimmed = line.trimStart().removePrefix("- ").removePrefix("* ")
            val indent = (line.length - line.trimStart().length) / 2
            Row(
                modifier = Modifier.padding(start = (8 + indent * 10).dp, top = 1.dp, bottom = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("•", color = AccentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = parseInlineMarkdown(trimmed, textColor),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        } else if (Regex("""^\s*\d+\.\s""").containsMatchIn(line)) {
            val numMatch = Regex("""^\s*(\d+\.)\s*(.*)""").find(line)
            val numPrefix = numMatch?.groups?.get(1)?.value ?: "1."
            val rest = numMatch?.groups?.get(2)?.value ?: line
            Row(
                modifier = Modifier.padding(start = 6.dp, top = 1.dp, bottom = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(numPrefix, color = AccentPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = parseInlineMarkdown(rest, textColor),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        } else {
            Text(
                text = parseInlineMarkdown(line, textColor),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            )
        }
        lineIdx++
    }
}

@Composable
fun RichMarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = ContentPrimary
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val parts = content.split("```")

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code Block Segment
                val firstLine = part.substringBefore("\n").trim()
                val codeBody = if (part.contains("\n")) part.substringAfter("\n").trimEnd() else part

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = CardSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = firstLine.ifEmpty { "code" },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(codeBody))
                                    Toast.makeText(context, "Kode berhasil disalin", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Salin Kode",
                                    tint = ContentSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Text(
                                text = codeBody,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    color = ContentPrimary,
                                    lineHeight = 17.sp
                                )
                            )
                        }
                    }
                }
            } else {
                RenderTextSegment(part, textColor)
            }
        }
    }
}
