package com.verbum.universalis.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.verbum.universalis.data.local.entities.*

@Dao
interface VerseDao {
    @Query("SELECT * FROM verses WHERE bookId = :bookId AND chapter = :chapter ORDER BY verseNumber ASC")
    fun getChapterVerses(bookId: Int, chapter: Int): Flow<List<VerseEntity>>

    @Query("SELECT * FROM texts WHERE verseId = :verseId")
    fun getTextsForVerse(verseId: Long): Flow<List<TextEntity>>

    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<BookEntity>>
}

@Dao
interface InterlinearDao {
    @Query("SELECT * FROM interlinear_words WHERE verseId = :verseId ORDER BY wordOrder ASC")
    fun getWordsForVerse(verseId: Long): Flow<List<InterlinearWordEntity>>
}

@Dao
interface LexiconDao {
    @Query("SELECT * FROM lexicon WHERE lemma = :lemma LIMIT 1")
    fun getDefinition(lemma: String): Flow<LexiconEntity?>
}
