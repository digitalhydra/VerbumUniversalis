package com.verbum.universalis.ui.catechism

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CccTopBar(
    isRead: Boolean,
    isBookmarked: Boolean,
    onToggleRead: () -> Unit,
    onToggleBookmark: () -> Unit,
    onSearchClick: () -> Unit,
    onBack: () -> Unit
) {
    TopAppBar(
        title = { Text("CATECHISM", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onToggleRead) {
                Icon(
                    imageVector = if (isRead) Icons.Default.CheckCircle else Icons.Default.Check,
                    contentDescription = if (isRead) "Mark as unread" else "Mark as read",
                    tint = if (isRead) Color(0xFF5D8B5D) else Color.Unspecified
                )
            }
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                    tint = if (isBookmarked) Color(0xFFD4AF37) else Color.Unspecified
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFFDF7E7) // Parchment color
        )
    )
}

@Composable
fun CccReferencePanel(
    footnotes: List<Footnote>,
    onNavigateToBibleRef: (Int, Int, Int?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
        Spacer(Modifier.height(16.dp))
        Text(
            "REFERENCES AND FOOTNOTES",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        )
        Spacer(Modifier.height(8.dp))
        footnotes.forEach { footnote ->
            val annotatedFootnote = buildAnnotatedString {
                append("${footnote.id}. ")
                val startOffset = length
                append(footnote.text)
                
                footnote.bibleRefs.forEach { ref ->
                    val totalStart = startOffset + ref.position
                    val totalEnd = totalStart + ref.length
                    
                    // Safety check for bounds
                    if (totalStart < length && totalEnd <= length) {
                        addStyle(
                            style = SpanStyle(
                                color = Color(0xFF2E5E8A),
                                textDecoration = TextDecoration.Underline
                            ),
                            start = totalStart,
                            end = totalEnd
                        )
                        addStringAnnotation(
                            tag = "BIBLE_REF",
                            annotation = "${ref.bookId}:${ref.chapter}:${ref.verse ?: ""}",
                            start = totalStart,
                            end = totalEnd
                        )
                    }
                }
            }

            ClickableText(
                text = annotatedFootnote,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = { offset ->
                    annotatedFootnote.getStringAnnotations(tag = "BIBLE_REF", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            val parts = annotation.item.split(":")
                            if (parts.size >= 2) {
                                val bookId = parts[0].toIntOrNull()
                                val chapter = parts[1].toIntOrNull()
                                val verse = if (parts.size > 2) parts[2].toIntOrNull() else null
                                if (bookId != null && chapter != null) {
                                    onNavigateToBibleRef(bookId, chapter, verse)
                                }
                            }
                        }
                }
            )
        }
    }
}
