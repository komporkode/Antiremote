package com.example

import com.example.ui.components.parseInlineMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun testParseInlineMarkdown() {
        val input = "Halo **tebal** dan *miring* serta `kode inline`"
        val annotated = parseInlineMarkdown(input)
        
        assertEquals("Halo tebal dan miring serta kode inline", annotated.text)
        assertTrue(annotated.spanStyles.isNotEmpty())
    }

    @Test
    fun testParseMarkdownLink() {
        val input = "Lihat [komit](file:///repo/path) ini"
        val annotated = parseInlineMarkdown(input)

        assertEquals("Lihat komit ini", annotated.text)
    }
}
