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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            TopAppBar(title = { Text("Review Scores") })
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
                    scores = state.scores
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
    scores: Map<Pair<Long, Int>, Int>
) {
    val totalThrows = scores.entries.filter { it.key.first == player.id }.sumOf { it.value }
    val parSoFar = holes.filter { scores[Pair(player.id, it.number)] != null }.sumOf { it.par }
    val totalVsPar = totalThrows - parSoFar

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(player.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
            // Hole breakdown grid — hole number + vs-par only
            val chunked = holes.chunked(9)
            chunked.forEach { group ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    group.forEach { hole ->
                        val throws = scores[Pair(player.id, hole.number)]
                        val vsPar = throws?.minus(hole.par)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${hole.number}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = vsPar?.let { formatVsPar(it) } ?: "—",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = vsPar?.let { vsParColor(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
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

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Standings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            standings.forEachIndexed { i, s ->
                val prefix = when (i) {
                    0 -> "🥇"
                    1 -> "🥈"
                    2 -> "🥉"
                    else -> "${i + 1}."
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$prefix  ${s.name}", style = MaterialTheme.typography.bodyMedium)
                    Text(formatVsPar(s.vsPar), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = vsParColor(s.vsPar))
                }
                if (i < standings.lastIndex) Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun vsParColor(vsPar: Int) = when {
    vsPar < 0 -> MaterialTheme.colorScheme.primary
    vsPar == 0 -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.error
}

private fun formatVsPar(vsPar: Int) = when {
    vsPar < 0 -> "$vsPar"
    vsPar == 0 -> "E"
    else -> "+$vsPar"
}
