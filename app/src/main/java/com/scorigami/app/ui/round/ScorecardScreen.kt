package com.scorigami.app.ui.round

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scorigami.app.viewmodel.RoundViewModel
import com.scorigami.shared.db.entity.HoleEntity
import com.scorigami.shared.db.entity.PlayerEntity

private val CardBackground = Color(0xFF1A3652)
private val HoleNumberColor = Color(0xFFFFD60A)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScorecardScreen(
    onEndRound: () -> Unit,
    onBack: () -> Unit,
    onCancelRound: () -> Unit,
    viewModel: RoundViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allPlayers by viewModel.allPlayers.collectAsStateWithLifecycle()

    if (!state.isActive) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentHoleEntity = state.holes.find { it.number == state.currentHole }
    val incompleteHoles = remember(state.scores, state.players, state.holes) {
        state.holes
            .filter { hole -> state.players.any { player -> (state.scores[Pair(player.id, hole.number)] ?: 0) == 0 } }
            .map { it.number }
            .toSet()
    }
    val hasMissingScores = incompleteHoles.isNotEmpty()
    var menuExpanded by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showPlayersSheet by remember { mutableStateOf(false) }
    var showMissingScoresDialog by remember { mutableStateOf(false) }
    var showScorecardSheet by remember { mutableStateOf(false) }

    // Hole number spring-scale animation
    val holeScale = remember { Animatable(1f) }
    LaunchedEffect(state.currentHole) {
        holeScale.snapTo(0.82f)
        holeScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
        )
    }

    if (showMissingScoresDialog) {
        AlertDialog(
            onDismissRequest = { showMissingScoresDialog = false },
            title = { Text("Missing Scores") },
            text = { Text("There are some missing scores. Continue to end the round? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMissingScoresDialog = false
                        onEndRound()
                    }
                ) { Text("Continue", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showMissingScoresDialog = false }) { Text("Go Back") }
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Round?") },
            text = { Text("All scores will be discarded. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelRound()
                        onCancelRound()
                    }
                ) { Text("Discard Round", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep Playing") }
            }
        )
    }

    if (showScorecardSheet) {
        ModalBottomSheet(onDismissRequest = { showScorecardSheet = false }) {
            FullScorecardSheet(
                players = state.players,
                holes = state.holes,
                scores = state.scores
            )
        }
    }

    if (showPlayersSheet) {
        ModalBottomSheet(onDismissRequest = { showPlayersSheet = false }) {
            AddRemovePlayersSheet(
                currentPlayers = state.players,
                allPlayers = allPlayers,
                onAddPlayer = { viewModel.addPlayerToRound(it) },
                onRemovePlayer = { viewModel.removePlayerFromRound(it) }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.courseName,
                        fontFamily = FontFamily.Cursive,
                        fontSize = 30.sp
                    )
                },
                actions = {
                    IconButton(onClick = { showScorecardSheet = true }) {
                        Icon(Icons.Default.TableChart, contentDescription = "View scorecard")
                    }
                    TextButton(onClick = {
                        if (hasMissingScores) showMissingScoresDialog = true else onEndRound()
                    }) {
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
                                    showPlayersSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cancel Round", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    showCancelDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(state.currentHole, state.holes.size) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f },
                        onDragEnd = {
                            val threshold = 80.dp.toPx()
                            when {
                                totalDrag < -threshold && state.currentHole < state.holes.size ->
                                    viewModel.navigateToHole(state.currentHole + 1)
                                totalDrag > threshold && state.currentHole > 1 ->
                                    viewModel.navigateToHole(state.currentHole - 1)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDrag += dragAmount
                        }
                    )
                }
        ) {
            // Hole navigation card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { viewModel.navigateToHole(state.currentHole - 1) },
                        enabled = state.currentHole > 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous hole")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Hole ${state.currentHole}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HoleNumberColor,
                            modifier = Modifier.scale(holeScale.value)
                        )
                        currentHoleEntity?.let {
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { viewModel.navigateToHole(state.currentHole + 1) },
                        enabled = state.currentHole < state.holes.size
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next hole")
                    }
                }
            }

            // Player score cards — slide direction matches hole navigation
            AnimatedContent(
                targetState = state.currentHole,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val toNext = targetState > initialState
                    slideInHorizontally(
                        animationSpec = tween(250),
                        initialOffsetX = { if (toNext) it else -it }
                    ) + fadeIn(tween(200)) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(250),
                        targetOffsetX = { if (toNext) -it else it }
                    ) + fadeOut(tween(200))
                },
                label = "hole_slide"
            ) { hole ->
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.players, key = { it.id }) { player ->
                        PlayerScoreCard(
                            player = player,
                            currentHole = hole,
                            scores = state.scores,
                            holes = state.holes,
                            onScoreChange = { throws ->
                                viewModel.updateScore(player.id, hole, throws)
                            }
                        )
                    }
                }
            }

            // Hole jump — golf icon + dropdown, bottom right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.GolfCourse,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                HoleJumpDropdown(
                    currentHole = state.currentHole,
                    holes = state.holes,
                    incompleteHoles = incompleteHoles,
                    onHoleSelected = { viewModel.navigateToHole(it) }
                )
            }
        }
    }
}

