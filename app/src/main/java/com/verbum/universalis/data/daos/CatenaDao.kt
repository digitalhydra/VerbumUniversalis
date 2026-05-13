package com.verbum.universalis.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verbum.universalis.data.entities.CatenaCommentaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatenaDao {

    @Query("SELECT * FROM commentaries WHERE book IN (:books) ORDER BY author_normalized, chapter, verse_start")
    fun getCommentariesByBook(books: List<String>): Flow<List<CatenaCommentaryEntity>>

    @Query("SELECT * FROM commentaries WHERE book IN (:books) AND chapter = :chapter ORDER BY author_normalized, verse_start")
    fun getCommentariesForChapter(books: List<String>, chapter: Int): Flow<List<CatenaCommentaryEntity>>

    @Query("SELECT * FROM commentaries WHERE book IN (:books) AND chapter = :chapter AND verse_start <= :verseNumber AND verse_end >= :verseNumber ORDER BY author_normalized")
    fun getCommentariesForVerse(books: List<String>, chapter: Int, verseNumber: Int): Flow<List<CatenaCommentaryEntity>>

    @Query("SELECT DISTINCT author_normalized FROM commentaries WHERE book IN (:books) ORDER BY author_normalized")
    fun getAuthorsForBook(books: List<String>): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM commentaries WHERE book IN (:books)")
    suspend fun getCommentaryCountForBook(books: List<String>): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commentaries: List<CatenaCommentaryEntity>)

    @Query("DELETE FROM commentaries")
    suspend fun clearAll()
}