# Multi-Calendar Daily Readings - Implementation Guide

## Executive Summary

This document outlines the complete implementation path for supporting three Catholic liturgical calendars (US, Colombia, Rome) in Verbum Universalis.

---

## Phase 0: Data Sourcing

Before any code changes, we need to obtain and format the reading data for all three calendars.

### 0.1 USCCB Readings (USA) ✅ ALREADY EXISTS

**Current Status:** Data exists in `raw_data/reading-plan/readings_2026.json`

**Format:**
```json
{
  "date": "2026-01-01",
  "monthDay": "1/1",
  "season": "Christmas",
  "subSeason": "Octave Day of Christmas",
  "readings": [
    { "type": "firstReading", "reference": "Numbers 6:22-27" },
    { "type": "psalm", "reference": "Psalm 67:2-3, 5-6, 8" },
    { "type": "gospel", "reference": "Luke 2:16-21" }
  ],
  "usccbLink": "https://bible.usccb.org/bible/readings/010126.cfm"
}
```

**Action:** Verify 2025-2027 data coverage exists or can be generated.

---

### 0.2 Colombia (CEC) - Data Acquisition

**Option A: Manual Research (Recommended for MVP)**
1. Identify key differences from US calendar:
   - Local feast days (Colombian saints, national celebrations)
   - Different celebration dates for some movable feasts
2. Create static JSON by adapting USCCB format with Colombian-specific days

**Option B: Web Scraping**
- Target: https://liturgia.catedralbogota.org.co or similar
- Tools: Python BeautifulSoup or similar
- Output: JSON in same format as USCCB

**Minimum Viable Data:**
- 366 days of readings (or difference days from US)
- Focus on: National feast days, different liturgical dates

**Action Items:**
- [ ] Research Colombian Catholic liturgical calendar differences
- [ ] Create `colombia-readings-2026.json` in USCCB-compatible format
- [ ] Document source attribution

---

### 0.3 Rome (General Roman) - Data Acquisition

**API Integration:** `calapi.inadiutorium.cz`

**API Documentation:**
```
Base URL: https://calapi.inadiutorium.cz/api/v0/
Endpoint: /en/calendars/general-romane/{year}/{month}/{day}
Example: https://calapi.inadiutorium.cz/api/v0/en/calendars/general-romane/2026/1/1
```

**Response Format:**
```json
{
  "date": "2026-01-01",
  "celebrations": [
    {
      "id": "maryMotherOfGod",
      "name": "Mary, Mother of God",
      "rank": "solemnity",
      "rankNum": 10
    }
  ],
  "readings": {
    "Mass": {
      "year A": {...},
      "year B": {...},
      "year C": {...}
    }
  }
}
```

**Implementation Options:**

| Option | Pros | Cons |
|--------|------|------|
| **A: Live API calls** | Always current, no storage | Requires internet |
| **B: Cache locally** | Offline capable | Must refresh periodically |
| **C: Static JSON** | Simple, offline-first | Manual updates needed |

**Recommended:** Option B - Cache locally, refresh on app launch if >30 days stale.

**Action Items:**
- [ ] Test API endpoint availability
- [ ] Write conversion script to transform API response → USCCB format
- [ ] Create initial `rome-readings-2026.json` for offline fallback

---

## Phase 1: Data Infrastructure

### 1.1 Update Data Models

**File:** `data/entities/LiturgicalEntities.kt`

```kotlin
// Add enum for calendar source
enum class CalendarSource(val displayName: String) {
    USCCB("United States (USCCB)"),
    COLOMBIA("Colombia (CEC)"),
    ROME("Roman General")
}

// Extend existing model
data class DailyMassReadingEntry(
    // ... existing fields ...
    val calendarSource: CalendarSource = CalendarSource.USCCB // NEW
)

data class UserSettings(
    // ... existing fields ...
    val preferredCalendar: CalendarSource = CalendarSource.USCCB // NEW
)
```

**Checklist:**
- [ ] Add `CalendarSource` enum
- [ ] Add `calendarSource` field to `DailyMassReadingEntry`
- [ ] Add `preferredCalendar` to `UserSettings`

---

### 1.2 Create Calendar Data Loader

**New File:** `data/download/CalendarDownloader.kt`

```kotlin
class CalendarDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Load bundled JSON files for each calendar
    fun loadCalendarData(source: CalendarSource): List<DailyMassReadingEntry>
    
    // Fetch from Rome API and cache
    suspend fun fetchRomeData(date: LocalDate): DailyMassReadingEntry?
    
    // Check if cached data is stale (>30 days)
    fun isDataStale(lastUpdate: Long): Boolean
}
```

