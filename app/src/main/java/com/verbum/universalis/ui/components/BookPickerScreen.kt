package com.verbum.universalis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.universalis.core.theme.Inter
import com.verbum.universalis.core.theme.DeepCharcoal
import com.verbum.universalis.core.theme.DarkGray
import com.verbum.universalis.core.theme.VerbumBlue
import com.verbum.universalis.core.theme.White
import com.verbum.universalis.data.entities.BookEntity
import com.verbum.universalis.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PickerStep { BOOKS, CHAPTERS, VERSES }
enum class TestamentFilter { OLD, NEW }

@HiltViewModel
class BookPickerViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {
    
    private val _currentStep = MutableStateFlow(PickerStep.BOOKS)
    val currentStep: StateFlow<PickerStep> = _currentStep.asStateFlow()

    private val _testamentFilter = MutableStateFlow(TestamentFilter.OLD)
    val testamentFilter: StateFlow<TestamentFilter> = _testamentFilter.asStateFlow()

    private val _selectedBook = MutableStateFlow<BookEntity?>(null)
    val selectedBook: StateFlow<BookEntity?> = _selectedBook.asStateFlow()

    private val _selectedChapter = MutableStateFlow<Int?>(null)
    val selectedChapter: StateFlow<Int?> = _selectedChapter.asStateFlow()

    val allBooks: StateFlow<List<BookEntity>> = repository.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val filteredBooks = combine(allBooks, _testamentFilter) { books, filter ->
        books.filter { it.testament.lowercase() == filter.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _chapters = MutableStateFlow<List<Int>>(emptyList())
    val chapters: StateFlow<List<Int>> = _chapters.asStateFlow()

    private val _verses = MutableStateFlow<List<Int>>(emptyList())
    val verses: StateFlow<List<Int>> = _verses.asStateFlow()

    fun setTestament(filter: TestamentFilter) {
        _testamentFilter.value = filter
        _currentStep.value = PickerStep.BOOKS
    }

    fun selectBook(book: BookEntity) {
        _selectedBook.value = book
        _currentStep.value = PickerStep.CHAPTERS
        viewModelScope.launch {
            val maxChapter = repository.getMaxChapterForBook(book.id).first() ?: 1
            _chapters.value = (1..maxChapter).toList()
        }
    }

    fun selectChapter(chapter: Int) {
        _selectedChapter.value = chapter
        _currentStep.value = PickerStep.VERSES
        viewModelScope.launch {
            _selectedBook.value?.let { book ->
                val maxVerse = repository.getMaxVerseForChapter(book.id, chapter).first() ?: 1
                _verses.value = (1..maxVerse).toList()
            }
        }
    }

    fun setStep(step: PickerStep) {
        if (step == PickerStep.CHAPTERS && _selectedBook.value == null) return
        if (step == PickerStep.VERSES && _selectedChapter.value == null) return
        _currentStep.value = step
    }

    fun goBack() {
        when (_currentStep.value) {
            PickerStep.BOOKS -> { /* Handle close */ }
            PickerStep.CHAPTERS -> _currentStep.value = PickerStep.BOOKS
            PickerStep.VERSES -> _currentStep.value = PickerStep.CHAPTERS
        }
    }
}

@Composable
fun BookPickerScreen(
    viewModel: BookPickerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onClose: () -> Unit,
    onResult: (bookId: Int, chapter: Int, verse: Int?) -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val testamentFilter by viewModel.testamentFilter.collectAsState()
    val books by viewModel.filteredBooks.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val verses by viewModel.verses.collectAsState()
    val selectedBook by viewModel.selectedBook.collectAsState()
    val selectedChapter by viewModel.selectedChapter.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DeepCharcoal
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (currentStep == PickerStep.BOOKS) onClose() else viewModel.goBack() }) {
                    Icon(
                        imageVector = if (currentStep == PickerStep.BOOKS) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = White
                    )
                }
                Text(
                    text = "Bible Index",
                    style = MaterialTheme.typography.titleLarge,
                    color = White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Tabs (Books, Chapters, Verses)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                PickerTab(
                    text = "Books",
                    isSelected = currentStep == PickerStep.BOOKS,
                    onClick = { viewModel.setStep(PickerStep.BOOKS) }
                )
                PickerTab(
                    text = "Chapters",
                    isSelected = currentStep == PickerStep.CHAPTERS,
                    onClick = { viewModel.setStep(PickerStep.CHAPTERS) }
                )
                PickerTab(
                    text = "Verses",
                    isSelected = currentStep == PickerStep.VERSES,
                    onClick = { viewModel.setStep(PickerStep.VERSES) }
                )
            }

            Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                when (currentStep) {
                    PickerStep.BOOKS -> {
                        BookGrid(books = books, onBookClick = { viewModel.selectBook(it) })
                    }
                    PickerStep.CHAPTERS -> {
                        NumberGrid(items = chapters, onNumberClick = { viewModel.selectChapter(it) })
                    }
                    PickerStep.VERSES -> {
                        NumberGrid(items = verses, onNumberClick = { verse ->
                            selectedBook?.let { book ->
                                selectedChapter?.let { chapter ->
                                    onResult(book.id, chapter, verse)
                                }
                            }
                        })
                    }
                }
            }

            // Bottom Testament Tabs (Only show in BOOKS step)
            if (currentStep == PickerStep.BOOKS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TestamentTab(
                        text = "Old Testament",
                        isSelected = testamentFilter == TestamentFilter.OLD,
                        onClick = { viewModel.setTestament(TestamentFilter.OLD) }
                    )
                    TestamentTab(
                        text = "New Testament",
                        isSelected = testamentFilter == TestamentFilter.NEW,
                        onClick = { viewModel.setTestament(TestamentFilter.NEW) }
                    )
                }
            } else if (currentStep == PickerStep.VERSES) {
                // Option to just jump to chapter
                Button(
                    onClick = {
                        selectedBook?.let { book ->
                            selectedChapter?.let { chapter ->
                                onResult(book.id, chapter, null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerbumBlue)
                ) {
                    Text("Jump to Chapter")
                }
            }
        }
    }
}

