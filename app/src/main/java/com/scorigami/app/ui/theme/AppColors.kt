package com.scorigami.app.ui.theme

import androidx.compose.ui.graphics.Color

// Player score card and hole navigation card background
val CardBackground = Color(0xFF1A3652)

// Hole info card background
val CardGrey = Color(0xFF18191C)

// Yellow hole number on the scorecard and hole-jump grid
val HoleNumberColor = Color(0xFFFFD60A)

// Grey highlight for the currently selected hole in the Jump to Hole grid (phone and watch)
val HoleJumpSelectedColor = Color(0xFF7A7A7A)

// Amber dot on holes with missing scores
val IncompleteHoleDotColor = Color(0xFFFFB300)

// Score colors vs par
val ScoreUnderParColor = Color(0xFF81C784)   // green  — under par
// at par and unscored use Color.White (standard Compose constant, no alias needed)
// over par uses MaterialTheme.colorScheme.error from the theme

// Home screen button gradients (start → end, left to right)
val NewRoundGradientStart   = Color(0xFF0A2472)   // deep navy
val NewRoundGradientEnd     = Color(0xFF1CA7EC)   // sky blue

val CoursesGradientStart    = Color(0xFF114B20)   // dark jungle green
val CoursesGradientEnd      = Color(0xFF43A047)   // fresh green

val HistoryGradientStart    = Color(0xFF2D0C00)   // espresso brown
val HistoryGradientEnd      = Color(0xFFCC6B0A)   // warm amber

val ResumeGradientStart     = Color(0xFF4527A0)   // deep violet
val ResumeGradientEnd       = Color(0xFF7E57C2)   // soft lavender
