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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.scorigami.app.ui.theme.CardBackground
import com.scorigami.app.ui.theme.CardGrey
import com.scorigami.app.ui.theme.ScaleGrey1
import com.scorigami.app.ui.theme.ScaleGrey2
import com.scorigami.app.ui.theme.HoleNumberColor
import com.scorigami.app.ui.theme.HoleJumpSelectedColor
import com.scorigami.app.ui.theme.IncompleteHoleDotColor
import com.scorigami.app.ui.theme.NewRoundGradientEnd
import com.scorigami.app.ui.theme.NewRoundGradientStart
import com.scorigami.app.ui.theme.ScoreUnderParColor
import androidx.compose.ui.graphics.Brush
import com.scorigami.shared.db.entity.HoleEntity
import com.scorigami.shared.db.entity.PlayerEntity

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

    // currentHoleEntity is now derived inside AnimatedContent using the animated hole value
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(NewRoundGradientStart, NewRoundGradientEnd)))
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = state.courseName,
                            fontFamily = FontFamily.Cursive,
                            fontSize = 32.sp
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
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
            // Hole card + player cards animate together on hole change
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
                val holeEntity = state.holes.find { it.number == hole }
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

                Column(modifier = Modifier.fillMaxSize()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding( vertical = 8.dp),
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
                                    onClick = { viewModel.navigateToHole(hole - 1) },
                                    enabled = hole > 1
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous hole")
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Hole $hole",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = HoleNumberColor,
                                        modifier = Modifier.scale(holeScale.value)
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
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.navigateToHole(hole + 1) },
                                    enabled = hole < state.holes.size
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next hole")
                                }
                            }
                            // Info icon — top-left, only shown when hole has notes
                            if (!holeEntity?.notes.isNullOrBlank()) {
                                IconButton(
                                    onClick = { showNotesSheet = true },
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Hole rules",
                                        tint = Color.White,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            // Group icon — top-right shortcut to Add/Remove Players
                            IconButton(
                                onClick = { showPlayersSheet = true },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Group,
                                    contentDescription = "Add / Remove Players",
                                    tint = Color.White,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
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
            }

            // Hole jump — golf icon + dropdown, top left
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
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                HoleJumpGrid(
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
private fun HoleJumpGrid(
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
                                                color = Color.White,
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
        throwsThisHole < holePar -> ScoreUnderParColor
        throwsThisHole == holePar -> Color.White
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ScaleGrey2)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = player.name,
                fontSize = 36.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
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
                    color = Color.Black
                )
                Text(
                    formatVsPar(totalVsPar),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Normal
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
