package com.verbum.universalis.ui.catechism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verbum.universalis.core.theme.SourceSerifPro
import com.verbum.universalis.core.theme.VerbumTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatechismScreen(
    paragraphNumber: Int,
    onBack: () -> Unit = {},
    onNavigateToBibleRef: (Int, Int, Int?) -> Unit = { _, _, _ -> },
    viewModel: CatechismViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(paragraphNumber) {
        viewModel.selectParagraph(paragraphNumber)
    }

    VerbumTheme {
        Scaffold(
            topBar = { CccTopBar(onBack) },
            bottomBar = {
                BottomAppBar(
                    containerColor = Color(0xFFFDF7E7),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { viewModel.navigatePrev() }) {
                            Text("← Prev")
                        }
                        Text(
                            "¶ ${uiState?.number ?: ""}/2865",
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        TextButton(onClick = { viewModel.navigateNext() }) {
                            Text("Next →")
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFFDF7E7)) // Parchment background
            ) {
                uiState?.let { state ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        ReadToggleButton(isRead = state.isRead, onToggle = { viewModel.toggleRead() })

                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Text(
                                text = "№ ${state.number}.",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = SourceSerifPro,
                                    fontSize = 24.sp
                                )
                            )
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = SourceSerifPro,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            val annotatedBody = buildAnnotatedString {
                                state.elements.forEach { element ->
                                    when (element) {
                                        is CccElement.Text -> {
                                            withStyle(
                                                style = SpanStyle(
                                                    fontWeight = if (element.b) FontWeight.Bold else FontWeight.Normal,
                                                    fontStyle = if (element.i) FontStyle.Italic else FontStyle.Normal
                                                )
                                            ) {
                                                append(element.text)
                                            }
                                        }
                                        is CccElement.BibleRef -> {
                                            pushStringAnnotation(tag = "BIBLE_REF", annotation = "${element.bookId}:${element.chapter}:${element.verseStart}")
                                            withStyle(
                                                style = SpanStyle(
                                                    color = Color(0xFF2E5E8A),
                                                    textDecoration = TextDecoration.Underline
                                                )
                                            ) {
                                                append(element.refText)
                                            }
                                            pop()
                                        }
                                        is CccElement.CccRef -> {
                                            // Handle CCC cross-refs if needed
                                        }
                                    }
                                }
                            }

                            ClickableText(
                                text = annotatedBody,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = SourceSerifPro,
                                    lineHeight = 28.sp,
                                    fontSize = 18.sp
                                ),
                                onClick = { offset ->
                                    annotatedBody.getStringAnnotations(tag = "BIBLE_REF", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            val parts = annotation.item.split(":")
                                            if (parts.size >= 3) {
                                                val bookId = parts[0].toIntOrNull()
                                                val chapter = parts[1].toIntOrNull()
                                                val verse = parts[2].toIntOrNull()
                                                if (bookId != null && chapter != null) {
                                                    onNavigateToBibleRef(bookId, chapter, verse)
                                                }
                                            }
                                        }
                                }
                            )
                        }

                        Spacer(Modifier.height(32.dp))
                        CccReferencePanel(
                            footnotes = state.footnotes,
                            onNavigateToBibleRef = onNavigateToBibleRef
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun CatechismScreenPreview() {
    CatechismScreen(
        paragraphNumber = 27,
        onNavigateToBibleRef = { _, _, _ -> }
    )
}