**Checklist:**
- [ ] Create `CalendarDownloader` class
- [ ] Implement bundled data loading for US and Colombia
- [ ] Implement Rome API integration
- [ ] Add stale data detection logic

---

### 1.3 Update LiturgicalRepository

**File:** `data/repository/LiturgicalRepository.kt`

```kotlin
class LiturgicalRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarDownloader: CalendarDownloader
) {
    private var currentSource: CalendarSource = CalendarSource.USCCB
    
    // NEW: Switch calendar source
    fun setCalendarSource(source: CalendarSource) {
        currentSource = source
        loadCalendarData(source)
    }
    
    // NEW: Get current source
    fun getCalendarSource(): CalendarSource = currentSource
    
    // MODIFIED: Get readings for current source
    fun getMassReadingsForDate(date: String): DailyMassReadingEntry? {
        return _massReadings.value.find { 
            it.date == date && it.calendarSource == currentSource 
        }
    }
    
    // NEW: Get readings for specific source (for offline pre-fetch)
    fun getMassReadingsForSource(source: CalendarSource, date: String): DailyMassReadingEntry?
}
```

**Checklist:**
- [ ] Add `currentSource` state
- [ ] Implement `setCalendarSource()` method
- [ ] Implement `getCalendarSource()` method
- [ ] Update `getMassReadingsForDate()` to respect current source
- [ ] Add source-aware data loading logic

---

## Phase 2: User Settings Integration

### 2.1 Update UserSettings

**File:** `data/json/UserSettings.kt`

```kotlin
data class UserSettings(
    // ... existing fields ...
    val preferredCalendar: String = "usccb",  // "usccb", "colombia", "rome"
    val lastCalendarSync: Long = 0L           // timestamp
)

fun UserSettings.toCalendarSource(): CalendarSource {
    return when (preferredCalendar) {
        "colombia" -> CalendarSource.COLOMBIA
        "rome" -> CalendarSource.ROME
        else -> CalendarSource.USCCB
    }
}
```

**Checklist:**
- [ ] Add `preferredCalendar` field
- [ ] Add `lastCalendarSync` timestamp field
- [ ] Add conversion helper function

---

### 2.2 Update Settings Repository

**File:** `data/json/FileManager.kt`

```kotlin
fun saveCalendarPreference(calendar: String) {
    val settings = loadSettings() ?: UserSettings()
    val updated = settings.copy(
        preferredCalendar = calendar,
        lastCalendarSync = System.currentTimeMillis()
    )
    saveSettings(updated)
}
```

**Checklist:**
- [ ] Add `saveCalendarPreference()` method

---

## Phase 3: ViewModel Layer

### 3.1 Update DashboardViewModel

**File:** `ui/dashboard/DashboardViewModel.kt`

```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val liturgicalRepository: LiturgicalRepository,
    private val fileManager: FileManager  // NEW
) : ViewModel() {
    
    // EXISTING: Current calendar source
    private val _calendarSource = MutableStateFlow(CalendarSource.USCCB)
    val calendarSource: StateFlow<CalendarSource> = _calendarSource.asStateFlow()
    
    init {
        // NEW: Load user's preferred calendar
        loadUserCalendarPreference()
    }
    
    private fun loadUserCalendarPreference() {
        val settings = fileManager.loadSettings()
        val preferred = settings?.preferredCalendar?.toCalendarSource() ?: CalendarSource.USCCB
        liturgicalRepository.setCalendarSource(preferred)
        _calendarSource.value = preferred
    }
    
    // NEW: Handle calendar source change
    fun setCalendarSource(source: CalendarSource) {
        _calendarSource.value = source
        liturgicalRepository.setCalendarSource(source)
        fileManager.saveCalendarPreference(source.name.lowercase())
        
        // Reload readings for new source
        updateDate(_selectedDate.value)
    }
}
```

**Checklist:**
- [ ] Add `calendarSource` state flow
- [ ] Implement `loadUserCalendarPreference()`
- [ ] Implement `setCalendarSource()`
- [ ] Persist selection to settings

---

## Phase 4: UI Implementation

### 4.1 Create Calendar Selector Component

**New File:** `ui/components/CalendarSourceSelector.kt`

```kotlin
@Composable
fun CalendarSourceSelector(
    currentSource: CalendarSource,
    onSourceChange: (CalendarSource) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = currentSource.displayName,
                style = MaterialTheme.typography.labelMedium
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select calendar"
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CalendarSource.entries.forEach { source ->
                DropdownMenuItem(
                    text = { Text(source.displayName) },
                    onClick = {
                        onSourceChange(source)
                        expanded = false
                    },
                    leadingIcon = {
                        if (source == currentSource) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                )
            }
        }
    }
}
```

