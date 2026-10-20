package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CleanDarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = ContentPrimary,
    primaryContainer = CardSurfaceVariant,
    onPrimaryContainer = AccentPrimary,
    secondary = AccentSuccess,
    onSecondary = ContentPrimary,
    secondaryContainer = CardSurfaceVariant,
    onSecondaryContainer = AccentSuccess,
    tertiary = AccentPurple,
    onTertiary = ContentPrimary,
    background = AppBg,
    onBackground = ContentPrimary,
    surface = CardSurface,
    onSurface = ContentPrimary,
    surfaceVariant = CardSurfaceVariant,
    onSurfaceVariant = ContentSecondary,
    outline = CardBorder,
    error = AccentDanger,
    onError = ContentPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = CleanDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = AppBg.toArgb()
                window.navigationBarColor = AppBg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
