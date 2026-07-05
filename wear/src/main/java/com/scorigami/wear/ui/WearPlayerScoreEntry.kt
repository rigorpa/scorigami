package com.scorigami.wear.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.scorigami.wear.ui.theme.ContentWhite
import com.scorigami.wear.ui.theme.HoleNumberColor
import com.scorigami.wear.ui.theme.ScoreOverParColor
import com.scorigami.wear.ui.theme.ScoreUnderParColor
import com.scorigami.wear.ui.theme.WearButtonBackground

@Composable
internal fun WearPlayerScoreEntry(
    currentHole: Int,
    playerName: String,
    holePar: Int,
    isLastPlayer: Boolean,
    pendingScore: Int,
    obCount: Int,
    c1xCount: Int,
    onPendingScoreChange: (Int) -> Unit,
    onObTap: () -> Unit,
    onC1xTap: () -> Unit,
    onCommit: () -> Unit,
    onShowHoleJump: () -> Unit,
    onShowTeeOrder: () -> Unit,
    focusRequester: FocusRequester
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // Hole indicator — tappable to open hole-jump picker
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)) {
                        append("$currentHole")
                    }
                },
                color = HoleNumberColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable { onShowHoleJump() }
            )

            // OB (left) and C1x (right) stat counters flanking the player name;
            // name itself still taps to show tee order
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WearStatButton(
                    text = if (obCount == 0) "OB" else if (obCount >= 3) "3+ OB" else "$obCount OB",
                    active = obCount > 0,
                    onClick = onObTap
                )
                Text(
                    playerName,
                    style = MaterialTheme.typography.title1,
                    fontWeight = FontWeight.SemiBold,
                    color = ContentWhite,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onShowTeeOrder() }
                )
                WearStatButton(
                    text = if (c1xCount == 0) "C1x" else if (c1xCount >= 3) "3+ C1x" else "$c1xCount C1x",
                    active = c1xCount > 0,
                    onClick = onC1xTap
                )
            }

            // − score + controls
            val scoreColor = when {
                pendingScore == 0 -> ContentWhite
                pendingScore < holePar -> ScoreUnderParColor
                pendingScore == holePar -> ContentWhite
                else -> ScoreOverParColor
            }
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
                        onPendingScoreChange(
                            if (pendingScore == 0) maxOf(1, holePar - 1) else pendingScore - 1
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = WearButtonBackground,
                        contentColor = ContentWhite,
                        disabledBackgroundColor = WearButtonBackground.copy(alpha = 0.4f),
                        disabledContentColor = ContentWhite.copy(alpha = 0.3f)
                    )
                ) {
                    Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
                        onPendingScoreChange(
                            if (pendingScore == 0) holePar else pendingScore + 1
                        )
                    },
                    enabled = pendingScore < 20,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = WearButtonBackground,
                        contentColor = ContentWhite,
                        disabledBackgroundColor = WearButtonBackground.copy(alpha = 0.4f),
                        disabledContentColor = ContentWhite.copy(alpha = 0.3f)
                    )
                ) {
                    Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Enter / Next Hole button
            Chip(
                onClick = onCommit,
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
                    contentColor = ContentWhite
                )
            )

            // End Round button — hidden for now. To re-enable: uncomment, add an
            // `onEndRound: () -> Unit` param back to this composable and to
            // WearScorecardScreen, and wire it in WearNavigation (navigate to
            // WearScreen.EndRoundPrompt.route).
            // Chip(
            //     onClick = onEndRound,
            //     label = { Text("End Round", fontSize = 12.sp) },
            //     modifier = Modifier.fillMaxWidth(0.65f),
            //     colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colors.error)
            // )
        }
    }
}

/**
 * Per-hole stat counter; tap cycles - → 1 → 2 → 3+ → back to - (caller owns the cycle).
 * Grey while unset so it doesn't shout; turns red once a count is entered.
 */
@Composable
private fun WearStatButton(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp)
            .widthIn(min = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) ScoreOverParColor else WearButtonBackground,
            maxLines = 1
        )
    }
}
