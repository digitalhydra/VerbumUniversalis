# Catechism of the Catholic Church (CCC) — Implementation Plan

## Architecture Decision: Separate Raw SQLite Database

The CCC will use a **separate raw SQLite database** (`verbum_ccc.db`) — NOT added to the existing Room `AppDatabase` / `verbum_seed.db`. This follows the exact same pattern already established by `CatenaRawDatabase` and `CrossRefsRawDatabase`.

**Rationale:**
- Enables native SQLite FTS5 for full-text search (Room FTS4 is limiting)
- Independent schema — CCC can be updated without touching Bible DB migrations
- Consistent with existing architecture (Catena, CrossRefs are separate raw-SQLite DBs)
- Small enough (~2-3 MB) to ship in `assets/` — always available offline

---

## Phase 1: ETL — Build `verbum_ccc.db` (Python)

### 1.1 Source: `raw_data/ccc.json`

**Structure discovered during analysis:**
- 2,865 CCC paragraphs (numbers 1–2865, no gaps)
- 484 TOC nodes (hierarchical, 207 leaf nodes)
- 483 content pages + 1 empty
- Bible references are INLINE in text elements (not pre-indexed)
- ~655+ parenthetical Bible references found via regex
- 62 Bible book abbreviations, 74 non-Bible abbreviations

**Element types in paragraphs:**
- `text` — body text with optional attrs (`b` bold, `i` italic, `href` link)
- `spacer` — layout whitespace
- `ref-ccc` — CCC paragraph number marker (appears at START of paragraph, indicates which CCC paragraph the text belongs to; one paragraph = one CCC number)
- `ref` — footnote mark (not relevant for Bible cross-linking)

### 1.2 ETL Script: `etl/build_ccc_db.py`

**Output:** `app/src/main/assets/verbum_ccc.db`

#### Table 1: `ccc_paragraphs`

```sql
CREATE TABLE ccc_paragraphs (
    number INTEGER PRIMARY KEY,       -- CCC paragraph number (1-2865)
    toc_path TEXT NOT NULL,           -- Hierarchical path, e.g., "PART ONE > SECTION TWO > CHAPTER ONE > Article 3 > I. The Desire for God"
    plain_text TEXT NOT NULL,         -- All text elements concatenated (for search)
    formatted_json TEXT NOT NULL,     -- JSON array of paragraph elements for rich rendering
    search_text TEXT NOT NULL         -- Lowercase plain_text for FTS indexing
);
```

#### Table 2: `ccc_bible_refs`

```sql
CREATE TABLE ccc_bible_refs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ccc_number INTEGER NOT NULL REFERENCES ccc_paragraphs(number),
    book_id INTEGER NOT NULL,         -- Verbum book_id (1-73)
    chapter INTEGER NOT NULL,
    verse_start INTEGER NOT NULL,
    verse_end INTEGER,                -- NULL = single verse, non-null = range end
    ref_text TEXT NOT NULL,           -- Original reference string, e.g., "Gen 1:1"
    ref_position INTEGER NOT NULL,    -- Character offset in plain_text (for inline highlighting)
    ref_length INTEGER NOT NULL       -- Length of reference in plain_text
);
CREATE INDEX idx_ccc_refs_book ON ccc_bible_refs(book_id, chapter, verse_start);
CREATE INDEX idx_ccc_refs_ccc ON ccc_bible_refs(ccc_number);
```

#### Table 3: `ccc_tags` (optional — for user tagging)

```sql
CREATE TABLE ccc_tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ccc_number INTEGER NOT NULL REFERENCES ccc_paragraphs(number),
    tag TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(ccc_number, tag)
);
CREATE INDEX idx_ccc_tags_tag ON ccc_tags(tag);
```

#### Table 4: FTS5 Full-Text Search

```sql
CREATE VIRTUAL TABLE ccc_fts USING fts5(
    number,        -- CCC paragraph number (unindexed, for retrieval)
    toc_path,      -- Breadcrumb hierarchy
    plain_text,    -- Full paragraph text
    content='ccc_paragraphs',
    content_rowid='number'
);

-- Triggers to keep FTS in sync
CREATE TRIGGER ccc_fts_insert AFTER INSERT ON ccc_paragraphs BEGIN
    INSERT INTO ccc_fts(rowid, toc_path, plain_text) VALUES (new.number, new.toc_path, new.plain_text);
END;
CREATE TRIGGER ccc_fts_delete AFTER DELETE ON ccc_paragraphs BEGIN
    INSERT INTO ccc_fts(ccc_fts, rowid, toc_path, plain_text) VALUES ('delete', old.number, old.toc_path, old.plain_text);
END;
```

