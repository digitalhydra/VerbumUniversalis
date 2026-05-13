package com.verbum.universalis.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.verbum.universalis.data.entities.CrossReferenceEntity
import java.io.File

/**
 * Opens the pre-built verbum_cross_refs.db directly with raw SQLite.
 */
class CrossRefsRawDatabase private constructor(private val db: SQLiteDatabase?) {

    private fun isTableExists(tableName: String): Boolean {
        if (db == null || !db.isOpen) return false
        return try {
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName))
            cursor.use { it.count > 0 }
        } catch (e: Exception) {
            false
        }
    }

    fun queryCrossRefsForVerse(book: String, chapter: Int, verse: Int): List<CrossReferenceEntity> {
        if (db == null) {
            Log.e("CrossRefsRaw", "DB is null")
            return emptyList()
        }
        if (!db.isOpen) {
            Log.e("CrossRefsRaw", "DB is not open")
            return emptyList()
        }
        if (!isTableExists("cross_references")) {
            Log.e("CrossRefsRaw", "Table 'cross_references' does not exist in DB at: ${db.path}")
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
                val tables = mutableListOf<String>()
                while (c.moveToNext()) tables.add(c.getString(0))
                Log.e("CrossRefsRaw", "Available tables: $tables")
            }
            return emptyList()
        }
        
        return try {
            val sql = """
                SELECT * FROM cross_references 
                WHERE from_book = ? AND from_chapter = ? 
                  AND from_verse_start <= ? AND from_verse_end >= ?
                ORDER BY votes DESC
            """.trimIndent()
            db.rawQuery(sql, arrayOf(book, chapter.toString(), verse.toString(), verse.toString()))
                .use { c -> mapCursor(c) }
        } catch (e: Exception) {
            Log.e("CrossRefsRawDatabase", "Error querying cross references", e)
            emptyList()
        }
    }

    fun queryCrossRefsForChapter(book: String, chapter: Int): List<CrossReferenceEntity> {
        if (db == null || !db.isOpen || !isTableExists("cross_references")) {
            Log.e("CrossRefsRaw", "DB unavailable for chapter query")
            return emptyList()
        }
        
        return try {
            val sql = """
                SELECT * FROM cross_references 
                WHERE from_book = ? AND from_chapter = ?
                ORDER BY votes DESC
            """.trimIndent()
            db.rawQuery(sql, arrayOf(book, chapter.toString()))
                .use { c -> mapCursor(c) }
        } catch (e: Exception) {
            Log.e("CrossRefsRawDatabase", "Error querying cross references for chapter", e)
            emptyList()
        }
    }

    fun close() {
        db?.close()
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
                        File(context.filesDir, DATABASE_NAME),
                        context.getDatabasePath(DATABASE_NAME)
                    )
                    
                    // Debug: log which files exist
                    sources.forEach { f ->
                        Log.i("CrossRefsRaw", "Checking ${f.absolutePath}: exists=${f.exists()}, size=${f.length()}")
                    }
                    
                    val dbFile = sources.firstOrNull { it.exists() && it.length() > 0 }
                    Log.i("CrossRefsRaw", "Using dbFile: ${dbFile?.absolutePath}")
                    
                    val db = try {
                        if (dbFile != null) {
                            SQLiteDatabase.openDatabase(
                                dbFile.absolutePath,
                                null,
                                SQLiteDatabase.OPEN_READONLY
                            ).also { opened ->
                                // Debug: check tables
                                opened.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
                                    val tables = mutableListOf<String>()
                                    while (c.moveToNext()) tables.add(c.getString(0))
                                    Log.i("CrossRefsRaw", "Tables in DB: $tables")
                                }
                            }
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("CrossRefsRawDatabase", "Failed to open cross references database", e)
                        null
                    }
                    val instance = CrossRefsRawDatabase(db)
                    INSTANCE = instance
                    instance
                }
            }
        }

        fun isDatabaseDownloaded(context: Context): Boolean {
            val dbFile = File(context.filesDir, "databases/$DATABASE_NAME")
            val downloadedFile = File(context.filesDir, DATABASE_NAME)
            val systemDbFile = context.getDatabasePath(DATABASE_NAME)
            return (dbFile.exists() && dbFile.length() > 0) || 
                   (downloadedFile.exists() && downloadedFile.length() > 0) ||
                   (systemDbFile.exists() && systemDbFile.length() > 0)
        }

        fun invalidate() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
