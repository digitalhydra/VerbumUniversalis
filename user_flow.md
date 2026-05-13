# Verbum Universalis User Flow

## Main Navigation
- Bottom Navigation Bar: Dashboard, Reading, Plans, Settings
- Drawer (hamburger): Language toggle, Sync, About

## Core Flows

### 1. Reading Flow
Dashboard → Reading Canvas (select book/chapter) → Verse interaction
- Tap verse: Action menu (Highlight/Note, References, Catena, Interlinear)
- Long press verse: Selection mode → Bottom drawer (note + highlight)
- Interlinear: Full-screen view with word-by-word analysis (Greek/Hebrew)
- Study Inspector (right pane on tablet, bottom sheet on mobile): 
  * Lexicon (Greek words only)
  * Catena commentary
  * References
  * Notes

### 2. Study Loop
Reading Canvas → Tap verse → Interlinear → Tap Greek word → Study Inspector (Lexicon)
OR
Reading Canvas → Tap Tradition icon → Study Inspector (Catena)

### 3. Reading Plans Flow
Dashboard → Plans → Select plan (e.g., Bible in a Year) → View progress by era
→ Select day → Jump to Reading Canvas for assigned passages
→ Mark complete → Return to Plans

### 4. Data Sync Flow
Settings → Cloud Sync → 
  1. Generate SSH key (if needed)
  2. Copy public key
  3. Add to GitHub/GitLab as Deploy Key (write access)
  4. Enter repo SSH URL
  5. Save & Sync
Background WorkManager handles automatic sync of notes/highlights/reading progress.

### 5. Language & Settings
Dashboard → Language dropdown (EN/ES/LA/EL) → Changes primary canvas
Settings → Theme (Light/Dark), Font size, Notification preferences

## Navigation Adaptation
- Mobile: Single pane, bottom sheets for Study Inspector/Note drawer
- Tablet: Two-pane layout (60% reading, 40% Study Inspector) using ListDetailPaneScaffold