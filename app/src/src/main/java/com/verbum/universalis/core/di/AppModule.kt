package com.verbum.universalis.core.di

import dagger.Module
import dagger.InstallIn
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Base singleton providers for shared utils, file managers, etc.
}
