# 🛠️ ETL Pipeline: Verbum Seed DB

This pipeline generates `verbum_seed.db`, providing offline-first Bible texts, interlinear data, and theological lexicons.

## 🚀 Quick Start
1. Install dependencies: `pip install pysword`
2. Run build:
   ```bash
   python3 etl/build_verbum_seed.py
   ```

Detailed instructions, data requirements, and schema info are available in [**etl/README.md**](etl/README.md).

## 📖 CCC ETL Pipeline

Build the Catechism of the Catholic Church database from scraped HTML data.

### 🚀 Quick Start
1. Ensure `raw_data/ccc.json` exists (scraped from scborromeo.org)
2. Run build:
   ```bash
   python3 etl/build_ccc_db.py
   ```
3. Output: `app/src/main/assets/verbum_ccc.db` (~5.5 MB)

### 📊 CCC DB Overview
- `ccc_paragraphs`: 2,865 catechism paragraphs (1–2865), each with plain text, TOC breadcrumb, and rich-text JSON.
- `ccc_bible_refs`: 118 inline Bible references resolved to `(book_id, chapter, verse_start, verse_end)`, with character positions for inline highlighting.
- `ccc_tradition_refs`: 84 Church document references (Vatican II, Church Fathers, papal encyclicals).
- `ccc_fts`: FTS5 full-text search index.
- `ccc_tags`: User-tagging table (empty, for in-app use).

### 🔍 Normalization Steps
1. **Deduplication** — Each CCC paragraph appears across 7–8 scraped page fragments; keeps first occurrence.
2. **Continuation joining** — Paragraphs without a CCC number marker belong to the preceding numbered paragraph.
3. **Reference parsing** — Inline Bible citations (`Gen 1:1`) and Tradition references (`GS 19`, `DV 10`) are parsed into structured tables.
4. **Whitespace collapse** — All text elements joined into single clean strings.

### ⚠️ Notes
- **Footnote references**: ~1,800 Bible/Tradition references are on external `cr/NNN.htm` footnote pages (not yet scraped) — only 118 inline citations are indexed.
- **Deterministic**: Same `ccc.json` produces identical DB.

## 🏗️ DB Overview
- `books`: Metadata for 73 Catholic books.
- `verses`: Unique anchors for every verse.
- `texts`: Translation content (DR, PLA, VL).
- `interlinear_words`: Greek/Hebrew text + Transliteration + English Gloss.
- `lexicon`: Strong's definitions.
