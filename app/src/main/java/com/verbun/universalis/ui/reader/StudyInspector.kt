package com.verbun.universalis.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verbun.universalis.data.entities.CatenaCommentaryEntity
import com.verbun.universalis.data.entities.InterlinearWordEntity
import com.verbun.universalis.data.entities.LexiconEntity

enum class InspectorTab { LEXICON, CATENA, REFERENCES, MY_NOTES }

@Composable
fun StudyInspector(
    selectedWord: InterlinearWordEntity?,
    lexiconEntry: LexiconEntity?,
    notes: List<com.verbun.universalis.data.json.Note> = emptyList(),
    catenaEntries: List<CatenaCommentaryEntity> = emptyList(),
    references: List<com.verbun.universalis.data.repository.BibleRepository.Reference> = emptyList(),
    activeTab: InspectorTab,
    onTabSelect: (InspectorTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(InspectorTab.LEXICON) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab.ordinal) {
            Tab(selected = activeTab == InspectorTab.LEXICON, onClick = { onTabSelect(InspectorTab.LEXICON) }, text = { Text("Lexicon") })
            Tab(selected = activeTab == InspectorTab.CATENA, onClick = { onTabSelect(InspectorTab.CATENA) }, text = { Text("Catena") })
            Tab(selected = activeTab == InspectorTab.REFERENCES, onClick = { onTabSelect(InspectorTab.REFERENCES) }, text = { Text("References") })
            Tab(selected = activeTab == InspectorTab.MY_NOTES, onClick = { onTabSelect(InspectorTab.MY_NOTES) }, text = { Text("My Notes") })
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (activeTab) {
                InspectorTab.LEXICON -> LexiconView(selectedWord, lexiconEntry)
                InspectorTab.CATENA -> CatenaView(catenaEntries)
                InspectorTab.REFERENCES -> ReferencesView(references)
                InspectorTab.MY_NOTES -> NotesView(notes)
            }
        }
    }
}

@Composable
fun LexiconView(selectedWord: InterlinearWordEntity?, lexiconEntry: LexiconEntity?) {
    if (selectedWord == null) {
        Text("Select a word to see its definition.")
    } else {
        Column {
            Text("Original: ${selectedWord.original}", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text("Transliteration: ${selectedWord.transliteration}", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            Text("Literal: ${selectedWord.literal}", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            if (lexiconEntry != null) {
                Text("Definition: ${lexiconEntry.definition}", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            } else {
                Text("No lexicon entry found for lemma: ${selectedWord.lemma}")
            }
        }
    }
}

@Composable
fun CatenaView(catenaEntries: List<CatenaCommentaryEntity>) {
    if (catenaEntries.isEmpty()) {
        Text("No Catena entries for this verse.", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
    } else {
        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(catenaEntries.size) { idx ->
                val entry = catenaEntries[idx]
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("• ${entry.fatherName}${entry.appendToAuthorName ?: ""}", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    if (entry.sourceTitle?.isNotEmpty() == true) {
                        Text(entry.sourceTitle, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    }
                    Text(entry.text, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun ReferencesView(references: List<com.verbun.universalis.data.repository.BibleRepository.Reference>) {
    if (references.isEmpty()) {
        Text("No references for this verse.", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
    } else {
        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(references) { ref ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("→ ${ref.ref}", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                    Text(ref.description, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun NotesView(notes: List<com.verbun.universalis.data.json.Note>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Notes", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        if (notes.isEmpty()) {
            Text("No notes yet.", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        } else {
            androidx.compose.foundation.lazy.LazyColumn {
                items(notes) { note ->
                    Text("• ${note.content}", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                    Text("  (Verse ${note.verseId})", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
