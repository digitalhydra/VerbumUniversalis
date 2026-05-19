package com.verbum.universalis.data.repository

import android.content.Context
import com.verbum.universalis.data.entities.DailyMassReadingEntry
import com.verbum.universalis.data.entities.DailyReadingRef
import com.verbum.universalis.data.entities.LiturgicalReadingEntry
import com.verbum.universalis.data.entities.Celebration
import com.verbum.universalis.data.entities.UnifiedReadingEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
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
        loadMassReadingsData("US")
        loadLiturgicalCalendarData()
    }

    fun setCalendar(calendar: String) {
        loadMassReadingsData(calendar)
    }

    private fun loadMassReadingsData(calendar: String) {
        try {
            val filename = when (calendar) {
                "CO" -> "plans/daily-mass-readings-colombia.json"
                "RO" -> "plans/daily-mass-readings-rome.json"
                else -> "plans/daily-mass-readings.json"
            }
            val assetJson = try {
                context.assets.open(filename).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                null
            }

            if (assetJson != null) {
                val unifiedData = json.decodeFromString<List<UnifiedReadingEntry>>(assetJson)
                _massReadings.value = unifiedData.map { entry ->
                    DailyMassReadingEntry(
                        date = entry.date,
                        monthDay = entry.monthDay,
                        season = entry.season,
                        celebrationName = entry.celebration,
                        readings = entry.readings.map { (type, ref) ->
                            DailyReadingRef(type, ref)
                        }
                    )
                }
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

    fun isDataLoaded(): Boolean {
        return _massReadings.value.isNotEmpty()
    }

    fun parseReference(ref: String): Triple<String, Int, IntRange>? {
        val parts = ref.split(":")
        if (parts.size < 2) return null

        val bookPart = parts[0].trim()
        val refPart = parts[1].trim()

        val bookWords = bookPart.split(" ")
        val bookCode = if (bookWords.isNotEmpty()) bookWords.last() else return null

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
