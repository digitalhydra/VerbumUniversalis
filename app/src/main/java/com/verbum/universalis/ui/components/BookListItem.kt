package com.verbum.universalis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.verbum.universalis.R
import com.verbum.universalis.data.entities.BookEntity

@Composable
fun BookListItem(
    book: BookEntity,
    appLanguage: String,
    onClick: () -> Unit
) {
    val bookName = if (appLanguage == "es") book.name_es else book.name_en
    val testament = if (book.id <= 46) stringResource(R.string.old_testament) else stringResource(R.string.new_testament)
    
    // Simple text item for the list
    Text(
        text = "$bookName ($testament)",
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    )
}
