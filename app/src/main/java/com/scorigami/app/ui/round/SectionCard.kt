package com.scorigami.app.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.DarkGradientEnd
import com.scorigami.app.ui.theme.DarkGradientStart

/** Dark gradient shared by the setup-screen section cards and fields (top bar stays blue). */
internal val SectionCardGradient = Brush.horizontalGradient(
    listOf(DarkGradientStart, DarkGradientEnd)
)

/** Bold white section title shown above a bubble (SectionCard or a gradient field). */
@Composable
internal fun SectionTitle(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = ContentWhite,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

/**
 * Filled section container (no border): gradient rounded card (top-bar blue) with its bold
 * white title sitting above the bubble. Used by RoundSetupScreen and AddRemovePlayersSheet.
 */
@Composable
internal fun SectionCard(
    label: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(label)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SectionCardGradient)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            content = content
        )
    }
}

/**
 * Borderless style for the text fields that accompany [SectionCard] sections (Course
 * dropdown, Add Player). Containers are transparent so the [SectionCardGradient] painted
 * behind the field (via `Modifier.background(SectionCardGradient, shape)`) shows through.
 * Pair with `shape = RoundedCornerShape(12.dp)` on the field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun sectionFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    unfocusedLabelColor = ContentWhite,
    focusedLabelColor = ContentWhite,
    unfocusedTextColor = ContentWhite,
    focusedTextColor = ContentWhite
)
