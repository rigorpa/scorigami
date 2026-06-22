package com.scorigami.wear.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Dialog
import com.scorigami.shared.sync.PlayerState
import com.scorigami.wear.ui.theme.ContentWhite
import com.scorigami.wear.ui.theme.HoleNumberColor

@Composable
internal fun TeeOrderDialog(
    players: List<PlayerState>,
    onDismiss: () -> Unit
) {
    Dialog(
        showDialog = true,
        onDismissRequest = onDismiss
    ) {
        Card(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Tee Order",
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.Bold,
                    color = HoleNumberColor,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                players.forEachIndexed { i, player ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${i + 1}.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.width(18.dp)
                        )
                        Text(
                            player.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = ContentWhite,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
