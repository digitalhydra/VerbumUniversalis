package com.verbum.universalis.data.repository

import android.content.Context
import com.verbum.universalis.data.db.CrossRefsRawDatabase
import com.verbum.universalis.data.download.DataDownloader
import com.verbum.universalis.data.entities.CrossReferenceEntity
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
class CrossRefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val downloader = DataDownloader()

    private val bookCodeToCrossRefsBook = mapOf(
        "GEN" to "genesis", "EXO" to "exodus", "LEV" to "leviticus", "NUM" to "numbers",
        "DEU" to "deuteronomy", "JOS" to "joshua", "JDG" to "judges", "RUT" to "ruth",
        "1SA" to "1_samuel", "2SA" to "2_samuel", "1KI" to "1_kings", "2KI" to "2_kings",
        "1CH" to "1_chronicles", "2CH" to "2_chronicles", "EZR" to "ezra",
        "NEH" to "nehemiah", "TOB" to "tobit", "JDT" to "judith", "EST" to "esther",
        "JOB" to "job", "PSA" to "psalms", "PRO" to "proverbs", "ECC" to "ecclesiastes",
        "SNG" to "song_of_solomon", "WIS" to "wisdom", "SIR" to "sirach",
        "ISA" to "isaiah", "JER" to "jeremiah", "LAM" to "lamentations",
        "BAR" to "baruch", "EZK" to "ezekiel", "DAN" to "daniel",
        "HOS" to "hosea", "JOL" to "joel", "AMO" to "amos", "OBA" to "obadiah",
        "JON" to "jonah", "MIC" to "micah", "NAH" to "nahum", "HAB" to "habakkuk",
        "ZEP" to "zephaniah", "HAG" to "haggai", "ZEC" to "zechariah",
        "MAL" to "malachi", "1MA" to "1_maccabees", "2MA" to "2_maccabees",
        "MAT" to "matthew", "MRK" to "mark", "LUK" to "luke", "JHN" to "john",
        "ACT" to "acts", "ROM" to "romans", "1CO" to "1_corinthians",
        "2CO" to "2_corinthians", "GAL" to "galatians", "EPH" to "ephesians",
        "PHP" to "philippians", "COL" to "colossians",
        "1TH" to "1_thessalonians", "2TH" to "2_thessalonians",
        "1TI" to "1_timothy", "2TI" to "2_timothy", "TIT" to "titus",
        "PHM" to "philemon", "HEB" to "hebrews", "JAS" to "james",
        "1PE" to "1_peter", "2PE" to "2_peter", "1JN" to "1_john",
        "2JN" to "2_john", "3JN" to "3_john", "JUD" to "jude", "REV" to "revelation"
    )

    fun getCrossRefsForVerse(bookCode: String, chapter: Int, verse: Int): Flow<List<CrossReferenceEntity>> {
        val book = bookCodeToCrossRefsBook[bookCode] ?: return flowOf(emptyList())
        return flow {
            val result = withContext(Dispatchers.IO) {
                CrossRefsRawDatabase.getDatabase(context).queryCrossRefsForVerse(book, chapter, verse)
            }
            emit(result)
        }
    }

    fun getCrossRefsForChapter(bookCode: String, chapter: Int): Flow<List<CrossReferenceEntity>> {
        val book = bookCodeToCrossRefsBook[bookCode] ?: return flowOf(emptyList())
        return flow {
            val result = withContext(Dispatchers.IO) {
                CrossRefsRawDatabase.getDatabase(context).queryCrossRefsForChapter(book, chapter)
            }
            emit(result)
        }
    }

    suspend fun isDatabaseDownloaded(): Boolean {
        return CrossRefsRawDatabase.isDatabaseDownloaded(context)
    }

    suspend fun downloadDatabase(): Boolean {
        val outputFile = File(context.filesDir, "verbum_cross_refs.db")
        val success = downloader.downloadCrossRefsDatabase(outputFile)
        if (success) {
            val dbDir = File(context.filesDir, "databases")
            if (!dbDir.exists()) dbDir.mkdirs()
            val finalDb = File(dbDir, "verbum_cross_refs.db")
            if (finalDb.exists()) finalDb.delete()
            outputFile.renameTo(finalDb)
            CrossRefsRawDatabase.invalidate()
        }
        return success
    }
}