### 1.3 Bible Abbreviation Mapping

The CCC uses slightly different abbreviations than the existing `BOOK_NAME_TO_ID` map:

| CCC Abbrev | Verbum book_id | Notes |
|-----------|---------------|-------|
| Gen | 1 | Same |
| Ex | 2 | Same |
| Lev | 3 | Same |
| Num | 4 | Same |
| Deut | 5 | Same |
| Josh | 6 | Same |
| Judg | 7 | Same |
| Ruth | 8 | Same |
| 1 Sam | 9 | Same |
| 2 Sam | 10 | Same |
| 1 Kings | 11 | Same |
| 2 Kings | 12 | Same |
| 1 Chr | 13 | Same |
| 2 Chr | 14 | Same |
| Ezra | 15 | Same |
| Neh | 16 | Same |
| Tob | 17 | Same |
| Jdt | 18 | Same |
| **Esth** | 19 | CCC uses "Esth" not "Esther" |
| Job | 20 | Same |
| Ps | 21 | CCC uses "Ps" |
| Prov | 22 | Same |
| **Eccles** | 23 | CCC uses "Eccles" not "Eccl" |
| Song | 24 | Same |
| Wis | 25 | Same |
| Sir | 26 | Same |
| Isa | 27 | Same |
| Jer | 28 | Same |
| Lam | 29 | Same |
| Bar | 30 | Same |
| Ezek | 31 | Same |
| Dan | 32 | Same |
| Hos | 33 | Same |
| Joel | 34 | Same |
| Amos | 35 | Same |
| Obad | 36 | Same |
| Jon | 37 | Same |
| Mic | 38 | Same |
| Nah | 39 | Same |
| Hab | 40 | Same |
| Zeph | 41 | Same |
| Hag | 42 | Same |
| Zech | 43 | Same |
| Mal | 44 | Same |
| **1 Mac, 2 Mac** | 45, 46 | CCC uses "Mac" not "Macc" |
| Mt | 47 | Same |
| Mk | 48 | Same |
| Lk | 49 | Same |
| Jn | 50 | Same |
| Acts | 51 | Same |
| Rom | 52 | Same |
| 1 Cor | 53 | Same |
| 2 Cor | 54 | Same |
| Gal | 55 | Same |
| Eph | 56 | Same |
| Phil | 57 | Same |
| Col | 58 | Same |
| 1 Thess | 59 | Same |
| 2 Thess | 60 | Same |
| 1 Tim | 61 | Same |
| 2 Tim | 62 | Same |
| Tit | 63 | Same |
| Philem | 64 | Same |
| Heb | 65 | Same |
| Jas | 66 | Same |
| 1 Pet | 67 | Same |
| 2 Pet | 68 | Same |
| 1 Jn | 69 | Same |
| 2 Jn | 70 | Same |
| 3 Jn | 71 | Same |
| Jude | 72 | Same |
| Rev | 73 | Same |

### 1.4 Reference Parsing Regex

Bible references in CCC text appear in parenthetical form:

```
Pattern: (?:cf\.?:?\s*)?([1-3]?\s*(?:Gen|Ex|Lev|Num|Deut|Josh|Judg|Ruth|1\s*Sam|...|Rev))\s+(\d+):(\d+(?:-\d+)?)
```

Key variations found in the data:
- `(Gen 1:1)` — simple reference
- `(cf. Mt 5:17)` — "confer" prefix
- `(cf.: Mt 5:33)` — "confer" with colon
- `(cf. Mt 5:17-19)` — verse range
- `(I Jn 4:10)` — "I" prefix for John epistles
- `(Eph 4:13)` — extra whitespace
- `(Rom 8:29; cf. Gal 4:19)` — multiple refs in one parenthetical (semicolon-separated)

False positives to filter:
- `(cf.: St. Leo the Great, Sermo 51, 3: PL 54, 310C)` — Church Father citation, not Bible
- `(GS 19 § 1)` — Vatican II document reference
- Non-Bible abbreviations from `ccc_refs.other`

### 1.5 `formatted_json` Structure

For rich text rendering in Compose, each paragraph's elements are serialized as a JSON array:

