package com.verbum.universalis.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.verbum.universalis.data.entities.CatenaCommentaryEntity
import java.io.File

/**
 * Opens the pre-built verbum_catena.db directly with raw SQLite,
 * avoiding Room schema validation issues with the externally-built DB.
 */
class CatenaRawDatabase private constructor(private val db: SQLiteDatabase) {

    fun queryCommentaries(books: List<String>, chapter: Int, verseNumber: Int): List<CatenaCommentaryEntity> {
        val placeholders = books.joinToString(",") { "'${it.replace("'", "''")}'" }
        val sql = """
            SELECT * FROM commentaries 
            WHERE book IN ($placeholders) 
              AND chapter = ? 
              AND verse_start <= ? AND verse_end >= ?
            ORDER BY author_normalized
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf(chapter.toString(), verseNumber.toString(), verseNumber.toString()))
        return cursor.use { c ->
            val results = mutableListOf<CatenaCommentaryEntity>()
            while (c.moveToNext()) {
                results.add(
                    CatenaCommentaryEntity(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        book = c.getString(c.getColumnIndexOrThrow("book")),
                        chapter = c.getInt(c.getColumnIndexOrThrow("chapter")),
                        verseStart = c.getInt(c.getColumnIndexOrThrow("verse_start")),
                        verseEnd = c.getInt(c.getColumnIndexOrThrow("verse_end")),
                        author = c.getString(c.getColumnIndexOrThrow("author")),
                        authorNormalized = c.getString(c.getColumnIndexOrThrow("author_normalized")),
                        period = c.getStringOrNull("period"),
                        sourceTitle = c.getStringOrNull("source_title"),
                        sourceUrl = c.getStringOrNull("source_url"),
                        content = c.getString(c.getColumnIndexOrThrow("content")),
                        contentHash = c.getStringOrNull("content_hash"),
                        datasetSource = c.getString(c.getColumnIndexOrThrow("dataset_source")),
                        createdAt = c.getStringOrNull("created_at")
                    )
                )
            }
            results
        }
    }

    fun queryCommentariesForChapter(books: List<String>, chapter: Int): List<CatenaCommentaryEntity> {
        val placeholders = books.joinToString(",") { "'${it.replace("'", "''")}'" }
        val sql = """
            SELECT * FROM commentaries 
            WHERE book IN ($placeholders) AND chapter = ?
            ORDER BY author_normalized, verse_start
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf(chapter.toString()))
        return cursor.use { c -> mapCursor(c) }
    }

    fun queryCommentariesByBook(books: List<String>): List<CatenaCommentaryEntity> {
        val placeholders = books.joinToString(",") { "'${it.replace("'", "''")}'" }
        val sql = """
            SELECT * FROM commentaries 
            WHERE book IN ($placeholders)
            ORDER BY author_normalized, chapter, verse_start
        """.trimIndent()
        val cursor = db.rawQuery(sql, null)
        return cursor.use { c -> mapCursor(c) }
    }

    fun close() {
        db.close()
    }

    private fun mapCursor(cursor: android.database.Cursor): List<CatenaCommentaryEntity> {
        val results = mutableListOf<CatenaCommentaryEntity>()
        while (cursor.moveToNext()) {
            results.add(
                CatenaCommentaryEntity(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    book = cursor.getString(cursor.getColumnIndexOrThrow("book")),
                    chapter = cursor.getInt(cursor.getColumnIndexOrThrow("chapter")),
                    verseStart = cursor.getInt(cursor.getColumnIndexOrThrow("verse_start")),
                    verseEnd = cursor.getInt(cursor.getColumnIndexOrThrow("verse_end")),
                    author = cursor.getString(cursor.getColumnIndexOrThrow("author")),
                    authorNormalized = cursor.getString(cursor.getColumnIndexOrThrow("author_normalized")),
                    period = cursor.getStringOrNull("period"),
                    sourceTitle = cursor.getStringOrNull("source_title"),
                    sourceUrl = cursor.getStringOrNull("source_url"),
                    content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                    contentHash = cursor.getStringOrNull("content_hash"),
                    datasetSource = cursor.getString(cursor.getColumnIndexOrThrow("dataset_source")),
                    createdAt = cursor.getStringOrNull("created_at")
                )
            )
        }
        return results
    }

    companion object {
        private const val DATABASE_NAME = "verbum_catena.db"

        @Volatile
        private var INSTANCE: CatenaRawDatabase? = null

        fun getDatabase(context: Context): CatenaRawDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val sources = listOf(
                        File(context.filesDir, "databases/$DATABASE_NAME"),
                        File(context.filesDir, DATABASE_NAME)
                    )
                    val roomPath = context.getDatabasePath(DATABASE_NAME)
                    val source = sources.firstOrNull { it.exists() }
                    if (source != null) {
                        roomPath.parentFile?.mkdirs()
                        source.copyTo(roomPath, overwrite = true)
                    }
                    val db = SQLiteDatabase.openDatabase(
                        roomPath.absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.CREATE_IF_NECESSARY
                    )
                    val instance = CatenaRawDatabase(db)
                    INSTANCE = instance
                    instance
                }
            }
        }

        fun isDatabaseDownloaded(context: Context): Boolean {
            val dbFile = File(context.filesDir, "databases/$DATABASE_NAME")
            val downloadedFile = File(context.filesDir, DATABASE_NAME)
            return dbFile.exists() || downloadedFile.exists()
        }

        fun invalidate() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}

private fun android.database.Cursor.getStringOrNull(columnName: String): String? {
    val idx = getColumnIndex(columnName)
    return if (idx >= 0) getString(idx) else null
}