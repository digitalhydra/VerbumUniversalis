package com.verbun.universalis.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.verbun.universalis.data.repository.BibleRepository
import com.verbun.universalis.data.entities.BookEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@HiltViewModel
class BookPickerViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {
    val allBooks: Flow<List<BookEntity>> = repository.getAllBooks() // Need to add this to rep
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: Flow<String> = _searchQuery

    val filteredBooks = combine(allBooks, _searchQuery) { books, query ->
        if (query.isBlank()) books else books.filter {
            it.name_en.contains(query, ignoreCase = true) ||
            it.name_es.contains(query, ignoreCase = true)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}

@Composable
fun BookPickerScreen(
    viewModel: BookPickerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onBookClick: (BookEntity) -> Unit
) {
    val books by viewModel.filteredBooks.collectAsState(initial = emptyList())
    val query by viewModel.searchQuery.collectAsState(initial = "")

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(query = query, onQueryChange = { viewModel.onSearchQueryChange(it) })
        LazyColumn {
            items(books) { book ->
                BookListItem(book = book, onClick = { onBookClick(book) })
            }
        }
    }
}
