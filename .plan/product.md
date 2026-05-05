# Verbum Universalis: Complete Project History & Master Specification

## 1. Executive Summary & Project Genesis

**Verbum Universalis** is a professional-grade, local-first Catholic Bible study application for Android. It bridges the gap between scholarly linguistic analysis and early Church Tradition through a user-owned, privacy-first data model.

This document serves as both the definitive technical specification and a historical record of the architectural and design decisions made during its conception.

---

## 2. The Evolution of the Reading Experience

### 2.1 The Interlinear Pivot: From Overlays to Continuous Routes

- **Initial Concept:** The interlinear translation was originally conceived as a quick look-up drawer or modal popup.
- **The Discussion:** We realized that a drawer breaks the flow of study. If a user wants to check previous or next verses, they are trapped in a modal.
- **The Decision:** We transformed the Interlinear feature into a dedicated, full-screen continuous route (`Route.InterlinearReader`). It uses a `LazyColumn` to load the entire chapter.
- **Technical Implementation:** Inside this list, Jetpack Compose's `FlowRow` is used to render the word blocks (Original, Transliteration, Translation, Morphology). `FlowRow` allows the stacked data to dynamically wrap to the next line on mobile devices as they hit the edge of the screen, preserving the grammatical alignment.

### 2.2 Tablet Optimization: The Two-Pane Architecture

- **The Discussion:** Simply stretching a mobile app on a tablet wastes valuable screen real estate.
- **The Decision:** We adopted a Two-Pane Adaptive Layout using Jetpack Compose's `ListDetailPaneScaffold` (Material 3).
- **Technical Implementation:**
  - **Left Pane (60%):** The primary reading canvas or continuous interlinear scroll.
  - **Right Pane (40%):** The "Study Inspector." This replaces all bottom sheets and modals. Tapping a Greek word in the Left Pane instantly loads its Lexicon definition in the Right Pane. Tapping the Tradition icon loads the Catena commentary in the Right Pane, keeping the context unbroken.

---

## 3. UI/UX & Design Philosophy: The "Bear App" Pivot

### 3.1 Rejecting Skeuomorphism

- **Initial Concept:** An older, "parchment" manuscript aesthetic was suggested to evoke the feeling of historical texts.
- **The Discussion:** The user rejected this, noting that for a tool meant for deep, prolonged study—especially one dealing with dense morphological data—forcing a theme gets in the way. The goal was something "useful and nice, clear and minimal."
- **The Decision:** We pivoted to **Typography-First Functionalism**, heavily inspired by modern markdown editors like the _Bear_ notes app.

### 3.2 The Minimalist Design System

- **Strictly Forbidden:** Drop shadows, heavy card elevations, thick borders, gradients, and skeuomorphic textures.
- **Dual-Typography System:** \* _Content Font (Serif):_ `Source Serif Pro`, `Lora`, or `Merriweather` for high legibility in long biblical texts (1.5x - 1.6x line height).
  - _UI/Metadata Font (Sans-Serif):_ `Inter` or `Roboto` for App Bars, grammar tags, and system controls. This creates a sharp boundary between "what I am reading" and "how the app works."
- **Semantic Tagging:** Replaced messy highlighter strokes with clean, borderless geometric pills with highly desaturated background tints. In interlinear mode, highlights are rendered as subtle colored underlines or borders to preserve transliteration legibility without cluttering the screen.

---

## 4. Data Sourcing & The Tier 1 Offline Strategy

A major portion of development was dedicated to making the app 100% offline, free, and DRM-less from the moment of installation, without violating copyright laws.

### 4.1 The Spanish Translation Dilemma

- **The Problem:** The modern _Biblia de Jerusalén_ requires DRM and commercial licensing (Tier 4).
- **The Discussion:** We sought a public domain Spanish alternative translated directly from the Latin Vulgata (to parallel the English Douay-Rheims). The choices were _Torres Amat_ (tradition-infused, dynamic) or _Scío de San Miguel_ (strictly literal).
- **The Decision:** We selected **La Biblia de Scío de San Miguel**. It perfectly maps word-for-word to the Latin Vulgata, enabling exact 1:1 structural comparisons in Polyglot mode.

