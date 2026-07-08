package com.scorigami.wear.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material.MaterialTheme
import com.scorigami.wear.ui.EndRoundPromptScreen
import com.scorigami.wear.ui.NoRoundScreen
import com.scorigami.wear.ui.WearScorecardScreen
import com.scorigami.wear.viewmodel.WearViewModel

sealed class WearScreen(val route: String) {
    object Loading : WearScreen("loading")
    object Scorecard : WearScreen("scorecard")
    object EndRoundPrompt : WearScreen("end_round_prompt")
    object NoRound : WearScreen("no_round")
}

@Composable
fun WearNavigation(viewModel: WearViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // On cold start the Data Layer hasn't been read yet (loaded == false): start on a
    // blank Loading screen so NoRoundScreen never flashes before the round resolves.
    val startDestination = when {
        !uiState.loaded -> WearScreen.Loading.route
        uiState.roundState != null -> WearScreen.Scorecard.route
        else -> WearScreen.NoRound.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        composable(WearScreen.Loading.route) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background))
        }

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
                onNextHole = { viewModel.navigateToHole((uiState.currentHole + 1).coerceAtMost(roundState.totalHoles)) },
                onScoreChange = { playerId, throws ->
                    viewModel.sendScoreUpdate(
                        roundId = roundState.roundId,
                        playerId = playerId,
                        holeNumber = uiState.currentHole,
                        throws = throws
                    )
                },
                onStatChange = { playerId, stat, count ->
                    viewModel.sendStatUpdate(
                        roundId = roundState.roundId,
                        playerId = playerId,
                        holeNumber = uiState.currentHole,
                        stat = stat,
                        count = count
                    )
                },
                onJumpToHole = { hole -> viewModel.navigateToHole(hole) }
            )
        }

        composable(WearScreen.EndRoundPrompt.route) {
            EndRoundPromptScreen(onBack = { navController.popBackStack() })
        }
    }

    // Once the Data Layer has been read, route to the right top-level screen — and react
    // to later round start/end transitions. Skipped while still loading so the blank
    // Loading screen stays put until we actually know the round state.
    LaunchedEffect(uiState.loaded, uiState.roundState != null) {
        if (!uiState.loaded) return@LaunchedEffect
        val target = if (uiState.roundState != null) WearScreen.Scorecard.route else WearScreen.NoRound.route
        if (navController.currentDestination?.route != target) {
            navController.navigate(target) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
