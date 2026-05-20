package com.verbum.universalis.ui.catechism

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.verbum.universalis.core.theme.VerbumTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CccTocScreen(
    onBack: () -> Unit,
    onParagraphClick: (Int) -> Unit,
    viewModel: CatechismViewModel = hiltViewModel()
) {
    val tocItems by viewModel.tocItems.collectAsState()

    VerbumTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("CATECHISM", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFFDF7E7)
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFFDF7E7))
            ) {
                items(tocItems) { node ->
                    TocItem(node, onParagraphClick)
                }
            }
        }
    }
}

@Composable
fun TocItem(node: CccTocNode, onClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = node.paragraphNumber != null) {
                node.paragraphNumber?.let { onClick(it) }
            }
            .padding(vertical = 12.dp, horizontal = (16 + (node.indentLevel * 16)).dp)
    ) {
        Text(
            text = node.title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (node.indentLevel == 0) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (node.indentLevel == 0) 16.sp else 14.sp,
                color = if (node.paragraphNumber != null) Color(0xFF2E5E8A) else Color.Black
            )
        )
        if (node.indentLevel == 0) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                color = Color.Black.copy(alpha = 0.05f)
            )
        }
    }
}
