package com.verbum.universalis.ui.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // In a real app, this would come from a NotesRepository via ViewModel
    // For now, we'll use a simple in-memory list for demonstration
    val notes by remember { 
        mutableStateOf(
            listOf(
                Note(
                    id = 1,
                    bookId = 1, // Genesis
                    chapter = 1,
                    verse = 1,
                    content = "In the beginning God created the heavens and the earth.",
                    highlightColor = 0xFFFFEB3B.toLong() // Yellow
                ),
                Note(
                    id = 2,
                    bookId = 40, // Matthew
                    chapter = 5,
                    verse = 14,
                    content = "You are the light of the world. A town built on a hill cannot be hidden.",
                    highlightColor = 0xFF4CAF50.toLong() // Green
                ),
                Note(
                    id = 3,
                    bookId = 58, // Hebrews
                    chapter = 4,
                    verse = 12,
                    content = "For the word of God is alive and active. Sharper than any double-edged sword, it penetrates even to dividing soul and spirit, joints and marrow; it judges the thoughts and attitudes of the heart.",
                    highlightColor = 0xFF2196F3.toLong() // Blue
                )
            )
        )
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
                    items(notes) { note ->
                        NoteItem(
                            note = note,
                            onNoteClick = { /* TODO: Handle note click to navigate to verse */ }
                        )
                    }
                }
            }
        }
    }
}

data class Note(
    val id: Long,
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val content: String,
    val highlightColor: Long // ARGB color as Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteItem(
    note: Note,
    onNoteClick: (Note) -> Unit = {}
) {
    val bookName = getBookName(note.bookId)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onNoteClick(note) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Book Chapter:Verse
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$bookName ${note.chapter}:${note.verse}",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Highlight color indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color = note.highlightColor.toInt())
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Note content
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun getBookName(bookId: Int): String {
    // This is a simplified mapping - in a real app, this would come from a constants file or database
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