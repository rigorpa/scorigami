package com.scorigami.app.ui.theme

import androidx.compose.ui.graphics.Color

// Player score card and hole navigation card background
val CardBackground = Color(0xFF1A3652)

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
