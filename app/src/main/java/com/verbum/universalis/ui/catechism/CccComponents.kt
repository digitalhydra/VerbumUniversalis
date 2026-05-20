package com.verbum.universalis.ui.catechism

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
fun CccTopBar(onBack: () -> Unit) {
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
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Bookmarks")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFFDF7E7) // Parchment color
        )
    )
}

@Composable
fun ReadToggleButton(isRead: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isRead) {
            OutlinedButton(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(8.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2E5E8A))),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E5E8A))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("MARK AS READ", fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D8B5D))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("READ", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun CccReferencePanel(footnotes: List<Footnote>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Divider(color = Color.Black.copy(alpha = 0.1f))
        Spacer(Modifier.height(16.dp))
        Text(
            "REFERENCES AND FOOTNOTES",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        )
        Spacer(Modifier.height(8.dp))
        footnotes.forEach { footnote ->
            Text(
                text = "${footnote.id}. ${footnote.text}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}
