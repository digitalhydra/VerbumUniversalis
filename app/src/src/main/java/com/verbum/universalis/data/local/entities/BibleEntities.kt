package com.verbum.universalis.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: Int,
    val nameEn: String,
    val nameEs: String,
    val nameLa: String,
    val testament: String
)

@Entity(
    tableName = "verses",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class VerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Int,
    val chapter: Int,
    val verseNumber: Int
)

@Entity(
    tableName = "texts",
    foreignKeys = [ForeignKey(
        entity = VerseEntity::class,
        parentColumns = ["id"],
        childColumns = ["verseId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class TextEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verseId: Long,
    val langCode: String, // e.g., "DR", "SCIO", "VL"
    val content: String
)

@Entity(
    tableName = "interlinear_words",
    foreignKeys = [ForeignKey(
        entity = VerseEntity::class,
        parentColumns = ["id"],
        childColumns = ["verseId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class InterlinearWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verseId: Long,
    val wordOrder: Int,
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
