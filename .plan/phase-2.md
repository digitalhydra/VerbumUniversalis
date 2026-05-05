# 📋 Phase 2 Specification: Tier 1 Database Payload & Room Layer

## 1. Goal

Implement the offline-first data core, including the schema, the ETL process for database creation, and the Room persistence layer.

## 2. Data Architecture (Normalized)

### A. SQL Schema

The database will use a normalized structure to allow for easy expansion of translations and scholarly data.

| Table                   | Columns                                                                                                   | Purpose                            |
| :---------------------- | :-------------------------------------------------------------------------------------------------------- | :--------------------------------- |
| **`books`**             | `id` (PK), `name_en`, `name_es`, `name_la`, `testament`                                                   | Book metadata.                     |
| **`verses`**            | `id` (PK), `book_id` (FK), `chapter`, `verse_number`                                                      | The unique anchor for every verse. |
| **`texts`**             | `id` (PK), `verse_id` (FK), `lang_code`, `content`                                                        | Verse text (DR, SCIO, VL, etc.).   |
| **`interlinear_words`** | `id` (PK), `verse_id` (FK), `word_order`, `original`, `transliteration`, `literal`, `morphology`, `lemma` | Word-by-word morphology.           |
| **`lexicon`**           | `lemma` (PK), `language`, `definition`                                                                    | Lexicon root definitions.          |

### B. The ETL Process (Extract, Transform, Load)

Since no seed database exists, an ETL pipeline will be developed:

1. **Sources:** Raw text/JSON files for Douay-Rheims, Scío de San Miguel, Vulgata Clementine, Byzantine Greek, and Masoretic Hebrew.
2. **Parser:** A script (Kotlin/Python) to normalize these sources into the schema above.
3. **Generator:** Create the `verbum_seed.db` SQLite file.
4. **Placement:** The resulting `.db` file will be placed in `src/main/assets/`.

### C. Lexicon Selection

To meet the "professional-grade scholarly" objective, we will implement a **Lemma-based Lexicon system**. We will prioritize **Strong's numbers** as the primary mapping key (as they are the industry standard for linking morphology to definitions) while allowing for expanded definitions from **Thayer's** or **BDAG** where possible.

## 3. Room Implementation

### A. Entities & DAOs

- **Entities:** Direct mapping of the SQL tables to Kotlin `@Entity` classes.
- **DAOs:**
  - `VerseDao`: `fun getChapter(bookId: Int, chapter: Int): Flow<List<VerseWithTexts>>`
  - `InterlinearDao`: `fun getWordsForVerse(verseId: Int): Flow<List<InterlinearWord>>`
  - `LexiconDao`: `fun getDefinition(lemma: String): Flow<LexiconEntry?>`

### B. Database Configuration

- **Pre-population:** Use `RoomDatabase.Callback` and `.createFromAsset("verbum_seed.db")` to ensure the app is functional immediately upon installation.
- **Threading:** All Room operations will use `Dispatchers.IO` via Coroutines.

## 4. Acceptance Criteria (Definition of Done)

1. [ ] ETL script successfully generates `verbum_seed.db` without data loss.
2. [ ] `verbum_seed.db` is bundled in assets and successfully loaded by Room.
3. [ ] `BibleRepository` can fetch a chapter of text as a `Flow` and emit it to a ViewModel.
4. [ ] Lexicon lookup works by passing a `lemma` from the interlinear table.
5. [ ] Database size is kept lean (no redundant metadata).
