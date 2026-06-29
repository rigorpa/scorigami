package com.scorigami.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.scorigami.app.ui.course.CourseEditorScreen
import com.scorigami.app.ui.course.CourseListScreen
import com.scorigami.app.ui.history.HistoryScreen
import com.scorigami.app.ui.history.RoundDetailScreen
import com.scorigami.app.ui.home.HomeScreen
import com.scorigami.app.ui.round.RoundReviewScreen
import com.scorigami.app.ui.round.RoundSetupScreen
import com.scorigami.app.ui.round.ScorecardScreen
import com.scorigami.shared.sync.SgCourse

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CourseList : Screen("course_list")
    object CourseEditor : Screen("course_editor?courseId={courseId}") {
        fun createRoute(courseId: Long = -1L) = "course_editor?courseId=$courseId"
    }
    object RoundSetup : Screen("round_setup")
    object Scorecard : Screen("scorecard")
    object RoundReview : Screen("round_review")
    object History : Screen("history")
    object RoundDetail : Screen("round_detail/{roundId}") {
        fun createRoute(roundId: Long) = "round_detail/$roundId"
    }
}

@Composable
fun AppNavigation(
    pendingImport: MutableState<SgCourse?> = mutableStateOf(null)
) {
    val navController = rememberNavController()

    // When a .sgcourse file is opened, navigate to the course list for import
    LaunchedEffect(pendingImport.value) {
        if (pendingImport.value != null) {
            navController.navigate(Screen.CourseList.route) {
                popUpTo(Screen.Home.route)
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onStartRound = { navController.navigate(Screen.RoundSetup.route) },
                onResume = { navController.navigate(Screen.Scorecard.route) },
                onCourses = { navController.navigate(Screen.CourseList.route) },
                onHistory = { navController.navigate(Screen.History.route) }
            )
        }

        composable(Screen.CourseList.route) {
            CourseListScreen(
                onBack = { navController.popBackStack() },
                onCreateCourse = { navController.navigate(Screen.CourseEditor.createRoute()) },
                onEditCourse = { id -> navController.navigate(Screen.CourseEditor.createRoute(id)) },
                pendingImport = pendingImport
            )
        }

        composable(
            route = Screen.CourseEditor.route,
            arguments = listOf(navArgument("courseId") {
                type = NavType.LongType; defaultValue = -1L
            })
        ) {
            CourseEditorScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.RoundSetup.route) {
            RoundSetupScreen(
                onBack = { navController.popBackStack() },
                onRoundStarted = {
                    navController.navigate(Screen.Scorecard.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.Scorecard.route) {
            ScorecardScreen(
                onEndRound = { navController.navigate(Screen.RoundReview.route) },
                onBack = { navController.popBackStack() },
                onCancelRound = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.RoundReview.route) {
            RoundReviewScreen(
                onConfirm = {
                    navController.navigate(Screen.History.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onRoundDetail = { id -> navController.navigate(Screen.RoundDetail.createRoute(id)) }
            )
        }

        composable(
            route = Screen.RoundDetail.route,
            arguments = listOf(navArgument("roundId") { type = NavType.LongType })
        ) {
            RoundDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}

