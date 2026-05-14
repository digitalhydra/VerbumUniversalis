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
import androidx.compose.material.icons.filled.Search
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
import com.verbum.universalis.core.theme.VerbumBlue
import com.verbum.universalis.core.theme.White
import com.verbum.universalis.core.theme.SoftGray
import com.verbum.universalis.core.theme.TextPrimaryLight
import com.verbum.universalis.core.theme.TextSecondaryLight
import com.verbum.universalis.data.entities.BookEntity
import com.verbum.universalis.data.repository.BibleRepository
import com.verbum.universalis.ui.reader.Passage
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

    private val _selectedVerse = MutableStateFlow<Int?>(null)
    val selectedVerse: StateFlow<Int?> = _selectedVerse.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allBooks: StateFlow<List<BookEntity>> = repository.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    val filteredBooks: StateFlow<List<BookEntity>> = combine(allBooks, _testamentFilter, _searchQuery) { books, filter, query ->
        val queryClean = query.trim()
        val isReferenceSearch = queryClean.any { it.isDigit() }
        
        books.filter { 
            // Use ID ranges for testaments as a more robust fallback due to ETL mapping bug
            val matchesTestament = if (filter == TestamentFilter.OLD) it.id <= 46 else it.id > 46
            
            val matchesQuery = if (isReferenceSearch) {
                val bookPart = queryClean.split(" ")[0]
                it.name_en.contains(bookPart, ignoreCase = true) || 
                it.name_es.contains(bookPart, ignoreCase = true) ||
                getAbbreviation(it.id).contains(bookPart, ignoreCase = true)
            } else {
                queryClean.isEmpty() || 
                it.name_en.contains(queryClean, ignoreCase = true) || 
                it.name_es.contains(queryClean, ignoreCase = true) ||
                getAbbreviation(it.id).contains(queryClean, ignoreCase = true)
            }
            matchesTestament && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _chapters = MutableStateFlow<List<Int>>(emptyList())
    val chapters: StateFlow<List<Int>> = _chapters.asStateFlow()

    private val _verses = MutableStateFlow<List<Int>>(emptyList())
    val verses: StateFlow<List<Int>> = _verses.asStateFlow()

    fun setInitialSelection(bookId: Int, chapter: Int, verse: Int?) {
        viewModelScope.launch {
            allBooks.filter { it.isNotEmpty() }.first().let { books ->
                val book = books.find { it.id == bookId }
                _selectedBook.value = book
                _selectedChapter.value = chapter
                _selectedVerse.value = verse
                
                if (book != null) {
                    _testamentFilter.value = if (book.id > 46) TestamentFilter.NEW else TestamentFilter.OLD
                    val maxChapter = repository.getMaxChapterForBook(book.id).first() ?: 1
                    _chapters.value = (1..maxChapter).toList()
                    
                    val maxVerse = repository.getMaxVerseForChapter(book.id, chapter).first() ?: 1
                    _verses.value = (1..maxVerse).toList()
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun handleSearchAction(onResult: (Int, Int, Int?, String?) -> Unit) {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) return
        
        val passage = Passage.fromString(query, Passage.BOOK_NAME_TO_ID)
        if (passage != null) {
            onResult(passage.bookId, passage.chapter, passage.firstVerse, passage.verseFilter)
        }
    }

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
            PickerStep.BOOKS -> { /* Handled by UI onClose */ }
            PickerStep.CHAPTERS -> _currentStep.value = PickerStep.BOOKS
            PickerStep.VERSES -> _currentStep.value = PickerStep.CHAPTERS
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookPickerScreen(
    viewModel: BookPickerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    initialBookId: Int? = null,
    initialChapter: Int? = null,
    initialVerse: Int? = null,
    onClose: () -> Unit,
    onResult: (bookId: Int, chapter: Int, verse: Int?, filter: String?) -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val testamentFilter by viewModel.testamentFilter.collectAsState()
    val books by viewModel.filteredBooks.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val verses by viewModel.verses.collectAsState()
    val selectedBook by viewModel.selectedBook.collectAsState()
    val selectedChapter by viewModel.selectedChapter.collectAsState()
    val selectedVerse by viewModel.selectedVerse.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    LaunchedEffect(initialBookId, initialChapter, initialVerse) {
        if (initialBookId != null && initialChapter != null) {
            viewModel.setInitialSelection(initialBookId, initialChapter, initialVerse)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = White
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (currentStep == PickerStep.BOOKS) onClose() else viewModel.goBack() }) {
                    Icon(
                        imageVector = if (currentStep == PickerStep.BOOKS) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimaryLight
                    )
                }
                Text(
                    text = "Bible Index",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryLight,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text("Jump to:") },
                placeholder = { Text("Search book or ref (e.g. Gen 1:1)") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextPrimaryLight) },
                trailingIcon = {
                    if (searchQuery.any { it.isDigit() }) {
                        TextButton(onClick = { viewModel.handleSearchAction { b, c, v, f -> onResult(b, c, v, f) } }) {
                            Text("Go", color = VerbumBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                textStyle = LocalTextStyle.current.copy(color = TextPrimaryLight),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VerbumBlue,
                    unfocusedBorderColor = SoftGray,
                    focusedTextColor = TextPrimaryLight,
                    unfocusedTextColor = TextPrimaryLight,
                    cursorColor = VerbumBlue,
                    focusedLabelColor = VerbumBlue,
                    unfocusedLabelColor = TextSecondaryLight
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Go
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onGo = { viewModel.handleSearchAction(onResult) }
                )
            )

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

            Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (books.isEmpty() && currentStep == PickerStep.BOOKS && searchQuery.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VerbumBlue)
                    }
                } else {
                    when (currentStep) {
                        PickerStep.BOOKS -> {
                            BookGrid(
                                books = books, 
                                selectedBookId = selectedBook?.id,
                                onBookClick = { viewModel.selectBook(it) }
                            )
                        }
                        PickerStep.CHAPTERS -> {
                            NumberGrid(
                                items = chapters, 
                                selectedItem = selectedChapter,
                                onNumberClick = { viewModel.selectChapter(it) }
                            )
                        }
                        PickerStep.VERSES -> {
                            NumberGrid(
                                items = verses, 
                                selectedItem = selectedVerse,
                                onNumberClick = { verse ->
                                    selectedBook?.let { book ->
                                        selectedChapter?.let { chapter ->
                                            onResult(book.id, chapter, verse, null)
                                        }
                                    }
                                }
                            )
                        }
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
                                onResult(book.id, chapter, null, null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerbumBlue)
                ) {
                    Text("Jump to Chapter", fontFamily = Inter, color = White)
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
            color = if (isSelected) TextPrimaryLight else TextSecondaryLight,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = Inter
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .height(2.dp)
                    .width(40.dp)
                    .background(VerbumBlue)
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
            color = if (isSelected) TextPrimaryLight else TextSecondaryLight,
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
                    .background(VerbumBlue)
            )
        }
    }
}

@Composable
fun BookGrid(books: List<BookEntity>, selectedBookId: Int?, onBookClick: (BookEntity) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(books) { book ->
            BookItem(
                book = book, 
                isSelected = book.id == selectedBookId,
                onClick = { onBookClick(book) }
            )
        }
    }
}

@Composable
fun BookItem(book: BookEntity, isSelected: Boolean, onClick: () -> Unit) {
    val abbreviation = getAbbreviation(book.id)
    val fullName = Passage.BOOK_ID_TO_LONG_NAME[book.id] ?: book.name_en
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) VerbumBlue.copy(alpha = 0.1f) else SoftGray.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, VerbumBlue) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = abbreviation,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) VerbumBlue else TextPrimaryLight,
                fontWeight = FontWeight.Bold,
                fontFamily = Inter
            )
            Text(
                text = fullName,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) VerbumBlue.copy(alpha = 0.7f) else TextSecondaryLight,
                maxLines = 1,
                fontFamily = Inter
            )
        }
    }
}

@Composable
fun NumberGrid(items: List<Int>, selectedItem: Int?, onNumberClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            val isSelected = item == selectedItem
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onNumberClick(item) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) VerbumBlue.copy(alpha = 0.1f) else SoftGray.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, VerbumBlue) else null
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = item.toString(), 
                        color = if (isSelected) VerbumBlue else TextPrimaryLight,
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
