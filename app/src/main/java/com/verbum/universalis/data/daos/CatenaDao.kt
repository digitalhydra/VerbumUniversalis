package com.verbum.universalis.data.daos

import androidx.room.*
import com.verbum.universalis.data.entities.CatenaCommentaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatenaDao {

    @Query("SELECT * FROM commentaries WHERE book = :book ORDER BY author_normalized, chapter, verse_start")
    fun getCommentariesByBook(book: String): Flow<List<CatenaCommentaryEntity>>

    @Query("SELECT * FROM commentaries WHERE book = :book AND chapter = :chapter ORDER BY author_normalized, verse_start")
    fun getCommentariesForChapter(book: String, chapter: Int): Flow<List<CatenaCommentaryEntity>>

    @Query("SELECT * FROM commentaries WHERE book = :book AND chapter = :chapter AND verse_start <= :verseNumber AND verse_end >= :verseNumber ORDER BY author_normalized")
    fun getCommentariesForVerse(book: String, chapter: Int, verseNumber: Int): Flow<List<CatenaCommentaryEntity>>

    @Query("SELECT DISTINCT author_normalized FROM commentaries WHERE book = :book ORDER BY author_normalized")
    fun getAuthorsForBook(book: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM commentaries WHERE book = :book")
    suspend fun getCommentaryCountForBook(book: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commentaries: List<CatenaCommentaryEntity>)

    @Query("DELETE FROM commentaries")
    suspend fun clearAll()
}
