package com.verbum.universalis.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.verbum.universalis.core.theme.VerbumBlue
import com.verbum.universalis.core.theme.VerbumBlueLight
import com.verbum.universalis.data.json.ReadingPlanViewModel
import com.verbum.universalis.data.json.ReadingInfo
import com.verbum.universalis.ui.navigation.MassReadings
import com.verbum.universalis.ui.navigation.PlanReadings
import com.verbum.universalis.ui.reader.Passage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ReadingPlanViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToReading: (bookId: Int, chapter: Int) -> Unit = { _, _ -> },
    onNavigateToMassReading: (bookId: Int, chapter: Int, verse: Int?, allReadings: MassReadings, currentIndex: Int) -> Unit = { _, _, _, _, _ -> },
    onNavigateToPlanReading: (bookId: Int, chapter: Int, verse: Int?, allDays: List<List<String>>, currentDayIndex: Int) -> Unit = { _, _, _, _, _ -> },
    onNavigateToPlanTracking: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val currentPlan by viewModel.currentPlan.collectAsState(initial = null)
    val currentDayReadings by viewModel.currentDayReadings.collectAsState(initial = emptyList())
    val currentDayIndex by viewModel.currentDayIndex.collectAsState(initial = 0)
    val selectedDateStr by dashboardViewModel.selectedDate.collectAsState()
    val massReadingsEntry by dashboardViewModel.massReadings.collectAsState()
    val progressMap by viewModel.progressMap.collectAsState()

    val allDates = remember { dashboardViewModel.getAllDates() }
    val initialPage = remember(selectedDateStr) { 
        val idx = allDates.indexOf(selectedDateStr)
        if (idx != -1) idx else allDates.indexOf(LocalDate.now().toString()).coerceAtLeast(0)
    }
    
    val pagerState = rememberPagerState(initialPage = initialPage) { allDates.size }

    // Sync ViewModel with Pager
    LaunchedEffect(pagerState.currentPage) {
        val date = allDates[pagerState.currentPage]
        if (date != selectedDateStr) {
            dashboardViewModel.updateDate(date)
        }
    }

    // Sync Pager with ViewModel (e.g. when picking a date from calendar)
    LaunchedEffect(selectedDateStr) {
        val idx = allDates.indexOf(selectedDateStr)
        if (idx != -1 && idx != pagerState.currentPage) {
            pagerState.animateScrollToPage(idx)
        }
    }

    val selectedDate = LocalDate.parse(selectedDateStr)
    val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())

    val accentColor = remember(massReadingsEntry?.season) {
        getLiturgicalColor(massReadingsEntry?.season)
    }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        dashboardViewModel.updateDate(date.toString())
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        dashboardViewModel.updateDate(LocalDate.now().toString())
                        showDatePicker = false
                    }) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Today")
                    }
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        modifier = Modifier.background(Color.White),
        topBar = {
            Text("")
        },
        content = {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Date and Title
                Text(
                    text = selectedDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date and title on the left
                    Column(
                        alignment = Alignment.Start
                    ) {
                        Text(
                            text = if (selectedDate == LocalDate.now()) "Today" else selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            ),
                            color = Color.Black
                        )
                        massReadingsEntry?.season?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                color = accentColor,
                                modifier = Modifier.widthIn(max = 180.dp),
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                    // Then only the date picker button (settings moved to bottom bar)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date picker button
                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select date")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Scrollable Date Strip
                ScrollableDateStrip(
                    allDates = allDates,
                    selectedDate = selectedDate,
                    accentColor = accentColor,
                    onDateSelected = { date: LocalDate ->
                        dashboardViewModel.updateDate(date.toString())
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) { page ->
                val dateStr = allDates[page]
                
                val pageMassReadings = if (dateStr == selectedDateStr) {
                    massReadingsEntry
                } else {
                    remember(dateStr) { dashboardViewModel.getMassReadingsForDate(dateStr) }
                }

                DashboardDayContent(
                    massReadingsEntry = pageMassReadings,
                    currentDayReadings = currentDayReadings,
                    currentDayIndex = currentDayIndex,
                    progressMap = progressMap,
                    currentPlan = currentPlan,
                    accentColor = if (dateStr == selectedDateStr) accentColor else getLiturgicalColor(pageMassReadings?.season),
                    isCurrentDatePage = dateStr == selectedDateStr,
                    onNavigateToReading = onNavigateToReading,
                    onNavigateToMassReading = onNavigateToMassReading,
                    onNavigateToPlanReading = onNavigateToPlanReading,
                    onNavigateToPlanTracking = onNavigateToPlanTracking,
                    onTogglePlanReadingComplete = { planId: String, dayIdx: Int, readingIdx: Int ->
                        viewModel.toggleReadingCompleted(planId, dayIdx, readingIdx)
                    }
                )
            }
        },
        bottomBar = {
            BottomAppBar(
                content = {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Dashboard icon (current)
                        IconButton(
                            onClick = { /* Do nothing, we are already on Dashboard */ },
                            enabled = false
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Dashboard")
                        }
                        // Reading icon
                        IconButton(
                            onClick = { /* TODO: Navigate to Reading */ }
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = "Reading")
                        }
                        // Plans icon
                        IconButton(
                            onClick = { /* TODO: Navigate to Plans */ }
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Plans")
                        }
                        // Settings icon
                        IconButton(
                            onClick = { onNavigateToSettings() }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        }
    )
}

@Composable
fun ScrollableDateStrip(
    allDates: List<String>,
    selectedDate: LocalDate,
    accentColor: Color,
    onDateSelected: (LocalDate) -> Unit
) {
    val listState = rememberLazyListState()
    val selectedIndex = remember(selectedDate) {
        allDates.indexOf(selectedDate.toString()).coerceAtLeast(0)
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            // Center the selected date in the strip
            listState.animateScrollToItem((selectedIndex - 3).coerceAtLeast(0))
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(allDates.size) { index ->
            val dateStr = allDates[index]
            val date = LocalDate.parse(dateStr)
            val isSelected = date == selectedDate
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .width(45.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onDateSelected(date) }
            ) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) accentColor else Color.Gray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) accentColor else Color.Black
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                } else {
                    Spacer(modifier = Modifier.size(4.dp))
                }
            }
        }
    }
}

