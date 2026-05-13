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
            17 -> "Tobit"
            18 -> "Judith"
            19 -> "Esther"
            20 -> "Job"
            21 -> "Psalms"
            22 -> "Proverbs"
            23 -> "Ecclesiastes"
            24 -> "Song of Solomon"
            25 -> "Wisdom"
            26 -> "Sirach"
            27 -> "Isaiah"
            28 -> "Jeremiah"
            29 -> "Lamentations"
            30 -> "Baruch"
            31 -> "Ezekiel"
            32 -> "Daniel"
            33 -> "Hosea"
            34 -> "Joel"
            35 -> "Amos"
            36 -> "Obadiah"
            37 -> "Jonah"
            38 -> "Micah"
            39 -> "Nahum"
            40 -> "Habakkuk"
            41 -> "Zephaniah"
            42 -> "Haggai"
            43 -> "Zechariah"
            44 -> "Malachi"
            45 -> "1 Maccabees"
            46 -> "2 Maccabees"
            47 -> "Matthew"
            48 -> "Mark"
            49 -> "Luke"
            50 -> "John"
            51 -> "Acts"
            52 -> "Romans"
            53 -> "1 Corinthians"
            54 -> "2 Corinthians"
            55 -> "Galatians"
            56 -> "Ephesians"
            57 -> "Philippians"
            58 -> "Colossians"
            59 -> "1 Thessalonians"
            60 -> "2 Thessalonians"
            61 -> "1 Timothy"
            62 -> "2 Timothy"
            63 -> "Titus"
            64 -> "Philemon"
            65 -> "Hebrews"
            66 -> "James"
            67 -> "1 Peter"
            68 -> "2 Peter"
            69 -> "1 John"
            70 -> "2 John"
            71 -> "3 John"
            72 -> "Jude"
            73 -> "Revelation"
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