# Verbum Universalis: Comprehensive User Flow Map

## 1. Global Navigation Architecture
The app follows a **Local-First, Typography-First** navigation model. It uses a `ListDetailPaneScaffold` to adapt between mobile (single-pane) and tablet (two-pane) layouts.

### Primary Routes:
- `Route.Dashboard`: Entry point, daily readings, and plan progress.
- `Route.ReadingCanvas`: Primary bilingual reading experience.
- `Route.InterlinearReader`: Deep linguistic analysis (Greek/Hebrew).
- `Route.ReadingPlans`: JSON-driven plan tracking.
- `Route.Settings`: Git Sync, Theme, and Data management.

---

## 2. Detailed User Flow Maps

### A. The Study Loop (The Core Experience)
`Dashboard` $\rightarrow$ `ReadingCanvas` $\rightarrow$ `InterlinearReader` $\rightarrow$ `StudyInspector`

1. **Accessing Scripture:**
   - User opens `Dashboard` $\rightarrow$ Taps a daily reading or selects a book via `BookPicker`.
   - Navigation $\rightarrow$ `ReadingCanvas` (Shows Douay-Rheims or Scío).
2. **Diving Deeper (Linguistic):**
   - User taps a verse in `ReadingCanvas`.
   - Navigation $\rightarrow$ `InterlinearReader` (Full-screen continuous scroll).
   - User taps a specific Greek/Hebrew word.
   - Navigation $\rightarrow$ `StudyInspector` (Right Pane/Bottom Sheet) loads **Lexicon Definition**.
3. **Diving Deeper (Tradition):**
   - User taps the **Tradition Icon** next to a verse in `ReadingCanvas`.
   - Navigation $\rightarrow$ `StudyInspector` loads **Catena Commentary**.

### B. The Reading Plan Loop
`Dashboard` $\rightarrow$ `ReadingPlans` $\rightarrow$ `ReadingCanvas`

1. **Tracking Progress:**
   - User opens `Dashboard` $\rightarrow$ Taps "My Plans".
   - Navigation $\rightarrow$ `ReadingPlans` (Displays Eras and a 2px progress bar).
2. **Executing a Day:**
   - User selects a specific day/reading.
   - Navigation $\rightarrow$ `ReadingCanvas` (Automatically jumps to the assigned chapter/verse).
   - User finishes reading $\rightarrow$ Marks as complete $\rightarrow$ Returns to `ReadingPlans`.

### C. The Data Sovereignty Loop (Git Sync)
`Settings` $\rightarrow$ `GitSyncConfig` $\rightarrow$ `WorkManager (Background)`

1. **Configuration:**
   - User opens `Settings` $\rightarrow$ Taps "Cloud Sync".
   - Input: Private Git Repo URL + Personal Access Token.
2. **Synchronization:**
   - User saves note or highlight in `ReadingCanvas`.
   - System $\rightarrow$ Writes to local `.json` $\rightarrow$ Triggers `WorkManager` $\rightarrow$ JGit commits/pushes to GitHub.

---

## 3. Adaptive Navigation Matrix

| Feature | Mobile Navigation | Tablet Navigation (Split) |
| :--- | :--- | :--- |
| **Lexicon/Catena** | Bottom Sheet (Overlay) | Right Pane (Persistent) |
| **Interlinear** | Full-screen Route | Left Pane (Interlinear) $\rightarrow$ Right Pane (Lexicon) |
| **Reading Plans** | Dedicated Route | Dedicated Route / Dashboard Side-panel |
| **Verse Selection** | Long-press $\rightarrow$ Bottom Drawer | Long-press $\rightarrow$ Right Pane Note Editor |

---

## 4. Phase-Based Navigation Review
- **Phases 1-3 (Foundation):** Established the `ReadingCanvas` and basic route switching.
- **Phase 4 (Interlinear):** Introduced the `InterlinearReader` route and the `StudyInspector` logic.
- **Phase 5 (Plans/Notes):** Integrated `ReadingPlans` route and the Note/Highlight bottom sheet.
- **Phase 6 (Sync):** Added `Settings` configuration for Git synchronization.