### 4.2 The Greek Text Dilemma

- **The Problem:** The academic standard _Nestle-Aland (NA28)_ is copyrighted.
- **The Discussion:** We compared public domain alternatives like Tischendorf 8th Edition and the Byzantine Majority Text. Tischendorf (based on Alexandrian texts) omits verses found in the Latin tradition (e.g., Mark 16 ending, John 8 adulteress, Comma Johanneum).
- **The Decision:** We selected the **Byzantine Majority Text**. The Latin Vulgate aligns closely with the Byzantine tradition. If a user is reading the Douay-Rheims or Scío and opens the interlinear, the Greek words will actually be there, preventing broken UX and maintaining theological consistency.

### 4.3 The Morphology/Grammar Layer Debate

- **The Problem:** Could we save database space by dropping the Greek grammar layer?
- **The Discussion:** Dropping it saves only ~1MB in the final APK. However, it completely breaks the Lexicon lookup engine (which relies on morphology to trace conjugated words back to their root lemmas) and reduces the app to a simple word-matcher.
- **The Decision:** We kept the morphology data in the Room database. If the UI feels cluttered, the grammar row can be hidden via a UI toggle, but the data must remain to power the study tools.

### 4.4 The Final 4-Tier Data Architecture

- **Tier 1 (Bundled in APK, 100% Offline):** Douay-Rheims, Scío de San Miguel, Clementina Vulgata, Byzantine Majority Text (with morphology), Masoretic Hebrew (with morphology), Lexicon definitions.
- **Tier 2 (Rolling Cache):** Catena Commentaries (Church Fathers) sourced from CCEL. Auto-downloads for the current month's daily readings.
- **Tier 3 (Lightweight API):** Daily Liturgical Calendars (USCCB, CEC Colombia, Rome).
- **Tier 4 (Proprietary Modules):** Future BYOL (Bring Your Own License) support for copyrighted study bibles.

---

## 5. The Reading Plan Engine & Copyright Solutions

### 5.1 The Jeff Cavins Timeline

- **The Problem:** We wanted to integrate the "Great Adventure Bible Timeline" by Jeff Cavins, but it is heavily trademarked and copyrighted by Ascension Press.
- **The Discussion:** The underlying theological concept—reading the 14 narrative books of the Bible to understand salvation history—is universal Catholic tradition and cannot be copyrighted.
- **The Decision:** We built a generic, royalty-free **JSON Reading Plan Engine**.
- **The Result:** We designed a 365-day "Bible in a Year (Chronological)" plan, broken into 12 historical periods (Early World, Patriarchs, Exodus, etc.), mixing narrative texts with Psalms. The UI is minimalist, using a thin progress line and grouping days cleanly by Era. Users can also drop their own custom `.json` files into the app to track any personal plan.

---

## 6. File System, Data Sovereignty & Git Sync

- **Local-First:** User data is never locked in a proprietary database. Highlights, notes, and reading progress are stored as flat `.json` files in the local Android filesystem (`/userdata/`).
- **Git Sync Integration:** Instead of a proprietary cloud, the app uses Eclipse JGit to commit and push changes to a user-provided private Git repository URL (e.g., GitHub, GitLab) via a Personal Access Token.
- **Background Processing:** Android WorkManager handles automatic synchronization seamlessly.

---

## 7. AI Coding Agent Directives

To prevent the AI from "hallucinating" features, drifting into old Android paradigms, or mocking data unnecessarily, the following strict guardrails were established for the implementation phase:

- **Persona:** Expert Senior Android Engineer specializing in Clean Architecture, Kotlin 1.9+, and Jetpack Compose.
- **State Management:** Use `StateFlow` in ViewModels. Strict State Hoisting in Compose (keep UI components stateless).
- **Dependency Injection:** Use Hilt exclusively (`@HiltViewModel`).
- **Database:** Use Room with Coroutines/Flows. All DAO queries returning lists must return `Flow<List<T>>`.
- **Theming:** Use Compose Material 3 `dynamicLightColorScheme` and `dynamicDarkColorScheme`.
- **Execution Constraint:** The AI must build step-by-step and WAIT for the user to trigger the next phase (Phases 1 through 6).
