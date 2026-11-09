package com.scorigami.wear.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.scorigami.wear.ui.theme.C1xActiveColor
import com.scorigami.wear.ui.theme.C2ActiveColor
import com.scorigami.wear.ui.theme.ContentWhite
import com.scorigami.wear.ui.theme.HoleNumberColor
import com.scorigami.wear.ui.theme.StatUnsetColor
import com.scorigami.wear.ui.theme.WearButtonBackground

/**
 * Putting-stat picker opened from the "CX" button. Inline full-screen Scaffold branch like
 * [TeeOrderScreen] (not a wear Dialog — those are slow to appear). Tapping C1x steps its
 * count one forward (- → 1 → 2 → 3+ → -), tapping C2 toggles it; either tap returns to the
 * scorecard. Tapping the background goes back without changing anything.
 */
@Composable
internal fun PuttStatPickerScreen(
    c1xCount: Int,
    c2Made: Boolean,
    onC1xTap: () -> Unit,
    onC2Tap: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() }
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Text(
            "Putting",
            style = MaterialTheme.typography.title3,
            fontWeight = FontWeight.Bold,
            color = HoleNumberColor,
            textAlign = TextAlign.Center
        )
        PuttStatChip(
            label = "C1x",
            detail = when {
                c1xCount == 0 -> "Missed C1 putt"
                c1xCount >= 3 -> "Missed · 3+"
                else -> "Missed · $c1xCount"
            },
            active = c1xCount > 0,
            activeColor = C1xActiveColor,
            onClick = {
                onC1xTap()
                onDismiss()
            }
        )
        PuttStatChip(
            label = "C2",
            detail = if (c2Made) "Made ✓" else "Made C2 putt",
            active = c2Made,
            activeColor = C2ActiveColor,
            onClick = {
                onC2Tap()
                onDismiss()
            }
        )
    }
}

@Composable
private fun PuttStatChip(
    label: String,
    detail: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val color = if (active) activeColor else StatUnsetColor
    Chip(
        onClick = onClick,
        label = {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
        },
        secondaryLabel = {
            Text(detail, fontSize = 12.sp, color = if (active) color else ContentWhite, maxLines = 1)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ChipDefaults.chipColors(
            backgroundColor = WearButtonBackground,
            contentColor = ContentWhite
        )
    )
}
