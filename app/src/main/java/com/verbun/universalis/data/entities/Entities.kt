package com.verbun.universalis.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: Int,
    val name_en: String,
    val name_es: String,
    val name_la: String,
    val testament: String
)

@Entity(tableName = "verses")
data class VerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val book_id: Int,
    val chapter: Int,
    val verse_number: Int
)

@Entity(tableName = "texts")
data class TextEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val verse_id: Int,
    val lang_code: String,
    val content: String
)

@Entity(tableName = "interlinear_words")
data class InterlinearWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val verse_id: Int,
    val word_order: Int,
    val original: String,
    val transliteration: String,
    val literal: String,
    val morphology: String,
    val lemma: String
)

@Entity(tableName = "lexicon")
data class LexiconEntity(
    @PrimaryKey val lemma: String,
    val language: String,
    val definition: String
)
