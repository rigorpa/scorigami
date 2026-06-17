package com.scorigami.app.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.scorigami.app.ui.theme.CardBackground
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.HoleJumpSelectedColor
import com.scorigami.app.ui.theme.IncompleteHoleDotColor
import com.scorigami.shared.db.entity.HoleEntity

@Composable
internal fun HoleJumpGrid(
    currentHole: Int,
    holes: List<HoleEntity>,
    incompleteHoles: Set<Int>,
    onHoleSelected: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { showDialog = true }) {
        Text("Hole $currentHole")
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }

    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showDialog = false }
                    .padding(top = 320.dp, start = 12.dp, end = 12.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {},
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                    rowHoles.forEach { hole ->
                                        val isCurrent = hole.number == currentHole
                                        val incomplete = hole.number in incompleteHoles
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(60.dp)
                                                .background(
                                                    if (isCurrent) HoleJumpSelectedColor else CardBackground,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    onHoleSelected(hole.number)
                                                    showDialog = false
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${hole.number}",
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
        }
    }
}
