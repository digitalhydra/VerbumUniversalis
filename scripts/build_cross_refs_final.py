#!/usr/bin/env python3
"""
Verbum Universalis - Cross-Reference Engine Builder
==============================================

Parses cross_references.txt (tab-separated) and creates
a cross_references table in the catena database.

Format: From Verse    To Verse    Votes    #metadata
Example: Gen.1.1    Rev.21.6    56

Book abbreviations use standard Bible abbreviations.
"""

import sqlite3
import re
from pathlib import Path
from collections import defaultdict

# === PATHS ===
SCRIPT_DIR = Path(__file__).parent
CROSS_REF_DIR = SCRIPT_DIR.parent  # Go up one level to raw_data/
CROSS_REF_FILE = CROSS_REF_DIR / "cross_references.txt"
OUTPUT_DB = SCRIPT_DIR / "verbum_cross_refs.db"

# === BOOK ABBREVIATION MAP ===
# openbible.info uses standard abbreviations
BOOK_MAP = {
    # Old Testament
    'gen': 'genesis', 'exod': 'exodus', 'lev': 'leviticus', 'num': 'numbers',
    'deut': 'deuteronomy', 'jos': 'joshua', 'judg': 'judges', 'ruth': 'ruth',
    '1sam': '1_samuel', '2sam': '2_samuel',
    '1kin': '1_kings', '2kin': '2_kings',
    '1chr': '1_chronicles', '2chr': '2_chronicles',
    'ezra': 'ezra', 'nehm': 'nehemiah', 'esth': 'esther',
    'job': 'job', 'ps': 'psalms', 'prov': 'proverbs',
    'eccl': 'ecclesiastes', 'song': 'song_of_songs',
    'wis': 'wisdom', 'sir': 'sirach',
    'isa': 'isaiah', 'jer': 'jeremiah', 'lam': 'lamentations',
    'bar': 'baruch', 'ezek': 'ezekiel', 'dan': 'daniel',
    'hos': 'hosea', 'joel': 'joel', 'amos': 'amos',
    'obad': 'obadiah', 'jonah': 'jonah', 'mic': 'micah',
    'nah': 'nahum', 'hab': 'habakkuk', 'zeph': 'zephaniah',
    'hag': 'haggai', 'zech': 'zechariah', 'mal': 'malachi',
    '1macc': '1_maccabees', '2macc': '2_maccabees',
    
    # New Testament  
    'mat': 'matthew', 'matt': 'matthew', 'mt': 'matthew', 'matthew': 'matthew',
    'mar': 'mark', 'mrk': 'mark', 'mk': 'mark', 'mark': 'mark',
    'luk': 'luke', 'lk': 'luke', 'luke': 'luke',
    'joh': 'john', 'jn': 'john', 'john': 'john',
    'act': 'acts', 'acts': 'acts',
    'rom': 'romans',
    '1cor': '1_corinthians', '2cor': '2_corinthians',
    'gal': 'galatians', 'eph': 'ephesians', 'phil': 'philippians',
    'col': 'colossians',
    '1thess': '1_thessalonians', '2thess': '2_thessalonians',
    '1tim': '1_timothy', '2tim': '2_timothy', 'tit': 'titus',
    'phlm': 'philemon', 'heb': 'hebrews', 'jas': 'james',
    '1pet': '1_peter', '2pet': '2_peter',
    '1joh': '1_john', '2joh': '2_john', '3joh': '3_john',
    'jud': 'jude', 'rev': 'revelation',
    
    # Alternative spellings found in data
    '1kgs': '1_kings', '2kgs': '2_kings',
    'josh': 'joshua', 'neh': 'nehemiah',
    '1john': '1_john', '2john': '2_john', '3john': '3_john',
    '1pet': '1_peter', '2pet': '2_peter',
    'titus': 'titus',
    'jonah': 'jonah',
    'jude': 'jude',
}

