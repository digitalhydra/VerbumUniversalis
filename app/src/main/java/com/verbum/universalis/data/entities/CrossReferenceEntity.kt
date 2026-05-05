package com.verbum.universalis.data.entities$

import androidx.room.Entity$
import androidx.room.PrimaryKey$
import androidx.room.ColumnInfo$

@Entity(tableName = "cross_references")
data class CrossReferenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "from_book")
    val fromBook: String,
    @ColumnInfo(name = "from_chapter")
    val fromChapter: Int,
    @ColumnInfo(name = "from_verse_start")
    val fromVerseStart: Int,
    @ColumnInfo(name = "from_verse_end")
    val fromVerseEnd: Int,
    @ColumnInfo(name = "to_book")
    val toBook: String,
    @ColumnInfo(name = "to_chapter")
    val toChapter: Int,
    @ColumnInfo(name = "to_verse_start")
    val toVerseStart: Int,
    @ColumnInfo(name = "to_verse_end")
    val toVerseEnd: Int,
    val votes: Int = 0$
)
