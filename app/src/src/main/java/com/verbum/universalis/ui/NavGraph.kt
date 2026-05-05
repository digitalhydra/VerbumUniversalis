package com.verbum.universalis.ui

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import com.verbum.universalis.ui.dashboard.DashboardScreen
import com.verbum.universalis.ui.reader.ReadingCanvasScreen
import com.verbum.universalis.ui.reader.InterlinearReaderScreen
import com.verbum.universalis.ui.plans.ReadingPlansScreen
import com.verbum.universalis.ui.settings.SettingsScreen

sealed class Route(val path: String) {
    object Dashboard : Route("dashboard")
    object ReadingCanvas : Route("reading_canvas")
    object InterlinearReader : Route("interlinear_reader")
    object ReadingPlans : Route("reading_plans")
    object Settings : Route("settings")
}

@Composable
fun VerbumNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Route.Dashboard.path
    ) {
        composable(Route.Dashboard.path) { DashboardScreen(navController) }
        composable(Route.ReadingCanvas.path) { ReadingCanvasScreen(navController) }
        composable(Route.InterlinearReader.path) { InterlinearReaderScreen(navController) }
        composable(Route.ReadingPlans.path) { ReadingPlansScreen(navController) }
        composable(Route.Settings.path) { SettingsScreen(navController) }
    }
}