@Composable
private fun HoleJumpDropdown(
    currentHole: Int,
    holes: List<HoleEntity>,
    incompleteHoles: Set<Int>,
    onHoleSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("Hole $currentHole")
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val scrollState = rememberScrollState()
            val canScrollUp by remember { derivedStateOf { scrollState.value > 0 } }
            val canScrollDown by remember { derivedStateOf { scrollState.value < scrollState.maxValue } }

            Box(modifier = Modifier.heightIn(max = 480.dp)) {
                Column(modifier = Modifier.verticalScroll(scrollState)) {
                    holes.forEach { hole ->
                        DropdownMenuItem(
                            text = { Text("Hole ${hole.number}") },
                            leadingIcon = if (hole.number == currentHole) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            trailingIcon = if (hole.number in incompleteHoles) {
                                {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFFFFB300), CircleShape)
                                    )
                                }
                            } else null,
                            onClick = {
                                onHoleSelected(hole.number)
                                expanded = false
                            }
                        )
                    }
                }
                if (canScrollUp) {
                    Icon(
                        Icons.Default.ExpandLess,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                if (canScrollDown) {
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 2.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddRemovePlayersSheet(
    currentPlayers: List<PlayerEntity>,
    allPlayers: List<PlayerEntity>,
    onAddPlayer: (String) -> Unit,
    onRemovePlayer: (Long) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    val suggestions = allPlayers.filter { p -> currentPlayers.none { it.id == p.id } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Players", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))

        currentPlayers.forEach { player ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(player.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                IconButton(
                    onClick = { onRemovePlayer(player.id) },
                    enabled = currentPlayers.size > 1
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove ${player.name}",
                        tint = if (currentPlayers.size > 1) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
            HorizontalDivider()
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Add player") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val name = nameInput.trim()
                    if (name.isNotEmpty() && currentPlayers.none { it.name == name }) {
                        onAddPlayer(name)
                        nameInput = ""
                    }
                },
                enabled = nameInput.isNotBlank() && currentPlayers.none { it.name == nameInput.trim() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add player")
            }
        }

        if (suggestions.isNotEmpty()) {
            Text(
                "Previous players",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                suggestions.forEach { player ->
                    SuggestionChip(
                        onClick = { onAddPlayer(player.name) },
                        label = { Text(player.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerScoreCard(
    player: PlayerEntity,
    currentHole: Int,
    scores: Map<Pair<Long, Int>, Int>,
    holes: List<HoleEntity>,
    onScoreChange: (Int) -> Unit
) {
    val throwsThisHole = scores[Pair(player.id, currentHole)] ?: 0
    val playerScores = scores.entries.filter { it.key.first == player.id }
    val totalThrows = playerScores.sumOf { it.value }
    val parSoFar = holes
        .filter { hole -> playerScores.any { it.key.second == hole.number } }
        .sumOf { it.par }
    val totalVsPar = totalThrows - parSoFar
    val holePar = holes.find { it.number == currentHole }?.par ?: 3
    val scoreColor = when {
        throwsThisHole == 0 -> Color.White
        throwsThisHole < holePar -> Color(0xFF81C784)
        throwsThisHole == holePar -> Color.White
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 3-letter abbreviation — natural width so all 3 chars always render
            Text(
                text = player.name.take(3).uppercase(),
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(end = 12.dp),
                maxLines = 1
            )

            // Round vs-par — center column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Round",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatVsPar(totalVsPar),
                    style = MaterialTheme.typography.titleMedium,
                    color = vsParColor(totalVsPar),
                    fontWeight = FontWeight.Bold
                )
            }

            // − score + controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val next = if (throwsThisHole == 0) maxOf(1, holePar - 1) else throwsThisHole - 1
                        onScoreChange(next)
                    }
                ) {
                    Text(
                        "−",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = if (throwsThisHole == 0) "—" else "$throwsThisHole",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = scoreColor,
                    modifier = Modifier.widthIn(min = 40.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = {
                        val next = if (throwsThisHole == 0) holePar else throwsThisHole + 1
                        onScoreChange(next)
                    }
                ) {
                    Text(
                        "+",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
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
    vsPar < 0 -> MaterialTheme.colorScheme.primary
    vsPar == 0 -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.error
}

@Composable
private fun FullScorecardSheet(
    players: List<PlayerEntity>,
    holes: List<HoleEntity>,
    scores: Map<Pair<Long, Int>, Int>
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 32.dp)
    ) {
        items(players, key = { it.id }) { player ->
            val playerScores = scores.entries.filter { it.key.first == player.id }
            val totalThrows = playerScores.sumOf { it.value }
            val parSoFar = holes.filter { scores[Pair(player.id, it.number)] != null }.sumOf { it.par }
            val totalVsPar = totalThrows - parSoFar

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(player.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = formatVsPar(totalVsPar),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = vsParColor(totalVsPar)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    holes.chunked(9).forEach { group ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            group.forEach { hole ->
                                val throws = scores[Pair(player.id, hole.number)]
                                val vsPar = throws?.minus(hole.par)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${hole.number}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = vsPar?.let { formatVsPar(it) } ?: "—",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = vsPar?.let { vsParColor(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
