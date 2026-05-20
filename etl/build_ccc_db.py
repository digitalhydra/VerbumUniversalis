#!/usr/bin/env python3
"""
ETL: Build verbum_ccc.db from raw_data/ccc.json

Normalizes scraped CCC data into clean paragraphs with:
- Deduplicated paragraph text (same para appears across multiple page fragments)
- Continuation paragraphs joined (paragraphs without ref-ccc belong to preceding CCC number)
- Inline Bible references parsed (stored in `ccc_bible_refs`)
- Inline Tradition references parsed (stored in `ccc_tradition_refs`)
- **Footnote extraction** – each `[n]` marker is linked to a footnote entry; footnotes are stored in `ccc_footnotes`. Any Bible citations inside footnotes are also parsed and stored in `ccc_footnote_bible_refs`.
- FTS5 full-text search index
- TOC hierarchy
- User tags table (empty, for app use)

Output: verbum_ccc.db (SQLite, ready to ship in app/src/main/assets/)
"""

import json
import re
import sqlite3
import sys
import os
from pathlib import Path

# ── Configuration ──────────────────────────────────────────────────────────

CCC_JSON_PATH = Path(__file__).parent.parent / "raw_data" / "ccc.json"
OUTPUT_DB = Path(__file__).parent.parent / "app" / "src" / "main" / "assets" / "verbum_ccc.db"

# ── Bible book abbreviations used in CCC text → Verbum book_id ────────────
# Maps CCC inline abbreviation patterns to internal book IDs (1-73)
CCC_BIBLE_ABBREV_TO_ID = {
    # Old Testament
    "Gen": 1, "Ex": 2, "Lev": 3, "Num": 4, "Deut": 5,
    "Josh": 6, "Judg": 7, "Ruth": 8,
    "1 Sam": 9, "2 Sam": 10, "1 Kings": 11, "2 Kings": 12,
    "1 Chr": 13, "2 Chr": 14, "1 Chron": 13, "2 Chron": 14,
    "Ezra": 15, "Neh": 16, "Tob": 17, "Jdt": 18, "Esth": 19,
    "Job": 20, "Ps": 21, "Prov": 22, "Eccles": 23, "Song": 24,
    "Wis": 25, "Sir": 26,
    "Isa": 27, "Jer": 28, "Lam": 29, "Bar": 30, "Ezek": 31, "Dan": 32,
    "Hos": 33, "Joel": 34, "Amos": 35, "Obad": 36, "Jon": 37,
    "Mic": 38, "Nah": 39, "Hab": 40, "Zeph": 41, "Hag": 42,
    "Zech": 43, "Mal": 44,
    "1 Mac": 45, "2 Mac": 46, "1 Macc": 45, "2 Macc": 46,
    # New Testament
    "Mt": 47, "Mk": 48, "Lk": 49, "Jn": 50,
    "Acts": 51, "Rom": 52,
    "1 Cor": 53, "2 Cor": 54, "Gal": 55, "Eph": 56, "Phil": 57, "Col": 58,
    "1 Thess": 59, "2 Thess": 60, "1 Tim": 61, "2 Tim": 62,
    "Tit": 63, "Philem": 64, "Heb": 65, "Jas": 66,
    "1 Pet": 67, "2 Pet": 68, "1 Jn": 69, "2 Jn": 70, "3 Jn": 71,
    "Jude": 72, "Rev": 73,
    # Common alternate forms in CCC
    "I Jn": 69, "II Jn": 70, "III Jn": 71,
    "I Cor": 53, "II Cor": 54,
    "I Thess": 59, "II Thess": 60,
    "I Tim": 61, "II Tim": 62,
    "I Pet": 67, "II Pet": 68,
    "I Sam": 9, "II Sam": 10,
    "I Kings": 11, "II Kings": 12,
    "I Chr": 13, "II Chr": 14,
    "I Mac": 45, "II Mac": 46,
}

