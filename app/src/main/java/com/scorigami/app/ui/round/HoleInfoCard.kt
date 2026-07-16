package com.scorigami.app.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scorigami.app.ui.theme.CardBackground
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.DefaultCardBackground
import com.scorigami.app.ui.theme.HoleJumpSelectedColor
import com.scorigami.app.ui.theme.IncompleteHoleDotColor
import com.scorigami.app.ui.theme.ScorecardHoleNumberColor
import com.scorigami.shared.db.entity.HoleEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HoleInfoCard(
    hole: Int,
    holeEntity: HoleEntity?,
    totalHoles: Int,
    holeScale: Float,
    holes: List<HoleEntity>,
    incompleteHoles: Set<Int>,
    onNavigateToHole: (Int) -> Unit,
    onHoleSelected: (Int) -> Unit,
    onAddRemovePlayers: () -> Unit,
    scoresVisible: Boolean,
    onToggleScoresVisible: () -> Unit
) {
    var showNotesSheet by remember(hole) { mutableStateOf(false) }
    var showHoleJumpDialog by remember { mutableStateOf(false) }

    if (showNotesSheet && !holeEntity?.notes.isNullOrBlank()) {
        ModalBottomSheet(onDismissRequest = { showNotesSheet = false }) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Hole $hole Notes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = holeEntity?.notes ?: "",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    if (showHoleJumpDialog) {
        // Default container (surfaceContainerLow grey) — consistent with the app's other sheets
        ModalBottomSheet(onDismissRequest = { showHoleJumpDialog = false }) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Jump to Hole",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    holes.chunked(3).forEach { rowHoles ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowHoles.forEach { h ->
                                val isCurrent = h.number == hole
                                val incomplete = h.number in incompleteHoles
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .background(
                                            if (isCurrent) HoleJumpSelectedColor else CardBackground,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            onHoleSelected(h.number)
                                            showHoleJumpDialog = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${h.number}",
                                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = ContentWhite,
                                        fontSize = 20.sp
                                    )
                                    if (incomplete) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 4.dp, end = 4.dp)
                                                .size(6.dp)
                                                .background(IncompleteHoleDotColor, CircleShape)
                                        )
                                    }
                                }
                            }
                            repeat(3 - rowHoles.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = DefaultCardBackground)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onNavigateToHole(hole - 1) },
                    enabled = hole > 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous hole", modifier = Modifier.size(48.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Hole",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        fontSize = 20.sp,
                        color = ContentWhite
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showHoleJumpDialog = true }
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$hole",
                            style = MaterialTheme.typography.displayMedium,
                            fontSize = 124.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ScorecardHoleNumberColor,
                            modifier = Modifier.scale(holeScale)
                        )
                    }
                    holeEntity?.let {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Par ${it.par}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            it.distanceFeet?.let { feet ->
                                val meters = (feet / 3.28084).toInt()
                                Text(
                                    "  ·  $feet ft / $meters m",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ContentWhite
                                )
                            }
                        }
                    }
                }
                IconButton(
                    onClick = { onNavigateToHole(hole + 1) },
                    enabled = hole < totalHoles
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next hole", modifier = Modifier.size(48.dp))
                }
            }
            if (!holeEntity?.notes.isNullOrBlank()) {
                IconButton(
                    onClick = { showNotesSheet = true },
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Hole rules",
                        tint = ContentWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            IconButton(
                onClick = onAddRemovePlayers,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = "Add / Remove Players",
                    tint = ContentWhite,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(
                onClick = onToggleScoresVisible,
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Icon(
                    if (scoresVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (scoresVisible) "Hide scores" else "Show scores",
                    tint = ContentWhite,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
