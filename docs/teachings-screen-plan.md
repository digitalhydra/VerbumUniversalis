# Teachings Screen & CST Feature Plan

## Overview

Convert the current single-destination "Teaching" bottom bar item (→ CccTocScreen) into a **Teachings Hub** screen with two options:
1. **CCC** (Catechism of the Catholic Church) – redirects to existing `CccTocScreen`
2. **CST** (Catholic Social Teaching) – new document listing and reader with search

Both options must support EN & ES languages.

---

## 1. Navigation Changes

### Current Flow
```
BottomBar("Teaching") → CccTocScreen
```

### New Flow
```
BottomBar("Teaching") → TeachingsScreen (hub)
                           ├── "Catecismo de la Iglesia Católica" → CccTocScreen (existing)
                           └── "Doctrina Social de la Iglesia"  → CstDocListScreen (new)
                                                                     └── select document → CstReaderScreen (new)
```

### File Changes

**`Route.kt`** – Add 3 new routes:
```kotlin
object Teachings : Route("teachings")                          // Hub screen
object CstDocList : Route("cst_doc_list")                      // Document list
object CstReader : Route("cst_reader/{documentId}/{sectionNumber}") {
    fun createRoute(documentId: Int, sectionNumber: Int = 0): String = "cst_reader/$documentId/$sectionNumber"
}
```

**`MainScreen.kt`** – Change the "Teaching" bottom bar item:
- Route: `Route.Teachings.route` (was `Route.CatechismToc.route`)

**`VerbumNavGraph.kt`** – Add 3 composable destinations:
- `Route.Teachings.route` → `TeachingsScreen`
- `Route.CstDocList.route` → `CstDocListScreen`
- `Route.CstReader.route` → `CstReaderScreen`

**`Layout.kt`** – Keep bottom bar visible on both Teachings hub and CstDocList (not on CstReader — reading screen hides it).

---

## 2. Teachings Hub Screen (New)

A simple card-based selection screen.

**File**: `com.verbum.universalis.ui.teachings.TeachingsScreen.kt`

### UI
- Two large cards side-by-side (or stacked on narrow screens)
- Each card has: icon, title (EN/ES from strings.xml), brief description
- Card 1: "Catechism of the Catholic Church" → navigates to `Route.CatechismToc`
- Card 2: "Catholic Social Teaching" → navigates to `Route.CstDocList`

### State Management
- Stateless — just forwarding navigation callbacks

---

## 3. CST Data Layer

### Data Sources

CST documents are best stored in a **SQLite database** (`verbum_cst.db`) shipped as an asset, mirroring the CCC pattern for consistency and enabling FTS4 search.

#### Database Schema

```sql
-- Document metadata
CREATE TABLE cst_documents (
    id INTEGER PRIMARY KEY,
    document_type TEXT NOT NULL,          -- 'encyclical', 'exhortation', 'letter', 'constitution'
    promulgation_date TEXT,               -- '1891-05-15'
    pope_name TEXT NOT NULL,              -- 'Leo XIII', 'Francis', etc.
    has_en INTEGER DEFAULT 1,
    has_es INTEGER DEFAULT 1
);

-- Sections/chapters within documents  
CREATE TABLE cst_sections (
    id INTEGER PRIMARY KEY,
    document_id INTEGER NOT NULL,
    lang TEXT NOT NULL,                   -- 'en', 'es'
    section_number INTEGER NOT NULL,      -- sequential within document
    title TEXT NOT NULL,                  -- section/chapter title
    toc_path TEXT NOT NULL,               -- hierarchical: "Chapter I > The Nature of Work"
    plain_text TEXT NOT NULL,
    formatted_json TEXT NOT NULL,         -- same format as CCC's formatted_json
    FOREIGN KEY (document_id) REFERENCES cst_documents(id)
);

CREATE INDEX idx_cst_sections_doc_lang ON cst_sections(document_id, lang, section_number);

-- FTS4 for full-text search (same pattern as CCC)
CREATE VIRTUAL TABLE cst_fts USING fts4(
    document_id,
    section_number,
    lang,
    toc_path,
    plain_text,
    tokenize=porter
);
```

### FTS Triggers (same pattern as CCC)
```sql
CREATE TRIGGER cst_fts_insert AFTER INSERT ON cst_sections ...
CREATE TRIGGER cst_fts_delete AFTER DELETE ON cst_sections ...  
CREATE TRIGGER cst_fts_update AFTER UPDATE ON cst_sections ...
```

