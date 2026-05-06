package com.verbum.universalis.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.verbum.universalis.ui.reader.Passage
import com.verbum.universalis.ui.reader.ReadingScreen
import com.verbum.universalis.ui.reader.ReadingViewModel

// Type aliases
typealias MassReadings = List<Pair<String, String>>
typealias PlanReadings = List<List<String>>

@Composable
fun ReadingCanvasScreen(
    initialBookId: Int? = null,
    initialChapter: Int? = null,
    massReadings: MassReadings = emptyList(),
    currentReadingIndex: Int = -1,
    planReadings: PlanReadings = emptyList(),
    currentPlanDayIndex: Int = -1,
    onNavigateNext: ((bookId: Int, chapter: Int, nextIndex: Int) -> Unit)? = null,
    onNavigateNextDay: ((bookId: Int, chapter: Int, nextDayIndex: Int) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    viewModel: ReadingViewModel = hiltViewModel()
) {
    LaunchedEffect(initialBookId, initialChapter) {
        if (initialBookId != null && initialChapter != null) {
            viewModel.setPassage(initialBookId, initialChapter)
        }
    }
    
    val currentPassage by viewModel.currentPassage.collectAsState()
    
    // Calculate next reading text for mass readings
    val nextMassReadingText = getNextMassReadingText(massReadings, currentReadingIndex)
    
    // Calculate next day text for Bible in a Year
    val nextDayText = getNextDayText(planReadings, currentPlanDayIndex)
    
    ReadingScreen(
        viewModel = viewModel,
        initialBookId = initialBookId,
        initialChapter = initialChapter,
        showNextReading = massReadings.isNotEmpty() && currentReadingIndex >= 0,
        nextReadingText = nextMassReadingText,
        onNextReadingClick = {
            val nextIdx = currentReadingIndex + 1
            if (nextIdx < massReadings.size) {
                val (_, nextRef) = massReadings[nextIdx]
                parseAndNavigate(nextRef) { bookId, chapter ->
                    onNavigateNext?.invoke(bookId, chapter, nextIdx)
                }
            }
        },
        showNextDay = planReadings.isNotEmpty() && currentPlanDayIndex >= 0,
        nextDayText = nextDayText,
        onNextDayClick = {
            val nextDayIdx = currentPlanDayIndex + 1
            if (nextDayIdx < planReadings.size) {
                val nextDayReadings = planReadings[nextDayIdx]
                if (nextDayReadings.isNotEmpty()) {
                    parseAndNavigate(nextDayReadings[0]) { bookId, chapter ->
                        onNavigateNextDay?.invoke(bookId, chapter, nextDayIdx)
                    }
                }
            }
        },
        onBack = onBack
    )
}

private fun getNextMassReadingText(readings: MassReadings, currentIndex: Int): String? {
    val nextIdx = currentIndex + 1
    if (nextIdx >= readings.size) return null
    val (type, _) = readings[nextIdx]
    val label = when (type) {
        "firstReading" -> "Next: 1st Reading"
        "psalm" -> "Next: Psalm"
        "gospel" -> "Next: Gospel"
        else -> "Next Reading"
    }
    return "$label →"
}

private fun getNextDayText(allDays: PlanReadings, currentDayIndex: Int): String? {
    val nextDayIdx = currentDayIndex + 1
    if (nextDayIdx >= allDays.size) return null
    return "Next Day →"
}

private fun parseAndNavigate(ref: String, onNavigate: (bookId: Int, chapter: Int) -> Unit) {
    try {
        // Handle "Genesis 1:1" or "GEN.1" format
        val parts = if (ref.contains(":")) {
            ref.split(":")
        } else {
            ref.split(".")
        }
        
        if (parts.isNotEmpty()) {
            val bookPart = parts[0].trim()
            val chapterPart = if (parts.size > 1) parts[1].split("-")[0].split(",")[0].trim() else "1"
            
            val bookId = Passage.BOOK_NAME_TO_ID[bookPart] ?: 
                Passage.BOOK_NAME_TO_ID.entries.find { it.key.contains(bookPart, ignoreCase = true) }?.value
            val chapter = chapterPart.toIntOrNull()
            
            if (bookId != null && chapter != null) {
                onNavigate(bookId, chapter)
            }
        }
    } catch (e: Exception) { /* ignore */ }
}