# ── Tradition abbreviations used in CCC text ──────────────────────────────
# These are Church documents, councils, papal writings referenced as (ABBREV NUMBER)
TRADITION_ABBREVS = {
    "AA": "Apostolicam actositatem",
    "AG": "Ad gentes",
    "CA": "Centesimus annus",
    "CCEO": "Corpus Canonum Ecclisarum Orientalium",
    "CD": "Christus Dominus",
    "CDF": "Congregation for the Doctrine of the Faith",
    "CIC": "Codex Iuris Canonici",
    "CL": "Christifideles laici",
    "CT": "Catechesi tradendae",
    "DH": "Dignitatis humanae",
    "DM": "Dives in misericordia",
    "DS": "Denzinger-Schönmetzer",
    "DV": "Dei Verbum",
    "DeV": "Dominum et Vivificanum",
    "EN": "Evangelii nuntiandi",
    "FC": "Familiaris consortio",
    "GE": "Gravissimum educationis",
    "GS": "Gaudium et spes",
    "HV": "Humanae vitae",
    "IM": "Inter mirifica",
    "LE": "Laborem exercens",
    "LG": "Lumen gentium",
    "MC": "Marialis cultus",
    "MD": "Mulieris dignitatem",
    "MF": "Mysterium fidei",
    "MM": "Mater et magistra",
    "NA": "Nostra aetate",
    "OE": "Orientalium ecclesiarum",
    "OT": "Optatam totius",
    "PC": "Perfectae caritatis",
    "PG": "Patrologia Graeca (Migne)",
    "PL": "Patrologia Latina (Migne)",
    "PO": "Presbyterorum ordinis",
    "PP": "Populorum progressio",
    "PT": "Pacem in terris",
    "RH": "Redemptor hominis",
    "RMat": "Redemptoris Mater",
    "RMiss": "Redemptoris Missio",
    "RP": "Reconciliatio et paenitentia",
    "SC": "Sacrosanctum concilium",
    "SRS": "Sollicitudo rei socialis",
    "STh": "Summa Theologiae",
    "UR": "Unitatis redintegratio",
}

# ── Official CCC Hierarchy Ranges ─────────────────────────────────────────
# Structure: (min_para, max_para, title, level)
# level 0: PART, level 1: SECTION, level 2: CHAPTER
CCC_HIERARCHY = [
    # Parts
    (26, 1065, "PART ONE: THE PROFESSION OF FAITH", 0),
    (1066, 1690, "PART TWO: THE CELEBRATION OF THE CHRISTIAN MYSTERY", 0),
    (1691, 2557, "PART THREE: LIFE IN CHRIST", 0),
    (2558, 2865, "PART FOUR: CHRISTIAN PRAYER", 0),

    # Sections
    (26, 184, "SECTION ONE: 'I BELIEVE' - 'WE BELIEVE'", 1),
    (185, 1065, "SECTION TWO: THE PROFESSION OF THE CHRISTIAN FAITH", 1),
    (1076, 1209, "SECTION ONE: THE SACRAMENTAL ECONOMY", 1),
    (1210, 1690, "SECTION TWO: THE SEVEN SACRAMENTS OF THE CHURCH", 1),
    (1699, 2051, "SECTION ONE: MAN'S VOCATION: LIFE IN THE SPIRIT", 1),
    (2052, 2557, "SECTION TWO: THE TEN COMMANDMENTS", 1),
    (2559, 2758, "SECTION ONE: PRAYER IN THE CHRISTIAN LIFE", 1),
    (2759, 2865, "SECTION TWO: THE LORD'S PRAYER: 'OUR FATHER!'", 1),

    # Chapters
    (27, 49, "CHAPTER ONE: MAN'S CAPACITY FOR GOD", 2),
    (50, 141, "CHAPTER TWO: GOD COMES TO MEET MAN", 2),
    (142, 184, "CHAPTER THREE: MAN'S RESPONSE TO GOD", 2),
    (198, 421, "CHAPTER ONE: I BELIEVE IN GOD THE FATHER", 2),
    (422, 682, "CHAPTER TWO: I BELIEVE IN JESUS CHRIST, THE ONLY SON OF GOD", 2),
    (683, 1065, "CHAPTER THREE: I BELIEVE IN THE HOLY SPIRIT", 2),
    (1077, 1112, "CHAPTER ONE: THE PASCHAL MYSTERY IN THE AGE OF THE CHURCH", 2),
    (1135, 1209, "CHAPTER TWO: THE SACRAMENTAL CELEBRATION", 2),
    (1212, 1419, "CHAPTER ONE: THE SACRAMENTS OF CHRISTIAN INITIATION", 2),
    (1420, 1532, "CHAPTER TWO: THE SACRAMENTS OF HEALING", 2),
    (1533, 1666, "CHAPTER THREE: THE SACRAMENTS AT THE SERVICE OF COMMUNION", 2),
    (1667, 1690, "CHAPTER FOUR: OTHER LITURGICAL CELEBRATIONS", 2),
    (1700, 1876, "CHAPTER ONE: THE DIGNITY OF THE HUMAN PERSON", 2),
    (1877, 1948, "CHAPTER TWO: THE HUMAN COMMUNITY", 2),
    (1949, 2051, "CHAPTER THREE: GOD'S SALVATION: LAW AND GRACE", 2),
    (2083, 2195, "CHAPTER ONE: 'YOU SHALL LOVE THE LORD YOUR GOD...'", 2),
    (2196, 2257, "CHAPTER TWO: 'YOU SHALL LOVE YOUR NEIGHBOR AS YOURSELF'", 2),
    (2566, 2649, "CHAPTER ONE: THE REVELATION OF PRAYER", 2),
    (2650, 2696, "CHAPTER TWO: THE TRADITION OF PRAYER", 2),
    (2697, 2758, "CHAPTER THREE: THE LIFE OF PRAYER", 2)
]


