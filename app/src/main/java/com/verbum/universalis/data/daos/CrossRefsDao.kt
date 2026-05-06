package com.verbum.universalis.data.daos

import androidx.room.*
import com.verbum.universalis.data.entities.CrossReferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CrossRefsDao {

    @Query("SELECT * FROM cross_references WHERE from_book = :book AND from_chapter = :chapter AND from_verse_start <= :verse AND from_verse_end >= :verse ORDER BY votes DESC")
    fun getCrossRefsForVerse(book: String, chapter: Int, verse: Int): Flow<List<CrossReferenceEntity>>

    @Query("SELECT * FROM cross_references WHERE from_book = :book AND from_chapter = :chapter ORDER BY from_verse_start, votes DESC")
    fun getCrossRefsForChapter(book: String, chapter: Int): Flow<List<CrossReferenceEntity>>

    @Query("SELECT COUNT(*) FROM cross_references")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(refs: List<CrossReferenceEntity>)

    @Query("DELETE FROM cross_references")
    suspend fun clearAll()
}
