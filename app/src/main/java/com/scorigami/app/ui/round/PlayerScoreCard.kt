package com.scorigami.app.ui.round

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.scorigami.app.ui.theme.C1xActiveColor
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.ScoreButtonBackground
import com.scorigami.app.ui.theme.ScreenBackground
import com.scorigami.app.ui.theme.ScoreUnderParColor
import com.scorigami.app.ui.theme.StatActiveColor
import com.scorigami.app.ui.theme.StatUnsetColor
import com.scorigami.shared.db.entity.HoleEntity
import com.scorigami.shared.db.entity.PlayerEntity

@Composable
internal fun PlayerScoreCard(
    player: PlayerEntity,
    currentHole: Int,
    scores: Map<Pair<Long, Int>, Int>,
    obCounts: Map<Pair<Long, Int>, Int>,
    c1xCounts: Map<Pair<Long, Int>, Int>,
    holes: List<HoleEntity>,
    scoresVisible: Boolean,
    onScoreChange: (Int) -> Unit,
    onObChange: (Int) -> Unit,
    onC1xChange: (Int) -> Unit
) {
    val throwsThisHole = scores[Pair(player.id, currentHole)] ?: 0
    val obThisHole = obCounts[Pair(player.id, currentHole)] ?: 0
    val c1xThisHole = c1xCounts[Pair(player.id, currentHole)] ?: 0
    val playerScores = scores.entries.filter { it.key.first == player.id }
    val totalThrows = playerScores.sumOf { it.value }
    val parSoFar = holes
        .filter { hole -> playerScores.any { it.key.second == hole.number } }
        .sumOf { it.par }
    val totalVsPar = totalThrows - parSoFar
    val holePar = holes.find { it.number == currentHole }?.par ?: 3
    val scoreColor = when {
        throwsThisHole == 0 -> ContentWhite
        throwsThisHole < holePar -> ScoreUnderParColor
        throwsThisHole == holePar -> ContentWhite
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ScreenBackground)
    ) {
        // name and round score
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = player.name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ContentWhite,
                    maxLines = 1
                )
                Text(
                    text = if (scoresVisible) formatVsPar(totalVsPar) else "•••",
                    style = MaterialTheme.typography.titleSmall,
                    color = ContentWhite,
                    fontWeight = FontWeight.Light
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Per-hole stat counters — cycle - → 1 → 2 → 3+ → back to -; long-press steps one back
                StatCycleButton(
                    label = "OB",
                    count = obThisHole,
                    playerName = player.name,
                    activeColor = StatActiveColor,
                    onCountChange = onObChange
                )
                StatCycleButton(
                    label = "C1x",
                    count = c1xThisHole,
                    playerName = player.name,
                    activeColor = C1xActiveColor,
                    onCountChange = onC1xChange
                )

                // Gap pulls the stat counters toward the card's center, away from the − control
                Spacer(Modifier.width(16.dp))

                // − score + controls — filled dark-grey circles matching the wear app
                FilledIconButton(
                    onClick = {
                        val next = if (throwsThisHole == 0) maxOf(1, holePar - 1) else throwsThisHole - 1
                        onScoreChange(next)
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ScoreButtonBackground,
                        contentColor = ContentWhite
                    )
                ) {
                    Text("−", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ContentWhite)
                }
                Text(
                    text = if (throwsThisHole == 0) "—" else "$throwsThisHole",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = scoreColor,
                    modifier = Modifier.widthIn(min = 40.dp),
                    textAlign = TextAlign.Center
                )
                FilledIconButton(
                    onClick = {
                        val next = if (throwsThisHole == 0) holePar else throwsThisHole + 1
                        onScoreChange(next)
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ScoreButtonBackground,
                        contentColor = ContentWhite
                    )
                ) {
                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ContentWhite)
                }
            }
        }
    }
}

/**
 * Per-hole stat counter: tap cycles - → 1 → 2 → 3+ → -, long-press steps back.
 * White while unset; [activeColor] (OB red / C1x orange) once a count is entered.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StatCycleButton(
    label: String,
    count: Int,
    playerName: String,
    activeColor: Color,
    onCountChange: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { onCountChange(if (count >= 3) 0 else count + 1) },
                onLongClick = { if (count > 0) onCountChange(count - 1) },
                onClickLabel = "Add $label for $playerName",
                onLongClickLabel = "Remove $label for $playerName"
            )
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .widthIn(min = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                count == 0 -> label
                count >= 3 -> "3+ $label"
                else -> "$count $label"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (count > 0) activeColor else StatUnsetColor,
            maxLines = 1
        )
    }
}
