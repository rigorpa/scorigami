package com.scorigami.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Warm neutral scheme built from the Material Theme Builder tonal palette in
// AppColors.kt. Neutral slots come straight from the palette tokens; primary and
// secondary are neutralized to warm monochrome (the palette defines no accent
// hues — Material assigns those to separate tonal palettes). Error keeps the
// established red so over-par and OB stay semantic.
private val ThemeError            = Color(0xFFEF5350)
private val ThemeOnError          = Color(0xFF680020)
private val ThemeErrorContainer   = Color(0xFF5C0010)
private val ThemeOnErrorContainer = Color(0xFFFF9DA1)

private val DarkColors = darkColorScheme(
    primary = OnSurface,
    onPrimary = SurfaceDim,
    primaryContainer = SurfaceContainerHigh,
    onPrimaryContainer = OnSurface,
    secondary = OnSurfaceVariant,
    onSecondary = SurfaceDim,
    secondaryContainer = SurfaceContainer,
    onSecondaryContainer = OnSurface,
    background = SurfaceDim,
    onBackground = OnSurface,
    surface = SurfaceDim,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceDim,
    surfaceContainerLow = SurfaceContainer,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHigh,
    error = ThemeError,
    onError = ThemeOnError,
    errorContainer = ThemeErrorContainer,
    onErrorContainer = ThemeOnErrorContainer
)

private val AppTypography = Typography(
    displayLarge  = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontSize = 45.sp, fontWeight = FontWeight.Normal),
    displaySmall  = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Normal),
    headlineLarge  = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall  = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge  = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.15.sp),
    titleSmall  = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium,   letterSpacing = 0.1.sp),
    bodyLarge   = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.5.sp),
    bodyMedium  = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.25.sp),
    bodySmall   = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.4.sp),
    labelLarge  = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
)

@Composable
fun ScorigamiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
