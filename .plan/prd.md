# Verbum Universalis: Complete Master Specification

## 1. Executive Summary

**Verbum Universalis** is a professional-grade, local-first Catholic Bible study application for Android. It bridges the gap between scholarly linguistic analysis and early Church Tradition through a user-owned, privacy-first data model. It features a completely offline-capable core, adaptive tablet layouts, continuous interlinear reading, and a minimalist, typography-driven UI inspired by the Bear notes app.

---

## 2. Data Sourcing & The 4-Tier Strategy

To guarantee reliability, performance, and offline capability without violating copyright laws, the application's data is strictly partitioned into four tiers.

### Tier 1: The Bundled Payload (100% Offline, Free, DRM-less)

This core SQLite database (`verbum_seed.db`) is bundled directly into the APK (~25-35 MB). The app is fully functional and bilingual the moment it is installed.

- **Primary English Canvas:** Douay-Rheims (Challoner Revision).
- **Primary Spanish Canvas:** La Biblia de Scío de San Miguel (Exact literal translation of the Vulgata).
- **Latin Reference:** Clementina Vulgata.
- **Greek Interlinear:** Byzantine Majority Text (Includes full morphology layer for Lexicon mapping).
- **Hebrew Interlinear:** Masoretic Text (Includes full morphology layer).
- **Lexicons:** Root text definitions for Greek and Hebrew (e.g., Strong's/Thayer's).
- **Semantic Tag Legend:** Base 20 adaptive color configurations (`tags_legend.json`).

### Tier 2: The Catena Commentary Engine (Dynamic Cache)

Massive historical texts are cached intelligently to preserve device storage.

- **Source:** Public domain datasets from the Christian Classics Ethereal Library (CCEL) (e.g., Aquinas's _Catena Aurea_, Early Church Fathers).
- **Rolling Cache:** The app automatically downloads and caches commentaries strictly for the daily readings of the current month.
- **Manual Download:** Users can explicitly choose to download full books (e.g., "Gospel of John") for permanent offline access.

### Tier 3: Liturgical Calendars (Lightweight API)

Syncs a lightweight JSON payload every 30 days to drive the daily reading dashboard and the Tier 2 auto-cache.

- **US (USCCB):** Parsed from official USCCB JSON/RSS feeds.
- **Colombia (CEC):** Static JSON payload maintained via custom ETL scraping.
- **Rome (General Roman):** Sourced from the open-source `calapi.inadiutorium.cz` REST API.

### Tier 4: Proprietary Expansions (Future Roadmap)

Copyrighted texts are treated as opt-in modules utilizing external license authentication (BYOL - Bring Your Own License) or In-App Purchases to avoid restricting the core open application.

- **Modern Translations:** Biblia de Jerusalén.
- **Modern Commentary:** Ignatius Study Bible.

---

## 3. UI/UX & Design System

The app utilizes a "Typography-First Functionalism" aesthetic, heavily inspired by minimalist editors like Bear. The UI gets completely out of the way of the Scripture.

### Core Aesthetic Rules

- **Strictly Forbid:** Drop shadows, heavy card elevations, thick borders, gradients, and skeuomorphic textures (no parchment backgrounds).
- **Whitespace & Hairlines:** Use generous padding, margin, and 1px hairlines (`#EAEAEA` or `#3A3A3C`) for separation.
- **Colors:** High-contrast neutral palettes. Pure white (`#FFFFFF`) or pure black (`#000000`) backgrounds.

### Dual-Typography System

- **Content Font (Serif):** `Source Serif Pro`, `Lora`, or `Merriweather`. Used for all biblical verses and commentary. High legibility, generous line height (1.5x - 1.6x).
- **UI/Metadata Font (Sans-Serif):** `Inter` or `Roboto`. Used for Top App Bars, interlinear grammar tags, and semantic tag labels.

### Continuous Polyglot / Interlinear Mode

Tapping a verse in the primary reading canvas navigates to a dedicated full-screen Interlinear Reader (Vertical Scroll).

- **No Boxes/Grids:** The UI relies entirely on text hierarchy.
- **Word Block Stack:** 1. _Original Word_ (Greek/Hebrew) - Serif, Black/White, Large. 2. _Transliteration_ - Sans-Serif, Italic, Muted Gray. 3. _Literal Translation_ - Serif, Normal Text Color. 4. _Grammar/Morphology_ - Sans-Serif, Muted Gray, Smallest.
- **FlowRow implementation:** Word blocks dynamically wrap to the next line on mobile without breaking vertical alignment.

### Adaptive Tablet Layout (Two-Pane split)

Utilizes Jetpack Compose `ListDetailPaneScaffold` for foldable/tablet optimization.

- **Left Pane (60%):** The continuous reading canvas or interlinear scroll.
- **Right Pane (40%):** The Study Inspector. Replaces bottom sheets; dynamically displays Lexicon definitions, Catena commentary, or Note-taking fields.

### Semantic Highlighting

- **Tags:** Rendered as clean, borderless geometric pills with highly desaturated background tints and crisp sans-serif text.
- **Text Highlight:** In interlinear mode, highlights are rendered as subtle colored underlines or borders to preserve transliteration legibility.

---

## 4. Reading Plan Engine (JSON Driven)

A native, royalty-free engine for structured reading plans, designed to accommodate chronologies like the "Salvation History" timeline.

- **Data Structure:** Powered entirely by local JSON files (e.g., `bible_in_a_year.json`) located in `/userdata/reading_plans/`.
- **Timeline Support:** Groups days by historical eras (e.g., "Early World", "Patriarchs", "Divided Kingdom"). Mixes narrative texts with wisdom literature (Psalms).
- **Tracker UI:** A minimalist dashboard tracking completion via a thin 2px progress bar, grouped by Era, linking directly to the reading canvas.

---

## 5. File System, Data Sovereignty & Git Sync

The application operates on a Local-First architecture. User data is never locked in a proprietary database.

- **Data Format:** All user preferences, highlights, notes, and reading progress are stored as flat `.json` files in the local Android filesystem (`/userdata/`).
- **Git Sync:** Eclipse JGit commits and pushes changes to a user-provided private Git repository URL (via Personal Access Token).
- **WorkManager:** Handles automatic background synchronization.

---

## 6. AI Coding Agent Directives

Strict guardrails for AI coding assistants to ensure architectural integrity.

**Agent Persona:** Expert Senior Android Engineer specializing in Clean Architecture, Kotlin 1.9+, and Jetpack Compose.
**Rules of Engagement:**

1. Do NOT wander out of scope. Implement ONLY the requested phase/step.
2. Do NOT hallucinate features.
3. Do NOT mock data unless explicitly commanded; build actual Room DB schemas.
4. Strict State Hoisting in Compose. Keep UI pure and stateless. Use `StateFlow`.
5. Use Hilt exclusively for Dependency Injection.

**Implementation Roadmap:**

- **Phase 1:** Project Scaffolding & Core Architecture (Hilt, Compose BOM, Theme).
- **Phase 2:** The Tier 1 Database Payload (Room Entities, DAO Flows).
- **Phase 3:** Reading Canvas Domain & UI (Douay-Rheims / Scío Toggle).
- **Phase 4:** Interlinear Engine (`FlowRow` UI, Tablet `ListDetailPaneScaffold`).
- **Phase 5:** User Data & Reading Plans (Kotlin Serialization, JSON Engine, Minimalist UI).
- **Phase 6:** Git Sync Integration (JGit, WorkManager).
