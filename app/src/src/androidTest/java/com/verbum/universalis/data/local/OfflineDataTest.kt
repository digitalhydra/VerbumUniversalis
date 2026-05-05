package com.verbum.universalis.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.verbum.universalis.data.local.dao.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineDataTest {
    private lateinit var db: VerbumDatabase
    private lateinit var verseDao: VerseDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Use the actual asset-based database for this test
        db = Room.databaseBuilder(context, VerbumDatabase::class.java, "verbum.db")
            .createFromAsset("verbum_seed.db")
            .build()
        verseDao = db.verseDao()
    }

    @Test
    fun testSeedDataPresence() = runBlocking {
        val verses = verseDao.getChapterVerses(1, 1).first()
        
        // Verify Genesis 1:1 exists
        assertNotNull("Verses should not be null", verses)
        assertTrue("Database should contain seed data for Gen 1:1", verses.any { it.verseNumber == 1 })
        
        val texts = verseDao.getTextsForVerse(verses[0].id).first()
        assertTrue("Should contain DR translation", texts.any { it.langCode == "DR" })
        assertTrue("Should contain SCIO translation", texts.any { it.langCode == "SCIO" })
    }
}
