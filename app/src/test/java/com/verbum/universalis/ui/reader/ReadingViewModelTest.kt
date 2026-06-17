package com.verbum.universalis.ui.reader

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.SavedStateHandle
import com.verbum.universalis.core.LanguageManager
import com.verbum.universalis.data.json.FileManager
import com.verbum.universalis.data.repository.BibleRepository
import com.verbum.universalis.data.repository.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class ReadingViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var viewModel: ReadingViewModel
    private lateinit var fileManager: FileManager
    
    @Mock
    lateinit var repository: BibleRepository
    
    @Mock
    lateinit var notesRepository: NotesRepository

    @Mock
    lateinit var languageManager: LanguageManager

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        fileManager = FileManager(app)
        
        val savedStateHandle = SavedStateHandle(mapOf("bookId" to 1, "chapter" to 1))
        viewModel = ReadingViewModel(repository, notesRepository, languageManager, app, savedStateHandle)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun testNoteHighlightSheetVisibility() = runTest {
        // Initially false
        assertFalse(viewModel.showNoteHighlightSheet.value)
        
        // Show sheet
        viewModel.showNoteHighlightSheet(1)
        assertTrue(viewModel.showNoteHighlightSheet.value)
        assertEquals(1, viewModel.selectedVerseForNoteHighlight.value)
        
        // Hide sheet
        viewModel.hideNoteHighlightSheet()
        assertFalse(viewModel.showNoteHighlightSheet.value)
        assertNull(viewModel.selectedVerseForNoteHighlight.value)
    }
    
    @Test
    fun testSaveNote() = runTest {
        viewModel.selectVerse(1)
        viewModel.saveNote("Test note content")
        
        // Sheet and selection should be cleared after save
        assertNull(viewModel.selectedVerseId.value)
        assertFalse(viewModel.isSelectionMode.value)
    }
}
