package com.verbum.universalis.core.di

import android.content.Context
import com.verbum.universalis.data.db.CatenaDatabase
import com.verbum.universalis.data.db.CrossRefsDatabase
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
object AppModule {
    
    @Singleton
    @Provides
    fun provideCatenaDatabase(@ApplicationContext context: Context): CatenaDatabase {
        return CatenaDatabase.getDatabase(context)
    }

    @Singleton
    @Provides
    fun provideCrossRefsDatabase(@ApplicationContext context: Context): CrossRefsDatabase {
        return CrossRefsDatabase.getDatabase(context)
    }

    @Singleton
    @Provides
    fun provideCatenaRepository(
        catenaDb: CatenaDatabase,
        @ApplicationContext context: Context
    ): CatenaRepository {
        return CatenaRepository(catenaDb.catenaDao(), context)
    }

    @Singleton
    @Provides
    fun provideCrossRefsRepository(
        crossRefsDb: CrossRefsDatabase,
        @ApplicationContext context: Context
    ): CrossRefsRepository {
        return CrossRefsRepository(crossRefsDb.crossRefsDao(), context)
    }
}
