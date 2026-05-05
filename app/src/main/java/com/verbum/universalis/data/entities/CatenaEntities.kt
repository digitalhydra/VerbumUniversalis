package com.verbum.universalis.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Catena commentary entry from the Commentaries-Database.
 * Matches the SQLite schema from catena.sqlite
 */
@Entity(tableName = "commentary")
data class CatenaCommentaryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "father_name")
    val fatherName: String,
    
    @ColumnInfo(name = "file_name")
    val fileName: String,
    
    @ColumnInfo(name = "append_to_author_name")
    val appendToAuthorName: String?,
    
    @ColumnInfo(name = "ts")
    val timestamp: Int,
    
    @ColumnInfo(name = "book")
    val book: String,
    
    @ColumnInfo(name = "location_start")
    val locationStart: Int,
    
    @ColumnInfo(name = "location_end")
    val locationEnd: Int,
    
    @ColumnInfo(name = "txt")
    val text: String,
    
    @ColumnInfo(name = "source_url")
    val sourceUrl: String?,
    
    @ColumnInfo(name = "source_title")
    val sourceTitle: String?
)

/**
 * Father metadata from the Commentaries-Database
 */
@Entity(tableName = "father_meta")
data class FatherMetaEntity(
    @PrimaryKey
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "default_year")
    val defaultYear: String?,
    
    @ColumnInfo(name = "wiki_url")
    val wikiUrl: String?
)
