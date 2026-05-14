package com.verbum.universalis.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data class representing commentary data for the BiblicalCommentaryCard
 */
data class CommentaryData(
    val author: String,
    val date: String,
    val content: String,
    val verseReference: String,
    val authorAvatarUrl: String? = null
)

/**
 * A Material 3 composable card for displaying biblical commentary.
 * Features a modern, clean design with rounded corners, author info, and tags.
 */
@Composable
fun BiblicalCommentaryCard(
    commentary: CommentaryData,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = Color.White
        ),
        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // ===== HEADER =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Reference Badge
                Surface(
                    color = Color(0xFFFFF3E0), // Light orange (pastel)
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = commentary.verseReference,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100), // Dark orange
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Right: Calendar icon + date
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF9E9E9E) // Medium gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = commentary.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E) // Medium gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== TITLE / SUBTITLE =====
            Text(
                text = commentary.verseReference,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ===== CONTENT =====
            Text(
                text = commentary.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF424242), // Dark gray, not pure black
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== TAGS =====
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = "#Teología",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFE3F2FD), // Light blue
                        labelColor = Color(0xFF1565C0)
                    ),
                    border = null,
                    modifier = Modifier.height(28.dp)
                )
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = "#Exégesis",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFF3E5F5), // Light purple
                        labelColor = Color(0xFF7B1FA2)
                    ),
                    border = null,
                    modifier = Modifier.height(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== DIVIDER (DOTTED LINE) =====
            DashedDivider()

            Spacer(modifier = Modifier.height(12.dp))

            // ===== FOOTER (AUTHOR SECTION) =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Author Avatar - placeholder with initials (no avatar URLs available in current data)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = commentary.author.take(1).uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF757575)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Author Name
                Text(
                    text = commentary.author,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF424242)
                )
            }
        }
    }
}

/**
 * Custom dotted/dashed horizontal divider using Canvas
 */
@Composable
private fun DashedDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        val strokeWidth = 1.dp.toPx()
        val dashWidth = 6.dp.toPx()
        val dashGap = 4.dp.toPx()
        var startX = 0f

        while (startX < size.width) {
            drawLine(
                color = Color(0xFFE0E0E0), // Light gray
                start = Offset(startX, 0f),
                end = Offset(startX + dashWidth, 0f),
                strokeWidth = strokeWidth
            )
            startX += dashWidth + dashGap
        }
    }
}