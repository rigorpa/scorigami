package com.scorigami.app.ui.history

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.GradientContentWhite
import com.scorigami.app.ui.theme.ContentLightGrey
import com.scorigami.app.ui.round.formatVsPar
import com.scorigami.app.ui.theme.HistoryGradientEnd
import com.scorigami.app.ui.theme.HistoryGradientStart
import com.scorigami.app.ui.round.StatTotalsLine
import com.scorigami.app.ui.round.StatUnderlines
import com.scorigami.app.ui.theme.ScoreUnderParColor
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
    var showShareDialog by remember { mutableStateOf(false) }

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
                                    color = GradientContentWhite.copy(alpha = 0.75f)
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
                            onClick = { showShareDialog = true },
                            enabled = detail.players.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share round")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = GradientContentWhite,
                        navigationIconContentColor = GradientContentWhite,
                        actionIconContentColor = GradientContentWhite
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
                DetailPlayerCard(
                    player = player,
                    holes = detail.holes,
                    scores = detail.scores,
                    obCounts = detail.obCounts,
                    c1xCounts = detail.c1xCounts
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showShareDialog) {
        ShareRoundDialog(
            detail = detail,
            onDismiss = { showShareDialog = false }
        )
    }
}

@Composable
private fun DetailPlayerCard(
    player: PlayerEntity,
    holes: List<HoleEntity>,
    scores: Map<Pair<Long, Int>, Int>,
    obCounts: Map<Pair<Long, Int>, Int>,
    c1xCounts: Map<Pair<Long, Int>, Int>
) {
    val totalThrows = scores.entries.filter { it.key.first == player.id }.sumOf { it.value }
    val totalPar = holes.filter { scores[Pair(player.id, it.number)] != null }.sumOf { it.par }
    val vsPar = totalThrows - totalPar
    val totalOb = obCounts.entries.filter { it.key.first == player.id }.sumOf { it.value }
    val totalC1x = c1xCounts.entries.filter { it.key.first == player.id }.sumOf { it.value }

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
                        // Raw throw count colored by par relation — matches FullScorecardSheet
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
                            Text("${hole.number}", style = MaterialTheme.typography.labelLarge, color = ContentLightGrey)
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
        }
    }
}

