package com.ledger.iou.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ColorTextPrimary,
    onPrimary = ColorBackground,
    primaryContainer = ColorSurfaceCardElevated,
    onPrimaryContainer = ColorTextPrimary,
    secondary = ColorAccentNeutral,
    onSecondary = ColorBackground,
    background = ColorBackground,
    onBackground = ColorTextPrimary,
    surface = ColorSurfaceCard,
    onSurface = ColorTextPrimary,
    surfaceVariant = ColorSurfaceCardElevated,
    onSurfaceVariant = ColorTextSecondary,
    outline = ColorSurfaceBorder,
    error = ColorAccentNegative,
    onError = ColorBackground
)

@Composable
fun LedgerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = ColorBackground.toArgb()
            window.navigationBarColor = ColorBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LedgerTypography,
        content = content
    )
}
