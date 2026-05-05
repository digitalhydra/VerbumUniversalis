package com.verbum.universalis.core.di

import android.content.Context
import com.verbum.universalis.data.db.AppDatabase
import com.verbum.universalis.data.db.CatenaDatabase
import com.verbum.universalis.data.github.GitHubApiService
import com.verbum.universalis.data.json.FileManager
import com.verbum.universalis.data.oauth.OAuthManager
import com.verbum.universalis.data.repository.BibleRepository
import com.verbum.universalis.data.repository.CatenaRepository
import com.verbum.universalis.data.ssh.SSHKeyManager
import com.verbum.universalis.data.sync.GitSyncService
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Singleton
    @Provides
    fun provideCatenaDatabase(@ApplicationContext context: Context): CatenaDatabase {
        return CatenaDatabase.getDatabase(context)
    }

    @Singleton
    @Provides
    fun provideCatenaRepository(catenaDb: CatenaDatabase, @ApplicationContext context: Context): CatenaRepository {
        return CatenaRepository(catenaDb.catenaDao(), context)
    }

    @Singleton
    @Provides
    fun provideFileManager(@ApplicationContext context: Context): FileManager {
        return FileManager(context)
    }

    @Singleton
    @Provides
    fun provideBibleRepository(db: AppDatabase, catenaRepository: CatenaRepository): BibleRepository {
        return com.verbum.universalis.data.repository.BibleRepository(
            db.verseDao(),
            db.interlinearDao(),
            db.lexiconDao(),
            catenaRepository
        )
    }

    @Singleton
    @Provides
    fun provideSSHKeyManager(@ApplicationContext context: Context): SSHKeyManager {
        return SSHKeyManager(context)
    }

    @Singleton
    @Provides
    fun provideOAuthManager(): OAuthManager {
        return OAuthManager()
    }

    @Singleton
    @Provides
    fun provideGitHubApiService(): GitHubApiService {
        return GitHubApiService()
    }

    @Singleton
    @Provides
    fun provideGitSyncService(
        @ApplicationContext context: Context,
        fileManager: FileManager,
        sshKeyManager: SSHKeyManager
    ): GitSyncService {
        return GitSyncService(context, fileManager, sshKeyManager)
    }
}
