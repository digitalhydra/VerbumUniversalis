#!/usr/bin/env python3
"""
Verbum Universalis - Final Catena Database Builder
==============================================

Unified script to build the catena database from:
1. Commentaries-Database (dataset1) - SQLite
2. bible-commentaries-dataset (dataset2) - JSON per verse

Features:
- Removes Protestant/non-Catholic authors
- Normalizes author names
- Deduplicates entries
- Normalizes book names
- Outputs final verbum_catena.db

Future updates: Just re-run this script with new data.
"""

import sqlite3
import json
import os
import re
import hashlib
from pathlib import Path
from collections import defaultdict

# === PATHS ===
SCRIPT_DIR = Path(__file__).parent
ROOT_DIR = SCRIPT_DIR.parent
OUTPUT_DB = SCRIPT_DIR / "verbum_catena.db"

# Dataset 1: Commentaries-Database SQLite
DATASET1_SQLITE = ROOT_DIR / "catena.sqlite"

# Dataset 2: bible-commentaries-dataset
DATASET2_BASE = ROOT_DIR / "raw_data/catena/bible-commentaries-dataset/data/01_cleaned/catena_bible"

# === PROTESTANT/NON-CATHOLIC AUTHORS TO REMOVE ===
PROTESTANT_AUTHORS = {
    "John Calvin",
    "Martin Luther", 
    "Ulrich Zwingli",
    "Ulich Zwingli",
    "Huldrych Zwingli",
    "John Wesley",
    "Douglas Wilson",
    "John Knox",
    "William Tyndale",
    "Philip Melanchthon",
    "Menno Simons",
    "Roger Williams",
    "Jonathan Edwards",
    "Charles Spurgeon",
    "Billy Graham",
    "Rick Warren",
    "Joel Osteen",
    "JB Lightfoot",  # Anglican/Protestant scholar
    "William Hendriksen",
    "John Gill",
    "Matthew Henry",
    "Adam Clarke",
}

# Authors to KEEP (user explicitly said so)
KEEP_AUTHORS = {
    "CS Lewis",
    "C.S. Lewis",
    "JRR Tolkien",
    "J.R.R. Tolkien",
    "GK Chesterton",
    "G.K. Chesterton",
}

