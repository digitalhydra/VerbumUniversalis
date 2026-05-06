package com.verbum.universalis.ui.reader

import android.app.Application
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.universalis.data.repository.BibleRepository
import com.verbum.universalis.data.entities.VerseWithTexts
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            "3 John" to 71, "Jude" to 72, "Revelation" to 73, "Rev" to 73"
        )

        fun fromString(query: String, bookNameToId: Map<String, Int>): Passage? {
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
    private val app: Application
) : ViewModel() {

    private val LAST_PASSAGE_KEY = stringPreferencesKey("last_passage")

    private val _currentPassage = MutableStateFlow(Passage(1, 1, null)) // Default: Genesis 1
    val currentPassage: StateFlow<Passage> = _currentPassage.asStateFlow()

    init {
        // Load last read passage from DataStore
        viewModelScope.launch {
            app.dataStore.data.collect { preferences ->
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

    // Language codes: "en_DRB"=English, "es_PLA"=Spanish, "la_VUL"=Latin
    private val _activeLanguage = MutableStateFlow("en_DRB") // Default: English
    val activeLanguage: StateFlow<String> = _activeLanguage.asStateFlow()

    private val _showNoteBottomSheet = MutableStateFlow(false)
    val showNoteBottomSheet: StateFlow<Boolean> = _showNoteBottomSheet.asStateFlow()

    private val _selectedVerseIdForNote = MutableStateFlow<Int?>(null)
    val selectedVerseIdForNote: StateFlow<Int?> = _selectedVerseIdForNote.asStateFlow()

    val verses: Flow<List<VerseWithTexts>> = _currentPassage.map { passage ->
        repository.getChapter(passage.bookId, passage.chapter)
    }

    fun setPassage(bookId: Int, chapter: Int) {
        _currentPassage.value = Passage(bookId, chapter, null)
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

    private fun saveLastPassage(bookId: Int, chapter: Int) {
        viewModelScope.launch {
            app.dataStore.edit { preferences ->
                preferences[LAST_PASSAGE_KEY] = "$bookId:$chapter"
            }
        }
    }

    fun setLanguage(langCode: String) {
        _activeLanguage.value = langCode
    }

    // Cycle through languages: en_DRB -> es_PLA -> la_VUL -> en_DRB
    fun toggleLanguage() {
        _activeLanguage.value = when (_activeLanguage.value) {
            "en_DRB" -> "es_PLA"
            "es_PLA" -> "la_VUL"
            else -> "en_DRB"
        }
    }

    fun getLanguageDisplayName(): String {
        return when (_activeLanguage.value) {
            "en_DRB" -> "EN"
            "es_PLA" -> "ES"
            "la_VUL" -> "LA"
            else -> "EN"
        }
    }

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
    }

    fun getDisplayText(verseWithTexts: VerseWithTexts): String {
        val langCode = _activeLanguage.value
        return verseWithTexts.texts.find { it.lang_code == langCode }?.content ?: "N/A"
    }

    fun getDisplayNameForLangCode(langCode: String): String {
        return when (langCode) {
            "en_DRB" -> "English (Douay-Rheims)"
            "es_PLA" -> "Español (Platense)"
            "la_VUL" -> "Latina (Vulgata)"
            else -> langCode
        }
    }

    fun showNoteSheet(verseId: Int) {
        _selectedVerseIdForNote.value = verseId
        _showNoteBottomSheet.value = true
    }

    fun hideNoteSheet() {
        _showNoteBottomSheet.value = false
        _selectedVerseIdForNote.value = null
    }

    fun saveNote(content: String) {
        val verseId = _selectedVerseIdForNote.value ?: return
        viewModelScope.launch {
            val fileManager = (app as VerbumApplication).fileManager
            val notes = fileManager.loadNotes().toMutableList()
            notes.add(Note(verseId = verseId, content = content, timestamp = System.currentTimeMillis()))
            fileManager.saveNotes(notes)
            hideNoteSheet()
        }
    }

    private val _showHighlightPicker = MutableStateFlow(false)
    val showHighlightPicker: StateFlow<Boolean> = _showHighlightPicker.asStateFlow()

    private val _selectedVerseIdForHighlight = MutableStateFlow<Int?>(null)
    val selectedVerseIdForHighlight: StateFlow<Int?> = _selectedVerseIdForHighlight.asStateFlow()

    fun showHighlightPicker(verseId: Int) {
        _selectedVerseIdForHighlight.value = verseId
        _showHighlightPicker.value = true
    }

    fun hideHighlightPicker() {
        _showHighlightPicker.value = false
        _selectedVerseIdForHighlight.value = null
    }

    fun saveHighlight(colorId: Int) {
        val verseId = _selectedVerseIdForHighlight.value ?: return
        viewModelScope.launch {
            val fileManager = (app as VerbumApplication).fileManager
            val highlights = fileManager.loadHighlights().toMutableList()
            highlights.add(Highlight(verseId = verseId, colorId = colorId, timestamp = System.currentTimeMillis()))
            fileManager.saveHighlights(highlights)
            hideHighlightPicker()
        }
    }

    fun getHighlightsForVerse(verseId: Int): List<Int> {
        val fileManager = (app as VerbumApplication).fileManager
        val highlights = fileManager.loadHighlights()
        return highlights.filter { it.verseId == verseId }.map { it.colorId }
    }
}