```json
[
  {"type": "ref-ccc", "ref_number": 456},
  {"type": "text", "text": "Mary 'remained a virgin...", "attrs": {"b": true}},
  {"type": "text", "text": "Chapter One", "attrs": {"i": true}},
  {"type": "text", "text": "), then the divine...", "attrs": {}},
  {"type": "ref", "number": 1},
  {"type": "text", "text": "...", "attrs": {"b": true, "href": "..."}},
  {"type": "bible-ref", "book_id": 49, "chapter": 1, "verse_start": 38, "ref_text": "Lk 1:38", "position": 245, "length": 8}
]
```

During ETL, inline Bible references are detected and replaced with `bible-ref` element types, storing the resolved book_id/chapter/verse for direct navigation.

---

## Phase 2: Database Access Layer (Raw SQLite)

### 2.1 `data/db/CatechismRawDatabase.kt`

Follows the exact pattern of `CatenaRawDatabase` / `CrossRefsRawDatabase`:

```kotlin
class CatechismRawDatabase private constructor(private val db: SQLiteDatabase?) {

    // === PARAGRAPH QUERIES ===

    fun getParagraph(number: Int): CccParagraphEntity? { ... }
    fun getParagraphsInRange(start: Int, end: Int): List<CccParagraphEntity> { ... }
    fun getNextParagraph(currentNumber: Int): CccParagraphEntity? { ... }
    fun getPreviousParagraph(currentNumber: Int): CccParagraphEntity? { ... }

    // === BIBLE REFERENCE QUERIES ===

    fun getBibleRefsForParagraph(cccNumber: Int): List<CccBibleRefEntity> { ... }

    // Reverse: which CCC paragraphs reference a given verse?
    fun getCccRefsForVerse(bookId: Int, chapter: Int, verse: Int): List<CccParagraphEntity> { ... }

    // === FTS SEARCH ===

    fun search(query: String, limit: Int = 20): List<CccSearchResult> { ... }
    // Uses FTS5: SELECT number, toc_path, snippet(ccc_fts, 2, '<b>', '</b>', '...', 40) 
    //              FROM ccc_fts WHERE ccc_fts MATCH ? ORDER BY rank LIMIT ?

    // === TAG QUERIES ===

    fun addTag(cccNumber: Int, tag: String) { ... }
    fun removeTag(cccNumber: Int, tag: String) { ... }
    fun getTagsForParagraph(cccNumber: Int): List<String> { ... }
    fun getAllUniqueTags(): List<String> { ... }
    fun getParagraphsByTag(tag: String): List<CccParagraphEntity> { ... }

    // === TOC QUERIES ===

    fun getTocTree(): List<CccTocNode> { ... }
    // Returns hierarchical TOC built from toc_nodes/path data

    // === META ===

    fun getTotalParagraphs(): Int { ... }

    companion object {
        private const val DATABASE_NAME = "verbum_ccc.db"

        @Volatile private var INSTANCE: CatechismRawDatabase? = null

        fun getDatabase(context: Context): CatechismRawDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    // First try assets (shipped with app)
                    val dbFile = File(context.filesDir, "databases/$DATABASE_NAME")
                    if (!dbFile.exists()) {
                        // Copy from assets to filesDir/databases/
                        dbFile.parentFile?.mkdirs()
                        context.assets.open(DATABASE_NAME).use { input ->
                            dbFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    val db = SQLiteDatabase.openDatabase(
                        dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
                    )
                    val instance = CatechismRawDatabase(db)
                    INSTANCE = instance
                    instance
                }
            }
        }

        fun invalidate() { INSTANCE?.close(); INSTANCE = null }
    }
}
```

### 2.2 Data Classes (`data/entities/CatechismEntities.kt`)

```kotlin
data class CccParagraphEntity(
    val number: Int,
    val tocPath: String,
    val plainText: String,
    val formattedJson: String
)

data class CccBibleRefEntity(
    val id: Int,
    val cccNumber: Int,
    val bookId: Int,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int?,
    val refText: String,
    val refPosition: Int,
    val refLength: Int
)

data class CccSearchResult(
    val number: Int,
    val tocPath: String,
    val snippet: String  // FTS5 snippet with <b> highlights
)

data class CccTocNode(
    val id: String,
    val title: String,
    val indentLevel: Int,
    val children: List<CccTocNode> = emptyList(),
    val cccStartNumber: Int? = null,  // First CCC paragraph in this section
    val cccEndNumber: Int? = null     // Last CCC paragraph in this section
)
```

