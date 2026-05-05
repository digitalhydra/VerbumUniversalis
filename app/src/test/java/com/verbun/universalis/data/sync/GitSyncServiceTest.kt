package com.verbun.universalis.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.verbun.universalis.data.json.FileManager
import kotlinx.coroutines.test.runTest
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class GitSyncServiceTest {
    private lateinit var syncService: GitSyncService
    private lateinit var fileManager: FileManager
    private lateinit var testRepoDir: File
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fileManager = FileManager(context)
        syncService = GitSyncService(context, fileManager)
        testRepoDir = File(context.filesDir, "test_repo")
        testRepoDir.mkdirs()
    }
    
    @After
    fun tearDown() {
        testRepoDir.deleteRecursively()
        val syncRepoDir = File(context.filesDir, GitSyncService.SYNC_REPO_DIR)
        syncRepoDir.deleteRecursively()
    }
    
    @Test
    fun testInitialSyncStatus() {
        val status = syncService.syncStatus.value
        assertFalse(status.isConfigured)
        assertFalse(status.isSyncing)
        assertNull(status.repoUrl)
    }
    
    @Test
    fun testConfigureRepo() = runTest {
        syncService.configureRepo("https://github.com/test/repo.git", "fake-token")
        val status = syncService.syncStatus.value
        assertTrue(status.isConfigured)
        assertEquals("https://github.com/test/repo.git", status.repoUrl)
    }
    
    @Test
    fun testSyncNowCreatesLocalRepo() = runTest {
        // Configure repo
        syncService.configureRepo("https://github.com/test/repo.git", "fake-token")
        
        // Create a test repo with initial commit
        createTestRepoWithCommit()
        
        // Try to sync (will fail since we don't have real remote, but should create local repo)
        syncService.syncNow()
        
        // Check that local repo was created
        val syncRepoDir = File(context.filesDir, GitSyncService.SYNC_REPO_DIR)
        val gitDir = File(syncRepoDir, ".git")
        assertTrue("Local repo should be created", gitDir.exists())
    }
    
    @Test
    fun testGetRepoInfo() {
        val info = syncService.getRepoInfo()
        assertTrue(info.contains("No local repo") || info.contains("Repo:"))
    }
    
    private fun createTestRepoWithCommit() {
        // Create a bare repository to act as "remote"
        val bareRepoDir = File(testRepoDir, "bare.git")
        Git.init()
            .setDirectory(bareRepoDir)
            .setBare(true)
            .call()
            
        // Configure sync service to use this as remote
        syncService.configureRepo(bareRepoDir.absolutePath, null)
    }
}
