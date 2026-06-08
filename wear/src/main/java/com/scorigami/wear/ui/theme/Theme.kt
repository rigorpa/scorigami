package com.scorigami.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val WearColors = Colors(
    primary = Color(0xFFBBDAFF),
    primaryVariant = Color(0xFF99BBEE),
    secondary = Color(0xFF99FFFF),
    secondaryVariant = Color(0xFF66DDDD),
    background = Color(0xFF000000),
    surface = Color(0xFF00346E),
    error = Color(0xFFFF9DA1),
    onPrimary = Color(0xFF002451),
    onSecondary = Color(0xFF002451),
    onBackground = Color(0xFFE8EAF0),
    onSurface = Color(0xFFE8EAF0),
    onSurfaceVariant = Color(0xFF99AACC),
    onError = Color(0xFF002451)
)

@Composable
fun ScorigamiWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = WearColors, content = content)
}
