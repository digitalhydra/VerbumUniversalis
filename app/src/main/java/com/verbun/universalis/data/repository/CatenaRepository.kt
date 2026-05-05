package com.verbun.universalis.data.repository

import android.content.Context
import com.verbun.universalis.data.daos.CatenaDao
import com.verbun.universalis.data.db.CatenaDatabase
import com.verbun.universalis.data.download.DataDownloader
import com.verbun.universalis.data.entities.CatenaCommentaryEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

class CatenaRepository(
    private val catenaDao: CatenaDao,
    @androidx.annotation.qualifiers.ApplicationContext private val context: Context
) {
    private val downloader = DataDownloader()
    
    // Book name mapping: our book_id -> catena book string
    private val bookIdToCatenaBook = mapOf(
        1 to "genesis", 2 to "exodus", 3 to "leviticus", 4 to "numbers", 5 to "deuteronomy",
        6 to "joshua", 7 to "judges", 8 to "ruth", 9 to "1samuel", 10 to "2samuel",
        11 to "1kings", 12 to "2kings", 13 to "1chronicles", 14 to "2chronicles",
        15 to "ezra", 16 to "nehemiah", 17 to "tobit", 18 to "judith", 19 to "esther",
        20 to "job", 21 to "psalms", 22 to "proverbs", 23 to "ecclesiastes", 24 to "songofsolomon",
        25 to "wisdom", 26 to "sirach", 27 to "isaiah", 28 to "jeremiah",
        29 to "lamentations", 30 to "baruch", 31 to "ezekiel", 32 to "daniel",
        33 to "hosea", 34 to "joel", 35 to "amos", 36 to "obadiah",
        37 to "jonah", 38 to "micah", 39 to "nahum", 40 to "habakkuk",
        41 to "zephaniah", 42 to "haggai", 43 to "zechariah", 44 to "malachi",
        45 to "1maccabees", 46 to "2maccabees", 47 to "matthew", 48 to "mark",
        49 to "luke", 50 to "john", 51 to "acts", 52 to "romans",
        53 to "1corinthians", 54 to "2corinthians", 55 to "galatians", 56 to "ephesians",
        57 to "philippians", 58 to "colossians", 59 to "1thessalonians", 60 to "2thessalonians",
        61 to "1timothy", 62 to "2timothy", 63 to "titus", 64 to "philemon",
        65 to "hebrews", 66 to "james", 67 to "1peter", 68 to "2peter",
        69 to "1john", 70 to "2john", 71 to "3john", 72 to "jude",
        73 to "revelation"
    )
    
    // Encode chapter/verse to match catena.sqlite format (chapter * 1000000 + verse)
    fun encodeVerse(chapter: Int, verse: Int): Int = (chapter * 1000000) + verse
    
    fun getCommentariesForVerse(bookId: Int, chapter: Int, verseNumber: Int): Flow<List<CatenaCommentaryEntity>> {
        val book = bookIdToCatenaBook[bookId] ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        val encodedVerse = encodeVerse(chapter, verseNumber)
        return catenaDao.getCommentariesByVerse(encodedVerse)
    }
    
    fun getCommentariesForChapter(bookId: Int, chapter: Int, startVerse: Int, endVerse: Int): Flow<List<CatenaCommentaryEntity>> {
        val book = bookIdToCatenaBook[bookId] ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        val start = encodeVerse(chapter, startVerse)
        val end = encodeVerse(chapter, endVerse)
        return catenaDao.getCommentariesForChapter(book, start, end)
    }
    
    suspend fun isDatabaseDownloaded(): Boolean {
        return CatenaDatabase.isDatabaseDownloaded(context)
    }
    
    suspend fun downloadDatabase(): Boolean {
        val outputFile = File(context.filesDir, "catena.sqlite")
        val success = downloader.downloadCatenaDatabase(outputFile)
        if (success) {
            // Move to databases folder for Room
            val dbDir = File(context.filesDir, "databases")
            if (!dbDir.exists()) dbDir.mkdirs()
            val finalDb = File(dbDir, "catena.sqlite")
            outputFile.renameTo(finalDb)
        }
        return success
    }
}
