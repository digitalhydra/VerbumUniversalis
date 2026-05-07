package com.verbum.universalis.ui.dashboard

import androidx.compose.foundation.clickable
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
import com.verbum.universalis.data.json.ReadingPlan
import com.verbum.universalis.data.json.ReadingPlanViewModel
import com.verbum.universalis.data.sync.GitSyncViewModel
import com.verbum.universalis.data.sync.SyncStage
import com.verbum.universalis.ui.reader.Passage
import com.verbum.universalis.ui.navigation.MassReadings
import com.verbum.universalis.ui.navigation.PlanReadings

@Composable
fun DashboardScreen(
    viewModel: ReadingPlanViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    gitSyncViewModel: GitSyncViewModel = hiltViewModel(),
    onNavigateToReading: (bookId: Int, chapter: Int) -> Unit = { _, _ -> },
    onNavigateToMassReading: (bookId: Int, chapter: Int, allReadings: MassReadings, currentIndex: Int) -> Unit = { _, _, _, _ -> },
    onNavigateToPlanReading: (bookId: Int, chapter: Int, allDays: List<List<String>>, currentDayIndex: Int) -> Unit = { _, _, _, _ -> }
) {
    val currentPlan by viewModel.currentPlan.collectAsState(initial = null)
    val progress by viewModel.progressPercentage.collectAsState(initial = 0f)
    val currentDayReadings by viewModel.currentDayReadings.collectAsState(initial = emptyList())
    val currentDayIndex by viewModel.currentDayIndex.collectAsState(initial = 0)
    val syncStatus by gitSyncViewModel.syncStatus.collectAsState()
    val syncProgress by gitSyncViewModel.syncProgress.collectAsState()
    val todayLiturgical by dashboardViewModel.todayLiturgical.collectAsState(initial = null)
    val todayMassReadings by dashboardViewModel.todayMassReadings.collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sync Status
        if (!syncStatus.isConfigured) {
            Text(
                text = "Warning: Data is local-only. Configure Git Sync in Settings to backup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            val statusText = when (syncProgress) {
                SyncStage.IDLE -> if (syncStatus.isSyncing) "Starting sync..." else "Idle"
                SyncStage.PULLING -> "Pulling remote changes..."
                SyncStage.ADDING -> "Adding local changes..."
                SyncStage.COMMITTING -> "Committing changes..."
                SyncStage.PUSHING -> "Pushing to remote..."
                SyncStage.DONE -> "Sync complete"
                SyncStage.ERROR -> "Sync error"
            }
            Text(text = "Sync Status: $statusText", style = MaterialTheme.typography.bodySmall)
            if (syncStatus.isSyncing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            syncStatus.lastSyncTime?.let {
                Text("Last sync: $it", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { gitSyncViewModel.triggerSync() }, enabled = !syncStatus.isSyncing) {
                Text("Sync Now")
            }
        }

        Text("Dashboard", style = MaterialTheme.typography.headlineLarge)

        // Today's Liturgical Reading
        todayLiturgical?.let { entry ->
            Text("Today: ${entry.celebration?.name ?: "No celebration"}", style = MaterialTheme.typography.titleMedium)
            if (entry.readings.isNotEmpty()) {
                entry.readings.forEach { ref ->
                    val parts = ref.reference.split(".", ":", " ")
                    val book = if (parts.isNotEmpty()) parts[0] else ""
                    val chapter = if (parts.size >= 2) parts[1].toIntOrNull() ?: 1 else 1
                    
                    Text(
                        text = "• $book $chapter",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable {
                            val bookId = Passage.BOOK_NAME_TO_ID[book] ?: return@clickable
                            onNavigateToReading(bookId, chapter)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Today's Mass Readings (with flow navigation)
        todayMassReadings?.let { entry ->
            Text("Mass Readings:", style = MaterialTheme.typography.titleMedium)
            val readings = entry.readings.map { it.type to it.reference }
            entry.readings.forEachIndexed { idx, ref ->
                val typeLabel = when (ref.type) {
                    "firstReading" -> "1st Reading"
                    "psalm" -> "Psalm"
                    "gospel" -> "Gospel"
                    else -> ref.type ?: "Reading"
                }
                Text(
                    text = "• $typeLabel: ${ref.reference}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        val parts = ref.reference.split(":")
                        if (parts.size >= 2) {
                            val bookPart = parts[0].trim()
                            val chapterVerse = parts[1].split("-")[0].split(",")[0].trim()
                            val bookId = Passage.BOOK_NAME_TO_ID[bookPart] ?: return@clickable
                            val chapter = chapterVerse.toIntOrNull() ?: return@clickable
                            onNavigateToMassReading(bookId, chapter, readings, idx)
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bible in a Year Plan
        Text("Current Plan: ${currentPlan?.title ?: "None"}", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Today's Readings:", style = MaterialTheme.typography.titleMedium)
        
        // Get all plan readings for navigation flow
        val allPlanDays = currentPlan?.days?.map { day -> 
            day.readings.mapNotNull { reading ->
                // Parse reading reference "Genesis 1:1" or just "GEN.1"
                val ref = when (reading) {
                    is String -> reading
                    else -> null
                }
                ref
            }
        } ?: emptyList()
        
        currentDayReadings.forEachIndexed { idx, reading ->
            Text(
                text = "- ${reading.book} ${reading.chapter}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable {
                    val bookId = Passage.BOOK_NAME_TO_ID[reading.book] ?: return@clickable
                    onNavigateToPlanReading(bookId, reading.chapter, allPlanDays, currentDayIndex)
                }
            )
        }
    }
}
