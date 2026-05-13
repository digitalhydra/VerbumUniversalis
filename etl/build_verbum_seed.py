#!/usr/bin/env python3
"""
Verbum Seed DB builder - deterministic ETL.
Parses SWORD modules for 73 Catholic books.
Outputs: verbum_seed.db (same input -> same output).
"""

import os
import sys
import struct
import sqlite3
import re
import json
import xml.etree.ElementTree as ET
from pathlib import Path
import shutil

# ─── CONFIG ────────────────────────────────────────────────────────────────────

CWD = Path(__file__).parent.parent
RAW = CWD / "raw_data"
EXTRACTED = RAW / "_extracted/modules"

DB_PATH = CWD / "verbum_seed.db"
ASSETS_DIR = CWD / "app/src/main/assets"
ASSETS_DIR.mkdir(parents=True, exist_ok=True)
DB_ASSET_PATH = ASSETS_DIR / "verbum_seed.db"

# Catholic books: 46 OT + 27 NT = 73
BOOKS = [
    # (id, sword_code, name_en, name_es, name_la, testament)
    (1,  'Gen',  'Genesis',           'Génesis',        'Genesis',        'ot'),
    (2,  'Exod', 'Exodus',            'Éxodo',          'Exodus',         'ot'),
    (3,  'Lev',  'Leviticus',         'Levítico',       'Leviticus',      'ot'),
    (4,  'Num',  'Numbers',           'Números',        'Numeri',         'ot'),
    (5,  'Deut', 'Deuteronomy',       'Deuteronomio',   'Deuteronomium',  'ot'),
    (6,  'Josh', 'Joshua',            'Josué',          'Iosue',          'ot'),
    (7,  'Judg', 'Judges',            'Jueces',         'Iudices',        'ot'),
    (8,  'Ruth', 'Ruth',              'Rut',            'Ruth',           'ot'),
    (9,  '1Sam', '1 Samuel',         '1 Samuel',       '1 Samuelis',     'ot'),
    (10, '2Sam', '2 Samuel',          '2 Samuel',       '2 Samuelis',     'ot'),
    (11, '1Kgs', '1 Kings',          '1 Reyes',        '1 Regum',        'ot'),
    (12, '2Kgs', '2 Kings',          '2 Reyes',        '2 Regum',        'ot'),
    (13, '1Chr', '1 Chronicles',      '1 Crónicas',     '1 Paralipomenon','ot'),
    (14, '2Chr', '2 Chronicles',      '2 Crónicas',     '2 Paralipomenon','ot'),
    (15, 'Ezra', 'Ezra',             'Esdras',         'Esdrae',         'ot'),
    (16, 'Neh',  'Nehemiah',          'Nehemías',       'Nehemiae',       'ot'),
    (17, 'Tob',  'Tobit',             'Tobías',         'Tobiae',         'ot'),
    (18, 'Jdt',  'Judith',            'Judit',          'Iudith',         'ot'),
    (19, 'Esth', 'Esther',            'Ester',          'Esther',         'ot'),
    (20, 'Job',  'Job',               'Job',            'Iob',            'ot'),
    (21, 'Ps',   'Psalms',            'Salmos',         'Psalmi',         'ot'),
    (22, 'Prov', 'Proverbs',          'Proverbios',     'Proverbia',      'ot'),
    (23, 'Eccl', 'Ecclesiastes',      'Eclesiastés',   'Ecclesiastes',   'ot'),
    (24, 'Song', 'Song of Songs',     'Cantar de los Cantares', 'Canticum','ot'),
    (25, 'Wis',  'Wisdom',            'Sabiduría',     'Sapientia',      'ot'),
    (26, 'Sir',  'Sirach',            'Eclesiástico',  'Ecclesiasticus', 'ot'),
    (27, 'Isa',  'Isaiah',            'Isaías',         'Isaias',         'ot'),
    (28, 'Jer',  'Jeremiah',          'Jeremías',       'Ieremias',       'ot'),
    (29, 'Lam',  'Lamentations',      'Lamentaciones', 'Threni',         'ot'),
    (30, 'Bar',  'Baruch',            'Baruc',         'Baruch',         'ot'),
    (31, 'Ezek', 'Ezekiel',           'Ezequiel',       'Ezechiel',       'ot'),
    (32, 'Dan',  'Daniel',            'Daniel',         'Daniel',         'ot'),
    (33, 'Hos',  'Hosea',             'Oseas',         'Osee',           'ot'),
    (34, 'Joel', 'Joel',              'Joel',           'Ioel',           'ot'),
    (35, 'Amos', 'Amos',              'Amós',           'Amos',           'ot'),
    (36, 'Obad', 'Obadiah',           'Abdías',         'Abdias',         'ot'),
    (37, 'Jonah','Jonah',             'Jonás',          'Ionas',          'ot'),
    (38, 'Mic',  'Micah',             'Miqueas',        'Michaeas',       'ot'),
    (39, 'Nah',  'Nahum',             'Nahúm',         'Naum',           'ot'),
    (40, 'Hab',  'Habakkuk',          'Habacuc',        'Habacuc',        'ot'),
    (41, 'Zeph', 'Zephaniah',         'Sofonías',       'Sophonias',      'ot'),
    (42, 'Hag',  'Haggai',            'Ageo',           'Aggaeus',        'ot'),
    (43, 'Zech', 'Zechariah',         'Zacarías',       'Zacharias',      'ot'),
    (44, 'Mal',  'Malachi',           'Malaquías',      'Malachias',      'ot'),
    (45, '1Macc','1 Maccabees',       '1 Macabeos',     '1 Machabaeorum', 'ot'),
    (46, '2Macc','2 Maccabees',       '2 Macabeos',     '2 Machabaeorum', 'ot'),
    (47, 'Matt', 'Matthew',           'Mateo',          'Matthaeus',      'nt'),
    (48, 'Mark', 'Mark',              'Marcos',         'Marcus',         'nt'),
    (49, 'Luke', 'Luke',              'Lucas',          'Lucas',          'nt'),
    (50, 'John', 'John',              'Juan',           'Ioannes',        'nt'),
    (51, 'Acts', 'Acts',              'Hechos',         'Actus',          'nt'),
    (52, 'Rom',  'Romans',            'Romanos',        'Romanos',        'nt'),
    (53, '1Cor', '1 Corinthians',     '1 Corintios',    '1 Corinthios',   'nt'),
    (54, '2Cor', '2 Corinthians',     '2 Corintios',    '2 Corinthios',   'nt'),
    (55, 'Gal',  'Galatians',         'Gálatas',        'Galatas',        'nt'),
    (56, 'Eph',  'Ephesians',         'Efesios',        'Ephesios',       'nt'),
    (57, 'Phil', 'Philippians',       'Filipenses',      'Philippenses',   'nt'),
    (58, 'Col',  'Colossians',        'Colosenses',      'Colossenses',    'nt'),
    (59, '1Thess','1 Thessalonians',  '1 Tesalonicenses','1 Thessalonices','nt'),
    (60, '2Thess','2 Thessalonians',  '2 Tesalonicenses','2 Thessalonices','nt'),
    (61, '1Tim', '1 Timothy',         '1 Timoteo',      '1 Timotheum',    'nt'),
    (62, '2Tim', '2 Timothy',         '2 Timoteo',      '2 Timotheum',    'nt'),
    (63, 'Titus', 'Titus',            'Tito',           'Titum',          'nt'),
    (64, 'Phlm', 'Philemon',          'Filemón',        'Philemonem',     'nt'),
    (65, 'Heb',  'Hebrews',           'Hebreos',        'Hebraeos',       'nt'),
    (66, 'Jas',  'James',             'Santiago',       'Iacobi',         'nt'),
    (67, '1Pet', '1 Peter',           '1 Pedro',        '1 Petri',        'nt'),
    (68, '2Pet', '2 Peter',           '2 Pedro',        '2 Petri',        'nt'),
    (69, '1John','1 John',            '1 Juan',         '1 Ioannis',      'nt'),
    (70, '2John','2 John',            '2 Juan',         '2 Ioannis',      'nt'),
    (71, '3John','3 John',            '3 Juan',         '3 Ioannis',      'nt'),
    (72, 'Jude', 'Jude',              'Judas',          'Iudae',          'nt'),
    (73, 'Rev',  'Revelation',        'Apocalipsis',    'Apocalypsis',    'nt'),
]