# === AUTHOR NAME NORMALIZATION ===
AUTHOR_NORMALIZATION = {
    # Spelling fixes
    "Augustine of Hippo": "Augustine of Hippo",
    "Athanasius the Apostolic": "Athanasius of Alexandria",
    "Basil of Caesarea": "Basil the Great",
    "Basil the Great": "Basil the Great",
    "John Chrysostom": "John Chrysostom",
    "Chrysostom": "John Chrysostom",
    "Gregory the Dialogist": "Gregory the Great",
    "Gregory the Great": "Gregory the Great",
    "Gregory of Nazianzus": "Gregory of Nazianzus",
    "Gregory the Theologian": "Gregory of Nazianzus",
    "Leo of Rome": "Pope Leo I (the Great)",
    "Leo the Great": "Pope Leo I (the Great)",
    "Jerome": "Jerome",
    "Eusebius of Caesarea": "Eusebius of Caesarea",
    "Cyril of Alexandria": "Cyril of Alexandria",
    "Cyril of Jerusalem": "Cyril of Jerusalem",
    "Theophylact of Ochrid": "Theophylact of Ochrid",
    "Theophylact of Ohrid": "Theophylact of Ochrid",
    "Bede": "The Venerable Bede",
    "Venerable Bede": "The Venerable Bede",
    "Anselm of Canterbury": "Anselm of Canterbury",
    "Thomas Aquinas": "Thomas Aquinas",
    "Albert the Great": "Albertus Magnus",
    
    # Handle "quoted by" patterns
    "Augustine of Hippo (as quoted by Aquinas, AD 1274)": "Augustine of Hippo",
    "Bede (as quoted by Aquinas, AD 1274)": "The Venerable Bede",
    "Athanasius of Alexandria (as quoted by Aquinas, AD 1274)": "Athanasius of Alexandria",
    "Basil of Caesarea (as quoted by Aquinas, AD 1274)": "Basil the Great",
    "John Chrysostom (as quoted by Aquinas, AD 1274)": "John Chrysostom",
    "Gregory the Dialogist (as quoted by Aquinas, AD 1274)": "Gregory the Great",
    "Cyril of Alexandria (as quoted by Aquinas, AD 1274)": "Cyril of Alexandria",
    "Origen of Alexandria (as quoted by Aquinas, AD 1274)": "Origen of Alexandria",
    "Ambrose of Milan (as quoted by Aquinas, AD 1274)": "Ambrose of Milan",
    "Eusebius of Caesarea (as quoted by Aquinas, AD 1274)": "Eusebius of Caesarea",
    "Epiphanius of Salamis (as quoted by Aquinas, AD 1274)": "Epiphanius of Salamis",
    "Theophylact of Ochrid (as quoted by Aquinas, AD 1274)": "Theophylact of Ochrid",
    "Hesychius of Jerusalem (as quoted by Aquinas, AD 1274)": "Hesychius of Jerusalem",
    "Severus of Antioch (as quoted by Aquinas, AD 1274)": "Severus of Antioch",
    "Tertullian (as quoted by Aquinas, AD 1274)": "Tertullian",
    "Cyprian (as quoted by Aquinas, AD 1274)": "Cyprian",
    
    # Pseudo authors
    "Pseudo-Augustine": "Pseudo-Augustine",
    "Pseudo-Jerome": "Pseudo-Jerome",
    "Pseudo-Chrysostom": "Pseudo-Chrysostom",
    "Pseudo-Dionysius the Areopagite": "Pseudo-Dionysius the Areopagite",
    "Pseudo-Athanasius": "Pseudo-Athanasius",
    "Pseudo-Basil": "Pseudo-Basil",
    "Pseudo-Clement": "Pseudo-Clement",
    "Pseudo-Cyprian": "Pseudo-Cyprian",
    "Pseudo-Ephrem": "Pseudo-Ephrem",
    "Pseudo-Hegesippus": "Pseudo-Hegesippus",
    "Pseudo-Hippolytus": "Pseudo-Hippolytus",
    "Pseudo-Ignatius": "Pseudo-Ignatius",
    "Pseudo-Justin": "Pseudo-Justin",
    "Pseudo-Macarius": "Pseudo-Macarius",
    "Pseudo-Origen": "Pseudo-Origen",
    "Pseudo-Tertullian": "Pseudo-Tertullian",
    
    # Keep these
    "CS Lewis": "C.S. Lewis",
    "C.S. Lewis": "C.S. Lewis",
    "GK Chesterton": "G.K. Chesterton",
    "G.K. Chesterton": "G.K. Chesterton",
    "JRR Tolkien": "J.R.R. Tolkien",
    "J.R.R. Tolkien": "J.R.R. Tolkien",
    
    # Book names (not authors - skip these)
    "1 Corinthians": None,
    "2 Corinthians": None,
    "1 Peter": None,
    "2 Peter": None,
    "1 Timothy": None,
    "Acts": None,
    "Ephesians": None,
    "Galatians": None,
    "Hebrews": None,
    "James": None,
    "John": None,
    "Jude": None,
    "Luke": None,
    "Mark": None,
    "Matthew": None,
    "Philippians": None,
    "Revelation": None,
    "Romans": None,
    "1 John": None,
    "2 John": None,
    "3 John": None,
}

# === BOOK NAME NORMALIZATION ===
BOOK_NORMALIZATION = {
    # Dataset 1 uses full lowercase
    '1corinthians': '1_corinthians',
    '2corinthians': '2_corinthians',
    '1cor': '1_corinthians',
    '2cor': '2_corinthians',
    '1john': '1_john',
    '2john': '2_john',
    '3john': '3_john',
    '1peter': '1_peter',
    '2peter': '2_peter',
    '1timothy': '1_timothy',
    '2timothy': '2_timothy',
    '1samuel': '1_samuel',
    '2samuel': '2_samuel',
    '1kings': '1_kings',
    '2kings': '2_kings',
    '1chronicles': '1_chronicles',
    '2chronicles': '2_chronicles',
    '1maccabees': '1_maccabees',
    '2maccabees': '2_maccabees',
    'psalms': 'psalms',
    'proverbs': 'proverbs',
    'ecclesiastes': 'ecclesiastes',
}

