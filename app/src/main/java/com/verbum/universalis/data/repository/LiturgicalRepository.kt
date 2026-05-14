package com.verbum.universalis.data.repository

import android.content.Context
import com.verbum.universalis.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import java.io.File
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class LiturgicalRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _massReadings = MutableStateFlow<List<DailyMassReadingEntry>>(emptyList())
    val massReadings: StateFlow<List<DailyMassReadingEntry>> = _massReadings.asStateFlow()

    private val _liturgicalCalendar = MutableStateFlow<List<LiturgicalReadingEntry>>(emptyList())
    val liturgicalCalendar: StateFlow<List<LiturgicalReadingEntry>> = _liturgicalCalendar.asStateFlow()

    init {
        loadMassReadingsData()
        loadLiturgicalCalendarData()
    }

    private fun loadMassReadingsData() {
        try {
            // Priority: daily-mass-readings.json
            val filename = "plans/daily-mass-readings.json"
            val assetJson = try {
                context.assets.open(filename).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                null
            }

            if (assetJson != null) {
                val data = json.decodeFromString<DailyMassReadings>(assetJson)
                _massReadings.value = data.days
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLiturgicalCalendarData() {
        try {
            val filename = "liturgical_calendar.json"
            val assetJson = try {
                context.assets.open(filename).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                null
            }

            if (assetJson != null) {
                val data = json.decodeFromString<List<LiturgicalReadingEntry>>(assetJson)
                _liturgicalCalendar.value = data
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Get today's mass readings
    fun getTodayMassReadings(): DailyMassReadingEntry? {
        val today = java.time.LocalDate.now().toString()
        return _massReadings.value.find { it.date == today }
    }

    fun getMassReadingsForDate(date: String): DailyMassReadingEntry? {
        return _massReadings.value.find { it.date == date }
    }

    fun getCelebrationForDate(date: String): Celebration? {
        return _liturgicalCalendar.value.find { it.date == date }?.celebration
    }

    fun getAllDates(): List<String> {
        return _massReadings.value.map { it.date }
    }

    // Check if data is loaded
    fun isDataLoaded(): Boolean {
        return _massReadings.value.isNotEmpty()
    }

    // Parse reference string "GEN.1.1" to (bookCode, chapter, verseStart, verseEnd)
    fun parseReference(ref: String): Triple<String, Int, IntRange>? {
        // Format: "1 Thessalonians 1:1-5, 8b-10" (simplified)
        // For now, handle "GEN.1.1" or "1TH.1-5"
        val parts = ref.split(":")
        if (parts.size < 2) return null

        val bookPart = parts[0].trim()
        val refPart = parts[1].trim()

        // Extract book code (last word(s) before chapter)
        val bookWords = bookPart.split(" ")
        val bookCode = if (bookWords.isNotEmpty()) bookWords.last() else return null

        // Parse verse range
        val rangeParts = refPart.split("-")
        val startVerse = rangeParts[0].filter { it.isDigit() }.toIntOrNull() ?: return null
        val endVerse = if (rangeParts.size > 1) {
            rangeParts[1].filter { it.isDigit() }.toIntOrNull() ?: startVerse
        } else {
            startVerse
        }

        return Triple(bookCode, startVerse, IntRange(startVerse, endVerse))
    }
}
