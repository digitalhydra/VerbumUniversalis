package com.verbum.universalis.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verbum.universalis.data.entities.BookEntity

@Composable
fun BookListItem(
    book: BookEntity,
    onClick: () -> Unit
) {
    // Simple text item for the list
    Text(
        text = "${book.name_en} (${book.testament})",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp)
    )
}
