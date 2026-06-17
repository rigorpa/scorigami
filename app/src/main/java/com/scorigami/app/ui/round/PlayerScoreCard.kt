package com.scorigami.app.ui.round

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.ScaleGrey2
import com.scorigami.app.ui.theme.ScreenBackground
import com.scorigami.app.ui.theme.ScoreUnderParColor
import com.scorigami.shared.db.entity.HoleEntity
import com.scorigami.shared.db.entity.PlayerEntity

@Composable
internal fun PlayerScoreCard(
    player: PlayerEntity,
    currentHole: Int,
    scores: Map<Pair<Long, Int>, Int>,
    holes: List<HoleEntity>,
    scoresVisible: Boolean,
    onScoreChange: (Int) -> Unit
) {
    val throwsThisHole = scores[Pair(player.id, currentHole)] ?: 0
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = player.name,
                fontSize = 36.sp,
                fontWeight = FontWeight.Normal,
                color = ContentWhite,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                maxLines = 1
            )

            // Round vs-par — center column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Round",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = ScreenBackground
                )
                Text(
                    text = if (scoresVisible) formatVsPar(totalVsPar) else "•••",
                    style = MaterialTheme.typography.titleMedium,
                    color = ContentWhite,
                    fontWeight = FontWeight.Normal
                )
            }

            // − score + controls
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    fontSize = 32.sp,
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
