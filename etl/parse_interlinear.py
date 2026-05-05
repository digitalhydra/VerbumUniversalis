#!/usr/bin/env python3
"""Parse OSIS w tags into interlinear_words table. Fixed versification lookup."""

import os
import re
import sqlite3
from pysword.bible import SwordBible, SwordModuleType

BOOKS = [
    ('Gen', 'ot'), ('Exod', 'ot'), ('Lev', 'ot'), ('Num', 'ot'), ('Deut', 'ot'),
    ('Josh', 'ot'), ('Judg', 'ot'), ('Ruth', 'ot'), ('1Sam', 'ot'), ('2Sam', 'ot'),
    ('1Kgs', 'ot'), ('2Kgs', 'ot'), ('1Chr', 'ot'), ('2Chr', 'ot'), ('Ezra', 'ot'),
    ('Neh', 'ot'), ('Esth', 'ot'), ('Job', 'ot'), ('Ps', 'ot'), ('Prov', 'ot'),
    ('Eccl', 'ot'), ('Song', 'ot'), ('Isa', 'ot'), ('Jer', 'ot'), ('Lam', 'ot'),
    ('Ezek', 'ot'), ('Dan', 'ot'), ('Hos', 'ot'), ('Joel', 'ot'), ('Amos', 'ot'),
    ('Obad', 'ot'), ('Jonah', 'ot'), ('Mic', 'ot'), ('Nah', 'ot'), ('Hab', 'ot'),
    ('Zeph', 'ot'), ('Hag', 'ot'), ('Zech', 'ot'), ('Mal', 'ot'),
    ('Matt', 'nt'), ('Mark', 'nt'), ('Luke', 'nt'), ('John', 'nt'), ('Acts', 'nt'),
    ('Rom', 'nt'), ('1Cor', 'nt'), ('2Cor', 'nt'), ('Gal', 'nt'), ('Eph', 'nt'),
    ('Phil', 'nt'), ('Col', 'nt'), ('1Thess', 'nt'), ('2Thess', 'nt'),
    ('1Tim', 'nt'), ('2Tim', 'nt'), ('Titus', 'nt'), ('Phlm', 'nt'),
    ('Heb', 'nt'), ('Jas', 'nt'), ('1Pet', 'nt'), ('2Pet', 'nt'),
    ('1John', 'nt'), ('2John', 'nt'), ('3John', 'nt'), ('Jude', 'nt'), ('Rev', 'nt'),
]


def parse_w_tags(osis_text):
    """Parse <w ...>...</w> tags. Returns list of dicts."""
    words = []
    pattern = r'<w\b([^>]*)>(.*?)</w>'
    for match in re.finditer(pattern, osis_text, re.DOTALL):
        attrs_str = match.group(1)
        content = match.group(2).strip()

        lemma = None
        lm = re.search(r'lemma="([^"]*)"', attrs_str)
        if lm:
            lemma = lm.group(1)

        morph = None
        mm = re.search(r'morph="([^"]*)"', attrs_str)
        if mm:
            morph = mm.group(1)

        if content:
            words.append({
                'original': content,
                'lemma': lemma,
                'morphology': morph,
            })
    return words


def build_verse_lookup(conn, module, testament):
    """
    Build verse lookup from actual module output.
    Returns dict: (book_code, chapter, verse) -> verse_id
    """
    lookup = {}

    for book_code, test in BOOKS:
        if test != testament:
            continue

        # Get book_id from DB
        row = conn.execute('SELECT id FROM books WHERE name_en=?', (book_code,)).fetchone()
        if not row:
            continue
        book_id = row[0]

        # Get verses for this book from DB
        verses = conn.execute(
            'SELECT id, chapter, verse_number FROM verses WHERE book_id=?',
            (book_id,)
        ).fetchall()

        # Build lookup
        for verse_id, chapter, verse_num in verses:
            lookup[(book_code, chapter, verse_num)] = verse_id

    return lookup


