package com.scorigami.app.ui.history

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.ContentLightGrey
import com.scorigami.app.ui.theme.HistoryGradientEnd
import com.scorigami.app.ui.theme.HistoryGradientStart
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
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(HistoryGradientStart, HistoryGradientEnd)))
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(detail.courseName.ifEmpty { "Round Detail" })
                            if (detail.date.isNotEmpty()) {
                                Text(
                                    text = "Played on ${detail.date}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ContentWhite.copy(alpha = 0.75f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val text = buildShareText(
                                    courseName = detail.courseName,
                                    date = detail.date,
                                    holes = detail.holes,
                                    players = detail.players,
                                    scores = detail.scores
                                )
                                shareRound(context, text)
                            },
                            enabled = detail.players.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share round")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = ContentWhite,
                        navigationIconContentColor = ContentWhite,
                        actionIconContentColor = ContentWhite
                    )
                )
            }
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
                    color = ContentLightGrey
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
                            Text("${hole.number}", style = MaterialTheme.typography.labelLarge, color = ContentLightGrey)
                            Text(
                                text = diff?.let { formatVsPar(it) } ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
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

private fun buildShareText(
    courseName: String,
    date: String,
    holes: List<HoleEntity>,
    players: List<PlayerEntity>,
    scores: Map<Pair<Long, Int>, Int>
): String = buildString {
    appendLine("Scorigami | $courseName")
    if (date.isNotEmpty()) appendLine("Played on $date")
    appendLine("${holes.size} holes · Par ${holes.sumOf { it.par }}")
    players.forEach { player ->
        appendLine()
        val totalThrows = holes.sumOf { scores[Pair(player.id, it.number)] ?: 0 }
        val totalPar = holes.filter { scores[Pair(player.id, it.number)] != null }.sumOf { it.par }
        val vsPar = totalThrows - totalPar
        appendLine("${player.name} — $totalThrows (${formatVsPar(vsPar)})")
        holes.chunked(9).forEach { group ->
            val holeNums = group.joinToString("  ") { "%2d".format(it.number) }
            val holeScores = group.joinToString("  ") { hole ->
                val throws = scores[Pair(player.id, hole.number)]
                val diff = throws?.minus(hole.par)
                "%2s".format(diff?.let { formatVsPar(it) } ?: "—")
            }
            appendLine(holeNums)
            appendLine(holeScores)
        }
    }
}

private fun shareRound(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share round"))
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
