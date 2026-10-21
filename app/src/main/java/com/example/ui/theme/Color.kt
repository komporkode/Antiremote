package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Linear Dark Theme - Clean, Minimalist & Productive
val AppBg = Color(0xFF0E1015)
val CardSurface = Color(0xFF16181D)
val CardSurfaceVariant = Color(0xFF1F232B)
val CardBorder = Color(0xFF282D37)

// Professional Accent Colors (Linear Indigo & Status Colors)
val AccentPrimary = Color(0xFF5E6AD2)          // Linear Indigo / Violet
val AccentPrimarySoft = Color(0x285E6AD2)
val AccentSuccess = Color(0xFF2EA043)          // Clean GitHub/Linear Green
val AccentSuccessSoft = Color(0x242EA043)
val AccentWarning = Color(0xFFD29922)          // Warm Amber
val AccentWarningSoft = Color(0x24D29922)
val AccentDanger = Color(0xFFF85149)           // Clean Coral Red
val AccentDangerSoft = Color(0x24F85149)
val AccentPurple = Color(0xFFA371F7)           // Lavender
val AccentPurpleSoft = Color(0x24A371F7)
val AccentSecondary = AccentPrimary

// Diff Viewer Line Backgrounds & Highlighters
val DiffAddedBg = Color(0xFF112318)
val DiffAddedText = Color(0xFF56D364)
val DiffDeletedBg = Color(0xFF271416)
val DiffDeletedText = Color(0xFFFFA198)
val DiffHeaderBg = Color(0xFF191D24)
val DiffHeaderText = Color(0xFF7D8590)

// Typography Colors
val ContentPrimary = Color(0xFFF2F4F8)         // Clear off-white
val ContentSecondary = Color(0xFF949BA6)       // Readable soft neutral gray
val ContentTertiary = Color(0xFF5C6470)        // Muted label gray

// Terminal / Code Colors (Subtle & high contrast)
val TerminalDarkBg = Color(0xFF0A0C0F)
val TerminalLineNumbers = Color(0xFF484F58)

// Semantic aliases mapping old tokens to the clean theme
val DarkBg = AppBg
val SurfaceDark = CardSurface
val SurfaceVariantDark = CardSurfaceVariant
val SurfaceBorder = CardBorder

val NeonCyan = AccentPrimary
val NeonMint = AccentSuccess
val LaserPurple = AccentPurple
val CyberAmber = AccentWarning
val NeonRed = AccentDanger

val TextPrimary = ContentPrimary
val TextSecondary = ContentSecondary
val TextTertiary = ContentTertiary
val TerminalBg = TerminalDarkBg
