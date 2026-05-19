package com.verbum.universalis.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun DashboardDayContentPreview() {
    val mockEntry = com.verbum.universalis.data.entities.DailyMassReadingEntry(
        date = "2026-05-14",
        season = "Easter Time",
        readings = listOf(
            com.verbum.universalis.data.entities.DailyReadingRef("firstReading", "Acts 1:1-11"),
            com.verbum.universalis.data.entities.DailyReadingRef("psalm", "Psalm 47"),
            com.verbum.universalis.data.entities.DailyReadingRef("gospel", "John 17:1-11")
        )
    )
    
    DashboardDayContent(
        massReadingsEntry = mockEntry,
        celebration = null,
        currentDayReadings = emptyList(),
        currentDayIndex = 0,
        progressMap = emptyMap(),
        currentPlan = null,
        accentColor = Color(0xFF388E3C),
        isCurrentDatePage = true,
        onNavigateToReading = { _, _ -> },
        onNavigateToMassReading = { _, _, _, _, _ -> },
        onNavigateToPlanReading = { _, _, _, _, _ -> },
        onNavigateToPlanTracking = {},
        onTogglePlanReadingComplete = { _, _, _ -> }
    )
}
