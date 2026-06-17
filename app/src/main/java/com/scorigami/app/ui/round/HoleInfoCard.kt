package com.scorigami.app.ui.round

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.HoleNumberColor
import com.scorigami.app.ui.theme.ScaleGrey1
import com.scorigami.shared.db.entity.HoleEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HoleInfoCard(
    hole: Int,
    holeEntity: HoleEntity?,
    totalHoles: Int,
    holeScale: Float,
    onNavigateToHole: (Int) -> Unit,
    onAddRemovePlayers: () -> Unit,
    scoresVisible: Boolean,
    onToggleScoresVisible: () -> Unit
) {
    var showNotesSheet by remember(hole) { mutableStateOf(false) }

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = ScaleGrey1)
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous hole")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                                append("Hole ")
                            }
                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                append("$hole")
                            }
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = 44.sp,
                        color = HoleNumberColor,
                        modifier = Modifier.scale(holeScale)
                    )
                    holeEntity?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Par ${it.par}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        it.distanceFeet?.let { feet ->
                            val meters = (feet / 3.28084).toInt()
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "$feet ft / $meters m",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ContentWhite
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { onNavigateToHole(hole + 1) },
                    enabled = hole < totalHoles
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next hole")
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
                        modifier = Modifier.size(21.dp)
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
                    modifier = Modifier.size(25.dp)
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
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}