@Composable
fun DashboardDayContent(
    massReadingsEntry: com.verbum.universalis.data.entities.DailyMassReadingEntry?,
    currentDayReadings: List<ReadingInfo>,
    currentDayIndex: Int,
    progressMap: Map<String, Map<Int, com.verbum.universalis.data.json.DayProgress>>,
    currentPlan: com.verbum.universalis.data.json.ReadingPlan?,
    accentColor: Color,
    isCurrentDatePage: Boolean,
    onNavigateToReading: (bookId: Int, chapter: Int) -> Unit,
    onNavigateToMassReading: (bookId: Int, chapter: Int, verse: Int?, allReadings: MassReadings, currentIndex: Int) -> Unit,
    onNavigateToPlanReading: (bookId: Int, chapter: Int, verse: Int?, allDays: List<List<String>>, currentDayIndex: Int) -> Unit,
    onNavigateToPlanTracking: () -> Unit,
    onTogglePlanReadingComplete: (String, Int, Int) -> Unit
) {
    // Readings Timeline
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Daily Mass Section
        massReadingsEntry?.let { mass ->
            item {
                Text(
                    text = "Daily Mass",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = accentColor,
                    modifier = Modifier.padding(start = 44.dp, top = 8.dp, bottom = 4.dp)
                )
            }

            val readings = mass.readings.map { it.type to it.reference }
            items(mass.readings.size) { index ->
                val ref = mass.readings[index]
                val typeLabel = when (ref.type) {
                    "firstReading" -> "First Reading"
                    "psalm" -> "Responsorial Psalm"
                    "gospel" -> "Gospel"
                    else -> ref.type.replaceFirstChar { it.uppercase() }
                }
                
                TimelineItem(
                    title = typeLabel,
                    subtitle = ref.reference,
                    time = if (index == 0) "Liturgical" else "",
                    isHighlighted = ref.type == "gospel",
                    accentColor = accentColor,
                    onClick = {
                        val parts = ref.reference.split(":")
                        if (parts.size >= 2) {
                            val bookAndChapter = parts[0].trim()
                            val versePartWithRange = parts[1].trim()
                            
                            val bookName = if (bookAndChapter.contains(" ")) bookAndChapter.substringBeforeLast(" ").trim() else bookAndChapter
                            val chapterString = if (bookAndChapter.contains(" ")) bookAndChapter.substringAfterLast(" ").trim() else "1"
                            
                            val bookId = Passage.BOOK_NAME_TO_ID[bookName] ?: 
                                        Passage.BOOK_NAME_TO_ID.entries.find { it.key.contains(bookName, ignoreCase = true) }?.value
                            
                            if (bookId != null) {
                                val chapter = chapterString.toIntOrNull() ?: 1
                                val verse = versePartWithRange.split("-")[0].split(",")[0].trim().toIntOrNull()
                                onNavigateToMassReading(bookId, chapter, verse, readings, index)
                            }
                        } else {
                            // Fallback for simple references like "John 1" or "John 1-5"
                            val refText = ref.reference.trim()
                            val bookName = if (refText.contains(" ")) refText.substringBeforeLast(" ").trim() else refText
                            val chapterString = if (refText.contains(" ")) refText.substringAfterLast(" ").trim() else "1"
                            
                            val bookId = Passage.BOOK_NAME_TO_ID[bookName] ?: 
                                        Passage.BOOK_NAME_TO_ID.entries.find { it.key.contains(bookName, ignoreCase = true) }?.value
                            if (bookId != null) {
                                val chapter = chapterString.split("-")[0].split(",")[0].trim().toIntOrNull() ?: 1
                                onNavigateToReading(bookId, chapter)
                            }
                        }
                    }
                )
            }
        }

        // Separator between Mass and Plan
        if (massReadingsEntry != null && isCurrentDatePage && currentDayReadings.isNotEmpty()) {
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                    thickness = 1.dp,
                    color = Color(0xFFF0F0F0)
                )
            }
        }

        // 2. Yearly Plan Section
        if (isCurrentDatePage && currentDayReadings.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPlanTracking() }
                        .padding(start = 44.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Yearly Plan",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.Gray
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }

            val allPlanDays = currentPlan?.days?.map { day ->
                day.readings.map { it.toString() }
            } ?: emptyList()

            val planId = currentPlan?.planId ?: ""
            val dayProgress = progressMap[planId]?.get(currentDayIndex)

            items(currentDayReadings.size) { index ->
                val reading = currentDayReadings[index]
                val isCompleted = dayProgress?.completedReadings?.contains(index) ?: false

                TimelineItem(
                    title = "Bible in a Year",
                    subtitle = "${reading.book} ${reading.chapter}",
                    time = "Day ${currentDayIndex + 1}",
                    isHighlighted = false,
                    accentColor = accentColor,
                    isCompleted = isCompleted,
                    onToggleComplete = {
                        onTogglePlanReadingComplete(planId, currentDayIndex, index)
                    },
                    onClick = {
                        val bookId = Passage.BOOK_NAME_TO_ID[reading.book] ?: 
                                    Passage.BOOK_NAME_TO_ID.entries.find { it.key.contains(reading.book, ignoreCase = true) }?.value
                        if (bookId != null) {
                            onNavigateToPlanReading(bookId, reading.chapter, reading.verse, allPlanDays, currentDayIndex)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TimelineItem(
    title: String,
    subtitle: String,
    time: String,
    isHighlighted: Boolean,
    accentColor: Color,
    isCompleted: Boolean = false,
    onToggleComplete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline line and dot
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            // The vertical line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .padding(top = 12.dp) // Start below the dot center
                    .background(Color(0xFFF0F0F0))
            )
            
            // The dot
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isHighlighted || isCompleted) accentColor else Color.White)
                    .then(
                        if (!isHighlighted && !isCompleted) Modifier.background(Color.White).padding(2.dp).clip(CircleShape).background(Color.LightGray) else Modifier
                    )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content Card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isHighlighted) accentColor else Color(0xFFF8F9FA)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isHighlighted) Color.White else Color.Black
                        )
                        if (time.isNotEmpty()) {
                            Text(
                                text = time,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isHighlighted) Color.White.copy(alpha = 0.8f) else Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isHighlighted) Color.White.copy(alpha = 0.9f) else Color.Gray
                    )
                }
                
                if (onToggleComplete != null) {
                    IconButton(onClick = onToggleComplete) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Complete",
                            tint = if (isCompleted) accentColor else Color.LightGray
                        )
                    }
                }
            }
        }
    }
}

fun getLiturgicalColor(season: String?): Color {
    return when {
        season == null -> VerbumBlue
        season.contains("Advent", ignoreCase = true) -> Color(0xFF673AB7) // Violet
        season.contains("Christmas", ignoreCase = true) -> Color(0xFFB8860B) // Dark Gold
        season.contains("Lent", ignoreCase = true) -> Color(0xFF673AB7) // Violet
        season.contains("Easter", ignoreCase = true) -> Color(0xFFB8860B) // Dark Gold
        season.contains("Ordinary", ignoreCase = true) -> Color(0xFF388E3C) // Green
        season.contains("Holy Week", ignoreCase = true) || 
        season.contains("Pentecost", ignoreCase = true) ||
        season.contains("Passion", ignoreCase = true) -> Color(0xFFC62828) // Red
        else -> VerbumBlue
    }
}