**Checklist:**
- [ ] Create `CalendarSourceSelector` component
- [ ] Implement dropdown menu
- [ ] Add selection indicator

---

### 4.2 Update DashboardScreen

**File:** `ui/dashboard/DashboardScreen.kt`

```kotlin
@Composable
fun DashboardScreen(
    // ... existing parameters ...
) {
    val calendarSource by dashboardViewModel.calendarSource.collectAsState()
    // ... existing state ...
    
    // MODIFY: Add calendar selector to top bar or header
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Daily Readings")
                        // NEW: Calendar source indicator
                        CalendarSourceSelector(
                            currentSource = calendarSource,
                            onSourceChange = { source ->
                                dashboardViewModel.setCalendarSource(source)
                            }
                        )
                    }
                },
                // ... actions ...
            )
        }
    ) { /* ... */ }
}
```

**Checklist:**
- [ ] Import `CalendarSourceSelector`
- [ ] Collect `calendarSource` state
- [ ] Add selector to TopAppBar
- [ ] Connect `onSourceChange` callback

---

### 4.3 Add Visual Indicator for Different Calendars

**In DashboardScreen:**

```kotlin
// Show when calendar differs from US (default)
if (calendarSource != CalendarSource.USCCB) {
    AssistChip(
        onClick = { /* show info */ },
        label = { Text(calendarSource.displayName) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}
```

**Checklist:**
- [ ] Add calendar indicator chip
- [ ] Show source-specific information in readings cards

---

## Phase 5: Testing & Polish

### 5.1 Test Scenarios

| Test | Description |
|------|-------------|
| T1 | Switch calendar source and verify readings change |
| T2 | Switch to Colombia, verify local feast days appear |
| T3 | Switch to Rome, verify API fallback works |
| T4 | Close and reopen app, verify preference persists |
| T5 | Check offline mode works for all three calendars |
| T6 | Verify date navigation works per calendar |

### 5.2 Edge Cases

- **No internet for Rome API:** Fall back to cached/bundled data
- **Missing date in calendar:** Show "No readings" message gracefully
- **API rate limiting:** Implement exponential backoff
- **Stale data warning:** Show "Last updated X days ago" if >30 days

---

## Implementation Checklist Summary

### Data Sourcing
- [ ] Verify USCCB data is complete (2025-2027)
- [ ] Create Colombia readings JSON
- [ ] Create Rome readings JSON (or cache strategy)

### Data Layer
- [ ] Add `CalendarSource` enum to LiturgicalEntities
- [ ] Create CalendarDownloader
- [ ] Update LiturgicalRepository for multi-source

### Settings Layer
- [ ] Update UserSettings with preferredCalendar
- [ ] Add preference persistence

### ViewModel Layer
- [ ] Update DashboardViewModel for calendar selection
- [ ] Connect to repository

### UI Layer
- [ ] Create CalendarSourceSelector component
- [ ] Integrate into DashboardScreen
- [ ] Add visual indicators

### Testing
- [ ] Unit tests for repository
- [ ] Integration tests for selector
- [ ] Manual testing all calendars

---

## Files to Modify

| File | Action |
|------|--------|
| `data/entities/LiturgicalEntities.kt` | Add enum, update model |
| `data/json/UserSettings.kt` | Add preferredCalendar |
| `data/json/FileManager.kt` | Add saveCalendarPreference |
| `data/download/CalendarDownloader.kt` | NEW - Data loading |
| `data/repository/LiturgicalRepository.kt` | Multi-source support |
| `ui/dashboard/DashboardViewModel.kt` | Calendar selection |
| `ui/dashboard/DashboardScreen.kt` | Add selector UI |
| `ui/components/CalendarSourceSelector.kt` | NEW - Selector component |

## New Files to Create

| File | Purpose |
|------|---------|
| `raw_data/reading-plan/colombia-readings-2026.json` | Colombian calendar data |
| `raw_data/reading-plan/rome-readings-2026.json` | Roman calendar data (or cache) |
| `data/download/CalendarDownloader.kt` | Calendar data loading |
| `ui/components/CalendarSourceSelector.kt` | UI component |
| `.plan/multi-calendar-readings.md` | Documentation (DONE) |

---

## Related Documentation

- [PRD - Tier 3 Liturgical Calendars](./prd.md#tier-3-liturgical-calendars-lightweight-api)
- [Multi-Calendar Readings Overview](./multi-calendar-readings.md)
- [LiturgicalEntities.kt](../../app/src/main/java/com/verbum/universalis/data/entities/LiturgicalEntities.kt)
- [LiturgicalRepository.kt](../../app/src/main/java/com/verbum/universalis/data/repository/LiturgicalRepository.kt)