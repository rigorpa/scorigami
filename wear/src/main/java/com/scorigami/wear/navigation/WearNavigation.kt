package com.scorigami.wear.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scorigami.wear.ui.EndRoundPromptScreen
import com.scorigami.wear.ui.NoRoundScreen
import com.scorigami.wear.ui.WearScorecardScreen
import com.scorigami.wear.viewmodel.WearViewModel

sealed class WearScreen(val route: String) {
    object Scorecard : WearScreen("scorecard")
    object EndRoundPrompt : WearScreen("end_round_prompt")
    object NoRound : WearScreen("no_round")
}

@Composable
fun WearNavigation(viewModel: WearViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val startDestination = if (uiState.roundState != null) WearScreen.Scorecard.route else WearScreen.NoRound.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(WearScreen.NoRound.route) {
            NoRoundScreen()
        }

        composable(WearScreen.Scorecard.route) {
            val roundState = uiState.roundState
            if (roundState == null) {
                NoRoundScreen()
                return@composable
            }
            WearScorecardScreen(
                roundState = roundState,
                currentHole = uiState.currentHole,
                onPrevHole = { viewModel.navigateToHole((uiState.currentHole - 1).coerceAtLeast(1)) },
                onNextHole = { viewModel.navigateToHole((uiState.currentHole + 1).coerceAtMost(roundState.totalHoles)) },
                onEndRound = { navController.navigate(WearScreen.EndRoundPrompt.route) },
                onScoreChange = { playerId, throws ->
                    viewModel.sendScoreUpdate(
                        roundId = roundState.roundId,
                        playerId = playerId,
                        holeNumber = uiState.currentHole,
                        throws = throws
                    )
                }
            )
        }

        composable(WearScreen.EndRoundPrompt.route) {
            EndRoundPromptScreen(onBack = { navController.popBackStack() })
        }
    }

    // React to round state changes: navigate to scorecard when a round starts
    LaunchedEffect(uiState.roundState != null) {
        if (uiState.roundState != null) {
            navController.navigate(WearScreen.Scorecard.route) {
                popUpTo(WearScreen.NoRound.route) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            navController.navigate(WearScreen.NoRound.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
