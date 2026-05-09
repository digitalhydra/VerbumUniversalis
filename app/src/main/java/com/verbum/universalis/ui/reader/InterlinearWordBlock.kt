package com.verbum.universalis.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verbum.universalis.core.theme.Inter
import com.verbum.universalis.core.theme.SourceSerifPro
import com.verbum.universalis.data.entities.InterlinearWordEntity

@Composable
fun InterlinearWordBlock(
    word: InterlinearWordEntity,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    highlightColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent,
    showMorphology: Boolean = true,
    onClick: () -> Unit
) {
    val backgroundColor = if (isHighlighted) highlightColor.copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color.Transparent
    Column(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .background(backgroundColor),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // 1. Original Word (Serif, Bold)
        Text(
            text = word.original,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        // 2. Transliteration (Sans-Serif, Italic, Muted Gray)
        if (!word.transliteration.isNullOrBlank()) {
            Text(
                text = word.transliteration,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // 3. Literal Translation (Serif)
        if (!word.literal.isNullOrBlank()) {
            Text(
                text = word.literal,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = SourceSerifPro
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 4. Morphology (Sans-Serif, Small, Muted Gray) - Controlled by global toggle
        if (showMorphology && !word.morphology.isNullOrBlank()) {
            Text(
                text = word.morphology,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
