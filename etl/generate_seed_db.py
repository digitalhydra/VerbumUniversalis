#!/usr/bin/env python3
"""
ETL Script: Generate verbum_seed.db from SWORD modules.
"""

import os
import sqlite3
from pysword.bible import SwordBible, SwordModuleType

# Book names: SWORD code -> (English name, testament)
BOOK_NAMES = {
    'Gen': ('Genesis', 'ot'), 'Exod': ('Exodus', 'ot'), 'Lev': ('Leviticus', 'ot'),
    'Num': ('Numbers', 'ot'), 'Deut': ('Deuteronomy', 'ot'), 'Josh': ('Joshua', 'ot'),
    'Judg': ('Judges', 'ot'), 'Ruth': ('Ruth', 'ot'), '1Sam': ('1 Samuel', 'ot'),
    '2Sam': ('2 Samuel', 'ot'), '1Kgs': ('1 Kings', 'ot'), '2Kgs': ('2 Kings', 'ot'),
    '1Chr': ('1 Chronicles', 'ot'), '2Chr': ('2 Chronicles', 'ot'), 'Ezra': ('Ezra', 'ot'),
    'Neh': ('Nehemiah', 'ot'), 'Esth': ('Esther', 'ot'), 'Job': ('Job', 'ot'),
    'Ps': ('Psalms', 'ot'), 'Prov': ('Proverbs', 'ot'), 'Eccl': ('Ecclesiastes', 'ot'),
    'Song': ('Song of Solomon', 'ot'), 'Isa': ('Isaiah', 'ot'), 'Jer': ('Jeremiah', 'ot'),
    'Lam': ('Lamentations', 'ot'), 'Ezek': ('Ezekiel', 'ot'), 'Dan': ('Daniel', 'ot'),
    'Hos': ('Hosea', 'ot'), 'Joel': ('Joel', 'ot'), 'Amos': ('Amos', 'ot'),
    'Obad': ('Obadiah', 'ot'), 'Jonah': ('Jonah', 'ot'), 'Mic': ('Micah', 'ot'),
    'Nah': ('Nahum', 'ot'), 'Hab': ('Habakkuk', 'ot'), 'Zeph': ('Zephaniah', 'ot'),
    'Hag': ('Haggai', 'ot'), 'Zech': ('Zechariah', 'ot'), 'Mal': ('Malachi', 'ot'),
    'Tob': ('Tobit', 'ot'), 'Jdt': ('Judith', 'ot'), 'Wis': ('Wisdom of Solomon', 'ot'),
    'Sir': ('Sirach', 'ot'), 'Bar': ('Baruch', 'ot'), '1Macc': ('1 Maccabees', 'ot'),
    '2Macc': ('2 Maccabees', 'ot'), 'AddEsth': ('Additions to Esther', 'ot'),
    'PrAzar': ('Prayer of Azariah', 'ot'), 'Sus': ('Susanna', 'ot'),
    'Bel': ('Bel and the Dragon', 'ot'), 'PrMan': ('Prayer of Manasseh', 'ot'),
    'EpJer': ('Epistle of Jeremiah', 'ot'),
    'Matt': ('Matthew', 'nt'), 'Mark': ('Mark', 'nt'), 'Luke': ('Luke', 'nt'),
    'John': ('John', 'nt'), 'Acts': ('Acts', 'nt'), 'Rom': ('Romans', 'nt'),
    '1Cor': ('1 Corinthians', 'nt'), '2Cor': ('2 Corinthians', 'nt'), 'Gal': ('Galatians', 'nt'),
    'Eph': ('Ephesians', 'nt'), 'Phil': ('Philippians', 'nt'), 'Col': ('Colossians', 'nt'),
    '1Thess': ('1 Thessalonians', 'nt'), '2Thess': ('2 Thessalonians', 'nt'),
    '1Tim': ('1 Timothy', 'nt'), '2Tim': ('2 Timothy', 'nt'), 'Titus': ('Titus', 'nt'),
    'Phlm': ('Philemon', 'nt'), 'Heb': ('Hebrews', 'nt'), 'Jas': ('James', 'nt'),
    '1Pet': ('1 Peter', 'nt'), '2Pet': ('2 Peter', 'nt'), '1John': ('1 John', 'nt'),
    '2John': ('2 John', 'nt'), '3John': ('3 John', 'nt'), 'Jude': ('Jude', 'nt'),
    'Rev': ('Revelation', 'nt'),
}


def create_schema(conn):
    """Create the database schema."""
    conn.executescript("""
        CREATE TABLE IF NOT EXISTS books (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name_en TEXT NOT NULL,
            name_es TEXT,
            name_la TEXT,
            testament TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS verses (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            book_id INTEGER NOT NULL,
            chapter INTEGER NOT NULL,
            verse_number INTEGER NOT NULL,
            FOREIGN KEY (book_id) REFERENCES books(id),
            UNIQUE(book_id, chapter, verse_number)
        );

        CREATE TABLE IF NOT EXISTS texts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            verse_id INTEGER NOT NULL,
            lang_code TEXT NOT NULL,
            content TEXT NOT NULL,
            FOREIGN KEY (verse_id) REFERENCES verses(id),
            UNIQUE(verse_id, lang_code)
        );

        CREATE TABLE IF NOT EXISTS interlinear_words (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            verse_id INTEGER NOT NULL,
            word_order INTEGER NOT NULL,
            original TEXT,
            transliteration TEXT,
            literal TEXT,
            morphology TEXT,
            lemma TEXT,
            FOREIGN KEY (verse_id) REFERENCES verses(id),
            UNIQUE(verse_id, word_order)
        );

        CREATE TABLE IF NOT EXISTS lexicon (
            lemma TEXT PRIMARY KEY,
            language TEXT NOT NULL,
            definition TEXT
        );

        CREATE INDEX IF NOT EXISTS idx_verses_book ON verses(book_id, chapter);
        CREATE INDEX IF NOT EXISTS idx_texts_verse ON texts(verse_id);
        CREATE INDEX IF NOT EXISTS idx_words_verse ON interlinear_words(verse_id);
        CREATE INDEX IF NOT EXISTS idx_lexicon_lemma ON lexicon(lemma);
    """)
    conn.commit()


