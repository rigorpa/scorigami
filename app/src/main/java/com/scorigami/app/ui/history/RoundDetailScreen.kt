package com.scorigami.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scorigami.app.viewmodel.HistoryViewModel
import com.scorigami.shared.db.entity.HoleEntity
import com.scorigami.shared.db.entity.PlayerEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundDetailScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(detail.courseName.ifEmpty { "Round Detail" })
                        if (detail.date.isNotEmpty()) {
                            Text(
                                text = "Played on ${detail.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (detail.players.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            item {
                Text(
                    "${detail.holes.size} holes · Par ${detail.holes.sumOf { it.par }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(detail.players) { player ->
                DetailPlayerCard(player = player, holes = detail.holes, scores = detail.scores)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun DetailPlayerCard(
    player: PlayerEntity,
    holes: List<HoleEntity>,
    scores: Map<Pair<Long, Int>, Int>
) {
    val totalThrows = scores.entries.filter { it.key.first == player.id }.sumOf { it.value }
    val totalPar = holes.filter { scores[Pair(player.id, it.number)] != null }.sumOf { it.par }
    val vsPar = totalThrows - totalPar

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(player.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "$totalThrows  (${formatVsPar(vsPar)})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            holes.chunked(9).forEach { group ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    group.forEach { hole ->
                        val throws = scores[Pair(player.id, hole.number)]
                        val diff = throws?.minus(hole.par)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${hole.number}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(throws?.toString() ?: "—", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = diff?.let { formatVsPar(it) } ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = diff?.let { vsParColor(it) } ?: MaterialTheme.colorScheme.onSurface,
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
private fun vsParColor(v: Int) = when {
    v < 0 -> MaterialTheme.colorScheme.primary
    v == 0 -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.error
}

private fun formatVsPar(v: Int) = when {
    v < 0 -> "$v"
    v == 0 -> "E"
    else -> "+$v"
}