# ── Helper: normalize whitespace ──────────────────────────────────────────

def clean_text(text: str) -> str:
    """Collapse whitespace and trim."""
    return ' '.join(text.split())


def join_paragraph_elements(elements: list) -> str:
    """
    Join all text elements from a paragraph's element array into a single string.
    Spacers are replaced with space. ref and ref-ccc elements are rendered as markers.
    """
    parts = []
    for el in elements:
        t = el.get("type")
        if t == "text":
            parts.append(el.get("text", ""))
        elif t == "spacer":
            parts.append(" ")
        elif t == "ref-ccc":
            # CCC internal cross-reference — render as marker
            parts.append(f" [¶{el['ref_number']}] ")
        elif t == "ref":
            # Footnote number — render as superscript marker
            parts.append(f"[{el['number']}]")
    return clean_text(''.join(parts))


def build_formatted_json(elements: list) -> str:
    """
    Serialize paragraph elements as JSON for rich-text rendering in the app.
    Adds resolved bible-ref elements for inline references.
    """
    # Simple pass-through for now — preserves bold/italic/href attrs
    return json.dumps(elements, ensure_ascii=False)


# ── Bible reference parsing ───────────────────────────────────────────────

def parse_bible_refs(plain_text: str, ccc_number: int) -> list[dict]:
    """
    Find inline Bible references in CCC paragraph text.
    Returns list of {book_id, chapter, verse_start, verse_end, ref_text, ref_position, ref_length}
    """
    refs = []
    
    # Build regex from known abbreviations
    abbrevs_sorted = sorted(CCC_BIBLE_ABBREV_TO_ID.keys(), key=len, reverse=True)
    abbrev_pattern = '|'.join(re.escape(a) for a in abbrevs_sorted)
    
    # Pattern: book_abbrev chapter:verse or chapter:verse-verse
    # Captures: (book, chapter, verse_start, verse_end_or_none)
    pattern = re.compile(
        r'\b(' + abbrev_pattern + r')\s+(\d{1,3}):(\d{1,3})(?:-(\d{1,3}))?',
        re.IGNORECASE
    )
    
    for match in pattern.finditer(plain_text):
        book_raw = match.group(1)
        chapter = int(match.group(2))
        verse_start = int(match.group(3))
        verse_end_str = match.group(4)
        verse_end = int(verse_end_str) if verse_end_str else None
        
        # Normalize book abbreviation for lookup
        book_normalized = book_raw.strip()
        # Try exact match first, then case-insensitive
        book_id = CCC_BIBLE_ABBREV_TO_ID.get(book_normalized)
        if book_id is None:
            # Try uppercase
            book_id = CCC_BIBLE_ABBREV_TO_ID.get(book_normalized.upper())
        if book_id is None:
            # Try with space normalization (e.g., "1Sam" → "1 Sam")
            for key, val in CCC_BIBLE_ABBREV_TO_ID.items():
                if key.replace(' ', '') == book_normalized.replace(' ', '').upper():
                    book_id = val
                    break
        
        if book_id is not None:
            ref_text = f"{book_raw} {chapter}:{verse_start}"
            if verse_end and verse_end != verse_start:
                ref_text += f"-{verse_end}"
            
            refs.append({
                "ccc_number": ccc_number,
                "book_id": book_id,
                "chapter": chapter,
                "verse_start": verse_start,
                "verse_end": verse_end,
                "ref_text": ref_text,
                "ref_position": match.start(),
                "ref_length": match.end() - match.start(),
            })
    
    return refs


