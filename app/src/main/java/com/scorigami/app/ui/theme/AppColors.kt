package com.scorigami.app.ui.theme

import androidx.compose.ui.graphics.Color

// Scorigami font main color
val ScorigamiFont = Color.White

// Player score card and hole navigation card background
val CardBackground = Color(0xFF37474F)
val ScaleGrey2 = Color(0xFF5A6164)

// Hole info card background
val ScaleGrey1 = Color(0xFF354045)

val ScaleGrey0 = Color(0xFFA1A1A1)

// Yellow hole number on the scorecard and hole-jump grid
val HoleNumberColor = Color(0xFFFFD60A)

// Light Grey — the large hole number on the scorecard hole card
val ScorecardHoleNumberColor = Color(0xFFE0E0E0)

// Grey highlight for the currently selected hole in the Jump to Hole grid (phone and watch)
val HoleJumpSelectedColor = Color(0xFF7A7A7A)

// Amber dot on holes with missing scores
val IncompleteHoleDotColor = Color(0xFFFFB300)

// Primary content color on dark surfaces — text
val ContentWhite = Color.White

// Secondary lighter color on dark surfaces - text
val ContentLightGrey = Color(0xFFBDBDBD)

// App background color — list screens, list items, scorecard Round label
val ScreenBackground = Color.Black
//val ScreenBackground = Color(0xFF151A1D)
val DefaultCardBackground = Color(0xFF151A1D)

// Score colors vs par
val ScoreUnderParColor = Color(0xFF81C784)   // green  — under par

// Dark yellow — OB/C1x round-total lines (Review, Full-scorecard sheet, History detail)
val ObColor = Color(0xFFC9A227)

// OB / C1x stat counter buttons on the scorecard player card. The unset shade must differ
// from ScaleGrey2 — the card background — or the label disappears into it; ScaleGrey1 is
// darker than the card, so the label reads as a subtle inset.
val StatUnsetColor = ScaleGrey0             // quiet dark grey while no count is entered (bare "OB")

val StatActiveColor = Color(0xFFEF5350)     // OB once a count is entered (matches the theme error red)

val C1xActiveColor = Color(0xFFFF9800)      // C1x once a count is entered — orange, distinct from OB red
// at par and unscored use ContentWhite; over par uses MaterialTheme.colorScheme.error from the theme

// −/+ score buttons on the scorecard player card — matches the wear app's WearButtonBackground
val ScoreButtonBackground = Color(0xFF2A2A2A)

// Home screen button gradients (start → end, left to right)

val NewRoundGradientStart   = Color(0xFF1C2E42)   // deep grey
val NewRoundGradientEnd     = Color(0xFF474B50)   // sky grey

val CoursesGradientStart    = Color(0xFF24534B)   // dark jungle green
val CoursesGradientEnd      = Color(0xFF506B67)   // fresh green

val HistoryGradientStart    = Color(0xFF2D0C00)   // espresso brown
val HistoryGradientEnd      = Color(0xFFCC6B0A)   // warm amber

val ResumeGradientStart     = Color(0xFF4527A0)   // deep violet
val ResumeGradientEnd       = Color(0xFF7E57C2)   // soft lavender

val DarkGradientStart       = Color(0xFF272727)   // deep black
val DarkGradientEnd         = Color(0xFF5D5D5D)   // soft black

// Disabled home-screen button gradient (start → end)
val DisabledButtonGradientStart = Color(0xFF3A3A3A)
val DisabledButtonGradientEnd   = Color(0xFF5A5A5A)