---

## Phase 3: Repository Layer

### 3.1 `data/repository/CatechismRepository.kt`

```kotlin
@Singleton
class CatechismRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun db(): CatechismRawDatabase = CatechismRawDatabase.getDatabase(context)

    fun getParagraph(number: Int): CccParagraphEntity?
    fun getBibleRefsForParagraph(cccNumber: Int): List<CccBibleRefEntity>
    fun getCccRefsForVerse(bookId: Int, chapter: Int, verse: Int): List<CccParagraphEntity>
    fun getTocTree(): List<CccTocNode>

    // Search
    fun search(query: String, limit: Int = 20): List<CccSearchResult>

    // Tags
    fun addTag(cccNumber: Int, tag: String)
    fun removeTag(cccNumber: Int, tag: String)
    fun getTagsForParagraph(cccNumber: Int): List<String>
    fun getAllUniqueTags(): List<String>
    fun getParagraphsByTag(tag: String): List<CccParagraphEntity>

    // For StudyInspector integration
    data class CccReference(
        val cccNumber: Int,
        val tocPath: String,
        val excerpt: String  // First ~120 chars of paragraph
    )
    fun getCccReferencesForVerse(bookId: Int, chapter: Int, verse: Int): List<CccReference>
}
```

### 3.2 DI Wiring

Add to `RepositoryModule.kt`:
```kotlin
@Provides @Singleton
fun provideCatechismRepository(@ApplicationContext context: Context): CatechismRepository {
    return CatechismRepository(context)
}
```

---

## Phase 4: ViewModel

### 4.1 `ui/catechism/CatechismViewModel.kt`

```kotlin
@HiltViewModel
class CatechismViewModel @Inject constructor(
    private val catechismRepository: CatechismRepository
) : ViewModel() {

    // Reading state
    private val _currentParagraph = MutableStateFlow<CccParagraphEntity?>(null)
    val currentParagraph: StateFlow<CccParagraphEntity?> = _currentParagraph

    private val _bibleRefs = MutableStateFlow<List<CccBibleRefEntity>>(emptyList())
    val bibleRefs: StateFlow<List<CccBibleRefEntity>> = _bibleRefs

    // TOC state (eager-loaded, small: 484 nodes)
    private val _tocTree = MutableStateFlow<List<CccTocNode>>(emptyList())
    val tocTree: StateFlow<List<CccTocNode>> = _tocTree

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<CccSearchResult>>(emptyList())
    val searchResults: StateFlow<List<CccSearchResult>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    // Tags state
    private val _currentTags = MutableStateFlow<List<String>>(emptyList())
    val currentTags: StateFlow<List<String>> = _currentTags

    private val _allTags = MutableStateFlow<List<String>>(emptyList())
    val allTags: StateFlow<List<String>> = _allTags

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _tocTree.value = catechismRepository.getTocTree()
            _allTags.value = catechismRepository.getAllUniqueTags()
        }
        // Start at paragraph 1
        selectParagraph(1)
    }

    fun selectParagraph(number: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentParagraph.value = catechismRepository.getParagraph(number)
            _bibleRefs.value = catechismRepository.getBibleRefsForParagraph(number)
            _currentTags.value = catechismRepository.getTagsForParagraph(number)
        }
    }

    fun navigateNext() {
        val current = _currentParagraph.value?.number ?: return
        selectParagraph(current + 1)
    }

    fun navigatePrev() {
        val current = _currentParagraph.value?.number ?: return
        if (current > 1) selectParagraph(current - 1)
    }

    fun search(query: String) { ... }
    fun addTag(tag: String) { ... }
    fun removeTag(tag: String) { ... }
}
```

---

## Phase 5: UI — CatechismScreen

### 5.1 `CatechismScreen.kt` — Complete Redesign

**Current:** Empty placeholder showing "Catechism - Feature Coming Soon"

**New layout (phone):**
```
┌─────────────────────────┐
│ ← Catechism    🔍  ⋮   │  Top bar
├─────────────────────────┤
│ PART ONE > SECTION TWO  │  Breadcrumb
│ > CHAPTER TWO           │
│                         │
│ ¶ 456                   │  CCC paragraph number
│                         │
│ Mary "remained a        │
│ virgin in conceiving    │  Formatted text body
│ her Son, a virgin in    │  (bold, italic, Bible
│ giving birth to him..." │   refs as tappable links)
│                         │
│ 📖 Lk 1:38              │  Bible reference chips
│                         │
├─────────────────────────┤
│  ← Prev    ¶456/2865 →  │  Navigation bar
└─────────────────────────┘
```