def parse_tradition_refs(plain_text: str, ccc_number: int) -> list[dict]:
    """
    Find inline Tradition/Church document references in CCC paragraph text.
    Returns list of {abbrev, number, description, ref_position, ref_length}
    """
    refs = []
    
    # Build regex from known tradition abbreviations (longest first to avoid partial matches)
    abbrevs_sorted = sorted(TRADITION_ABBREVS.keys(), key=len, reverse=True)
    abbrev_pattern = '|'.join(re.escape(a) for a in abbrevs_sorted)
    
    pattern = re.compile(
        r'\b(' + abbrev_pattern + r')\s+(\d+)',
        re.IGNORECASE
    )
    
    for match in pattern.finditer(plain_text):
        abbrev = match.group(1)
        number = match.group(2)
        
        # Normalize case for lookup
        abbrev_key = abbrev
        if abbrev_key not in TRADITION_ABBREVS:
            abbrev_key = next((k for k in TRADITION_ABBREVS if k.upper() == abbrev.upper()), None)
        
        if abbrev_key:
            refs.append({
                "ccc_number": ccc_number,
                "abbrev": abbrev_key,
                "number": number,
                "description": TRADITION_ABBREVS[abbrev_key],
                "ref_position": match.start(),
                "ref_length": match.end() - match.start(),
            })
    
    return refs


# ── TOC path extraction ───────────────────────────────────────────────────

def build_toc_paths(data: dict) -> dict[str, str]:
    """
    Build a mapping from toc_node_id → full hierarchical path string.
    e.g., "toc-13" → "PART ONE > SECTION ONE > CHAPTER ONE > I. The Desire for God"
    """
    # Build adjacency from toc_link_tree
    children_of = {}  # parent_id → [child_ids]
    parent_of = {}    # child_id → parent_id
    
    def walk_tree(nodes, parent_id=None):
        for node in nodes:
            nid = node["id"]
            if parent_id:
                parent_of[nid] = parent_id
            kids = node.get("children", [])
            if kids:
                children_of[nid] = [c["id"] for c in kids]
                walk_tree(kids, nid)
    
    walk_tree(data["toc_link_tree"])
    
    # Find root nodes (no parent)
    all_ids = set(data["toc_nodes"].keys())
    roots = [nid for nid in all_ids if nid not in parent_of]
    
    # Build path for each node
    paths = {}
    for nid in all_ids:
        path_parts = []
        current = nid
        while current:
            title = data["toc_nodes"].get(current, {}).get("text", current)
            path_parts.append(title)
            current = parent_of.get(current)
        paths[nid] = " > ".join(reversed(path_parts))
    
    return paths


# Known URL slug → display title fixes
SLUG_FIXES = {
    "aposletr": "APOSTOLIC LETTER",
    "aposcons": "APOSTOLIC CONSTITUTION",
}

def get_toc_path_for_page(n, current_path):
    """
    Standardize the TOC path based on official hierarchy ranges.
    Uses the paragraph number (n) to find Part, Section, Chapter.
    Then appends relevant Article/In Brief info from the existing path.
    """
    if 1 <= n <= 25:
        return "PROLOGUE"

    parts = []
    for level in [0, 1, 2]:
        match = next((h[2] for h in CCC_HIERARCHY if h[0] <= n <= h[1] and h[3] == level), None)
        if match:
            parts.append(match)

    prefix = " > ".join(parts)

    # Determine the tail (e.g., Article info) from the current_path
    existing_parts = [p.strip() for p in current_path.split(">")]
    tail = []
    for p in existing_parts:
        # If this part is NOT already covered by the prefix
        if p not in prefix and p not in SLUG_FIXES.values():
            # Keep parts that look like Articles or specific sub-sections
            if any(x in p.upper() for x in ["ARTICLE", "PARAGRAPH", "IN BRIEF"]):
                tail.append(p)

    # If no Article/Paragraph found but the last part of existing path is very specific
    if not tail and existing_parts:
        last_part = existing_parts[-1]
        # Map slugs if needed
        last_part = SLUG_FIXES.get(last_part, last_part)
        if last_part not in prefix:
            tail.append(last_part)

    new_path = prefix
    if tail:
        # Avoid redundant parts
        clean_tail = []
        for t in tail:
            if t not in new_path:
                clean_tail.append(t)

        if clean_tail:
            new_path += " > " + " > ".join(clean_tail)

    return new_path


