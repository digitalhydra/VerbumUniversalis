# 📋 Phase 4 Specification: Interlinear Engine & Adaptive Layout

## 1. Goal
Implement the scholarly interlinear reading system and a professional two-pane adaptive layout for tablets, allowing for simultaneous reading and deep study.

## 2. Interlinear Reader (The Scholarly View)

### A. The Word-Block Stack
Each word is rendered as a vertical stack using `FlowRow` to ensure a natural, wrapping text flow on all screen sizes.
1. **Original Word:** Serif, Bold, Primary Text Color.
2. **Transliteration:** Sans-Serif, Italic, Muted Gray.
3. **Literal Translation:** Serif, Normal, Primary Text Color.
4. **Morphology:** Sans-Serif, Small, Muted Gray (Controlled by a global toggle).

### B. UI Controls
- **Morphology Toggle:** A global switch in the TopAppBar to show/hide the morphology row across the entire chapter.
- **Minimal Navigation:** A stripped-down version of the Book Picker and Quick Jump search to maximize reading space.

## 3. Adaptive Tablet Layout (The Two-Pane System)

### A. Structure
Using `ListDetailPaneScaffold` (Material 3), the app splits into:
- **Left Pane (60%):** The active `ReadingCanvas` or `InterlinearReader`.
- **Right Pane (40%):** The **Study Inspector**.

### B. The Study Inspector (Right Pane)
The Inspector acts as the "Knowledge Hub" and is organized into **Tabs** for rapid context switching:
1. **Lexicon Tab:** Displays the root definition and grammar of the selected word.
2. **Catena Tab:** A "feed-style" list of commentary from the Church Fathers.
3. **References Tab:** A list of cross-references to other biblical passages.

### C. State & Persistence Logic
- **Context-Aware Reset:** 
    - When switching from `ReadingCanvas` $\rightarrow$ `InterlinearReader` for the *same verse*, the Study Inspector **persists** its state (References/Catena of that verse remain visible) as they provide essential context.
    - When navigating to a *new* verse or chapter, the Inspector resets to the default state of that new passage.
- **Interlinear Interaction:** Tapping a word in the `InterlinearReader` instantly updates the **Lexicon Tab** in the Study Inspector and switches the Inspector's focus to that tab.

## 4. Technical Architecture

### A. StudyInspectorViewModel
- **State Management:**
    - `selectedWord`: `StateFlow<InterlinearWord?>`
    - `activeTab`: `StateFlow<InspectorTab>` (LEXICON, CATENA, REFERENCES).
    - `inspectorContent`: `StateFlow<InspectorState>` (Loading, Success, Empty).
- **Logic:** Orchestrates the data fetching from `BibleRepository` based on the selected word's lemma or the verse's ID.

### B. UI Components
- `InterlinearWordBlock`: Stateless component for the 4-tier word stack.
- `StudyInspector`: A Tabbed layout combining `LexiconView`, `CatenaFeed`, and `ReferencesList`.
- `AdaptiveScaffold`: Manages the `ListDetailPaneScaffold` logic for mobile (single pane) vs tablet (dual pane).

## 5. Acceptance Criteria (Definition of Done)
1. [ ] `FlowRow` correctly wraps interlinear word stacks on mobile and tablet.
2. [ ] Morphology toggle globally hides/shows grammar data in the interlinear view.
3. [ ] Tablet layout correctly displays the Study Inspector in the right pane.
4. [ ] Tapping a word in the interlinear view instantly populates the Lexicon tab in the inspector.
5. [ ] Catena and References are clearly separated into tabs with a "feed" layout for long texts.
6. [ ] Navigation (Picker/Search) remains accessible but minimalist in the interlinear view.
7. [ ] Inspector state persists correctly when switching between canvas and interlinear for the same verse.
