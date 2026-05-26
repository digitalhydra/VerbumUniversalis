package com.verbum.universalis.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class CatechismRawDatabase private constructor(private val db: SQLiteDatabase?) {

    fun getParagraph(number: Int, lang: String): CccParagraphEntity? {
        if (db == null || !db.isOpen) return null
        return try {
            val cursor = db.rawQuery(
                "SELECT * FROM ccc_paragraphs WHERE number = ? AND lang = ?",
                arrayOf(number.toString(), lang)
            )
            cursor.use { c ->
                if (c.moveToFirst()) {
                    CccParagraphEntity(
                        number = c.getInt(c.getColumnIndexOrThrow("number")),
                        lang = c.getString(c.getColumnIndexOrThrow("lang")),
                        tocPath = c.getString(c.getColumnIndexOrThrow("toc_path")),
                        plain_text = c.getString(c.getColumnIndexOrThrow("plain_text")),
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

    fun search(query: String, lang: String, limit: Int = 50): List<CccSearchResultEntity> {
        if (db == null || !db.isOpen || query.isBlank()) return emptyList()
        Log.d("CatechismSearch", "--- SEARCH START ---")
        Log.d("CatechismSearch", "Raw Query: '$query', Lang: $lang")
        
        val results = mutableListOf<CccSearchResultEntity>()
        val sanitizedQuery = query.replace("\"", "").trim()
        val words = sanitizedQuery.split(" ").filter { it.isNotBlank() }

        // 1. Try FTS Search (with specific Android version compatibility logic)
        try {
            // Syntax A: Column filter
            val ftsQueryA = if (words.size == 1) "lang:$lang plain_text:${words[0]}*" else "lang:$lang plain_text:(${words.joinToString(" ") { "$it*" }})"
            // Syntax B: Global match
            val ftsQueryB = "lang:$lang " + words.joinToString(" AND ") { "$it*" }

            val trySyntaxes = listOf(ftsQueryA, ftsQueryB)
            
            for (syntax in trySyntaxes) {
                try {
                    Log.d("CatechismSearch", "Trying FTS Syntax: $syntax")
                    val sql = "SELECT number, toc_path, snippet(ccc_fts, 3, '<b>', '</b>', '...', 25) as snippet FROM ccc_fts WHERE ccc_fts MATCH ? ORDER BY rank LIMIT ?"
                    db.rawQuery(sql, arrayOf(syntax, limit.toString())).use { c ->
                        if (c.count > 0) {
                            Log.d("CatechismSearch", "FOUND ${c.count} results with FTS")
                            while (c.moveToNext()) {
                                results.add(CccSearchResultEntity(
                                    number = c.getInt(c.getColumnIndexOrThrow("number")),
                                    tocPath = c.getString(c.getColumnIndexOrThrow("toc_path")),
                                    snippet = c.getString(c.getColumnIndexOrThrow("snippet"))
                                ))
                            }
                            return results
                        }
                    }
                } catch (e: Exception) {
                    Log.w("CatechismSearch", "FTS syntax failed, trying next or falling back. Error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("CatechismSearch", "General FTS failure", e)
        }

        // 2. ULTIMATE FALLBACK: Simple LIKE query (Guaranteed to work if FTS4 is missing)
        try {
            Log.w("CatechismSearch", "FTS unavailable or found 0 results. Falling back to deep scan (LIKE).")
            val likeSql = "SELECT number, toc_path, plain_text FROM ccc_paragraphs WHERE lang = ? AND (plain_text LIKE ? OR toc_path LIKE ?) LIMIT ?"
            db.rawQuery(likeSql, arrayOf(lang, "%$sanitizedQuery%", "%$sanitizedQuery%", limit.toString())).use { c ->
                Log.d("CatechismSearch", "LIKE query found ${c.count} results")
                while (c.moveToNext()) {
                    val text = c.getString(c.getColumnIndexOrThrow("plain_text"))
                    val start = text.lowercase().indexOf(sanitizedQuery.lowercase()).coerceAtLeast(0)
                    val end = (start + 120).coerceAtMost(text.length)
                    val snippet = "...${text.substring(start, end)}..."
                    results.add(CccSearchResultEntity(
                        number = c.getInt(c.getColumnIndexOrThrow("number")),
                        tocPath = c.getString(c.getColumnIndexOrThrow("toc_path")),
                        snippet = snippet.replace(sanitizedQuery, "<b>$sanitizedQuery</b>", ignoreCase = true)
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e("CatechismRawDatabase", "LIKE search also failed (serious DB error)", e)
        } finally {
            Log.d("CatechismSearch", "Final results size: ${results.size}")
            Log.d("CatechismSearch", "--- SEARCH END ---")
        }
        return results
    }

    fun getAllParagraphs(lang: String): List<CccParagraphEntity> {
        if (db == null || !db.isOpen) return emptyList()
        return try {
            val cursor = db.rawQuery("SELECT * FROM ccc_paragraphs WHERE lang = ? ORDER BY number", arrayOf(lang))
            cursor.use { c ->
                val results = mutableListOf<CccParagraphEntity>()
                while (c.moveToNext()) {
                    results.add(
                        CccParagraphEntity(
                            number = c.getInt(c.getColumnIndexOrThrow("number")),
                            lang = c.getString(c.getColumnIndexOrThrow("lang")),
                            tocPath = c.getString(c.getColumnIndexOrThrow("toc_path")),
                            plain_text = c.getString(c.getColumnIndexOrThrow("plain_text")),
                            formattedJson = c.getString(c.getColumnIndexOrThrow("formatted_json"))
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            Log.e("CatechismRawDatabase", "Error fetching all paragraphs for $lang", e)
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
                Log.d("CatechismRawDatabase", "Database copied to: ${dbFile.absolutePath}")
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
    val lang: String,
    val tocPath: String,
    val plain_text: String,
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
