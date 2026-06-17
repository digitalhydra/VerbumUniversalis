package com.verbum.universalis.ui

import com.verbum.universalis.ui.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteTest {
    @Test
    fun `test routes paths`() {
        assertEquals("dashboard", Route.Dashboard.route)
        assertEquals("reading_canvas", Route.ReadingCanvas.route)
        assertEquals("interlinear_reader/{verseId}?tab={tab}", Route.InterlinearReader.route)
        assertEquals("reading_plans", Route.ReadingPlans.route)
        assertEquals("settings", Route.Settings.route)
    }
}
