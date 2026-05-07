package com.verbum.universalis.data.sync

import android.content.Context
import android.util.Log
import com.verbum.universalis.data.json.FileManager
import com.verbum.universalis.data.json.Highlight
import com.verbum.universalis.data.json.Note
import com.verbum.universalis.data.json.ReadingProgress
import com.verbum.universalis.data.ssh.SSHKeyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import javax.inject.Inject

enum class SyncStage {
    IDLE, PULLING, ADDING, COMMITTING, PUSHING, DONE, ERROR
}

class GitSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager,
    private val sshKeyManager: SSHKeyManager
) {
    companion object {
        private const val TAG = "GitSyncService"
        private const val SYNC_REPO_DIR = "sync_repo"
        private const val GITIGNORE_CONTENT = """
*.tmp
.idea/
build/
"""
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _syncProgress = MutableStateFlow(SyncStage.IDLE)
    val syncProgress: StateFlow<SyncStage> = _syncProgress.asStateFlow()

    private val repoDir = File(context.filesDir, SYNC_REPO_DIR)
    private val userDataDir = File(context.filesDir, "userdata")

    init {
        // Load saved repo config from settings
        try {
            val settings = fileManager.loadSettings()
            settings?.let {
                if (!it.gitRepoUrl.isNullOrEmpty()) {
                    _syncStatus.value = _syncStatus.value.copy(
                        repoUrl = it.gitRepoUrl,
                        authToken = it.gitToken,
                        isConfigured = true
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load settings", e)
        }
    }

    fun configureRepo(url: String, token: String? = null) {
        _syncStatus.value = _syncStatus.value.copy(
            repoUrl = url,
            authToken = token,
            isConfigured = true,
            errorMessage = null
        )
        // Generate SSH key if not exists
        if (!sshKeyManager.keyExists()) {
            sshKeyManager.generateKeyPair()
        }
        // Save to settings
        try {
            val settings = fileManager.loadSettings() ?: com.verbum.universalis.data.json.UserSettings()
            fileManager.saveSettings(
                settings.copy(
                    gitRepoUrl = url,
                    gitToken = token
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings", e)
        }
    }

    suspend fun syncNow(): Boolean = withContext(Dispatchers.IO) {
        val status = _syncStatus.value
        if (!status.isConfigured || status.repoUrl == null) {
            _syncStatus.value = status.copy(errorMessage = "Repo not configured")
            _syncProgress.value = SyncStage.ERROR
            return@withContext false
        }

        _syncStatus.value = status.copy(isSyncing = true, errorMessage = null)
        _syncProgress.value = SyncStage.PULLING

        try {
            // Handle new device scenario: backup local data if repo doesn't exist yet
            if (!repoDir.exists()) {
                backupLocalDataIfNeeded()
            }

            // Ensure repo exists locally
            val git = ensureLocalRepo(status.repoUrl!!)

            // Pull remote changes first
            pullChanges(git, status)

            // LWW Conflict Resolution: Merge remote (just pulled) with local using timestamps
            _syncProgress.value = SyncStage.ADDING
            mergeWithLWW(git)

            // Commit if there are changes
            _syncProgress.value = SyncStage.COMMITTING
            commitChanges(git)

            // Push to remote
            _syncProgress.value = SyncStage.PUSHING
            pushChanges(git, status)

            _syncProgress.value = SyncStage.DONE
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                errorMessage = null
            )
            Log.i(TAG, "Sync completed successfully")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            _syncProgress.value = SyncStage.ERROR
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                errorMessage = e.message ?: "Unknown error"
            )
            return@withContext false
        }
    }

    private fun backupLocalDataIfNeeded() {
        if (!userDataDir.exists()) return

        val jsonFiles = userDataDir.listFiles()?.filter { it.extension == "json" } ?: emptyList()
        if (jsonFiles.isEmpty()) return

        // Backup local data to userdata_backup_timestamp
        val backupDir = File(context.filesDir, "userdata_backup_${System.currentTimeMillis()}")
        Log.i(TAG, "New device detected. Backing up local data to ${backupDir.name}")
        userDataDir.copyRecursively(backupDir, overwrite = false)
        _syncStatus.value = _syncStatus.value.copy(backupCreated = backupDir.name)
        Log.i(TAG, "Backup completed")
    }

    private fun mergeWithLWW(git: Git) {
        // Files to merge: notes.json, highlights.json, progress.json
        val jsonFiles = listOf("notes.json", "highlights.json", "progress.json")

        jsonFiles.forEach { filename ->
            val localFile = File(userDataDir, filename)
            val remoteFile = File(repoDir, filename)

            // If remote file doesn't exist (first sync), just copy local
            if (!remoteFile.exists()) {
                if (localFile.exists()) {
                    localFile.copyTo(remoteFile, overwrite = true)
                }
                return@forEach
            }

            // If local file doesn't exist, keep remote
            if (!localFile.exists()) {
                // Remote file stays in repoDir
                return@forEach
            }

            try {
                // Read local and remote JSON arrays
                val localContent = localFile.readText()
                val remoteContent = remoteFile.readText()

                val mergedContent = when (filename) {
                    "notes.json" -> {
                        val localNotes: List<Note> = json.decodeFromString(localContent)
                        val remoteNotes: List<Note> = json.decodeFromString(remoteContent)
                        json.encodeToString(mergeObjectsLWW(localNotes, remoteNotes) { Pair(it.verseId, it.timestamp) })
                    }
                    "highlights.json" -> {
                        val localHighlights: List<Highlight> = json.decodeFromString(localContent)
                        val remoteHighlights: List<Highlight> = json.decodeFromString(remoteContent)
                        json.encodeToString(mergeObjectsLWW(localHighlights, remoteHighlights) { Pair(it.verseId, it.timestamp) })
                    }
                    "progress.json" -> {
                        val localProgress: List<ReadingProgress> = json.decodeFromString(localContent)
                        val remoteProgress: List<ReadingProgress> = json.decodeFromString(remoteContent)
                        json.encodeToString(mergeObjectsLWW(localProgress, remoteProgress) { Pair(it.planId, it.lastUpdated) })
                    }
                    else -> return@forEach
                }

                // Write merged content to repoDir (overwriting remote version)
                remoteFile.writeText(mergedContent)
                Log.i(TAG, "LWW merge completed for $filename")

            } catch (e: Exception) {
                Log.w(TAG, "Failed to merge $filename, using local version", e)
                // Fallback: use local version
                if (localFile.exists()) {
                    localFile.copyTo(remoteFile, overwrite = true)
                }
            }
        }

        // Add merged files to git staging
        git.add()
            .addFilepattern(".")
            .call()

        Log.i(TAG, "Added merged changes to staging")
    }

    private inline fun <T> mergeObjectsLWW(
        localList: List<T>,
        remoteList: List<T>,
        crossinline getIdAndTimestamp: (T) -> Pair<Any, Long>
    ): List<T> {
        val map = mutableMapOf<Any, T>()

        // Add remote objects first
        remoteList.forEach { obj ->
            val (id, _) = getIdAndTimestamp(obj)
            map[id] = obj
        }

        // Override with local objects if they have newer timestamp
        localList.forEach { obj ->
            val (id, timestamp) = getIdAndTimestamp(obj)
            val existing = map[id]
            if (existing == null) {
                map[id] = obj
            } else {
                val (_, existingTimestamp) = getIdAndTimestamp(existing)
                if (timestamp > existingTimestamp) {
                    map[id] = obj
                }
                // If remote is newer, keep remote (already in map)
            }
        }

        return map.values.toList()
    }

    private fun ensureLocalRepo(repoUrl: String): Git {
        val gitDir = File(repoDir, ".git")

        return if (gitDir.exists()) {
            // Open existing repo
            val repository = FileRepositoryBuilder()
                .setGitDir(gitDir)
                .build()
            Git(repository)
        } else {
            // Clone remote repo with SSH key
            repoDir.mkdirs()
            val cloneCommand = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(repoDir)

            // Use SSH key if available
            val publicKey = sshKeyManager.getPublicKey()
            if (publicKey != null) {
                // Configure JGit to use SSH
                // This requires setting up SshSessionFactory with the private key
                // For now, just clone with default SSH config
            }

            cloneCommand.call()
        }
    }

    private fun pullChanges(git: Git, status: SyncStatus) {
        try {
            val pullCommand = git.pull()
                .setRemote("origin")

            val token = status.authToken
            if (token != null) {
                pullCommand.setCredentialsProvider(
                    org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider("token", token)
                )
            }

            // Try to set remote branch, but don't fail if it doesn't exist
            try {
                val branch = git.repository.branch
                pullCommand.setRemoteBranchName("origin/$branch")
            } catch (e: Exception) {
                Log.w(TAG, "Could not determine remote branch: ${e.message}")
            }

            pullCommand.call()
            Log.i(TAG, "Pulled remote changes")
        } catch (e: Exception) {
            // If pull fails (e.g., empty repo), just log and continue
            Log.w(TAG, "Pull failed (may be first sync): ${e.message}")
        }
    }

    private fun commitChanges(git: Git) {
        val status = git.status().call()
        if (status.hasUncommittedChanges()) {
            git.commit()
                .setMessage("Sync: ${System.currentTimeMillis()}")
                .call()
            Log.i(TAG, "Committed changes")
        } else {
            Log.i(TAG, "No changes to commit")
        }
    }

    private fun pushChanges(git: Git, status: SyncStatus) {
        val pushCommand = git.push()
            .setRemote("origin")

        val token = status.authToken
        if (token != null) {
            pushCommand.setCredentialsProvider(
                org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider("token", token)
            )
        }

        pushCommand.call()
        Log.i(TAG, "Pushed to remote")
    }

    fun getRepoInfo(): String {
        return try {
            val gitDir = File(repoDir, ".git")
            if (gitDir.exists()) {
                val repository = FileRepositoryBuilder()
                    .setGitDir(gitDir)
                    .build()
                val git = Git(repository)
                val remoteUrl = git.remoteList().call().find { it.name == "origin" }?.URIs?.firstOrNull()?.toString()
                "Repo: $remoteUrl\nBranch: ${repository.branch}"
            } else {
                "No local repo"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

data class SyncStatus(
    val isConfigured: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val errorMessage: String? = null,
    val repoUrl: String? = null,
    val authToken: String? = null,
    val backupCreated: String? = null
)
