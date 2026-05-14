package com.verbum.universalis.ui.reader

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
import com.verbum.universalis.ui.navigation.MassReadings
import com.verbum.universalis.ui.navigation.PlanReadings
import com.verbum.universalis.ui.reader.Passage
import com.verbum.universalis.ui.reader.ReadingScreen
import com.verbum.universalis.ui.reader.ReadingViewModel


@Composable
fun ReadingCanvasScreen(
    initialBookId: Int? = null,
    initialChapter: Int? = null,
    initialVerse: Int? = null,
    initialFilter: String? = null,
    massReadings: MassReadings = emptyList(),
    currentReadingIndex: Int = -1,
    planReadings: PlanReadings = emptyList(),
    currentPlanDayIndex: Int = -1,
    onNavigateNext: ((bookId: Int, chapter: Int, verse: Int?, filter: String?, nextIndex: Int) -> Unit)? = null,
    onNavigateNextDay: ((bookId: Int, chapter: Int, verse: Int?, filter: String?, nextDayIndex: Int) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    viewModel: ReadingViewModel = hiltViewModel()
) {
    val currentPassage by viewModel.currentPassage.collectAsState()
    
    // Calculate next reading text for mass readings
    val nextMassReadingText = getNextMassReadingText(massReadings, currentReadingIndex)
    
    // Calculate next day text for Bible in a Year
    val nextDayText = getNextDayText(planReadings, currentPlanDayIndex)
    
    ReadingScreen(
        viewModel = viewModel,
        initialBookId = initialBookId,
        initialChapter = initialChapter,
        initialVerse = initialVerse,
        initialFilter = initialFilter,
        showNextReading = massReadings.isNotEmpty() && currentReadingIndex >= 0,
        nextReadingText = nextMassReadingText,
        onNextReadingClick = {
            val nextIdx = currentReadingIndex + 1
            if (nextIdx < massReadings.size) {
                val (_, nextRef) = massReadings[nextIdx]
                parseAndNavigate(nextRef) { bookId, chapter, verse, filter ->
                    onNavigateNext?.invoke(bookId, chapter, verse, filter, nextIdx)
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
                    parseAndNavigate(nextDayReadings[0]) { bookId, chapter, verse, filter ->
                        onNavigateNextDay?.invoke(bookId, chapter, verse, filter, nextDayIdx)
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

private fun parseAndNavigate(ref: String, onNavigate: (bookId: Int, chapter: Int, verse: Int?, filter: String?) -> Unit) {
    try {
        val parts = ref.split(":")
        if (parts.size >= 2) {
            val bookAndChapter = parts[0].trim()
            val filterPart = parts[1].trim()
            
            val bookName = if (bookAndChapter.contains(" ")) bookAndChapter.substringBeforeLast(" ").trim() else bookAndChapter
            val chapterString = if (bookAndChapter.contains(" ")) bookAndChapter.substringAfterLast(" ").trim() else "1"
            
            val bookId = Passage.BOOK_NAME_TO_ID[bookName] ?: 
                        Passage.BOOK_NAME_TO_ID.entries.find { it.key.contains(bookName, ignoreCase = true) }?.value
            val chapter = chapterString.toIntOrNull()
            
            // If it's a simple verse like "1", filter is null and verse is 1.
            // If it's complex like "1-11, 13-15", filter is the whole string.
            val verse = filterPart.split("-")[0].split(",")[0].trim().toIntOrNull()
            val filter = if (filterPart.contains("-") || filterPart.contains(",")) filterPart else null
            
            if (bookId != null && chapter != null) {
                onNavigate(bookId, chapter, verse, filter)
            }
        } else {
            // Handle "GEN.1" or "Genesis 1"
            val refText = ref.trim().replace(".", " ")
            val bookName = if (refText.contains(" ")) refText.substringBeforeLast(" ").trim() else refText
            val chapterString = if (refText.contains(" ")) refText.substringAfterLast(" ").trim() else "1"
            
            val bookId = Passage.BOOK_NAME_TO_ID[bookName] ?: 
                        Passage.BOOK_NAME_TO_ID.entries.find { it.key.contains(bookName, ignoreCase = true) }?.value
            val chapter = chapterString.toIntOrNull()
            
            if (bookId != null && chapter != null) {
                onNavigate(bookId, chapter, null, null)
            }
        }
    } catch (e: Exception) { /* ignore */ }
}
