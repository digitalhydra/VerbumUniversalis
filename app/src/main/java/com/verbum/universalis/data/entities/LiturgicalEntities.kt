package com.verbum.universalis.data.entities

import kotlinx.serialization.Serializable

@Serializable
data class LiturgicalReadingEntry(
    val date: String,
    val monthDay: String? = null,
    val season: String? = null,
    val celebration: Celebration? = null,
    val readings: List<ReadingRef> = emptyList()
)

@Serializable
data class Celebration(
    val name: String,
    val type: String? = null,
    val quote: String? = null,
    val description: String? = null
)

@Serializable
data class ReadingRef(
    val type: String? = null,
    val reference: String // e.g., "GEN.1.1" or "1TH.1-5, 8b-10"
)

@Serializable
data class DailyMassReadingEntry(
    val date: String,
    val monthDay: String? = null,
    val season: String? = null,
    val subSeason: String? = null,
    val readings: List<DailyReadingRef> = emptyList(),
    val usccbLink: String? = null,
    val apiEndpoint: String? = null
)

@Serializable
data class DailyReadingRef(
    val type: String, // "firstReading", "psalm", "gospel"
    val reference: String // e.g., "1 Thessalonians 1:1-5, 8b-10"
)

@Serializable
data class LiturgicalCalendar(
    val planId: String? = null,
    val title: String? = null,
    val entries: List<LiturgicalReadingEntry> = emptyList()
)

@Serializable
data class DailyMassReadings(
    val planId: String? = null,
    val title: String? = null,
    val totalDays: Int? = null,
    val days: List<DailyMassReadingEntry> = emptyList()
)