# Dataset 2 book mapping (abbreviations -> full names)
DATASET2_BOOK_MAP = {
    'gn': 'genesis', 'ex': 'exodus', 'lv': 'leviticus', 'nm': 'numbers', 'dt': 'deuteronomy',
    'jos': 'joshua', 'jdg': 'judges', 'ru': 'ruth', '1sa': '1_samuel', '2sa': '2_samuel',
    '1ki': '1_kings', '2ki': '2_kings', '1ch': '1_chronicles', '2ch': '2_chronicles',
    'ezr': 'ezra', 'ne': 'nehemiah', 'es': 'esther', 'job': 'job', 'ps': 'psalms',
    'pr': 'proverbs', 'ec': 'ecclesiastes', 'song': 'song_of_songs', 'wis': 'wisdom',
    'sir': 'sirach', 'isa': 'isaiah', 'jer': 'jeremiah', 'lam': 'lamentations',
    'bar': 'baruch', 'ezk': 'ezekiel', 'dan': 'daniel', 'hos': 'hosea',
    'jol': 'joel', 'am': 'amos', 'ob': 'obadiah', 'jon': 'jonah', 'mic': 'micah',
    'nam': 'nahum', 'hab': 'habakkuk', 'zep': 'zephaniah', 'hag': 'haggai',
    'zec': 'zechariah', 'mal': 'malachi', 'mt': 'matthew', 'mk': 'mark',
    'lk': 'luke', 'jn': 'john', 'act': 'acts', 'rom': 'romans',
    '1co': '1_corinthians', '2co': '2_corinthians', 'gal': 'galatians',
    'eph': 'ephesians', 'php': 'philippians', 'col': 'colossians',
    '1th': '1_thessalonians', '2th': '2_thessalonians', '1ti': '1_timothy',
    '2ti': '2_timothy', 'tit': 'titus', 'phm': 'philemon', 'heb': 'hebrews',
    'jas': 'james', '1pe': '1_peter', '2pe': '2_peter', '1jn': '1_john',
    '2jn': '2_john', '3jn': '3_john', 'jud': 'jude', 'rev': 'revelation'
}


def is_protestant(author_name):
    """Check if author is Protestant/non-Catholic."""
    if not author_name:
        return False
    
    # Remove append_to_author_name suffixes
    clean = re.sub(r'\s*\(.*?\)\s*', '', author_name).strip()
    
    # Check if in protestant list
    for prot in PROTESTANT_AUTHORS:
        if prot.lower() in clean.lower():
            return True
    return False


def normalize_author(raw_author):
    """Normalize author name."""
    if not raw_author:
        return "Unknown"
    
    # Strip "append_to_author_name" patterns
    clean = re.sub(r'\s*\(as quoted by.*?\)\s*', '', raw_author).strip()
    clean = re.sub(r'\s*-\s*\w+\s*$', '', clean).strip()
    
    # Check mapping
    if clean in AUTHOR_NORMALIZATION:
        result = AUTHOR_NORMALIZATION[clean]
        return result if result else None  # None = skip
    
    # Try partial matches
    for key, value in AUTHOR_NORMALIZATION.items():
        if key.lower() in clean.lower():
            return value if value else None
    
    return clean


def normalize_book(book, dataset="1"):
    """Normalize book names."""
    if not book:
        return book
    
    if dataset == "2":
        # Dataset 2 uses abbreviations
        book = book.lower().strip()
        return DATASET2_BOOK_MAP.get(book, book)
    else:
        # Dataset 1 uses full lowercase
        book = book.lower().strip()
        return BOOK_NORMALIZATION.get(book, book)


