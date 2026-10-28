package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
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
 * Handles nested formatting cleanly (e.g., links or code inside bold text)
 */
fun parseInlineMarkdown(
    raw: String,
    normalColor: Color = ContentPrimary,
    accentColor: Color = AccentPrimary,
    isParentBold: Boolean = false
): AnnotatedString {
    return buildAnnotatedString {
        // Regex matches:
        // 1 & 2: Bold **text**
        // 3 & 4: Inline Code `text`
        // 5, 6, 7: Link [text](url)
        // 8 & 9: Italic *text*
        val pattern = Regex("""(\*\*([^*]+)\*\*)|(`([^`]+)`)|(\[([^\]]+)\]\(([^)]+)\))|(\*([^*]+)\*)""")
        var cursor = 0

        for (match in pattern.findAll(raw)) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > cursor) {
                val plain = raw.substring(cursor, start)
                val s = length
                append(plain)
                if (isParentBold) {
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), s, length)
                }
            }

            when {
                // Bold: **text** -> Recursively parse inside so links and code render cleanly
                match.groups[2] != null -> {
                    val innerText = match.groups[2]!!.value
                    val parsedInner = parseInlineMarkdown(innerText, normalColor, accentColor, isParentBold = true)
                    val s = length
                    append(parsedInner)
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), s, length)
                }
                // Inline Code: `text`
                match.groups[4] != null -> {
                    val text = match.groups[4]!!.value
                    val s = length
                    append(text)
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            color = accentColor,
                            fontWeight = if (isParentBold) FontWeight.Bold else FontWeight.Medium,
                            background = CardBorder.copy(alpha = 0.55f)
                        ),
                        s,
                        length
                    )
                }
                // Link: [text](url) -> Display only the link title cleanly
                match.groups[6] != null -> {
                    val linkTitle = match.groups[6]!!.value
                    val s = length
                    append(linkTitle)
                    addStyle(
                        SpanStyle(
                            color = accentColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = if (isParentBold) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        s,
                        length
                    )
                }
                // Italic: *text*
                match.groups[9] != null -> {
                    val text = match.groups[9]!!.value
                    val s = length
                    append(text)
                    addStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic,
                            fontWeight = if (isParentBold) FontWeight.Bold else FontWeight.Normal
                        ),
                        s,
                        length
                    )
                }
            }
            cursor = end
        }

        if (cursor < raw.length) {
            val s = length
            append(raw.substring(cursor))
            if (isParentBold) {
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), s, length)
            }
        }
    }
}

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

        val headerMatch = Regex("""^(#{1,6})\s+(.*)""").find(line.trimStart())
        val isHorizontalRule = line.trim() == "---" || line.trim() == "***" || line.trim() == "___"

        if (line.isBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
        } else if (isHorizontalRule) {
            HorizontalDivider(
                color = CardBorder,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        } else if (headerMatch != null) {
            // Heading levels 1 to 6
            val level = headerMatch.groups[1]!!.value.length
            val title = headerMatch.groups[2]!!.value
            val (fontSize, fontWeight, topPadding) = when (level) {
                1 -> Triple(16.sp, FontWeight.Bold, 10.dp)
                2 -> Triple(15.sp, FontWeight.Bold, 8.dp)
                3 -> Triple(14.sp, FontWeight.Bold, 6.dp)
                4 -> Triple(13.sp, FontWeight.SemiBold, 5.dp)
                else -> Triple(12.5.sp, FontWeight.SemiBold, 4.dp)
            }
            Text(
                text = parseInlineMarkdown(title.trim(), textColor),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = fontWeight,
                    color = textColor,
                    fontSize = fontSize,
                    lineHeight = (fontSize.value * 1.35).sp
                ),
                modifier = Modifier.padding(top = topPadding, bottom = 3.dp)
            )
        } else if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
            val trimmed = line.trimStart().removePrefix("- ").removePrefix("* ")
            val indent = ((line.length - line.trimStart().length) / 2).coerceAtMost(3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (indent * 12).dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(5.dp)
                        .background(AccentPrimary, CircleShape)
                )
                Text(
                    text = parseInlineMarkdown(trimmed, textColor),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        fontSize = 12.5.sp,
                        lineHeight = 18.5.sp
                    )
                )
            }
        } else if (Regex("""^\s*(\d+\.)\s*(.*)""").containsMatchIn(line)) {
            val numMatch = Regex("""^\s*(\d+\.)\s*(.*)""").find(line)
            val numPrefix = numMatch?.groups?.get(1)?.value ?: "1."
            val rest = numMatch?.groups?.get(2)?.value ?: line
            val indent = ((line.length - line.trimStart().length) / 2).coerceAtMost(3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (indent * 12).dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = numPrefix,
                    color = AccentPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = parseInlineMarkdown(rest, textColor),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        fontSize = 12.5.sp,
                        lineHeight = 18.5.sp
                    )
                )
            }
        } else {
            // Standard paragraph
            Text(
                text = parseInlineMarkdown(line, textColor),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor,
                    fontSize = 12.5.sp,
                    lineHeight = 18.5.sp
                ),
                modifier = Modifier.padding(vertical = 1.dp)
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

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code Block Segment
                val firstLine = part.substringBefore("\n").trim()
                val codeBody = if (part.contains("\n")) part.substringAfter("\n").trimEnd() else part

                val isDiff = firstLine.contains("diff", ignoreCase = true) ||
                    firstLine.contains("patch", ignoreCase = true) ||
                    codeBody.lines().any { it.startsWith("+") || it.startsWith("-") }

                val isPatchApplied = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF14171F),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isDiff) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF3B2D54),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6))
                                    ) {
                                        Text(
                                            text = "DIAGNOSIS & PATCH",
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFFC4B5FD),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = firstLine.ifEmpty { if (isDiff) "diff" else "code" },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AccentPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isDiff) {
                                    FilledTonalButton(
                                        onClick = {
                                            isPatchApplied.value = true
                                            clipboardManager.setText(AnnotatedString(codeBody))
                                            Toast.makeText(context, "Patch disalin dan siap diaplikasikan", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (isPatchApplied.value) AccentSuccess else AccentPrimary,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isPatchApplied.value) Icons.Filled.Check else Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (isPatchApplied.value) "Applied" else "Apply Patch",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

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
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            if (isDiff) {
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    codeBody.lines().forEach { line ->
                                        val lineCol = when {
                                            line.startsWith("+") -> AccentSuccess
                                            line.startsWith("-") -> AccentDanger
                                            line.startsWith("@@") -> AccentWarning
                                            else -> ContentPrimary
                                        }
                                        val lineBg = when {
                                            line.startsWith("+") -> AccentSuccess.copy(alpha = 0.12f)
                                            line.startsWith("-") -> AccentDanger.copy(alpha = 0.12f)
                                            else -> Color.Transparent
                                        }
                                        Text(
                                            text = line,
                                            modifier = Modifier
                                                .background(lineBg)
                                                .fillMaxWidth(),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = lineCol,
                                                lineHeight = 16.sp
                                            )
                                        )
                                    }
                                }
                            } else {
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
                }
            } else {
                RenderTextSegment(part, textColor)
            }
        }
    }
}
