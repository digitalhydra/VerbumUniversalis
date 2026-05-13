package com.verbum.universalis.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verbum.universalis.data.entities.InterlinearWordEntity

/**
 * 3-layer interlinear word block:
 *   Top:    Pronunciation (transliteration) — italic, gray, 11sp
 *   Middle: Original Greek/Hebrew word — bold serif, 18sp, primary color
 *   Bottom: English gloss (literal translation) — normal, green-ish, 12sp
 */
@Composable
fun InterlinearWordBlock(
    word: InterlinearWordEntity,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    highlightColor: Color = Color.Transparent,
    showMorphology: Boolean = false, // Hidden by default; toggle for future
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else if (isHighlighted) {
        highlightColor.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Pronunciation / Transliteration (Top)
        val translitText = word.transliteration?.takeIf { it.isNotBlank() } ?: word.original
        Text(
            text = translitText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
                color = Color.Gray
            ),
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        // 2. Original Word (Middle — largest)
        Text(
            text = word.original,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        // 3. English Gloss / Literal Translation (Bottom)
        val glossText = word.literal?.takeIf { it.isNotBlank() } ?: word.original
        Text(
            text = glossText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = Color(0xFF2E7D32) // Dark Green
            ),
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        // Optional: Morphology badge (hidden by default)
        if (showMorphology && !word.morphology.isNullOrBlank()) {
            val morphText = word.morphology.substringAfter(":")
            Text(
                text = morphText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = Color.DarkGray
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
