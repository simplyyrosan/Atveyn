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
    primary = BrandViolet,
    onPrimary = Color.White,
    primaryContainer = BrandVioletSubtle,
    onPrimaryContainer = BrandVioletLight,
    secondary = Cyan500,
    onSecondary = Color.Black,
    secondaryContainer = CyanSubtle,
    onSecondaryContainer = Cyan400,
    background = DarkBg,
    onBackground = Color(0xFFFFFFFF),
    surface = DarkSurface,
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = DarkSurfaceBorder,
    outlineVariant = Color(0xFF1C1E2B)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandVioletDark,
    onPrimary = Color.White,
    primaryContainer = BrandVioletSubtle,
    onPrimaryContainer = BrandVioletDark,
    secondary = Cyan500,
    onSecondary = Color.White,
    secondaryContainer = CyanSubtle,
    onSecondaryContainer = Cyan500,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = Color(0xFFE2E8F0)
)

@Immutable
data class ExtendedGlassColors(
    val cardBackground: Color,
    val cardBackgroundElevated: Color,
    val cardBackgroundHighlight: Color,
    val cardBorder: Brush,
    val cardBorderSubtle: Brush,
    val brandGradient: Brush,
    val subtleOverlay: Color,
    val isDark: Boolean
)

val LocalGlassColors = staticCompositionLocalOf {
    ExtendedGlassColors(
        cardBackground = DarkSurface,
        cardBackgroundElevated = DarkSurfaceElevated,
        cardBackgroundHighlight = DarkSurfaceHighlight,
        cardBorder = CardBorderGradient,
        cardBorderSubtle = Brush.verticalGradient(listOf(DarkSurfaceBorder, Color.Transparent)),
        brandGradient = BrandGradient,
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
            cardBackground = DarkSurface,
            cardBackgroundElevated = DarkSurfaceElevated,
            cardBackgroundHighlight = DarkSurfaceHighlight,
            cardBorder = CardBorderGradient,
            cardBorderSubtle = Brush.verticalGradient(
                listOf(
                    Color(0xFF262838),
                    Color(0x10262838)
                )
            ),
            brandGradient = BrandGradient,
            subtleOverlay = GlassOverlayDark,
            isDark = true
        )
    } else {
        ExtendedGlassColors(
            cardBackground = Color(0xFFFFFFFF),
            cardBackgroundElevated = Color(0xFFF8FAFC),
            cardBackgroundHighlight = Color(0xFFF1F5F9),
            cardBorder = Brush.verticalGradient(
                listOf(
                    Color(0x70CBD5E1),
                    Color(0x20E2E8F0)
                )
            ),
            cardBorderSubtle = Brush.verticalGradient(
                listOf(
                    Color(0x40E2E8F0),
                    Color(0x10E2E8F0)
                )
            ),
            brandGradient = BrandGradient,
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
