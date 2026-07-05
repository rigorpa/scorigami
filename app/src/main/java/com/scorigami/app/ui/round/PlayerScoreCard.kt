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
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.ObColor
import com.scorigami.app.ui.theme.ScaleGrey2
import com.scorigami.app.ui.theme.ScoreUnderParColor
import com.scorigami.shared.db.entity.HoleEntity
import com.scorigami.shared.db.entity.PlayerEntity

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlayerScoreCard(
    player: PlayerEntity,
    currentHole: Int,
    scores: Map<Pair<Long, Int>, Int>,
    obCounts: Map<Pair<Long, Int>, Int>,
    holes: List<HoleEntity>,
    scoresVisible: Boolean,
    onScoreChange: (Int) -> Unit,
    onObChange: (Int) -> Unit
) {
    val throwsThisHole = scores[Pair(player.id, currentHole)] ?: 0
    val obThisHole = obCounts[Pair(player.id, currentHole)] ?: 0
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
        colors = CardDefaults.cardColors(containerColor = ScaleGrey2)
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

            // OB counter for this hole — tap to add one, long-press to remove one
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cycles - → 1 → 2 → 3+ → back to -; long-press steps one back
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = { onObChange(if (obThisHole >= 3) 0 else obThisHole + 1) },
                            onLongClick = { if (obThisHole > 0) onObChange(obThisHole - 1) },
                            onClickLabel = "Add OB for ${player.name}",
                            onLongClickLabel = "Remove OB for ${player.name}"
                        )
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                        .widthIn(min = 44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            obThisHole == 0 -> "- OB"
                            obThisHole >= 3 -> "3+ OB"
                            else -> "$obThisHole OB"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ObColor,
                        maxLines = 1
                    )
                }

                // − score + controls
                IconButton(
                    onClick = {
                        val next = if (throwsThisHole == 0) maxOf(1, holePar - 1) else throwsThisHole - 1
                        onScoreChange(next)
                    }
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
                IconButton(
                    onClick = {
                        val next = if (throwsThisHole == 0) holePar else throwsThisHole + 1
                        onScoreChange(next)
                    }
                ) {
                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ContentWhite)
                }
            }
        }
    }
}
