package com.scorigami.app.ui.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.ScreenBackground
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scorigami.app.ui.theme.CoursesGradientEnd
import com.scorigami.app.ui.theme.CoursesGradientStart
import com.scorigami.app.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    onBack: () -> Unit,
    onCreateCourse: () -> Unit,
    onEditCourse: (Long) -> Unit,
    viewModel: CourseViewModel = hiltViewModel()
) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    var courseToDelete by remember { mutableStateOf<com.scorigami.shared.db.entity.CourseEntity?>(null) }

    Scaffold(
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
                Text("No courses yet. Tap + to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().background(ScreenBackground).padding(padding)) {
                items(courses) { courseWithHoles ->
                    val course = courseWithHoles.course
                    val par = courseWithHoles.holes.sumOf { it.par }
                    ListItem(
                        headlineContent = { Text(course.name) },
                        supportingContent = { Text("${course.holeCount} holes · Par $par") },
                        trailingContent = {
                            IconButton(onClick = { courseToDelete = course }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.clickable { onEditCourse(course.id) },
                        colors = ListItemDefaults.colors(containerColor = ScreenBackground)
                    )
                    HorizontalDivider()
                }
            }
        }
    }

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
}
