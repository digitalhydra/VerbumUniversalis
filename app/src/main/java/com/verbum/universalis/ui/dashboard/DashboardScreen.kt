package com.verbum.universalis.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.verbum.universalis.ui.navigation.MassReadings
import com.verbum.universalis.ui.navigation.PlanReadings
import com.verbum.universalis.ui.reader.Passage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: ReadingPlanViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToReading: (bookId: Int, chapter: Int) -> Unit = { _, _ -> },
    onNavigateToMassReading: (bookId: Int, chapter: Int, verse: Int?, allReadings: MassReadings, currentIndex: Int) -> Unit = { _, _, _, _, _ -> },
    onNavigateToPlanReading: (bookId: Int, chapter: Int, verse: Int?, allDays: List<List<String>>, currentDayIndex: Int) -> Unit = { _, _, _, _, _ -> }
) {
    val currentPlan by viewModel.currentPlan.collectAsState(initial = null)
    val currentDayReadings by viewModel.currentDayReadings.collectAsState(initial = emptyList())
    val currentDayIndex by viewModel.currentDayIndex.collectAsState(initial = 0)
    val selectedDateStr by dashboardViewModel.selectedDate.collectAsState()
    val massReadingsEntry by dashboardViewModel.massReadings.collectAsState()

    val selectedDate = LocalDate.parse(selectedDateStr)
    val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())

    val accentColor = remember(massReadingsEntry?.season) {
        getLiturgicalColor(massReadingsEntry?.season)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
    ) {
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

        Spacer(modifier = Modifier.height(24.dp))

        // Week Calendar
        WeekCalendar(
            selectedDate = selectedDate,
            accentColor = accentColor,
            onDateSelected = { date ->
                dashboardViewModel.updateDate(date.toString())
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Readings Timeline
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
            if (massReadingsEntry != null && currentDayReadings.isNotEmpty()) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        thickness = 1.dp,
                        color = Color(0xFFF0F0F0)
                    )
                }
            }

            // 2. Yearly Plan Section
            if (currentDayReadings.isNotEmpty()) {
                item {
                    Text(
                        text = "Yearly Plan",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 44.dp, bottom = 4.dp)
                    )
                }

                val allPlanDays = currentPlan?.days?.map { day ->
                    day.readings.map { it.toString() }
                } ?: emptyList()

                items(currentDayReadings.size) { index ->
                    val reading = currentDayReadings[index]
                    TimelineItem(
                        title = "Bible in a Year",
                        subtitle = "${reading.book} ${reading.chapter}",
                        time = "Day ${currentDayIndex + 1}",
                        isHighlighted = false,
                        accentColor = accentColor,
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
}

@Composable
fun WeekCalendar(
    selectedDate: LocalDate,
    accentColor: Color,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    // Show a window of 7 days around the selected date or just the current week
    val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        (0..6).forEach { dayOffset ->
            val date = startOfWeek.plusDays(dayOffset.toLong())
            val isSelected = date == selectedDate
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable(
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
fun TimelineItem(
    title: String,
    subtitle: String,
    time: String,
    isHighlighted: Boolean,
    accentColor: Color,
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
                    .background(if (isHighlighted) accentColor else Color.White)
                    .then(
                        if (!isHighlighted) Modifier.background(Color.White).padding(2.dp).clip(CircleShape).background(Color.LightGray) else Modifier
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
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
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
