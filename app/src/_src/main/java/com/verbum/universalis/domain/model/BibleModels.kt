package com.verbum.universalis.domain.model

import com.verbum.universalis.data.local.entities.*

data class BibleVerse(
    val id: Long,
    val bookId: Int,
    val chapter: Int,
    val verseNumber: Int,
    val texts: Map<String, String> // langCode -> content
)

data class InterlinearWord(
    val original: String,
    val transliteration: String,
    val literal: String,
    val morphology: String,
    val lemma: String
)

data class LexiconEntry(
    val lemma: String,
    val definition: String
)
