package com.verbum.universalis.domain.repository

import kotlinx.coroutines.flow.Flow
import com.verbum.universalis.domain.model.*

interface BibleRepository {
    fun getChapter(bookId: Int, chapter: Int): Flow<List<BibleVerse>>
    fun getInterlinearWords(verseId: Long): Flow<List<InterlinearWord>>
    fun getLexiconDefinition(lemma: String): Flow<LexiconEntry?>
    fun getAllBooks(): Flow<List<BookEntity>>
}
