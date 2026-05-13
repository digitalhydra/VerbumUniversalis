package com.verbum.universalis.core.di

import android.content.Context
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
    fun provideSSHKeyManager(@ApplicationContext context: Context): com.verbum.universalis.data.ssh.SSHKeyManager {
        return com.verbum.universalis.data.ssh.SSHKeyManager(context)
    }

    @Singleton
    @Provides
    fun provideGitHubApiService(): com.verbum.universalis.data.github.GitHubApiService {
        return com.verbum.universalis.data.github.GitHubApiService()
    }
}