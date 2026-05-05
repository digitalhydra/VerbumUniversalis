package com.verbun.universalis.data.json

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class FileManagerTest {
    private lateinit var fileManager: FileManager
    private val json = Json { prettyPrint = true }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fileManager = FileManager(context)
        // Clean up
        fileManager.saveSettings(UserSettings())
        fileManager.saveHighlights(emptyList())
        fileManager.saveNotes(emptyList())
        fileManager.saveProgress(emptyList())
    }

    @Test
    fun testSettingsAtomicWrite() {
        val settings = UserSettings(theme = "Dark", language = "ES")
        fileManager.saveSettings(settings)
        val loaded = fileManager.loadSettings()
        assertNotNull(loaded)
        assertEquals("Dark", loaded?.theme)
        assertEquals("ES", loaded?.language)
    }

    @Test
    fun testHighlightsPersistence() {
        val highlights = listOf(
            Highlight(verseId = 1, colorId = 0, timestamp = 1234L)
        )
        fileManager.saveHighlights(highlights)
        val loaded = fileManager.loadHighlights()
        assertEquals(1, loaded.size)
        assertEquals(1, loaded[0].verseId)
    }

    @Test
    fun testNotesPersistence() {
        val notes = listOf(
            Note(verseId = 1, content = "Test note", timestamp = 1234L)
        )
        fileManager.saveNotes(notes)
        val loaded = fileManager.loadNotes()
        assertEquals(1, loaded.size)
        assertEquals("Test note", loaded[0].content)
    }

    @Test
    fun testProgressPersistence() {
        val progress = listOf(
            ReadingProgress("plan1", 0, 0, true, 1234L)
        )
        fileManager.saveProgress(progress)
        val loaded = fileManager.loadProgress()
        assertEquals(1, loaded.size)
        assertTrue(loaded[0].completed)
    }
}
