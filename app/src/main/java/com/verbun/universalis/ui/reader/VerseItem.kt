package com.verbun.universalis.ui.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.combinedClickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verbun.universalis.core.theme.SourceSerifProFontFamily
import com.verbun.universalis.ui.components.Hairline
import com.verbun.universalis.data.entities.VerseWithTexts

@Composable
fun VerseItem(
    verseWithTexts: VerseWithTexts,
    displayText: String,
    isSelected: Boolean = false,
    highlights: List<Int> = emptyList(), // colorIds
    onVerseClick: (Int) -> Unit,
    onLongClick: () -> Unit,
    onAction: (String) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    val highlightColor = if (highlights.isNotEmpty()) {
        val colorId = highlights.first()
        HighlightPalette.all[colorId % 20].copy(alpha = 0.3f)
    } else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .background(
                if (isSelected) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                else if (highlights.isNotEmpty()) 
                    highlightColor
                else 
                    androidx.compose.ui.graphics.Color.Transparent
            )
            .combinedClickable(
                onClick = {
                    if (!isSelected) {
                        expanded.value = true // Single Tap: Open Menu
                    } else {
                        onVerseClick(verseWithTexts.verse.id) // In selection mode, select this verse
                    }
                },
                onLongClick = onLongClick // Hold Tap: Enter Selection Mode
            )
    ) {
        Text(
            text = "${verseWithTexts.verse.verse_number}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = SourceSerifProFontFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        if (isSelected) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = SourceSerifProFontFamily,
                        lineHeight = 1.6.em // 1.6x line height
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            // Render highlights as underlined text
            val annotatedString = buildAnnotatedString {
                if (highlights.isEmpty()) {
                    append(displayText)
                } else {
                    // Simplified: just underline whole verse if highlighted
                    withStyle(style = SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                        append(displayText)
                    }
                }
            }
            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = SourceSerifProFontFamily,
                    lineHeight = 1.6.em // 1.6x line height
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Hairline() // 1dp hairline between verses

        // Action Menu (Single Tap)
        DropdownMenu(
            expanded = expanded.value && !isSelected,
            onDismissRequest = { expanded.value = false }
        ) {
            DropdownMenuItem(text = { Text("Highlight") }, onClick = { onAction("highlight"); expanded.value = false })
            DropdownMenuItem(text = { Text("Note on Verse") }, onClick = { onAction("note"); expanded.value = false })
            DropdownMenuItem(text = { Text("References") }, onClick = { onAction("reference"); expanded.value = false })
            DropdownMenuItem(text = { Text("Catena Commentary") }, onClick = { onAction("catena"); expanded.value = false })
            DropdownMenuItem(text = { Text("Interlinear Reader") }, onClick = { onAction("interlinear"); expanded.value = false })
        }
    }
}
