package com.scorigami.wear.ui

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
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.scorigami.shared.sync.PlayerState
import com.scorigami.shared.sync.RoundState

private val HoleNumberColor = Color(0xFFFFD60A)

@Composable
fun WearScorecardScreen(
    roundState: RoundState,
    currentHole: Int,
    onPrevHole: () -> Unit,
    onNextHole: () -> Unit,
    onEndRound: () -> Unit,
    onScoreChange: (playerId: Long, throws: Int) -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
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
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp, start = 8.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            item {
                Text(
                    "Hole $currentHole / ${roundState.totalHoles}",
                    style = MaterialTheme.typography.title1,
                    fontWeight = FontWeight.ExtraBold,
                    color = HoleNumberColor,
                    textAlign = TextAlign.Center
                )
            }

            items(roundState.players) { player ->
                val throwsThisHole = player.holeScores[currentHole] ?: 0
                val holePar = roundState.holePars[currentHole] ?: 3
                WearPlayerRow(
                    player = player,
                    currentThrows = throwsThisHole,
                    onDecrement = {
                        val next = if (throwsThisHole == 0) maxOf(1, holePar - 1) else throwsThisHole - 1
                        onScoreChange(player.playerId, next)
                    },
                    onIncrement = {
                        val next = if (throwsThisHole == 0) holePar else throwsThisHole + 1
                        onScoreChange(player.playerId, next)
                    }
                )
            }

            // End Round button — hidden for now, re-enable by uncommenting
            // item {
            //     Spacer(Modifier.height(4.dp))
            //     Chip(
            //         onClick = onEndRound,
            //         label = { Text("End Round", fontSize = 12.sp) },
            //         colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.error)
            //     )
            // }
        }
    }
}

@Composable
private fun WearPlayerRow(
    player: PlayerState,
    currentThrows: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 2-letter abbreviation
            Text(
                text = player.name.take(2).uppercase(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                modifier = Modifier.width(40.dp),
                maxLines = 1
            )

            Spacer(Modifier.weight(1f))

            // − score +
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompactButton(
                    modifier = Modifier.size(36.dp),
                    onClick = onDecrement,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White,
                        disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.3f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = currentThrows.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.widthIn(min = 24.dp),
                    textAlign = TextAlign.Center
                )
                CompactButton(
                    modifier = Modifier.size(36.dp),
                    onClick = onIncrement,
                    enabled = currentThrows < 20,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White,
                        disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.3f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
