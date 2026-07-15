package com.scorigami.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware color palette. One instance per theme (dark / light), provided through
 * [LocalAppPalette] by ScorigamiTheme. Screen files never touch this class directly —
 * they keep using the top-level accessor vals below (same names as the old constants),
 * which resolve against whichever palette is active.
 */
@Immutable
data class AppPalette(
    val screenBackground: Color,
    val contentWhite: Color,          // primary content color (white on dark, near-black on light)
    val contentLightGrey: Color,      // secondary content color
    val cardBackground: Color,        // hole-jump grid non-selected cells
    val scaleGrey1: Color,            // hole info card background
    val scaleGrey2: Color,            // player score card background
    val scoreButtonBackground: Color, // −/+ filled score buttons
    val holeJumpSelected: Color,      // current-hole cell in the hole-jump grid
    val statUnset: Color,             // OB/C1x counter while no count entered
    val statActive: Color,            // OB counter once a count is entered (red)
    val c1xActive: Color,             // C1x counter once a count is entered (orange)
    val obColor: Color,               // OB/C1x round-total lines
    val scoreUnderPar: Color,         // under-par score display (green)
    val holeNumber: Color,            // big hole number / hole-jump accents
    val sectionGradientStart: Color,  // round-setup section bubble gradient
    val sectionGradientEnd: Color,
)

val DarkPalette = AppPalette(
    screenBackground = Color.Black,
    contentWhite = Color.White,
    contentLightGrey = Color(0xFFBDBDBD),
    cardBackground = Color(0xFF37474F),
    scaleGrey1 = Color(0xFF354045),
    scaleGrey2 = Color(0xFF5A6164),
    scoreButtonBackground = Color(0xFF2A2A2A),
    holeJumpSelected = Color(0xFF7A7A7A),
    statUnset = Color(0xFF354045),         // == scaleGrey1 — reads as a subtle inset on the card
    statActive = Color(0xFFEF5350),
    c1xActive = Color(0xFFFF9800),
    obColor = Color(0xFFC9A227),
    scoreUnderPar = Color(0xFF81C784),
    holeNumber = Color(0xFFFFD60A),
    sectionGradientStart = Color(0xFF171717),
    sectionGradientEnd = Color(0xFF292929),
)

val LightPalette = AppPalette(
    screenBackground = Color(0xFFF4F6F8),  // soft off-white, not pure white
    contentWhite = Color(0xFF1A1C1E),      // near-black text
    contentLightGrey = Color(0xFF5F6368),
    cardBackground = Color(0xFFE2E8ED),
    scaleGrey1 = Color(0xFFDDE3E7),
    scaleGrey2 = Color(0xFFCBD2D7),
    scoreButtonBackground = Color(0xFFD5DADE),
    holeJumpSelected = Color(0xFFAEB8BF),
    statUnset = Color(0xFFC6CFD5),         // darker than the light card — same inset rationale as dark
    statActive = Color(0xFFD32F2F),        // deeper red for light backgrounds
    c1xActive = Color(0xFFE65100),         // deeper orange
    obColor = Color(0xFF9A7B0A),
    scoreUnderPar = Color(0xFF2E7D32),     // dark green — 81C784 fails contrast on white
    holeNumber = Color(0xFFB58900),        // dark goldenrod — FFD60A is invisible on white
    sectionGradientStart = Color(0xFFE6E9EC),
    sectionGradientEnd = Color(0xFFD8DCE0),
)

val LocalAppPalette = staticCompositionLocalOf { DarkPalette }

// ---------------------------------------------------------------------------
// Theme-aware accessors — same names as the old top-level constants, so the
// ~19 consumer files compile unchanged. Names keep their dark-theme reading
// ("ContentWhite" = primary content color, near-black in light mode).
// ---------------------------------------------------------------------------

val ScreenBackground: Color      @Composable get() = LocalAppPalette.current.screenBackground
val ContentWhite: Color          @Composable get() = LocalAppPalette.current.contentWhite
val ContentLightGrey: Color      @Composable get() = LocalAppPalette.current.contentLightGrey
val CardBackground: Color        @Composable get() = LocalAppPalette.current.cardBackground
val ScaleGrey1: Color            @Composable get() = LocalAppPalette.current.scaleGrey1
val ScaleGrey2: Color            @Composable get() = LocalAppPalette.current.scaleGrey2
val ScoreButtonBackground: Color @Composable get() = LocalAppPalette.current.scoreButtonBackground
val HoleJumpSelectedColor: Color @Composable get() = LocalAppPalette.current.holeJumpSelected
val StatUnsetColor: Color        @Composable get() = LocalAppPalette.current.statUnset
val StatActiveColor: Color       @Composable get() = LocalAppPalette.current.statActive
val C1xActiveColor: Color        @Composable get() = LocalAppPalette.current.c1xActive
val ObColor: Color               @Composable get() = LocalAppPalette.current.obColor
val ScoreUnderParColor: Color    @Composable get() = LocalAppPalette.current.scoreUnderPar
val HoleNumberColor: Color       @Composable get() = LocalAppPalette.current.holeNumber
val DarkGradientStart: Color     @Composable get() = LocalAppPalette.current.sectionGradientStart
val DarkGradientEnd: Color       @Composable get() = LocalAppPalette.current.sectionGradientEnd

// ---------------------------------------------------------------------------
// Theme-invariant colors — identical in both themes, stay plain vals.
// ---------------------------------------------------------------------------

/**
 * Fixed white for content sitting ON the identity gradients below, which stay dark in
 * both themes. Do NOT use ContentWhite there — it flips to near-black in light mode and
 * becomes unreadable on the dark gradients.
 */
val GradientContentWhite = Color.White

// Amber dot on holes with missing scores — reads on both themes' card greys
val IncompleteHoleDotColor = Color(0xFFFFB300)

// Home screen / top bar identity gradients (start → end, left to right)
val NewRoundGradientStart   = Color(0xFF1C2E42)   // deep grey
val NewRoundGradientEnd     = Color(0xFF474B50)   // sky grey

val CoursesGradientStart    = Color(0xFF24534B)   // dark jungle green
val CoursesGradientEnd      = Color(0xFF506B67)   // fresh green

val HistoryGradientStart    = Color(0xFF2D0C00)   // espresso brown
val HistoryGradientEnd      = Color(0xFFCC6B0A)   // warm amber

val ResumeGradientStart     = Color(0xFF4527A0)   // deep violet
val ResumeGradientEnd       = Color(0xFF7E57C2)   // soft lavender

// Disabled home-screen button gradient (start → end)
val DisabledButtonGradientStart = Color(0xFF3A3A3A)
val DisabledButtonGradientEnd   = Color(0xFF5A5A5A)
