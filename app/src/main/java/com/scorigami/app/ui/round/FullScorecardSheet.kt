package com.scorigami.app.ui.round

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.ObColor
import com.scorigami.app.ui.theme.ScoreUnderParColor
import com.scorigami.shared.db.entity.HoleEntity
import com.scorigami.shared.db.entity.PlayerEntity

@Composable
internal fun FullScorecardSheet(
    players: List<PlayerEntity>,
    holes: List<HoleEntity>,
    scores: Map<Pair<Long, Int>, Int>,
    obCounts: Map<Pair<Long, Int>, Int>
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 32.dp)
    ) {
        items(players, key = { it.id }) { player ->
            val playerScores = scores.entries.filter { it.key.first == player.id }
            val totalThrows = playerScores.sumOf { it.value }
            val parSoFar = holes.filter { scores[Pair(player.id, it.number)] != null }.sumOf { it.par }
            val totalVsPar = totalThrows - parSoFar
            val totalOb = obCounts.entries.filter { it.key.first == player.id }.sumOf { it.value }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(player.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ContentWhite)
                    Text(
                        text = formatVsPar(totalVsPar),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = vsParColor(totalVsPar)
                    )
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                holes.chunked(9).forEach { group ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        group.forEach { hole ->
                            val throws = scores[Pair(player.id, hole.number)]
                            val scoreColor = when {
                                throws == null -> ContentWhite
                                throws < hole.par -> ScoreUnderParColor
                                throws == hole.par -> ContentWhite
                                else -> MaterialTheme.colorScheme.error
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${hole.number}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ContentWhite.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = throws?.toString() ?: "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = scoreColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                if (totalOb > 0) {
                    Text(
                        text = "$totalOb OB",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ObColor
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
