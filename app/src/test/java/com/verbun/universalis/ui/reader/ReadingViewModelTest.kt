package com.verbun.universalis.ui.reader

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.verbun.universalis.data.json.FileManager
import com.verbun.universalis.data.repository.BibleRepository
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
    
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        fileManager = FileManager(app)
        viewModel = ReadingViewModel(repository, app)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun testNoteSheetVisibility() = runTest {
        // Initially false
        assertFalse(viewModel.showNoteBottomSheet.value)
        
        // Show note sheet
        viewModel.showNoteSheet(1)
        assertTrue(viewModel.showNoteBottomSheet.value)
        assertEquals(1, viewModel.selectedVerseIdForNote.value)
        
        // Hide note sheet
        viewModel.hideNoteSheet()
        assertFalse(viewModel.showNoteBottomSheet.value)
        assertNull(viewModel.selectedVerseIdForNote.value)
    }
    
    @Test
    fun testHighlightPickerVisibility() = runTest {
        // Initially false
        assertFalse(viewModel.showHighlightPicker.value)
        
        // Show highlight picker
        viewModel.showHighlightPicker(1)
        assertTrue(viewModel.showHighlightPicker.value)
        assertEquals(1, viewModel.selectedVerseIdForHighlight.value)
        
        // Hide highlight picker
        viewModel.hideHighlightPicker()
        assertFalse(viewModel.showHighlightPicker.value)
        assertNull(viewModel.selectedVerseIdForHighlight.value)
    }
    
    @Test
    fun testSaveNote() = runTest {
        viewModel.showNoteSheet(1)
        viewModel.saveNote("Test note content")
        
        // Verify note was saved
        val notes = fileManager.loadNotes()
        assertEquals(1, notes.size)
        assertEquals("Test note content", notes[0].content)
        assertEquals(1, notes[0].verseId)
        
        // Sheet should be hidden after save
        assertFalse(viewModel.showNoteBottomSheet.value)
    }
}