# Reverse: book name -> ID mapping (for app)
BOOK_NAME_TO_ID = {
    'genesis': 1, 'exodus': 2, 'leviticus': 3, 'numbers': 4, 'deuteronomy': 5,
    'joshua': 6, 'judges': 7, 'ruth': 8, '1_samuel': 9, '2_samuel': 10,
    '1_kings': 11, '2_kings': 12, '1_chronicles': 13, '2_chronicles': 14,
    'ezra': 15, 'nehemiah': 16, 'tobit': 17, 'judith': 18, 'esther': 19,
    'job': 20, 'psalms': 21, 'proverbs': 22, 'ecclesiastes': 23, 'song_of_songs': 24,
    'wisdom': 25, 'sirach': 26, 'isaiah': 27, 'jeremiah': 28, 'lamentations': 29,
    'baruch': 30, 'ezekiel': 31, 'daniel': 32, 'hosea': 33, 'joel': 34,
    'amos': 35, 'obadiah': 36, 'jonah': 37, 'micah': 38, 'nahum': 39,
    'habakkuk': 40, 'zephaniah': 41, 'haggai': 42, 'zechariah': 43, 'malachi': 44,
    '1_maccabees': 45, '2_maccabees': 46,
    'matthew': 47, 'mark': 48, 'luke': 49, 'john': 50, 'acts': 51,
    'romans': 52, '1_corinthians': 53, '2_corinthians': 54, 'galatians': 55,
    'ephesians': 56, 'philippians': 57, 'colossians': 58,
    '1_thessalonians': 59, '2_thessalonians': 60,
    '1_timothy': 61, '2_timothy': 62, 'titus': 63, 'philemon': 64,
    'hebrews': 65, 'james': 66, '1_peter': 67, '2_peter': 68,
    '1_john': 69, '2_john': 70, '3_john': 71, 'jude': 72, 'revelation': 73,
}


def parse_verse_ref(verse_str):
    """
    Parse verse reference like 'Gen.1.1' or 'Rev.21.6' or 'Ps.104.24-Ps.104.25'
    Returns: (book, chapter, verse_start, verse_end) or None
    """
    verse_str = verse_str.strip()
    
    # Handle ranges like "Ps.104.24-Ps.104.25"
    if '-' in verse_str:
        parts = verse_str.split('-')
        # If second part doesn't have book, use first part's book
        from_ref = parse_verse_ref(parts[0].strip())
        to_ref = parse_verse_ref(parts[1].strip())
        
        if from_ref and to_ref:
            # If to_ref doesn't have book, use from_ref's book
            if to_ref[0] is None:
                to_ref = (from_ref[0], to_ref[1], to_ref[2], to_ref[3])
            return (from_ref[0], from_ref[1], from_ref[2], to_ref[2])
        return None
    
    # Pattern: Book.Abbr.Chapter.Verse
    # Example: Gen.1.1 or 1Cor.1.1
    match = re.match(r'^([1-3]?\w+)\.(\d+)\.(\d+)$', verse_str)
    if not match:
        # Try without verse number
        match = re.match(r'^([1-3]?\w+)\.(\d+)$', verse_str)
        if match:
            book_abbr = match.group(1).lower()
            chapter = int(match.group(2))
            book = BOOK_MAP.get(book_abbr)
            if book:
                return (book, chapter, 0, 0)  # 0 = whole chapter
        return None
    
    book_abbr = match.group(1).lower()
    chapter = int(match.group(2))
    verse = int(match.group(3))
    
    book = BOOK_MAP.get(book_abbr)
    if not book:
        return None
    
    return (book, chapter, verse, verse)


