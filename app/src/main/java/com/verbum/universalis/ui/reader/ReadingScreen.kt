package com.verbum.universalis.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
        val studyInspectorVM: StudyInspectorViewModel = hiltViewModel()
        val activeLanguage by viewModel.activeLanguage.collectAsState(initial = "en_DRB")
        val currentPassage by viewModel.currentPassage.collectAsState()
        var showBookPicker by remember { mutableStateOf(false) }
        val showNoteHighlightSheet by viewModel.showNoteHighlightSheet.collectAsState(initial = false)
        val showStudyInspector by viewModel.showStudyInspector.collectAsState(initial = false)
        val selectedGreekWord by viewModel.selectedGreekWord.collectAsState(initial = null)
        val catenaEntries by studyInspectorVM.catenaEntries.collectAsState(initial = emptyList())
        val crossRefs by studyInspectorVM.crossRefs.collectAsState(initial = emptyList())
        val activeInspectorTab by studyInspectorVM.activeTab.collectAsState(initial = InspectorTab.CATENA)
        val inspectorWord by studyInspectorVM.selectedWord.collectAsState()
        val lexiconEntry by studyInspectorVM.lexiconEntry.collectAsState()

        LaunchedEffect(currentPassage) {
            studyInspectorVM.setCurrentVerse(
                currentPassage.bookId,
                currentPassage.chapter,
                currentPassage.verseRange?.start ?: 1
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Passage Chip
                                AssistChip(
                                    onClick = { showBookPicker = true },
                                    label = { Text(viewModel.getPassageReference(currentPassage)) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = null
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))

                                // Language Chip
                                var showLangMenu by remember { mutableStateOf(false) }
                                Box {
                                    AssistChip(
                                        onClick = { showLangMenu = true },
                                        label = { Text(viewModel.getLanguageDisplayName()) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            labelColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = null
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
                                        DropdownMenuItem(
                                            text = { Text("HE - עברית (Hebrew)") },
                                            onClick = {
                                                viewModel.setLanguage("he_HEB")
                                                showLangMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { 
                                if (onBack != null) onBack() 
                                else showBookPicker = true 
                            }) {
                                Icon(
                                    imageVector = if (onBack != null) Icons.Default.Home else Icons.Default.Menu,
                                    contentDescription = if (onBack != null) "Home" else "Menu"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.toggleStudyInspector() }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Study Inspector"
                                )
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
                            studyInspectorVM.selectWord(word)
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
                                    .align(Alignment.CenterEnd)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(paddingValues)
                            ) {
                                com.verbum.universalis.ui.reader.StudyInspector(
                                    selectedWord = inspectorWord,
                                    lexiconEntry = lexiconEntry,
                                    catenaEntries = catenaEntries,
                                    references = crossRefs,
                                    activeTab = activeInspectorTab,
                                    onTabSelect = { studyInspectorVM.setActiveTab(it) },
                                    onReferenceClick = { ref ->
                                        studyInspectorVM.parseReference(ref)?.let { (bookId, chapter, verse) ->
                                            viewModel.setPassage(bookId, chapter, verse)
                                            viewModel.toggleStudyInspector()
                                        }
                                    },
                                    showLexicon = activeLanguage == "el_GRK"
                                )
                            }
                        }
                    }

                    if (showNoteHighlightSheet) {
                        com.verbum.universalis.ui.components.NoteAndHighlightBottomSheet(
                            verseReference = viewModel.getPassageReference(currentPassage),
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

            // Top-down Book Picker Overlay
            AnimatedVisibility(
                visible = showBookPicker,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                BookPickerScreen(
                    initialBookId = currentPassage.bookId,
                    initialChapter = currentPassage.chapter,
                    initialVerse = currentPassage.verseRange?.start,
                    onClose = { showBookPicker = false },
                    onResult = { b, c, v ->
                        viewModel.setPassage(b, c, v)
                        showBookPicker = false
                    }
                )
            }
        }
    }
}
