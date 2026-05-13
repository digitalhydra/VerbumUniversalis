package com.verbum.universalis.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val verseEntity by viewModel.verseEntity.collectAsState(initial = null)
    
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
    val isLoadingCatena by studyInspectorViewModel.isLoadingCatena.collectAsState(initial = false)
    val isLoadingRefs by studyInspectorViewModel.isLoadingRefs.collectAsState(initial = false)

    // Set the verse and load data when verse changes (data always visible in split pane)
    LaunchedEffect(verseId) {
        viewModel.setVerse(verseId)
        studyInspectorViewModel.loadCatenaAndRefs()
    }

    // Remember navigation scaffold for adaptive layout
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Any>()
    ListDetailPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        scaffoldState = scaffoldNavigator.scaffoldState,
        listPane = {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header with Verse Reference
                Text(
                    text = verseEntity?.let { "Verse ${it.verse_number}" } ?: "Loading...",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val isHebrew = verseEntity?.book_id?.let { it <= 46 } ?: false
                val layoutDirection = if (isHebrew) LayoutDirection.Rtl else LayoutDirection.Ltr

                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                onReferenceClick = { ref -> onReferenceClick(ref) },
                isLoadingCatena = isLoadingCatena,
                isLoadingRefs = isLoadingRefs
            )
        }
    )
}
