package com.verbum.universalis.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteTest {
    @Test
    fun `test routes paths`() {
        assertEquals("dashboard", Route.Dashboard.path)
        assertEquals("reading_canvas", Route.ReadingCanvas.path)
        assertEquals("interlinear_reader", Route.InterlinearReader.path)
        assertEquals("reading_plans", Route.ReadingPlans.path)
        assertEquals("settings", Route.Settings.path)
    }
}
