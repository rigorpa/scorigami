package com.scorigami.wear.ui.theme

import androidx.compose.ui.graphics.Color

// Yellow hole number on the scorecard
val HoleNumberColor = Color(0xFFFFD60A)

// Grey highlight for the currently selected hole in the hole-jump picker (matches phone)
val HoleJumpSelectedColor = Color(0xFF7A7A7A)

// Dark grey used for score buttons (−/+), Enter/Next Hole chip, and non-current hole cells
val WearButtonBackground = Color(0xFF2A2A2A)

// Amber dot on holes with missing scores in the hole-jump picker
val IncompleteHoleDotColor = Color(0xFFFFB300)

// Primary content color on dark surfaces — text/icons (mirrors phone AppColors)
val ContentWhite = Color.White

// Score colors vs par
val ScoreUnderParColor = Color(0xFF81C784)   // green — under par
val ScoreOverParColor = Color.Red            // red — over par, and active OB/C1x stat counters
// at par and unscored use ContentWhite

// OB / C1x stat counter while no count is entered — light grey so it stays readable on the
// black OLED background (WearButtonBackground was near-invisible as text)
val StatUnsetColor = Color(0xFF9E9E9E)
