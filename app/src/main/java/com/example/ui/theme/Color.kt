package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Modern, Clean Developer Dark Theme (Material 3 inspired, calming and professional)
val AppBg = Color(0xFF0F131A)
val CardSurface = Color(0xFF181D27)
val CardSurfaceVariant = Color(0xFF212836)
val CardBorder = Color(0xFF2C3547)

// Professional Accent Colors
val AccentPrimary = Color(0xFF388BFD)      // Google/Developer Blue
val AccentPrimarySoft = Color(0x22388BFD)
val AccentSuccess = Color(0xFF2EA043)      // Clean Green
val AccentSuccessSoft = Color(0x222EA043)
val AccentWarning = Color(0xFFD29922)      // Warm Amber
val AccentWarningSoft = Color(0x22D29922)
val AccentDanger = Color(0xFFF85149)       // Clean Red
val AccentDangerSoft = Color(0x22F85149)
val AccentPurple = Color(0xFFA371F7)       // Soft Lavender Purple
val AccentPurpleSoft = Color(0x22A371F7)
val AccentSecondary = AccentPurple

// Typography Colors
val ContentPrimary = Color(0xFFF0F6FC)     // Clear off-white
val ContentSecondary = Color(0xFF8B949E)   // Readable soft gray
val ContentTertiary = Color(0xFF6E7681)    // Muted label gray

// Terminal / Code Colors (Subtle & high contrast)
val TerminalDarkBg = Color(0xFF0D1117)
val TerminalLineNumbers = Color(0xFF484F58)

// Semantic aliases mapping old tokens to the clean, non-lebay theme
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