# SWORD modules: (dir, lang_code, has_ot, has_nt, compress, source_type, notes)
MODULES = [
    ('drc',           'en_DRB',  True,  True,  'ZIP', 'OSIS', 'Douay-Rheims (EN)'),
    ('spaplatense',   'es_PLA',  True,  True,  'ZIP', 'OSIS', 'Platense (ES Whole Bible)'),
    ('vulgclementine','la_VUL',  True,  True,  'ZIP', 'OSIS', 'Vulgata Clementina (LA)'),
]

# ─── LZSS DECOMPRESS ───────────────────────────────────────────────────────────

def decompress_lzss(compressed_data):
    """Decompress SWORD LZSS data. Buffer initialized with spaces."""
    if not compressed_data:
        return b''
    buf_size = 4096
    buffer = bytearray([0x20] * buf_size)  # SWORD init: spaces (0x20)
    buf_pos = 0
    output = bytearray()
    pos = 0
    while pos < len(compressed_data):
        flags = compressed_data[pos]
        pos += 1
        for bit in range(8):
            if pos >= len(compressed_data):
                break
            if flags & (1 << bit):
                output.append(compressed_data[pos])
                buffer[buf_pos] = compressed_data[pos]
                buf_pos = (buf_pos + 1) % buf_size
                pos += 1
            else:
                if pos + 1 >= len(compressed_data):
                    break
                byte1 = compressed_data[pos]
                byte2 = compressed_data[pos + 1]
                pos += 2
                offset = byte1 | ((byte2 & 0x0F) << 8)
                length = (byte2 >> 4) + 3
                for _ in range(length):
                    src = (buf_pos - offset + buf_size) % buf_size
                    byte = buffer[src]
                    output.append(byte)
                    buffer[buf_pos] = byte
                    buf_pos = (buf_pos + 1) % buf_size
    return bytes(output)

# ─── SWORD MODULE PARSING ─────────────────────────────────────────────────────

def read_binary(path):
    with open(path, 'rb') as f:
        return f.read()


def parse_book_index(bzs_data):
    """Parse .bzs: 4 bytes per entry (book_num:2, offset:2)."""
    books = []
    for i in range(0, len(bzs_data), 4):
        if i + 3 >= len(bzs_data):
            break
        book_num = struct.unpack('<H', bzs_data[i:i+2])[0]
        offset = struct.unpack('<H', bzs_data[i+2:i+4])[0]
        books.append((book_num, offset))
    return books


def parse_verse_index(bzv_data):
    """Parse .bzv: 6 bytes per entry (verse_num:2, offset:4)."""
    verses = []
    for i in range(0, len(bzv_data), 6):
        if i + 5 >= len(bzv_data):
            break
        verse_num = struct.unpack('<H', bzv_data[i:i+2])[0]
        offset = struct.unpack('<I', bzv_data[i+2:i+6])[0]
        verses.append((verse_num, offset))
    return verses


def strip_thml(text):
    """Strip ThML/HTML tags and clean whitespace."""
    text = re.sub(r'<[^>]+>', ' ', text)
    text = re.sub(r'\s+', ' ', text).strip()
    return text


def extract_verses(text_data, verse_index):
    """Extract verse texts using offset table. Returns {verse_num: text}."""
    result = {}
    for idx, (verse_num, offset) in enumerate(verse_index):
        start = offset
        if idx + 1 < len(verse_index):
            end = verse_index[idx + 1][1]
        else:
            end = len(text_data)
        raw = text_data[start:end]
        # Remove null bytes
        raw = raw.replace(b'\x00', b'').replace(b'\r', b'').replace(b'\n', b' ')
        if raw:
            try:
                decoded = raw.decode('utf-8', errors='replace').strip()
                decoded = strip_thml(decoded)
                if decoded:
                    result[verse_num] = decoded
            except Exception:
                pass
    return result


