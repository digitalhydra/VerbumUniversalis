package com.verbum.universalis.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.verbum.universalis.core.theme.VerbumTheme
import com.verbum.universalis.data.entities.InterlinearWordEntity
import com.verbum.universalis.ui.components.BookPickerScreen
import com.verbum.universalis.ui.navigation.Route
import com.verbum.universalis.ui.navigation.MassReadings
import com.verbum.universalis.ui.navigation.PlanReadings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    viewModel: ReadingViewModel = hiltViewModel(),
    initialBookId: Int? = null,
    initialChapter: Int? = null,
    initialVerse: Int? = null,
    // Mass readings flow
    showNextReading: Boolean = false,
    nextReadingText: String? = null,
    onNextReadingClick: (() -> Unit)? = null,
    // Bible in a Year flow
    showNextDay: Boolean = false,
    nextDayText: String? = null,
    onNextDayClick: (() -> Unit)? = null,
    // Back navigation
    onBack: (() -> Unit)? = null
) {
    VerbumTheme {
        LaunchedEffect(initialBookId, initialChapter, initialVerse) {
            if (initialBookId != null && initialChapter != null) {
                viewModel.setPassage(initialBookId, initialChapter, initialVerse)
            }
        }

        val navController = rememberNavController()
        var showLanguageToggle by remember { mutableStateOf(false) }
        val activeLanguage by viewModel.activeLanguage.collectAsState(initial = "en_DRB")
        var showBookPicker by remember { mutableStateOf(false) }
        var quickJumpQuery by remember { mutableStateOf("") }
        val showNoteHighlightSheet by viewModel.showNoteHighlightSheet.collectAsState(initial = false)
        val showStudyInspector by viewModel.showStudyInspector.collectAsState(initial = false)
        val selectedGreekWord by viewModel.selectedGreekWord.collectAsState(initial = null)

        if (showBookPicker) {
            BookPickerScreen(
                onBookClick = { book ->
                    viewModel.setPassage(book.id, 1) // Default to chapter 1
                    showBookPicker = false
                }
            )
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        OutlinedTextField(
                            value = quickJumpQuery,
                            onValueChange = {
                                quickJumpQuery = it
                                if (it.contains(":") || it.contains(" ")) {
                                    viewModel.setPassageFromString(it)
                                }
                            },
                            placeholder = { Text("Jump to... (e.g. Ps 22:1)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )
                    },
                    navigationIcon = {
                        Text(
                            text = if (onBack != null) "←" else "☰",
                            modifier = Modifier
                                .padding(16.dp)
                                .clickable { 
                                    if (onBack != null) onBack() 
                                    else showBookPicker = true 
                                }
                        )
                    },
                    actions = {
                        // Study Inspector toggle
                        Text(
                            text = if (showStudyInspector) "◀" else "▶",
                            modifier = Modifier
                                .padding(12.dp)
                                .clickable { viewModel.toggleStudyInspector() }
                        )
                        // Language dropdown + Greek
                        var showLangMenu by remember { mutableStateOf(false) }
                        Box {
                            Text(
                                text = viewModel.getLanguageDisplayName(),
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .clickable { showLangMenu = true }
                            )
                            DropdownMenu(
                                expanded = showLangMenu,
                                onDismissRequest = { showLangMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("EN - English (DRB)") },
                                    onClick = {
                                        viewModel.setLanguage("en_DRB")
                                        showLangMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("ES - Español (Platense)") },
                                    onClick = {
                                        viewModel.setLanguage("es_PLA")
                                        showLangMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("LA - Latina (Vulgata)") },
                                    onClick = {
                                        viewModel.setLanguage("la_VUL")
                                        showLangMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("EL - Ελληνικά (Greek)") },
                                    onClick = {
                                        viewModel.setLanguage("el_GRK")
                                        showLangMenu = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Next Day button (Bible in a Year)
                    if (showNextDay && nextDayText != null) {
                        Text(
                            text = nextDayText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable { onNextDayClick?.invoke() }
                        )
                    }
                    // Next Reading button (Mass)
                    if (showNextReading && nextReadingText != null) {
                        Text(
                            text = nextReadingText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable { onNextReadingClick?.invoke() }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                ReadingCanvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    onAction = { action, verseId ->
                        when (action) {
                            "interlinear" -> {
                                // Navigate to the InterlinearReader full-screen route
                                navController.navigate(Route.InterlinearReader.createRoute(verseId))
                            }
                            "reference", "catena" -> {
                                // Toggle the study inspector to show the relevant tab
                                viewModel.toggleStudyInspector()
                            }
                            "note", "highlight" -> {
                                viewModel.showNoteHighlightSheet(verseId)
                            }
                        }
                    },
                    showStudyInspector = showStudyInspector,
                    onWordClick = { word -> 
                        viewModel.selectGreekWord(word)
                        if (!showStudyInspector) viewModel.toggleStudyInspector()
                    }
                )

                // StudyInspector - Slide in from RIGHT
                if (showStudyInspector) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Semi-transparent overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { viewModel.toggleStudyInspector() }
                                .background(
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
                                )
                        )
                        // Right panel
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(350.dp)
                                .align(androidx.compose.ui.Alignment.CenterEnd)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(paddingValues)
                        ) {
                            com.verbum.universalis.ui.reader.StudyInspector(
                                selectedWord = selectedGreekWord,
                                lexiconEntry = null,
                                catenaEntries = emptyList(),
                                references = emptyList(),
                                activeTab = com.verbum.universalis.ui.reader.InspectorTab.CATENA,
                                onTabSelect = { },
                                showLexicon = activeLanguage == "el_GRK"
                            )
                        }
                    }
                }

                if (showNoteHighlightSheet) {
                    com.verbum.universalis.ui.components.NoteAndHighlightBottomSheet(
                        verseReference = viewModel.getCurrentPassageReference(),
                        existingNote = null,
                        existingHighlightColorId = null,
                        availableColors = com.verbum.universalis.ui.theme.HighlightPalette.all,
                        onSave = { note, colorId ->
                            viewModel.saveNoteWithHighlight(note, colorId)
                        },
                        onDismiss = { viewModel.hideNoteHighlightSheet() }
                    )
                }
            }
        }
    }
}