**Components:**

1. **Top Bar:** Back button, title, search icon, overflow menu (TOC, tags)
2. **Breadcrumb:** Tappable TOC path (tapping opens TOC drawer)
3. **Paragraph Body:** Rich text rendered from `formatted_json`:
   - `text` elements with `b: true` → bold
   - `text` elements with `i: true` → italic  
   - `bible-ref` elements → tappable colored spans
   - `ref-ccc` → small superscript CCC cross-reference (tappable)
4. **Reference Panel:** Expandable section below paragraph showing Bible references as chips
5. **Navigation Bar:** Previous/Next arrows + paragraph counter

### 5.2 TOC Drawer

Slide-in from left (or bottom sheet on phone):

```
┌────────────────────┐
│ Catechism Contents  │
│                     │
│ APOSTOLIC LETTER    │
│   Prologue          │
│     I. The Life...  │
│     II. Handing...  │
│ PART ONE            │
│   SECTION ONE       │
│     CHAPTER ONE     │
│       I. Desire...  │  ← Indented by level
│       II. Ways...   │
│     CHAPTER TWO     │
│   SECTION TWO       │
│ PART TWO            │
│ ...                 │
└────────────────────┘
```

Tapping a TOC item navigates to the first CCC paragraph in that section.

### 5.3 Search Experience

Search bar (shown when 🔍 tapped):
- FTS5-powered instant search as you type
- Results show: paragraph number, TOC breadcrumb, highlighted snippet
- Tapping a result navigates to that paragraph

### 5.4 Tags Panel

- Bottom section: list of tags for current paragraph
- "Add tag" button → text field + autocomplete from existing tags
- Tap tag to remove (with confirmation)
- "Browse by tag" mode from overflow menu

### 5.5 Bible Reference Navigation

When a Bible reference is tapped:
1. Read `bookId`, `chapter`, `verseStart` from the `bible-ref` element
2. Call `onNavigateToBibleRef(bookId, chapter, verseStart)`
3. This navigates to `Route.ReadingCanvas.createRoute(bookId, chapter, verseStart)`

---

## Phase 6: Navigation Updates

### 6.1 Route Updates (`Route.kt`)

```kotlin
object Catechism : Route("catechism?paragraph={paragraph}") {
    const val routeWithArgs = "catechism?paragraph={paragraph}"
    fun createRoute(paragraph: Int? = null): String =
        if (paragraph != null) "catechism?paragraph=$paragraph" else "catechism"
}
```

### 6.2 `VerbumNavGraph.kt` Updates

```kotlin
composable(
    route = Route.Catechism.routeWithArgs,
    arguments = listOf(
        navArgument("paragraph") { type = NavType.IntType; defaultValue = -1 }
    )
) { backStackEntry ->
    val paragraph = backStackEntry.arguments?.getInt("paragraph")?.takeIf { it != -1 }
    CatechismScreen(
        initialParagraph = paragraph,
        onBack = { navController.popBackStack() },
        onNavigateToBibleRef = { bookId, chapter, verse ->
            navController.navigate(
                Route.ReadingCanvas.createRoute(bookId, chapter, verse)
            )
        }
    )
}
```

### 6.3 ReadingCanvas → CCC Back-Navigation

When the user navigates from CCC to ReadingCanvas (via Bible ref tap), the ReadingCanvas should show CCC references for the current verse. This is handled in Phase 7 (StudyInspector integration). The back button returns to the CCC at the same paragraph (natural back-stack behavior via NavHost).

---

## Phase 7: StudyInspector — CCC Tab Integration

### 7.1 New Tab: "Catechism"

Add a 4th tab to StudyInspector (currently: Lexicon, Catena, References):

```kotlin
enum class InspectorTab { LEXICON, CATENA, REFERENCES, CATECHISM }
```

### 7.2 `ReadingViewModel.kt` Additions

```kotlin
private val _cccRefsForVerse = MutableStateFlow<List<CccReference>>(emptyList())
val cccRefsForVerse: StateFlow<List<CccReference>> = _cccRefsForVerse

// Load when verse changes
fun loadCccRefsForVerse(bookId: Int, chapter: Int, verseNumber: Int) {
    viewModelScope.launch(Dispatchers.IO) {
        _cccRefsForVerse.value = catechismRepository.getCccReferencesForVerse(bookId, chapter, verseNumber)
    }
}
```

