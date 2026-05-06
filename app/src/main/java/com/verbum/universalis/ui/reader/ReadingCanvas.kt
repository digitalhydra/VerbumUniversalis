package com.verbum.universalis.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.verbum.universalis.core.theme.VerbumTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingCanvas(
    viewModel: ReadingViewModel = hiltViewModel(),
    onAction: (String, Int) -> Unit, // action string, verseId
    showStudyInspector: Boolean = false, // for tablet
    onWordClick: (InterlinearWordEntity) -> Unit = {} // word selection for Greek
) {
    VerbumTheme {
        val verses by viewModel.verses.collectAsState(initial = emptyList())
        val activeLanguage by viewModel.activeLanguage.collectAsState(initial = "en_DRB")
        val greekWords by viewModel.greekWords.collectAsState(initial = emptyList())
        val listState = rememberLazyListState()
        val isSelectionMode by viewModel.isSelectionMode.collectAsState(initial = false)
        val selectedVerseId by viewModel.selectedVerseId.collectAsState(initial = null)

        // When Greek is selected, render interlinear word blocks
        if (activeLanguage == "el_GRK") {
            FlowRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                greekWords.forEach { word ->
                    InterlinearWordBlock(
                        word = word,
                        isSelected = false,
                        isHighlighted = false,
                        showMorphology = true,
                        onClick = { onWordClick(word) }
                    )
                }
            }
        } else {
            // Normal prose rendering
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(verses) { verseWithTexts ->
                    val displayText = viewModel.getDisplayText(verseWithTexts)
                    val highlights = viewModel.getHighlightsForVerse(verseWithTexts.verse.id)
                    val isThisVerseSelected = selectedVerseId == verseWithTexts.verse.id
                    VerseItem(
                        verseWithTexts = verseWithTexts,
                        displayText = displayText,
                        isSelectedVerse = isSelectionMode && isThisVerseSelected,
                        highlights = highlights,
                        onVerseClick = { verseId -> 
                            viewModel.selectVerse(verseId)
                            viewModel.showNoteHighlightSheet(verseId)
                        },
                        onLongClick = { viewModel.toggleSelectionMode() },
                        onAction = { action -> onAction(action, verseWithTexts.verse.id) }
                    )
                }
            }
        }
    }
}