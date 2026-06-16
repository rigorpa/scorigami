package com.scorigami.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        }
    ) { padding ->
        if (rounds.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No completed rounds yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(padding)) {
                items(rounds) { round ->
                    ListItem(
                        headlineContent = { Text(round.courseName, fontWeight = FontWeight.SemiBold) },
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
                        colors = ListItemDefaults.colors(containerColor = Color.Black)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
