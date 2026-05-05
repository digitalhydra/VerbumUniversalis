# Verbum Universalis: Development Plan

This document outlines the step-by-step implementation of the Verbum Universalis Android application. It is designed for AI agents to execute in sequence. Each phase must be completed and verified before moving to the next.

## General Guardrails
- **Persona:** Expert Senior Android Engineer (Clean Architecture, Kotlin 1.9+, Jetpack Compose).
- **State Management:** Use `StateFlow` in ViewModels. Strict State Hoisting in Compose.
- **Dependency Injection:** Use Hilt exclusively.
- **Database:** Room with Coroutines/Flows. DAOs must return `Flow<List<T>>`.
- **Theming:** Material 3 `dynamicLightColorScheme` / `dynamicDarkColorScheme` with a custom "Typography-First" overlay.

---

## Phase 1: Project Scaffolding & Core Architecture
**Goal:** Establish the technical foundation and the visual "Bear-app" aesthetic.

### Steps:
1. **Project Setup:** Create a new Android project with Kotlin 1.9+. Configure `build.gradle` with:
   - Jetpack Compose BOM.
   - Hilt (`dagger.hilt.android`).
   - Room (`androidx.room`).
   - Kotlinx Serialization (`org.jetbrains.kotlinx:kotlinx-serialization-json`).
   - Navigation Compose.
2. **Design System Implementation:**
   - **Typography:** Implement `Source Serif Pro` (Content) and `Inter` (UI). Configure line heights (1.5x-1.6x for Serif).
   - **Colors:** Create a high-contrast neutral palette (Pure White/Black backgrounds, `#EAEAEA` hairlines).
   - **Theme:** Wrap the app in a custom `VerbumTheme` that enforces the "Typography-First Functionalism" (no shadows, no cards, minimalist whitespace).
3. **Navigation Skeleton:**
   - Implement a `NavHost` with basic routes: `ReadingCanvas`, `InterlinearReader`, `ReadingPlans`, `Settings`.
4. **DI Setup:** Implement `@HiltAndroidApp` and a base `AppModule`.

**Expected Result:** A running app that displays a minimalist "Hello World" screen using the custom theme, with a working navigation graph.

---

## Phase 2: Tier 1 Database Payload & Room Layer
**Goal:** Implement the offline-first data core.

### Steps:
1. **Schema Definition:** Create Room entities for:
   - `Verse`: `id`, `bookId`, `chapter`, `verseNumber`, `text`, `language` (EN/ES/LA).
   - `InterlinearWord`: `id`, `verseId`, `wordOrder`, `originalText`, `transliteration`, `literalTranslation`, `morphology`.
   - `LexiconEntry`: `root`, `definition`.
2. **DAO Implementation:**
   - `VerseDao`: Queries for chapters (returning `Flow<List<Verse>>`) and specific verses.
   - `InterlinearDao`: Queries for all words in a chapter/verse.
   - `LexiconDao`: Root-based lookup.
3. **Database Seeding:**
   - Implement `RoomDatabase.Callback` to pre-populate the database from `verbum_seed.db` located in the `assets` folder.
4. **Repository Layer:** Create `BibleRepository` to abstract Room access and provide clean `Flow` streams to ViewModels.

**Expected Result:** The app can successfully boot, load the bundled SQLite database, and a test ViewModel can print a verse's text to the logs.

---

## Phase 3: Reading Canvas Domain & UI
**Goal:** A professional-grade, minimalist biblical reading experience.

### Steps:
1. **Reading ViewModel:** Implement logic to:
   - Load a chapter based on Book/Chapter selection.
   - Toggle between primary translations (Douay-Rheims $\leftrightarrow$ Scío de San Miguel).
2. **Canvas UI:**
   - Implement a `LazyColumn` for verses.
   - Use the Serif font with 1.6x line height.
   - Implement "hairline" separators between verses.
   - Remove all Material "Card" components; use plain text and generous whitespace.
3. **Interaction:**
   - Implement verse tapping $\rightarrow$ Navigate to `InterlinearReader` for that verse/chapter.
   - Simple TopAppBar for Book/Chapter navigation (minimalist dropdowns).

**Expected Result:** A user can read the Bible in English or Spanish with a clean, high-legibility interface.

---

## Phase 4: Interlinear Engine & Adaptive Layout
**Goal:** Scholarly linguistic analysis with tablet optimization.

### Steps:
1. **Interlinear UI (Mobile):**
   - Create a `LazyColumn` for the chapter.
   - Implement the **Word Block Stack** using `FlowRow`:
     1. Original Word (Serif, Bold).
     2. Transliteration (Sans-Serif, Italic, Muted).
     3. Literal Translation (Serif, Normal).
     4. Morphology (Sans-Serif, Small, Muted).
2. **Tablet Adaptive Layout:**
   - Implement `ListDetailPaneScaffold`.
   - **Left Pane:** `ReadingCanvas` or `InterlinearReader`.
   - **Right Pane:** `StudyInspector`.
3. **Study Inspector Logic:**
   - Tapping a word in the `InterlinearReader` $\rightarrow$ Update `StudyInspector` with the Lexicon definition for that root.
   - Implement a "Tradition" toggle in the inspector to show Catena commentary (mocked for now).

**Expected Result:** A functional interlinear reader that wraps text correctly on mobile and provides a side-by-side study experience on tablets.

---

## Phase 5: User Data, Reading Plans & JSON Engine
**Goal:** Data sovereignty and structured study.

### Steps:
1. **Local-First Storage:**
   - Implement a `FileManager` using `kotlinx.serialization` to read/write `.json` files in `/userdata/`.
   - Create models for `UserSettings`, `UserProgress`, and `UserHighlight`.
2. **Reading Plan Engine:**
   - Implement a parser for `bible_in_a_year.json`.
   - Create a `ReadingPlanViewModel` to track daily completion.
   - **UI:** A minimalist dashboard with thin 2px progress bars and Era-based grouping.
3. **Semantic Highlighting:**
   - Store highlight ranges in JSON.
   - Render highlights as subtle colored underlines in the `InterlinearReader`.

**Expected Result:** Users can follow a reading plan, track their progress, and save highlights, all stored as local JSON files.

---

## Phase 6: Git Sync Integration
**Goal:** User-owned cloud backup.

### Steps:
1. **JGit Integration:**
   - Add Eclipse JGit dependency.
   - Implement `GitSyncManager` to handle `clone`, `add`, `commit`, and `push`.
2. **Sync Workflow:**
   - Implement authentication via Personal Access Token (stored in encrypted `SharedPreferences`).
   - Create a `SyncWorker` using `WorkManager` to automatically push `/userdata/*.json` to a user-provided Git URL.
3. **Settings UI:**
   - Add fields for Git Repository URL and Access Token.
   - Manual "Sync Now" button with status indicator.

**Expected Result:** The app automatically backs up user progress and highlights to a private GitHub/GitLab repository.