def main():
    cwd = os.getcwd()
    print(f"Working directory: {cwd}\n")

    db_path = os.path.join(cwd, 'app/src/main/assets/verbum_seed.db')
    print(f"Database: {db_path}\n")

    if not os.path.exists(db_path):
        print("ERROR: Database not found.")
        return

    conn = sqlite3.connect(db_path)

    # Load modules
    base = os.path.join(cwd, 'raw_data/_extracted/modules/texts/ztext')

    print("Loading modules...")
    greek_mod = None
    hebrew_mod = None

    try:
        greek_mod = SwordBible(
            module_path=os.path.abspath(os.path.join(base, 'abpgrk')),
            module_type=SwordModuleType.ZTEXT,
            versification='kjv',
            encoding='utf-8',
            source_type='OSIS',
            compress_type='ZIP'
        )
        print("  Loaded ABPGRK (Greek)")
    except Exception as e:
        print(f"  Error loading Greek: {e}")

    try:
        hebrew_mod = SwordBible(
            module_path=os.path.abspath(os.path.join(base, 'oshb')),
            module_type=SwordModuleType.ZTEXT,
            versification='leningrad',
            encoding='utf-8',
            source_type='OSIS',
            compress_type='ZIP'
        )
        print("  Loaded OSHB (Hebrew)")
    except Exception as e:
        print(f"  Error loading Hebrew: {e}")

    # Build verse lookup from DB (for mapping module output to verse_id)
    # For Greek (NT), build lookup from NT books
    # For Hebrew (OT), build lookup from OT books
    # The lookup is: (book_code, chapter, verse) -> verse_id

    print("\nBuilding verse lookup from DB...")
    db_lookup = {}
    for book_code, testament in BOOKS:
        row = conn.execute('SELECT id FROM books WHERE name_en=?', (book_code,)).fetchone()
        if not row:
            continue
        book_id = row[0]

        verses = conn.execute(
            'SELECT id, chapter, verse_number FROM verses WHERE book_id=?',
            (book_id,)
        ).fetchall()

        for verse_id, chapter, verse_num in verses:
            db_lookup[(book_code, chapter, verse_num)] = verse_id

    print(f"  Loaded {len(db_lookup)} verses from DB\n")

    # Process Greek (NT)
    if greek_mod:
        print("Processing Greek interlinear (NT)...")
        count = 0
        for book_code, testament in BOOKS:
            if testament != 'nt':
                continue

            for chapter in range(1, 151):
                verses_found = False
                for verse in range(1, 201):
                    try:
                        result = greek_mod.get_iter(
                            books=[book_code], chapters=[chapter], verses=[verse], clean=False
                        )
                        raw_list = list(result)
                        if not raw_list:
                            if verse == 1:
                                break
                            continue

                        verses_found = True
                        raw = raw_list[0]

                        # Parse w tags
                        words = parse_w_tags(raw)

                        if not words:
                            continue

                        # Lookup verse_id
                        key = (book_code, chapter, verse)
                        if key not in db_lookup:
                            continue

                        verse_id = db_lookup[key]

                        # Insert interlinear words
                        for idx, wd in enumerate(words, 1):
                            conn.execute(
                                """INSERT OR IGNORE INTO interlinear_words
                                   (verse_id, word_order, original, lemma, morphology)
                                   VALUES (?, ?, ?, ?, ?)""",
                                (verse_id, idx, wd['original'], wd['lemma'], wd['morphology'])
                            )
                            count += 1

                    except Exception:
                        if verse == 1:
                            break
                        continue

                if not verses_found:
                    break

        print(f"  Completed Greek: {count} words")
        conn.commit()

    # Process Hebrew (OT)
    if hebrew_mod:
        print("\nProcessing Hebrew interlinear (OT)...")
        count = 0
        for book_code, testament in BOOKS:
            if testament != 'ot':
                continue

            for chapter in range(1, 151):
                verses_found = False
                for verse in range(1, 201):
                    try:
                        result = hebrew_mod.get_iter(
                            books=[book_code], chapters=[chapter], verses=[verse], clean=False
                        )
                        raw_list = list(result)
                        if not raw_list:
                            if verse == 1:
                                break
                            continue

                        verses_found = True
                        raw = raw_list[0]

                        # Parse w tags
                        words = parse_w_tags(raw)

                        if not words:
                            continue

                        # Lookup verse_id
                        key = (book_code, chapter, verse)
                        if key not in db_lookup:
                            continue

                        verse_id = db_lookup[key]

                        # Insert interlinear words
                        for idx, wd in enumerate(words, 1):
                            conn.execute(
                                """INSERT OR IGNORE INTO interlinear_words
                                   (verse_id, word_order, original, lemma, morphology)
                                   VALUES (?, ?, ?, ?, ?)""",
                                (verse_id, idx, wd['original'], wd['lemma'], wd['morphology'])
                            )
                            count += 1

                    except Exception:
                        if verse == 1:
                            break
                        continue

                if not verses_found:
                    break

        print(f"  Completed Hebrew: {count} words")
        conn.commit()

    # Stats
    print("\n" + "=" * 50)
    print("Interlinear processing complete!\n")
    total = conn.execute("SELECT COUNT(*) FROM interlinear_words").fetchone()[0]
    greek_count = conn.execute("SELECT COUNT(*) FROM interlinear_words WHERE lemma LIKE 'strong:G%'").fetchone()[0]
    hebrew_count = conn.execute("SELECT COUNT(*) FROM interlinear_words WHERE lemma LIKE 'strong:H%'").fetchone()[0]

    print("Stats:")
    print(f"  Total interlinear words: {total}")
    print(f"  Greek words: {greek_count}")
    print(f"  Hebrew words: {hebrew_count}")

    conn.close()


if __name__ == '__main__':
    main()
