package com.scorigami.app.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.NewRoundGradientEnd
import com.scorigami.app.ui.theme.NewRoundGradientStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScorecardTopBar(
    courseName: String,
    onViewScorecard: () -> Unit,
    onEndRound: () -> Unit,
    onAddRemovePlayers: () -> Unit,
    onCancelRound: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(NewRoundGradientStart, NewRoundGradientEnd)))
    ) {
        TopAppBar(
            title = {
                // Auto-shrink: start at 32sp and step down until the name fits on one line
                // (18sp floor, then ellipsis as a last resort). Drawing is suppressed until
                // the size settles so the oversized first pass never flashes on screen.
                var titleSize by remember(courseName) { mutableStateOf(32.sp) }
                var sizeSettled by remember(courseName) { mutableStateOf(false) }
                Text(
                    text = courseName,
                    fontFamily = FontFamily.Cursive,
                    fontSize = titleSize,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result ->
                        if (result.hasVisualOverflow && titleSize.value > 18f) {
                            titleSize = (titleSize.value * 0.9f).coerceAtLeast(18f).sp
                        } else {
                            sizeSettled = true
                        }
                    },
                    modifier = Modifier.drawWithContent { if (sizeSettled) drawContent() }
                )
            },
            actions = {
                IconButton(onClick = onViewScorecard) {
                    Icon(Icons.Default.TableChart, contentDescription = "View scorecard")
                }
                TextButton(onClick = onEndRound) {
                    Text("End Round", color = MaterialTheme.colorScheme.error)
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add / Remove Players") },
                            onClick = {
                                menuExpanded = false
                                onAddRemovePlayers()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cancel Round", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onCancelRound()
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = ContentWhite,
                navigationIconContentColor = ContentWhite,
                actionIconContentColor = ContentWhite
            )
        )
    }
}
