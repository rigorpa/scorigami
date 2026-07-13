package com.scorigami.app.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.scorigami.app.ui.theme.ContentLightGrey
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.DisabledButtonGradientEnd
import com.scorigami.app.ui.theme.DisabledButtonGradientStart
import androidx.compose.ui.unit.dp
import com.scorigami.app.ui.theme.NewRoundGradientEnd
import com.scorigami.app.ui.theme.NewRoundGradientStart
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scorigami.app.viewmodel.CourseViewModel
import com.scorigami.app.viewmodel.RoundViewModel
import com.scorigami.shared.db.entity.PlayerEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoundSetupScreen(
    onBack: () -> Unit,
    onRoundStarted: () -> Unit,
    roundViewModel: RoundViewModel = hiltViewModel(),
    courseViewModel: CourseViewModel = hiltViewModel()
) {
    val courses by courseViewModel.courses.collectAsStateWithLifecycle()
    val allPlayers by roundViewModel.allPlayers.collectAsStateWithLifecycle()
    val lastPlayedCourseId by roundViewModel.lastPlayedCourseId.collectAsStateWithLifecycle()
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var playerNameInput by remember { mutableStateOf("") }
    val players = remember { mutableStateListOf<String>() }
    var showCoursePicker by remember { mutableStateOf(false) }

    // Last played course first, remainder alphabetical
    val sortedCourses = remember(courses, lastPlayedCourseId) {
        val last = courses.find { it.course.id == lastPlayedCourseId }
        val rest = courses.filter { it.course.id != lastPlayedCourseId }.sortedBy { it.course.name }
        if (last != null) listOf(last) + rest else rest
    }

    val selectedCourse = sortedCourses.find { it.course.id == selectedCourseId }

    LaunchedEffect(sortedCourses) {
        if (selectedCourseId == null && sortedCourses.isNotEmpty()) {
            selectedCourseId = sortedCourses.first().course.id
        }
    }

    var playerToDelete by remember { mutableStateOf<PlayerEntity?>(null) }

    if (playerToDelete != null) {
        val player = playerToDelete!!
        AlertDialog(
            onDismissRequest = { playerToDelete = null },
            title = { Text("Remove Player?") },
            text = { Text("Are you sure you want to remove \"${player.name}\" from history? This will hide them from suggestions, but their past rounds and scores will remain intact.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        roundViewModel.archivePlayer(player.id)
                        playerToDelete = null
                    }
                ) {
                    Text("Remove", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(NewRoundGradientStart, NewRoundGradientEnd)))
            ) {
                TopAppBar(
                    title = { Text("New Round") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = ContentWhite,
                        navigationIconContentColor = ContentWhite
                    )
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
            ) {
                // Gradient pill matching HomeScreen's HomeActionButton style
                val startEnabled = selectedCourseId != null && players.isNotEmpty()
                val startGradient = if (startEnabled) {
                    Brush.horizontalGradient(listOf(NewRoundGradientStart, NewRoundGradientEnd))
                } else {
                    Brush.horizontalGradient(listOf(DisabledButtonGradientStart, DisabledButtonGradientEnd))
                }
                Button(
                    onClick = {
                        selectedCourseId?.let { courseId ->
                            if (players.isNotEmpty()) {
                                roundViewModel.startRound(courseId, players.toList())
                                onRoundStarted()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(startGradient, RoundedCornerShape(percent = 50)),
                    enabled = startEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = ContentWhite,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = ContentWhite.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp
                    )
                ) {
                    Text("Start Round")
                }
            }
        }
    ) { padding ->
        val suggestions = allPlayers.filter { !players.contains(it.name) }

        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                SectionTitle("Course")
                // Gradient bubble matching the Add Player field; a transparent overlay
                // intercepts the tap (a readOnly OutlinedTextField consumes clicks itself)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCourse?.let { "${it.course.name} (Par ${it.holes.sumOf { h -> h.par }})" } ?: "Choose a course",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = ContentWhite)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = sectionFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SectionCardGradient, RoundedCornerShape(12.dp))
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClickLabel = "Choose a course") { showCoursePicker = true }
                    )
                }
            }

            // Players area — filled section card (matches the scorecard's card language)
            item {
                SectionCard(label = "Players") {
                    if (players.isEmpty()) {
                        Text(
                            "No players yet — add below or tap a previous golfer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ContentLightGrey
                        )
                    } else {
                        // Shuffle sits at the top-right of the box, above the player list
                        if (players.size > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Shuffle",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ContentLightGrey
                                )
                                IconButton(onClick = { players.shuffle() }) {
                                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle player order")
                                }
                            }
                        }
                        Column {
                            players.forEachIndexed { index, name ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                    IconButton(onClick = { players.remove(name) }) {
                                        Icon(Icons.Default.Close, "Remove $name", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                if (index < players.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // Previous golfers — bottom portion of the screen, below the players box
            if (suggestions.isNotEmpty()) {
                item {
                    SectionCard(label = "Previous Golfers") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            suggestions.forEach { player ->
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Name area — tap to add
                                        Text(
                                            text = player.name,
                                            color = ContentWhite,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier
                                                .clickable { players.add(player.name) }
                                                .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp)
                                        )
                                        // Remove target
                                        Box(
                                            modifier = Modifier
                                                .size(width = 40.dp, height = 48.dp)
                                                .clickable(
                                                    onClick = { playerToDelete = player },
                                                    onClickLabel = "Remove ${player.name}"
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = null,
                                                tint = Color.Red,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add Player — below the previous-golfer pills
            item {
                SectionTitle("Add Player")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = playerNameInput,
                        onValueChange = { playerNameInput = it },
                        placeholder = { Text("Player name", color = ContentLightGrey) },
                        shape = RoundedCornerShape(12.dp),
                        colors = sectionFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .background(SectionCardGradient, RoundedCornerShape(12.dp)),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val name = playerNameInput.trim()
                            if (name.isNotEmpty() && !players.contains(name)) {
                                players.add(name)
                                playerNameInput = ""
                            }
                        },
                        enabled = playerNameInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, "Add player")
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Course picker — default container (surfaceContainerLow grey), matching the app's
    // other sheets (share picker, hole notes, add/remove players)
    if (showCoursePicker) {
        ModalBottomSheet(onDismissRequest = { showCoursePicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Choose a Course",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ContentWhite,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                sortedCourses.forEach { cwh ->
                    val isSelected = cwh.course.id == selectedCourseId
                    ListItem(
                        headlineContent = { Text(cwh.course.name, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("${cwh.course.holeCount} holes · Par ${cwh.holes.sumOf { it.par }}") },
                        trailingContent = {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = ContentWhite)
                            }
                        },
                        modifier = Modifier.clickable {
                            selectedCourseId = cwh.course.id
                            showCoursePicker = false
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            headlineColor = ContentWhite,
                            supportingColor = ContentLightGrey,
                            trailingIconColor = ContentWhite
                        )
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
