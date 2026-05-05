package com.verbun.universalis.ui.navigation

sealed class Route(val route: String) {
    object Dashboard : Route("dashboard")
    object ReadingCanvas : Route("reading_canvas")
    object InterlinearReader : Route("interlinear_reader/{verseId}") {
        fun createRoute(verseId: Int?): String {
            return if (verseId == null) "interlinear_reader/0" else "interlinear_reader/$verseId"
        }
    }
    object ReadingPlans : Route("reading_plans")
    object Settings : Route("settings")
}
