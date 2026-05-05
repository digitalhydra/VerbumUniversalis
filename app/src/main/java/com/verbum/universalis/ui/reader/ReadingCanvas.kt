package com.verbum.universalis.ui.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.verbum.universalis.core.theme.VerbumTheme

@Composable
fun ReadingCanvas(
    viewModel: ReadingViewModel = hiltViewModel(),
    onAction: (String, Int) -> Unit // action string, verseId
) {
    VerbumTheme {
        val verses by viewModel.verses.collectAsState(initial = emptyList())
        val listState = rememberLazyListState()
        val isSelectionMode by viewModel.isSelectionMode.collectAsState(initial = false)

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(verses) { verseWithTexts ->
                val displayText = viewModel.getDisplayText(verseWithTexts)
                val highlights = viewModel.getHighlightsForVerse(verseWithTexts.verse.id)
                VerseItem(
                    verseWithTexts = verseWithTexts,
                    displayText = displayText,
                    isSelected = isSelectionMode,
                    highlights = highlights,
                    onClick = { /* Handled by combinedClickable */ },
                    onLongClick = { viewModel.toggleSelectionMode() },
                    onAction = { action -> onAction(action, verseWithTexts.verse.id) }
                )
            }
        }
    }
}