def parse_lzss_nt_module(module_dir):
    """
    Parse LZSS-compressed NT SWORD module (spasciont).
    Returns list of (book_code, chapter, verse, text) tuples.
    """
    base = EXTRACTED / "texts/ztext" / module_dir
    if not base.exists():
        print(f"    SKIP: {base} not found")
        return []

    nt_bzz = base / 'nt.bzz'
    nt_bzv = base / 'nt.bzv'
    nt_bzs = base / 'nt.bzs'

    if not nt_bzz.exists():
        print(f"    SKIP: {nt_bzz} not found")
        return []

    # Decompress
    compressed = read_binary(nt_bzz)
    text_data = decompress_lzss(compressed)

    # Parse verse index
    if nt_bzv.exists():
        bzv_data = read_binary(nt_bzv)
        verse_index = parse_verse_index(bzv_data)
    else:
        print(f"    SKIP: {nt_bzv} not found")
        return []

    # Extract verses
    verse_texts = extract_verses(text_data, verse_index)

    # For NT, books are in KJV order: Matt=1, Mark=2, ..., Rev=27
    # The verse numbers in .bzv are sequential, not (book, chapter, verse)
    # We need to map sequential verses to (book, chapter, verse)

    # Read book index to get book boundaries
    book_boundaries = []
    if nt_bzs.exists():
        bzs_data = read_binary(nt_bzs)
        book_boundaries = parse_book_index(bzs_data)

    # Build result with proper (book_code, chapter, verse) mapping
    # This is complex - for now use a heuristic approach
    # SWORD stores consecutive verses, we need to track book/chapter boundaries

    # Alternative: Use the conf file's Scope=NTOnly and assume standard NT order
    nt_books = [b for b in BOOKS if b[5] == 'nt']

    # For spasciont, we know it's NT only
    # Map sequential verse numbers to (book, chapter, verse)
    # This requires knowing chapter/verse structure

    # Simpler approach: Use pysword-like iteration
    # Actually, let's just return the flat list and handle mapping later
    result = []
    for verse_num, text in verse_texts.items():
        # verse_num is sequential (1, 2, 3, ...)
        # We need to convert to (book_code, chapter, verse)
        # For now, just store with verse_num as a flat index
        result.append((None, None, verse_num, text))  # Placeholder

    return result


def parse_with_pysword(module_dir, testament, source_type='OSIS'):
    """
    Parse ZIP-compressed SWORD module using pysword.
    Returns {(book_code, chapter, verse): text}.
    """
    try:
        from pysword.bible import SwordBible, SwordModuleType
    except ImportError:
        print("  ERROR: pysword not installed. pip install pysword")
        return {}

    base = EXTRACTED / "texts/ztext" / module_dir
    if not base.exists():
        print(f"  SKIP: {base} not found")
        return {}

    try:
        bible = SwordBible(
            module_path=str(base),
            module_type=SwordModuleType.ZTEXT,
            versification='kjv',
            encoding='utf-8',
            source_type=source_type,
            compress_type='ZIP'
        )
    except Exception as e:
        print(f"  ERROR loading {module_dir}: {e}")
        return {}

    result = {}
    for book_entry in BOOKS:
        book_id, book_code, _, _, _, test = book_entry
        if test != testament:
            continue
        for chapter in range(1, 151):
            verses_found = False
            for verse in range(1, 201):
                try:
                    iter_data = bible.get_iter(
                        books=[book_code],
                        chapters=[chapter],
                        verses=[verse],
                        clean=True
                    )
                    items = list(iter_data)
                    if not items:
                        if verse == 1:
                            break
                        continue
                    verses_found = True
                    text = items[0].strip()
                    if text:
                        result[(book_code, chapter, verse)] = text
                except Exception:
                    if verse == 1:
                        break
                    continue
            if not verses_found:
                break
    return result

# ─── DB SETUP ──────────────────────────────────────────────────────────────────

def create_fresh_db(path):
    """Create fresh verbum_seed.db with schema."""
    if path.exists():
        path.unlink()
    conn = sqlite3.connect(str(path))
    conn.executescript("""
        CREATE TABLE books (
            id INTEGER PRIMARY KEY NOT NULL,
            name_en TEXT NOT NULL,
            name_es TEXT NOT NULL,
            name_la TEXT NOT NULL,
            testament TEXT NOT NULL
        );
        CREATE TABLE verses (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            book_id INTEGER NOT NULL,
            chapter INTEGER NOT NULL,
            verse_number INTEGER NOT NULL,
            FOREIGN KEY(book_id) REFERENCES books(id)
        );
        CREATE INDEX idx_verses_book ON verses(book_id, chapter, verse_number);
        CREATE TABLE texts (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            verse_id INTEGER NOT NULL,
            lang_code TEXT NOT NULL,
            content TEXT NOT NULL,
            FOREIGN KEY(verse_id) REFERENCES verses(id)
        );
        CREATE INDEX idx_texts_verse ON texts(verse_id, lang_code);
        CREATE TABLE interlinear_words (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            verse_id INTEGER NOT NULL,
            word_order INTEGER NOT NULL,
            original TEXT NOT NULL,
            transliteration TEXT,
            literal TEXT,
            morphology TEXT,
            lemma TEXT,
            FOREIGN KEY(verse_id) REFERENCES verses(id)
        );
        CREATE INDEX idx_interlinear_verse ON interlinear_words(verse_id);
        CREATE TABLE lexicon (
            lemma TEXT PRIMARY KEY NOT NULL,
            language TEXT NOT NULL,
            definition TEXT NOT NULL
        );
    """)
    conn.commit()
    return conn


def populate_books(conn):
    """Insert 73 Catholic books."""
    for entry in BOOKS:
        # BOOKS entry: (id, sword_code, name_en, name_es, name_la, testament)
        # Table: id, name_en, name_es, name_la, testament
        data = (entry[0], entry[2], entry[3], entry[4], entry[5])
        conn.execute(
            "INSERT INTO books (id, name_en, name_es, name_la, testament) VALUES (?, ?, ?, ?, ?)",
            data
        )
    conn.commit()
    print(f"  Inserted {len(BOOKS)} books")


