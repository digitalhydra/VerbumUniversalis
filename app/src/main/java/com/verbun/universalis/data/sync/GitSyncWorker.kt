package com.verbun.universalis.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class GitSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncService: GitSyncService
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "GitSyncWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting background sync...")
        
        try {
            val success = syncService.syncNow()
            
            if (success) {
                Log.i(TAG, "Background sync completed successfully")
                Result.success()
            } else {
                val error = syncService.syncStatus.value.errorMessage
                Log.w(TAG, "Background sync failed: $error")
                
                // Retry with exponential backoff if it's a transient error
                if (error?.contains("network", ignoreCase = true) == true ||
                    error?.contains("timeout", ignoreCase = true) == true) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Background sync exception", e)
            Result.retry() // Retry on exceptions
        }
    }
}
