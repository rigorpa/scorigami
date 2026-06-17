package com.scorigami.app.ui.round

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal fun formatVsPar(vsPar: Int): String = when {
    vsPar < 0 -> "$vsPar"
    vsPar == 0 -> "E"
    else -> "+$vsPar"
}

@Composable
internal fun vsParColor(vsPar: Int): Color = when {
    vsPar < 0 -> MaterialTheme.colorScheme.primary
    vsPar == 0 -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.error
}
