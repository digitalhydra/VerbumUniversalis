package com.verbum.universalis.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verbum.universalis.data.entities.CatenaCommentaryEntity
import com.verbum.universalis.ui.components.BiblicalCommentaryCard
import com.verbum.universalis.ui.components.CommentaryData
import com.verbum.universalis.data.entities.InterlinearWordEntity
import com.verbum.universalis.data.entities.LexiconEntity
import com.verbum.universalis.data.repository.BibleRepository.Reference


@Composable
fun StudyInspector(
    selectedWord: InterlinearWordEntity?,
    lexiconEntry: LexiconEntity?,
    catenaEntries: List<CatenaCommentaryEntity> = emptyList(),
    references: List<Reference> = emptyList(),
    activeTab: InspectorTab,
    onTabSelect: (InspectorTab) -> Unit,
    onReferenceClick: (String) -> Unit = {},
    showLexicon: Boolean = true, // Only show Lexicon when Greek
    isLoadingCatena: Boolean = false,
    isLoadingRefs: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Filter tabs based on language mode
    val availableTabs = if (showLexicon) {
        listOf(InspectorTab.LEXICON, InspectorTab.CATENA, InspectorTab.REFERENCES)
    } else {
        listOf(InspectorTab.CATENA, InspectorTab.REFERENCES)
    }
    
    // If current tab is not available, switch to first available
    androidx.compose.runtime.LaunchedEffect(activeTab, showLexicon) {
        if (!availableTabs.contains(activeTab)) {
            onTabSelect(availableTabs.first())
        }
    }
    Column(modifier = modifier.fillMaxSize().padding(top = 56.dp)) {
        TabRow(selectedTabIndex = availableTabs.indexOf(activeTab).coerceAtLeast(0)) {
            availableTabs.forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { onTabSelect(tab) },
                    text = { Text(when (tab) {
                        InspectorTab.LEXICON -> "Lexicon"
                        InspectorTab.CATENA -> "Catena"
                        InspectorTab.REFERENCES -> "Refs"
                        else -> tab.name
                    }) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(3.dp)) {
            when (activeTab) {
                InspectorTab.LEXICON -> LexiconView(selectedWord, lexiconEntry)
                InspectorTab.CATENA -> CatenaView(catenaEntries, isLoadingCatena)
                InspectorTab.REFERENCES -> ReferencesView(references, onReferenceClick, isLoadingRefs)
                else -> { /* Handle other tabs or do nothing */ }
            }
        }
    }
}

@Composable
fun LexiconView(selectedWord: InterlinearWordEntity?, lexiconEntry: LexiconEntity?, showLexicon: Boolean = true) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (!showLexicon) {
            Text(
                "Select Greek language to view lexicon.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (selectedWord == null) {
            Text("Select a word to see its definition.")
        } else {
            Text(
                "Original: ${selectedWord.original}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Transliteration: ${selectedWord.transliteration ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Literal: ${selectedWord.literal ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (lexiconEntry != null) {
                Text(
                    "Definition: ${lexiconEntry.definition}",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text("No lexicon entry for lemma: ${selectedWord.lemma}")
            }
        }
    }
}

@Composable
fun CatenaView(
    catenaEntries: List<CatenaCommentaryEntity>,
    isLoading: Boolean = false
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (catenaEntries.isEmpty()) {
        Text(
            "No Catena entries for this verse.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(3.dp)
        ) {
            items(catenaEntries.size) { idx ->
                val entry = catenaEntries[idx]
                
                // Build verse reference string (e.g., "Genesis 1:1" or "Genesis 1:1-3")
                val verseReference = if (entry.verseStart == entry.verseEnd) {
                    "${entry.book.replaceFirstChar { it.uppercase() }} ${entry.chapter}:${entry.verseStart}"
                } else {
                    "${entry.book.replaceFirstChar { it.uppercase() }} ${entry.chapter}:${entry.verseStart}-${entry.verseEnd}"
                }
                
                // Build author text (just name, period is shown separately)
                val authorText = entry.author
                
                // Get period from entry (like "1894-99" or "IV century")
                val periodText = entry.period ?: ""
                
                BiblicalCommentaryCard(
                    commentary = CommentaryData(
                        author = authorText,
                        period = periodText,
                        content = entry.content,
                        verseReference = verseReference,
                        authorAvatarUrl = null
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ReferencesView(
    references: List<Reference>,
    onReferenceClick: (String) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (references.isEmpty()) {
        Text(
            "No references for this verse.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.padding(16.dp)
        )
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(references.size) { idx ->
                val ref = references[idx]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onReferenceClick(ref.ref) }
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    Text(
                        "→ ${ref.ref}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (ref.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            ref.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}