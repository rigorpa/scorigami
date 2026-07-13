package com.scorigami.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.dropUnlessResumed
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

        // All navigation callbacks are wrapped in dropUnlessResumed: once a transition starts,
        // the departing screen's entry leaves RESUMED, so a second tap that lands on the
        // still-composed (fading) screen is dropped. Without this, a quick double-tap on a back
        // arrow fired popBackStack() twice — the second pop removed the Home start destination,
        // leaving an empty NavHost (blank navy windowBackground, no UI to interact with).
        composable(Screen.Home.route) {
            HomeScreen(
                onStartRound = dropUnlessResumed { navController.navigate(Screen.RoundSetup.route) },
                onResume = dropUnlessResumed { navController.navigate(Screen.Scorecard.route) },
                onCourses = dropUnlessResumed { navController.navigate(Screen.CourseList.route) },
                onHistory = dropUnlessResumed { navController.navigate(Screen.History.route) }
            )
        }

        composable(Screen.CourseList.route) {
            CourseListScreen(
                onBack = dropUnlessResumed { navController.popBackStack() },
                onCreateCourse = dropUnlessResumed { navController.navigate(Screen.CourseEditor.createRoute()) },
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
            CourseEditorScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }

        composable(Screen.RoundSetup.route) {
            RoundSetupScreen(
                onBack = dropUnlessResumed { navController.popBackStack() },
                onRoundStarted = dropUnlessResumed {
                    navController.navigate(Screen.Scorecard.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.Scorecard.route) {
            ScorecardScreen(
                onEndRound = dropUnlessResumed { navController.navigate(Screen.RoundReview.route) },
                onBack = dropUnlessResumed { navController.popBackStack() },
                onCancelRound = dropUnlessResumed {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.RoundReview.route) {
            RoundReviewScreen(
                onConfirm = dropUnlessResumed {
                    navController.navigate(Screen.History.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBack = dropUnlessResumed { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onBack = dropUnlessResumed { navController.popBackStack() },
                onRoundDetail = { id -> navController.navigate(Screen.RoundDetail.createRoute(id)) }
            )
        }

        composable(
            route = Screen.RoundDetail.route,
            arguments = listOf(navArgument("roundId") { type = NavType.LongType })
        ) {
            RoundDetailScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
    }
}

