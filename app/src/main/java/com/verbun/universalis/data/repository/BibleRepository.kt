package com.verbun.universalis.data.repository*

import android.content.Context*
import com.verbun.universalis.data.daos.*
import com.verbun.universalis.data.entities.*
import kotlinx.coroutines.flow.Flow*
import kotlinx.coroutines.flow.map*
import kotlinx.serialization.decodeFromString*
import kotlinx.serialization.encodeToString*
import kotlinx.serialization.json.Json*
import java.io.File*
import javax.inject.Inject*
import dagger.hilt.android.qualifiers.ApplicationContext*
import com.squareup.okhttp3.OkHttpClient*
import com.squareup.okhttp3.Request*
import java.util.concurrent.TimeUnit*

class BibleRepository(
    private val verseDao: VerseDao,
    private val interlinearDao: InterlinearDao,
    private val lexiconDao: LexiconDao,
    private val catenaRepository: CatenaRepository,
    @ApplicationContext private val context: Context
) {
    fun getChapter(bookId: Int, chapter: Int): Flow<List<VerseWithTexts>> {
        return verseDao.getChapter(bookId, chapter)
    }

    fun getInterlinearWordsForVerse(verseId: Int): Flow<List<InterlinearWordEntity>> {
        return interlinearDao.getWordsForVerse(verseId)
    }

    fun getLexiconEntry(lemma: String): Flow<LexiconEntity?> {
        return lexiconDao.getDefinition(lemma)
    }

    fun getAllBooks(): Flow<List<BookEntity>> {
        return verseDao.getAllBooks()
    }

    // Reverse mapping: bookId → bookCode (e.g., 1 → "GEN")
    private val bookIdToCode = mapOf(
        1 to "GEN", 2 to "EXO", 3 to "LEV", 4 to "NUM", 5 to "DEU",
        6 to "JOS", 7 to "JDG", 8 to "RUT", 9 to "1SA", 10 to "2SA",
        11 to "1KI", 12 to "2KI", 13 to "1CH", 14 to "2CH",
        15 to "EZR", 16 to "NEH", 17 to "TOB", 18 to "JDT", 19 to "EST",
        20 to "JOB", 21 to "PSA", 22 to "PRO", 23 to "ECC", 24 to "SNG",
        25 to "WIS", 26 to "SIR", 27 to "ISA", 28 to "JER",
        29 to "LAM", 30 to "BAR", 31 to "EZK", 32 to "DAN",
        33 to "HOS", 34 to "JOL", 35 to "AMO", 36 to "OBA",
        37 to "JON", 38 to "MIC", 39 to "NAH", 40 to "HAB",
        41 to "ZEP", 42 to "HAG", 43 to "ZEC", 44 to "MAL",
        45 to "1MA", 46 to "2MA", 47 to "MAT", 48 to "MAR",
        49 to "LUK", 50 to "JHN", 51 to "ACT", 52 to "ROM",
        53 to "1CO", 54 to "2CO", 55 to "GAL", 56 to "EPH",
        57 to "PHP", 58 to "COL", 59 to "1TH", 60 to "2TH",
        61 to "1TI", 62 to "2TI", 63 to "TIT", 64 to "PHM",
        65 to "HEB", 66 to "JAS", 67 to "1PE", 68 to "2PE",
        69 to "1JN", 70 to "2JN", 71 to "3JN", 72 to "JUD",
        73 to "REV"
    )

    fun getCatenaForVerse(bookId: Int, chapter: Int, verseNumber: Int): Flow<List<com.verbun.universalis.data.entities.CatenaCommentaryEntity>> {
        return catenaRepository.getCommentariesForVerse(bookId, chapter, verseNumber)
    }

    suspend fun isCatenaDownloaded(): Boolean {
        return catenaRepository.isDatabaseDownloaded()
    }

    suspend fun downloadCatena(): Boolean {
        return catenaRepository.downloadDatabase()
    }

    // References data classes
    data class Reference(val ref: String, val description: String)
    data class ReferenceEntry(val verse_ref: String, val references: List<Reference>)
    data class ReferencesData(val entries: List<ReferenceEntry>)

    // Load references for a specific verse (download if needed, filter by verse)
    suspend fun getReferencesForVerse(verseId: Int): List<Reference> {
        val cacheFile = File(context.filesDir, "cache/references.json")
        if (!cacheFile.exists()) {
            downloadFile(REFERENCES_URL, cacheFile)
        }

        return try {
            val jsonString = cacheFile.readText()
            val data = Json { ignoreUnknownKeys = true }.decodeFromString<ReferencesData>(jsonString)
            
            // Get verse to build ref
            val verse = getVerseByIdSync(verseId)
            if (verse != null) {
                val bookCode = bookIdToCode[verse.bookId] ?: return emptyList()
                val refPrefix = "${bookCode}.${verse.chapter}.${verse.verseNumber}"
                // Filter entries where verse_ref starts with refPrefix, then merge all references
                val matchingEntries = data.entries.filter { it.verse_ref.startsWith(refPrefix) }
                val allReferences = mutableListOf<Reference>()
                matchingEntries.forEach { entry ->
                    allReferences.addAll(entry.references)
                }
                allReferences
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Liturgical Calendar data classes
    data class LiturgicalEntry(val date: String, val celebration: String, val readings: List<ReadingRef>)
    data class ReadingRef(val book: String, val chapter: Int, val verse_start: Int?, val verse_end: Int?)
    data class LiturgicalCalendar(val readings: List<LiturgicalEntry>)

    // Get today's liturgical reading
    suspend fun getTodayLiturgicalReading(): LiturgicalEntry? {
        val cacheFile = File(context.filesDir, "cache/liturgical_calendar.json")
        if (!cacheFile.exists()) {
            downloadFile(LITURGICAL_URL, cacheFile)
        }

        return try {
            val jsonString = cacheFile.readText()
            val data = Json { ignoreUnknownKeys = true }.decodeFromString<LiturgicalCalendar>(jsonString)
            val today = java.time.LocalDate.now().toString() // "2024-01-01" format
            data.readings.find { it.date == today }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        // TODO: Replace with real GitHub raw URLs
        private const val CATENA_URL = "https://raw.githubusercontent.com/YOUR_USER/VERBUM_DATA/main/catena.json"
        private const val REFERENCES_URL = "https://raw.githubusercontent.com/YOUR_USER/VERBUM_DATA/main/references.json"
        private const val LITURGICAL_URL = "https://raw.githubusercontent.com/YOUR_USER/VERBUM_DATA/main/liturgical_calendar.json"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private suspend fun downloadFile(url: String, outputFile: File) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && body != null) {
                outputFile.parentFile?.mkdirs()
                outputFile.writeText(body)
                android.util.Log.i("BibleRepository", "Downloaded $url to ${outputFile.absolutePath}")
            } else {
                android.util.Log.e("BibleRepository", "Failed to download $url: ${response.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("BibleRepository", "Error downloading $url", e)
        }
    }

    // Extension to get verse synchronously (for suspend context)
    fun getVerseByIdSync(verseId: Int): VerseEntity? {
        return try {
            val flow = verseDao.getVerseById(verseId)
            kotlinx.coroutines.runBlocking {
                flow.first()
            }
        } catch (e: Exception) {
            null
        }
    }
}
