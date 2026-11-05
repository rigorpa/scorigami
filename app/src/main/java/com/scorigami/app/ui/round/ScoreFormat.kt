package com.scorigami.app.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.scorigami.app.ui.theme.C2ActiveColor
import com.scorigami.app.ui.theme.ContentLightGrey
import com.scorigami.app.ui.theme.ScoreUnderParColor
import com.scorigami.app.ui.theme.StatActiveColor

internal fun formatVsPar(vsPar: Int): String = when {
    vsPar < 0 -> "$vsPar"
    vsPar == 0 -> "E"
    else -> "+$vsPar"
}

@Composable
internal fun vsParColor(vsPar: Int): Color = when {
    // Green rather than colorScheme.primary: primary is a warm neutral in this
    // theme, and under-par must stay visually distinct (matches PlayerScoreCard
    // and the per-hole grids, which already use ScoreUnderParColor)
    vsPar < 0 -> ScoreUnderParColor
    vsPar == 0 -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.error
}

/**
 * Per-hole OB / C1x indicator under a hole's throw count in the scorecard grids:
 * a short red bar when any OB was entered on the hole, an orange bar for C1x, a green bar for a made C2.
 * One bar per stat regardless of count. Renders nothing when neither is set.
 */
@Composable
internal fun StatUnderlines(hasOb: Boolean, hasC1x: Boolean, hasC2: Boolean = false) {
    if (!hasOb && !hasC1x && !hasC2) return
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
        if (hasC2) {
            Box(Modifier.size(width = 14.dp, height = 2.dp).background(C2ActiveColor, RoundedCornerShape(1.dp)))
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
    totalC2: Int = 0,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    if (totalOb <= 0 && totalC1x <= 0 && totalC2 <= 0) return
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
            if ((totalOb > 0 || totalC1x > 0) && totalC2 > 0) {
                withStyle(SpanStyle(color = ContentLightGrey)) { append("  ·  ") }
            }
            if (totalC2 > 0) {
                withStyle(SpanStyle(color = C2ActiveColor)) { append("$totalC2 C2") }
            }
        },
        style = style,
        fontWeight = FontWeight.Bold
    )
}

/**
 * C1 make rate (0–100) for a player: every scored hole counts as a C1 make unless C1x was
 * marked on it; a hole with a made C2 and no C1x wasn't a C1 attempt and is left out.
 * Null when there are no attempts yet.
 */
internal fun c1MakePercent(
    playerId: Long,
    holes: List<com.scorigami.shared.db.entity.HoleEntity>,
    scores: Map<Pair<Long, Int>, Int>,
    c1xCounts: Map<Pair<Long, Int>, Int>,
    c2Counts: Map<Pair<Long, Int>, Int>
): Int? {
    val scored = holes.filter { scores[Pair(playerId, it.number)] != null }
    val misses = scored.count { (c1xCounts[Pair(playerId, it.number)] ?: 0) > 0 }
    val attempts = scored.count {
        (c1xCounts[Pair(playerId, it.number)] ?: 0) > 0 || (c2Counts[Pair(playerId, it.number)] ?: 0) == 0
    }
    return if (attempts > 0) Math.round((attempts - misses) * 100f / attempts) else null
}

/** OB / C1x / C2 totals on the left, "C1 NN%" make rate right-aligned on the same line. */
@Composable
internal fun StatSummaryRow(
    totalOb: Int,
    totalC1x: Int,
    totalC2: Int,
    c1Pct: Int?,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatTotalsLine(totalOb, totalC1x, totalC2, style)
        if (c1Pct != null) {
            Text(
                text = "C1 $c1Pct%",
                style = style,
                fontWeight = FontWeight.Bold,
                color = ContentLightGrey
            )
        }
    }
}
