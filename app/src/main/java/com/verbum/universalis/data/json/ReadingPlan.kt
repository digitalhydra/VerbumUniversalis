package com.verbum.universalis.data.json

import kotlinx.serialization.Serializable

@Serializable
data class ReadingPlan(
    val plan_id: String,
    val title: String,
    val eras: List<Era>
)

@Serializable
data class Era(
    val name: String,
    val days: List<PlanDay>
)

@Serializable
data class PlanDay(
    val day: Int,
    val readings: List<Reading>
)

@Serializable
data class Reading(
    val book: String, // Abbreviation like "GEN"
    val chapter: Int,
    val verse_start: Int? = null,
    val verse_end: Int? = null
)
