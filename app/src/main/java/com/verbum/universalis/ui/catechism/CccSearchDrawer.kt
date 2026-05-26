package com.verbum.universalis.ui.catechism

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verbum.universalis.R
import com.verbum.universalis.data.db.CccSearchResultEntity

@Composable
fun CccSearchDrawer(
    isVisible: Boolean,
    query: String,
    results: List<CccSearchResultEntity>,
    onQueryChange: (String) -> Unit,
    onResultClick: (Int) -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            color = Color(0xFFFDF7E7), // Parchment
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Top Bar with Close Icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Search",
                            tint = Color.Black.copy(alpha = 0.6f)
                        )
                    }
                }

                // Full-width Search Input with 7px margins
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 7.dp),
                    placeholder = { Text(stringResource(R.string.search_catechism)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.5f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.3f),
                        focusedIndicatorColor = Color(0xFF2E5E8A),
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                if (query.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.found_results, results.size),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                        color = Color.Black.copy(alpha = 0.4f)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Search Results
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (results.isEmpty() && query.length >= 3) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.no_results, query),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    
                    items(results) { result ->
                        CccSearchResultItem(result, onResultClick)
                    }
                }
            }
        }
    }
}

@Composable
fun CccSearchResultItem(
    result: CccSearchResultEntity,
    onClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(result.number) },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¶ ${result.number}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF2E5E8A)
                )
            }
            Text(
                text = result.tocPath,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 6.dp)
            )
            Text(
                text = parseHtmlSnippet(result.snippet),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                maxLines = 4
            )
        }
    }
}

fun parseHtmlSnippet(snippet: String): AnnotatedString {
    return buildAnnotatedString {
        val parts = snippet.split("<b>", "</b>")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF2E5E8A))) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}
