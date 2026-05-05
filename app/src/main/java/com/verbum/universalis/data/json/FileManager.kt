package com.verbum.universalis.data.json

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { prettyPrint = true }
    private val baseDir = File(context.filesDir, "userdata")
    
    init {
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
    }

    private fun <T> writeAtomic(filename: String, data: T, serializer: (T) -> String) {
        val file = File(baseDir, filename)
        val tempFile = File(baseDir, "$filename.tmp")
        
        try {
            tempFile.writeText(serializer(data))
            tempFile.renameTo(file)
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    private inline fun <reified T> readFile(filename: String, crossinline deserializer: (String) -> T): T? {
        val file = File(baseDir, filename)
        if (!file.exists()) return null
        
        return try {
            deserializer(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun saveSettings(settings: UserSettings) {
        writeAtomic("settings.json", settings) { json.encodeToString(it) }
    }

    fun loadSettings(): UserSettings? {
        return readFile("settings.json") { json.decodeFromString<UserSettings>(it) }
    }

    fun saveHighlights(highlights: List<Highlight>) {
        writeAtomic("highlights.json", highlights) { json.encodeToString(it) }
    }

    fun loadHighlights(): List<Highlight> {
        return readFile("highlights.json") { json.decodeFromString<List<Highlight>>(it) } ?: emptyList()
    }

    fun saveNotes(notes: List<Note>) {
        writeAtomic("notes.json", notes) { json.encodeToString(it) }
    }

    fun loadNotes(): List<Note> {
        return readFile("notes.json") { json.decodeFromString<List<Note>>(it) } ?: emptyList()
    }

    fun saveProgress(progress: List<ReadingProgress>) {
        writeAtomic("progress.json", progress) { json.encodeToString(it) }
    }

    fun loadProgress(): List<ReadingProgress> {
        return readFile("progress.json") { json.decodeFromString<List<ReadingProgress>>(it) } ?: emptyList()
    }
}
