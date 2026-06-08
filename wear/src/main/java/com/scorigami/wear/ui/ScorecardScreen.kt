package com.scorigami.wear.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.scorigami.shared.sync.RoundState

private val HoleNumberColor = Color(0xFFFFD60A)

@Composable
fun WearScorecardScreen(
    roundState: RoundState,
    currentHole: Int,
    onPrevHole: () -> Unit,
    onNextHole: () -> Unit,
    onEndRound: () -> Unit,
    onScoreChange: (playerId: Long, throws: Int) -> Unit,
    onJumpToHole: (Int) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var showHoleJump by remember { mutableStateOf(false) }

    // Mirror the phone's honor-system sort so the watch doesn't have to wait for a
    // re-push: sort by each player's score on the previous hole, lowest first.
    val players = remember(roundState.players, currentHole) {
        if (currentHole <= 1) roundState.players
        else roundState.players.sortedWith(
            compareBy { it.holeScores[currentHole - 1] ?: Int.MAX_VALUE }
        )
    }

    var currentPlayerIndex by remember { mutableIntStateOf(0) }

    // Reset to first player whenever the hole changes
    LaunchedEffect(currentHole) { currentPlayerIndex = 0 }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val currentPlayer = players.getOrNull(currentPlayerIndex) ?: return
    val holePar = roundState.holePars[currentHole] ?: 3
    val isLastPlayer = currentPlayerIndex == players.lastIndex

    // Key on player ID (not index) so pendingScore resets correctly if the list reorders
    var pendingScore by remember(currentPlayer.playerId, currentHole) {
        mutableIntStateOf(currentPlayer.holeScores[currentHole] ?: 0)
    }

    fun commitAndAdvance() {
        onScoreChange(currentPlayer.playerId, pendingScore)
        if (isLastPlayer) {
            onNextHole()
        } else {
            currentPlayerIndex++
        }
    }

    Scaffold {
        if (showHoleJump) {
            val listState = rememberScalingLazyListState(initialCenterItemIndex = currentHole - 1)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragCancel = { totalDrag = 0f },
                            onDragEnd = {
                                if (totalDrag > 40.dp.toPx()) showHoleJump = false
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDrag += dragAmount
                            }
                        )
                    }
            ) {
                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(roundState.totalHoles) { index ->
                        val holeNum = index + 1
                        Chip(
                            onClick = {
                                onJumpToHole(holeNum)
                                showHoleJump = false
                            },
                            label = {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Hole $holeNum", fontSize = 14.sp, maxLines = 1)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(36.dp),
                            colors = if (holeNum == currentHole)
                                ChipDefaults.primaryChipColors()
                            else
                                ChipDefaults.secondaryChipColors()
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .pointerInput(currentHole, roundState.totalHoles) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragCancel = { totalDrag = 0f },
                            onDragEnd = {
                                val threshold = 40.dp.toPx()
                                when {
                                    totalDrag < -threshold && currentHole < roundState.totalHoles -> onNextHole()
                                    totalDrag > threshold && currentHole > 1 -> onPrevHole()
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDrag += dragAmount
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // Hole indicator — tappable to open hole-jump picker
                    Text(
                        "Hole $currentHole / ${roundState.totalHoles}",
                        style = MaterialTheme.typography.title2,
                        fontWeight = FontWeight.ExtraBold,
                        color = HoleNumberColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { showHoleJump = true }
                    )

                    // Current player name
                    Text(
                        currentPlayer.name,
                        style = MaterialTheme.typography.title1,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    // − score + controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactButton(
                            modifier = Modifier.size(36.dp),
                            onClick = {
                                pendingScore = if (pendingScore == 0) maxOf(1, holePar - 1) else pendingScore - 1
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.primary,
                                contentColor = MaterialTheme.colors.onPrimary,
                                disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.3f),
                                disabledContentColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.3f)
                            )
                        ) {
                            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        val scoreColor = when {
                            pendingScore == 0 -> Color.White
                            pendingScore < holePar -> Color(0xFF81C784)
                            pendingScore == holePar -> Color.White
                            else -> MaterialTheme.colors.error
                        }
                        Text(
                            text = pendingScore.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = scoreColor,
                            modifier = Modifier.widthIn(min = 28.dp),
                            textAlign = TextAlign.Center
                        )
                        CompactButton(
                            modifier = Modifier.size(36.dp),
                            onClick = {
                                pendingScore = if (pendingScore == 0) holePar else pendingScore + 1
                            },
                            enabled = pendingScore < 20,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.primary,
                                contentColor = MaterialTheme.colors.onPrimary,
                                disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.3f),
                                disabledContentColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.3f)
                            )
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Enter / Next Hole button
                    Chip(
                        onClick = ::commitAndAdvance,
                        label = {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    if (isLastPlayer) "Next Hole ▶" else "Enter",
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(if (isLastPlayer) 0.72f else 0.52f)
                            .height(36.dp),
                        colors = ChipDefaults.primaryChipColors()
                    )

                    // End Round button — hidden for now, re-enable by uncommenting
                    // Chip(
                    //     onClick = onEndRound,
                    //     label = { Text("End Round", fontSize = 12.sp) },
                    //     modifier = Modifier.fillMaxWidth(0.65f),
                    //     colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.error)
                    // )
                }
            }
        }
    }
}
