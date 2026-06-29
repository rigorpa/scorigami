package com.scorigami.app.ui.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.ContentLightGrey
import com.scorigami.app.ui.theme.CoursesGradientEnd
import com.scorigami.app.ui.theme.CoursesGradientStart
import com.scorigami.app.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditorScreen(
    onBack: () -> Unit,
    viewModel: CourseViewModel = hiltViewModel()
) {
    val existing by viewModel.editingCourse.collectAsStateWithLifecycle()
    var initialized by remember { mutableStateOf(false) }

    var courseName by remember { mutableStateOf("") }
    var parValues by remember { mutableStateOf(List(18) { 3 }) }
    var notesValues by remember { mutableStateOf(List(18) { "" }) }

    LaunchedEffect(existing, viewModel.isEditing) {
        if (initialized) return@LaunchedEffect
        if (!viewModel.isEditing) {
            // New course — keep the blank defaults.
            initialized = true
        } else if (existing != null) {
            // Editing — populate from the loaded course. Wait (don't initialize)
            // while existing is still null, which means the DB load hasn't finished.
            courseName = existing!!.course.name
            val sorted = existing!!.holes.sortedBy { it.number }
            parValues = sorted.map { it.par }
            notesValues = sorted.map { it.notes ?: "" }
            initialized = true
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(CoursesGradientStart, CoursesGradientEnd)))
            ) {
                TopAppBar(
                    title = { Text(if (existing == null) "New Course" else "Edit Course") },
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("Course Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedLabelColor = ContentLightGrey,
                        focusedLabelColor = ContentLightGrey,
                        unfocusedTextColor = ContentWhite,
                        focusedTextColor = ContentWhite
                    )
                )
            }
            item {
                Text("Par per Hole", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ContentWhite)
            }
            items(parValues.size) { index ->
                HoleEditorRow(
                    holeNumber = index + 1,
                    par = parValues[index],
                    notes = notesValues[index],
                    canRemove = parValues.size > 1,
                    onParChange = { newPar ->
                        parValues = parValues.toMutableList().also { it[index] = newPar }
                    },
                    onNotesChange = { newNotes ->
                        notesValues = notesValues.toMutableList().also { it[index] = newNotes }
                    },
                    onRemove = {
                        parValues = parValues.toMutableList().also { it.removeAt(index) }
                        notesValues = notesValues.toMutableList().also { it.removeAt(index) }
                    }
                )
            }
            item {
                OutlinedButton(
                    onClick = {
                        parValues = parValues + 3
                        notesValues = notesValues + ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Hole")
                }
            }
            item {
                Button(
                    onClick = {
                        if (courseName.isNotBlank()) {
                            viewModel.saveCourse(courseName, parValues, notesValues)
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = courseName.isNotBlank()
                ) {
                    Text("Save Course")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HoleEditorRow(
    holeNumber: Int,
    par: Int,
    notes: String,
    canRemove: Boolean,
    onParChange: (Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Hole $holeNumber", modifier = Modifier.weight(1f), color = ContentWhite)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (par > 2) onParChange(par - 1) },
                    enabled = par > 2
                ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Text(
                    text = "Par $par",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.widthIn(min = 56.dp),
                )
                IconButton(
                    onClick = { if (par < 6) onParChange(par + 1) },
                    enabled = par < 6
                ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                IconButton(onClick = onRemove, enabled = canRemove) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove hole",
                        tint = if (canRemove) MaterialTheme.colorScheme.error
                               else ContentLightGrey.copy(alpha = 0.3f)
                    )
                }
            }
        }
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Hole rules / notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 8,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedLabelColor = ContentLightGrey,
                focusedLabelColor = ContentLightGrey,
                unfocusedTextColor = ContentWhite,
                focusedTextColor = ContentWhite
            )
        )
    }
}
