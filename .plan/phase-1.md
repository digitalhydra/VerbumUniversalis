# 📋 Phase 1 Specification: Project Scaffolding & Core Architecture

## 1. Technical Foundation
- **Package Name:** `com.verbum.universalis`
- **Minimum SDK:** 26 (Android 8.0)
- **Language:** Kotlin 1.9+
- **Core Stack:**
    - **UI:** Jetpack Compose (Latest Stable BOM), Material 3.
    - **DI:** Hilt (`dagger.hilt.android`).
    - **Navigation:** Navigation Compose.
    - **Storage:** Room (Architecture setup only).
    - **Serialization:** Kotlinx Serialization JSON.

## 2. Design System (The "Bear" Aesthetic)

### A. Color Palette
| Role | Light Mode | Dark Mode (Deep Charcoal) |
| :--- | :--- | :--- |
| **Background** | `#FFFFFF` (Pure White) | `#121212` (Deep Charcoal) |
| **Surface** | `#FAFAFA` (Off-White) | `#1E1E1E` (Dark Gray) |
| **Primary (Accent)**| `#D9CFCC` (Light Gold) | `#D9CFCC` (Light Gold) |
| **Outline/Hairline**| `#EAEAEA` (Soft Gray) | `#3A3A3C` (Muted Charcoal) |
| **Text (Primary)** | `#000000` | `#FFFFFF` |
| **Text (Secondary)**| `#666666` | `#B0B0B0` |

### B. Typography (Bundled Assets)
- **Content Font:** `SourceSerifPro` (Serif)
    - Applied to: Biblical verses, commentaries.
    - Spec: Line-height `1.6x`, generous letter spacing.
- **UI Font:** `Inter` (Sans-Serif)
    - Applied to: App bars, buttons, tags, settings, metadata.
    - Spec: Medium weight for labels, Regular for system text.

### C. Visual Rules
- **No Elevations:** `elevation = 0.dp` for all components.
- **No Heavy Cards:** Use `1px` hairlines (`#EAEAEA` / `#3A3A3C`) instead of shadows or borders.
- **Whitespace:** Use standard 16dp/24dp padding to create a "breathable" layout.

## 3. Navigation Architecture

**Root Navigation Graph:**
- `Route.Dashboard` (Default Entry): 
    - Section 1: Daily Liturgical Readings (Today's verse).
    - Section 2: Active Reading Plan progress.
- `Route.ReadingCanvas`: The main reading experience.
- `Route.InterlinearReader`: The word-by-word scholarly view.
- `Route.ReadingPlans`: Management of reading plan JSONs.
- `Route.Settings`: App preferences and Git Sync config.

## 4. Proposed Folder Structure (Clean Architecture)
```text
com.verbum.universalis/
├── core/
│   ├── di/              # Hilt Modules (AppModule, DatabaseModule)
│   ├── theme/           # Color.kt, Type.kt, Theme.kt
│   └── util/            # File managers, Constants
├── data/
│   ├── local/           # Room DB, DAOs, Entities
│   └── repository/      # BibleRepository implementation
├── domain/
│   ├── model/           # Domain entities (Verse, Word)
│   └── repository/      # Interface definitions
└── ui/
    ├── components/      # Reusable atoms (Hairline, GoldButton)
    ├── dashboard/       # Dashboard screen & ViewModel
    ├── reader/          # ReadingCanvas & InterlinearReader
    ├── plans/           # ReadingPlan screens
    └── settings/        # Settings screens
```

## 5. Acceptance Criteria (Definition of Done)
1. [ ] App compiles and runs on SDK 26+.
2. [ ] `VerbumTheme` correctly switches between Light and Deep Charcoal modes.
3. [ ] Bundled fonts (`Source Serif Pro` and `Inter`) are applied globally.
4. [ ] The app boots directly into the `Dashboard`.
5. [ ] Hilt is successfully injecting a singleton `AppModule`.
6. [ ] Navigation between the 5 main routes is functional (even if screens are currently skeletons).
