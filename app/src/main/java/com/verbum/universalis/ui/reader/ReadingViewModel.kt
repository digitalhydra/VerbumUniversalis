package com.verbum.universalis.ui.reader

import android.app.Application
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.universalis.data.repository.BibleRepository
import com.verbum.universalis.VerbumApplication
import com.verbum.universalis.data.json.Note
import com.verbum.universalis.data.daos.VerseWithTexts
import com.verbum.universalis.data.entities.InterlinearWordEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class Passage(val bookId: Int, val chapter: Int, val verseRange: IntRange?) {
    companion object {
        val BOOK_NAME_TO_ID = mapOf(
            // English
            "Genesis" to 1, "Gen" to 1, "Exodus" to 2, "Exod" to 2, "Leviticus" to 3, "Lev" to 3,
            "Numbers" to 4, "Num" to 4, "Deuteronomy" to 5, "Deut" to 5, "Joshua" to 6, "Josh" to 6,
            "Judges" to 7, "Judg" to 7, "Ruth" to 8, "1 Samuel" to 9, "1 Sam" to 9, "2 Samuel" to 10,
            "2 Sam" to 10, "1 Kings" to 11, "1 Kgs" to 11, "2 Kings" to 12, "2 Kgs" to 12,
            "1 Chronicles" to 13, "1 Chron" to 13, "2 Chronicles" to 14, "2 Chron" to 14,
            "Ezra" to 15, "Nehemiah" to 16, "Tobit" to 17, "Judith" to 18, "Esther" to 19,
            "Job" to 20, "Psalms" to 21, "Ps" to 21, "Psalm" to 21, "Proverbs" to 22, "Prov" to 22,
            "Ecclesiastes" to 23, "Eccl" to 23, "Song of Solomon" to 24, "Song" to 24, "Wisdom" to 25,
            "Sirach" to 26, "Isaiah" to 27, "Isa" to 27, "Jeremiah" to 28, "Jer" to 28,
            "Lamentations" to 29, "Lam" to 29, "Baruch" to 30, "Ezekiel" to 31, "Ezek" to 31,
            "Daniel" to 32, "Dan" to 32, "Hosea" to 33, "Hos" to 33, "Joel" to 34, "Amos" to 35,
            "Obadiah" to 36, "Obad" to 36, "Jonah" to 37, "Jon" to 37, "Micah" to 38, "Mic" to 38,
            "Nahum" to 39, "Nah" to 39, "Habakkuk" to 40, "Hab" to 40, "Zephaniah" to 41, "Zeph" to 41,
            "Haggai" to 42, "Hag" to 42, "Zechariah" to 43, "Zech" to 43, "Malachi" to 44, "Mal" to 44,
            "1 Maccabees" to 45, "2 Maccabees" to 46, "Matthew" to 47, "Matt" to 47, "Mark" to 48,
            "Luke" to 49, "John" to 50, "Acts" to 51, "Romans" to 52, "Rom" to 52,
            "1 Corinthians" to 53, "1 Cor" to 53, "2 Corinthians" to 54, "2 Cor" to 54,
            "Galatians" to 55, "Gal" to 55, "Ephesians" to 56, "Eph" to 56, "Philippians" to 57, "Phil" to 57,
            "Colossians" to 58, "Col" to 58, "1 Thessalonians" to 59, "1 Thess" to 59,
            "2 Thessalonians" to 60, "2 Thess" to 60, "1 Timothy" to 61, "1 Tim" to 61,
            "2 Timothy" to 62, "2 Tim" to 62, "Titus" to 63, "Philemon" to 64, "Hebrews" to 65,
            "Heb" to 65, "James" to 66, "Jas" to 66, "1 Peter" to 67, "1 Pet" to 67,
            "2 Peter" to 68, "2 Pet" to 68, "1 John" to 69, "2 John" to 70,
            "3 John" to 71, "Jude" to 72, "Revelation" to 73, "Rev" to 73
        )
        
        val BOOK_ID_TO_LONG_NAME = mapOf(
            1 to "Genesis", 2 to "Exodus", 3 to "Leviticus", 4 to "Numbers", 5 to "Deuteronomy",
            6 to "Joshua", 7 to "Judges", 8 to "Ruth", 9 to "1 Samuel", 10 to "2 Samuel",
            11 to "1 Kings", 12 to "2 Kings", 13 to "1 Chronicles", 14 to "2 Chronicles",
            15 to "Ezra", 16 to "Nehemiah", 17 to "Tobit", 18 to "Judith", 19 to "Esther",
            20 to "Job", 21 to "Psalms", 22 to "Proverbs", 23 to "Ecclesiastes",
            24 to "Song of Solomon", 25 to "Wisdom", 26 to "Sirach", 27 to "Isaiah",
            28 to "Jeremiah", 29 to "Lamentations", 30 to "Baruch", 31 to "Ezekiel",
            32 to "Daniel", 33 to "Hosea", 34 to "Joel", 35 to "Amos", 36 to "Obadiah",
            37 to "Jonah", 38 to "Micah", 39 to "Nahum", 40 to "Habakkuk",
            41 to "Zephaniah", 42 to "Haggai", 43 to "Zechariah", 44 to "Malachi",
            45 to "1 Maccabees", 46 to "2 Maccabees", 47 to "Matthew", 48 to "Mark",
            49 to "Luke", 50 to "John", 51 to "Acts", 52 to "Romans", 53 to "1 Corinthians",
            54 to "2 Corinthians", 55 to "Galatians", 56 to "Ephesians", 57 to "Philippians",
            58 to "Colossians", 59 to "1 Thessalonians", 60 to "2 Thessalonians",
            61 to "1 Timothy", 62 to "2 Timothy", 63 to "Titus", 64 to "Philemon",
            65 to "Hebrews", 66 to "James", 67 to "1 Peter", 68 to "2 Peter",
            69 to "1 John", 70 to "2 John", 71 to "3 John", 72 to "Jude", 73 to "Revelation"
        )

        val BOOK_ID_TO_NAME = BOOK_ID_TO_LONG_NAME

        fun fromString(query: String, bookNameToId: Map<String, Int>): Passage? {
            // Check for simple ID:CHAPTER format
            if (query.contains(":")) {
                val parts = query.split(":")
                val bId = parts[0].toIntOrNull()
                val cNum = parts[1].toIntOrNull()
                if (bId != null && cNum != null) {
                    return Passage(bId, cNum, null)
                }
            }

            val regex = Regex("(\\d*\\s*[a-zA-Z]+)\\s*(\\d+):?(\\d*)-?(\\d*)")
            val match = regex.find(query)
            if (match != null) {
                val (bookName, chap, vStart, vEnd) = match.destructured
                val cleanedBookName = bookName.trim()
                val bookId = bookNameToId[cleanedBookName] ?: bookNameToId.entries.find { 
                    it.key.contains(cleanedBookName, ignoreCase = true) || 
                    cleanedBookName.contains(it.key, ignoreCase = true)
                }?.value ?: return null
                val chapter = chap.toIntOrNull() ?: 1
                val start = vStart.toIntOrNull()
                val end = vEnd.toIntOrNull()
                val range = if (start != null && end != null) IntRange(start, end) else null
                return Passage(bookId, chapter, range)
            }
            // Try just chapter number
            val chapOnly = query.toIntOrNull()
            if (chapOnly != null) {
                return Passage(1, chapOnly, null) // Default to Genesis
            }
            return null
        }
    }
}

