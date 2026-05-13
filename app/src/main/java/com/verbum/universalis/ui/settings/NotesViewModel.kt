package com.verbum.universalis.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.universalis.data.json.FileManager
import com.verbum.universalis.data.json.Note
import com.verbum.universalis.data.repository.BibleRepository
import com.verbum.universalis.data.entities.VerseEntity
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteDisplay(
    val note: Note,
    val bookName: String,
    val chapter: Int,
    val verse: Int
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val fileManager: FileManager,
    private val bibleRepository: BibleRepository
) : ViewModel() {

    private val _notes = MutableStateFlow<List<NoteDisplay>>(emptyList())
    val notes: StateFlow<List<NoteDisplay>> = _notes.asStateFlow()

    private val _verseLookups = MutableStateFlow<Map<Int, Triple<String, Int, Int>>>(emptyMap())

    init {
        loadNotes()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            val rawNotes = fileManager.loadNotes()
            val uniqueVerseIds = rawNotes.map { it.verseId }.distinct()
            
            // Pre-fetch all unique verseIds
            val verseMap = mutableMapOf<Int, Triple<String, Int, Int>>()
            uniqueVerseIds.forEach { verseId ->
                bibleRepository.getVerseById(verseId)
                    .mapNotNull { it }
                    .collectLatest { verse ->
                        verseMap[verseId] = Triple(
                            getBookName(verse.book_id),
                            verse.chapter,
                            verse.verse_number
                        )
                        // Re-map notes with updated verse info
                        _notes.value = rawNotes.map { note ->
                            val (book, ch, v) = verseMap[note.verseId] ?: Triple("?", 0, 0)
                            NoteDisplay(note = note, bookName = book, chapter = ch, verse = v)
                        }
                    }
            }
        }
    }

    fun deleteNote(note: Note) {
        val currentNotes = fileManager.loadNotes()
        val updatedNotes = currentNotes.filterNot { it.timestamp == note.timestamp }
        fileManager.saveNotes(updatedNotes)
        loadNotes()
    }

    private fun getBookName(bookId: Int): String {
        return when (bookId) {
            1 -> "Genesis"
            2 -> "Exodus"
            3 -> "Leviticus"
            4 -> "Numbers"
            5 -> "Deuteronomy"
            6 -> "Joshua"
            7 -> "Judges"
            8 -> "Ruth"
            9 -> "1 Samuel"
            10 -> "2 Samuel"
            11 -> "1 Kings"
            12 -> "2 Kings"
            13 -> "1 Chronicles"
            14 -> "2 Chronicles"
            15 -> "Ezra"
            16 -> "Nehemiah"
            17 -> "Esther"
            18 -> "Job"
            19 -> "Psalms"
            20 -> "Proverbs"
            21 -> "Ecclesiastes"
            22 -> "Song of Solomon"
            23 -> "Isaiah"
            24 -> "Jeremiah"
            25 -> "Lamentations"
            26 -> "Ezekiel"
            27 -> "Daniel"
            28 -> "Hosea"
            29 -> "Joel"
            30 -> "Amos"
            31 -> "Obadiah"
            32 -> "Jonah"
            33 -> "Micah"
            34 -> "Nahum"
            35 -> "Habakkuk"
            36 -> "Zephaniah"
            37 -> "Haggai"
            38 -> "Zechariah"
            39 -> "Malachi"
            40 -> "Matthew"
            41 -> "Mark"
            42 -> "Luke"
            43 -> "John"
            44 -> "Acts"
            45 -> "Romans"
            46 -> "1 Corinthians"
            47 -> "2 Corinthians"
            48 -> "Galatians"
            49 -> "Ephesians"
            50 -> "Philippians"
            51 -> "Colossians"
            52 -> "1 Thessalonians"
            53 -> "2 Thessalonians"
            54 -> "1 Timothy"
            55 -> "2 Timothy"
            56 -> "Titus"
            57 -> "Philemon"
            58 -> "Hebrews"
            59 -> "James"
            60 -> "1 Peter"
            61 -> "2 Peter"
            62 -> "1 John"
            63 -> "2 John"
            64 -> "3 John"
            65 -> "Jude"
            66 -> "Revelation"
            else -> "Unknown Book ($bookId)"
        }
    }

    companion object {
        fun formatTimestamp(timestamp: Long): String {
            val javaDate = java.util.Date(timestamp)
            val format = java.text.SimpleDateFormat("MMM d, yyyy HH:mm")
            return format.format(javaDate)
        }

        fun getHighlightColor(colorId: Int): Color {
            val palette = listOf(
                0xFFFFEB3B, 0xFF4CAF50, 0xFF2196F3, 0xFF9C27B0,
                0xFFFF5722, 0xFF00BCD4, 0xFF8BC34A, 0xFFFF9800,
                0xFFE91E63, 0xFF607D8B, 0xFF795548, 0xFFFFC107,
                0xFF009688, 0xFFFF5252, 0xFFAAAAAA, 0xFFFFFF00,
                0xFF00FFFF, 0xFFFF00FF
            )
            return Color(palette[colorId % palette.size].toULong())
        }
    }
}