package com.verbum.universalis.data.json

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val theme: String = "system", // "light", "dark", "system"
    val language: String = "DR", // Bible language: "DR" (EN), "Spa" (ES)
    val appLanguage: String = "en", // App UI and Content language: "en", "es"
    val lastReadPassage: String = "1:1", // "bookId:chapter"
    val lastReadVerseId: Int? = null,
    val readingCalendar: String = "US", // "US", "CO", "RO"
    // Git Sync settings
    val gitRepoUrl: String? = null,
    val gitToken: String? = null
)
