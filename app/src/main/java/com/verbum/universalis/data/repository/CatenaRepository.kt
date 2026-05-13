package com.verbum.universalis.data.repository

import android.content.Context
import com.verbum.universalis.data.db.CatenaRawDatabase
import com.verbum.universalis.data.download.DataDownloader
import com.verbum.universalis.data.entities.CatenaCommentaryEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatenaRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val downloader = DataDownloader()

    // Maps bookId -> list of book name variants found in the catena DB.
    private val bookIdToCatenaBooks: Map<Int, List<String>> = mapOf(
        1 to listOf("genesis"), 2 to listOf("exodus"), 3 to listOf("leviticus"),
        4 to listOf("numbers"), 5 to listOf("deuteronomy"), 6 to listOf("joshua"),
        7 to listOf("judges", "jgs"), 8 to listOf("ruth"),
        9 to listOf("1_samuel", "1sm"), 10 to listOf("2_samuel", "2sm"),
        11 to listOf("1kgs", "1_kings"), 12 to listOf("2kgs", "2_kings"),
        13 to listOf("1chr", "1_chronicles"), 14 to listOf("2chr", "2_chronicles"),
        15 to listOf("ezra"), 16 to listOf("neh", "nehemiah"),
        17 to listOf("tobit"), 18 to listOf("judith"),
        19 to listOf("esther", "est"), 20 to listOf("job", "jb"),
        21 to listOf("psalms", "psalm"), 22 to listOf("proverbs", "prv"),
        23 to listOf("eccl", "ecclesiastes"), 24 to listOf("songofsolomon", "sg"),
        25 to listOf("wisdom"), 26 to listOf("sirach"),
        27 to listOf("isaiah", "is"), 28 to listOf("jeremiah"),
        29 to listOf("lamentations"), 30 to listOf("baruch"),
        31 to listOf("ezekiel", "ez"), 32 to listOf("daniel", "dn"),
        33 to listOf("hosea"), 34 to listOf("joel", "jl"), 35 to listOf("amos"),
        36 to listOf("obadiah"), 37 to listOf("jonah"),
        38 to listOf("micah", "mi"), 39 to listOf("nahum", "na"),
        40 to listOf("habakkuk", "hb"), 41 to listOf("zephaniah"),
        42 to listOf("haggai", "hg"), 43 to listOf("zechariah"),
        44 to listOf("malachi"), 45 to listOf("1_maccabees"),
        46 to listOf("2_maccabees"), 47 to listOf("matthew"), 48 to listOf("mark"),
        49 to listOf("luke"), 50 to listOf("john"), 51 to listOf("acts"),
        52 to listOf("romans"), 53 to listOf("1cor", "1_corinthians"),
        54 to listOf("2cor", "2_corinthians"), 55 to listOf("galatians"),
        56 to listOf("ephesians"), 57 to listOf("phil", "philippians"),
        58 to listOf("colossians"), 59 to listOf("1thes", "1thessalonians"),
        60 to listOf("2thes", "2thessalonians"), 61 to listOf("1tim", "1_timothy"),
        62 to listOf("2tim", "2_timothy"), 63 to listOf("titus"),
        64 to listOf("phlm", "philemon"), 65 to listOf("hebrews"),
        66 to listOf("james"), 67 to listOf("1pet", "1_peter"),
        68 to listOf("2pet", "2_peter"), 69 to listOf("1_john"),
        70 to listOf("2_john"), 71 to listOf("3_john"), 72 to listOf("jude"),
        73 to listOf("revelation")
    )

    private fun booksFor(bookId: Int): List<String> =
        bookIdToCatenaBooks[bookId] ?: emptyList()

    fun getCommentariesForVerse(bookId: Int, chapter: Int, verseNumber: Int): Flow<List<CatenaCommentaryEntity>> {
        val books = booksFor(bookId)
        if (books.isEmpty()) return flowOf(emptyList())
        return flow {
            val result = withContext(Dispatchers.IO) {
                CatenaRawDatabase.getDatabase(context).queryCommentaries(books, chapter, verseNumber)
            }
            emit(result)
        }
    }

    fun getCommentariesForChapter(bookId: Int, chapter: Int): Flow<List<CatenaCommentaryEntity>> {
        val books = booksFor(bookId)
        if (books.isEmpty()) return flowOf(emptyList())
        return flow {
            val result = withContext(Dispatchers.IO) {
                CatenaRawDatabase.getDatabase(context).queryCommentariesForChapter(books, chapter)
            }
            emit(result)
        }
    }

    fun getCommentariesForBook(bookId: Int): Flow<List<CatenaCommentaryEntity>> {
        val books = booksFor(bookId)
        if (books.isEmpty()) return flowOf(emptyList())
        return flow {
            val result = withContext(Dispatchers.IO) {
                CatenaRawDatabase.getDatabase(context).queryCommentariesByBook(books)
            }
            emit(result)
        }
    }

    suspend fun isDatabaseDownloaded(): Boolean {
        return CatenaRawDatabase.isDatabaseDownloaded(context)
    }

    suspend fun downloadDatabase(): Boolean {
        val outputFile = File(context.filesDir, "verbum_catena.db")
        val success = downloader.downloadCatenaDatabase(outputFile)
        if (success) {
            val dbDir = File(context.filesDir, "databases")
            if (!dbDir.exists()) dbDir.mkdirs()
            val finalDb = File(dbDir, "verbum_catena.db")
            if (finalDb.exists()) finalDb.delete()
            outputFile.renameTo(finalDb)
            CatenaRawDatabase.invalidate()
        }
        return success
    }
}