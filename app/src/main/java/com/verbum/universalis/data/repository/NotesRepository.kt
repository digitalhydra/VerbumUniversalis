package com.verbum.universalis.data.repository

import android.content.Context
import com.verbum.universalis.data.json.FileManager
import com.verbum.universalis.data.json.Note
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager
) {

    fun getNotes(): List<Note> {
        return fileManager.loadNotes()
    }

    fun saveNote(note: Note) {
        val currentNotes = fileManager.loadNotes()
        val updatedNotes = currentNotes + note
        fileManager.saveNotes(updatedNotes)
    }

    fun deleteNote(noteId: Long) {
        // Note: The Note data class in DataClasses.kt does not have an id field.
        // We are using the timestamp as a pseudo-id for simplicity.
        // In a real app, we would have a proper id.
        val currentNotes = fileManager.loadNotes()
        val filteredNotes = currentNotes.filter { it.timestamp != noteId }
        fileManager.saveNotes(filteredNotes)
    }
}