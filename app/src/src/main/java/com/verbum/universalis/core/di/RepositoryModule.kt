package com.verbum.universalis.core.di

import com.verbum.universalis.data.repository.BibleRepositoryImpl
import com.verbum.universalis.domain.repository.BibleRepository
import dagger.Binds
import dagger.Module
import dagger.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindBibleRepository(impl: BibleRepositoryImpl): BibleRepository
}
