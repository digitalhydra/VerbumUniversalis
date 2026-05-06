# 📋 Phase 3 Specification: Reading Canvas Domain & UI

## 1. Goal
Create a high-legibility, typography-driven reading experience that prioritizes the text while providing deep "study hooks" for scholarly analysis.

## 2. UI & User Experience

### A. The Reading Canvas
- **Rendering Mode:** Verse-by-Verse (each verse starts on a new line).
- **Typography:** `Source Serif Pro`, line-height `1.6x`, no card elevations, 1px hairlines between verses.
- **Layout:** A `LazyColumn` that renders the current chapter.
- **Translation Toggle:** A minimalist toggle in the TopAppBar to switch between Douay-Rheims (EN) and Scío de San Miguel (ES).

### B. Navigation & Discovery
- **Book Picker:** A small menu icon in the TopAppBar that opens a searchable list of books.
- **Quick Jump (Acronym Search):** A text field allowing input like `"Psalm 22:1-3"`.
    - **Logic:** A regex-based parser converts the acronym to `BookID`, `Chapter`, and `VerseRange`.
- **Default Entry:** Boots to the last read passage or the Daily Reading.

### C. Interaction Model
| Action | Trigger | Result |
| :--- | :--- | :--- |
| **Verse Action Menu** | Single Tap | Opens a clean `DropdownMenu` with: <br>1. Highlight <br>2. Note on Verse <br>3. References <br>4. Catena Commentary <br>5. Interlinear Reader |
| **Precision Highlight** | Hold Tap | Enters "Highlight Mode." The text becomes selectable, allowing the user to tag specific word ranges or create notes on a specific phrase. |
| **Quick Nav** | Tap Book/Chapter | Opens the Book Picker. |

## 3. Technical Architecture

### A. ReadingViewModel
- **State Management:** 
    - `currentPassage`: `StateFlow<Passage>` (Book, Chapter, VerseRange).
    - `activeLanguage`: `StateFlow<Language>` (EN/ES).
    - `verses`: `StateFlow<List<Verse>>` (Fetched from `BibleRepository` as a `Flow`).
- **Logic:** Handles the translation toggle and the "Quick Jump" parsing logic.

### B. UI Component Hierarchy
- `ReadingScreen` (Main Scaffold)
    - `TopAppBar` $\rightarrow$ `QuickJumpBar` + `BookPickerIcon` + `LanguageToggle`.
    - `ReadingCanvas` (Stateless)
        - `VerseItem` $\rightarrow$ `Text` $\rightarrow$ `VersePopupMenu`.
- `StudyInspectorHook`: A mechanism to send a "Focus Event" (e.g., `Focus.Reference(verseId)`) to the Tablet Right Pane.

## 4. Integration with Future Phases
- **Phase 4 Hook:** "Interlinear" action in the popup menu navigates to `Route.InterlinearReader`.
- **Phase 4 Hook:** "References" and "Catena" actions send focus events to the `StudyInspector` (Right Pane).
- **Phase 5 Hook:** "Highlight" and "Note" actions trigger the JSON `FileManager` to save user data.

## 5. Acceptance Criteria (Definition of Done)
1. [x] User can navigate to any Book/Chapter via the Picker or Acronym Search.
2. [x] The canvas renders in Verse-by-Verse mode with the correct Serif typography.
3. [x] The translation toggle instantly switches between English and Spanish.
4. [x] Tapping a verse opens the action menu with all 5 requested study hooks.
5. [x] Hold-tapping triggers a visual "Selection Mode" for precision highlighting.
6. [x] The UI is strictly "Bear-style" (no shadows, no cards, high-contrast neutrals).

## 6. Additional Features Implemented
- **Greek Language Support:** Added Greek (EL) as 4th translation option
- **Interlinear Rendering:** When Greek selected, shows word blocks (original + transliteration + literal + morphology)
- **Study Inspector:** Right-side panel with Lexicon (Greek only), Catena, References tabs
- **Unified Note/Highlight:** Single bottom drawer for note + highlight with single save
- **Verse Actions:** Tap verse → menu (Highlight/Note, References, Catena, Interlinear)
- **Selection Mode:** Long press → select verses → bottom drawer for note/highlight
- **Last Read Persistence:** Saves last passage to DataStore