def load_sword_module(name, path, versification, source_type='OSIS', compress_type='ZIP'):
    """Load a SWORD module using pysword."""
    print(f"  Loading {name} from {path}...")
    try:
        module = SwordBible(
            module_path=os.path.abspath(path),
            module_type=SwordModuleType.ZTEXT,
            versification=versification,
            encoding='utf-8',
            source_type=source_type,
            compress_type=compress_type
        )
        print(f"  Loaded {name}")
        return module
    except Exception as e:
        print(f"  Error loading {name}: {e}")
        return None


def get_verse_text(module, book_code, chapter, verse):
    """Get cleaned verse text from a SWORD module."""
    try:
        result = module.get(books=[book_code], chapters=[chapter], verses=[verse], clean=True)
        texts = list(result)
        return texts[0] if texts else ''
    except Exception:
        return ''


def main():
    """Main ETL function."""
    cwd = os.getcwd()
    print(f"Working directory: {cwd}\n")

    base = os.path.join(cwd, 'raw_data/_extracted/modules/texts/ztext')
    output_db = os.path.join(cwd, 'app/src/main/assets/verbum_seed.db')

    print(f"Base path: {base}")
    print(f"Output DB: {output_db}\n")

    # Ensure output directory exists
    os.makedirs(os.path.dirname(output_db), exist_ok=True)

    # Remove existing DB
    if os.path.exists(output_db):
        os.remove(output_db)

    # Create schema
    conn = sqlite3.connect(output_db)
    create_schema(conn)

    # Load SWORD modules
    print("Loading SWORD modules...")
    modules = {}

    modules['en'] = load_sword_module('DRC (English)', os.path.join(base, 'drc'), 'catholic')
    modules['la'] = load_sword_module('VulgClementine (Latin)', os.path.join(base, 'vulgclementine'), 'catholic')
    modules['gr'] = load_sword_module('ABPGRK (Greek)', os.path.join(base, 'abpgrk'), 'kjv')
    modules['he'] = load_sword_module('OSHB (Hebrew)', os.path.join(base, 'oshb'), 'leningrad')

    # Check if we have at least the English module
    if not modules.get('en'):
        print("\nERROR: English module (DRC) not loaded. Cannot proceed.")
        conn.close()
        return

    print("\nProcessing books and verses...\n")

    # Process each book
    for book_code, (book_name, testament) in BOOK_NAMES.items():
        print(f"Processing {book_name} ({book_code})...")

        # Insert book
        conn.execute(
            'INSERT INTO books (name_en, name_es, name_la, testament) VALUES (?, NULL, NULL, ?)',
            (book_name, testament)
        )
        book_id = conn.execute('SELECT last_insert_rowid()').fetchone()[0]

        # Process chapters (try up to 150)
        for chapter in range(1, 151):
            verses_found = False

            # Try verses (up to 200 per chapter)
            for verse in range(1, 201):
                try:
                    # Get English text as reference
                    en_text = get_verse_text(modules['en'], book_code, chapter, verse)
                    if not en_text:
                        if verse == 1:
                            break  # No more chapters
                        continue

                    verses_found = True

                    # Insert verse
                    conn.execute(
                        'INSERT OR IGNORE INTO verses (book_id, chapter, verse_number) VALUES (?, ?, ?)',
                        (book_id, chapter, verse)
                    )
                    verse_row = conn.execute(
                        'SELECT id FROM verses WHERE book_id=? AND chapter=? AND verse_number=?',
                        (book_id, chapter, verse)
                    ).fetchone()

                    if not verse_row:
                        continue

                    verse_id = verse_row[0]

                    # Insert texts for each available language
                    for lang_code, module in modules.items():
                        if not module:
                            continue
                        text = get_verse_text(module, book_code, chapter, verse)
                        if text:
                            conn.execute(
                                'INSERT OR IGNORE INTO texts (verse_id, lang_code, content) VALUES (?, ?, ?)',
                                (verse_id, lang_code, text)
                            )

                except Exception:
                    if verse == 1:
                        break
                    continue

            if not verses_found:
                break

        # Commit after each book
        conn.commit()
        print(f"  Completed {book_name}")

    # Print stats
    print("\n" + "=" * 50)
    print("Database created successfully!")
    print(f"Output: {output_db}\n")

    books_count = conn.execute('SELECT COUNT(*) FROM books').fetchone()[0]
    verses_count = conn.execute('SELECT COUNT(*) FROM verses').fetchone()[0]
    texts_count = conn.execute('SELECT COUNT(*) FROM texts').fetchone()[0]

    print("Stats:")
    print(f"  Books: {books_count}")
    print(f"  Verses: {verses_count}")
    print(f"  Texts: {texts_count}")

    conn.close()


if __name__ == '__main__':
    main()
