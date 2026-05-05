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

@HiltViewModel
class ReadingPlanViewModel @Inject constructor(
    private val fileManager: FileManager,
    private val app: Application
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _currentPlanId = MutableStateFlow("bible-in-a-year")
    val currentPlanId: StateFlow<String?> = _currentPlanId.asStateFlow()

    val currentPlan: Flow<ReadingPlan?> = _currentPlanId.map { planId ->
        loadPlanFromAssets(app, "$planId.json")
    }

    private val _currentEraIndex = MutableStateFlow(0)
    val currentEraIndex: StateFlow<Int> = _currentEraIndex.asStateFlow()

    private val _currentDayIndex = MutableStateFlow(0)
    val currentDayIndex: StateFlow<Int> = _currentDayIndex.asStateFlow()

    val currentDayReadings: Flow<List<Reading>> = combine(
        currentPlan, currentEraIndex, currentDayIndex
    ) { plan, eraIdx, dayIdx ->
        if (plan == null || eraIdx >= plan.eras.size) emptyList()
        else {
            val era = plan.eras[eraIdx]
            if (dayIdx >= era.days.size) emptyList()
            else era.days[dayIdx].readings
        }
    }

    private val _progressList = MutableStateFlow<List<ReadingProgress>>(emptyList())
    val progressList: StateFlow<List<ReadingProgress>> = _progressList.asStateFlow()

    val progressPercentage: Flow<Float> = combine(currentPlan, _progressList) { plan, progressList ->
        if (plan == null || plan.eras.isEmpty()) 0f
        else {
            val totalDays = plan.eras.sumOf { it.days.size }
            val completedDays = progressList.count { it.completed }
            if (totalDays == 0) 0f else completedDays.toFloat() / totalDays.toFloat()
        }
    }

    init {
        viewModelScope.launch {
            _progressList.value = fileManager.loadProgress()
        }
    }

    fun selectDay(eraIndex: Int, dayIndex: Int) {
        _currentEraIndex.value = eraIndex
        _currentDayIndex.value = dayIndex
    }

    fun markDayCompleted(eraIndex: Int, dayIndex: Int) {
        viewModelScope.launch {
            val progressList = fileManager.loadProgress().toMutableList()
            val existing = progressList.find { it.planId == _currentPlanId.value && it.eraIndex == eraIndex && it.dayIndex == dayIndex }
            if (existing != null) {
                existing.completed = true
                existing.lastUpdated = System.currentTimeMillis()
            } else {
                progressList.add(ReadingProgress(_currentPlanId.value!!, eraIndex, dayIndex, true))
            }
            fileManager.saveProgress(progressList)
            _progressList.value = progressList
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
}