def hash_content(text):
    """Generate MD5 hash for deduplication."""
    return hashlib.md5(text.encode('utf-8')).hexdigest()[:16]


def create_schema(conn):
    """Create the database schema."""
    conn.execute('''
        CREATE TABLE IF NOT EXISTS commentaries (
            id TEXT PRIMARY KEY,
            book TEXT NOT NULL,
            chapter INTEGER NOT NULL,
            verse_start INTEGER NOT NULL,
            verse_end INTEGER NOT NULL,
            author TEXT NOT NULL,
            author_normalized TEXT NOT NULL,
            period TEXT,
            source_title TEXT,
            source_url TEXT,
            content TEXT NOT NULL,
            content_hash TEXT,
            dataset_source TEXT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    
    # Indexes
    conn.execute('CREATE INDEX IF NOT EXISTS idx_book ON commentaries(book)')
    conn.execute('CREATE INDEX IF NOT EXISTS idx_verse ON commentaries(verse_start, verse_end)')
    conn.execute('CREATE INDEX IF NOT EXISTS idx_author ON commentaries(author_normalized)')
    conn.execute('CREATE INDEX IF NOT EXISTS idx_content_hash ON commentaries(content_hash)')
    conn.commit()


def import_dataset1(conn):
    """Import from Commentaries-Database SQLite."""
    print("[Dataset1] Importing from Commentaries-Database...")
    
    if not DATASET1_SQLITE.exists():
        print(f"  ERROR: {DATASET1_SQLITE} not found!")
        return 0, 0
    
    source_conn = sqlite3.connect(str(DATASET1_SQLITE))
    source_conn.row_factory = sqlite3.Row
    
    cursor = source_conn.execute('''
        SELECT id, father_name, book, location_start, location_end, 
               append_to_author_name, ts, source_title, source_url, txt
        FROM commentary
    ''')
    
    count = 0
    skipped = 0
    seen_hashes = defaultdict(set)
    
    for row in cursor:
        author = row['father_name']
        
        # Skip Protestant
        if is_protestant(author):
            skipped += 1
            continue
        
        # Normalize author
        normalized_author = normalize_author(author)
        if not normalized_author:
            skipped += 1
            continue
        
        # Decode location
        loc_start = row['location_start']
        loc_end = row['location_end']
        
        chapter_start = loc_start // 1000000
        verse_start = loc_start % 1000000
        
        chapter_end = loc_end // 1000000
        verse_end = loc_end % 1000000
        
        # Normalize book
        book = normalize_book(row['book'], dataset="1")
        
        # Handle append_to_author_name
        full_author = author
        if row['append_to_author_name']:
            full_author = f"{author} {row['append_to_author_name']}"
        
        content = row['txt'].strip()
        if not content:
            skipped += 1
            continue
        
        content_hash = hash_content(content)
        
        # Check for duplicates
        dedup_key = (book, verse_start, verse_end)
        if content_hash in seen_hashes[dedup_key]:
            skipped += 1
            continue
        seen_hashes[dedup_key].add(content_hash)
        
        # Insert
        try:
            conn.execute('''
                INSERT INTO commentaries 
                (id, book, chapter, verse_start, verse_end, author, author_normalized, 
                 period, source_title, source_url, content, content_hash, dataset_source)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                row['id'],
                book,
                chapter_start,
                verse_start,
                verse_end,
                full_author,
                normalized_author,
                str(row['ts']) if row['ts'] else None,
                row['source_title'],
                row['source_url'],
                content,
                content_hash,
                'commentaries-db'
            ))
            count += 1
        except sqlite3.IntegrityError:
            pass  # Skip duplicates
    
    source_conn.close()
    conn.commit()
    print(f"[Dataset1] Imported {count} entries, skipped {skipped} Protestant/duplicate entries")
    return count, skipped


