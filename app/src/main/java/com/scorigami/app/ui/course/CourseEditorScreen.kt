package com.scorigami.app.ui.course

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    var holeCount by remember { mutableStateOf("18") }
    var parValues by remember { mutableStateOf(List(18) { 3 }) }
    var notesValues by remember { mutableStateOf(List(18) { "" }) }

    LaunchedEffect(existing) {
        if (!initialized && existing != null) {
            courseName = existing!!.course.name
            holeCount = existing!!.course.holeCount.toString()
            val sorted = existing!!.holes.sortedBy { it.number }
            parValues = sorted.map { it.par }
            notesValues = sorted.map { it.notes ?: "" }
            initialized = true
        } else if (!initialized && existing == null) {
            initialized = true
        }
    }

    val count = holeCount.toIntOrNull()?.coerceIn(1, 36) ?: 18
    LaunchedEffect(count) {
        if (parValues.size != count) {
            parValues = List(count) { i -> parValues.getOrElse(i) { 3 } }
            notesValues = List(count) { i -> notesValues.getOrElse(i) { "" } }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New Course" else "Edit Course") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = holeCount,
                    onValueChange = { holeCount = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Number of Holes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                Text("Par per Hole", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(parValues.size) { index ->
                HoleEditorRow(
                    holeNumber = index + 1,
                    par = parValues[index],
                    notes = notesValues[index],
                    onParChange = { newPar ->
                        parValues = parValues.toMutableList().also { it[index] = newPar }
                    },
                    onNotesChange = { newNotes ->
                        notesValues = notesValues.toMutableList().also { it[index] = newNotes }
                    }
                )
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
    onParChange: (Int) -> Unit,
    onNotesChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Hole $holeNumber", modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (par > 3) onParChange(par - 1) },
                    enabled = par > 3
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
            }
        }
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Hole rules / notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 8
        )
    }
}