def build_verse_lookup(conn):
    """Build lookup: (book_id, chapter, verse_number) -> verse_id."""
    rows = conn.execute(
        "SELECT id, book_id, chapter, verse_number FROM verses ORDER BY book_id, chapter, verse_number"
    ).fetchall()
    return {(r[1], r[2], r[3]): r[0] for r in rows}

# ─── TEXT INSERTION ────────────────────────────────────────────────────────────

def insert_texts(conn, verse_lookup, parsed, lang_code):
    """
    Insert parsed texts into DB.
    parsed: {(book_code, chapter, verse): text}
    """
    count = 0
    # Sort for determinism
    sorted_items = sorted(parsed.items(), key=lambda x: (
        next((i for i, b in enumerate(BOOKS) if b[1] == x[0][0]), 999),
        x[0][1],  # chapter
        x[0][2],  # verse
    ))

    for (book_code, chapter, verse), text in sorted_items:
        # Find book_id
        book_entry = next((b for b in BOOKS if b[1] == book_code), None)
        if not book_entry:
            continue
        book_id = book_entry[0]

        # Get or create verse
        key = (book_id, chapter, verse)
        if key not in verse_lookup:
            cur = conn.execute(
                "INSERT INTO verses (book_id, chapter, verse_number) VALUES (?, ?, ?)",
                (book_id, chapter, verse)
            )
            verse_id = cur.lastrowid
            verse_lookup[key] = verse_id
        else:
            verse_id = verse_lookup[key]

        # Insert text (skip if exists)
        existing = conn.execute(
            "SELECT 1 FROM texts WHERE verse_id=? AND lang_code=?",
            (verse_id, lang_code)
        ).fetchone()
        if not existing:
            conn.execute(
                "INSERT INTO texts (verse_id, lang_code, content) VALUES (?, ?, ?)",
                (verse_id, lang_code, text)
            )
            count += 1

    conn.commit()
    print(f"  Inserted {count} texts for {lang_code}")


# ─── TRANSLITERATION ──────────────────────────────────────────────────────────

