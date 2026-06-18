package com.scorigami.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Dialog
import com.scorigami.wear.ui.theme.HoleNumberColor
import com.scorigami.wear.ui.theme.HoleJumpSelectedColor
import com.scorigami.wear.ui.theme.WearButtonBackground
import com.scorigami.wear.ui.theme.IncompleteHoleDotColor
import com.scorigami.wear.ui.theme.ScoreUnderParColor
import com.scorigami.shared.sync.RoundState

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
    var showTeeOrder by remember { mutableStateOf(false) }
    var showEndRoundPrompt by remember { mutableStateOf(false) }

    // Mirror the phone's honor-system sort: primary key is score on the previous hole,
    // ties broken by the hole before that, cascading back to hole 1, then DB order.
    val players = remember(roundState.players, currentHole) {
        if (currentHole <= 1) roundState.players
        else roundState.players.sortedWith(
            Comparator { a, b ->
                for (h in currentHole - 1 downTo 1) {
                    val sa = a.holeScores[h] ?: Int.MAX_VALUE
                    val sb = b.holeScores[h] ?: Int.MAX_VALUE
                    if (sa != sb) return@Comparator sa - sb
                }
                0
            }
        )
    }

    var currentPlayerIndex by remember { mutableIntStateOf(0) }

    // Reset to first player whenever the hole changes
    LaunchedEffect(currentHole) { currentPlayerIndex = 0 }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val currentPlayer = players.getOrNull(currentPlayerIndex) ?: return
    val holePar = roundState.holePars[currentHole] ?: 3
    val isLastPlayer = currentPlayerIndex == players.lastIndex

    // Key on player ID (not index) so pendingScore resets correctly if the list reorders.
    // Also key on knownScore so an externally pushed score (from phone) is reflected immediately.
    val knownScore = currentPlayer.holeScores[currentHole] ?: 0
    var pendingScore by remember(currentPlayer.playerId, currentHole, knownScore) {
        mutableIntStateOf(knownScore)
    }

    fun commitAndAdvance() {
        if (pendingScore > 0) onScoreChange(currentPlayer.playerId, pendingScore)
        if (isLastPlayer) {
            if (currentHole >= roundState.totalHoles) {
                showEndRoundPrompt = true
            } else {
                onNextHole()
            }
        } else {
            currentPlayerIndex++
        }
    }

    val incompleteHoles = remember(roundState.players) {
        (1..roundState.totalHoles).filter { holeNum ->
            roundState.players.any { player -> (player.holeScores[holeNum] ?: 0) == 0 }
        }.toSet()
    }

    Scaffold {
        if (showHoleJump) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                (1..roundState.totalHoles).chunked(3).forEach { rowHoles ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowHoles.forEach { holeNum ->
                            val isCurrent = holeNum == currentHole
                            val incomplete = holeNum in incompleteHoles
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(
                                        if (isCurrent) HoleJumpSelectedColor else WearButtonBackground,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onJumpToHole(holeNum)
                                        showHoleJump = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$holeNum",
                                    fontSize = 14.sp,
                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = Color.White
                                )
                                if (incomplete) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 3.dp, end = 3.dp)
                                            .size(5.dp)
                                            .background(IncompleteHoleDotColor, CircleShape)
                                    )
                                }
                            }
                        }
                        repeat(3 - rowHoles.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // Hole indicator — tappable to open hole-jump picker
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)) {
                                append("$currentHole")
                            }
                            withStyle(SpanStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)) {
                                append(" / ${roundState.totalHoles}")
                            }
                        },
                        color = HoleNumberColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { showHoleJump = true }
                    )

                    // Current player name — tap to show tee order
                    Text(
                        currentPlayer.name,
                        style = MaterialTheme.typography.title1,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.clickable { showTeeOrder = true }
                    )

                    // − score + controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        CompactButton(
                            modifier = Modifier.size(48.dp),
                            onClick = {
                                pendingScore = if (pendingScore == 0) maxOf(1, holePar - 1) else pendingScore - 1
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = WearButtonBackground,
                                contentColor = Color.White,
                                disabledBackgroundColor = WearButtonBackground.copy(alpha = 0.4f),
                                disabledContentColor = Color.White.copy(alpha = 0.3f)
                            )
                        ) {
                            Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        val scoreColor = when {
                            pendingScore == 0 -> Color.White
                            pendingScore < holePar -> ScoreUnderParColor
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
                            modifier = Modifier.size(48.dp),
                            onClick = {
                                pendingScore = if (pendingScore == 0) holePar else pendingScore + 1
                            },
                            enabled = pendingScore < 20,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = WearButtonBackground,
                                contentColor = Color.White,
                                disabledBackgroundColor = WearButtonBackground.copy(alpha = 0.4f),
                                disabledContentColor = Color.White.copy(alpha = 0.3f)
                            )
                        ) {
                            Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
                        colors = ChipDefaults.chipColors(
                            backgroundColor = WearButtonBackground,
                            contentColor = Color.White
                        )
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

    // End-of-round prompt — shown when Next Hole is pressed on the final hole
    if (showEndRoundPrompt) {
        Dialog(
            showDialog = true,
            onDismissRequest = { showEndRoundPrompt = false }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    onClick = { showEndRoundPrompt = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "End round on the phone app",
                            style = MaterialTheme.typography.body1,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Tee order popup — shown when player name is tapped
    if (showTeeOrder) {
        Dialog(
            showDialog = true,
            onDismissRequest = { showTeeOrder = false }
        ) {
            Card(
                onClick = { showTeeOrder = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Tee Order",
                        style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                        color = HoleNumberColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(2.dp))
                    players.forEachIndexed { i, player ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${i + 1}.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colors.onSurfaceVariant,
                                modifier = Modifier.width(18.dp)
                            )
                            Text(
                                player.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
