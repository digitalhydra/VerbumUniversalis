package com.verbum.universalis.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.verbum.universalis.data.json.FileManager
import com.verbum.universalis.data.json.Note as JsonNote
import com.verbum.universalis.data.repository.BibleRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onBack: () -> Unit = {},
    fileManager: FileManager = hiltViewModel(),
    bibleRepository: BibleRepository = hiltViewModel()
) {
    val context = LocalContext.current
    var notes by remember { 
        mutableStateOf(fileManager.loadNotes())
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Notes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (notes.isEmpty()) {
                Text(
                    text = "No notes yet. Create notes from the Reading or Interlinear screens.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 48.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(notes) { index, note ->
                        NoteItem(
                            note = note,
                            noteIndex = index,
                            onNoteDeleted = { deletedNote ->
                                notes = fileManager.loadNotes().filter { it.timestamp != deletedNote.timestamp }
                                fileManager.saveNotes(notes)
                            },
                            bibleRepository = bibleRepository
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteItem(
    note: JsonNote,
    noteIndex: Int,
    onNoteClick: (JsonNote) -> Unit = {},
    onNoteDeleted: (JsonNote) -> Unit = {},
    bibleRepository: BibleRepository
) {
    var bookName by remember { mutableStateOf("Loading...") }
    var chapterNum by remember { mutableStateOf(0) }
    var verseNum by remember { mutableStateOf(0) }
    var verseLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(note.verseId) {
        bookName = "Loading..."
        chapterNum = 0
        verseNum = 0
        verseLoaded = false

        try {
            val verse = bibleRepository.getVerseById(note.verseId)
                .mapNotNull { verseEntity -> verseEntity }
                .first()
            if (verse != null) {
                bookName = getBookName(verse.book_id)
                chapterNum = verse.chapter
                verseNum = verse.verse_number
                verseLoaded = true
            }
        } catch (_: Exception) {
            bookName = "Book ${note.verseId}"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable { onNoteClick(note) }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (verseLoaded) "$bookName $chapterNum:$verseNum" else "Loading...",
                    style = MaterialTheme.typography.titleMedium
                )
                
                IconButton(
                    onClick = { onNoteDeleted(note) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete note",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                val highlightColor = note.highlightColorId?.let { getHighlightColor(it) } 
                    ?: MaterialTheme.colorScheme.surfaceVariant
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp, 12.dp)
                            .background(highlightColor, RoundedCornerShape(2.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "Highlight",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = formatTimestamp(note.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getBookName(bookId: Int): String {
    val names = mapOf(
        1 to "Genesis", 2 to "Exodus", 3 to "Leviticus", 4 to "Numbers",
        5 to "Deuteronomy", 6 to "Joshua", 7 to "Judges", 8 to "Ruth",
        9 to "1 Samuel", 10 to "2 Samuel", 11 to "1 Kings", 12 to "2 Kings",
        13 to "1 Chronicles", 14 to "2 Chronicles", 15 to "Ezra", 16 to "Nehemiah",
        17 to "Esther", 18 to "Job", 19 to "Psalms", 20 to "Proverbs",
        21 to "Ecclesiastes", 22 to "Song of Solomon", 23 to "Isaiah", 24 to "Jeremiah",
        25 to "Lamentations", 26 to "Ezekiel", 27 to "Daniel", 28 to "Hosea",
        29 to "Joel", 30 to "Amos", 31 to "Obadiah", 32 to "Jonah",
        33 to "Micah", 34 to "Nahum", 35 to "Habakkuk", 36 to "Zephaniah",
        37 to "Haggai", 38 to "Zechariah", 39 to "Malachi", 40 to "Matthew",
        41 to "Mark", 42 to "Luke", 43 to "John", 44 to "Acts", 45 to "Romans",
        46 to "1 Corinthians", 47 to "2 Corinthians", 48 to "Galatians", 49 to "Ephesians",
        50 to "Philippians", 51 to "Colossians", 52 to "1 Thessalonians",
        53 to "2 Thessalonians", 54 to "1 Timothy", 55 to "2 Timothy", 56 to "Titus",
        57 to "Philemon", 58 to "Hebrews", 59 to "James", 60 to "1 Peter",
        61 to "2 Peter", 62 to "1 John", 63 to "2 John", 64 to "3 John",
        65 to "Jude", 66 to "Revelation"
    )
    return names[bookId] ?: "Unknown Book ($bookId)"
}

private fun getHighlightColor(colorId: Int): Color {
    val palette = listOf(
        0xFFFFEB3B, 0xFF4CAF50, 0xFF2196F3, 0xFF9C27B0, 0xFFFF5722,
        0xFF00BCD4, 0xFF8BC34A, 0xFFFF9800, 0xFFE91E63, 0xFF607D8B,
        0xFF795548, 0xFFFFC107, 0xFF009688, 0xFFFF5252, 0xFFAAAAAA,
        0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF
    )
    return Color(palette[colorId % palette.size].toULong())
}

private fun formatTimestamp(timestamp: Long): String {
    val javaDate = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMM d, yyyy HH:mm")
    return format.format(javaDate)
}