def transliterate_greek(text):
    """
    SBL-style transliteration: Greek polytonic Unicode → Latin.
    Handles letters with breathing marks, accents, iota subscript.
    """
    GREEK_MAP = {
        # Alpha
        '\u03b1': 'a', '\u1f00': 'a', '\u1f01': 'ha', '\u1f02': 'a', '\u1f03': 'ha',
        '\u1f04': 'a', '\u1f05': 'ha', '\u1f06': 'a', '\u1f07': 'ha',
        '\u1f70': 'a', '\u1f71': 'a', '\u1fb6': 'a', '\u1fb7': 'a',
        '\u0391': 'A', '\u1f08': 'A', '\u1f09': 'Ha', '\u1f0a': 'A', '\u1f0b': 'Ha',
        '\u1f0c': 'A', '\u1f0d': 'Ha', '\u1f0e': 'A', '\u1f0f': 'Ha',
        '\u1fb8': 'A', '\u1fb9': 'A', '\u1fba': 'A', '\u1fbb': 'Ha',
        '\u1fbc': 'A',
        # Epsilon
        '\u03b5': 'e', '\u1f10': 'e', '\u1f11': 'he', '\u1f12': 'e', '\u1f13': 'he',
        '\u1f14': 'e', '\u1f15': 'he', '\u1f72': 'e', '\u1f73': 'e',
        '\u0395': 'E', '\u1f18': 'E', '\u1f19': 'He', '\u1f1a': 'E', '\u1f1b': 'He',
        '\u1f1c': 'E', '\u1f1d': 'He', '\u1fc8': 'E', '\u1fc9': 'E',
        # Eta
        '\u03b7': '\u0113', '\u1f20': '\u0113', '\u1f21': 'h\u0113', '\u1f22': '\u0113', '\u1f23': 'h\u0113',
        '\u1f24': '\u0113', '\u1f25': 'h\u0113', '\u1f26': '\u0113', '\u1f27': 'h\u0113',
        '\u1f74': '\u0113', '\u1f75': '\u0113', '\u1fc6': '\u0113', '\u1fc7': '\u0113',
        '\u0397': '\u0112', '\u1f28': '\u0112', '\u1f29': 'H\u0112', '\u1f2a': '\u0112', '\u1f2b': 'H\u0112',
        '\u1f2c': '\u0112', '\u1f2d': 'H\u0112', '\u1f2e': '\u0112', '\u1f2f': 'H\u0112',
        '\u1fca': '\u0112', '\u1fcb': '\u0112', '\u1fcc': '\u0112',
        # Iota
        '\u03b9': 'i', '\u1f30': 'i', '\u1f31': 'hi', '\u1f32': 'i', '\u1f33': 'hi',
        '\u1f34': 'i', '\u1f35': 'hi', '\u1f36': 'i', '\u1f37': 'hi',
        '\u1f76': 'i', '\u1f77': 'i', '\u1fd6': 'i', '\u1fd7': 'i',
        '\u03ca': 'i', '\u1fd2': 'i', '\u1fd3': 'i',
        '\u0399': 'I', '\u1f38': 'I', '\u1f39': 'Hi', '\u1f3a': 'I', '\u1f3b': 'Hi',
        '\u1f3c': 'I', '\u1f3d': 'Hi', '\u1f3e': 'I', '\u1f3f': 'Hi',
        '\u1fd8': 'I', '\u1fd9': 'I', '\u1fda': 'I', '\u1fdb': 'I',
        '\u1fd0': 'i', '\u1fd1': 'i',
        # Omicron
        '\u03bf': 'o', '\u1f40': 'o', '\u1f41': 'ho', '\u1f42': 'o', '\u1f43': 'ho',
        '\u1f44': 'o', '\u1f45': 'ho', '\u1f78': 'o', '\u1f79': 'o',
        '\u039f': 'O', '\u1f48': 'O', '\u1f49': 'Ho', '\u1f4a': 'O', '\u1f4b': 'Ho',
        '\u1f4c': 'O', '\u1f4d': 'Ho', '\u1ff8': 'O', '\u1ff9': 'O',
        # Upsilon
        '\u03c5': 'y', '\u1f50': 'y', '\u1f51': 'hy', '\u1f52': 'y', '\u1f53': 'hy',
        '\u1f54': 'y', '\u1f55': 'hy', '\u1f56': 'y', '\u1f57': 'hy',
        '\u1f7a': 'y', '\u1f7b': 'y', '\u1fe6': 'y', '\u1fe7': 'y',
        '\u03cb': 'y', '\u1fe2': 'y', '\u1fe3': 'y',
        '\u03a5': 'Y', '\u1f59': 'Hy', '\u1f5b': 'Hy', '\u1f5d': 'Hy', '\u1f5f': 'Hy',
        '\u1fe8': 'Y', '\u1fe9': 'Y', '\u1fea': 'Y', '\u1feb': 'Y',
        '\u1fe0': 'y', '\u1fe1': 'y',
        # Omega
        '\u03c9': '\u014d', '\u1f60': '\u014d', '\u1f61': 'h\u014d', '\u1f62': '\u014d', '\u1f63': 'h\u014d',
        '\u1f64': '\u014d', '\u1f65': 'h\u014d', '\u1f66': '\u014d', '\u1f67': 'h\u014d',
        '\u1f7c': '\u014d', '\u1f7d': '\u014d', '\u1ff6': '\u014d', '\u1ff7': '\u014d',
        '\u03a9': '\u014c', '\u1f68': '\u014c', '\u1f69': 'H\u014c', '\u1f6a': '\u014c', '\u1f6b': 'H\u014c',
        '\u1f6c': '\u014c', '\u1f6d': 'H\u014c', '\u1f6e': '\u014c', '\u1f6f': 'H\u014c',
        '\u1ffa': '\u014c', '\u1ffb': '\u014c', '\u1ffc': '\u014c',
        # Rho
        '\u03c1': 'r', '\u1fe4': 'r', '\u1fe5': 'rh', '\u1fec': 'r',
        '\u03a1': 'R',
        # Tonos/accented vowels (V + tonos = U+03AC-03CE range)
        '\u03ac': 'a',  # ά alpha with tonos
        '\u03ad': 'e',  # έ epsilon with tonos
        '\u03ae': '\u0113',  # ή eta with tonos
        '\u03af': 'i',  # ί iota with tonos
        '\u03cc': 'o',  # ό omicron with tonos
        '\u03cd': 'y',  # ύ upsilon with tonos
        '\u03ce': '\u014d',  # ώ omega with tonos
        '\u0386': 'A',  # Ά alpha with tonos (capital)
        '\u0388': 'E',  # Έ epsilon with tonos (capital)
        '\u0389': '\u0112',  # Ή eta with tonos (capital)
        '\u038a': 'I',  # Ί iota with tonos (capital)
        '\u038c': 'O',  # Ό omicron with tonos (capital)
        '\u038e': 'Y',  # Ύ upsilon with tonos (capital)
        '\u038f': '\u014c',  # Ώ omega with tonos (capital)
        # Iota with dialytika + tonos
        '\u0390': 'i',  # ΐ
        '\u03b0': 'y',  # ΰ
        # Other consonants (no diacritic variants needed)
        '\u03b2': 'b', '\u03b3': 'g', '\u03b4': 'd', '\u03b6': 'z',
        '\u03b8': 'th', '\u03ba': 'k', '\u03bb': 'l', '\u03bc': 'm',
        '\u03bd': 'n', '\u03be': 'x', '\u03c0': 'p', '\u03c3': 's',
        '\u03c2': 's', '\u03c4': 't', '\u03c6': 'ph', '\u03c7': 'ch',
        '\u03c8': 'ps',
        '\u0392': 'B', '\u0393': 'G', '\u0394': 'D', '\u0396': 'Z',
        '\u0398': 'Th', '\u039a': 'K', '\u039b': 'L', '\u039c': 'M',
        '\u039d': 'N', '\u039e': 'X', '\u03a0': 'P', '\u03a3': 'S',
        '\u03a4': 'T', '\u03a6': 'Ph', '\u03a7': 'Ch', '\u03a8': 'Ps',
        # Punctuation / special
        '\u00b7': '',  # middle dot (ano teleia)
        '\u037e': '?',
    }
    result = []
    for ch in text:
        result.append(GREEK_MAP.get(ch, ch))
    return ''.join(result)


def transliterate_hebrew(text):
    """
    Basic transliteration: Hebrew Unicode → Latin.
    Follows SBL general-purpose style. Simplified: skips cantillation,
    maps consonants + vowels only.
    """
    # Hebrew Unicode ranges
    # Consonants: 05D0-05EA
    # Vowels (nikkud): 05B0-05BC (sheva, hatafim, hiriq, tsere, segol, patah, qamats, holam, dagesh)
    #   also 05C7 (qamats qatan)
    # Shin/sin dots: 05C1 (shin), 05C2 (sin)
    # Cantillation marks: 0591-05AF, 05BD-05BF
    # Other marks: 05C0 (paseq), 05C3 (sof pasuq), 05C4-05C5

    CONSONANT_MAP = {
        '\u05d0': '\u02bc',  # alef
        '\u05d1': 'b', '\u05d2': 'g', '\u05d3': 'd',
        '\u05d4': 'h', '\u05d5': 'w', '\u05d6': 'z',
        '\u05d7': '\u1e25', '\u05d8': '\u1e6d', '\u05d9': 'y',
        '\u05db': 'k', '\u05da': 'k',  # kaf (final)
        '\u05dc': 'l', '\u05de': 'm', '\u05dd': 'm',
        '\u05e0': 'n', '\u05df': 'n',
        '\u05e1': 's', '\u05e2': '\u02bf',
        '\u05e4': 'p', '\u05e3': 'p',
        '\u05e6': '\u1e63', '\u05e5': '\u1e63',
        '\u05e7': 'q', '\u05e8': 'r',
        '\u05e9': '\u0161', '\u05ea': 't',
    }
    VOWEL_MAP = {
        '\u05b0': '\u0115',  # sheva
        '\u05b1': '\u0115',  # hataf segol
        '\u05b2': '\u0103',  # hataf patah
        '\u05b3': '\u014f',  # hataf qamats
        '\u05b4': 'i',  # hiriq
        '\u05b5': '\u0113',  # tsere
        '\u05b6': 'e',  # segol
        '\u05b7': 'a',  # patah
        '\u05b8': '\u0101',  # qamats
        '\u05b9': '\u014d',  # holam
        '\u05ba': 'o',  # holam haser
        '\u05bb': 'u',  # qibbuts
        '\u05c7': 'o',  # qamats qatan
    }
    # Characters to skip (cantillation, dagesh, dots, etc.)
    SKIP = set()
    for cp in range(0x0591, 0x05AF + 1):
        SKIP.add(chr(cp))
    for cp in range(0x05BD, 0x05BF + 1):
        SKIP.add(chr(cp))
    SKIP.update({chr(cp) for cp in [0x05BC, 0x05C0, 0x05C1, 0x05C2, 0x05C3, 0x05C4, 0x05C5]})
    SKIP.update({'\u05be', '\u05c3'})  # maqaf, sof pasuq

    result = []
    for ch in text:
        if ch in SKIP:
            continue
        if ch in CONSONANT_MAP:
            result.append(CONSONANT_MAP[ch])
        elif ch in VOWEL_MAP:
            result.append(VOWEL_MAP[ch])
        elif ch == '\u05be':
            result.append('-')
    return ''.join(result)