# ── Main ETL ──────────────────────────────────────────────────────────────

def main():
    print(f"Loading {CCC_JSON_PATH}...")
    with open(CCC_JSON_PATH, 'r') as f:
        data = json.load(f)
    
    print(f"CCC data version: {data['meta'].get('version', 'unknown')}")
    
    # Build TOC paths
    print("Building TOC hierarchy...")
    toc_paths = build_toc_paths(data)
    print(f"  {len(toc_paths)} TOC nodes mapped")
    
    # ── Step 1: Collect paragraph text (deduplicated) ──
    print("Collecting paragraph text...")
    
    # For each CCC paragraph number, store the assembled text from the first page it appears on
    paragraphs = {}  # ccc_number → {plain_text, formatted_json, toc_path}
    
    # Track which page_nodes we've processed per CCC number (first wins)
    seen_on_pages = {}  # ccc_number → first page_node_id
    
    # Mapping of (page_id, footnote_number) → ccc_number so we can assign footnotes to paragraphs
    footnote_to_paragraph = {}
    
    for pn_id, pn in data["page_nodes"].items():
        page_paragraphs = pn.get("paragraphs", [])
        
        # Track the current CCC number being built as we walk page paragraphs
        current_ccc = None
        current_text_parts = []
        current_elements = []
        
        for para_elements_wrapper in page_paragraphs:
            elements = para_elements_wrapper.get("elements", [])
            
            # Check if this paragraph starts a new CCC number
            has_ref_ccc = False
            new_ccc_number = None
            for el in elements:
                if el.get("type") == "ref-ccc":
                    has_ref_ccc = True
                    new_ccc_number = el["ref_number"]
                    break
            
            if has_ref_ccc and new_ccc_number is not None:
                # Finalize previous CCC paragraph
                if current_ccc is not None and current_text_parts:
                    if current_ccc not in paragraphs:
                        raw_path = toc_paths.get(pn_id, "")
                        paragraphs[current_ccc] = {
                            "plain_text": clean_text(' '.join(current_text_parts)),
                            "formatted_json": build_formatted_json(current_elements),
                            "toc_path": get_toc_path_for_page(current_ccc, raw_path),
                        }
                        seen_on_pages[current_ccc] = pn_id
                
                # Start new CCC paragraph
                current_ccc = new_ccc_number
                # Collect text from THIS paragraph's elements (skip the ref-ccc marker itself for text)
                current_text_parts = [el.get("text", "") for el in elements if el.get("type") == "text"]
                current_elements = list(elements)  # Keep all elements for formatted JSON
                # Record any footnote markers in this paragraph (they belong to this CCC number)
                for el in elements:
                    if el.get("type") == "ref":
                        footnote_to_paragraph[(pn_id, el["number"])] = current_ccc
            else:
                # Continuation paragraph — belongs to current CCC number
                if current_ccc is not None:
                    continuation_text = [el.get("text", "") for el in elements if el.get("type") == "text"]
                    current_text_parts.extend(continuation_text)
                    current_elements.extend(elements)
                    # Record footnote markers in continuation paragraphs as well
                    for el in elements:
                        if el.get("type") == "ref":
                            footnote_to_paragraph[(pn_id, el["number"])] = current_ccc
        
        # Finalize last paragraph on this page
        if current_ccc is not None and current_text_parts and current_ccc not in paragraphs:
            raw_path = toc_paths.get(pn_id, "")
            paragraphs[current_ccc] = {
                "plain_text": clean_text(' '.join(current_text_parts)),
                "formatted_json": build_formatted_json(current_elements),
                "toc_path": get_toc_path_for_page(current_ccc, raw_path),
            }
            seen_on_pages[current_ccc] = pn_id
    
    print(f"  {len(paragraphs)} unique CCC paragraphs collected")
    
    # ── Step 2: Parse references ──
    print("Parsing Bible references...")
    all_bible_refs = []
    for ccc_num, para in paragraphs.items():
        refs = parse_bible_refs(para["plain_text"], ccc_num)
        all_bible_refs.extend(refs)
    print(f"  {len(all_bible_refs)} Bible references found")
    
    print("Parsing Tradition references...")
    all_tradition_refs = []
    for ccc_num, para in paragraphs.items():
        refs = parse_tradition_refs(para["plain_text"], ccc_num)
        all_tradition_refs.extend(refs)
    print(f"  {len(all_tradition_refs)} Tradition references found")
    
    # ── Step 3: Extract footnotes ──
    print("Extracting footnotes...")
    footnote_rows = []  # each dict {ccc_number, footnote_number, footnote_text}
    footnote_bible_refs = []  # each dict {ccc_number, footnote_number, refs [...]}
    for pn_id, pn in data["page_nodes"].items():
        footnotes = pn.get("footnotes", {})
        if not footnotes:
            continue
        for fn_key, fn in footnotes.items():
            # footnote number may be string key or int
            try:
                fn_number = int(fn_key)
            except Exception:
                fn_number = fn_key
            ccc_number = footnote_to_paragraph.get((pn_id, fn_number))
            if ccc_number is None:
                continue
            # Build footnote text by joining refs texts
            footnote_text = "; ".join(r.get("text", "") for r in fn.get("refs", []))
            footnote_rows.append({
                "ccc_number": ccc_number,
                "footnote_number": fn_number,
                "footnote_text": footnote_text,
            })
            # Parse any Bible references inside the footnote text
            bible_refs = parse_bible_refs(footnote_text, ccc_number)
            if bible_refs:
                footnote_bible_refs.append({
                    "ccc_number": ccc_number,
                    "footnote_number": fn_number,
                    "refs": bible_refs,
                })
    print(f"  {len(footnote_rows)} footnotes extracted")
    
    # Dedup footnotes: keep first occurrence for each (ccc_number, footnote_number)
    footnote_seen = {}
    footnote_rows_unique = []
    for row in footnote_rows:
        key = (row["ccc_number"], row["footnote_number"])
        if key not in footnote_seen:
            footnote_seen[key] = True
            footnote_rows_unique.append(row)
    footnote_rows = footnote_rows_unique
    # Filter footnote_bible_refs to only those footnotes we keep
    footnote_bible_refs = [fb for fb in footnote_bible_refs if (fb["ccc_number"], fb["footnote_number"]) in footnote_seen]
    print(f"  {len(footnote_rows)} unique footnotes after dedup")
    
    # ── Step 4: Build SQLite database ──
    print(f"Building {OUTPUT_DB}...")
    OUTPUT_DB.parent.mkdir(parents=True, exist_ok=True)
    
    conn = sqlite3.connect(str(OUTPUT_DB))
    conn.execute("PRAGMA journal_mode=WAL")

    # ── Create tables ──
    conn.executescript("""
        PRAGMA foreign_keys=OFF;
        DROP TABLE IF EXISTS ccc_bible_refs;
        DROP TABLE IF EXISTS ccc_tradition_refs;
        DROP TABLE IF EXISTS ccc_tags;
        DROP TABLE IF EXISTS ccc_paragraphs;
        DROP TABLE IF EXISTS ccc_fts;
        DROP TABLE IF EXISTS ccc_footnotes;
        DROP TABLE IF EXISTS ccc_footnote_bible_refs;
        
        CREATE TABLE ccc_paragraphs (
            number INTEGER PRIMARY KEY,
            toc_path TEXT NOT NULL,
            plain_text TEXT NOT NULL,
            formatted_json TEXT NOT NULL
        );
        
        CREATE TABLE ccc_bible_refs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ccc_number INTEGER NOT NULL REFERENCES ccc_paragraphs(number),
            book_id INTEGER NOT NULL,
            chapter INTEGER NOT NULL,
            verse_start INTEGER NOT NULL,
            verse_end INTEGER,
            ref_text TEXT NOT NULL,
            ref_position INTEGER NOT NULL,
            ref_length INTEGER NOT NULL
        );
        CREATE INDEX idx_ccc_refs_book ON ccc_bible_refs(book_id, chapter, verse_start);
        CREATE INDEX idx_ccc_refs_ccc ON ccc_bible_refs(ccc_number);
        
        CREATE TABLE ccc_tradition_refs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ccc_number INTEGER NOT NULL REFERENCES ccc_paragraphs(number),
            abbrev TEXT NOT NULL,
            number TEXT NOT NULL,
            description TEXT NOT NULL,
            ref_position INTEGER NOT NULL,
            ref_length INTEGER NOT NULL
        );
        CREATE INDEX idx_ccc_trad_refs_ccc ON ccc_tradition_refs(ccc_number);
        CREATE INDEX idx_ccc_trad_refs_abbrev ON ccc_tradition_refs(abbrev);
        
        CREATE TABLE ccc_tags (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ccc_number INTEGER NOT NULL REFERENCES ccc_paragraphs(number),
            tag TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT (datetime('now')),
            UNIQUE(ccc_number, tag)
        );
        CREATE INDEX idx_ccc_tags_tag ON ccc_tags(tag);
        CREATE INDEX idx_ccc_tags_ccc ON ccc_tags(ccc_number);
        
        -- FTS5 full-text search
        CREATE VIRTUAL TABLE ccc_fts USING fts5(
            number,
            toc_path,
            plain_text,
            content='ccc_paragraphs',
            content_rowid='number'
        );
        
        -- Triggers to keep FTS in sync
        CREATE TRIGGER ccc_fts_insert AFTER INSERT ON ccc_paragraphs BEGIN
            INSERT INTO ccc_fts(rowid, number, toc_path, plain_text) 
            VALUES (new.number, new.number, new.toc_path, new.plain_text);
        END;
        CREATE TRIGGER ccc_fts_delete AFTER DELETE ON ccc_paragraphs BEGIN
            INSERT INTO ccc_fts(ccc_fts, rowid, number, toc_path, plain_text) 
            VALUES ('delete', old.number, old.number, old.toc_path, old.plain_text);
        END;
        CREATE TRIGGER ccc_fts_update AFTER UPDATE ON ccc_paragraphs BEGIN
            INSERT INTO ccc_fts(ccc_fts, rowid, number, toc_path, plain_text) 
            VALUES ('delete', old.number, old.number, old.toc_path, old.plain_text);
            INSERT INTO ccc_fts(rowid, number, toc_path, plain_text) 
            VALUES (new.number, new.number, new.toc_path, new.plain_text);
        END;
        
        -- Footnotes tables
        CREATE TABLE ccc_footnotes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ccc_number INTEGER NOT NULL REFERENCES ccc_paragraphs(number),
            footnote_number INTEGER NOT NULL,
            footnote_text TEXT NOT NULL,
            UNIQUE(ccc_number, footnote_number)
        );
        CREATE INDEX idx_footnotes_ccc ON ccc_footnotes(ccc_number);
        
        CREATE TABLE ccc_footnote_bible_refs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            footnote_id INTEGER NOT NULL REFERENCES ccc_footnotes(id),
            book_id INTEGER NOT NULL,
            chapter INTEGER NOT NULL,
            verse_start INTEGER NOT NULL,
            verse_end INTEGER,
            ref_text TEXT NOT NULL,
            ref_position INTEGER NOT NULL,
            ref_length INTEGER NOT NULL
        );
        CREATE INDEX idx_footnote_bible_book ON ccc_footnote_bible_refs(book_id, chapter, verse_start);
        CREATE INDEX idx_footnote_bible_footnote ON ccc_footnote_bible_refs(footnote_id);
    """)
    
    # ── Insert paragraphs ──
    conn.execute("BEGIN TRANSACTION")
    for ccc_num, para in sorted(paragraphs.items()):
        conn.execute(
            "INSERT INTO ccc_paragraphs (number, toc_path, plain_text, formatted_json) VALUES (?, ?, ?, ?)",
            (ccc_num, para["toc_path"], para["plain_text"], para["formatted_json"])
        )
    conn.execute("COMMIT")
    print(f"  {len(paragraphs)} paragraphs inserted")
    
    # ── Insert Bible refs ──
    if all_bible_refs:
        conn.execute("BEGIN TRANSACTION")
        for ref in all_bible_refs:
            conn.execute(
                """INSERT INTO ccc_bible_refs 
                   (ccc_number, book_id, chapter, verse_start, verse_end, ref_text, ref_position, ref_length)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                (ref["ccc_number"], ref["book_id"], ref["chapter"],
                 ref["verse_start"], ref["verse_end"], ref["ref_text"],
                 ref["ref_position"], ref["ref_length"])
            )
        conn.execute("COMMIT")
    print(f"  {len(all_bible_refs)} Bible refs inserted")
    
    # ── Insert Tradition refs ──
    if all_tradition_refs:
        conn.execute("BEGIN TRANSACTION")
        for ref in all_tradition_refs:
            conn.execute(
                """INSERT INTO ccc_tradition_refs 
                   (ccc_number, abbrev, number, description, ref_position, ref_length)
                   VALUES (?, ?, ?, ?, ?, ?)""",
                (ref["ccc_number"], ref["abbrev"], ref["number"],
                 ref["description"], ref["ref_position"], ref["ref_length"])
            )
        conn.execute("COMMIT")
    print(f"  {len(all_tradition_refs)} Tradition refs inserted")
    
    # ── Insert footnotes ──
    footnote_id_map = {}
    if footnote_rows:
        conn.execute("BEGIN TRANSACTION")
        for row in footnote_rows:
            cur = conn.execute(
                "INSERT INTO ccc_footnotes (ccc_number, footnote_number, footnote_text) VALUES (?, ?, ?)",
                (row["ccc_number"], row["footnote_number"], row["footnote_text"])
            )
            fid = cur.lastrowid
            footnote_id_map[(row["ccc_number"], row["footnote_number"])] = fid
        conn.execute("COMMIT")
        print(f"  {len(footnote_rows)} footnotes inserted")
    else:
        print("  No footnotes to insert")
    
    # ── Insert footnote Bible refs ──
    total_footnote_bible = 0
    if footnote_bible_refs:
        conn.execute("BEGIN TRANSACTION")
        for fb in footnote_bible_refs:
            ccc_num = fb["ccc_number"]
            fn_num = fb["footnote_number"]
            fid = footnote_id_map.get((ccc_num, fn_num))
            if not fid:
                continue
            for ref in fb["refs"]:
                conn.execute(
                    """INSERT INTO ccc_footnote_bible_refs 
                       (footnote_id, book_id, chapter, verse_start, verse_end, ref_text, ref_position, ref_length)
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                    (fid, ref["book_id"], ref["chapter"], ref["verse_start"],
                     ref["verse_end"], ref["ref_text"], ref["ref_position"], ref["ref_length"])
                )
                total_footnote_bible += 1
        conn.execute("COMMIT")
        print(f"  {total_footnote_bible} Bible refs in footnotes inserted")
    else:
        print("  No footnote Bible refs to insert")
    
    # ── Rebuild FTS index ──
    conn.execute("INSERT INTO ccc_fts(ccc_fts) VALUES('rebuild')")
    
    # ── Verify ──
    count = conn.execute("SELECT COUNT(*) FROM ccc_paragraphs").fetchone()[0]
    bible_count = conn.execute("SELECT COUNT(*) FROM ccc_bible_refs").fetchone()[0]
    trad_count = conn.execute("SELECT COUNT(*) FROM ccc_tradition_refs").fetchone()[0]
    footnote_count = conn.execute("SELECT COUNT(*) FROM ccc_footnotes").fetchone()[0]
    footnote_bible_count = conn.execute("SELECT COUNT(*) FROM ccc_footnote_bible_refs").fetchone()[0]
    
    # Show sample
    print(f"\n=== Verification ===")
    print(f"  Paragraphs: {count}")
    print(f"  Bible refs: {bible_count}")
    print(f"  Tradition refs: {trad_count}")
    print(f"  Footnotes: {footnote_count}")
    print(f"  Footnote Bible refs: {footnote_bible_count}")
    
    sample = conn.execute("SELECT number, substr(plain_text, 1, 120) FROM ccc_paragraphs WHERE number = 27").fetchone()
    if sample:
        print(f"\n  Sample ¶{sample[0]}: \"{sample[1]}...\"")
    
    sample_refs = conn.execute(
        "SELECT ref_text FROM ccc_bible_refs WHERE ccc_number = 27"
    ).fetchall()
    if sample_refs:
        print(f"  Bible refs in ¶27: {[r[0] for r in sample_refs]}")
    else:
        print(f"  No inline Bible refs in ¶27 (footnotes only)")
    
    # Sample footnote
    sample_fn = conn.execute("SELECT ccc_number, footnote_number, footnote_text FROM ccc_footnotes LIMIT 3").fetchall()
    if sample_fn:
        print(f"\n  Sample footnotes:")
        for row in sample_fn:
            print(f"    ¶{row[0]} note {row[1]}: {row[2][:60]}...")
    
    conn.close()
    
    db_size = OUTPUT_DB.stat().st_size
    print(f"\nDone! {OUTPUT_DB} ({db_size:,} bytes)")


if __name__ == "__main__":
    main()