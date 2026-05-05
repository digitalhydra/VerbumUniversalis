package com.verbun.universalis.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.verbun.universalis.ui.dashboard.DashboardScreen
import com.verbun.universalis.ui.reader.ReadingCanvasScreen
import com.verbun.universalis.ui.reader.InterlinearReaderScreen
import com.verbun.universalis.ui.plans.ReadingPlansScreen
import com.verbun.universalis.ui.settings.SettingsScreen

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