def import_dataset2(conn):
    """Import from bible-commentaries-dataset (JSON per verse)."""
    print("[Dataset2] Importing from bible-commentaries-dataset...")
    
    if not DATASET2_BASE.exists():
        print(f"  ERROR: {DATASET2_BASE} not found!")
        return 0, 0
    
    import uuid
    
    count = 0
    skipped = 0
    seen_hashes = defaultdict(set)
    
    base = Path(DATASET2_BASE)
    
    for json_file in base.rglob('verses/*.json'):
        try:
            with open(json_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            book_abbr = data.get('book', '')
            book = normalize_book(book_abbr, dataset="2")
            chapter = data.get('chapter', 0)
            verse = data.get('verse', 0)
            
            for comm in data.get('commentaries', []):
                author = comm.get('author', 'Unknown')
                
                # Skip Protestant
                if is_protestant(author):
                    skipped += 1
                    continue
                
                # Normalize author
                normalized_author = normalize_author(author)
                if not normalized_author:
                    skipped += 1
                    continue
                
                content = comm.get('content', '').strip()
                period = comm.get('period', '')
                
                if not content:
                    skipped += 1
                    continue
                
                content_hash = hash_content(content)
                
                # Check for duplicates
                dedup_key = (book, verse, verse)
                if content_hash in seen_hashes[dedup_key]:
                    skipped += 1
                    continue
                seen_hashes[dedup_key].add(content_hash)
                
                conn.execute('''
                    INSERT INTO commentaries
                    (id, book, chapter, verse_start, verse_end, author, author_normalized,
                     period, source_title, source_url, content, content_hash, dataset_source)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (
                    str(uuid.uuid4()),
                    book,
                    chapter,
                    verse,
                    verse,
                    author,
                    normalized_author,
                    period,
                    None,
                    None,
                    content,
                    content_hash,
                    'bible-commentaries-dataset'
                ))
                count += 1
        except Exception as e:
            print(f"  Error processing {json_file}: {e}")
    
    conn.commit()
    print(f"[Dataset2] Imported {count} entries, skipped {skipped} Protestant/duplicate entries")
    return count, skipped


def print_stats(conn):
    """Print database statistics."""
    total = conn.execute('SELECT COUNT(*) FROM commentaries').fetchone()[0]
    
    print(f"\n=== Final Database Stats ===")
    print(f"Total commentaries: {total}")
    
    # Top authors
    print(f"\nTop 20 Authors:")
    for row in conn.execute('''
        SELECT author_normalized, COUNT(*) as cnt, COUNT(DISTINCT book) as books
        FROM commentaries
        GROUP BY author_normalized
        ORDER BY cnt DESC
        LIMIT 20
    '''):
        print(f"  {row[0]}: {row[1]} entries ({row[2]} books)")
    
    # Books covered
    books = conn.execute('SELECT COUNT(DISTINCT book) FROM commentaries').fetchone()[0]
    print(f"\nBooks covered: {books}")
    
    # Dataset breakdown
    print(f"\nDataset breakdown:")
    for row in conn.execute('''
        SELECT dataset_source, COUNT(*) as cnt
        FROM commentaries
        GROUP BY dataset_source
    '''):
        print(f"  {row[0]}: {row[1]} entries")
    
    db_size = OUTPUT_DB.stat().st_size / 1024 / 1024
    print(f"\nDatabase size: {db_size:.1f} MB")
    print(f"Location: {OUTPUT_DB}")


def main():
    print("=== Verbum Universalis Catena DB Builder ===")
    print(f"Output: {OUTPUT_DB}\n")
    
    # Remove existing DB
    if OUTPUT_DB.exists():
        OUTPUT_DB.unlink()
        print("[Init] Removed existing database")
    
    # Create new DB
    conn = sqlite3.connect(str(OUTPUT_DB))
    create_schema(conn)
    print("[Init] Schema created\n")
    
    # Import datasets
    d1_count, d1_skipped = import_dataset1(conn)
    d2_count, d2_skipped = import_dataset2(conn)
    
    # Print stats
    print_stats(conn)
    
    conn.close()
    print(f"\n=== Done ===")
    print(f"Database ready for upload to GitHub release!")


if __name__ == '__main__':
    main()