def create_cross_ref_table(conn):
    """Create cross_references table."""
    conn.execute('''
        CREATE TABLE IF NOT EXISTS cross_references (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            from_book TEXT NOT NULL,
            from_chapter INTEGER NOT NULL,
            from_verse_start INTEGER NOT NULL,
            from_verse_end INTEGER NOT NULL,
            to_book TEXT NOT NULL,
            to_chapter INTEGER NOT NULL,
            to_verse_start INTEGER NOT NULL,
            to_verse_end INTEGER NOT NULL,
            votes INTEGER DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    
    # Indexes
    conn.execute('CREATE INDEX IF NOT EXISTS idx_from ON cross_references(from_book, from_chapter, from_verse_start)')
    conn.execute('CREATE INDEX IF NOT EXISTS idx_to ON cross_references(to_book, to_chapter, to_verse_start)')
    conn.commit()


def import_cross_references(conn):
    """Import cross-references from tab-separated file."""
    print("[Cross-Refs] Importing from cross_references.txt...")
    
    if not CROSS_REF_FILE.exists():
        print(f"  ERROR: {CROSS_REF_FILE} not found!")
        return 0
    
    count = 0
    skipped = 0
    skipped_books = defaultdict(int)
    
    with open(CROSS_REF_FILE, 'r', encoding='utf-8') as f:
        header = f.readline()  # Skip header
        
        for line in f:
            line = line.strip()
            if not line:
                continue
            
            parts = line.split('\t')
            if len(parts) < 3:
                skipped += 1
                continue
            
            from_ref_str = parts[0]
            to_ref_str = parts[1]
            votes = int(parts[2]) if parts[2].isdigit() else 0
            
            # Parse references
            from_ref = parse_verse_ref(from_ref_str)
            to_ref = parse_verse_ref(to_ref_str)
            
            if not from_ref or not to_ref:
                skipped += 1
                if from_ref is None:
                    book_abbr = from_ref_str.split('.')[0] if '.' in from_ref_str else from_ref_str
                    skipped_books[book_abbr] += 1
                continue
            
            from_book, from_chapter, from_verse_start, from_verse_end = from_ref
            to_book, to_chapter, to_verse_start, to_verse_end = to_ref
            
            # Insert
            try:
                conn.execute('''
                    INSERT INTO cross_references
                    (from_book, from_chapter, from_verse_start, from_verse_end,
                     to_book, to_chapter, to_verse_start, to_verse_end, votes)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (
                    from_book,
                    from_chapter,
                    from_verse_start,
                    from_verse_end if from_verse_end else from_verse_start,
                    to_book,
                    to_chapter,
                    to_verse_start,
                    to_verse_end if to_verse_end else to_verse_start,
                    votes
                ))
                count += 1
            except Exception as e:
                print(f"  Error inserting: {e}")
                skipped += 1
    
    conn.commit()
    print(f"[Cross-Refs] Imported {count} references, skipped {skipped}")
    
    if skipped_books:
        print(f"\n  Skipped books (not in mapping):")
        for book, cnt in sorted(skipped_books.items(), key=lambda x: -x[1])[:10]:
            print(f"    {book}: {cnt} entries")
    
    return count


def print_stats(conn):
    """Print cross-reference statistics."""
    total = conn.execute('SELECT COUNT(*) FROM cross_references').fetchone()[0]
    
    print(f"\n=== Cross-Reference Stats ===")
    print(f"Total references: {total}")
    
    if total > 0:
        # Top source books
        print(f"\nTop 10 source books:")
        for row in conn.execute('''
            SELECT from_book, COUNT(*) as cnt
            FROM cross_references
            GROUP BY from_book
            ORDER BY cnt DESC
            LIMIT 10
        '''):
            print(f"  {row[0]}: {row[1]} references")
        
        # Top target books
        print(f"\nTop 10 target books:")
        for row in conn.execute('''
            SELECT to_book, COUNT(*) as cnt
            FROM cross_references
            GROUP BY to_book
            ORDER BY cnt DESC
            LIMIT 10
        '''):
            print(f"  {row[0]}: {row[1]} references")
        
        # Sample references
        print(f"\nSample references (top voted):")
        for row in conn.execute('''
            SELECT from_book, from_chapter, from_verse_start, 
                   to_book, to_chapter, to_verse_start, votes
            FROM cross_references
            ORDER BY votes DESC
            LIMIT 5
        '''):
            print(f"  {row[0]}.{row[1]}.{row[2]} -> {row[3]}.{row[4]}.{row[5]} (votes: {row[6]})")


def main():
    print("=== Verbum Universalis Cross-Reference Engine ===\n")
    
    if not CROSS_REF_FILE.exists():
        print(f"ERROR: Cross-reference file not found at {CROSS_REF_FILE}")
        print("Please download it first from: https://a.openbible.info/data/cross-references.zip")
        return
    
    conn = sqlite3.connect(str(OUTPUT_DB))
    
    # Create table
    create_cross_ref_table(conn)
    print("[Init] Cross-references table created\n")
    
    # Import data
    count = import_cross_references(conn)
    
    # Print stats
    print_stats(conn)
    
    conn.close()
    
    # File size
    size_mb = OUTPUT_DB.stat().st_size / 1024 / 1024
    print(f"\n=== Done ===")
    print(f"Database: {OUTPUT_DB}")
    print(f"Size: {size_mb:.1f} MB")
    print(f"\nDatabase ready with {count} cross-references!")


if __name__ == '__main__':
    main()
