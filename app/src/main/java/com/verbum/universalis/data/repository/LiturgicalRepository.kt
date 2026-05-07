package com.verbum.universalis.data.repository

import android.content.Context
import com.verbum.universalis.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class LiturgicalRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _calendarEntries = MutableStateFlow<List<LiturgicalReadingEntry>>(emptyList())
    val calendarEntries: StateFlow<List<LiturgicalReadingEntry>> = _calendarEntries.asStateFlow()

    private val _massReadings = MutableStateFlow<List<DailyMassReadingEntry>>(emptyList())
    val massReadings: StateFlow<List<DailyMassReadingEntry>> = _massReadings.asStateFlow()

    init {
        loadCalendarData()
        loadMassReadingsData()
    }

    private fun loadCalendarData() {
        try {
            val file = File(context.filesDir, "liturgical_calendar.json")
            if (file.exists()) {
                val text = file.readText()
                val entries = json.decodeFromString<List<LiturgicalReadingEntry>>(text)
                _calendarEntries.value = entries
            } else {
                val assetJson = context.assets?.open("liturgical_calendar.json")?.bufferedReader()?.use { it.readText() }
                if (assetJson != null) {
                    val entries = json.decodeFromString<List<LiturgicalReadingEntry>>(assetJson)
                    _calendarEntries.value = entries
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadMassReadingsData() {
        try {
            val year = java.time.Year.now().value
            val file = File(context.filesDir, "plans/readings-$year.json")
            if (file.exists()) {
                val text = file.readText()
                val data = json.decodeFromString<DailyMassReadings>(text)
                _massReadings.value = data.days ?: emptyList()
            } else {
                val assetJson = context.assets?.open("plans/readings-$year.json")?.bufferedReader()?.use { it.readText() }
                if (assetJson != null) {
                    val data = json.decodeFromString<DailyMassReadings>(assetJson)
                    _massReadings.value = data.days ?: emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Get today's liturgical entry
    fun getTodayCalendarEntry(): LiturgicalReadingEntry? {
        val today = java.time.LocalDate.now().toString() // "2026-05-05"
        return _calendarEntries.value.find { it.date == today }
    }

    // Get today's mass readings
    fun getTodayMassReadings(): DailyMassReadingEntry? {
        val today = java.time.LocalDate.now().toString()
        return _massReadings.value.find { it.date == today }
    }

    // Get entry for a specific date
    fun getCalendarEntryForDate(date: String): LiturgicalReadingEntry? {
        return _calendarEntries.value.find { it.date == date }
    }

    fun getMassReadingsForDate(date: String): DailyMassReadingEntry? {
        return _massReadings.value.find { it.date == date }
    }

    // Get all unique dates for day picker
    fun getAllDates(): List<String> {
        return _calendarEntries.value.map { it.date }
    }

    // Check if data is loaded
    fun isDataLoaded(): Boolean {
        return _calendarEntries.value.isNotEmpty() || _massReadings.value.isNotEmpty()
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
