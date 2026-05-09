package com.verbum.universalis.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.verbum.universalis.ui.reader.Passage

// Type alias for mass readings: List of (readingType, reference)
typealias MassReadings = List<Pair<String, String>>

// Type alias for plan readings: List of days, each day is list of references
typealias PlanReadings = List<List<String>>

sealed class Route(val route: String) {
    object Dashboard : Route("dashboard")
    
    object ReadingCanvas : Route("reading_canvas") {
        const val routeWithArgs = "reading_canvas?bookId={bookId}&chapter={chapter}&verse={verse}&readings={readings}&currentIndex={currentIndex}&plandays={plandays}&planday={planday}"
        
        fun createRoute(bookId: Int? = null, chapter: Int? = null, verse: Int? = null): String {
            return when {
                bookId != null && chapter != null -> {
                    val base = "reading_canvas?bookId=$bookId&chapter=$chapter"
                    if (verse != null) "$base&verse=$verse" else base
                }
                else -> "reading_canvas"
            }
        }
        
        fun createRouteWithReadings(
            bookId: Int, 
            chapter: Int, 
            verse: Int?,
            readings: MassReadings, 
            currentIndex: Int
        ): String {
            val readingsStr = if (readings.isNotEmpty()) {
                readings.joinToString("|") { "${it.first},${it.second}" }
            } else ""
            val base = "reading_canvas?bookId=$bookId&chapter=$chapter"
            val withVerse = if (verse != null) "$base&verse=$verse" else base
            return "$withVerse&readings=$readingsStr&currentIndex=$currentIndex"
        }
        
        fun createRouteWithPlan(
            bookId: Int, 
            chapter: Int, 
            verse: Int?,
            allDays: PlanReadings, 
            currentDayIndex: Int
        ): String {
            // Encode: day1ref1|day1ref2,day2ref1|day2ref2,...
            val daysStr = allDays.joinToString(",") { day ->
                day.joinToString("|")
            }
            val base = "reading_canvas?bookId=$bookId&chapter=$chapter"
            val withVerse = if (verse != null) "$base&verse=$verse" else base
            return "$withVerse&plandays=$daysStr&planday=$currentDayIndex"
        }
    }

    object InterlinearReader : Route("interlinear_reader/{verseId}?tab={tab}") {
        fun createRoute(verseId: Int?, tab: String = ""): String {
            val base = if (verseId == null) "interlinear_reader/0" else "interlinear_reader/$verseId"
            return if (tab.isNotEmpty()) "$base?tab=$tab" else base
        }
    }
    object ReadingPlans : Route("reading_plans")
    object Settings : Route("settings")
}

@Composable
fun VerbumNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Dashboard.route,
        modifier = modifier
    ) {
        composable(Route.Dashboard.route) {
            DashboardScreen(
                onNavigateToReading = { bookId, chapter ->
                    navController.navigate(Route.ReadingCanvas.createRoute(bookId, chapter))
                },
                onNavigateToMassReading = { bookId, chapter, verse, readings, index ->
                    navController.navigate(
                        Route.ReadingCanvas.createRouteWithReadings(bookId, chapter, verse, readings, index)
                    )
                },
                onNavigateToPlanReading = { bookId, chapter, verse, allDays, dayIndex ->
                    navController.navigate(
                        Route.ReadingCanvas.createRouteWithPlan(bookId, chapter, verse, allDays, dayIndex)
                    )
                }
            )
        }
        
        composable(
            route = Route.ReadingCanvas.routeWithArgs,
            arguments = listOf(
                navArgument("bookId") { type = NavType.IntType; defaultValue = -1 },
                navArgument("chapter") { type = NavType.IntType; defaultValue = -1 },
                navArgument("verse") { type = NavType.IntType; defaultValue = -1 },
                navArgument("readings") { type = NavType.StringType; nullable = true; defaultValue = "" },
                navArgument("currentIndex") { type = NavType.IntType; defaultValue = -1 },
                navArgument("plandays") { type = NavType.StringType; nullable = true; defaultValue = "" },
                navArgument("planday") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getInt("bookId").takeIf { it != -1 }
            val chapter = backStackEntry.arguments?.getInt("chapter").takeIf { it != -1 }
            val verse = backStackEntry.arguments?.getInt("verse").takeIf { it != -1 }
            val readingsStr = backStackEntry.arguments?.getString("readings") ?: ""
            val currentIndex = backStackEntry.arguments?.getInt("currentIndex") ?: -1
            val planDaysStr = backStackEntry.arguments?.getString("plandays") ?: ""
            val planDay = backStackEntry.arguments?.getInt("planday") ?: -1
            
            val massReadings = parseReadingsString(readingsStr)
            val planReadings = parsePlanReadingsString(planDaysStr)
            
            ReadingCanvasScreen(
                initialBookId = bookId,
                initialChapter = chapter,
                initialVerse = verse,
                massReadings = massReadings,
                currentReadingIndex = currentIndex,
                planReadings = planReadings,
                currentPlanDayIndex = planDay,
                onNavigateNext = { nextBookId, nextChapter, nextVerse, nextIdx ->
                    navController.navigate(
                        Route.ReadingCanvas.createRouteWithReadings(nextBookId, nextChapter, nextVerse, massReadings, nextIdx)
                    )
                },
                onNavigateNextDay = { nextBookId, nextChapter, nextVerse, nextDayIdx ->
                    navController.navigate(
                        Route.ReadingCanvas.createRouteWithPlan(nextBookId, nextChapter, nextVerse, planReadings, nextDayIdx)
                    )
                },
                onBack = { 
                    navController.popBackStack(Route.Dashboard.route, inclusive = false)
                }
            )
        }

        composable(
            route = Route.InterlinearReader.route,
            arguments = listOf(
                navArgument("verseId") { type = NavType.IntType },
                navArgument("tab") { type = NavType.StringType; nullable = true; defaultValue = "" }
            )
        ) { backStackEntry ->
            val verseId = backStackEntry.arguments?.getInt("verseId")
            val initialTab = backStackEntry.arguments?.getString("tab") ?: ""
            InterlinearReaderScreen(
                verseId = verseId,
                initialTab = initialTab,
                onBack = { navController.popBackStack() },
                onReferenceClick = { ref ->
                    val parts = ref.split(".")
                    if (parts.size >= 2) {
                        val bookCode = parts[0]
                        val chapter = parts[1].toIntOrNull()
                        if (chapter != null) {
                            val bookId = Passage.BOOK_NAME_TO_ID[bookCode] ?: Passage.BOOK_NAME_TO_ID.entries.find { 
                                it.key.equals(bookCode, ignoreCase = true) 
                            }?.value
                            navController.navigate(Route.ReadingCanvas.createRoute(bookId, chapter))
                        }
                    }
                }
            )
        }
        composable(Route.ReadingPlans.route) { 
            ReadingPlansScreen(onNavigateBack = { navController.popBackStack() }) 
        }
        composable(Route.Settings.route) { 
            SettingsScreen(onBack = { navController.popBackStack() }) 
        }
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
