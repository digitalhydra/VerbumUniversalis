package com.verbum.universalis.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: Int,
    val name_en: String,
    val name_es: String,
    val name_la: String,
    val testament: String
)

@Entity(
    tableName = "verses",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"]
        )
    ],
    indices = [Index(value = ["book_id", "chapter", "verse_number"], name = "idx_verses_book")]
)
data class VerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val book_id: Int,
    val chapter: Int,
    val verse_number: Int
)

@Entity(
    tableName = "texts",
    foreignKeys = [
        ForeignKey(
            entity = VerseEntity::class,
            parentColumns = ["id"],
            childColumns = ["verse_id"]
        )
    ],
    indices = [Index(value = ["verse_id", "lang_code"], name = "idx_texts_verse")]
)
data class TextEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val verse_id: Int,
    val lang_code: String,
    val content: String
)

@Entity(
    tableName = "interlinear_words",
    foreignKeys = [
        ForeignKey(
            entity = VerseEntity::class,
            parentColumns = ["id"],
            childColumns = ["verse_id"]
        )
    ],
    indices = [Index(value = ["verse_id"], name = "idx_interlinear_verse")]
)
data class InterlinearWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val verse_id: Int,
    val word_order: Int,
    val original: String,
    val transliteration: String?,
    val literal: String?,
    val morphology: String?,
    val lemma: String?
)

@Entity(tableName = "lexicon")
data class LexiconEntity(
    @PrimaryKey val lemma: String,
    val language: String,
    val definition: String
)