### Initial CST Documents to Include
| # | Document | Pope | Year | Type |
|---|----------|------|------|------|
| 1 | Rerum Novarum | Leo XIII | 1891 | Encyclical |
| 2 | Quadragesimo Anno | Pius XI | 1931 | Encyclical |
| 3 | Mater et Magistra | John XXIII | 1961 | Encyclical |
| 4 | Pacem in Terris | John XXIII | 1963 | Encyclical |
| 5 | Gaudium et Spes | Vatican II | 1965 | Constitution |
| 6 | Populorum Progressio | Paul VI | 1967 | Encyclical |
| 7 | Laborem Exercens | John Paul II | 1981 | Encyclical |
| 8 | Sollicitudo Rei Socialis | John Paul II | 1987 | Encyclical |
| 9 | Centesimus Annus | John Paul II | 1991 | Encyclical |
| 10 | Caritas in Veritate | Benedict XVI | 2009 | Encyclical |
| 11 | Laudato Si' | Francis | 2015 | Encyclical |
| 12 | Fratelli Tutti | Francis | 2020 | Encyclical |

### EN/ES Support
- Each row in `cst_sections` has a `lang` column — same pattern as `ccc_paragraphs`
- The `cst_documents` table has `has_en`/`has_es` flags for filtering by language
- Content is stored as formatted_json (same format as CCC)

### ETL Script (Python)
- Create `etl/build_cst_db.py` → similar to `build_ccc_db.py`
- Input: JSON source files for each document (EN + ES)
- Output: `verbum_cst.db` with the schema above + FTS index
- The JSON source files should contain paragraph-level formatted_json (same format as the exported CCC JSON)

---

## 4. CST Repository & Database Access

### New file: `CstRawDatabase.kt`
- Same pattern as `CatechismRawDatabase.kt`
- Opens `verbum_cst.db` from assets
- Methods: `getDocuments(lang)`, `getSections(documentId, lang)`, `getSection(sectionId)`, `search(query, lang)`

### New file: `CstRepository.kt`
- Same pattern as `CatechismRepository.kt`
- Singleton, injectable via Hilt
- Wraps `CstRawDatabase`

### DI Module update (`DatabaseModule.kt`)
- Add `@Provides` for `CstRawDatabase` and `CstRepository`

---

## 5. CST Screen Implementations

### 5a. CstDocListScreen (Document Listing)

**File**: `com.verbum.universalis.ui.teachings.CstDocListScreen.kt`

#### UI
- TopAppBar with back button + title ("Catholic Social Teaching" / "Doctrina Social de la Iglesia")
- LazyColumn of document cards showing:
  - Document title (translated by lang)
  - Pope name
  - Year / document type badge
- Tapping a card navigates to `CstReaderScreen` with the document's first section

#### ViewModel: `CstDocListViewModel.kt`
- Injects `CstRepository` + `LanguageManager`
- Loads `cst_documents` filtered by `has_{lang} = 1`
- Exposes `StateFlow<List<CstDocumentUi>>`

### 5b. CstReaderScreen (Document Reader)

**File**: `com.verbum.universalis.ui.teachings.CstReaderScreen.kt`

#### UI
- Renders sections of a CST document — **reuses the same rendering logic** as `CatechismScreen`
- TopAppBar: document title, back button
- Section navigation: prev/next section buttons (bottom bar)
- TOC drawer: slide-out panel showing section/chapter titles (hierarchical from `toc_path`)
- Content area: renders `formatted_json` via `CccComponents` / `ClickableText` (same pattern)

#### ViewModel: `CstReaderViewModel.kt`
- Injects `CstRepository` + `LanguageManager`
- Loads document metadata + sections
- Tracks current section number
- Exposes: `documentTitle`, `sectionTitle`, `elements` (parsed from formatted_json), `tocStructure`, `prevSection`, `nextSection`
- Search functionality (reuses CccSearchDrawer or a similar pattern)

#### Reusing CatechismScreen components:
- The `CccComponents.kt` file already has rendering for `CccElement.Text`, `CccElement.BibleRef`, etc.
- CST sections will use the same `formatted_json` format → same `CccElementJson` parsing → same `CccElement` types
- The `CccSearchDrawer` composable can be adapted or a generic version extracted
- Bible refs in CST sections will use the same `book_id:chapter:verse` annotation → Bible navigation works

---

## 6. Search Across CST Documents

### Implementation
- Uses the same FTS4 approach as CCC search
- `CstRawDatabase.search(query, lang)` runs:
  ```sql
  SELECT s.document_id, s.section_number, s.toc_path, 
         snippet(cst_fts, 3, '<b>', '</b>', '...', 25) as snippet
  FROM cst_fts 
  JOIN cst_sections s ON s.id = cst_fts.docid
  WHERE cst_fts MATCH ?
  ORDER BY rank LIMIT ?
  ```
- Results show: document title (from `cst_documents`), section TOC path, snippet
- Selecting a result navigates to `CstReaderScreen` at the matching section
- Search can be invoked from:
  - **CstDocListScreen**: search icon in top bar → searches across ALL CST documents
  - **CstReaderScreen**: search icon in top bar → searches within current document (or all)

### Search Result Entity
```kotlin
data class CstSearchResultEntity(
    val documentId: Int,
    val sectionNumber: Int,
    val documentTitle: String,
    val tocPath: String,
    val snippet: String
)
```

