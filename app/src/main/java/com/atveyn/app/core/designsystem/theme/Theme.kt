package com.atveyn.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Indigo500,
    onPrimary = Color.White,
    primaryContainer = Indigo600.copy(alpha = 0.25f),
    onPrimaryContainer = Indigo400,
    secondary = Cyan500,
    onSecondary = Color.Black,
    secondaryContainer = Cyan500.copy(alpha = 0.2f),
    onSecondaryContainer = Cyan400,
    background = Navy950,
    onBackground = Color(0xFFF8FAFC),
    surface = Navy900,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Navy850,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Navy700
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Indigo500.copy(alpha = 0.12f),
    onPrimaryContainer = Indigo600,
    secondary = Cyan500,
    onSecondary = Color.White,
    secondaryContainer = Cyan500.copy(alpha = 0.12f),
    onSecondaryContainer = Cyan500,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

@Immutable
data class ExtendedGlassColors(
    val cardBackground: Color,
    val cardBackgroundElevated: Color,
    val cardBorder: Brush,
    val subtleOverlay: Color,
    val isDark: Boolean
)

val LocalGlassColors = staticCompositionLocalOf {
    ExtendedGlassColors(
        cardBackground = Navy850.copy(alpha = 0.82f),
        cardBackgroundElevated = Navy800.copy(alpha = 0.88f),
        cardBorder = Brush.verticalGradient(listOf(GlassBorderDark, Color(0x05818CF8))),
        subtleOverlay = GlassOverlayDark,
        isDark = true
    )
}

object AtveynTheme {
    val glass: ExtendedGlassColors
        @Composable
        get() = LocalGlassColors.current
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Composable
fun AtveynTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val glassColors = if (darkTheme) {
        ExtendedGlassColors(
            cardBackground = Navy850.copy(alpha = 0.82f),
            cardBackgroundElevated = Navy800.copy(alpha = 0.88f),
            cardBorder = Brush.verticalGradient(
                listOf(
                    Color(0x40818CF8),
                    Color(0x10818CF8)
                )
            ),
            subtleOverlay = GlassOverlayDark,
            isDark = true
        )
    } else {
        ExtendedGlassColors(
            cardBackground = Color(0xF5FFFFFF),
            cardBackgroundElevated = Color(0xFFF8FAFC),
            cardBorder = Brush.verticalGradient(
                listOf(
                    Color(0x60CBD5E1),
                    Color(0x20E2E8F0)
                )
            ),
            subtleOverlay = GlassOverlayLight,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalGlassColors provides glassColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
