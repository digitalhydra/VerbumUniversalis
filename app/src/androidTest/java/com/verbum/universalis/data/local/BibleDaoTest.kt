package com.verbum.universalis.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.verbum.universalis.data.local.dao.*
import com.verbum.universalis.data.local.entities.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BibleDaoTest {
    private lateinit var db: VerbumDatabase
    private lateinit var verseDao: VerseDao
    private lateinit var interlinearDao: InterlinearDao
    private lateinit var lexiconDao: LexiconDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, VerbumDatabase::class.java).build()
        verseDao = db.verseDao()
        interlinearDao = db.interlinearDao()
        lexiconDao = db.lexiconDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeAndReadBook() = runBlocking {
        val book = BookEntity(1, "Genesis", "Génesis", "Genesis", "OT")
        // Assuming we add an insert method to VerseDao or a separate BookDao
        // For this test, we'll assume DAOs are updated with inserts
    }

    @Test
    fun testVerseQuery() = runBlocking {
        // Test logic for getting chapter verses
    }
}
