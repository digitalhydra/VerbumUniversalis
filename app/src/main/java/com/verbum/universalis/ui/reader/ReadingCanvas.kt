package com.verbum.universalis.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import com.verbum.universalis.core.theme.SourceSerifPro
import com.verbum.universalis.core.theme.VerbumTheme
import com.verbum.universalis.data.entities.InterlinearWordEntity
import com.verbum.universalis.ui.components.SelectionHoverMenu
import com.verbum.universalis.ui.theme.HighlightPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingCanvas(
    modifier: Modifier = Modifier,
    viewModel: ReadingViewModel = hiltViewModel(),
    onAction: (String, Int) -> Unit, // action string, verseId
    showStudyInspector: Boolean = false, // for tablet
    onWordClick: (InterlinearWordEntity) -> Unit = {} // word selection
) {
    VerbumTheme {
        val verses by viewModel.verses.collectAsState(initial = emptyList())
        val activeLanguage by viewModel.activeLanguage.collectAsState(initial = "en_DRB")
        val interlinearWords by viewModel.interlinearWords.collectAsState(initial = emptyList())
        val isSelectionMode by viewModel.isSelectionMode.collectAsState(initial = false)
        val selectedVerseId by viewModel.selectedVerseId.collectAsState(initial = null)
        val currentPassage by viewModel.currentPassage.collectAsState()
        val savedHighlights by viewModel.highlights.collectAsState()

        var offsetX by remember { mutableStateOf(0f) }
        val listState = rememberLazyListState()
        val scrollState = rememberScrollState()
        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        val verseOffsets = remember(verses, activeLanguage) { mutableMapOf<Int, Int>() }
        val verseRangeOffsets = remember(verses, activeLanguage) { mutableMapOf<Int, IntRange>() }
        val clipboardManager = LocalClipboardManager.current

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
            verseRangeOffsets.clear()
            verses.forEach { verseWithTexts ->
                val verseNum = verseWithTexts.verse.verse_number
                val text = viewModel.getDisplayText(verseWithTexts, activeLanguage)
                val highlights = savedHighlights.filter { it.verseId == verseWithTexts.verse.id }.map { it.colorId }
                
                val start = this.length
                verseOffsets[verseNum] = start

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
                
                val isSelected = selectedVerseId == verseWithTexts.verse.id && isSelectionMode
                val spanStyle = if (isSelected) {
                    SpanStyle(background = Color(0xFFD0E4FF))
                } else if (highlights.isNotEmpty()) {
                    val colorId = highlights.first()
                    SpanStyle(background = HighlightPalette.all[colorId % HighlightPalette.all.size].copy(alpha = 0.3f))
                } else {
                    SpanStyle()
                }

                withStyle(style = spanStyle) {
                    append(" $text")
                }
                pop()
                append(" ")
                verseRangeOffsets[verseWithTexts.verse.id] = start until this.length
            }
        }

        // Handle scrolling to a specific verse
        LaunchedEffect(currentPassage.verseFilter, layoutResult, listState) {
            val filter = currentPassage.verseFilter
            val targetVerse = if (filter != null) {
                // Scroll to first verse of first range/part
                filter.split(",")[0].trim().split("-")[0].filter { it.isDigit() }.toIntOrNull()
            } else null

            if (targetVerse != null) {
                if (activeLanguage == "il_IL") {
                    val index = verses.indexOfFirst { it.verse.verse_number == targetVerse }
                    if (index >= 0) {
                        listState.animateScrollToItem(index)
                    }
                } else {
                    layoutResult?.let { lr ->
                        verseOffsets[targetVerse]?.let { charOffset ->
                            val y = lr.getCursorRect(charOffset).top
                            scrollState.animateScrollTo(y.toInt())
                        }
                    }
                }
            }
        }

        if (activeLanguage == "il_IL") {
            val isHebrew = currentPassage.bookId <= 46
            val layoutDirection = if (isHebrew) LayoutDirection.Rtl else LayoutDirection.Ltr

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .then(swipeModifier),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Group words by verse
                val wordsByVerse = interlinearWords.groupBy { it.verse_id }
                
                items(verses) { verseWithTexts ->
                    val verseWords = wordsByVerse[verseWithTexts.verse.id] ?: emptyList()
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Verse Number
                        Text(
                            text = verseWithTexts.verse.verse_number.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                verseWords.forEach { word ->
                                    val wordHighlights = savedHighlights.filter { it.verseId == verseWithTexts.verse.id }.map { it.colorId }
                                    InterlinearWordBlock(
                                        word = word,
                                        isSelected = false,
                                        isHighlighted = wordHighlights.isNotEmpty(),
                                        highlightColor = if (wordHighlights.isNotEmpty()) HighlightPalette.all[wordHighlights.first() % HighlightPalette.all.size] else Color.Transparent,
                                        showMorphology = false,  // Hidden by default; grammar for later
                                        onClick = { onWordClick(word) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
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
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box {
                    val selectionColor = Color(0xFF4A90E2)
                    
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            lineHeight = 30.sp
                        ),
                        onTextLayout = { layoutResult = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                if (isSelectionMode && selectedVerseId != null) {
                                    val range = verseRangeOffsets[selectedVerseId!!]
                                    val lr = layoutResult
                                    if (range != null && lr != null) {
                                        // Start handle
                                        val startRect = lr.getCursorRect(range.first)
                                        drawCircle(
                                            color = selectionColor,
                                            radius = 6.dp.toPx(),
                                            center = Offset(startRect.left, startRect.top)
                                        )
                                        drawLine(
                                            color = selectionColor,
                                            start = Offset(startRect.left, startRect.top),
                                            end = Offset(startRect.left, startRect.bottom),
                                            strokeWidth = 2.dp.toPx()
                                        )

                                        // End handle
                                        val endRect = lr.getCursorRect(range.last)
                                        drawCircle(
                                            color = selectionColor,
                                            radius = 6.dp.toPx(),
                                            center = Offset(endRect.right, endRect.bottom)
                                        )
                                        drawLine(
                                            color = selectionColor,
                                            start = Offset(endRect.right, endRect.top),
                                            end = Offset(endRect.right, endRect.bottom),
                                            strokeWidth = 2.dp.toPx()
                                        )
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { offsetPosition ->
                                        layoutResult?.let { lr ->
                                            val offset = lr.getOffsetForPosition(offsetPosition)
                                            annotatedString.getStringAnnotations(tag = "VERSE", start = offset, end = offset)
                                                .firstOrNull()?.let { annotation ->
                                                    val verseId = annotation.item.toInt()
                                                    if (selectedVerseId == verseId && isSelectionMode) {
                                                        viewModel.clearSelection()
                                                    } else {
                                                        viewModel.selectVerse(verseId)
                                                    }
                                                } ?: viewModel.clearSelection()
                                        }
                                    },
                                    onLongPress = { offsetPosition ->
                                        layoutResult?.let { lr ->
                                            val offset = lr.getOffsetForPosition(offsetPosition)
                                            annotatedString.getStringAnnotations(tag = "VERSE", start = offset, end = offset)
                                                .firstOrNull()?.let { annotation ->
                                                    viewModel.selectVerse(annotation.item.toInt())
                                                }
                                        }
                                    }
                                )
                            }
                    )

                    if (isSelectionMode && selectedVerseId != null) {
                        val range = verseRangeOffsets[selectedVerseId!!]
                        val lr = layoutResult
                        if (range != null && lr != null) {
                            val startRect = lr.getCursorRect(range.first)
                            val density = LocalDensity.current
                            val popupY = with(density) { (startRect.top - 60.dp.toPx()).toInt() }

                            Popup(
                                offset = IntOffset(
                                    x = startRect.left.toInt(),
                                    y = popupY
                                ),
                                onDismissRequest = { viewModel.clearSelection() }
                            ) {
                                SelectionHoverMenu(
                                    onAction = { action ->
                                        when (action) {
                                            "copy" -> {
                                                val textToCopy = annotatedString.substring(range.first, range.last)
                                                clipboardManager.setText(AnnotatedString(textToCopy))
                                                viewModel.clearSelection()
                                            }
                                            "note", "reference", "catena" -> {
                                                onAction(action, selectedVerseId!!)
                                                viewModel.clearSelection()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
