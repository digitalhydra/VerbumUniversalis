package com.verbum.universalis.data.repository

import android.content.Context
import com.verbum.universalis.data.daos.*
import com.verbum.universalis.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class BibleRepository(
    private val verseDao: VerseDao,
    private val interlinearDao: InterlinearDao,
    private val lexiconDao: LexiconDao,
    private val catenaRepository: CatenaRepository,
    private val crossRefsRepository: CrossRefsRepository,
    @ApplicationContext private val context: Context
) {
    fun getChapter(bookId: Int, chapter: Int): Flow<List<VerseWithTexts>> {
        return verseDao.getChapter(bookId, chapter)
    }

    fun getVerseById(verseId: Int): Flow<VerseEntity?> {
        return verseDao.getVerseById(verseId)
    }

    fun getInterlinearWordsForVerse(verseId: Int): Flow<List<InterlinearWordEntity>> {
        return interlinearDao.getWordsForVerse(verseId)
    }

    fun getGreekWordsForChapter(bookId: Int, chapter: Int): Flow<List<InterlinearWordEntity>> {
        return interlinearDao.getWordsForChapter(bookId, chapter)
    }

    fun getLexiconEntry(lemma: String): Flow<LexiconEntity?> {
        return lexiconDao.getDefinition(lemma)
    }

    fun getAllBooks(): Flow<List<BookEntity>> {
        return verseDao.getAllBooks()
    }

    fun getMaxChapterForBook(bookId: Int): Flow<Int?> {
        return verseDao.getMaxChapterForBook(bookId)
    }

    fun getMaxVerseForChapter(bookId: Int, chapter: Int): Flow<Int?> {
        return verseDao.getMaxVerseForChapter(bookId, chapter)
    }

    // Catena methods
    suspend fun getCatenaForVerse(bookId: Int, chapter: Int, verseNumber: Int): List<CatenaCommentaryEntity> {
        return catenaRepository.getCommentariesForVerse(bookId, chapter, verseNumber).first()
    }

    suspend fun getCatenaForVerse(verseId: Int): List<CatenaCommentaryEntity> {
        val verse = getVerseByIdSync(verseId) ?: return emptyList()
        return getCatenaForVerse(verse.book_id, verse.chapter, verse.verse_number)
    }

    suspend fun isCatenaDownloaded(): Boolean {
        return catenaRepository.isDatabaseDownloaded()
    }

    suspend fun downloadCatena(): Boolean {
        return catenaRepository.downloadDatabase()
    }

    // Cross-references methods
    fun getCrossRefsForVerse(bookCode: String, chapter: Int, verse: Int): Flow<List<CrossReferenceEntity>> {
        return crossRefsRepository.getCrossRefsForVerse(bookCode, chapter, verse)
    }

    fun getCrossRefsForChapter(bookCode: String, chapter: Int): Flow<List<CrossReferenceEntity>> {
        return crossRefsRepository.getCrossRefsForChapter(bookCode, chapter)
    }

    suspend fun isCrossRefsDownloaded(): Boolean {
        return crossRefsRepository.isDatabaseDownloaded()
    }

    suspend fun downloadCrossRefs(): Boolean {
        return crossRefsRepository.downloadDatabase()
    }

    // Reference data classes (for JSON)
    @kotlinx.serialization.Serializable
    data class Reference(val ref: String, val description: String)
    @kotlinx.serialization.Serializable
    data class ReferenceEntry(val verse_ref: String, val references: List<Reference>)
    @kotlinx.serialization.Serializable
    data class ReferencesData(val entries: List<ReferenceEntry>)

    // Get references for a verse with descriptions
    suspend fun getReferencesForVerse(verseId: Int): List<Reference> {
        val verse = getVerseByIdSync(verseId) ?: return emptyList()
        return getReferencesForVerse(verse.book_id, verse.chapter, verse.verse_number)
    }

    suspend fun getReferencesForVerse(bookId: Int, chapter: Int, verseNumber: Int): List<Reference> {
        val bookCode = getBookCodeForId(bookId) ?: return emptyList()
        
        val refPrefix = "${bookCode}.${chapter}.${verseNumber}"
        val cacheFile = File(context.filesDir, "cache/references.json")
        
        // Download and parse TSV if not cached yet
        if (!cacheFile.exists()) {
            downloadAndParseReferences(cacheFile)
            if (!cacheFile.exists()) return emptyList()
        }

        return try {
            val jsonString = cacheFile.readText()
            val data = Json { ignoreUnknownKeys = true }.decodeFromString<ReferencesData>(jsonString)
            val matchingEntries = data.entries.filter { it.verse_ref == refPrefix }
            val allReferences = mutableListOf<Reference>()
            matchingEntries.forEach { entry ->
                allReferences.addAll(entry.references)
            }
            allReferences
        } catch (e: Exception) {
            android.util.Log.e("BibleRepository", "Failed to read references cache", e)
            emptyList()
        }
    }

    /** Preload references TSV (download + parse + cache). Safe to call from any coroutine. */
    suspend fun preloadReferences(): Boolean {
        val cacheFile = File(context.filesDir, "cache/references.json")
        if (cacheFile.exists()) return true
        downloadAndParseReferences(cacheFile)
        return cacheFile.exists()
    }

    private suspend fun downloadAndParseReferences(cacheFile: File) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val request = Request.Builder().url(REFERENCES_URL).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                android.util.Log.e("BibleRepository", "Failed to download references: ${response.code}")
                return@withContext
            }
            val tsv = response.body?.string() ?: return@withContext
            
            // Parse TSV: From Verse\tTo Verse\tVotes
            val lines = tsv.lines()
            // Group by from-verse
            val grouped = mutableMapOf<String, MutableList<Reference>>()
            for (line in lines) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("From")) continue
                val parts = line.split("\t")
                if (parts.size < 2) continue
                val fromRef = normalizeTsvRef(parts[0].trim())
                val toRef = normalizeTsvRef(parts[1].trim())
                val votes = parts.getOrNull(2)?.trim()?.toIntOrNull()
                val desc = if (votes != null) "votes: $votes" else ""
                grouped.getOrPut(fromRef) { mutableListOf() }.add(Reference(toRef, desc))
            }
            
            // Convert to JSON and cache
            val entries = grouped.map { (verseRef, refs) ->
                ReferenceEntry(verse_ref = verseRef, references = refs)
            }
            val data = ReferencesData(entries = entries)
            val json = Json { prettyPrint = false }.encodeToString(data)
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(json)
            android.util.Log.i("BibleRepository", "Parsed ${entries.size} reference entries")
        } catch (e: Exception) {
            android.util.Log.e("BibleRepository", "Error downloading/parsing references", e)
        }
    }

    // Navigation: parse reference string "GEN.1.1" or "Gen.1.1" to (bookId, chapter, verse)
    fun parseReference(ref: String): Triple<Int, Int, Int>? {
        val normalized = normalizeTsvRef(ref)
        val parts = normalized.split(".")
        if (parts.size < 3) return null

        val bookCode = parts[0]
        val chapter = parts[1].toIntOrNull() ?: return null
        val verse = parts[2].split("-")[0].toIntOrNull() ?: return null

        val bookId = getBookIdForCode(bookCode) ?: return null
        return Triple(bookId, chapter, verse)
    }

    // Private helpers
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
        45 to "1MA", 46 to "2MA", 47 to "MAT", 48 to "MRK",
        49 to "LUK", 50 to "JHN", 51 to "ACT", 52 to "ROM",
        53 to "1CO", 54 to "2CO", 55 to "GAL", 56 to "EPH",
        57 to "PHP", 58 to "COL", 59 to "1TH", 60 to "2TH",
        61 to "1TI", 62 to "2TI", 63 to "TIT", 64 to "PHM",
        65 to "HEB", 66 to "JAS", 67 to "1PE", 68 to "2PE",
        69 to "1JN", 70 to "2JN", 71 to "3JN", 72 to "JUD",
        73 to "REV"
    )

    private fun getBookCodeForId(bookId: Int): String? = bookIdToCode[bookId]
    private fun getBookIdForCode(code: String): Int? = bookIdToCode.entries.find { it.value == code }?.key

    // Map TSV book codes (Gen, Matt, Ps, 2Pet, etc.) → our internal book codes (GEN, MAT, PSA, 2PE)
    private val tsvBookCodeToInternal: Map<String, String> = mapOf(
        "Gen" to "GEN", "Exod" to "EXO", "Lev" to "LEV", "Num" to "NUM", "Deut" to "DEU",
        "Josh" to "JOS", "Judg" to "JDG", "Ruth" to "RUT",
        "1Sam" to "1SA", "2Sam" to "2SA", "1Kgs" to "1KI", "2Kgs" to "2KI",
        "1Chr" to "1CH", "2Chr" to "2CH", "Ezra" to "EZR", "Neh" to "NEH",
        "Tob" to "TOB", "Jdt" to "JDT", "Esth" to "EST",
        "Job" to "JOB", "Ps" to "PSA", "Prov" to "PRO", "Eccl" to "ECC",
        "Song" to "SNG", "Wis" to "WIS", "Sir" to "SIR",
        "Isa" to "ISA", "Jer" to "JER", "Lam" to "LAM", "Bar" to "BAR",
        "Ezek" to "EZK", "Dan" to "DAN",
        "Hos" to "HOS", "Joel" to "JOL", "Amos" to "AMO", "Obad" to "OBA",
        "Jonah" to "JON", "Mic" to "MIC", "Nah" to "NAH", "Hab" to "HAB",
        "Zeph" to "ZEP", "Hag" to "HAG", "Zech" to "ZEC", "Mal" to "MAL",
        "1Macc" to "1MA", "2Macc" to "2MA",
        "Matt" to "MAT", "Mark" to "MRK", "Luke" to "LUK", "John" to "JHN",
        "Acts" to "ACT", "Rom" to "ROM",
        "1Cor" to "1CO", "2Cor" to "2CO", "Gal" to "GAL", "Eph" to "EPH",
        "Phil" to "PHP", "Col" to "COL",
        "1Thess" to "1TH", "2Thess" to "2TH",
        "1Tim" to "1TI", "2Tim" to "2TI", "Titus" to "TIT",
        "Phlm" to "PHM", "Heb" to "HEB", "Jas" to "JAS",
        "1Pet" to "1PE", "2Pet" to "2PE",
        "1John" to "1JN", "2John" to "2JN", "3John" to "3JN",
        "Jude" to "JUD", "Rev" to "REV"
    )

    /** Convert a TSV verse ref ("Gen.1.1", "Rom.1.19-Rom.1.20") to our internal format */
    private fun normalizeTsvRef(ref: String): String {
        if (ref.contains('-')) {
            val parts = ref.split("-", limit = 2)
            return normalizeOneRef(parts[0]) + "-" + normalizeOneRef(parts[1])
        }
        return normalizeOneRef(ref)
    }

    private fun normalizeOneRef(ref: String): String {
        val dotIndex = ref.indexOf('.')
        if (dotIndex < 0) return ref.uppercase()
        val bookCode = ref.substring(0, dotIndex)
        val rest = ref.substring(dotIndex)
        val internalCode = tsvBookCodeToInternal[bookCode] ?: bookCode.uppercase()
        return internalCode + rest
    }

    private fun getVerseByIdSync(verseId: Int): VerseEntity? {
        return try {
            val flow = verseDao.getVerseById(verseId)
            kotlinx.coroutines.runBlocking { flow.first() }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val REFERENCES_URL = "https://raw.githubusercontent.com/digitalhydra/VerbumUniversalis/refs/heads/master/raw_data/cross_references.txt"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
}
