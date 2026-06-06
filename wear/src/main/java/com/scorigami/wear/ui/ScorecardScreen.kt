package com.scorigami.wear.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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
                .focusable(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 24.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        roundState.courseName,
                        fontFamily = FontFamily.Cursive,
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Hole $currentHole / ${roundState.totalHoles}",
                        style = MaterialTheme.typography.title1,
                        fontWeight = FontWeight.ExtraBold,
                        color = HoleNumberColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactButton(onClick = onPrevHole, enabled = currentHole > 1) {
                        Text("◀", fontSize = 12.sp)
                    }
                    CompactButton(onClick = onNextHole, enabled = currentHole < roundState.totalHoles) {
                        Text("▶", fontSize = 12.sp)
                    }
                }
            }

            items(roundState.players) { player ->
                val throwsThisHole = player.holeScores[currentHole] ?: 0
                val holePar = roundState.holePars[currentHole] ?: 3
                WearPlayerRow(
                    player = player,
                    currentThrows = throwsThisHole,
                    onDecrement = {
                        if (throwsThisHole > 0) onScoreChange(player.playerId, throwsThisHole - 1)
                    },
                    onIncrement = {
                        val next = if (throwsThisHole == 0) maxOf(1, holePar - 1) else throwsThisHole + 1
                        onScoreChange(player.playerId, next)
                    }
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                Chip(
                    onClick = onEndRound,
                    label = { Text("End Round", fontSize = 12.sp) },
                    colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.error)
                )
            }
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
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 3-letter abbreviation — width sized for 3 uppercase chars at 24sp
            Text(
                text = player.name.take(3).uppercase(),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.width(56.dp),
                maxLines = 1
            )

            Spacer(Modifier.weight(1f))

            // − score + (transparent background, no circle)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompactButton(
                    modifier = Modifier.size(44.dp),
                    onClick = onDecrement,
                    enabled = currentThrows > 0,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White,
                        disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.3f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = currentThrows.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.widthIn(min = 28.dp),
                    textAlign = TextAlign.Center
                )
                CompactButton(
                    modifier = Modifier.size(44.dp),
                    onClick = onIncrement,
                    enabled = currentThrows < 20,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White,
                        disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.3f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
