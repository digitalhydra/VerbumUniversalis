package com.verbum.universalis.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.verbum.universalis.data.json.ReadingPlanViewModel
import com.verbum.universalis.data.sync.GitSyncViewModel
import com.verbum.universalis.data.sync.SyncStage
import com.verbum.universalis.ui.reader.Passage

@Composable 
fun DashboardScreen( 
    viewModel: ReadingPlanViewModel = hiltViewModel(), 
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    gitSyncViewModel: GitSyncViewModel = hiltViewModel(),
    onNavigateToReading: (bookId: Int, chapter: Int) -> Unit 
) { 
    val currentPlan by viewModel.currentPlan.collectAsState(initial = null) 
    val progress by viewModel.progressPercentage.collectAsState(initial = 0f) 
    val currentDayReadings by viewModel.currentDayReadings.collectAsState(initial = emptyList())
    val syncStatus by gitSyncViewModel.syncStatus.collectAsState()
    val syncProgress by gitSyncViewModel.syncProgress.collectAsState()
    val todayLiturgical by dashboardViewModel.todayLiturgical.collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sync Status
        if (!syncStatus.isConfigured) {
            Text("Warning: Data is local-only. Configure Git Sync in Settings to backup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            // Show progress stage
            val statusText = when (syncProgress) {
                SyncStage.IDLE -> if (syncStatus.isSyncing) "Starting sync..." else "Idle"
                SyncStage.PULLING -> "Pulling remote changes..."
                SyncStage.ADDING -> "Adding local changes..."
                SyncStage.COMMITTING -> "Committing changes..."
                SyncStage.PUSHING -> "Pushing to remote..."
                SyncStage.DONE -> "Sync complete"
                SyncStage.ERROR -> "Sync error"
            }
            Text("Sync Status: $statusText",
                style = MaterialTheme.typography.bodySmall
            )
            if (syncStatus.isSyncing) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            syncStatus.lastSyncTime?.let {
                Text("Last sync: $it", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { gitSyncViewModel.triggerSync() },
                enabled = !syncStatus.isSyncing
            ) {
                Text("Sync Now")
            }
        }

        Text("Dashboard", style = MaterialTheme.typography.headlineLarge) 

        // Today's Liturgical Reading
        todayLiturgical?.let { entry ->
            Text("Today: ${entry.celebration}", style = MaterialTheme.typography.titleMedium)
            entry.readings.forEach { ref ->
                Text("• ${ref.book} ${ref.chapter}", 
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { 
                        val bookId = Passage.BOOK_NAME_TO_ID[ref.book] ?: 1
                        onNavigateToReading(bookId, ref.chapter)
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp)) 

        Text("Current Plan: ${currentPlan?.title ?: \"None\"}", style = MaterialTheme.typography.titleMedium) 
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Today's Readings:", style = MaterialTheme.typography.titleMedium)
        currentDayReadings.forEach { reading ->
            Text("- ${reading.book} ${reading.chapter}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.clickable {
                val bookId = Passage.BOOK_NAME_TO_ID[reading.book] ?: 1
                onNavigateToReading(bookId, reading.chapter)
            })
        }
    }
}
