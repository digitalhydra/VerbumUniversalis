package com.verbum.universalis.data.json

import kotlinx.serialization.Serializable

@Serializable
data class Highlight(
    val verseId: Int,
    val startOffset: Int = 0, // Character offset within the verse
    val endOffset: Int = 0,
    val colorId: Int = 0, // Index into the 20-color palette
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class Note(
    val verseId: Int,
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ReadingProgress(
    val planId: String,
    val eraIndex: Int,
    val dayIndex: Int,
    val completed: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class UserDataContainer(
    val settings: UserSettings = UserSettings(),
    val highlights: List<Highlight> = emptyList(),
    val notes: List<Note> = emptyList(),
    val progress: List<ReadingProgress> = emptyList()
)
