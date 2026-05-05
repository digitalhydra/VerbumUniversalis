#!/usr/bin/env python3
"""Debug Hebrew interlinear insertion."""

import os
import re
import sqlite3
from pysword.bible import SwordBible, SwordModuleType

cwd = os.getcwd()
db_path = os.path.join(cwd, 'app/src/main/assets/verbum_seed.db')
conn = sqlite3.connect(db_path)

# Build verse_lookup for Genesis
print("Building verse lookup for Genesis...")
verse_lookup = {}
row = conn.execute("SELECT id FROM books WHERE name_en='Genesis'").fetchone()
if row:
    book_id = row[0]
    verses = conn.execute(
        'SELECT id, chapter, verse_number FROM verses WHERE book_id=?',
        (book_id,)
    ).fetchall()
    for verse_id, chapter, verse_num in verses:
        verse_lookup[( 'Gen', chapter, verse_num)] = verse_id
    print(f"  Found {len(verse_lookup)} verses for Genesis")

# Load Hebrew module
hebrew = SwordBible(
    module_path=os.path.abspath('raw_data/_extracted/modules/texts/ztext/oshb'),
    module_type=SwordModuleType.ZTEXT,
    versification='leningrad',
    encoding='utf-8',
    source_type='OSIS',
    compress_type='ZIP'
)

# Process Genesis 1:1
print("\nProcessing Genesis 1:1...")
result = hebrew.get_iter(books=['Gen'], chapters=[1], verses=[1], clean=False)
raw_list = list(result)
print(f"  Raw text length: {len(raw_list[0]) if raw_list else 0}")

if raw_list:
    raw = raw_list[0]
    # Parse w tags
    pattern = r'<w\b([^>]*)>(.*?)</w>'
    matches = re.findall(pattern, raw, re.DOTALL)
    print(f"  Found {len(matches)} w tags")

    verse_key = ('Gen', 1, 1)
    if verse_key in verse_lookup:
        verse_id = verse_lookup[verse_key]
        print(f"  verse_id: {verse_id}")

        # Try inserting
        for idx, (attrs, content) in enumerate(matches[:3], 1):
            lemma = None
            lm = re.search(r'lemma="([^"]*)"', attrs)
            if lm:
                lemma = lm.group(1)

            morph = None
            mm = re.search(r'morph="([^"]*)"', attrs)
            if mm:
                morph = mm.group(1)

            conn.execute(
                """INSERT OR IGNORE INTO interlinear_words
                   (verse_id, word_order, original, lemma, morphology)
                   VALUES (?, ?, ?, ?, ?)""",
                (verse_id, idx, content.strip(), lemma, morph)
            )
            print(f"  Inserted word {idx}: {content.strip()[:30]} (lemma: {lemma})")

        conn.commit()
        print("  Committed!")
    else:
        print(f"  verse_key {verse_key} NOT in verse_lookup!")

# Check if inserted
print("\nChecking interlinear_words for Genesis 1:1...")
if 'verse_id' in locals():
    count = conn.execute("SELECT COUNT(*) FROM interlinear_words WHERE verse_id=?", (verse_id,)).fetchone()[0]
    print(f"  Words for verse_id {verse_id}: {count}")

conn.close()
