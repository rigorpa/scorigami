package com.scorigami.app.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.HandicapColor
import com.scorigami.app.ui.theme.NewRoundGradientEnd
import com.scorigami.app.ui.theme.NewRoundGradientStart
import com.scorigami.app.ui.theme.ScoreUnderParColor
import com.scorigami.app.viewmodel.RoundViewModel
import com.scorigami.shared.db.entity.HoleEntity
import com.scorigami.shared.db.entity.PlayerEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundReviewScreen(
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    viewModel: RoundViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(NewRoundGradientStart, NewRoundGradientEnd)))
            ) {
                TopAppBar(
                    title = { Text("Review Scores") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = ContentWhite
                    )
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text("Edit Scores", textAlign = TextAlign.Center)
                }
                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm & Finish", textAlign = TextAlign.Center)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                Text(
                    text = state.courseName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.holes.size} holes · Par ${state.holes.sumOf { it.par }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // One card per player
            items(state.players) { player ->
                PlayerReviewCard(
                    player = player,
                    holes = state.holes,
                    scores = state.scores,
                    obCounts = state.obCounts,
                    c1xCounts = state.c1xCounts,
                    handicap = state.handicaps[player.id] ?: 0
                )
            }

            // Final standings
            if (state.players.size > 1) {
                item {
                    StandingsCard(
                        players = state.players,
                        holes = state.holes,
                        scores = state.scores
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Finish Round?") },
            text = { Text("Are all scores correct? This round will be saved to history.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.completeRound()
                    showConfirmDialog = false
                    onConfirm()
                }) { Text("Finish") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Go Back") }
            }
        )
    }
}

@Composable
private fun PlayerReviewCard(
    player: PlayerEntity,
    holes: List<HoleEntity>,
    scores: Map<Pair<Long, Int>, Int>,
    obCounts: Map<Pair<Long, Int>, Int>,
    c1xCounts: Map<Pair<Long, Int>, Int>,
    handicap: Int = 0
) {
    val totalThrows = scores.entries.filter { it.key.first == player.id }.sumOf { it.value }
    val parSoFar = holes.filter { scores[Pair(player.id, it.number)] != null }.sumOf { it.par }
    val totalVsPar = totalThrows - parSoFar
    val totalOb = obCounts.entries.filter { it.key.first == player.id }.sumOf { it.value }
    val totalC1x = c1xCounts.entries.filter { it.key.first == player.id }.sumOf { it.value }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(player.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ContentWhite)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Handicap-adjusted total sits to the left of the normal vs-par score
                if (handicap != 0) {
                    Text(
                        text = "Hcp ${formatVsPar(totalVsPar + handicap)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HandicapColor,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = formatVsPar(totalVsPar),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = vsParColor(totalVsPar)
                )
            }
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
                        StatUnderlines(
                            hasOb = (obCounts[Pair(player.id, hole.number)] ?: 0) > 0,
                            hasC1x = (c1xCounts[Pair(player.id, hole.number)] ?: 0) > 0
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        StatTotalsLine(totalOb, totalC1x)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StandingsCard(
    players: List<PlayerEntity>,
    holes: List<HoleEntity>,
    scores: Map<Pair<Long, Int>, Int>
) {
    data class Standing(val name: String, val total: Int, val vsPar: Int)
    val standings = players.map { player ->
        val total = scores.entries.filter { it.key.first == player.id }.sumOf { it.value }
        val par = holes.filter { scores[Pair(player.id, it.number)] != null }.sumOf { it.par }
        Standing(player.name, total, total - par)
    }.sortedBy { it.vsPar }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text("Standings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ContentWhite)
        Spacer(Modifier.height(8.dp))
        standings.forEachIndexed { i, s ->
            val prefix = when (i) {
                0 -> "🥇"
                1 -> "🥈"
                2 -> "🥉"
                else -> "${i + 1}."
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$prefix  ${s.name}", style = MaterialTheme.typography.bodyMedium, color = ContentWhite)
                Text(formatVsPar(s.vsPar), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = vsParColor(s.vsPar))
            }
            if (i < standings.lastIndex) Spacer(Modifier.height(4.dp))
        }
    }
}
