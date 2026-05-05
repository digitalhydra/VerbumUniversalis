package com.verbun.universalis.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.verbun.universalis.data.daos.*
import com.verbun.universalis.data.db.AppDatabase
import com.verbun.universalis.data.entities.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var verseDao: VerseDao
    private lateinit var interlinearDao: InterlinearDao
    private lateinit var lexiconDao: LexiconDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Use in-memory database for testing
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        verseDao = db.verseDao()
        interlinearDao = db.interlinearDao()
        lexiconDao = db.lexiconDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndFetchVerseWithTexts() = runBlocking {
        // 1. Setup data
        val bookId = 1
        val verse = VerseEntity(book_id = bookId, chapter = 1, verse_number = 1)
        val vId = verseDao.insertVerse(verse).toInt()
        
        val texts = listOf(
            TextEntity(verse_id = vId, lang_code = "DR", content = "In the beginning"),
            TextEntity(verse_id = vId, lang_code = "Vulg", content = "In principio")
        )
        verseDao.insertTexts(texts)
        
        // 2. Fetch
        val result = verseDao.getChapter(bookId, 1).first()
        
        // 3. Verify
        assertThat(result).hasSize(1)
        assertThat(result[0].verse.verse_number).isEqualTo(1)
        assertThat(result[0].texts).hasSize(2)
    }

    @Test
    fun testLexiconLookup() = runBlocking {
        val entry = LexiconEntity(lemma = "H7225", language = "Heb", definition = "In the beginning")
        lexiconDao.insertLexicon(entry)
        
        val result = lexiconDao.getDefinition("H7225").first()
        assertThat(result).isNotNull()
        assertThat(result?.definition).isEqualTo("In the beginning")
    }

    @Test
    fun testInterlinearFetch() = runBlocking {
        val vId = 100
        val words = listOf(
            InterlinearWordEntity(verse_id = vId, word_order = 1, original = "Bereshit", transliteration = "Brs", literal = "Beginning", morphology = "Noun", lemma = "H7225"),
            InterlinearWordEntity(verse_id = vId, word_order = 2, original = "Bara", transliteration = "Br", literal = "Created", morphology = "Verb", lemma = "H1254")
        )
        interlinearDao.insertWords(words)
        
        val result = interlinearDao.getWordsForVerse(vId).first()
        assertThat(result).hasSize(2)
        assertThat(result[0].original).isEqualTo("Bereshit")
    }
}