# ─── INTERLINEAR PARSING ──────────────────────────────────────────────────────

def parse_interlinear_osis(osis_text):
    """Parse OSIS <w> tags for interlinear data."""
    words = []
    pattern = r'<w\b([^>]*)>(.*?)</w>'
    for match in re.finditer(pattern, osis_text, re.DOTALL):
        attrs_str = match.group(1)
        content = match.group(2).strip()
        if not content:
            continue
        lemma = None
        lm = re.search(r'lemma="([^"]*)"', attrs_str)
        if lm:
            lemma = lm.group(1)
        morph = None
        mm = re.search(r'morph="([^"]*)"', attrs_str)
        if mm:
            morph = mm.group(1)
        translit = None
        tm = re.search(r'translit="([^"]*)"', attrs_str)
        if tm:
            translit = tm.group(1)
        words.append({
            'original': content,
            'lemma': lemma,
            'morphology': morph,
            'transliteration': translit,
        })
    return words


def parse_interlinear():
    """Parse ABPGRK (Greek NT) and OSHB (Hebrew OT) interlinear."""
    try:
        from pysword.bible import SwordBible, SwordModuleType
    except ImportError:
        print("  ERROR: pysword not installed")
        return

    conn = sqlite3.connect(str(DB_PATH))
    verse_lookup = build_verse_lookup(conn)

    # ABPGRK (Greek, NT)
    abp_path = EXTRACTED / "texts/ztext/abpgrk"
    if abp_path.exists():
        print("\n  Parsing ABPGRK (Greek interlinear, NT)...")
        try:
            bible = SwordBible(
                module_path=str(abp_path),
                module_type=SwordModuleType.ZTEXT,
                versification='kjv',
                encoding='utf-8',
                source_type='OSIS',
                compress_type='ZIP'
            )
            count = 0
            for book_entry in BOOKS:
                book_id, book_code, _, _, _, test = book_entry
                if test != 'nt':
                    continue
                for chapter in range(1, 151):
                    verses_found = False
                    for verse in range(1, 201):
                        try:
                            result = list(bible.get_iter(
                                books=[book_code], chapters=[chapter], verses=[verse], clean=False
                            ))
                            if not result:
                                if verse == 1:
                                    break
                                continue
                            verses_found = True
                            raw = result[0]
                            words = parse_interlinear_osis(raw)
                            if not words:
                                continue
                            key = (book_id, chapter, verse)
                            if key not in verse_lookup:
                                continue
                            verse_id = verse_lookup[key]
                            for idx, wd in enumerate(words, 1):
                                conn.execute(
                                    "INSERT OR IGNORE INTO interlinear_words "
                                    "(verse_id, word_order, original, lemma, morphology, transliteration) "
                                    "VALUES (?, ?, ?, ?, ?, ?)",
                                    (verse_id, idx, wd['original'], wd['lemma'], wd['morphology'], wd['transliteration'])
                                )
                                count += 1
                        except Exception:
                            if verse == 1:
                                break
                            continue
                    if not verses_found:
                        break
            conn.commit()
            print(f"    Inserted {count} Greek interlinear words")
        except Exception as e:
            print(f"    ERROR parsing ABPGRK: {e}")

    # OSHB (Hebrew, OT)
    oshb_path = EXTRACTED / "texts/ztext/oshb"
    if oshb_path.exists():
        print("\n  Parsing OSHB (Hebrew interlinear, OT)...")
        try:
            bible = SwordBible(
                module_path=str(oshb_path),
                module_type=SwordModuleType.ZTEXT,
                versification='leningrad',
                encoding='utf-8',
                source_type='OSIS',
                compress_type='ZIP'
            )
            count = 0
            for book_entry in BOOKS:
                book_id, book_code, _, _, _, test = book_entry
                if test != 'ot':
                    continue
                for chapter in range(1, 151):
                    verses_found = False
                    for verse in range(1, 201):
                        try:
                            result = list(bible.get_iter(
                                books=[book_code], chapters=[chapter], verses=[verse], clean=False
                            ))
                            if not result:
                                if verse == 1:
                                    break
                                continue
                            verses_found = True
                            raw = result[0]
                            words = parse_interlinear_osis(raw)
                            if not words:
                                continue
                            key = (book_id, chapter, verse)
                            if key not in verse_lookup:
                                continue
                            verse_id = verse_lookup[key]
                            for idx, wd in enumerate(words, 1):
                                conn.execute(
                                    "INSERT OR IGNORE INTO interlinear_words "
                                    "(verse_id, word_order, original, lemma, morphology, transliteration) "
                                    "VALUES (?, ?, ?, ?, ?, ?)",
                                    (verse_id, idx, wd['original'], wd['lemma'], wd['morphology'], wd['transliteration'])
                                )
                                count += 1
                        except Exception:
                            if verse == 1:
                                break
                            continue
                    if not verses_found:
                        break
            conn.commit()
            print(f"    Inserted {count} Hebrew interlinear words")
        except Exception as e:
            print(f"    ERROR parsing OSHB: {e}")

    conn.close()


