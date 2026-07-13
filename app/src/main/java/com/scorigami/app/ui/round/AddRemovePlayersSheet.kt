package com.scorigami.app.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scorigami.app.ui.theme.ContentLightGrey
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.shared.db.entity.PlayerEntity

/**
 * Add/remove-players sheet body — styled to match RoundSetupScreen's filled-card
 * language (SectionCard sections, pill chips, bold white field labels). The hosting
 * ModalBottomSheet uses the default container (surfaceContainerLow grey).
 */
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard(label = "Players") {
            currentPlayers.forEachIndexed { index, player ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        player.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ContentWhite
                    )
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
                if (index < currentPlayers.lastIndex) HorizontalDivider()
            }
        }

        if (suggestions.isNotEmpty()) {
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
                            Text(
                                text = player.name,
                                color = ContentWhite,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .clickable { onAddPlayer(player.name) }
                                    .padding(horizontal = 18.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        Column {
            SectionTitle("Add Player")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
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
        }
    }
}
