package com.verbum.universalis.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.verbum.universalis.core.theme.SourceSerifPro
import com.verbum.universalis.core.theme.VerbumTheme
import com.verbum.universalis.data.entities.InterlinearWordEntity
import com.verbum.universalis.ui.theme.HighlightPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingCanvas(
    modifier: Modifier = Modifier,
    viewModel: ReadingViewModel = hiltViewModel(),
    onAction: (String, Int) -> Unit, // action string, verseId
    showStudyInspector: Boolean = false, // for tablet
    onWordClick: (InterlinearWordEntity) -> Unit = {} // word selection for Greek
) {
    VerbumTheme {
        val verses by viewModel.verses.collectAsState(initial = emptyList())
        val activeLanguage by viewModel.activeLanguage.collectAsState(initial = "en_DRB")
        val greekWords by viewModel.greekWords.collectAsState(initial = emptyList())
        val isSelectionMode by viewModel.isSelectionMode.collectAsState(initial = false)
        val selectedVerseId by viewModel.selectedVerseId.collectAsState(initial = null)
        val currentPassage by viewModel.currentPassage.collectAsState()

        var offsetX by remember { mutableStateOf(0f) }
        val scrollState = rememberScrollState()
        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        val verseOffsets = remember(verses, activeLanguage) { mutableMapOf<Int, Int>() }

        val swipeModifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                },
                onDragEnd = {
                    if (offsetX > 150) {
                        viewModel.previousChapter()
                    } else if (offsetX < -150) {
                        viewModel.nextChapter()
                    }
                    offsetX = 0f
                },
                onDragCancel = { offsetX = 0f }
            )
        }

        // Build the continuous text
        val annotatedString = buildAnnotatedString {
            verseOffsets.clear()
            verses.forEach { verseWithTexts ->
                val verseNum = verseWithTexts.verse.verse_number
                val text = viewModel.getDisplayText(verseWithTexts, activeLanguage)
                val highlights = viewModel.getHighlightsForVerse(verseWithTexts.verse.id)
                
                verseOffsets[verseNum] = this.length

                // Verse Number (Superscript)
                withStyle(
                    style = SpanStyle(
                        fontSize = 12.sp,
                        baselineShift = BaselineShift.Superscript,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                ) {
                    append("$verseNum")
                }

                // Verse Text
                pushStringAnnotation(tag = "VERSE", annotation = verseWithTexts.verse.id.toString())
                
                val spanStyle = if (highlights.isNotEmpty()) {
                    val colorId = highlights.first()
                    SpanStyle(background = HighlightPalette.all[colorId % HighlightPalette.all.size].copy(alpha = 0.3f))
                } else if (selectedVerseId == verseWithTexts.verse.id && isSelectionMode) {
                    SpanStyle(background = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                } else {
                    SpanStyle()
                }

                withStyle(style = spanStyle) {
                    append(" $text")
                }
                pop()
                append(" ")
            }
        }

        // Handle scrolling to a specific verse
        LaunchedEffect(currentPassage.verseRange, layoutResult) {
            val targetVerse = currentPassage.verseRange?.start
            val lr = layoutResult
            if (targetVerse != null && lr != null) {
                verseOffsets[targetVerse]?.let { charOffset ->
                    val y = lr.getCursorRect(charOffset).top
                    scrollState.animateScrollTo(y.toInt())
                }
            }
        }

        if (activeLanguage == "el_GRK") {
            FlowRow(
                modifier = modifier
                    .fillMaxSize()
                    .then(swipeModifier)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
            var menuExpanded by remember { mutableStateOf(false) }
            var clickedVerseId by remember { mutableStateOf<Int?>(null) }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .then(swipeModifier)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Heading
                Text(
                    text = viewModel.getPassageReference(currentPassage),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = SourceSerifPro,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box {
                    SelectionContainer {
                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = SourceSerifPro,
                                fontSize = 18.sp,
                                lineHeight = 30.sp
                            ),
                            onTextLayout = { layoutResult = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { offsetPosition ->
                                            layoutResult?.let { lr ->
                                                val offset = lr.getOffsetForPosition(offsetPosition)
                                                annotatedString.getStringAnnotations(tag = "VERSE", start = offset, end = offset)
                                                    .firstOrNull()?.let { annotation ->
                                                        clickedVerseId = annotation.item.toInt()
                                                        menuExpanded = true
                                                    }
                                            }
                                        },
                                        onLongPress = { offsetPosition ->
                                            layoutResult?.let { lr ->
                                                val offset = lr.getOffsetForPosition(offsetPosition)
                                                annotatedString.getStringAnnotations(tag = "VERSE", start = offset, end = offset)
                                                    .firstOrNull()?.let { annotation ->
                                                        viewModel.selectVerse(annotation.item.toInt())
                                                        viewModel.toggleSelectionMode()
                                                    }
                                            }
                                        }
                                    )
                                }
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Highlight / Note") },
                            onClick = { 
                                clickedVerseId?.let { 
                                    viewModel.selectVerse(it)
                                    onAction("note", it) 
                                }
                                menuExpanded = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("References") },
                            onClick = { 
                                clickedVerseId?.let { onAction("reference", it) }
                                menuExpanded = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Catena Commentary") },
                            onClick = { 
                                clickedVerseId?.let { onAction("catena", it) }
                                menuExpanded = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Interlinear") },
                            onClick = { 
                                clickedVerseId?.let { onAction("interlinear", it) }
                                menuExpanded = false 
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
