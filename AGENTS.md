# Verbun Universalis — Agent Guide

Android app (Kotlin/Compose) + Python ETL pipeline. Catholic Bible study tool with offline-first DB, interlinear, Catena commentary, CCC, and Git sync.

---

## Stack

- **Android**: Kotlin 2.1.0, AGP 8.12, compileSdk 36, targetSdk 34, minSdk 26
- **UI**: Jetpack Compose + Material 3 + Adaptive Layout
- **DI**: Hilt 2.53 (kapt)
- **DB**: Room 2.7.0 + raw SQLite (`SQLiteDatabase.openDatabase`)
- **Navigation**: Compose Navigation 2.8.5
- **WorkManager**: 2.9.0 (HiltWorkerFactory)
- **Serialization**: kotlinx-serialization-json 1.6.3
- **Git sync**: JGit 6.8.0 + JSch + BouncyCastle
- **Network**: OkHttp 4.12.0
- **ETL**: Python 3.10+, pysword

## Build Commands

```bash
# Android build
./gradlew assembleDebug

# Unit tests (Robolectric)
./gradlew testDebugUnitTest

# Instrumented tests (emulator/device)
./gradlew connectedDebugAndroidTest

# ETL — seed DB (Bible texts + interlinear)
python3 etl/build_verbum_seed.py

# ETL — CCC DB
python3 etl/build_ccc_db.py

# Android build + regenerate seed DB
./gradlew assembleDebug -Pbuild_seed
```

## Project Layout

```
VerbunUniversalis/
├── app/                          # Android app module
│   ├── src/main/
│   │   ├── assets/               # Shipped DBs: verbum_seed.db, verbum_ccc.db
│   │   ├── java/com/verbum/universalis/
│   │   │   ├── core/
│   │   │   │   ├── di/           # Hilt modules (AppModule, DatabaseModule, RepositoryModule)
│   │   │   │   ├── theme/        # Custom colors, typography, Theme composable
│   │   │   │   └── LanguageManager.kt
│   │   │   ├── data/
│   │   │   │   ├── daos/         # Room DAOs (VerseDao, InterlinearDao, LexiconDao)
│   │   │   │   ├── db/           # AppDatabase (Room), CatenaRawDatabase, CrossRefsRawDatabase, CatechismRawDatabase
│   │   │   │   ├── entities/     # Room entities + CatenaCommentaryEntity
│   │   │   │   ├── json/         # FileManager (atomic writes), DataClasses (Note, Highlight, etc.)
│   │   │   │   ├── repository/   # BibleRepository, CatenaRepository, NotesRepository, CrossRefsRepository
│   │   │   │   ├── download/     # DataDownloader (GitHub releases)
│   │   │   │   ├── sync/         # GitSyncService (JGit), GitSyncWorker, GitSyncViewModel
│   │   │   │   ├── ssh/          # SSHKeyManager
│   │   │   │   ├── github/       # GitHubApiService
│   │   │   │   └── oauth/        # OAuthManager
│   │   │   └── ui/
│   │   │       ├── dashboard/    # DashboardScreen, DashboardActivity, DashboardViewModel
│   │   │       ├── reader/       # ReadingCanvas, ReadingScreen, ReadingViewModel, InterlinearReader, Passage, StudyInspector
│   │   │       ├── catechism/    # CatechismScreen, CccTocScreen, CccSearchDrawer, CccEntities
│   │   │       ├── settings/     # SettingsScreen, ThemeScreen, LanguageScreen, NotesScreen, SyncScreen, DownloadCatenaScreen
│   │   │       ├── plans/        # ReadingPlansScreen
│   │   │       ├── components/   # BookPicker, BookListItem, SearchBar, NoteBottomSheet, SelectionHoverMenu
│   │   │       ├── theme/        # HighlightPalette (shared color picker)
│   │   │       └── navigation/   # Route (sealed class), VerbumNavGraph, MainScreen
│   │   └── res/
│   │       ├── values/strings.xml
│   │       └── values-es/strings.xml  # Spanish translations
│   └── src/test/                 # Unit tests (Robolectric + Mockito)
├── etl/                          # Python ETL pipeline
│   ├── build_verbum_seed.py      # Main Bible DB builder (1103 lines)
│   ├── build_ccc_db.py           # CCC DB builder
│   ├── translate_*.py            # CCC translation scripts
│   ├── parse_*.py                # SWORD module parsers
│   └── README.md                 # ETL details
├── scripts/                      # Additional Node.js/Python scripts
├── sql/                          # SQL batch files
├── raw_data/                     # Unversioned raw data (SWORD modules, etc.)
└── docs/                         # Design docs (CCC plan, teachings plan)
```

