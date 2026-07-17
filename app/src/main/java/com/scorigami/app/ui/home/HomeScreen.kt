package com.scorigami.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scorigami.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scorigami.app.BuildConfig
import com.scorigami.app.ui.theme.ContentLightGrey
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.CoursesGradientEnd
import com.scorigami.app.ui.theme.CoursesGradientStart
import com.scorigami.app.ui.theme.DefaultCardBackground
import com.scorigami.app.ui.theme.DisabledButtonGradientEnd
import com.scorigami.app.ui.theme.DisabledButtonGradientStart
import com.scorigami.app.ui.theme.HistoryGradientEnd
import com.scorigami.app.ui.theme.HistoryGradientStart
import com.scorigami.app.ui.theme.NewRoundGradientEnd
import com.scorigami.app.ui.theme.NewRoundGradientStart
import com.scorigami.app.ui.theme.ResumeGradientEnd
import com.scorigami.app.ui.theme.ResumeGradientStart
import com.scorigami.app.ui.theme.ScorigamiFont
import com.scorigami.app.viewmodel.RoundViewModel

@Composable
fun HomeScreen(
    onStartRound: () -> Unit,
    onResume: () -> Unit,
    onCourses: () -> Unit,
    onHistory: () -> Unit,
    viewModel: RoundViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Scorigami logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(28.dp))
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "SCORIGAMI",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Normal,
                    color = ScorigamiFont
                )
                Text(
                    text = "A Frolf Scorecard",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ContentLightGrey
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Widget-style container: one wide rounded card holding the option cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(DefaultCardBackground)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.isActive) {
                        HomeOptionCard(
                            title = "Resume Round",
                            description = "Continue your round in progress",
                            icon = Icons.Default.PlayArrow,
                            gradient = Brush.horizontalGradient(listOf(ResumeGradientStart, ResumeGradientEnd)),
                            onClick = onResume
                        )
                    }
                    HomeOptionCard(
                        title = "New Round",
                        description = "Start a round — pick your course and players",
                        icon = Icons.Default.Adjust,
                        gradient = Brush.horizontalGradient(listOf(NewRoundGradientStart, NewRoundGradientEnd)),
                        onClick = onStartRound,
                        enabled = !state.isActive
                    )
                    HomeOptionCard(
                        title = "My Courses",
                        description = "Manage your courses. Input a new course",
                        icon = Icons.Default.Park,
                        gradient = Brush.horizontalGradient(listOf(CoursesGradientStart, CoursesGradientEnd)),
                        onClick = onCourses
                    )
                    HomeOptionCard(
                        title = "Round History",
                        description = "Review past rounds and scorecards",
                        icon = Icons.Default.History,
                        gradient = Brush.horizontalGradient(listOf(HistoryGradientStart, HistoryGradientEnd)),
                        onClick = onHistory
                    )
                }
            }

            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * One tappable option inside the home widget card: identity-gradient background,
 * title + smaller description on the left, action icon on the right. Disabled
 * (New Round while a round is active) falls back to the muted disabled gradient
 * with half-alpha content.
 */
@Composable
private fun HomeOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val background = if (enabled) gradient else Brush.horizontalGradient(
        listOf(DisabledButtonGradientStart, DisabledButtonGradientEnd)
    )
    val contentColor = if (enabled) ContentWhite else ContentWhite.copy(alpha = 0.5f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = contentColor.alpha * 0.8f)
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
