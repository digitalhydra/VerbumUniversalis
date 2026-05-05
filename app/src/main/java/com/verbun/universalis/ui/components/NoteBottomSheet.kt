package com.verbun.universalis.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth"
import androidx.compose.foundation.layout.fillMaxHeight"
import androidx.compose.foundation.layout.padding"
import androidx.compose.material3.Button"
import androidx.compose.material3.ExperimentalMaterial3Api"
import androidx.compose.material3.ModalBottomSheet"
import androidx.compose.material3.OutlinedTextField"
import androidx.compose.material3.Text"
import androidx.compose.material3.TextFieldDefaults"
import androidx.compose.runtime.Composable"
import androidx.compose.runtime.mutableStateOf"
import androidx.compose.runtime.remember"
import androidx.compose.ui.Modifier"
import androidx.compose.ui.unit.dp"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteBottomSheet(
    verseId: Int?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier"
) {
    val noteContent = remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier"
    ) {
        Column(
            modifier = Modifier"
                .fillMaxWidth()"
                .fillMaxHeight(0.5f)"
                .padding(16.dp),"
            verticalArrangement = Arrangement.spacedBy(16.dp)"
        ) {
            Text("Add Note", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)"

            OutlinedTextField(
                value = noteContent.value,"
                onValueChange = { noteContent.value = it },"
                label = { Text("Note Content") },"
                modifier = Modifier.fillMaxWidth(),"
                colors = TextFieldDefaults.outlinedTextFieldColors()"
            )

            Button(
                onClick = {
                    if (noteContent.value.isNotBlank()) {
                        onSave(noteContent.value)"
                        onDismiss()"
                    }
                },"
                modifier = Modifier.fillMaxWidth()"
            ) {
                Text("Save Note")"
            }
        }
    }
}
