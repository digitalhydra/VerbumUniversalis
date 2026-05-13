package com.verbum.universalis.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.verbum.universalis.data.entities.CrossReferenceEntity
import java.io.File

/**
 * Opens the pre-built verbum_cross_refs.db directly with raw SQLite.
 */
class CrossRefsRawDatabase private constructor(private val db: SQLiteDatabase) {

    fun queryCrossRefsForVerse(book: String, chapter: Int, verse: Int): List<CrossReferenceEntity> {
        val sql = """
            SELECT * FROM cross_references 
            WHERE from_book = ? AND from_chapter = ? 
              AND from_verse_start <= ? AND from_verse_end >= ?
            ORDER BY votes DESC
        """.trimIndent()
        return db.rawQuery(sql, arrayOf(book, chapter.toString(), verse.toString(), verse.toString()))
            .use { c -> mapCursor(c) }
    }

    fun queryCrossRefsForChapter(book: String, chapter: Int): List<CrossReferenceEntity> {
        val sql = """
            SELECT * FROM cross_references 
            WHERE from_book = ? AND from_chapter = ?
            ORDER BY votes DESC
        """.trimIndent()
        return db.rawQuery(sql, arrayOf(book, chapter.toString()))
            .use { c -> mapCursor(c) }
    }

    fun close() {
        db.close()
    }

    private fun mapCursor(cursor: android.database.Cursor): List<CrossReferenceEntity> {
        val results = mutableListOf<CrossReferenceEntity>()
        while (cursor.moveToNext()) {
            results.add(
                CrossReferenceEntity(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    fromBook = cursor.getString(cursor.getColumnIndexOrThrow("from_book")),
                    fromChapter = cursor.getInt(cursor.getColumnIndexOrThrow("from_chapter")),
                    fromVerseStart = cursor.getInt(cursor.getColumnIndexOrThrow("from_verse_start")),
                    fromVerseEnd = cursor.getInt(cursor.getColumnIndexOrThrow("from_verse_end")),
                    toBook = cursor.getString(cursor.getColumnIndexOrThrow("to_book")),
                    toChapter = cursor.getInt(cursor.getColumnIndexOrThrow("to_chapter")),
                    toVerseStart = cursor.getInt(cursor.getColumnIndexOrThrow("to_verse_start")),
                    toVerseEnd = cursor.getInt(cursor.getColumnIndexOrThrow("to_verse_end")),
                    votes = cursor.getInt(cursor.getColumnIndexOrThrow("votes"))
                )
            )
        }
        return results
    }

    companion object {
        private const val DATABASE_NAME = "verbum_cross_refs.db"

        @Volatile
        private var INSTANCE: CrossRefsRawDatabase? = null

        fun getDatabase(context: Context): CrossRefsRawDatabase {
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
                    val instance = CrossRefsRawDatabase(db)
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