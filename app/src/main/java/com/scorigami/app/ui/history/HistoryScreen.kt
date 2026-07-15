package com.scorigami.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.ContentLightGrey
import com.scorigami.app.ui.theme.ScreenBackground
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scorigami.app.ui.theme.HistoryGradientEnd
import com.scorigami.app.ui.theme.HistoryGradientStart
import com.scorigami.app.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onRoundDetail: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val rounds by viewModel.rounds.collectAsStateWithLifecycle()
    var showShareHint by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(HistoryGradientStart, HistoryGradientEnd)))
            ) {
                TopAppBar(
                    title = { Text("Round History") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showShareHint = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Share round", tint = ContentWhite)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = ContentWhite,
                        navigationIconContentColor = ContentWhite
                    )
                )
            }
        }
    ) { padding ->
        if (rounds.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No completed rounds yet.", color = ContentWhite)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().background(ScreenBackground).padding(padding)) {
                items(rounds) { round ->
                    ListItem(
                        headlineContent = { Text(round.courseName, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Column {
                                Text(round.date, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    round.playerResults.joinToString("  ·  ") { "${it.first}: ${it.second}" },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        modifier = Modifier.clickable { onRoundDetail(round.roundId) },
                        colors = ListItemDefaults.colors(
                            containerColor = ScreenBackground,
                            headlineColor = ContentWhite,
                            supportingColor = ContentLightGrey
                        )
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    // Share hint
    if (showShareHint) {
        ModalBottomSheet(onDismissRequest = { showShareHint = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Share a Round",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Select a round first to share",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ContentLightGrey
                )
            }
        }
    }
}
