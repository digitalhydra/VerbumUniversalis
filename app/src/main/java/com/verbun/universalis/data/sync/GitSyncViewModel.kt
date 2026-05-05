package com.verbun.universalis.data.sync

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@HiltViewModel
class GitSyncViewModel @Inject constructor(
    private val syncService: GitSyncService,
    @ApplicationContext private val context: android.content.Context
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatus> = syncService.syncStatus

    fun configureRepo(url: String, token: String? = null) {
        viewModelScope.launch {
            syncService.configureRepo(url, token)
            // Schedule background sync after configuring
            scheduleBackgroundSync()
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            syncService.syncNow()
        }
    }

    fun scheduleBackgroundSync(intervalHours: Long = 6) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Only sync on unmetered networks
            .setRequiresBatteryNotLow(true)
            .build()

        val syncWork = PeriodicWorkRequestBuilder<GitSyncWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    "repo_url" to syncStatus.value.repoUrl,
                    "auth_token" to syncStatus.value.authToken
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "git_sync_work",
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            syncWork
        )
    }

    fun cancelBackgroundSync() {
        WorkManager.getInstance(context).cancelUniqueWork("git_sync_work")
    }
}
