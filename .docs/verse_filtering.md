# Verse Filtering and Navigation

This document explains how verse filtering works in Verbum Universalis and how the UI handles navigation to specific ranges.

## Data Structure: `Passage`

The core logic for passage handling resides in the `Passage` data class (within `ReadingViewModel.kt`).

- **`bookId`**: Internal ID of the book.
- **`chapter`**: Chapter number.
- **`verseFilter`**: A string representing ranges or individual verses (e.g., `"17-23"`, `"1-3, 5"`).

### Parsing Logic
`Passage.parseFilter(filter: String?)` splits the filter string by commas and hyphens to create a list of `IntRange` objects.

### Visibility Logic
`Passage.isVerseVisible(verseNumber: Int)` checks if a given verse number is contained within any of the parsed ranges. If `verseFilter` is null, all verses are considered visible.

## UI Implementation

### `ReadingViewModel`
The `ReadingViewModel` exposes a `verses` Flow which combines the current passage and active language. It filters the raw list of verses from the database using `Passage.isVerseVisible`.

```kotlin
val verses: Flow<List<VerseWithTexts>> = combine(_currentPassage, _activeLanguage) { passage, _ -> 
    passage 
}.flatMapLatest { passage ->
    repository.getChapter(passage.bookId, passage.chapter).map { list ->
        if (passage.verseFilter == null) list
        else list.filter { passage.isVerseVisible(it.verse.verse_number) }
    }
}
```

### `ReadingScreen` and `ReadingCanvasScreen`
Navigation usually happens through `ReadingCanvasScreen`, which parses the incoming navigation arguments (from Reading Plans or Mass Readings).

- **`initialFilter`**: This parameter is passed from the navigation graph to `ReadingCanvasScreen` and then to `ReadingScreen`.
- **Initialization**: `ReadingScreen` uses a `LaunchedEffect` to notify the `ReadingViewModel` of the current passage including the filter.

```kotlin
LaunchedEffect(initialBookId, initialChapter, initialVerse, initialFilter) {
    if (initialBookId != null && initialChapter != null) {
        viewModel.setPassage(initialBookId, initialChapter, initialVerse, initialFilter)
    }
}
```

## Navigation Routes

Routes are defined in `Route.kt`. When navigating to a filtered passage, the `filter` argument is appended to the query string:

`reading_canvas?bookId=56&chapter=1&filter=17-23`

This filter is then extracted in `VerbumNavGraph` and passed down to the screens.
