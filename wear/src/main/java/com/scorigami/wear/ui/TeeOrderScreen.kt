package com.scorigami.wear.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.scorigami.wear.R
import com.scorigami.shared.sync.PlayerState
import com.scorigami.wear.ui.theme.ContentWhite
import com.scorigami.wear.ui.theme.HoleNumberColor
import com.scorigami.wear.ui.theme.ScoreOverParColor
import com.scorigami.wear.ui.theme.ScoreUnderParColor

/**
 * Inline full-screen tee-order view — rendered as a Scaffold branch like WearHoleJumpGrid,
 * NOT a wear Dialog. A Dialog spawns a second platform window plus the Wear OS entrance
 * animation, which is very slow on emulators; swapping composables in the same window is
 * instant. Tap anywhere (except the eye toggle) to go back.
 */
@Composable
internal fun TeeOrderScreen(
    players: List<PlayerState>,
    onDismiss: () -> Unit
) {
    var scoresVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = ContentWhite,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (scoresVisible) formatVsPar(player.totalVsPar) else "•••",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (scoresVisible) vsParColor(player.totalVsPar) else ContentWhite,
                    textAlign = TextAlign.End
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { scoresVisible = !scoresVisible }
                .padding(6.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (scoresVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                ),
                contentDescription = if (scoresVisible) "Hide scores" else "Show scores",
                tint = ContentWhite,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatVsPar(vsPar: Int): String = when {
    vsPar < 0 -> "$vsPar"
    vsPar == 0 -> "E"
    else -> "+$vsPar"
}

@Composable
private fun vsParColor(vsPar: Int) = when {
    vsPar < 0 -> ScoreUnderParColor
    vsPar == 0 -> ContentWhite
    else -> ScoreOverParColor
}
