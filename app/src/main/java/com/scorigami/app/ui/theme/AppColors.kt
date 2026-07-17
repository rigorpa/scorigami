package com.scorigami.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Material neutral tonal palette (Google Material Theme Builder) — the single
// source of truth for every neutral in the app. Ordered darkest → lightest.
// "Surface" (plain) shares SurfaceDim's value; it is not declared separately
// because a top-level `Surface` val would clash with material3's Surface
// composable at call sites.
// ---------------------------------------------------------------------------
val SurfaceDim           = Color(0xFF15130B)   // darkest — screen background
val SurfaceContainer     = Color(0xFF222017)   // dark medium — cards, section bubbles
val SurfaceContainerHigh = Color(0xFF2D2A21)   // dark light — elevated cells, dialogs
val SurfaceBright        = Color(0xFF3C3930)   // dark lightest — highest-emphasis fills
val OutlineVariant       = Color(0xFF4B4739)   // light dark — dividers, subtle borders
val Outline              = Color(0xFF969080)   // light medium — muted labels, outlines
val OnSurfaceVariant     = Color(0xFFCDC6B4)   // light — secondary text/icons
val OnSurface            = Color(0xFFE8E2D4)   // lightest — primary text/icons

// ---------------------------------------------------------------------------
// Role aliases — the names the screens actually use. Each resolves to a
// palette token above, so the whole app re-themes from one place.
// ---------------------------------------------------------------------------

// Scorigami title on the home screen
val ScorigamiFont = OnSurface

// Hole-jump grid non-selected cell background
val CardBackground = SurfaceContainerHigh

// Share-round dialog chrome
val ScaleGrey1 = SurfaceContainerHigh

// The large hole number on the scorecard hole card — quieter than primary text
val ScorecardHoleNumberColor = OnSurfaceVariant

// Selected hole cell in the Jump to Hole grid — brightest surface = highest emphasis
val HoleJumpSelectedColor = SurfaceBright

// Amber dot on holes with missing scores (semantic accent — not part of the neutral palette)
val IncompleteHoleDotColor = Color(0xFFFFB300)

// Primary content color on dark surfaces — text/icons
val ContentWhite = OnSurface

// Secondary text/icons on dark surfaces
val ContentLightGrey = OnSurfaceVariant

// App background — list screens, list items, scorecard
val ScreenBackground = SurfaceDim

// Card fill for the scorecard player/hole cards and setup-screen section bubbles
val DefaultCardBackground = SurfaceContainer

// Score colors vs par (semantic accents — kept outside the neutral palette)
val ScoreUnderParColor = Color(0xFF81C784)   // green — under par

// OB / C1x stat counter buttons: quiet muted label while unset, semantic color once set
val StatUnsetColor = Outline
val StatActiveColor = Color(0xFFEF5350)      // OB — matches the theme error red
val C1xActiveColor = Color(0xFFFF9800)       // C1x — orange, distinct from OB red
// at par and unscored use ContentWhite; over par uses MaterialTheme.colorScheme.error

// −/+ score buttons on the player card — brightest fill so the controls pop
val ScoreButtonBackground = SurfaceBright

// ---------------------------------------------------------------------------
// Brand identity gradients (start → end, left to right) — chromatic accents
// deliberately outside the neutral palette (Material assigns accent roles to
// separate tonal palettes; this list only defines the neutrals).
// ---------------------------------------------------------------------------

val NewRoundGradientStart   = Color(0xFF1C2E42)   // deep grey
val NewRoundGradientEnd     = Color(0xFF474B50)   // sky grey

val CoursesGradientStart    = Color(0xFF10443F)   // dark jungle green
val CoursesGradientEnd      = Color(0xFF5A9791)   // fresh green

val HistoryGradientStart    = Color(0xFF2D0C00)   // espresso brown
val HistoryGradientEnd      = Color(0xFFCC6B0A)   // warm amber

val ResumeGradientStart     = Color(0xFF4527A0)   // deep violet
val ResumeGradientEnd       = Color(0xFF7E57C2)   // soft lavender

// Disabled home-screen button gradient — muted warm neutrals from the palette
val DisabledButtonGradientStart = SurfaceContainerHigh
val DisabledButtonGradientEnd   = OutlineVariant


val NewRoundGradientTop     = Color(0xFF101222)
val NewRoundGradientBottom  = Color(0xFF1F2344)
val CoursesGradientTop      = Color(0xFF3E4489)
val CoursesGradientBottom   = Color(0xFF64294)
val HistoryGradientTop      = Color(0xFF92442B)
val HistoryGradientBottom   = Color(0xFFCF7C63)

// Test color samples for Home Screen widgets //

//val NewRoundGradientStart   = SurfaceContainerHigh
//val NewRoundGradientEnd     = SurfaceContainerHigh

//val CoursesGradientStart    = SurfaceBright
//val CoursesGradientEnd      = SurfaceBright

//val HistoryGradientStart    = OutlineVariant
//val HistoryGradientEnd      = OutlineVariant
