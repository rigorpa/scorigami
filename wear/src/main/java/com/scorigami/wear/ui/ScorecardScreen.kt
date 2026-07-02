package com.scorigami.wear.ui

import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.wear.compose.material.Scaffold
import com.scorigami.shared.sync.RoundState

@Composable
fun WearScorecardScreen(
    roundState: RoundState,
    currentHole: Int,
    onNextHole: () -> Unit,
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
            WearHoleJumpGrid(
                totalHoles = roundState.totalHoles,
                currentHole = currentHole,
                incompleteHoles = incompleteHoles,
                onHoleSelected = { holeNum ->
                    onJumpToHole(holeNum)
                    showHoleJump = false
                }
            )
        } else {
            WearPlayerScoreEntry(
                currentHole = currentHole,
                playerName = currentPlayer.name,
                holePar = holePar,
                isLastPlayer = isLastPlayer,
                pendingScore = pendingScore,
                onPendingScoreChange = { pendingScore = it },
                onCommit = ::commitAndAdvance,
                onShowHoleJump = { showHoleJump = true },
                onShowTeeOrder = { showTeeOrder = true },
                focusRequester = focusRequester
            )
        }
    }

    if (showEndRoundPrompt) {
        EndRoundPromptDialog(onDismiss = { showEndRoundPrompt = false })
    }

    if (showTeeOrder) {
        TeeOrderDialog(
            players = players,
            onDismiss = { showTeeOrder = false }
        )
    }
}
