package com.verbum.universalis.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/
 * Catena commentary entry.
 * Matches verbum_catena.db schema.
 */
@Entity(tableName = "commentaries")
data class CatenaCommentaryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "book")
    val book: String,

    @ColumnInfo(name = "chapter")
    val chapter: Int,

    @ColumnInfo(name = "verse_start")
    val verseStart: Int,

    @ColumnInfo(name = "verse_end")
    val verseEnd: Int,

    @ColumnInfo(name = "author")
    val author: String,

    @ColumnInfo(name = "author_normalized")
    val authorNormalized: String,

    @ColumnInfo(name = "period")
    val period: String?,

    @ColumnInfo(name = "source_title")
    val sourceTitle: String?,

    @ColumnInfo(name = "source_url")
    val sourceUrl: String?,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "content_hash")
    val contentHash: String?,

    @ColumnInfo(name = "dataset_source")
    val datasetSource: String
)
