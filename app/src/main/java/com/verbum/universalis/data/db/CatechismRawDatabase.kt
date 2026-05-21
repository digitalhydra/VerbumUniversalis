package com.verbum.universalis.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class CatechismRawDatabase private constructor(private val db: SQLiteDatabase?) {

    fun getParagraph(number: Int): CccParagraphEntity? {
        if (db == null || !db.isOpen) return null
        return try {
            val cursor = db.rawQuery(
                "SELECT * FROM ccc_paragraphs WHERE number = ?",
                arrayOf(number.toString())
            )
            cursor.use { c ->
                if (c.moveToFirst()) {
                    CccParagraphEntity(
                        number = c.getInt(c.getColumnIndexOrThrow("number")),
                        tocPath = c.getString(c.getColumnIndexOrThrow("toc_path")),
                        plainText = c.getString(c.getColumnIndexOrThrow("plain_text")),
                        formattedJson = c.getString(c.getColumnIndexOrThrow("formatted_json"))
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e("CatechismRawDatabase", "Error fetching paragraph $number", e)
            null
        }
    }

    fun getBibleRefsForParagraph(number: Int): List<CccBibleRefEntity> {
        if (db == null || !db.isOpen) return emptyList()
        return try {
            val cursor = db.rawQuery(
                "SELECT * FROM ccc_bible_refs WHERE ccc_number = ?",
                arrayOf(number.toString())
            )
            cursor.use { c ->
                val results = mutableListOf<CccBibleRefEntity>()
                while (c.moveToNext()) {
                    results.add(
                        CccBibleRefEntity(
                            id = c.getInt(c.getColumnIndexOrThrow("id")),
                            cccNumber = c.getInt(c.getColumnIndexOrThrow("ccc_number")),
                            bookId = c.getInt(c.getColumnIndexOrThrow("book_id")),
                            chapter = c.getInt(c.getColumnIndexOrThrow("chapter")),
                            verseStart = c.getInt(c.getColumnIndexOrThrow("verse_start")),
                            verseEnd = if (c.isNull(c.getColumnIndexOrThrow("verse_end"))) null else c.getInt(c.getColumnIndexOrThrow("verse_end")),
                            refText = c.getString(c.getColumnIndexOrThrow("ref_text")),
                            refPosition = c.getInt(c.getColumnIndexOrThrow("ref_position")),
                            refLength = c.getInt(c.getColumnIndexOrThrow("ref_length"))
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            Log.e("CatechismRawDatabase", "Error fetching Bible refs for paragraph $number", e)
            emptyList()
        }
    }

    fun getFootnotesForParagraph(number: Int): List<CccFootnoteEntity> {
        if (db == null || !db.isOpen) return emptyList()
        return try {
            val cursor = db.rawQuery(
                "SELECT * FROM ccc_footnotes WHERE ccc_number = ? ORDER BY footnote_number",
                arrayOf(number.toString())
            )
            cursor.use { c ->
                val results = mutableListOf<CccFootnoteEntity>()
                while (c.moveToNext()) {
                    results.add(
                        CccFootnoteEntity(
                            id = c.getInt(c.getColumnIndexOrThrow("id")),
                            cccNumber = c.getInt(c.getColumnIndexOrThrow("ccc_number")),
                            footnoteNumber = c.getInt(c.getColumnIndexOrThrow("footnote_number")),
                            footnoteText = c.getString(c.getColumnIndexOrThrow("footnote_text"))
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            Log.e("CatechismRawDatabase", "Error fetching footnotes for paragraph $number", e)
            emptyList()
        }
    }

    fun getFootnoteBibleRefs(footnoteId: Int): List<CccFootnoteBibleRefEntity> {
        if (db == null || !db.isOpen) return emptyList()
        return try {
            val cursor = db.rawQuery(
                "SELECT * FROM ccc_footnote_bible_refs WHERE footnote_id = ?",
                arrayOf(footnoteId.toString())
            )
            cursor.use { c ->
                val results = mutableListOf<CccFootnoteBibleRefEntity>()
                while (c.moveToNext()) {
                    results.add(
                        CccFootnoteBibleRefEntity(
                            id = c.getInt(c.getColumnIndexOrThrow("id")),
                            footnoteId = c.getInt(c.getColumnIndexOrThrow("footnote_id")),
                            bookId = c.getInt(c.getColumnIndexOrThrow("book_id")),
                            chapter = c.getInt(c.getColumnIndexOrThrow("chapter")),
                            verseStart = c.getInt(c.getColumnIndexOrThrow("verse_start")),
                            verseEnd = if (c.isNull(c.getColumnIndexOrThrow("verse_end"))) null else c.getInt(c.getColumnIndexOrThrow("verse_end")),
                            refText = c.getString(c.getColumnIndexOrThrow("ref_text")),
                            refPosition = c.getInt(c.getColumnIndexOrThrow("ref_position")),
                            refLength = c.getInt(c.getColumnIndexOrThrow("ref_length"))
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            Log.e("CatechismRawDatabase", "Error fetching footnote Bible refs for ID $footnoteId", e)
            emptyList()
        }
    }

    fun search(query: String, limit: Int = 50): List<CccSearchResultEntity> {
        if (db == null || !db.isOpen || query.isBlank()) return emptyList()
        return try {
            // Use FTS5 MATCH with snippet for highlighting
            // We explicitly match against the plain_text column
            val sql = """
                SELECT number, toc_path, snippet(ccc_fts, 2, '<b>', '</b>', '...', 20) as snippet
                FROM ccc_fts 
                WHERE plain_text MATCH ? 
                ORDER BY rank 
                LIMIT ?
            """.trimIndent()
            
            // Basic sanitization and multi-word prefix matching
            val sanitizedQuery = query.replace("\"", "").trim()
            val ftsQuery = sanitizedQuery.split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { "$it*" }

            val cursor = db.rawQuery(sql, arrayOf(ftsQuery, limit.toString()))
            
            cursor.use { c ->
                val results = mutableListOf<CccSearchResultEntity>()
                while (c.moveToNext()) {
                    results.add(
                        CccSearchResultEntity(
                            number = c.getInt(c.getColumnIndexOrThrow("number")),
                            tocPath = c.getString(c.getColumnIndexOrThrow("toc_path")),
                            snippet = c.getString(c.getColumnIndexOrThrow("snippet"))
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            Log.e("CatechismRawDatabase", "Error searching for '$query'", e)
            emptyList()
        }
    }

    fun getAllParagraphs(): List<CccParagraphEntity> {
        if (db == null || !db.isOpen) return emptyList()
        return try {
            val cursor = db.rawQuery("SELECT * FROM ccc_paragraphs ORDER BY number", null)
            cursor.use { c ->
                val results = mutableListOf<CccParagraphEntity>()
                while (c.moveToNext()) {
                    results.add(
                        CccParagraphEntity(
                            number = c.getInt(c.getColumnIndexOrThrow("number")),
                            tocPath = c.getString(c.getColumnIndexOrThrow("toc_path")),
                            plainText = c.getString(c.getColumnIndexOrThrow("plain_text")),
                            formattedJson = c.getString(c.getColumnIndexOrThrow("formatted_json"))
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            Log.e("CatechismRawDatabase", "Error fetching all paragraphs", e)
            emptyList()
        }
    }

    fun close() {
        db?.close()
    }

    companion object {
        private const val DATABASE_NAME = "verbum_ccc.db"

        @Volatile
        private var INSTANCE: CatechismRawDatabase? = null

        fun getDatabase(context: Context): CatechismRawDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val dbFile = File(context.filesDir, "databases/$DATABASE_NAME")
                    // Force copy for now to ensure user has the latest data and FTS index
                    // In production, we'd check a version flag or file size
                    copyDatabaseFromAssets(context, dbFile)

                    val db = try {
                        SQLiteDatabase.openDatabase(
                            dbFile.absolutePath,
                            null,
                            SQLiteDatabase.OPEN_READONLY
                        )
                    } catch (e: Exception) {
                        Log.e("CatechismRawDatabase", "Failed to open CCC database", e)
                        null
                    }
                    val instance = CatechismRawDatabase(db)
                    INSTANCE = instance
                    instance
                }
            }
        }

        private fun copyDatabaseFromAssets(context: Context, dbFile: File) {
            try {
                dbFile.parentFile?.mkdirs()
                context.assets.open(DATABASE_NAME).use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e("CatechismRawDatabase", "Error copying database from assets", e)
            }
        }

        fun invalidate() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}

data class CccParagraphEntity(
    val number: Int,
    val tocPath: String,
    val plainText: String,
    val formattedJson: String
)

data class CccBibleRefEntity(
    val id: Int,
    val cccNumber: Int,
    val bookId: Int,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int?,
    val refText: String,
    val refPosition: Int,
    val refLength: Int
)

data class CccFootnoteEntity(
    val id: Int,
    val cccNumber: Int,
    val footnoteNumber: Int,
    val footnoteText: String
)

data class CccFootnoteBibleRefEntity(
    val id: Int,
    val footnoteId: Int,
    val bookId: Int,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int?,
    val refText: String,
    val refPosition: Int,
    val refLength: Int
)

data class CccSearchResultEntity(
    val number: Int,
    val tocPath: String,
    val snippet: String
)
