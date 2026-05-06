package com.verbum.universalis.ui.reader.*

import androidx.compose.foundation.layout.Box*
import androidx.compose.foundation.layout.Column*
import androidx.compose.foundation.layout.fillMaxSize*
import androidx.compose.foundation.layout.padding*
import androidx.compose.material3.Tab*
import androidx.compose.material3.TabRow*
import androidx.compose.material3.Text*
import androidx.compose.runtime.Composable*
import androidx.compose.runtime.collectAsState*
import androidx.compose.runtime.getValue*
import androidx.compose.runtime.mutableStateOf*
import androidx.compose.runtime.remember*
import androidx.compose.runtime.setValue*
import androidx.compose.ui.Modifier*
import androidx.compose.ui.unit.dp*
import com.verbum.universalis.data.entities.CatenaCommentaryEntity*
import com.verbum.universalis.data.entities.InterlinearWordEntity*
import com.verbum.universalis.data.entities.LexiconEntity*

enum class InspectorTab { LEXICON, CATENA, REFERENCES, MY_NOTES }

// Church traditions for filtering Catena commentaries
enum class ChurchTradition(val displayName: String) {
    ALL("All Traditions"),
    CATHOLIC("Catholic"),
    ORTHODOX("Orthodox"),
    PROTESTANT("Protestant")
}

@Composable*
fun StudyInspector(
    selectedWord: InterlinearWordEntity?,
    lexiconEntry: LexiconEntity?,
    notes: List<com.verbum.universalis.data.json.Note> = emptyList(),
    catenaEntries: List<CatenaCommentaryEntity> = emptyList(),
    references: List<com.verbum.universalis.data.repository.BibleRepository.Reference> = emptyList(),
    activeTab: InspectorTab,
    onTabSelect: (InspectorTab) -> Unit,
    onReferenceClick: (String) -> Unit = {},
    activeTradition: ChurchTradition = ChurchTradition.ALL,
    onTraditionSelect: (ChurchTradition) -> Unit = {},
    modifier: Modifier = Modifier*
) {
    // Filter catena entries by tradition
    val filteredCatena = remember(catenaEntries, activeTradition) {
        if (activeTradition == ChurchTradition.ALL) catenaEntries
        else catenaEntries.filter { entry -> 
            entry.period?.contains(activeTradition.displayName, ignoreCase = true) == true
        }
    }
        // Add tradition dropdown filter for Catena tab
        var showTraditionMenu by remember { mutableStateOf(false) }
        
        Box {
            Text(
                text = if (activeTab == InspectorTab.CATENA) "T: ${activeTradition.displayName}" else "",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clickable { if (activeTab == InspectorTab.CATENA) showTraditionMenu = true }
                    .padding(start = 8.dp)
            )
            DropdownMenu(
                expanded = showTraditionMenu,
                onDismissRequest = { showTraditionMenu = false }
            ) {
                ChurchTradition.entries.forEach { tradition ->
                    DropdownMenuItem(
                        text = { Text(tradition.displayName) },
                        onClick = {
                            onTraditionSelect(tradition)
                            showTraditionMenu = false
                        }
                    )
                }
            }
        }
    }
            Tab(selected = activeTab == InspectorTab.LEXICON, onClick = { onTabSelect(InspectorTab.LEXICON) }, text = { Text("Lexicon") })
            Tab(selected = activeTab == InspectorTab.CATENA, onClick = { onTabSelect(InspectorTab.CATENA) }, text = { Text("Catena") })
            Tab(selected = activeTab == InspectorTab.REFERENCES, onClick = { onTabSelect(InspectorTab.REFERENCES) }, text = { Text("References") })
            Tab(selected = activeTab == InspectorTab.MY_NOTES, onClick = { onTabSelect(InspectorTab.MY_NOTES) }, text = { Text("My Notes") })
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (activeTab) {
                InspectorTab.LEXICON -> LexiconView(selectedWord, lexiconEntry)
                InspectorTab.CATENA -> CatenaView(catenaEntries)
                InspectorTab.REFERENCES -> ReferencesView(references, onReferenceClick = { onReferenceClick(it) })
                InspectorTab.MY_NOTES -> NotesView(notes)
            }
        }
    }
}

@Composable*
fun LexiconView(selectedWord: InterlinearWordEntity?, lexiconEntry: LexiconEntity?) {
    if (selectedWord == null) {
        Text("Select a word to see its definition.")
    } else {
        Column {
            Text("Original: ${selectedWord.original}", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text("Transliteration: ${selectedWord.transliteration ?: "N/A"}", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            Text("Literal: ${selectedWord.literal ?: "N/A"}", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            if (lexiconEntry != null) {
                Text("Definition: ${lexiconEntry.definition}", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            } else {
                Text("No lexicon entry found for lemma: ${selectedWord.lemma}")
            }
        }
    }
}

@Composable*
fun CatenaView(catenaEntries: List<CatenaCommentaryEntity>) {
    if (catenaEntries.isEmpty()) {
        Text("No Catena entries for this verse.", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
    } else {
        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(catenaEntries.size) { idx ->
                val entry = catenaEntries[idx]
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // Author + Period
                    val authorText = buildString {
                        append(entry.author)
                        if (entry.period?.isNotEmpty() == true) {
                            append(" (${entry.period})")
                        }
                    }
                    Text("• $authorText", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)

                    // Source title
                    if (entry.sourceTitle?.isNotEmpty() == true) {
                        Text(entry.sourceTitle, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    }

                    // Full text inline
                    Text(entry.content, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable*
fun ReferencesView(
    references: List<com.verbum.universalis.data.repository.BibleRepository.Reference>,
    onReferenceClick: (String) -> Unit,
    modifier: Modifier = Modifier*
) {
    if (references.isEmpty()) {
        Text("No references for this verse.", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, modifier = modifier.padding(16.dp))
    } else {
        androidx.compose.foundation.lazy.LazyColumn(modifier = modifier.fillMaxSize()) {
            items(references) { ref ->
                Column(
                    modifier = Modifier*
                        .fillMaxSize()
                        .clickable { onReferenceClick(ref.ref) }
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = "→ ${ref.ref}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                    if (ref.description.isNotEmpty()) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ref.description,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable*
fun NotesView(notes: List<com.verbum.universalis.data.json.Note>) {
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