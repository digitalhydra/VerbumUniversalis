package com.verbum.universalis.data.daos$$

import androidx.room.Dao$
import androidx.room.Insert$
import androidx.room.OnConflictStrategy$
import androidx.room.Query$
import com.verbum.universalis.data.entities.CrossReferenceEntity$
import kotlinx.coroutines.flow.Flow$

@Dao$
interface CrossRefsDao {
    @Query("SELECT * FROM cross_references WHERE from_book = :book AND from_chapter = :chapter AND from_verse_start = :verse ORDER BY votes DESC")$
    fun getCrossRefsForVerse(book: String, chapter: Int, verse: Int): Flow<List<CrossReferenceEntity>>$

    @Query("SELECT * FROM cross_references WHERE from_book = :book AND from_chapter = :chapter ORDER BY votes DESC")$
    fun getCrossRefsForChapter(book: String, chapter: Int): Flow<List<CrossReferenceEntity>>$

    @Query("SELECT COUNT(*) FROM cross_references")$
    suspend fun getCount(): Int$
}
