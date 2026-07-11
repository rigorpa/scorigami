package com.scorigami.app.ui.round

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scorigami.app.viewmodel.RoundViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorecardScreen(
    onEndRound: () -> Unit,
    onBack: () -> Unit,
    onCancelRound: () -> Unit,
    viewModel: RoundViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allPlayers by viewModel.allPlayers.collectAsStateWithLifecycle()

    if (!state.isActive) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val incompleteHoles = remember(state.scores, state.players, state.holes) {
        state.holes
            .filter { hole -> state.players.any { player -> (state.scores[Pair(player.id, hole.number)] ?: 0) == 0 } }
            .map { it.number }
            .toSet()
    }
    val hasMissingScores = incompleteHoles.isNotEmpty()
    var showCancelDialog by remember { mutableStateOf(false) }
    var showPlayersSheet by remember { mutableStateOf(false) }
    var showMissingScoresDialog by remember { mutableStateOf(false) }
    var showScorecardSheet by remember { mutableStateOf(false) }
    var scoresVisible by remember { mutableStateOf(true) }

    val holeScale = remember { Animatable(1f) }
    LaunchedEffect(state.currentHole) {
        holeScale.snapTo(0.82f)
        holeScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
        )
    }

    if (showMissingScoresDialog) {
        AlertDialog(
            onDismissRequest = { showMissingScoresDialog = false },
            title = { Text("Missing Scores") },
            text = { Text("There are some missing scores. Continue to end the round? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showMissingScoresDialog = false
                    onEndRound()
                }) { Text("Continue", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showMissingScoresDialog = false }) { Text("Go Back") }
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Round?") },
            text = { Text("All scores will be discarded. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancelRound()
                    onCancelRound()
                }) { Text("Discard Round", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep Playing") }
            }
        )
    }

    if (showScorecardSheet) {
        ModalBottomSheet(
            onDismissRequest = { showScorecardSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            FullScorecardSheet(
                players = state.players,
                holes = state.holes,
                scores = state.scores,
                obCounts = state.obCounts,
                c1xCounts = state.c1xCounts
            )
        }
    }

    if (showPlayersSheet) {
        // Default container (surfaceContainerLow grey) — matches the hole-notes sheet
        ModalBottomSheet(onDismissRequest = { showPlayersSheet = false }) {
            AddRemovePlayersSheet(
                currentPlayers = state.players,
                allPlayers = allPlayers,
                onAddPlayer = { viewModel.addPlayerToRound(it) },
                onRemovePlayer = { viewModel.removePlayerFromRound(it) }
            )
        }
    }

    Scaffold(
        topBar = {
            ScorecardTopBar(
                courseName = state.courseName,
                onViewScorecard = { showScorecardSheet = true },
                onEndRound = {
                    if (hasMissingScores) showMissingScoresDialog = true else onEndRound()
                },
                onAddRemovePlayers = { showPlayersSheet = true },
                onCancelRound = { showCancelDialog = true }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(state.currentHole, state.holes.size) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f },
                        onDragEnd = {
                            val threshold = 80.dp.toPx()
                            when {
                                totalDrag < -threshold && state.currentHole < state.holes.size ->
                                    viewModel.navigateToHole(state.currentHole + 1)
                                totalDrag > threshold && state.currentHole > 1 ->
                                    viewModel.navigateToHole(state.currentHole - 1)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDrag += dragAmount
                        }
                    )
                }
        ) {
            AnimatedContent(
                targetState = state.currentHole,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val toNext = targetState > initialState
                    slideInHorizontally(
                        animationSpec = tween(250),
                        initialOffsetX = { if (toNext) it else -it }
                    ) + fadeIn(tween(200)) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(250),
                        targetOffsetX = { if (toNext) -it else it }
                    ) + fadeOut(tween(200))
                },
                label = "hole_slide"
            ) { hole ->
                val holeEntity = state.holes.find { it.number == hole }

                Column(modifier = Modifier.fillMaxSize()) {
                    HoleInfoCard(
                        hole = hole,
                        holeEntity = holeEntity,
                        totalHoles = state.holes.size,
                        holeScale = holeScale.value,
                        holes = state.holes,
                        incompleteHoles = incompleteHoles,
                        onNavigateToHole = { viewModel.navigateToHole(it) },
                        onHoleSelected = { viewModel.navigateToHole(it) },
                        onAddRemovePlayers = { showPlayersSheet = true },
                        scoresVisible = scoresVisible,
                        onToggleScoresVisible = { scoresVisible = !scoresVisible }
                    )
                    LazyColumn(
                        // Extra top padding separates the player cards from the hole card above
                        contentPadding = PaddingValues(top = 20.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(state.players, key = { it.id }) { player ->
                            PlayerScoreCard(
                                player = player,
                                currentHole = hole,
                                scores = state.scores,
                                obCounts = state.obCounts,
                                c1xCounts = state.c1xCounts,
                                holes = state.holes,
                                scoresVisible = scoresVisible,
                                onScoreChange = { throws ->
                                    viewModel.updateScore(player.id, hole, throws)
                                },
                                onObChange = { count ->
                                    viewModel.setOb(player.id, hole, count)
                                },
                                onC1xChange = { count ->
                                    viewModel.setC1x(player.id, hole, count)
                                }
                            )
                        }
                    }
                }
            }

        }
    }
}