val Application.dataStore by preferencesDataStore(name = "verbum_settings")

@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val repository: BibleRepository,
    private val app: Application,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val LAST_PASSAGE_KEY = stringPreferencesKey("last_passage")

    private val _currentPassage: MutableStateFlow<Passage>
    val currentPassage: StateFlow<Passage>

    init {
        val bookId = savedStateHandle.get<Int>("bookId") ?: -1
        val chapter = savedStateHandle.get<Int>("chapter") ?: -1
        val verse = savedStateHandle.get<Int>("verse") ?: -1

        val initialPassage = if (bookId != -1 && chapter != -1) {
            Passage(bookId, chapter, if (verse != -1) IntRange(verse, verse) else null)
        } else {
            Passage(1, 1, null) // Temporary default
        }

        _currentPassage = MutableStateFlow(initialPassage)
        currentPassage = _currentPassage.asStateFlow()

        if (bookId == -1 || chapter == -1) {
            // Load last read passage from DataStore only if no arguments provided
            viewModelScope.launch {
                app.dataStore.data.first().let { preferences ->
                    val lastPassage = preferences[LAST_PASSAGE_KEY]
                    if (lastPassage != null) {
                        val passage = Passage.fromString(lastPassage, Passage.BOOK_NAME_TO_ID)
                        if (passage != null) {
                            _currentPassage.value = passage
                        }
                    }
                }
            }
        }
    }

    // Language codes: "en_DRB"=English, "es_PLA"=Spanish, "la_VUL"=Latin, "el_GRK"=Greek
    private val _activeLanguage = MutableStateFlow("en_DRB") // Default: English
    val activeLanguage: StateFlow<String> = _activeLanguage.asStateFlow()

    private val _showNoteBottomSheet = MutableStateFlow(false)
    val showNoteBottomSheet: StateFlow<Boolean> = _showNoteBottomSheet.asStateFlow()

    // Combined note + highlight bottom sheet (shown when verse selected in selection mode)
    private val _showNoteHighlightSheet = MutableStateFlow(false)
    val showNoteHighlightSheet: StateFlow<Boolean> = _showNoteHighlightSheet.asStateFlow()

    private val _selectedVerseForNoteHighlight = MutableStateFlow<Int?>(null)
    val selectedVerseForNoteHighlight: StateFlow<Int?> = _selectedVerseForNoteHighlight.asStateFlow()

    fun showNoteHighlightSheet(verseId: Int) {
        _selectedVerseForNoteHighlight.value = verseId
        _showNoteHighlightSheet.value = true
    }

    fun hideNoteHighlightSheet() {
        _showNoteHighlightSheet.value = false
        _selectedVerseForNoteHighlight.value = null
    }

    private val _selectedVerseIdForNote = MutableStateFlow<Int?>(null)
    val selectedVerseIdForNote: StateFlow<Int?> = _selectedVerseIdForNote.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val verses: Flow<List<VerseWithTexts>> = combine(_currentPassage, _activeLanguage) { passage, _ -> 
        passage 
    }.flatMapLatest { passage ->
        repository.getChapter(passage.bookId, passage.chapter)
    }
    
    // Interlinear words (Greek for NT, Hebrew for OT)
    @OptIn(ExperimentalCoroutinesApi::class)
    val interlinearWords: Flow<List<InterlinearWordEntity>> = combine(_currentPassage, _activeLanguage) { p, l -> p to l }
        .flatMapLatest { (passage, lang) ->
            if (lang == "il_IL") {
                repository.getGreekWordsForChapter(passage.bookId, passage.chapter)
            } else {
                flowOf(emptyList())
            }
        }
    
    // Load Greek words when language changed
    fun loadGreekWordsIfNeeded() {
        // No longer needed as greekWords is a derived Flow
    }

    // Right pane toggle (tablet/mobile)
    private val _showStudyInspector = MutableStateFlow(false)
    val showStudyInspector: StateFlow<Boolean> = _showStudyInspector.asStateFlow()

    fun toggleStudyInspector() {
        _showStudyInspector.value = !_showStudyInspector.value
    }

    // For passing Greek word selection to StudyInspector
    private val _selectedGreekWord = MutableStateFlow<InterlinearWordEntity?>(null)
    val selectedGreekWord: StateFlow<InterlinearWordEntity?> = _selectedGreekWord.asStateFlow()

    fun selectGreekWord(word: InterlinearWordEntity) {
        _selectedGreekWord.value = word
    }

    fun setPassage(bookId: Int, chapter: Int, verse: Int? = null) {
        val range = if (verse != null) IntRange(verse, verse) else null
        _currentPassage.value = Passage(bookId, chapter, range)
        saveLastPassage(bookId, chapter)
    }

    fun setPassageFromString(query: String) {
        val passage = Passage.fromString(query, Passage.BOOK_NAME_TO_ID)
        if (passage != null) {
            _currentPassage.value = passage
            saveLastPassage(passage.bookId, passage.chapter)
        } else {
            // Try just chapter number
            val chapOnly = query.toIntOrNull()
            if (chapOnly != null) {
                _currentPassage.value = Passage(1, chapOnly, null)
                saveLastPassage(1, chapOnly)
            }
        }
    }

    fun nextChapter() {
        Log.d("ReadingViewModel", "nextChapter called")
        viewModelScope.launch {
            val current = _currentPassage.value
            val maxChapter = repository.getMaxChapterForBook(current.bookId).first() ?: 1
            Log.d("ReadingViewModel", "current book: ${current.bookId}, chapter: ${current.chapter}, maxChapter: $maxChapter")
            if (current.chapter < maxChapter) {
                setPassage(current.bookId, current.chapter + 1)
            } else {
                // Next book
                if (current.bookId < 73) { // 73 is the last Catholic book ID
                    Log.d("ReadingViewModel", "Moving to next book: ${current.bookId + 1}")
                    setPassage(current.bookId + 1, 1)
                }
            }
        }
    }

    fun previousChapter() {
        Log.d("ReadingViewModel", "previousChapter called")
        viewModelScope.launch {
            val current = _currentPassage.value
            if (current.chapter > 1) {
                setPassage(current.bookId, current.chapter - 1)
            } else {
                // Previous book
                if (current.bookId > 1) {
                    val prevBookId = current.bookId - 1
                    val maxChapterOfPrev = repository.getMaxChapterForBook(prevBookId).first() ?: 1
                    Log.d("ReadingViewModel", "Moving to previous book: $prevBookId, maxChapter: $maxChapterOfPrev")
                    setPassage(prevBookId, maxChapterOfPrev)
                }
            }
        }
    }

    private fun saveLastPassage(bookId: Int, chapter: Int) {
        viewModelScope.launch {
            app.dataStore.edit { preferences ->
                preferences[LAST_PASSAGE_KEY] = "$bookId:$chapter"
            }
        }
    }

    fun setLanguage(langCode: String) {
        _activeLanguage.value = langCode
        // Load Greek words if Greek selected
        if (langCode == "el_GRK") {
            loadGreekWordsIfNeeded()
        }
    }

    // Cycle through languages: en_DRB -> es_PLA -> la_VUL -> el_GRK -> he_HEB -> en_DRB
    fun toggleLanguage() {
        _activeLanguage.value = when (_activeLanguage.value) {
            "en_DRB" -> "es_PLA"
            "es_PLA" -> "la_VUL"
            "la_VUL" -> "el_GRK"
            "el_GRK" -> "he_HEB"
            else -> "en_DRB"
        }
    }

    fun getLanguageDisplayName(): String {
        return when (_activeLanguage.value) {
            "en_DRB" -> "EN"
            "es_PLA" -> "ES"
            "la_VUL" -> "LA"
            "el_GRK" -> "EL" // Greek
            "il_IL" -> "IL" // Hebrew
            else -> "EN"
        }
    }

    // Selection mode state
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    // Currently selected verse (for highlight/note)
    private val _selectedVerseId = MutableStateFlow<Int?>(null)
    val selectedVerseId: StateFlow<Int?> = _selectedVerseId.asStateFlow()

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedVerseId.value = null
        }
    }

    fun selectVerse(verseId: Int) {
        _selectedVerseId.value = verseId
    }

    fun getDisplayText(verseWithTexts: VerseWithTexts, langCode: String): String {
        return verseWithTexts.texts.find { it.lang_code == langCode }?.content ?: "N/A"
    }

    fun getDisplayNameForLangCode(langCode: String): String {
        return when (langCode) {
            "en_DRB" -> "English (Douay-Rheims)"
            "es_PLA" -> "Español (Platense)"
            "la_VUL" -> "Latina (Vulgata)"
            "el_GRK" -> "Ελληνικά (Greek)"
            else -> langCode
        }
    }

    // Get display string for a passage (e.g., "Genesis 1")
    fun getPassageReference(p: Passage): String {
        val bookName = Passage.BOOK_ID_TO_NAME[p.bookId] ?: "Book ${p.bookId}"
        return "$bookName ${p.chapter}"
    }

    @Deprecated("Use getPassageReference(Passage) instead", ReplaceWith("getPassageReference(currentPassage.value)"))
    fun getCurrentPassageReference(): String {
        return getPassageReference(_currentPassage.value)
    }

    // Unified save: note with optional highlight
    fun saveNoteWithHighlight(noteContent: String, highlightColorId: Int?) {
        val verseId = _selectedVerseId.value ?: return
        viewModelScope.launch {
            val fileManager = (app as VerbumApplication).fileManager
            val notes = fileManager.loadNotes().toMutableList()
            notes.add(
                Note(
                    verseId = verseId,
                    content = noteContent,
                    highlightColorId = highlightColorId,
                    timestamp = System.currentTimeMillis()
                )
            )
            fileManager.saveNotes(notes)
            _selectedVerseId.value = null
            _isSelectionMode.value = false
        }
    }

    // Legacy compat
    fun saveNote(content: String) = saveNoteWithHighlight(content, null)
    fun saveHighlight(colorId: Int) = saveNoteWithHighlight("", colorId)

    fun getHighlightsForVerse(verseId: Int): List<Int> {
        val fileManager = (app as VerbumApplication).fileManager
        val highlights = fileManager.loadHighlights()
        return highlights.filter { it.verseId == verseId }.map { it.colorId }
    }
}