---

## 7. DI Module Updates

### `DatabaseModule.kt`
```kotlin
@Provides @Singleton
fun provideCstDatabase(@ApplicationContext context: Context): CstRawDatabase {
    return CstRawDatabase.getDatabase(context)
}

@Provides @Singleton
fun provideCstRepository(cstDb: CstRawDatabase): CstRepository {
    return CstRepository(cstDb)
}
```

### `AppModule.kt` — no changes needed

---

## 8. String Resources

### `values/strings.xml` (English)
```xml
<string name="teachings">Teaching</string>
<string name="catechism">Catechism of the Catholic Church</string>
<string name="cst">Catholic Social Teaching</string>
<string name="cst_description">Papal encyclicals and social documents</string>
<string name="cst_search">Search CST documents</string>
<string name="cst_search_results">Search results for \"%1$s\": %2$d</string>
<string name="no_cst_results">No results found for \"%1$s\"</string>
<string name="found_results">Found %1$d results</string>
```

### `values-es/strings.xml` (Spanish)
```xml
<string name="teachings">Enseñanza</string>
<string name="catechism">Catecismo de la Iglesia Católica</string>
<string name="cst">Doctrina Social de la Iglesia</string>
<string name="cst_description">Encíclicas papales y documentos sociales</string>
<string name="cst_search">Buscar en la DSI</string>
<string name="cst_search_results">Resultados para \"%1$s\": %2$d</string>
<string name="no_cst_results">No se encontraron resultados para \"%1$s\"</string>
```

Update existing: Rename `teaching` → `teachings` in both files (or keep `teaching` pointing to the hub).

---

## 9. Implementation Phases

### Phase 1: Navigation & Teachings Hub
1. Add new routes to `Route.kt`
2. Create `TeachingsScreen.kt` (hub with two cards)
3. Update `MainScreen.kt` bottom bar → `Route.Teachings`
4. Update `VerbumNavGraph.kt` → add composable destinations

### Phase 2: CST Database & ETL
1. Create CST JSON source files (EN + ES for each document)
2. Write `etl/build_cst_db.py` to convert JSON → SQLite + FTS
3. Generate `verbum_cst.db` and place in `app/src/main/assets/`
4. Create `CstRawDatabase.kt` (same pattern as `CatechismRawDatabase`)

### Phase 3: CST UI Screens
1. Create `CstDocListScreen.kt` + `CstDocListViewModel.kt`
2. Create `CstReaderScreen.kt` + `CstReaderViewModel.kt`
3. Add to `VerbumNavGraph.kt`
4. Update DI modules (`DatabaseModule.kt`)

### Phase 4: Search
1. Integrate search into CstDocListScreen (search icon → FTS query)
2. Integrate search into CstReaderScreen (search within current doc or all)
3. Reuse/adapt `CccSearchDrawer` or create `CstSearchDrawer`

### Phase 5: Polish
1. String resources EN/ES
2. Handle loading states, errors, empty states
3. Bookmarks/read status for CST sections (optional, via FileManager pattern)
4. Test with both languages

---

## 10. Key Files Summary

| File | Action |
|------|--------|
| `Route.kt` | Add `Teachings`, `CstDocList`, `CstReader` routes |
| `MainScreen.kt` | Update bottom bar route |
| `VerbumNavGraph.kt` | Add 3 new composable destinations |
| `TeachingsScreen.kt` | **NEW** — Hub screen with CCC / CST cards |
| `CstDocListScreen.kt` | **NEW** — CST document listing |
| `CstDocListViewModel.kt` | **NEW** |
| `CstReaderScreen.kt` | **NEW** — Section reader (reuses CCC rendering) |
| `CstReaderViewModel.kt` | **NEW** |
| `CstRawDatabase.kt` | **NEW** — Raw SQLite access for verbum_cst.db |
| `CstRepository.kt` | **NEW** — Repository layer |
| `CstEntities.kt` | **NEW** — Data entities and UI state models |
| `DatabaseModule.kt` | Add CST database/repository providers |
| `strings.xml` (EN/ES) | Add CST-related strings |
| `app/src/main/assets/verbum_cst.db` | **NEW** — CST database asset |
| `etl/build_cst_db.py` | **NEW** — ETL script to build CST DB |
| `cst_sources/*.json` | **NEW** — Raw CST content EN/ES |

---

## 11. Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| CST content copyright (papal documents are public domain in most jurisdictions) | Verify each document's copyright status; most encyclicals before 1978 are public domain in US |
| Large database size with FTS | Same approach as CCC DB; ship in assets and copy on first run |
| Same formatting as CCC | Use identical `formatted_json` schema → reuse all CCC rendering code verbatim |
| Search performance | FTS4 with `ORDER BY rank` — same as proven CCC search |
| Language switching | `LanguageManager.appLanguage` flow — same as CCC. Re-fetch sections on language change |
