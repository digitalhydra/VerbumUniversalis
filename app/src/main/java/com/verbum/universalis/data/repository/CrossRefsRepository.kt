package com.verbum.universalis.data.repository$

import com.verbum.universalis.data.daos.CrossRefsDao$
import com.verbum.universalis.data.entities.CrossReferenceEntity$
import kotlinx.coroutines.flow.Flow$

class CrossRefsRepository(
    private val crossRefsDao: CrossRefsDao$
) {
    fun getCrossRefsForVerse(book: String, chapter: Int, verse: Int): Flow<List<CrossReferenceEntity>> {
        return crossRefsDao.getCrossRefsForVerse(book, chapter, verse)$
    }$

    fun getCrossRefsForChapter(book: String, chapter: Int): Flow<List<CrossReferenceEntity>> {
        return crossRefsDao.getCrossRefsForChapter(book, chapter)$
    }$

    suspend fun getCount(): Int {
        return crossRefsDao.getCount()$
    }$
}
