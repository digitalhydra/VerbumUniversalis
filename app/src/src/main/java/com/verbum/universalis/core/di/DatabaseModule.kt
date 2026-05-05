package com.verbum.universalis.core.di

import android.content.Context
import androidx.room.Room
import com.verbum.universalis.data.local.VerbumDatabase
import dagger.Module
import dagger.InstallIn
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VerbumDatabase {
        return Room.databaseBuilder(
            context,
            VerbumDatabase::class.java,
            "verbum.db"
        ).createFromAsset("verbum_seed.db")
         .build()
    }

    @Provides
    fun provideVerseDao(db: VerbumDatabase) = db.verseDao()

    @Provides
    fun provideInterlinearDao(db: VerbumDatabase) = db.interlinearDao()

    @Provides
    fun provideLexiconDao(db: VerbumDatabase) = db.lexiconDao()
}
