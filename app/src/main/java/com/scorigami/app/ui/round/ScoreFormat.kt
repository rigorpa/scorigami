package com.scorigami.app.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.scorigami.app.ui.theme.C1xActiveColor
import com.scorigami.app.ui.theme.ContentLightGrey
import com.scorigami.app.ui.theme.StatActiveColor

internal fun formatVsPar(vsPar: Int): String = when {
    vsPar < 0 -> "$vsPar"
    vsPar == 0 -> "E"
    else -> "+$vsPar"
}

@Composable
internal fun vsParColor(vsPar: Int): Color = when {
    vsPar < 0 -> MaterialTheme.colorScheme.primary
    vsPar == 0 -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.error
}

/**
 * Per-hole OB / C1x indicator under a hole's throw count in the scorecard grids:
 * a short red bar when any OB was entered on the hole, an orange bar for C1x.
 * One bar per stat regardless of count. Renders nothing when neither is set.
 */
@Composable
internal fun StatUnderlines(hasOb: Boolean, hasC1x: Boolean) {
    if (!hasOb && !hasC1x) return
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(top = 2.dp)
    ) {
        if (hasOb) {
            Box(Modifier.size(width = 14.dp, height = 2.dp).background(StatActiveColor, RoundedCornerShape(1.dp)))
        }
        if (hasC1x) {
            Box(Modifier.size(width = 14.dp, height = 2.dp).background(C1xActiveColor, RoundedCornerShape(1.dp)))
        }
    }
}

/**
 * Round-total OB / C1x line ("N OB  ·  M C1x") shown under a player's scorecard block
 * in Review, Full-scorecard sheet, and History detail. Each part only when > 0;
 * renders nothing when both are 0. OB red / C1x orange, matching the entry counters.
 */
@Composable
internal fun StatTotalsLine(
    totalOb: Int,
    totalC1x: Int,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    if (totalOb <= 0 && totalC1x <= 0) return
    Text(
        text = buildAnnotatedString {
            if (totalOb > 0) {
                withStyle(SpanStyle(color = StatActiveColor)) { append("$totalOb OB") }
            }
            if (totalOb > 0 && totalC1x > 0) {
                withStyle(SpanStyle(color = ContentLightGrey)) { append("  ·  ") }
            }
            if (totalC1x > 0) {
                withStyle(SpanStyle(color = C1xActiveColor)) { append("$totalC1x C1x") }
            }
        },
        style = style,
        fontWeight = FontWeight.Bold
    )
}
