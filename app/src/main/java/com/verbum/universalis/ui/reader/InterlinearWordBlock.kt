package com.verbum.universalis.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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

@Composable
fun InterlinearWordBlock(
    word: InterlinearWordEntity,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    highlightColor: Color = Color.Transparent,
    showMorphology: Boolean = true,
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
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Morphology Tag (Badge)
        if (showMorphology && !word.morphology.isNullOrBlank()) {
            val morphText = word.morphology.substringAfter(":")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(getMorphologyColor(morphText))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = morphText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.DarkGray
                )
            }
        }

        // 2. Literal Translation (Greenish)
        Text(
            text = word.literal ?: "-",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = Color(0xFF2E7D32) // Dark Green
            ),
            textAlign = TextAlign.Center
        )

        // 3. Original Word (Large, Bold)
        Text(
            text = word.original,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black,
            textAlign = TextAlign.Center
        )

        // 4. Transliteration (Italic)
        if (!word.transliteration.isNullOrBlank()) {
            Text(
                text = word.transliteration,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    color = Color.Gray
                ),
                textAlign = TextAlign.Center
            )
        }

        // 5. Strong's Number (Blue)
        if (!word.lemma.isNullOrBlank()) {
            val strongs = word.lemma.substringAfter(":")
            Text(
                text = strongs.lowercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = Color(0xFF1976D2) // Strong Blue
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getMorphologyColor(morph: String): Color {
    return when {
        morph.startsWith("N") || morph.contains("/N") -> Color(0xFFE3F2FD) // Blue-ish for Nouns
        morph.startsWith("V") || morph.contains("/V") -> Color(0xFFFFEBEE) // Red-ish for Verbs
        morph.startsWith("Art") || morph.contains("/Art") || morph.startsWith("Td") -> Color(0xFFF3E5F5) // Purple-ish for Articles
        morph.startsWith("Prep") || morph.startsWith("R") -> Color(0xFFEFEBE9) // Brown-ish for Prepositions
        morph.startsWith("Conj") || morph.startsWith("C") -> Color(0xFFFFF3E0) // Orange-ish for Conjunctions
        morph.startsWith("Adj") || morph.startsWith("A") -> Color(0xFFE8F5E9) // Green-ish for Adjectives
        morph.startsWith("Pro") || morph.startsWith("P") -> Color(0xFFFFFDE7) // Yellow-ish for Pronouns
        else -> Color(0xFFF5F5F5) // Default gray
    }
}