def build_strongs_gloss_map():
    """
    Parse Strong's dictionaries and build lemma → English gloss lookup.
    Returns dict: { 'strong:G976': 'book', 'strong:H07225': 'beginning', ... }
    """
    gloss_map = {}

    # ── Greek Strong's ──
    greek_xml = RAW / "strongs-master/greek/StrongsGreekDictionaryXML_1.4/strongsgreek.xml"
    if greek_xml.exists():
        try:
            import xml.etree.ElementTree as ET
            tree = ET.parse(str(greek_xml))
            root = tree.getroot()
            entries = root.find('entries')
            if entries is not None:
                for entry in entries.findall('entry'):
                    strongs_num = entry.get('strongs', '')
                    if not strongs_num:
                        continue
                    lemma_key = f"strong:G{int(strongs_num)}"

                    # Get kjv_def as the concise gloss
                    kjv_el = entry.find('kjv_def')
                    if kjv_el is None:
                        continue
                    raw = ''.join(kjv_el.itertext()).strip()
                    # Clean KJV formatting: "--book." → "book"
                    # ":--do good." → "do good"
                    gloss = raw.replace('--', '').replace(':--', '').strip(' .:;,-')
                    # Take first word/phrase before comma or semicolon, strip newlines
                    gloss = gloss.split(',')[0].split(';')[0].replace('\n', ' ').strip()
                    if gloss and not gloss.startswith('X '):  # X = untranslated in KJV
                        gloss_map[lemma_key] = gloss
            print(f"    Parsed {len(gloss_map)} Greek glosses from Strong's")
        except Exception as e:
            print(f"    ERROR parsing Greek Strong's: {e}")
    else:
        print(f"    Greek Strong's XML not found at {greek_xml}")

    # ── Hebrew Strong's ──
    hebrew_xml = RAW / "strongs-master/hebrew/StrongHebrewG.xml"
    if hebrew_xml.exists():
        try:
            import xml.etree.ElementTree as ET
            tree = ET.parse(str(hebrew_xml))
            root = tree.getroot()
            # Hebrew XML uses OSIS namespace
            ns = {'osis': 'http://www.bibletechnologies.net/2003/OSIS/namespace'}
            for entry in root.findall('.//osis:div[@type="entry"]', ns):
                entry_num = entry.get('n', '')
                if not entry_num:
                    continue
                lemma_key = f"strong:H{int(entry_num)}"  # No zero-padding: H430 not H00430

                # Get first list item as gloss
                item = entry.find('.//osis:item', ns)
                if item is None:
                    continue
                raw = ''.join(item.itertext()).strip()
                # Clean numbering like "1) " prefix
                gloss = raw.split(')', 1)[-1].strip() if ')' in raw else raw
                if gloss:
                    gloss_map[lemma_key] = gloss
            print(f"    Parsed {len(gloss_map) - (len(gloss_map) - len([k for k in gloss_map if k.startswith('strong:G')]))} Hebrew glosses")
        except Exception as e:
            print(f"    ERROR parsing Hebrew Strong's: {e}")
    else:
        print(f"    Hebrew Strong's XML not found at {hebrew_xml}")

    return gloss_map


def populate_transliteration_and_glosses(gloss_map):
    """
    Post-process interlinear_words table:
    - Set transliteration column from original text (algorithmic)
    - Set literal column from Strong's gloss lookup
    """
    conn = sqlite3.connect(str(DB_PATH))

    # ── Transliteration ──
    print("\n  Generating transliterations...")
    rows = conn.execute("SELECT id, original FROM interlinear_words WHERE transliteration IS NULL").fetchall()
    greek_count = 0
    hebrew_count = 0
    
    # Batch updates for performance
    batch_size = 10000
    batch = []
    
    def flush_batch():
        if batch:
            conn.executemany("UPDATE interlinear_words SET transliteration = ? WHERE id = ?", batch)
            batch.clear()
    
    for row_id, original in rows:
        if not original:
            continue
        # Detect script: Greek or Hebrew
        first_char = original[0]
        if '\u0370' <= first_char <= '\u03ff' or '\u1f00' <= first_char <= '\u1fff':
            translit = transliterate_greek(original)
            greek_count += 1
        elif '\u0590' <= first_char <= '\u05ff':
            translit = transliterate_hebrew(original)
            hebrew_count += 1
        else:
            continue
        batch.append((translit, row_id))
        if len(batch) >= batch_size:
            flush_batch()
    flush_batch()
    conn.commit()
    print(f"    Transliterated: {greek_count} Greek, {hebrew_count} Hebrew")

    # ── Glosses from Strong's ──
    if gloss_map:
        print("  Populating English glosses from Strong's...")
        updated = 0
        # Fetch all distinct lemmas and normalize them for lookup
        rows = conn.execute("SELECT DISTINCT lemma FROM interlinear_words WHERE lemma IS NOT NULL AND literal IS NULL").fetchall()
        gloss_updates = []
        for (lemma,) in rows:
            # Normalize: 'strong:H0430' -> 'strong:H430', 'strong:G0976' -> 'strong:G976'
            if lemma.startswith('strong:G'):
                num = lemma[8:].lstrip('0') or '0'
                canon = f'strong:G{num}'
            elif lemma.startswith('strong:H'):
                num = lemma[8:].lstrip('0') or '0'
                canon = f'strong:H{num}'
            else:
                continue
            gloss = gloss_map.get(canon)
            if gloss:
                gloss_updates.append((gloss, lemma))
        if gloss_updates:
            conn.executemany("UPDATE interlinear_words SET literal = ? WHERE lemma = ? AND literal IS NULL", gloss_updates)
            conn.commit()
            # Count actual updated rows
            updated = conn.execute("SELECT COUNT(*) FROM interlinear_words WHERE literal IS NOT NULL").fetchone()[0]
        print(f"    Populated {updated} glosses")

    conn.close()


