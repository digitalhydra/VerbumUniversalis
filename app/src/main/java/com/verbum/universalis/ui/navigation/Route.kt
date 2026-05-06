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

// Type alias for mass readings: List of (readingType, reference)
typealias MassReadings = List<Pair<String, String>>

// Type alias for plan readings: List of days, each day is list of references
typealias PlanReadings = List<List<String>>

sealed class Route(val route: String) {
    object Dashboard : Route("dashboard")
    
    object ReadingCanvas : Route("reading_canvas") {
        const val routeWithArgs = "reading_canvas/{bookId}/{chapter}?readings={readings}&currentIndex={currentIndex}&plandays={plandays}&planday={planday}"
        
        fun createRoute(bookId: Int? = null, chapter: Int? = null): String {
            return if (bookId != null && chapter != null) {
                "reading_canvas/$bookId/$chapter"
            } else {
                "reading_canvas"
            }
        }
        
        fun createRouteWithReadings(
            bookId: Int, 
            chapter: Int, 
            readings: MassReadings, 
            currentIndex: Int
        ): String {
            val readingsStr = if (readings.isNotEmpty()) {
                readings.joinToString("|") { "${it.first},${it.second}" }
            } else ""
            return "reading_canvas/$bookId/$chapter?readings=$readingsStr&currentIndex=$currentIndex"
        }
        
        fun createRouteWithPlan(
            bookId: Int, 
            chapter: Int, 
            allDays: PlanReadings, 
            currentDayIndex: Int
        ): String {
            // Encode: day1ref1|day1ref2,day2ref1|day2ref2,...
            val daysStr = allDays.joinToString(",") { day ->
                day.joinToString("|")
            }
            return "reading_canvas/$bookId/$chapter?plandays=$daysStr&planday=$currentDayIndex"
        }
    }

    object InterlinearReader : Route("interlinear_reader/{verseId}") {
        fun createRoute(verseId: Int?): String {
            return if (verseId == null) "interlinear_reader/0" else "interlinear_reader/$verseId"
        }
    }
    object ReadingPlans : Route("reading_plans")
    object Settings : Route("settings")
}

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
                },
                onNavigateToPlanReading = { bookId, chapter, allDays, dayIndex ->
                    navController.navigate(
                        Route.ReadingCanvas.createRouteWithPlan(bookId, chapter, allDays, dayIndex)
                    )
                }
            )
        }
        
        composable(
            route = Route.ReadingCanvas.routeWithArgs,
            arguments = listOf(
                navArgument("bookId") { type = NavType.IntType },
                navArgument("chapter") { type = NavType.IntType },
                navArgument("readings") { type = NavType.StringType; nullable = true; defaultValue = "" },
                navArgument("currentIndex") { type = NavType.IntType; defaultValue = -1 },
                navArgument("plandays") { type = NavType.StringType; nullable = true; defaultValue = "" },
                navArgument("planday") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getInt("bookId")
            val chapter = backStackEntry.arguments?.getInt("chapter")
            val readingsStr = backStackEntry.arguments?.getString("readings") ?: ""
            val currentIndex = backStackEntry.arguments?.getInt("currentIndex") ?: -1
            val planDaysStr = backStackEntry.arguments?.getString("plandays") ?: ""
            val planDay = backStackEntry.arguments?.getInt("planday") ?: -1
            
            val massReadings = parseReadingsString(readingsStr)
            val planReadings = parsePlanReadingsString(planDaysStr)
            
            ReadingCanvasScreen(
                initialBookId = bookId,
                initialChapter = chapter,
                massReadings = massReadings,
                currentReadingIndex = currentIndex,
                planReadings = planReadings,
                currentPlanDayIndex = planDay,
                onNavigateNext = { nextBookId, nextChapter, nextIdx ->
                    navController.navigate(
                        Route.ReadingCanvas.createRouteWithReadings(nextBookId, nextChapter, massReadings, nextIdx)
                    )
                },
                onNavigateNextDay = { nextBookId, nextChapter, nextDayIdx ->
                    navController.navigate(
                        Route.ReadingCanvas.createRouteWithPlan(nextBookId, nextChapter, planReadings, nextDayIdx)
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

private fun parseReadingsString(str: String): MassReadings {
    if (str.isBlank()) return emptyList()
    return try {
        str.split("|").mapNotNull { item ->
            val parts = item.split(",")
            if (parts.size >= 2) parts[0].trim() to parts[1].trim() else null
        }
    } catch (e: Exception) { emptyList() }
}

private fun parsePlanReadingsString(str: String): PlanReadings {
    if (str.isBlank()) return emptyList()
    return try {
        str.split(",").map { day ->
            day.split("|").filter { it.isNotBlank() }
        }
    } catch (e: Exception) { emptyList() }
}
