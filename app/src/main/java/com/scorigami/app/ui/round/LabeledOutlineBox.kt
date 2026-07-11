package com.scorigami.app.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scorigami.app.ui.theme.ContentWhite

/**
 * Outlined container with a bold white label floating on the top border — mimics the
 * OutlinedTextField look so custom sections (Players, Previous Golfers) match text fields.
 * [labelPatchColor] must match the background the box sits on (screen or sheet) so the
 * label appears to notch the border line.
 */
@Composable
internal fun LabeledOutlineBox(
    label: String,
    labelPatchColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp) // room for the label to straddle the border
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            content = content
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ContentWhite,
            modifier = Modifier
                .padding(start = 12.dp)
                .background(labelPatchColor)
                .padding(horizontal = 4.dp)
        )
    }
}
