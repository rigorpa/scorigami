package com.scorigami.app.ui.round

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scorigami.shared.db.entity.PlayerEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AddRemovePlayersSheet(
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
        Text("Players", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
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
                label = { Text("Add Player") },
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
                Icon(Icons.Default.Add, contentDescription = "Add Player")
            }
        }

        if (suggestions.isNotEmpty()) {
            Text(
                "Previous",
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
