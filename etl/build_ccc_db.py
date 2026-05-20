#!/usr/bin/env python3
"""
ETL: Build verbum_ccc.db from raw_data/ccc.json

Normalizes scraped CCC data into clean paragraphs with:
- Deduplicated paragraph text (same para appears across multiple page fragments)
- Continuation paragraphs joined (paragraphs without ref-ccc belong to preceding CCC number)
- Inline Bible references parsed
- Inline Tradition references parsed  
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

def get_toc_path_for_page(page_id: str, toc_paths: dict[str, str]) -> str:
    """Get the TOC path for a page_node ID, with slug fixes."""
    path = toc_paths.get(page_id, "")
    # Apply slug fixes
    for slug, display in SLUG_FIXES.items():
        path = path.replace(slug, display)
    return path


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
    paragraphs = {}  # ccc_number → {plain_text, formatted_json, toc_paths_set}
    
    # Track which page_nodes we've processed per CCC number (first wins)
    seen_on_pages = {}  # ccc_number → first page_node_id
    
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
                        paragraphs[current_ccc] = {
                            "plain_text": clean_text(' '.join(current_text_parts)),
                            "formatted_json": build_formatted_json(current_elements),
                            "toc_path": get_toc_path_for_page(pn_id, toc_paths),
                        }
                        seen_on_pages[current_ccc] = pn_id
                
                # Start new CCC paragraph
                current_ccc = new_ccc_number
                # Collect text from THIS paragraph's elements (skip the ref-ccc marker itself for text)
                current_text_parts = [el.get("text", "") for el in elements if el.get("type") == "text"]
                current_elements = list(elements)  # Keep all elements for formatted JSON
            else:
                # Continuation paragraph — belongs to current CCC number
                if current_ccc is not None:
                    continuation_text = [el.get("text", "") for el in elements if el.get("type") == "text"]
                    current_text_parts.extend(continuation_text)
                    current_elements.extend(elements)
        
        # Finalize last paragraph on this page
        if current_ccc is not None and current_text_parts and current_ccc not in paragraphs:
            paragraphs[current_ccc] = {
                "plain_text": clean_text(' '.join(current_text_parts)),
                "formatted_json": build_formatted_json(current_elements),
                "toc_path": get_toc_path_for_page(pn_id, toc_paths),
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
    
    # ── Step 3: Build SQLite database ──
    print(f"Building {OUTPUT_DB}...")
    OUTPUT_DB.parent.mkdir(parents=True, exist_ok=True)
    
    conn = sqlite3.connect(str(OUTPUT_DB))
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    
    # ── Create tables ──
    conn.executescript("""
        DROP TABLE IF EXISTS ccc_bible_refs;
        DROP TABLE IF EXISTS ccc_tradition_refs;
        DROP TABLE IF EXISTS ccc_tags;
        DROP TABLE IF EXISTS ccc_paragraphs;
        DROP TABLE IF EXISTS ccc_fts;
        
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
    
    # ── Rebuild FTS index ──
    conn.execute("INSERT INTO ccc_fts(ccc_fts) VALUES('rebuild')")
    
    # ── Verify ──
    count = conn.execute("SELECT COUNT(*) FROM ccc_paragraphs").fetchone()[0]
    bible_count = conn.execute("SELECT COUNT(*) FROM ccc_bible_refs").fetchone()[0]
    trad_count = conn.execute("SELECT COUNT(*) FROM ccc_tradition_refs").fetchone()[0]
    
    # Show sample
    print(f"\n=== Verification ===")
    print(f"  Paragraphs: {count}")
    print(f"  Bible refs: {bible_count}")
    print(f"  Tradition refs: {trad_count}")
    
    sample = conn.execute("SELECT number, substr(plain_text, 1, 120) FROM ccc_paragraphs WHERE number = 27").fetchone()
    if sample:
        print(f"\n  Sample ¶{sample[0]}: \"{sample[1]}...\"")
    
    sample_refs = conn.execute(
        "SELECT ref_text FROM ccc_bible_refs WHERE ccc_number = 27"
    ).fetchall()
    if sample_refs:
        print(f"  Bible refs in ¶27: {[r[0] for r in sample_refs]}")
    else:
        print(f"  No Bible refs in ¶27 (footnotes only)")
    
    conn.close()
    
    db_size = OUTPUT_DB.stat().st_size
    print(f"\nDone! {OUTPUT_DB} ({db_size:,} bytes)")


if __name__ == "__main__":
    main()
