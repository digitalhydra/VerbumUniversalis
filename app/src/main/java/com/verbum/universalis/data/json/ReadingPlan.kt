package com.verbum.universalis.data.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ReadingPlanIndex(
    val plans: List<PlanSummary>,
    val lastUpdated: String
)

@Serializable
data class PlanSummary(
    val planId: String,
    val title: String,
    val description: String,
    val type: String,
    val totalDays: Int,
    val file: String
)

@Serializable
data class ReadingPlan(
    val planId: String,
    val title: String,
    val description: String,
    val type: String,
    val totalDays: Int,
    val days: List<PlanDay>
)

@Serializable
data class PlanDay(
    val day: Int,
    val date: String? = null,
    val monthDay: String? = null,
    val season: String? = null,
    val subSeason: String? = null,
    val era: String? = null,
    val readings: List<JsonElement> = emptyList(),
    val readingCount: Int = 0,
    val progress: ProgressData? = null
)

@Serializable
data class ProgressData(
    val completed: Boolean = false,
    val completedAt: String? = null,
    val readingsCompleted: List<Int> = emptyList()
)

@Serializable
data class ReadingReference(
    val type: String,
    val reference: String
)

@Serializable
data class DayProgress(
    val completed: Boolean = false,
    val lastUpdated: Long = 0L,
    val completedReadings: List<Int> = emptyList()
)
