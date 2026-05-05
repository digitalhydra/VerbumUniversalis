# 📋 Phase 5 Specification: User Data, Reading Plans & JSON Engine

## 1. Goal
Implement the "Local-First" data architecture, ensuring user-owned sovereignty over highlights and notes, and a structured engine for following biblical reading plans.

## 2. Data Sovereignty (Local-First)

### A. The JSON Storage Model
User data is stored as flat `.json` files in the app's internal storage (`/userdata/`), ensuring data is portable and not locked in a database.
- `settings.json`: Theme preferences, language, and last read position.
- `highlights.json`: List of highlight objects (VerseID, Range, ColorID, Timestamp).
- `notes.json`: List of notes (VerseID/Range, Content, Timestamp).
- `progress.json`: Completion status for active reading plans.

### B. The Semantic Highlight Palette
To maintain the "Bear" aesthetic, the app uses a curated palette of **20 desaturated colors**. These are designed to be subtle but clearly contrasting against both Pure White and Deep Charcoal backgrounds.
- **Palette Type:** Muted pastels and earth tones (e.g., Sage, Dusty Rose, Steel Blue, Ochre).
- **Application:** Rendered as subtle colored underlines in the `InterlinearReader` and soft background tints in the `ReadingCanvas`.

### C. Note-Taking Interface (Contextual Separation)
Notes are treated as a separate personal context, distinct from the "Tradition" (Catena/Lexicon).
- **Mobile:** A sliding bottom sheet for creating and editing notes.
- **Tablet (Study Inspector):** A dedicated **"My Notes" Tab** in the right pane. This allows the user to switch between "Church Wisdom" (Catena) and "Personal Insights" (Notes) without leaving the verse.

## 3. Reading Plan Engine

### A. JSON Plan Schema
Reading plans are loaded from local JSON files.
```json
{
  "plan_id": "string",
  "title": "string",
  "eras": [
    { 
      "name": "Era Name", 
      "days": [ 
        { "day": 1, "readings": [{"book": "GEN", "chapter": 1, "verse_start": 1, "verse_end": 31}] } 
      ] 
    }
  ]
}
```

### B. Experience & Interaction
- **Dashboard:** A minimalist view showing the current era and a 2px progress bar.
- **Activation:** Selecting a day in the plan immediately navigates the user to the first reading of that day in the `ReadingCanvas`.
- **Backup Warning:** If Git Sync (Phase 6) is not configured, the app will periodically warn the user that their data is local-only and at risk of loss upon app deletion/device failure.

## 4. Technical Architecture

### A. FileManager (The JSON Engine)
- **Implementation:** A singleton using `kotlinx.serialization`.
- **Operation:** Atomic read/write operations to prevent data corruption during app crashes.
- **Encryption:** Basic internal storage protection (Android's standard internal file system).

### B. ReadingPlanViewModel
- **Logic:** Parses plan JSONs, tracks current day, and calculates the percentage of completion per era.

## 5. Acceptance Criteria (Definition of Done)
1. [ ] `FileManager` successfully reads/writes user notes and highlights as `.json` files.
2. [ ] 20-color desaturated palette is implemented and looks consistent in both Light and Dark modes.
3. [ ] Note-taking is accessible via bottom sheet (mobile) and a dedicated Tab in the Study Inspector (tablet).
4. [ ] Reading Plan dashboard correctly tracks progress and jumps to the first reading of the day.
5. [ ] User is warned about data loss if Git Sync is not active.
6. [ ] User notes are visually and logically separated from the Lexicon and Catena.
