package com.scorigami.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Tomorrow Night Blue palette — true dark (OLED black background)
private val TnbBackground         = Color(0xFF000000)
private val TnbSurface            = Color(0xFF00346E)
private val TnbSurfaceVariant     = Color(0xFF003A7A)
private val TnbPrimary            = Color(0xFFBBDAFF)
private val TnbOnPrimary          = Color(0xFF002451)
private val TnbPrimaryContainer   = Color(0xFF003A7A)
private val TnbOnPrimaryContainer = Color(0xFFBBDAFF)
private val TnbSecondary            = Color(0xFF99FFFF)
private val TnbOnSecondary          = Color(0xFF002451)
private val TnbSecondaryContainer   = Color(0xFF003D3D)
private val TnbOnSecondaryContainer = Color(0xFF99FFFF)
private val TnbOnBackground       = Color(0xFFFFFFFF)
private val TnbOnSurface          = Color(0xFFFFFFFF)
private val TnbOnSurfaceVariant   = Color(0xFF7285B7)
private val TnbError              = Color(0xFFFF9DA1)
private val TnbOnError            = Color(0xFF680020)
private val TnbErrorContainer     = Color(0xFF5C0010)
private val TnbOnErrorContainer   = Color(0xFFFF9DA1)

private val DarkColors = darkColorScheme(
    primary = TnbPrimary,
    onPrimary = TnbOnPrimary,
    primaryContainer = TnbPrimaryContainer,
    onPrimaryContainer = TnbOnPrimaryContainer,
    secondary = TnbSecondary,
    onSecondary = TnbOnSecondary,
    secondaryContainer = TnbSecondaryContainer,
    onSecondaryContainer = TnbOnSecondaryContainer,
    background = TnbBackground,
    onBackground = TnbOnBackground,
    surface = TnbSurface,
    onSurface = TnbOnSurface,
    surfaceVariant = TnbSurfaceVariant,
    onSurfaceVariant = TnbOnSurfaceVariant,
    error = TnbError,
    onError = TnbOnError,
    errorContainer = TnbErrorContainer,
    onErrorContainer = TnbOnErrorContainer
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
