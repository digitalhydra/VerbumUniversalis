package com.verbum.universalis.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.verbum.universalis.ui.dashboard.DashboardScreen
import com.verbum.universalis.ui.reader.ReadingCanvasScreen
import com.verbum.universalis.ui.reader.InterlinearReaderScreen
import com.verbum.universalis.ui.plans.ReadingPlansScreen
import com.verbum.universalis.ui.settings.SettingsScreen

@Composable
fun VerbumNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Route.Dashboard.route
    ) {
        composable(Route.Dashboard.route) { DashboardScreen() }
        composable(Route.ReadingCanvas.route) { ReadingCanvasScreen() }
        composable(Route.InterlinearReader.route) { backStackEntry ->
            val verseId = backStackEntry.arguments?.getString("verseId")?.toIntOrNull()
            InterlinearReaderScreen(
                verseId = verseId,
                onBack = { /* TODO: Navigate back */ }
            )
        }
        composable(Route.ReadingPlans.route) { ReadingPlansScreen() }
        composable(Route.Settings.route) { SettingsScreen() }
    }
}
