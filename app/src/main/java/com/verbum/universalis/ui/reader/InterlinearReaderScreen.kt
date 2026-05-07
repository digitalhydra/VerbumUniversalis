package com.verbum.universalis.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.runtime.Composable
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun InterlinearReaderScreen(
    viewModel: InterlinearViewModel = hiltViewModel(),
    studyInspectorViewModel: StudyInspectorViewModel = hiltViewModel(),
    verseId: Int? = null,
    initialTab: String = "", // "catena" or "references" to open directly
    onBack: () -> Unit = {},
    onReferenceClick: (String) -> Unit = {}
) {
    val words by viewModel.words.collectAsState(initial = emptyList())
    val selectedWord by viewModel.selectedWord.collectAsState(initial = null)
    val showMorphology by viewModel.showMorphology.collectAsState(initial = true)
    val lexiconEntry by viewModel.lexiconEntry.collectAsState(initial = null)
    
    // Set initial tab based on navigation parameter
    LaunchedEffect(initialTab) {
        when (initialTab) {
            "catena" -> studyInspectorViewModel.setActiveTab(InspectorTab.CATENA)
            "references" -> studyInspectorViewModel.setActiveTab(InspectorTab.REFERENCES)
            else -> studyInspectorViewModel.setActiveTab(InspectorTab.LEXICON)
        }
    }
    
    val activeTab by studyInspectorViewModel.activeTab.collectAsState(initial = InspectorTab.LEXICON)
    val catenaEntries by studyInspectorViewModel.catenaEntries.collectAsState(initial = emptyList())
    val references by studyInspectorViewModel.crossRefs.collectAsState(initial = emptyList())

    LaunchedEffect(verseId) { viewModel.setVerse(verseId) }

    // Remember navigation scaffold for adaptive layout
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Any>()
    ListDetailPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        scaffoldState = scaffoldNavigator.scaffoldState,
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
                            highlightColor = com.verbum.universalis.ui.theme.HighlightPalette.all[0],
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
                catenaEntries = catenaEntries,
                references = references,
                activeTab = activeTab,
                onTabSelect = { studyInspectorViewModel.setActiveTab(it) },
                onReferenceClick = { ref -> onReferenceClick(ref) }
            )
        }
    )
}