# ─── STRONG'S LEXICON ─────────────────────────────────────────────────────────

def parse_strongs_xml(xml_path, language):
    """Parse Strong's XML dictionary."""
    if not xml_path.exists():
        print(f"  SKIP: {xml_path} not found")
        return {}

    try:
        tree = ET.parse(str(xml_path))
        root = tree.getroot()
    except Exception as e:
        print(f"  ERROR parsing {xml_path}: {e}")
        return {}

    result = {}
    for entry in root.findall('.//entry'):
        strongs_num = entry.get('strongs')
        if not strongs_num:
            continue

        # Build definition
        parts = []
        for elem in entry:
            if elem.tag == 'strongs_def':
                text = ''.join(elem.itertext()).strip()
                if text:
                    parts.append(f"Strong's: {text}")
            elif elem.tag == 'kjv_def':
                text = ''.join(elem.itertext()).strip()
                if text:
                    parts.append(f"KJV: {text}")
            elif elem.tag == 'strongs_derivation':
                text = ''.join(elem.itertext()).strip()
                if text:
                    parts.append(f"Derivation: {text}")
            elif elem.tag == 'greek':
                translit = elem.get('translit', '')
                if translit:
                    parts.append(f"Translit: {translit}")

        definition = ' | '.join(parts)
        result[strongs_num] = (language, definition)

    return result


def parse_strongs():
    """Parse Strong's dictionaries into lexicon table."""
    conn = sqlite3.connect(str(DB_PATH))

    # Clear existing
    conn.execute("DELETE FROM lexicon")
    conn.commit()

    # Greek
    greek_xml = RAW / "strongs-master/greek/StrongsGreekDictionaryXML_1.4/strongsgreek.xml"
    if greek_xml.exists():
        print("\n  Parsing Greek Strong's...")
        data = parse_strongs_xml(greek_xml, 'grc')
        count = 0
        for strong_num, (lang, definition) in sorted(data.items()):
            conn.execute(
                "INSERT OR REPLACE INTO lexicon (lemma, language, definition) VALUES (?, ?, ?)",
                (strong_num, lang, definition)
            )
            count += 1
        conn.commit()
        print(f"    Inserted {count} Greek entries")
    else:
        print(f"\n  Greek Strong's XML not found at {greek_xml}")

    # Hebrew
    hebrew_xml = RAW / "strongs-master/hebrew/strongshebrew.xml"
    if hebrew_xml.exists():
        print("\n  Parsing Hebrew Strong's...")
        data = parse_strongs_xml(hebrew_xml, 'hbo')
        count = 0
        for strong_num, (lang, definition) in sorted(data.items()):
            conn.execute(
                "INSERT OR REPLACE INTO lexicon (lemma, language, definition) VALUES (?, ?, ?)",
                (strong_num, lang, definition)
            )
            count += 1
        conn.commit()
        print(f"    Inserted {count} Hebrew entries")
    else:
        print(f"\n  Hebrew Strong's XML not found at {hebrew_xml}")

    conn.close()


# ─── MAIN ETL PIPELINE ────────────────────────────────────────────────────────

def main():
    print("=" * 60)
    print("VERBUM SEED ETL - Deterministic Build")
    print("=" * 60)

    # Step 1: Fresh DB
    print("\n[1/5] Creating fresh database...")
    conn = create_fresh_db(DB_PATH)
    populate_books(conn)
    print(f"  DB: {DB_PATH}")

    # Step 2: Parse SWORD modules for texts
    print("\n[2/5] Parsing SWORD modules for Bible texts...")
    verse_lookup = {}

    for module_dir, lang_code, has_ot, has_nt, compress, source_type, desc in MODULES:
        print(f"\n  Parsing {desc} ({module_dir})...")
        if compress == 'ZIP':
            if has_ot:
                print(f"    OT ({lang_code})...")
                parsed = parse_with_pysword(module_dir, 'ot', source_type)
                print(f"    Parsed {len(parsed)} OT verses")
                insert_texts(conn, verse_lookup, parsed, lang_code)
            if has_nt:
                print(f"    NT ({lang_code})...")
                parsed = parse_with_pysword(module_dir, 'nt', source_type)
                print(f"    Parsed {len(parsed)} NT verses")
                insert_texts(conn, verse_lookup, parsed, lang_code)
        else:
            print(f"    SKIPPED (unsupported compression)")

    # Step 3: Interlinear (ABPGRK + OSHB)
    print("\n[3/5] Parsing interlinear (Greek + Hebrew)...")
    parse_interlinear()

    # Step 3.5: Build Strong's gloss map + populate transliteration and glosses
    print("\n[3.5] Building Strong's gloss map...")
    gloss_map = build_strongs_gloss_map()
    print("\n[3.6] Populating transliterations and English glosses...")
    populate_transliteration_and_glosses(gloss_map)

    # Step 4: Strong's Lexicon (full definitions)
    print("\n[4/5] Parsing Strong's Lexicon...")
    parse_strongs()

    # Step 5: Stats + Copy to assets
    print("\n[5/5] Final stats...")
    conn = sqlite3.connect(str(DB_PATH))
    stats = {
        'books': conn.execute("SELECT COUNT(*) FROM books").fetchone()[0],
        'verses': conn.execute("SELECT COUNT(*) FROM verses").fetchone()[0],
        'texts': conn.execute("SELECT COUNT(*) FROM texts").fetchone()[0],
        'interlinear': conn.execute("SELECT COUNT(*) FROM interlinear_words").fetchone()[0],
        'lexicon': conn.execute("SELECT COUNT(*) FROM lexicon").fetchone()[0],
    }
    conn.close()

    for key, val in stats.items():
        print(f"  {key}: {val}")

    # Copy to assets
    shutil.copy2(DB_PATH, DB_ASSET_PATH)
    print(f"\n  Copied to {DB_ASSET_PATH}")

    print("\n" + "=" * 60)
    print("ETL COMPLETE")
    print("=" * 60)


if __name__ == '__main__':
    main()