@Composable
fun PickerTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = if (isSelected) White else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = Inter
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .height(2.dp)
                    .width(40.dp)
                    .background(White)
            )
        }
    }
}

@Composable
fun TestamentTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = if (isSelected) White else Color.Gray,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = Inter
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .height(2.dp)
                    .width(80.dp)
                    .background(White)
            )
        }
    }
}

@Composable
fun BookGrid(books: List<BookEntity>, onBookClick: (BookEntity) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(books) { book ->
            BookItem(book = book, onClick = { onBookClick(book) })
        }
    }
}

@Composable
fun BookItem(book: BookEntity, onClick: () -> Unit) {
    val abbreviation = getAbbreviation(book.id)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkGray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = abbreviation,
                style = MaterialTheme.typography.titleMedium,
                color = White,
                fontWeight = FontWeight.Bold,
                fontFamily = Inter
            )
            Text(
                text = book.name_en,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                maxLines = 1,
                fontFamily = Inter
            )
        }
    }
}

@Composable
fun NumberGrid(items: List<Int>, onNumberClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onNumberClick(item) },
                colors = CardDefaults.cardColors(containerColor = DarkGray),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = item.toString(), 
                        color = White, 
                        fontWeight = FontWeight.Medium,
                        fontFamily = Inter
                    )
                }
            }
        }
    }
}

fun getAbbreviation(bookId: Int): String {
    return mapOf(
        1 to "Gen", 2 to "Exo", 3 to "Lev", 4 to "Num", 5 to "Deu", 6 to "Jos", 7 to "Jdg", 8 to "Rut",
        9 to "1Sa", 10 to "2Sa", 11 to "1Ki", 12 to "2Ki", 13 to "1Ch", 14 to "2Ch", 15 to "Ezr", 16 to "Neh",
        17 to "Tob", 18 to "Jdt", 19 to "Est", 20 to "Job", 21 to "Psa", 22 to "Pro", 23 to "Ecc", 24 to "Son",
        25 to "Wis", 26 to "Sir", 27 to "Isa", 28 to "Jer", 29 to "Lam", 30 to "Bar", 31 to "Eze", 32 to "Dan",
        33 to "Hos", 34 to "Joe", 35 to "Amo", 36 to "Oba", 37 to "Jon", 38 to "Mic", 39 to "Nah", 40 to "Hab",
        41 to "Zep", 42 to "Hag", 43 to "Zec", 44 to "Mal", 45 to "1Ma", 46 to "2Ma", 47 to "Mat", 48 to "Mrk",
        49 to "Luk", 50 to "Jhn", 51 to "Act", 52 to "Rom", 53 to "1Co", 54 to "2Co", 55 to "Gal", 56 to "Eph",
        57 to "Phi", 58 to "Col", 59 to "1Th", 60 to "2Th", 61 to "1Ti", 62 to "2Ti", 63 to "Tit", 64 to "Phm",
        65 to "Heb", 66 to "Jas", 67 to "1Pe", 68 to "2Pe", 69 to "1Jn", 70 to "2Jn", 71 to "3Jn", 72 to "Jud",
        73 to "Rev"
    )[bookId] ?: "Bk"
}