### 7.3 CCC Tab UI

Each CCC reference shows:
```
🏛️  CCC ¶456
    PART ONE > SECTION TWO > CHAPTER TWO
    "Mary remained a virgin in conceiving her Son..."
```
Tapping navigates to `Route.Catechism.createRoute(456)`.

### 7.4 Verse Action Menu

Add "CCC References" item to the verse tap menu (VerseItem.kt dropdown):
- Shows only when CCC references exist for this verse
- Opens StudyInspector with CCC tab selected

---

## Phase 8: Dashboard

The bottom bar already has "Teaching" → `Route.Catechism`. No changes needed.

Future enhancement: Add a "CCC Paragraph of the Day" card to the dashboard.

---

## Phase 9: Testing & Polish

### 9.1 Critical Edge Cases Verified in Data Analysis

- ✅ No paragraphs have multiple `ref-ccc` markers (1:1 mapping)
- ✅ No gaps in CCC numbering (1–2865 continuous)
- ✅ 13 paragraphs have Bible refs but no CCC number (introductory material — skip in index)
- ✅ Bible refs appear only in parenthetical form — no pipe-form refs found in actual Bible patterns
- ✅ Multiple Bible refs per paragraph possible (e.g., `[Col 1:27, Heb 12:3, ...]` in one CCC paragraph)

### 9.2 Bible Reference Edge Cases to Handle in ETL

- `(cf. Mt 5:17-19)` — verse ranges
- `(I Jn 4:10)` — "I" prefix for 1 John (also II Jn, III Jn)
- `(Rom 8:29; cf. Gal 4:19)` — multiple refs separated by semicolons
- `(cf.: Mt 5:33)` — colon after "cf"
- False positive: `(GS 19 § 1)` — Vatican II docs, not Bible
- False positive: `(St. Augustine, Sermo 52...)` — Church Father citations

### 9.3 UI Edge Cases

- First/last paragraph navigation (disable prev at ¶1, next at ¶2865)
- Deep link from ReadingCanvas to a CCC paragraph that the user hasn't scrolled to yet
- Search with empty query → show nothing (not all 2865 paragraphs)
- Tag with special characters (sanitize)

---

## Implementation Order

| # | Task | Files | Effort | Depends on |
|---|------|-------|--------|------------|
| 1 | **ETL script** | `etl/build_ccc_db.py` (new) | Medium | — |
| 2 | **Raw database access** | `CatechismRawDatabase.kt`, `CatechismEntities.kt` | Small | #1 |
| 3 | **Repository + DI** | `CatechismRepository.kt`, update `RepositoryModule.kt` | Small | #2 |
| 4 | **ViewModel** | `CatechismViewModel.kt` | Medium | #3 |
| 5 | **CatechismScreen — reading** | Rewrite `CatechismScreen.kt` | Large | #4 |
| 6 | **TOC drawer** | New `CccTocDrawer.kt` component | Medium | #4 |
| 7 | **Search** | Update `CatechismScreen.kt` + `CatechismViewModel.kt` | Small | #4 |
| 8 | **Tags** | Update `CatechismScreen.kt` + `CatechismViewModel.kt` | Small | #4 |
| 9 | **Bible ref navigation** | Update `CatechismScreen.kt`, `VerbumNavGraph.kt`, `Route.kt` | Small | #5 |
| 10 | **StudyInspector CCC tab** | Update `StudyInspector.kt`, `ReadingViewModel.kt`, `ReadingScreen.kt` | Medium | #3, #9 |
| 11 | **Verse menu item** | Update `VerseItem.kt` | Tiny | #10 |
| 12 | **Polish & edge cases** | Various | Small | All |

---

## Summary of Key Technical Decisions

1. **Separate raw SQLite DB** (`verbum_ccc.db`) — follows Catena/CrossRefs pattern
2. **FTS5 for search** — full-text search of all 2,865 paragraphs
3. **In-asset shipping** — DB copied from `assets/` to `filesDir/databases/` on first launch
4. **formatted_json for rich text** — JSON-serialized paragraph elements with resolved Bible refs
5. **Bible references parsed at ETL time** — not at query time, for performance
6. **User tags table** — simple `ccc_tags` table for personal organization
7. **Bidirectional linking** — CCC → Bible (tappable refs) and Bible → CCC (StudyInspector tab)
