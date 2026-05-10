package com.verbum.universalis.ui.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.verbum.universalis.core.theme.Inter
import com.verbum.universalis.core.theme.VerbumBlue
import com.verbum.universalis.core.theme.VerbumTheme
import com.verbum.universalis.data.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingPlansScreen(
    viewModel: ReadingPlanViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    VerbumTheme {
        val plansIndex by viewModel.plansIndex.collectAsState()
        val currentPlan by viewModel.currentPlan.collectAsState(initial = null)
        val currentDayIdx by viewModel.currentDayIndex.collectAsState()
        val progressMap by viewModel.progressMap.collectAsState()
        
        var selectedPlan by remember { mutableStateOf<PlanSummary?>(null) }
        
        val filteredPlans = remember(plansIndex) {
            plansIndex?.plans?.filter { it.type != "daily_mass" } ?: emptyList()
        }
        
        // Auto-select the first plan if only one exists and we are coming from Dashboard
        LaunchedEffect(filteredPlans) {
            if (filteredPlans.size == 1 && selectedPlan == null) {
                selectedPlan = filteredPlans[0]
                viewModel.selectPlan(selectedPlan!!.file)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = selectedPlan?.title ?: "Reading Plans", 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Inter,
                                color = Color.Black
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            if (selectedPlan == null || filteredPlans.size == 1) onNavigateBack() 
                            else selectedPlan = null 
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Info dialog */ }) {
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.Black)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color.White
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color.White)) {
                if (selectedPlan == null) {
                    PlanListScreen(
                        plans = filteredPlans,
                        onPlanSelected = { plan ->
                            selectedPlan = plan
                            viewModel.selectPlan(plan.file)
                        }
                    )
                } else {
                    PlanDetailScreen(
                        plan = currentPlan,
                        currentDayIdx = currentDayIdx,
                        progressMap = progressMap,
                        onDaySelected = { viewModel.selectDay(it) },
                        onToggleReading = { readingIdx ->
                            viewModel.toggleReadingCompleted(currentPlan?.planId ?: "", currentDayIdx, readingIdx)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlanListScreen(
    plans: List<PlanSummary>,
    onPlanSelected: (PlanSummary) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(plans) { _, plan ->
            PlanCard(plan = plan, onClick = { onPlanSelected(plan) })
        }
    }
}

@Composable
fun PlanCard(plan: PlanSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = plan.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = Inter, color = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = plan.description, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray, fontFamily = Inter)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = VerbumBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "${plan.totalDays} days", style = MaterialTheme.typography.labelLarge, color = VerbumBlue, fontFamily = Inter)
            }
        }
    }
}

@Composable
fun PlanDetailScreen(
    plan: ReadingPlan?,
    currentDayIdx: Int,
    progressMap: Map<String, Map<Int, DayProgress>>,
    onDaySelected: (Int) -> Unit,
    onToggleReading: (Int) -> Unit
) {
    if (plan == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VerbumBlue)
        }
        return
    }

    val listState = rememberLazyListState()

    // Scroll to current day
    LaunchedEffect(currentDayIdx) {
        listState.animateScrollToItem(currentDayIdx)
    }

    val planProgress = progressMap[plan.planId] ?: emptyMap()
    val dayData = plan.days[currentDayIdx]
    val dayProgress = planProgress[currentDayIdx] ?: DayProgress()
    
    val completedCount = dayProgress.completedReadings.size
    val totalCount = dayData.readings.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Horizontal Day Selector
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(plan.days) { index, day ->
                val isSelected = index == currentDayIdx
                val isDayDone = planProgress[index]?.completed ?: false
                
                DaySelectorItem(
                    day = day,
                    isSelected = isSelected,
                    isCompleted = isDayDone,
                    onClick = { onDaySelected(index) }
                )
            }
        }

        // Active Day Progress Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dayData.date ?: "Day ${dayData.day}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Inter,
                        color = Color.Black
                    )
                    Text(
                        text = "Day ${dayData.day}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        fontFamily = Inter
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$totalCount Readings",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray,
                        fontFamily = Inter
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.width(60.dp).height(6.6.dp).clip(CircleShape),
                            color = VerbumBlue,
                            trackColor = Color(0xFFEEEEEE)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Inter,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "My readings",
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = Inter,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Readings List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(dayData.readings) { index, reading ->
                val isCompleted = dayProgress.completedReadings.contains(index)
                
                // Use parseReading to get reference
                // For simplicity, let's assume we can get a string
                val ref = try {
                    if (reading is kotlinx.serialization.json.JsonPrimitive) reading.content 
                    else reading.toString()
                } catch (e: Exception) { "Reading ${index + 1}" }

                ReadingCheckItem(
                    index = index + 1,
                    reference = ref,
                    isCompleted = isCompleted,
                    onToggle = { onToggleReading(index) }
                )
            }
        }
    }
}

@Composable
fun DaySelectorItem(
    day: PlanDay,
    isSelected: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(100.dp)
            .height(70.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color.White else Color(0xFFF8F9FA),
        border = if (isSelected) BorderStroke(2.dp, Color.Black) else BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.date ?: "Day ${day.day}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = Inter,
                color = if (isSelected) Color.Black else Color.DarkGray,
                maxLines = 1
            )
            Text(
                text = "Day ${day.day}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.Gray else Color.LightGray,
                fontFamily = Inter
            )
            
            if (isCompleted) {
                Spacer(modifier = Modifier.height(2.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = VerbumBlue,
                    modifier = Modifier.size(14.dp).align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun ReadingCheckItem(
    index: Int,
    reference: String,
    isCompleted: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.width(24.dp),
                fontFamily = Inter
            )
            
            Text(
                text = reference,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                modifier = Modifier.weight(1f),
                fontFamily = Inter
            )

            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Complete",
                    tint = if (isCompleted) VerbumBlue else Color.LightGray
                )
            }
        }
    }
}
