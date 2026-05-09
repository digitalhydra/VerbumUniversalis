package com.verbum.universalis.data.daos

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.verbum.universalis.data.entities.*
import kotlinx.coroutines.flow.Flow

data class VerseWithTexts(
    @Embedded val verse: VerseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "verse_id"
    )
    val texts: List<TextEntity>
)

@Dao
interface VerseDao {
    @Insert
    fun insertVerse(verse: VerseEntity): Long

    @Insert
    fun insertTexts(texts: List<TextEntity>)

    @Transaction
    @Query("SELECT * FROM verses WHERE book_id = :bookId AND chapter = :chapter ORDER BY verse_number ASC")
    fun getChapter(bookId: Int, chapter: Int): Flow<List<VerseWithTexts>>

    @Query("SELECT * FROM verses WHERE id = :verseId")
    fun getVerseById(verseId: Int): Flow<VerseEntity>

    @Query("SELECT * FROM books ORDER BY id ASC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT MAX(chapter) FROM verses WHERE book_id = :bookId")
    fun getMaxChapterForBook(bookId: Int): Flow<Int?>

    @Query("SELECT MAX(verse_number) FROM verses WHERE book_id = :bookId AND chapter = :chapter")
    fun getMaxVerseForChapter(bookId: Int, chapter: Int): Flow<Int?>
}

@Dao
interface InterlinearDao {
    @Insert
    fun insertWords(words: List<InterlinearWordEntity>)

    @Query("SELECT * FROM interlinear_words WHERE verse_id = :verseId ORDER BY word_order ASC")
    fun getWordsForVerse(verseId: Int): Flow<List<InterlinearWordEntity>>

    @Query("""
        SELECT w.* FROM interlinear_words w
        INNER JOIN verses v ON w.verse_id = v.id
        WHERE v.book_id = :bookId AND v.chapter = :chapter
        ORDER BY v.verse_number, w.word_order ASC
    """)
    fun getWordsForChapter(bookId: Int, chapter: Int): Flow<List<InterlinearWordEntity>>
}

@Dao
interface LexiconDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLexicon(entry: LexiconEntity)

    @Query("SELECT * FROM lexicon WHERE lemma = :lemma LIMIT 1")
    fun getDefinition(lemma: String): Flow<LexiconEntity?>
}
