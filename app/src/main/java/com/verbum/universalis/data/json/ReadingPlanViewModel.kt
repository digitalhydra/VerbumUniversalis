package com.verbum.universalis.data.json

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray

@HiltViewModel
class ReadingPlanViewModel @Inject constructor(
    private val fileManager: FileManager,
    private val app: Application
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _plansIndex = MutableStateFlow<ReadingPlanIndex?>(null)
    val plansIndex: StateFlow<ReadingPlanIndex?> = _plansIndex.asStateFlow()

    private val _currentPlanFile = MutableStateFlow("bible-in-a-year.json")
    val currentPlanFile: StateFlow<String> = _currentPlanFile.asStateFlow()

    val currentPlan: Flow<ReadingPlan?> = _currentPlanFile.map { filename ->
        loadPlanFromAssets(app, filename)
    }

    private val _currentDayIndex = MutableStateFlow(0)
    val currentDayIndex: StateFlow<Int> = _currentDayIndex.asStateFlow()

    val currentDay: Flow<PlanDay?> = combine(currentPlan, currentDayIndex) { plan, dayIdx ->
        if (plan == null || dayIdx >= plan.days.size) null
        else plan.days[dayIdx]
    }

    private val _progressMap = MutableStateFlow<Map<String, Map<Int, DayProgress>>>(emptyMap())
    val progressMap: StateFlow<Map<String, Map<Int, DayProgress>>> = _progressMap.asStateFlow()

    val progressPercentage: Flow<Float> = combine(currentPlan, _progressMap) { plan, progressMap ->
        if (plan == null || plan.totalDays == 0) 0f
        else {
            val planProgress = progressMap[plan.planId] ?: emptyMap()
            val completedDays = planProgress.count { it.value.completed }
            completedDays.toFloat() / plan.totalDays.toFloat()
        }
    }

    val currentDayReadings: Flow<List<ReadingInfo>> = currentDay.map { day ->
        day?.readings?.mapNotNull { element ->
            val ref = when (element) {
                is kotlinx.serialization.json.JsonPrimitive -> element.content
                is kotlinx.serialization.json.JsonObject -> element["reference"]?.jsonPrimitive?.content
                else -> null
            } ?: return@mapNotNull null

            // Parse "GEN.1" or "Genesis 1:1"
            val parts = ref.split(".", ":", " ")
            if (parts.size >= 3) {
                val book = parts[0]
                val chapter = parts[1].toIntOrNull() ?: 1
                val verse = parts[2].toIntOrNull()
                ReadingInfo(book, chapter, verse)
            } else if (parts.size == 2) {
                val book = parts[0]
                val chapter = parts[1].toIntOrNull() ?: 1
                ReadingInfo(book, chapter)
            } else {
                ReadingInfo(ref, 1)
            }
        } ?: emptyList()
    }

    init {
        viewModelScope.launch {
            _plansIndex.value = loadPlansIndex()
            _progressMap.value = fileManager.loadProgressV2()
        }
    }

    fun selectPlan(planFile: String) {
        _currentPlanFile.value = planFile
        _currentDayIndex.value = 0
    }

    fun selectDay(dayIndex: Int) {
        _currentDayIndex.value = dayIndex
    }

    fun markDayCompleted(planId: String, dayIndex: Int, completed: Boolean = true) {
        viewModelScope.launch {
            val progressMap = _progressMap.value.toMutableMap()
            val planProgress = progressMap.getOrPut(planId) { mutableMapOf() }.toMutableMap()
            
            val existing = planProgress[dayIndex]
            if (existing != null) {
                planProgress[dayIndex] = existing.copy(completed = completed, lastUpdated = System.currentTimeMillis())
            } else {
                planProgress[dayIndex] = DayProgress(completed, System.currentTimeMillis())
            }
            
            progressMap[planId] = planProgress
            _progressMap.value = progressMap
            fileManager.saveProgressV2(progressMap)
        }
    }

    fun toggleReadingCompleted(planId: String, dayIndex: Int, readingIndex: Int) {
        viewModelScope.launch {
            val progressMap = _progressMap.value.toMutableMap()
            val planProgress = progressMap.getOrPut(planId) { mutableMapOf() }.toMutableMap()
            val existing = planProgress[dayIndex] ?: DayProgress()
            
            val newList = if (existing.completedReadings.contains(readingIndex)) {
                existing.completedReadings.filter { it != readingIndex }
            } else {
                existing.completedReadings + readingIndex
            }
            
            // Auto-complete day if all readings are done (if we know the count)
            // For now, just update the readings list
            planProgress[dayIndex] = existing.copy(
                completedReadings = newList,
                lastUpdated = System.currentTimeMillis()
            )
            
            progressMap[planId] = planProgress
            _progressMap.value = progressMap
            fileManager.saveProgressV2(progressMap)
        }
    }

    fun setDayCompleted(planId: String, dayIndex: Int, completed: Boolean, totalReadings: Int) {
        viewModelScope.launch {
            val progressMap = _progressMap.value.toMutableMap()
            val planProgress = progressMap.getOrPut(planId) { mutableMapOf() }.toMutableMap()
            val existing = planProgress[dayIndex] ?: DayProgress()
            
            planProgress[dayIndex] = existing.copy(
                completed = completed,
                completedReadings = if (completed) (0 until totalReadings).toList() else emptyList(),
                lastUpdated = System.currentTimeMillis()
            )
            
            progressMap[planId] = planProgress
            _progressMap.value = progressMap
            fileManager.saveProgressV2(progressMap)
        }
    }

    fun resetPlan(planId: String) {
        viewModelScope.launch {
            val progressMap = _progressMap.value.toMutableMap()
            progressMap.remove(planId)
            _progressMap.value = progressMap
            fileManager.saveProgressV2(progressMap)
        }
    }

    fun isDayCompleted(planId: String, dayIndex: Int): Boolean {
        return _progressMap.value[planId]?.get(dayIndex)?.completed ?: false
    }

    fun getDayProgress(planId: String, dayIndex: Int): DayProgress? {
        return _progressMap.value[planId]?.get(dayIndex)
    }

    private fun loadPlansIndex(): ReadingPlanIndex? {
        return try {
            val jsonString = app.assets.open("plans/plans-index.json").bufferedReader().use { it.readText() }
            json.decodeFromString<ReadingPlanIndex>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadPlanFromAssets(context: Context, filename: String): ReadingPlan? {
        return try {
            val jsonString = context.assets.open("plans/$filename").bufferedReader().use { it.readText() }
            json.decodeFromString<ReadingPlan>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseReading(readingElement: kotlinx.serialization.json.JsonElement): ParsedReading {
        return try {
            if (readingElement is kotlinx.serialization.json.JsonPrimitive) {
                // Bible in a Year format: "GEN.1"
                val ref = readingElement.jsonPrimitive.content
                ParsedReading.Simple(ref)
            } else if (readingElement is kotlinx.serialization.json.JsonObject) {
                // Daily Mass format: {type, reference}
                val type = readingElement["type"]?.jsonPrimitive?.content ?: ""
                val reference = readingElement["reference"]?.jsonPrimitive?.content ?: ""
                ParsedReading.Detailed(type, reference)
            } else {
                ParsedReading.Simple("Unknown")
            }
        } catch (e: Exception) {
            ParsedReading.Simple("Error: ${e.message}")
        }
    }
}


sealed class ParsedReading {
    data class Simple(val reference: String) : ParsedReading()
    data class Detailed(val type: String, val reference: String) : ParsedReading()
}

data class ReadingInfo(val book: String, val chapter: Int, val verse: Int? = null)
