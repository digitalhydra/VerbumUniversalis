package com.verbum.universalis.core.di

import android.content.Context
import com.verbum.universalis.data.daos.InterlinearDao
import com.verbum.universalis.data.daos.LexiconDao
import com.verbum.universalis.data.daos.VerseDao
import com.verbum.universalis.data.repository.BibleRepository
import com.verbum.universalis.data.repository.CatenaRepository
import com.verbum.universalis.data.repository.CrossRefsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideBibleRepository(
        verseDao: VerseDao,
        interlinearDao: InterlinearDao,
        lexiconDao: LexiconDao,
        catenaRepository: CatenaRepository,
        crossRefsRepository: CrossRefsRepository,
        @ApplicationContext context: Context
    ): BibleRepository {
        return BibleRepository(
            verseDao,
            interlinearDao,
            lexiconDao,
            catenaRepository,
            crossRefsRepository,
            context
        )
    }
}
