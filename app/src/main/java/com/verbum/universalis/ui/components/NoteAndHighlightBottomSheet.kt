package com.verbum.universalis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.verbum.universalis.data.json.Note
import com.verbum.universalis.ui.theme.HighlightPalette

enum class DrawerMode {
    LIST, FORM
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteAndHighlightBottomSheet(
    verseReference: String,
    existingNotes: List<Note> = emptyList(),
    availableColors: List<Color>,
    onSave: (noteContent: String, highlightColorId: Int?) -> Unit,
    onDelete: (Note) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var noteContent by remember { mutableStateOf("") }
    var selectedColorId by remember { mutableStateOf<Int?>(null) }
    var currentMode by remember { 
        mutableStateOf(if (existingNotes.isNotEmpty()) DrawerMode.LIST else DrawerMode.FORM) 
    }
    
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    val hasContent = noteContent.isNotBlank() || selectedColorId != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentMode == DrawerMode.FORM && existingNotes.isNotEmpty()) {
                    IconButton(onClick = { currentMode = DrawerMode.LIST }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to List")
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Text(
                    "Note & Highlight: $verseReference",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                if (currentMode == DrawerMode.LIST) {
                    IconButton(onClick = { currentMode = DrawerMode.FORM }) {
                        Icon(Icons.Default.Add, contentDescription = "Create New Note")
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            if (currentMode == DrawerMode.LIST) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(existingNotes) { note ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(
                                    color = note.highlightColorId?.let { 
                                        availableColors.getOrNull(it)?.copy(alpha = 0.2f) 
                                    } ?: Color.Transparent,
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(note.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { noteToDelete = note }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Note",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Highlight Section
                    Text("Highlight", style = MaterialTheme.typography.labelMedium)
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableColors.forEachIndexed { index, color ->
                            val isSelected = selectedColorId == index
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.5f))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedColorId = if (isSelected) null else index
                                    }
                            )
                        }
                    }

                    if (selectedColorId != null) {
                        TextButton(
                            onClick = { selectedColorId = null },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text("Clear Highlight")
                        }
                    }

                    // Note Section
                    Text("Note", style = MaterialTheme.typography.labelMedium)

                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        placeholder = { Text("Add a note...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    // Single Save Button
                    Button(
                        onClick = {
                            onSave(noteContent, selectedColorId)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasContent,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    ) {
                        Text(
                            when {
                                noteContent.isNotBlank() && selectedColorId != null -> "Save Note & Highlight"
                                noteContent.isNotBlank() -> "Save Note"
                                selectedColorId != null -> "Save Highlight"
                                else -> ""
                            }
                        )
                    }
                }
            }
        }
    }

    if (noteToDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        noteToDelete?.let { onDelete(it) }
                        noteToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
