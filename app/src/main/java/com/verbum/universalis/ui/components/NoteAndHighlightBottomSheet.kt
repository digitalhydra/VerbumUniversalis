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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteAndHighlightBottomSheet(
    verseReference: String,
    existingNote: String? = null,
    existingHighlightColorId: Int? = null,
    availableColors: List<Color>,
    onSave: (noteContent: String, highlightColorId: Int?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var noteContent by remember { mutableStateOf(existingNote ?: "") }
    var selectedColorId by remember { mutableStateOf(existingHighlightColorId) }

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
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Note & Highlight: $verseReference",
                style = MaterialTheme.typography.titleLarge
            )

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
                enabled = hasContent
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
