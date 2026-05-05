package com.verbum.universalis.ui.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.verbum.universalis.core.theme.VerbumTheme
import com.verbum.universalis.ui.components.BookPickerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    viewModel: ReadingViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    VerbumTheme {
        val navController = rememberNavController()
        var showLanguageToggle by remember { mutableStateOf(false) }
        val activeLanguage by viewModel.activeLanguage.collectAsState(initial = "DR")
        var showBookPicker by remember { mutableStateOf(false) }
        var quickJumpQuery by remember { mutableStateOf("") }
        val showNoteBottomSheet by viewModel.showNoteBottomSheet.collectAsState(initial = false)
        val selectedVerseIdForNote by viewModel.selectedVerseIdForNote.collectAsState(initial = null)
        val showHighlightPicker by viewModel.showHighlightPicker.collectAsState(initial = false)
        val selectedVerseIdForHighlight by viewModel.selectedVerseIdForHighlight.collectAsState(initial = null)

        if (showBookPicker) {
            BookPickerScreen(
                onBookSelected = { bookId, chapter ->
                    viewModel.setPassage(bookId, chapter)
                    showBookPicker = false
                },
                onDismiss = { showBookPicker = false }
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
                        Text("☰", modifier = Modifier.clickable { showBookPicker = true }.padding(16.dp))
                    },
                    actions = {
                        Text(
                            text = if (activeLanguage == "DR") "EN" else "ES",
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clickable { viewModel.toggleLanguage() }
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            ReadingCanvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onAction = { action, verseId ->
                    when (action) {
                        "interlinear" -> {
                            navController.navigate(
                                com.verbum.universalis.ui.navigation.Route.InterlinearReader.createRoute(verseId)
                            )
                        }
                        "note" -> viewModel.showNoteSheet(verseId)
                        "highlight" -> viewModel.showHighlightPicker(verseId)
                    }
                }
            )
        }

        if (showNoteBottomSheet) {
            com.verbum.universalis.ui.components.NoteBottomSheet(
                verseId = selectedVerseIdForNote,
                onDismiss = { viewModel.hideNoteSheet() },
                onSave = { viewModel.saveNote(it) }
            )
        }

        if (showHighlightPicker) {
            com.verbum.universalis.ui.components.ColorPickerBottomSheet(
                colors = HighlightPalette.all,
                onColorSelected = { colorId -> viewModel.saveHighlight(colorId) },
                onDismiss = { viewModel.hideHighlightPicker() }
            )
        }
    }
}