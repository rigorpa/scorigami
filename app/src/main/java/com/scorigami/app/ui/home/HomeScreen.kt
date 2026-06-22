package com.scorigami.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
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
import com.scorigami.app.ui.theme.CoursesGradientEnd
import com.scorigami.app.ui.theme.CoursesGradientStart
import com.scorigami.app.ui.theme.DisabledButtonGradientEnd
import com.scorigami.app.ui.theme.DisabledButtonGradientStart
import com.scorigami.app.ui.theme.HistoryGradientEnd
import com.scorigami.app.ui.theme.HistoryGradientStart
import com.scorigami.app.ui.theme.NewRoundGradientEnd
import com.scorigami.app.ui.theme.NewRoundGradientStart
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.ResumeGradientEnd
import com.scorigami.app.ui.theme.ResumeGradientStart
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
                    .padding(horizontal = 56.dp),
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "A Frolf Scorecard",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(56.dp))

                if (state.isActive) {
                    HomeActionButton(
                        text = "Resume Round",
                        icon = Icons.Default.PlayArrow,
                        onClick = onResume,
                        gradient = Brush.horizontalGradient(listOf(ResumeGradientStart, ResumeGradientEnd))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                HomeActionButton(
                    text = "New Round",
                    icon = Icons.Default.Adjust,
                    onClick = onStartRound,
                    gradient = Brush.horizontalGradient(listOf(NewRoundGradientStart, NewRoundGradientEnd)),
                    enabled = !state.isActive
                )
                Spacer(modifier = Modifier.height(12.dp))
                HomeActionButton(
                    text = "My Courses",
                    icon = Icons.Default.Park,
                    onClick = onCourses,
                    gradient = Brush.horizontalGradient(listOf(CoursesGradientStart, CoursesGradientEnd))
                )
                Spacer(modifier = Modifier.height(12.dp))
                HomeActionButton(
                    text = "Round History",
                    icon = Icons.Default.History,
                    onClick = onHistory,
                    gradient = Brush.horizontalGradient(listOf(HistoryGradientStart, HistoryGradientEnd))
                )
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

@Composable
private fun HomeActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    gradient: Brush,
    modifier: Modifier = Modifier.fillMaxWidth().height(56.dp),
    enabled: Boolean = true,
) {
    val activeGradient = if (enabled) gradient else Brush.horizontalGradient(
        listOf(DisabledButtonGradientStart, DisabledButtonGradientEnd)
    )
    Button(
        onClick = onClick,
        modifier = modifier.background(activeGradient, RoundedCornerShape(percent = 50)),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = ContentWhite,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = ContentWhite.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
    }
}
