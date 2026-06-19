package com.scorigami.wear.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Dialog

@Composable
internal fun EndRoundPromptDialog(onDismiss: () -> Unit) {
    Dialog(
        showDialog = true,
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "End round on the phone app",
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