## Architecture

### Database Strategy (3 DBs + JSON)

| DB | Source | Size | Access |
|----|--------|------|--------|
| `verbum_seed.db` | Shipped in assets/ | ~67MB | Room (`AppDatabase`) |
| `verbum_catena.db` | Downloaded from GitHub releases | ~9MB | Raw SQLite (`CatenaRawDatabase`) |
| `verbum_cross_refs.db` | Downloaded from GitHub releases | — | Raw SQLite (`CrossRefsRawDatabase`) |
| `verbum_ccc.db` | Shipped in assets/ | ~5.5MB | Raw SQLite (`CatechismRawDatabase`) |
| JSON files | Generated at runtime | — | `FileManager` (atomic writes) |

### Core Entities (Room, verbum_seed.db)

- `books` — 73 Catholic books (46 OT + 27 NT), multilingual names
- `verses` — Unique verse anchors, FK→books
- `texts` — Translation content per verse (DR, SCIO, Vulg), FK→verses
- `interlinear_words` — Greek/Hebrew + transliteration + gloss (Strong's), FK→verses
- `lexicon` — Strong's definitions (PK: lemma, e.g., "G1234" or "H7225")

### Book ID Codex

Internal codes are UPPERCASE 3-char (GEN, EXO, LEV...). Two separate maps exist:

1. `bookIdToCode` in `BibleRepository.kt` — 1→GEN, 2→EXO, ..., 47→MAT, 73→REV
2. `tsvBookCodeToInternal` — Maps TSV codes (Gen, Exod, Matt, 1Pet) to internal codes

Always use the internal uppercase codes for internal operations. Use `tsvBookCodeToInternal` for cross-reference TSV parsing.

### Catena Raw SQLite Quirk

`CatenaRawDatabase` bypasses Room entirely — opens DB via `SQLiteDatabase.openDatabase()` with `OPEN_READONLY`. It checks three file locations in order:

1. `context.filesDir/databases/verbum_catena.db`
2. `context.filesDir/verbum_catena.db`
3. `context.getDatabasePath("verbum_catena.db")`

Same pattern for `CrossRefsRawDatabase` and `CatechismRawDatabase`.

### Git Sync (JGit + SSH + OAuth)

- `GitSyncService` uses JGit for pull/commit/push
- SSH key auth via JSch config session factory
- OAuth token via `UsernamePasswordCredentialsProvider("token", <token>)`
- LWW conflict resolution — merges notes, highlights, progress JSON by timestamp per object ID
- Local repo stored in `context.filesDir/sync_repo/`
- First sync: backs up local `userdata/` → `userdata_backup_<timestamp>`

### Download URLs

Catena and cross-refs DBs downloaded from GitHub releases:
```
https://github.com/digitalhydra/VerbunUniversalis/releases/download/WIP/verbum_catena.db
https://github.com/digitalhydra/VerbunUniversalis/releases/download/WIP/verbum_cross_refs.db
```

Cross-references TSV from raw.githubusercontent.com:
```
https://raw.githubusercontent.com/digitalhydra/VerbunUniversalis/refs/heads/master/raw_data/cross_references.txt
```

## State Management

- ViewModels with `StateFlow` / `MutableStateFlow`, collected as Compose state
- Room queries return `Flow<List<...>>` for reactive UI
- `ReadingViewModel` uses `flatMapLatest` + `combine` on passage/reading list state
- User settings persisted via `FileManager` (JSON) + loaded into `StateFlow`
- `LanguageManager` is `@Singleton`, applies locale via `AppCompatDelegate.setApplicationLocales`

## Navigation

- `Route` sealed class defines all routes with argument patterns
- `VerbumNavGraph` composable wires all routes into NavHost
- `MainScreen` renders a floating navigation bar with 4 items (Bible, Readings, Tradition, Settings)
- Bottom bar visible only on Dashboard route
- ReadingCanvas route encodes mass readings and plan readings in URL params (pipe-delimited, comma-delimited)

## Testing

### Unit Tests (Robolectric)

Run: `./gradlew testDebugUnitTest`

Pattern: JUnit 4 + Mockito + Robolectric + Truth/AssertJ

| Test | What it tests |
|------|--------------|
| `DatabaseTest` | Room in-memory DB: insert + query verse/texts/interlinear/lexicon |
| `BibleRepositoryImplTest` | Mocked DAOs: entity→domain mapping |
| `ReadingViewModelTest` | Robolectric + Mockito: note/highlight sheet, verse selection |
| `FileManagerTest` | Atomic write + read-back for all JSON types |
| `GitSyncServiceTest` | Local bare repo creation, sync status, configureRepo |
| `RouteTest` | Route string constants |
| `PassageTest` | Verse range filtering (single range, multi-range, no filter) |
| `ThemeTest` | Color palette constants |
| `GitSyncViewModelTest` | Stub (verify structure compiles) |

### Instrumented Tests

Run: `./gradlew connectedDebugAndroidTest`

| Test | What it tests |
|------|--------------|
| `OfflineDataTest` | Real asset DB (`verbum_seed.db`) — verifies Gen 1:1 has DR and SCIO translations |
| `BibleDaoTest` | Room in-memory (stub methods) |

### Test Setup Quirks

- `ReadingViewModelTest` creates `ReadingViewModel` with real `FileManager` + mocked repos via `SavedStateHandle`
- `GitSyncServiceTest` creates a real local bare repo as fake remote
- `DatabaseTest` uses `Room.inMemoryDatabaseBuilder` — no asset DB needed
- `OfflineDataTest` uses `createFromAsset("verbum_seed.db")` — requires real DB asset
- Some ViewModel fields are `internal` (not `private`) for test access

## ETL Pipeline (Python)

### Seed DB (build_verbum_seed.py)

- 1103 lines, deterministic (same input → identical binary DB)
- Parses 4 SWORD module types: `rawling`, `rawtext`, `rawgenbook`, `rawcom`
- Dependencies: `pip install pysword`
- Sources:
  - Douay-Rheims (DR) — EN
  - Spanish Platense (SPA) — ES
  - Vulgata Clementina (Vulg) — LA
  - ABP Interlinear Greek — Greek NT + transliteration
  - OSHB — Hebrew OT + transliteration
- Transliteration is algorithmic (no external library), handles Greek + Hebrew scripts
- Strong's: Greek keys prefix `G`, Hebrew keys prefix `H`
- Output: `verbum_seed.db` copied to `app/src/main/assets/`

### CCC DB (build_ccc_db.py)

- Input: `raw_data/ccc.json` (scraped from scborromeo.org)
- Output: `verbum_ccc.db` with 4 tables:
  - `ccc_paragraphs` (2865 paragraphs, TOC breadcrumb, rich-text JSON)
  - `ccc_bible_refs` (118 inline citations resolved to book/chapter/verse)
  - `ccc_tradition_refs` (84 Church document references)
  - `ccc_fts` (FTS5 full-text search)
  - `ccc_tags` (empty, for in-app use)
- ~1800 footnoted Bible/Tradition refs on external `cr/NNN.htm` pages not yet scraped

## Conventions & Gotchas

### Language Settings

`UserSettings` has TWO language fields:
- `language` — Bible text language (`"DR"` for Douay-Rheims/EN, `"Spa"` for Spanish)
- `appLanguage` — UI language (`"en"`, `"es"`)

Bible content lang_code in DB: `"DR"`, `"SCIO"`, `"Vulg"`
In UserSettings/default: `"DR"` for EN, `"Spa"` for ES (note: not `"SCIO"`)

### Translation Language Codes

| Code | Translation |
|------|-------------|
| DR | Douay-Rheims (English) — also `DRB` in some contexts |
| SCIO | Spanish Platense — also `Spa` in settings |
| Vulg | Vulgata Clementina (Latin) |

### Reading Calendars

- `"US"` — US daily mass readings
- `"CO"` — Colombia daily mass readings
- `"RO"` — Roman (standard) readings

### Book Name Resolution

`Passage.BOOK_NAME_TO_ID` (in `ReadingViewModel.kt`) maps ~140 English/Spanish name variants to book IDs. This is the third book mapping in the app (alongside `bookIdToCode` and `tsvBookCodeToInternal`).

### FileManager Atomic Writes

`FileManager` uses temp-file + rename pattern:
1. Write to `file.tmp`
2. Rename `file.tmp` → `file`

Files stored in `context.filesDir/userdata/`:
- `settings.json`, `highlights.json`, `notes.json`, `progress.json`, `progress_v2.json`, `ccc_bookmarks.json`

### Known Issues

- No `proguard-rules.pro` file exists (not created yet)
- Some ViewModel fields are `public` (not `private`) for test access — intentional
- `GitSyncViewModelTest` has stub `assertTrue(true)` test only
- `BibleDaoTest` (instrumented) has stub methods
- Catena DB is downloaded from `/releases/download/WIP/` — not versioned
- Translation (CCC) was removed from app; now server-side only
- CCC footnotes (~1800 references) on external pages not scraped
- `Passage` class defined in `ReadingViewModel.kt`, not own file
- Network timeouts: 60s for downloads (DataDownloader), 30/120s for references (BibleRepository)
