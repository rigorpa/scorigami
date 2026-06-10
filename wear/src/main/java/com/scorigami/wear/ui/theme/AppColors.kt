package com.scorigami.wear.ui.theme

import androidx.compose.ui.graphics.Color

// Yellow hole number and current-hole highlight in the hole-jump picker
val HoleNumberColor = Color(0xFFFFD60A)

// Dark grey used for score buttons (−/+), Enter/Next Hole chip, and non-current hole cells
val WearButtonBackground = Color(0xFF2A2A2A)

// Amber dot on holes with missing scores in the hole-jump picker
val IncompleteHoleDotColor = Color(0xFFFFB300)

// Score colors vs par
val ScoreUnderParColor = Color(0xFF81C784)   // green  — under par
// at par and unscored use Color.White (standard Compose constant, no alias needed)
// over par uses MaterialTheme.colors.error from the theme
