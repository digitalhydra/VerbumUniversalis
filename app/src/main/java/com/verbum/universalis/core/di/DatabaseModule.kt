package com.verbum.universalis.core.di

import android.content.Context
import com.verbum.universalis.data.db.AppDatabase
import com.verbum.universalis.data.daos.VerseDao
import com.verbum.universalis.data.daos.InterlinearDao
import com.verbum.universalis.data.daos.LexiconDao
import dagger.Module
import dagger.hilt.InstallIn
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideVerseDao(db: AppDatabase): VerseDao = db.verseDao()

    @Provides
    fun provideInterlinearDao(db: AppDatabase): InterlinearDao = db.interlinearDao()

    @Provides
    fun provideLexiconDao(db: AppDatabase): LexiconDao = db.lexiconDao()
}
