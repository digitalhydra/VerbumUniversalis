package com.verbum.universalis.ui.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.verbum.universalis.core.theme.VerbumTheme
import com.verbum.universalis.data.json.DayProgress
import com.verbum.universalis.data.json.PlanDay
import com.verbum.universalis.data.json.PlanSummary
import com.verbum.universalis.data.json.ReadingPlan
import com.verbum.universalis.data.json.ReadingPlanViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ReadingPlansScreen(
    viewModel: ReadingPlanViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    VerbumTheme {
        val plansIndex by viewModel.plansIndex.collectAsState()
        val currentPlan by viewModel.currentPlan.collectAsState(initial = null)
        val currentDay by viewModel.currentDay.collectAsState(initial = null)
        val progressMap by viewModel.progressMap.collectAsState()
        
        var selectedPlan by remember { mutableStateOf<PlanSummary?>(null) }
        
        Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
            if (selectedPlan == null) {
                PlanListScreen(
                    plans = plansIndex?.plans ?: emptyList(),
                    onPlanSelected = { plan ->
                        selectedPlan = plan
                        viewModel.selectPlan(plan.file)
                    }
                )
            } else {
                DayListScreen(
                    plan = currentPlan,
                    progressMap = progressMap,
                    onDaySelected = { dayIndex ->
                        viewModel.selectDay(dayIndex)
                    },
                    onBack = { selectedPlan = null },
                    onToggleComplete = { dayIndex, completed ->
                        viewModel.markDayCompleted(currentPlan?.planId ?: "", dayIndex, completed)
                    }
                )
            }
        }
    }
}

@Composable
fun PlanListScreen(
    plans: List<PlanSummary>,
    onPlanSelected: (PlanSummary) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Reading Plans",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn {
            itemsIndexed(plans) { _, plan ->
                PlanCard(
                    plan = plan,
                    onClick = { onPlanSelected(plan) }
                )
            }
        }
    }
}

@Composable
fun PlanCard(
    plan: PlanSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = plan.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = plan.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${plan.totalDays} days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DayListScreen(
    plan: ReadingPlan?,
    progressMap: Map<String, Map<Int, DayProgress>>,
    onDaySelected: (Int) -> Unit,
    onBack: () -> Unit,
    onToggleComplete: (Int, Boolean) -> Unit
) {
    if (plan == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    val planProgress = progressMap[plan.planId] ?: emptyMap()
    val completedCount = planProgress.count { it.value.completed }
    val progress = if (plan.totalDays > 0) completedCount.toFloat() / plan.totalDays.toFloat() else 0f
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back")
            }
        }
        
        Text(
            text = plan.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )
        
        Text(
            text = "$completedCount / ${plan.totalDays} days completed",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn {
            itemsIndexed(plan.days) { index, day ->
                DayItem(
                    day = day,
                    dayIndex = index,
                    isCompleted = planProgress[index]?.completed ?: false,
                    onClick = { onDaySelected(index) },
                    onToggleComplete = { completed ->
                        onToggleComplete(index, completed)
                    }
                )
            }
        }
    }
}

@Composable
fun DayItem(
    day: PlanDay,
    dayIndex: Int,
    isCompleted: Boolean,
    onClick: () -> Unit,
    onToggleComplete: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggleComplete(it) }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Day ${day.day}" + (day.date?.let { " ($it)" } ?: ""),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                if (day.era != null) {
                    Text(
                        text = day.era,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (day.season != null) {
                    Text(
                        text = day.season + (day.subSeason?.let { " - $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "${day.readingCount} readings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
