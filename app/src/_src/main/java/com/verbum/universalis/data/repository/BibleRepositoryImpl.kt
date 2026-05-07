package com.verbum.universalis.data.repository

import com.verbum.universalis.data.local.dao.*
import com.verbum.universalis.domain.model.*
import com.verbum.universalis.domain.repository.BibleRepository
import com.verbum.universalis.data.local.entities.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class BibleRepositoryImpl @Inject constructor(
    private val verseDao: VerseDao,
    private val interlinearDao: InterlinearDao,
    private val lexiconDao: LexiconDao
) : BibleRepository {

    override fun getChapter(bookId: Int, chapter: Int): Flow<List<BibleVerse>> {
        return verseDao.getChapterVerses(bookId, chapter).flatMapLatest { verses ->
            flow {
                val verseList = verses.map { v ->
                    val textsFlow = verseDao.getTextsForVerse(v.id).first()
                    BibleVerse(
                        id = v.id,
                        bookId = v.bookId,
                        chapter = v.chapter,
                        verseNumber = v.verseNumber,
                        texts = textsFlow.associate { it.langCode to it.content }
                    )
                }
                emit(verseList)
            }
        }
    }

    override fun getInterlinearWords(verseId: Long): Flow<List<InterlinearWord>> {
        return interlinearDao.getWordsForVerse(verseId).map { list ->
            list.map { 
                InterlinearWord(it.original, it.transliteration, it.literal, it.morphology, it.lemma) 
            }
        }
    }

    override fun getLexiconDefinition(lemma: String): Flow<LexiconEntry?> {
        return lexiconDao.getDefinition(lemma).map { 
            it?.let { LexiconEntry(it.lemma, it.definition) } 
        }
    }

    override fun getAllBooks(): Flow<List<BookEntity>> = verseDao.getAllBooks()
}
