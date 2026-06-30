package com.scorigami.app.ui.course

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.scorigami.app.ui.theme.ContentLightGrey
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.ScreenBackground
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scorigami.app.ui.theme.CoursesGradientEnd
import com.scorigami.app.ui.theme.CoursesGradientStart
import com.scorigami.shared.db.dao.CourseWithHoles
import com.scorigami.shared.sync.SgCourse
import com.scorigami.shared.sync.SgHole
import com.scorigami.app.viewmodel.CourseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    onBack: () -> Unit,
    onCreateCourse: () -> Unit,
    onEditCourse: (Long) -> Unit,
    pendingImport: MutableState<SgCourse?> = mutableStateOf(null),
    viewModel: CourseViewModel = hiltViewModel()
) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    var courseToDelete by remember { mutableStateOf<com.scorigami.shared.db.entity.CourseEntity?>(null) }
    var showSharePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle pending import from an ACTION_VIEW intent
    LaunchedEffect(pendingImport.value) {
        val sgCourse = pendingImport.value ?: return@LaunchedEffect
        viewModel.importCourse(sgCourse)
        pendingImport.value = null
    }

    // Show snackbar when an import completes; collected from viewModelScope so it survives navigation
    LaunchedEffect(Unit) {
        viewModel.importedCourse.collect { (importedName, holeCount) ->
            snackbarHostState.showSnackbar("Imported \"$importedName\" — $holeCount holes")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(CoursesGradientStart, CoursesGradientEnd)))
            ) {
                TopAppBar(
                    title = { Text("My Courses") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSharePicker = true },
                            enabled = courses.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share course",
                                tint = if (courses.isNotEmpty()) ContentWhite
                                       else ContentWhite.copy(alpha = 0.4f)
                            )
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
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateCourse) {
                Icon(Icons.Default.Add, contentDescription = "Add Course")
            }
        }
    ) { padding ->
        if (courses.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No courses yet. Tap + to create one.", 
                color = ContentLightGrey
            )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().background(ScreenBackground).padding(padding)) {
                items(courses) { courseWithHoles ->
                    val course = courseWithHoles.course
                    val par = courseWithHoles.holes.sumOf { it.par }
                    ListItem(
                        headlineContent = { Text(course.name, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("${course.holeCount} holes · Par $par") },
                        trailingContent = {
                            IconButton(onClick = { courseToDelete = course }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.clickable { onEditCourse(course.id) },
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

    // Delete confirmation dialog
    courseToDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            title = { Text("Delete ${course.name}?") },
            text = { Text("This will permanently remove the course. Rounds using it will be kept.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCourse(course)
                    courseToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { courseToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Share course picker — a bottom sheet matching the app's dark list styling
    if (showSharePicker) {
        ModalBottomSheet(
            onDismissRequest = { showSharePicker = false },
            containerColor = ScreenBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Share a Course",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ContentWhite,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                courses.forEach { courseWithHoles ->
                    val course = courseWithHoles.course
                    val par = courseWithHoles.holes.sumOf { it.par }
                    ListItem(
                        headlineContent = { Text(course.name, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("${course.holeCount} holes · Par $par") },
                        trailingContent = {
                            Icon(Icons.Default.Share, contentDescription = null, tint = ContentWhite)
                        },
                        modifier = Modifier.clickable {
                            showSharePicker = false
                            scope.launch { shareCourse(context, courseWithHoles) }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = ScreenBackground,
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

private suspend fun shareCourse(context: Context, courseWithHoles: CourseWithHoles) {
    val sgCourse = SgCourse(
        version = 1,
        name = courseWithHoles.course.name,
        holeCount = courseWithHoles.course.holeCount,
        holes = courseWithHoles.holes
            .sortedBy { it.number }
            .map { hole ->
                SgHole(
                    number = hole.number,
                    par = hole.par,
                    distanceFeet = hole.distanceFeet,
                    notes = hole.notes
                )
            }
    )

    // Strip filesystem-unsafe characters from the course name to avoid path traversal / IOException.
    val safeName = courseWithHoles.course.name.replace(Regex("""[/\\:*?"<>|]"""), "_")

    val uri = withContext(Dispatchers.IO) {
        val json = Json.encodeToString(SgCourse.serializer(), sgCourse)
        val dir = File(context.cacheDir, "shared_courses").apply { mkdirs() }
        val file = File(dir, "$safeName.sgcourse")
        file.writeText(json)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "${courseWithHoles.course.name} — Scorigami Course")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share course"))
}

