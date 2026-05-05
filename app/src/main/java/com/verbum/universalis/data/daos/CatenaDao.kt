package com.verbum.universalis.data.daos

import androidx.room.*
import com.verbum.universalis.data.entities.CatenaCommentaryEntity
import com.verbum.universalis.data.entities.FatherMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatenaDao {
    
    @Query("SELECT * FROM commentary WHERE book = :book ORDER BY location_start, ts")
    fun getCommentariesByBook(book: String): Flow<List<CatenaCommentaryEntity>>
    
    @Query("SELECT * FROM commentary WHERE location_start <= :encodedVerse AND location_end >= :encodedVerse ORDER BY ts")
    fun getCommentariesByVerse(encodedVerse: Int): Flow<List<CatenaCommentaryEntity>>
    
    @Query("SELECT * FROM commentary WHERE book = :book AND location_start <= :endVerse AND location_end >= :startVerse ORDER BY location_start, ts")
    fun getCommentariesForChapter(book: String, startVerse: Int, endVerse: Int): Flow<List<CatenaCommentaryEntity>>
    
    @Query("SELECT * FROM commentary WHERE book = :book AND location_start = :encodedVerse ORDER BY ts")
    fun getCommentariesForExactVerse(book: String, encodedVerse: Int): Flow<List<CatenaCommentaryEntity>>
    
    @Query("SELECT DISTINCT father_name FROM commentary WHERE book = :book ORDER BY father_name")
    fun getFathersForBook(book: String): Flow<List<String>>
    
    @Query("SELECT COUNT(*) FROM commentary WHERE book = :book")
    fun getCommentaryCountForBook(book: String): Flow<Int>
    
    @Query("SELECT * FROM father_meta WHERE name = :name LIMIT 1")
    fun getFatherMeta(name: String): Flow<FatherMetaEntity?>
    
    @Query("SELECT * FROM father_meta ORDER BY name")
    fun getAllFatherMeta(): Flow<List<FatherMetaEntity>>
}
