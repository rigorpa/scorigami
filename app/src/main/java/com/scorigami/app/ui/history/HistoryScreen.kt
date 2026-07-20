package com.scorigami.app.ui.history

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.ContentLightGrey
import com.scorigami.app.ui.theme.ScreenBackground
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.scorigami.app.ui.round.SectionCardColor
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scorigami.app.ui.theme.HistoryGradientEnd
import com.scorigami.app.ui.theme.HistoryGradientStart
import com.scorigami.app.viewmodel.HistoryViewModel
import com.scorigami.shared.sync.SgHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onRoundDetail: (Long) -> Unit,
    pendingHistoryImport: MutableState<SgHistory?>,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val rounds by viewModel.rounds.collectAsStateWithLifecycle()
    var showShareHint by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Consume a pending .sghistory import delivered via ACTION_VIEW
    LaunchedEffect(pendingHistoryImport.value) {
        val history = pendingHistoryImport.value ?: return@LaunchedEffect
        viewModel.importHistory(history)
        pendingHistoryImport.value = null
    }

    // Import-complete snackbar; collected from viewModelScope so it survives navigation
    LaunchedEffect(Unit) {
        viewModel.importedHistory.collect { (imported, skipped) ->
            val msg = buildString {
                append("Imported $imported round")
                if (imported != 1) append("s")
                if (skipped > 0) append(" — $skipped already present")
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            // Bubble cards matching the setup/editor/course-list widget language
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(ScreenBackground).padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onRoundDetail(round.roundId) },
                        colors = ListItemDefaults.colors(
                            containerColor = SectionCardColor,
                            headlineColor = ContentWhite,
                            supportingColor = ContentLightGrey
                        )
                    )
                }
            }
        }
    }

    // Share sheet: single-round hint + full-history export
    if (showShareHint) {
        ModalBottomSheet(
            onDismissRequest = { showShareHint = false },
            containerColor = ScreenBackground
        ) {
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
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    "Export History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Share every completed round as a .sghistory file — open it in Scorigami on another device to import",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ContentLightGrey
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val history = viewModel.buildExport()
                            shareHistory(context, history)
                            showShareHint = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = rounds.isNotEmpty()
                ) {
                    Text("Export All Rounds")
                }
            }
        }
    }
}

private suspend fun shareHistory(context: Context, history: SgHistory) {
    val uri = withContext(Dispatchers.IO) {
        val json = Json.encodeToString(SgHistory.serializer(), history)
        val dir = File(context.cacheDir, "shared_history").apply { mkdirs() }
        val file = File(dir, "scorigami-rounds.sghistory")
        file.writeText(json)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Scorigami Round History")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share round history"))
}
