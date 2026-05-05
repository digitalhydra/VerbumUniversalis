package com.verbum.universalis.data.json

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val theme: String = "system", // "light", "dark", "system"
    val language: String = "DR", // "DR" (EN), "Spa" (ES)
    val lastReadPassage: String = "1:1", // "bookId:chapter"
    val lastReadVerseId: Int? = null,
    // Git Sync settings
    val gitRepoUrl: String? = null,
    val gitToken: String? = null
)
