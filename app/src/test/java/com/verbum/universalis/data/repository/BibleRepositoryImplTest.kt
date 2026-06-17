package com.verbum.universalis.data.repository

import com.verbum.universalis.data.daos.*
import com.verbum.universalis.data.entities.*
import com.verbum.universalis.data.repository.BibleRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BibleRepositoryImplTest {
    private val verseDao: VerseDao = mock()
    private val interlinearDao: InterlinearDao = mock()
    private val lexiconDao: LexiconDao = mock()
    private val catenaRepository: CatenaRepository = mock()
    private val crossRefsRepository: CrossRefsRepository = mock()
    private val context: android.content.Context = mock()
    private val repository = BibleRepository(verseDao, interlinearDao, lexiconDao, catenaRepository, crossRefsRepository, context)

    @Test
    fun `getInterlinearWords maps entities correctly`() = runBlocking {
        val entity = InterlinearWordEntity(1, 100, 1, "λόγος", "logos", "word", "Noun", "logos")
        whenever(interlinearDao.getWordsForVerse(100)).thenReturn(flowOf(listOf(entity)))

        val result = repository.getInterlinearWordsForVerse(100).first()

        assertEquals(1, result.size)
        assertEquals("λόγος", result[0].original)
        assertEquals("logos", result[0].lemma)
    }

    @Test
    fun `getLexiconDefinition maps entity correctly`() = runBlocking {
        val entity = LexiconEntity("logos", "Greek", "The Word")
        whenever(lexiconDao.getDefinition("logos")).thenReturn(flowOf(entity))

        val result = repository.getLexiconEntry("logos").first()

        assertEquals("The Word", result?.definition)
    }
}
