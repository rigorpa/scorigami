package com.scorigami.wear.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.*
import com.scorigami.wear.ui.theme.C1xActiveColor
import com.scorigami.wear.ui.theme.C2ActiveColor
import com.scorigami.wear.ui.theme.ContentWhite
import com.scorigami.wear.ui.theme.HoleNumberColor
import com.scorigami.wear.ui.theme.ScoreOverParColor
import com.scorigami.wear.ui.theme.ScoreUnderParColor
import com.scorigami.wear.ui.theme.StatUnsetColor
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
    c2Made: Boolean,
    onPendingScoreChange: (Int) -> Unit,
    onObTap: () -> Unit,
    onShowPuttStats: () -> Unit,
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

            // OB counter (left) and CX putting-stat button (right) flanking the player
            // name; CX opens the C1x / C2 picker. Name itself still taps to show tee order
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WearStatButton(
                    text = if (obCount == 0) "OB" else if (obCount >= 3) "3+ OB" else "$obCount OB",
                    active = obCount > 0,
                    activeColor = ScoreOverParColor,
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
                    text = cxLabel(c1xCount, c2Made),
                    active = c1xCount > 0 || c2Made,
                    activeColor = StatUnsetColor, // colors come from the AnnotatedString spans
                    onClick = onShowPuttStats
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
 * Label for the CX putting-stat button: grey "CX" while nothing is logged, otherwise the
 * logged stats in their own colors — "2 C1x" orange, "C2" green, or "C1x C2" when both.
 */
private fun cxLabel(c1xCount: Int, c2Made: Boolean): AnnotatedString = buildAnnotatedString {
    when {
        c1xCount == 0 && !c2Made -> withStyle(SpanStyle(color = StatUnsetColor)) { append("CX") }
        c2Made && c1xCount == 0 -> withStyle(SpanStyle(color = C2ActiveColor)) { append("C2") }
        !c2Made -> withStyle(SpanStyle(color = C1xActiveColor)) {
            append(if (c1xCount >= 3) "3+ C1x" else "$c1xCount C1x")
        }
        else -> {
            withStyle(SpanStyle(color = C1xActiveColor)) { append("C1x ") }
            withStyle(SpanStyle(color = C2ActiveColor)) { append("C2") }
        }
    }
}

/**
 * Per-hole stat counter; tap cycles - → 1 → 2 → 3+ → back to - (caller owns the cycle).
 * Grey while unset so it doesn't shout; [activeColor] (OB red / C1x orange) once a count is entered.
 */
@Composable
private fun WearStatButton(
    text: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) = WearStatButton(AnnotatedString(text), active, activeColor, onClick)

@Composable
private fun WearStatButton(
    text: AnnotatedString,
    active: Boolean,
    activeColor: Color,
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
            color = if (active) activeColor else StatUnsetColor,
            maxLines = 1
        )
    }
}
