package com.verbum.universalis.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.verbum.universalis.ui.dashboard.DashboardScreen
import com.verbum.universalis.ui.reader.InterlinearReaderScreen
import com.verbum.universalis.ui.reader.ReadingCanvasScreen
import com.verbum.universalis.ui.plans.ReadingPlansScreen
import com.verbum.universalis.ui.settings.SettingsScreen

// Type alias for mass readings
typealias MassReadings = List<Pair<String, String>>

@Composable
fun VerbumNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Route.Dashboard.route
    ) {
        composable(Route.Dashboard.route) {
            DashboardScreen(
                onNavigateToReading = { bookId, chapter ->
                    navController.navigate(Route.ReadingCanvas.createRoute(bookId, chapter))
                },
                onNavigateToMassReading = { bookId, chapter, readings, index ->
                    navController.navigate(
                        Route.ReadingCanvas.createRouteWithReadings(bookId, chapter, readings, index)
                    )
                }
            )
        }
        
        composable(
            route = Route.ReadingCanvas.routeWithArgs,
            arguments = listOf(
                navArgument("bookId") { type = NavType.IntType },
                navArgument("chapter") { type = NavType.IntType },
                navArgument("readings") { type = NavType.StringType; nullable = true },
                navArgument("currentIndex") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getInt("bookId")
            val chapter = backStackEntry.arguments?.getInt("chapter")
            val readingsJson = backStackEntry.arguments?.getString("readings")
            val currentIndex = backStackEntry.arguments?.getInt("currentIndex") ?: -1
            
            // Parse readings JSON back to List<Pair<String, String>>
            val readings = parseReadingsJson(readingsJson)
            
            ReadingCanvasScreen(
                initialBookId = bookId,
                initialChapter = chapter,
                massReadings = readings,
                currentReadingIndex = currentIndex,
                onNavigateNext = { nextBookId, nextChapter, nextIdx ->
                    navController.navigate(
                        Route.ReadingCanvas.createRouteWithReadings(nextBookId, nextChapter, readings, nextIdx)
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.InterlinearReader.route) { backStackEntry ->
            val verseId = backStackEntry.arguments?.getString("verseId")?.toIntOrNull()
            InterlinearReaderScreen(
                verseId = verseId,
                onBack = { navController.popBackStack() },
                onReferenceClick = { ref ->
                    val parts = ref.split(".")
                    if (parts.size >= 2) {
                        val bookCode = parts[0]
                        val chapter = parts[1].toIntOrNull()
                        if (chapter != null) {
                            navController.navigate(Route.ReadingCanvas.createRoute(bookCode, chapter))
                        }
                    }
                }
            )
        }
        composable(Route.ReadingPlans.route) { ReadingPlansScreen() }
        composable(Route.Settings.route) { SettingsScreen() }
    }
}

// Parse readings JSON string back to List<Pair<String, String>>
private fun parseReadingsJson(json: String?): MassReadings {
    if (json.isNullOrEmpty()) return emptyList()
    return try {
        json.removeSurrounding("[", "]")
            .split("),(")
            .mapNotNull { item ->
                val parts = item.replace("(", "").replace(")", "").split(",")
                if (parts.size >= 2) {
                    val type = parts[0].trim().removeSurrounding("\"")
                    val ref = parts[1].trim().removeSurrounding("\"")
                    type to ref
                } else null
            }
    } catch (e: Exception) {
        emptyList()
    }
}
