package com.verbum.universalis.data.repository

import android.content.Context
import com.verbum.universalis.data.daos.CatenaDao
import com.verbum.universalis.data.db.CatenaDatabase
import com.verbum.universalis.data.download.DataDownloader
import com.verbum.universalis.data.entities.CatenaCommentaryEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File

class CatenaRepository(
    private val catenaDao: CatenaDao,
    @ApplicationContext private val context: Context
) {
    private val downloader = DataDownloader()

    // Book name mapping: our book_id -> catena book string (abbreviated)
    private val bookIdToCatenaBook = mapOf(
        1 to "gen", 2 to "exod", 3 to "lev", 4 to "num", 5 to "deut",
        6 to "josh", 7 to "judg", 8 to "ruth", 9 to "1sam", 10 to "2sam",
        11 to "1kgs", 12 to "2kgs", 13 to "1chr", 14 to "2chr",
        15 to "ezra", 16 to "neh", 17 to "tob", 18 to "jdt", 19 to "esth",
        20 to "job", 21 to "ps", 22 to "prov", 23 to "eccl", 24 to "song",
        25 to "wis", 26 to "sir", 27 to "isa", 28 to "jer",
        29 to "lam", 30 to "bar", 31 to "ezek", 32 to "dan",
        33 to "hos", 34 to "joel", 35 to "amos", 36 to "obad",
        37 to "jonah", 38 to "mic", 39 to "nah", 40 to "hab",
        41 to "zeph", 42 to "hag", 43 to "zech", 44 to "mal",
        45 to "1macc", 46 to "2macc", 47 to "matt", 48 to "mark",
        49 to "luk", 50 to "john", 51 to "acts", 52 to "rom",
        53 to "1cor", 54 to "2cor", 55 to "gal", 56 to "eph",
        57 to "phil", 58 to "col", 59 to "1thes", 60 to "2thes",
        61 to "1tim", 62 to "2tim", 63 to "titus", 64 to "phlm",
        65 to "heb", 66 to "jas", 67 to "1pet", 68 to "2pet",
        69 to "1john", 70 to "2john", 71 to "3john", 72 to "jude",
        73 to "rev"
    )

    fun getCommentariesForVerse(bookId: Int, chapter: Int, verseNumber: Int): Flow<List<CatenaCommentaryEntity>> {
        val book = bookIdToCatenaBook[bookId] ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return catenaDao.getCommentariesForVerse(book, chapter, verseNumber)
    }

    fun getCommentariesForChapter(bookId: Int, chapter: Int): Flow<List<CatenaCommentaryEntity>> {
        val book = bookIdToCatenaBook[bookId] ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return catenaDao.getCommentariesForChapter(book, chapter)
    }

    fun getCommentariesForBook(bookId: Int): Flow<List<CatenaCommentaryEntity>> {
        val book = bookIdToCatenaBook[bookId] ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return catenaDao.getCommentariesByBook(book)
    }

    suspend fun isDatabaseDownloaded(): Boolean {
        return CatenaDatabase.isDatabaseDownloaded(context)
    }

    suspend fun downloadDatabase(): Boolean {
        val outputFile = File(context.filesDir, "verbum_catena.db")
        val success = downloader.downloadCatenaDatabase(outputFile)
        if (success) {
            // Move to databases folder for Room
            val dbDir = File(context.filesDir, "databases")
            if (!dbDir.exists()) dbDir.mkdirs()
            val finalDb = File(dbDir, "verbum_catena.db")
            if (finalDb.exists()) finalDb.delete()
            outputFile.renameTo(finalDb)
        }
        return success
    }
}
