package com.scorigami.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * App-wide font sizing. Everything text-sized flows from the one knob below:
 *
 * - `MaterialTheme.typography.*` styles are built from the scale in Theme.kt
 * - One-off hard-coded sizes in screens use `N.scaledSp` instead of `N.sp`
 *
 * To adjust all fonts today: change [CurrentFontSize]. To make it user-selectable
 * later (Menu → Font Size → Small/Medium/Large): store an [AppFontSize] in a
 * DataStore preference and pass it into `ScorigamiTheme(fontSize = …)` from
 * MainActivity — nothing else needs to change.
 */
enum class AppFontSize(val label: String, val scale: Float) {
    Small("Small", 0.85f),
    Medium("Medium", 1.0f),
    Large("Large", 1.15f)
}

/**
 * THE FONT KNOB — the default font size on first launch. The live value is the
 * user's choice from Home → ⚙ → Font Size, persisted by SettingsRepository and
 * collected into ScorigamiTheme by MainActivity.
 */
val CurrentFontSize = AppFontSize.Medium

/** Provided by ScorigamiTheme; read via [scaledSp] for sizes outside the typography scale. */
val LocalFontScale = staticCompositionLocalOf { 1f }

/** A one-off font size that follows the app font-size setting: use `28.scaledSp` not `28.sp`. */
val Int.scaledSp: TextUnit
    @Composable
    @ReadOnlyComposable
    get() = (this * LocalFontScale.current).sp
