package com.verbum.universalis.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListDetailPaneScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.verbum.unbum.universalis.ui.reader.InterlinearWordBlock

@Composable
fun InterlinearReaderScreen(
    viewModel: InterlinearViewModel = hiltViewModel(),
    studyInspectorViewModel: StudyInspectorViewModel = hiltViewModel(),
    verseId: Int? = null,
    onBack: () -> Unit = {},
    onReferenceClick: (String) -> Unit = {}
) {
    val words by viewModel.words.collectAsState(initial = emptyList())
    val selectedWord by viewModel.selectedWord.collectAsState(initial = null)
    val showMorphology by viewModel.showMorphology.collectAsState(initial = true)
    val lexiconEntry by viewModel.lexiconEntry.collectAsState(initial = null)
    val activeTab by studyInspectorViewModel.activeTab.collectAsState(initial = InspectorTab.LEXICON)
    val activeTradition by studyInspectorViewModel.activeTradition.collectAsState(initial = ChurchTradition.ALL)
    val notes by viewModel.notes.collectAsState(initial = emptyList())
    val catenaEntries by viewModel.catenaEntries.collectAsState(initial = emptyList())
    val references by viewModel.references.collectAsState(initial = emptyList())

    val filteredCatena = remember(catenaEntries, activeTradition) {
        studyInspectorViewModel.getFilteredCatena(catenaEntries)
    }

    LaunchedEffect(verseId) { viewModel.setVerse(verseId) }

    ListDetailPaneScaffold(
        listPane = {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    words.forEach { word ->
                        InterlinearWordBlock(
                            word = word,
                            isSelected = word == selectedWord,
                            isHighlighted = viewModel.isWordHighlighted(word),
                            highlightColor = HighlightPalette.all[0],
                            showMorphology = showMorphology,
                            onClick = { viewModel.selectWord(word) }
                        )
                    }
                }
            }
        },
        detailPane = {
            StudyInspector(
                selectedWord = selectedWord,
                lexiconEntry = lexiconEntry,
                notes = notes,
                catenaEntries = filteredCatena,
                references = references,
                activeTab = activeTab,
                onTabSelect = { studyInspectorViewModel.setActiveTab(it) },
                onReferenceClick = { ref -> onReferenceClick(ref) },
                activeTradition = activeTradition,
                onTraditionSelect = { studyInspectorViewModel.setActiveTradition(it) }
            )
        }
    )
}