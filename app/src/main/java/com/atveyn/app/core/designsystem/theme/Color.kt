package com.atveyn.app.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// Modern Fluent Dark Theme Palette (Studied from Reference Designs)
// Deep pitch obsidian background (#08090D), rich charcoal-slate surfaces (#10121A, #151722),
// and electric violet/blurple brand gradients (#7059FE -> #8B5CF6).
// ============================================================================

val DarkBg = Color(0xFF08090D)            // Deepest pitch black background
val DarkSurface = Color(0xFF10121A)       // Primary card surface
val DarkSurfaceElevated = Color(0xFF151722) // Translucent/Elevated card base
val DarkSurfaceHighlight = Color(0xFF1C1E2B) // Pills, inputs, inner chips
val DarkSurfaceBorder = Color(0xFF262838)    // Hairline crisp card border
val DarkSurfaceBorderSubtle = Color(0x18FFFFFF) // Subtle top rim sheen

// Backwards-compatible aliases for Navy* constants
val Navy950 = DarkBg
val Navy900 = DarkSurface
val Navy850 = DarkSurfaceElevated
val Navy800 = DarkSurfaceHighlight
val Navy700 = DarkSurfaceBorder

// Electric Violet & Brand Accents
val BrandViolet = Color(0xFF7059FE)       // Electric blurple primary
val BrandVioletLight = Color(0xFF8B5CF6)  // Gradient secondary stop
val BrandVioletDark = Color(0xFF5B4AE4)   // Deep violet
val BrandVioletGlow = Color(0x357059FE)   // Ambient glow
val BrandVioletSubtle = Color(0x1C7059FE) // Container tint

val Indigo600 = BrandVioletDark
val Indigo500 = BrandViolet
val Indigo400 = BrandVioletLight

// Secondary Cyan / Electric Sky
val Cyan500 = Color(0xFF06B6D4)
val Cyan400 = Color(0xFF22D3EE)
val CyanGlow = Color(0x2806B6D4)
val CyanSubtle = Color(0x1806B6D4)

// Light Palette (Subtle clean ice tone)
val LightBg = Color(0xFFF6F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceTranslucent = Color(0xEEFFFFFF)
val LightSurfaceElevated = Color(0xFFF8FAFC)
val LightSurfaceHighlight = Color(0xFFF1F5F9)
val LightSurfaceBorder = Color(0x30CBD5E1)
val LightBorder = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)

// Attendance Status Colors (Fluent, luminous pills with borders)
val StatusGreen = Color(0xFF10B981)       // Above target (Emerald)
val StatusGreenBg = Color(0x2210B981)
val StatusGreenBorder = Color(0x4510B981)
val StatusGreenGlow = Color(0x3510B981)

val StatusAmber = Color(0xFFF59E0B)       // Warning near threshold
val StatusAmberBg = Color(0x22F59E0B)
val StatusAmberBorder = Color(0x45F59E0B)
val StatusAmberGlow = Color(0x35F59E0B)

val StatusRed = Color(0xFFEF4444)         // Shortage alert (Neon Coral)
val StatusRedBg = Color(0x22EF4444)
val StatusRedBorder = Color(0x45EF4444)
val StatusRedGlow = Color(0x35EF4444)

val StatusPending = Color(0xFF8B5CF6)     // Purple for pending
val StatusPendingBg = Color(0x228B5CF6)
val StatusCancelled = Color(0xFF64748B)   // Neutral slate
val StatusCancelledBg = Color(0x1F64748B)

// Glass Borders & Gradients
val GlassBorderDark = Color(0x357059FE)
val GlassBorderLight = Color(0x40CBD5E1)
val GlassOverlayDark = Color(0x12FFFFFF)
val GlassOverlayLight = Color(0x60FFFFFF)

// Gradients for Fluent Components
val BrandGradient = Brush.horizontalGradient(listOf(BrandViolet, BrandVioletLight))
val BrandGradientVertical = Brush.verticalGradient(listOf(BrandViolet, BrandVioletLight))
val GreenGradient = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF34D399)))
val AmberGradient = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)))
val RedGradient = Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFF87171)))
val CardBorderGradient = Brush.verticalGradient(
    listOf(
        Color(0x388B5CF6),
        Color(0x0C8B5CF6)
    )
)

// Subject Palette Colors for user selection (Vibrant modern accents)
val SubjectPalette = listOf(
    Color(0xFF7059FE), // Electric Violet
    Color(0xFF06B6D4), // Cyan
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFFEC4899), // Neon Pink
    Color(0xFF8B5CF6), // Purple
    Color(0xFF3B82F6), // Royal Blue
    Color(0xFF14B8A6), // Teal
    Color(0xFFF97316), // Vivid Orange
    Color(0xFFE11D48), // Rose
    Color(0xFF84CC16), // Lime
    Color(0xFF6366F1)  // Blurple
)
