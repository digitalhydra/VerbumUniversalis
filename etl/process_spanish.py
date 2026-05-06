#!/usr/bin/env python3
"""Process all Spanish Bible modules into verbum_seed.db."""

import struct
import os
import sqlite3


def decompress_lzss(compressed_data):
    """Decompress LZSS data from SWORD module."""
    if not compressed_data:
        return b''

    buffer_size = 4096
    buffer = bytearray(buffer_size)
    buf_pos = 0

    output = bytearray()
    pos = 0
    data = compressed_data

    while pos < len(data):
        flags = data[pos]
        pos += 1

        for bit in range(8):
            if pos >= len(data):
                break

            if flags & (1 << bit):
                # Literal byte
                output.append(data[pos])
                buffer[buf_pos] = data[pos]
                buf_pos = (buf_pos + 1) % buffer_size
                pos += 1
            else:
                # Reference (2 bytes)
                if pos + 1 >= len(data):
                    break

                byte1 = data[pos]
                byte2 = data[pos + 1]
                pos += 2

                # SWORD LZSS: offset = byte1 | ((byte2 & 0x0F) << 8)
                offset = byte1 | ((byte2 & 0x0F) << 8)
                length = (byte2 >> 4) + 3

                # Copy from ring buffer
                for i in range(length):
                    src_pos = (buf_pos - offset + buffer_size) % buffer_size
                    byte = buffer[src_pos]
                    output.append(byte)
                    buffer[buf_pos] = byte
                    buf_pos = (buf_pos + 1) % buffer_size

    return bytes(output)


def parse_verse_index(bzv_data):
    """Parse bzv (verse index). Returns list of (verse_num, offset)."""
    verses = []
    for i in range(0, len(bzv_data), 6):
        if i + 5 >= len(bzv_data):
            break
        verse_num = struct.unpack('<H', bzv_data[i:i+2])[0]
        offset = struct.unpack('<I', bzv_data[i+2:i+6])[0]
        verses.append((verse_num, offset))
    return verses


def parse_book_index(bzs_data):
    """Parse bzs (book index). Returns list of (book_num, verse_index_offset)."""
    books = []
    for i in range(0, len(bzs_data), 4):
        if i + 3 >= len(bzs_data):
            break
        book_num = struct.unpack('<H', bzs_data[i:i+2])[0]
        # Offset in verse index (multiply by 6 to get byte offset)
        voffset = struct.unpack('<H', bzs_data[i+2:i+4])[0]
        books.append((book_num, voffset))
    return books


def get_book_code(book_num, is_nt=True):
    """Map SWORD book number to 3-letter code."""
    if is_nt:
        # NT books: 1=MAT, 2=MRK, etc.
        nt_map = {
            1: 'MAT', 2: 'MRK', 3: 'LUK', 4: 'JHN',
            5: 'ACT', 6: 'ROM', 7: '1CO', 8: '2CO',
            9: 'GAL', 10: 'EPH', 11: 'PHP', 12: 'COL',
            13: '1TH', 14: '2TH', 15: '1TI', 16: '2TI',
            17: 'TIT', 18: 'PHM', 19: 'HEB', 20: 'JAS',
            21: '1PE', 22: '2PE', 23: '1JN', 24: '2JN',
            25: '3JN', 26: 'JUD', 27: 'REV'
        }
        return nt_map.get(book_num)
    else:
        # OT books: 1=GEN, 2=EXO, etc.
        ot_map = {
            1: 'GEN', 2: 'EXO', 3: 'LEV', 4: 'NUM', 5: 'DEU',
            6: 'JOS', 7: 'JDG', 8: 'RUT', 9: '1SA', 10: '2SA',
            11: '1KI', 12: '2KI', 13: '1CH', 14: '2CH',
            15: 'EZR', 16: 'NEH', 17: 'TOB', 18: 'JDT',
            19: 'EST', 20: 'JOB', 21: 'PSA', 22: 'PRO',
            23: 'ECC', 24: 'SNG', 25: 'WIS', 26: 'SIR',
            27: 'ISA', 28: 'JER', 29: 'LAM', 30: 'BAR',
            31: 'EZK', 32: 'DAN', 33: 'HOS', 34: 'JOL',
            35: 'AMO', 36: 'OBA', 37: 'JON', 38: 'MIC',
            39: 'NAH', 40: 'HAB', 41: 'ZEP', 42: 'HAG',
            43: 'ZEC', 44: 'MAL'
        }
        return ot_map.get(book_num)


def process_module(db_path, module_dir, lang_code='es'):
    """Process a SWORD module (handles both ZIP and LZSS)."""
    print(f"\nProcessing {module_dir} ({lang_code})...")
    base_path = os.path.join('raw_data/_extracted/modules/texts/ztext', module_dir)

    if not os.path.exists(base_path):
        print(f"  Directory not found: {base_path}")
        return

    # Read compressed text
    text_data = None
    for prefix in ['nt', 'ot']:
        bzz_path = os.path.join(base_path, f'{prefix}.bzz')
        if os.path.exists(bzz_path):
            print(f"  Found {prefix}.bzz")
            with open(bzz_path, 'rb') as f:
                compressed = f.read()
            text_data = decompress_lzss(compressed)
            is_nt = (prefix == 'nt')
            print(f"  Decompressed size: {len(text_data)}")
            break

    if text_data is None:
        print(f"  No .bzz files found")
        return

    # Parse verse index
    verses = []
    for prefix in ['nt', 'ot']:
        bzv_path = os.path.join(base_path, f'{prefix}.bzv')
        if os.path.exists(bzv_path):
            with open(bzv_path, 'rb') as f:
                bzv_data = f.read()
            verses = parse_verse_index(bzv_data)
            print(f"  Verse index entries: {len(verses)}")
            break

    if not verses:
        print("  No verse index found")
        return

    # Parse book index
    books = []
    for prefix in ['nt', 'ot']:
        bzs_path = os.path.join(base_path, f'{prefix}.bzs')
        if os.path.exists(bzs_path):
            with open(bzs_path, 'rb') as f:
                bsz_data = f.read()
            books = parse_book_index(bsz_data)
            print(f"  Book index entries: {len(books)}")
            break

    # Connect to database
    conn = sqlite3.connect(db_path)

    # Build book_id lookup from database
    book_cursor = conn.execute('SELECT id, name_en FROM books')
    book_id_map = {}
    code_map = {
        'Genesis': 'GEN', 'Exodus': 'EXO', 'Leviticus': 'LEV', 'Numbers': 'NUM',
        'Deuteronomy': 'DEU', 'Joshua': 'JOS', 'Judges': 'JDG', 'Ruth': 'RUT',
        '1 Samuel': '1SA', '2 Samuel': '2SA', '1 Kings': '1KI', '2 Kings': '2KI',
        '1 Chronicles': '1CH', '2 Chronicles': '2CH', 'Ezra': 'EZR', 'Nehemiah': 'NEH',
        'Tobit': 'TOB', 'Judith': 'JDT', 'Wisdom': 'WIS', 'Sirach': 'SIR',
        'Isaiah': 'ISA', 'Jeremiah': 'JER', 'Lamentations': 'LAM', 'Baruch': 'BAR',
        'Ezekiel': 'EZK', 'Daniel': 'DAN', 'Hosea': 'HOS', 'Joel': 'JOL',
        'Amos': 'AMO', 'Obadiah': 'OBA', 'Jonah': 'JON', 'Micah': 'MIC',
        'Nahum': 'NAH', 'Habakkuk': 'HAB', 'Zephaniah': 'ZEP', 'Haggai': 'HAG',
        'Zechariah': 'ZEC', 'Malachi': 'MAL',
        'Matthew': 'MAT', 'Mark': 'MRK', 'Luke': 'LUK', 'John': 'JHN',
        'Acts': 'ACT', 'Romans': 'ROM', '1 Corinthians': '1CO', '2 Corinthians': '2CO',
        'Galatians': 'GAL', 'Ephesians': 'EPH', 'Philippians': 'PHP', 'Colossians': 'COL',
        '1 Thessalonians': '1TH', '2 Thessalonians': '2TH',
        '1 Timothy': '1TI', '2 Timothy': '2TI', 'Titus': 'TIT', 'Philemon': 'PHM',
        'Hebrews': 'HEB', 'James': 'JAS', '1 Peter': '1PE', '2 Peter': '2PE',
        '1 John': '1JN', '2 John': '2JN', '3 John': '3JN', 'Jude': 'JUD',
        'Revelation': 'REV'
    }
    for book_id, name_en in book_cursor:
        code = code_map.get(name_en)
        if code:
            book_id_map[code] = book_id
    print(f"  Book ID map: {len(book_id_map)} entries")

    # Process each book
    count = 0
    for idx, (book_num, voffset) in enumerate(books):
        # Get verse range for this book
        start_voffset = voffset
        if idx + 1 < len(books):
            end_voffset = books[idx + 1][1]
        else:
            end_voffset = len(verses)

        # Get book code
        book_code = get_book_code(book_num, is_nt)
        if not book_code or book_code not in book_id_map:
            print(f"  Skipping book {book_num} (no mapping)")
            continue

        book_id = book_id_map[book_code]

        # Process verses for this book
        # Verse index entries from start_voffset to end_voffset
        # Each entry: (verse_num, text_offset)
        # verse_num encoding: chapter << 8 | verse
        for vi in range(start_voffset, min(end_voffset, len(verses))):
            verse_num, text_offset = verses[vi]

            # Decode verse_num: chapter = high byte, verse = low byte
            chapter = (verse_num >> 8) & 0xFF
            verse = verse_num & 0xFF

            if chapter == 0 or verse == 0:
                continue

            # Find text offset range
            start = text_offset
            if vi + 1 < len(verses):
                end = verses[vi + 1][1]
            else:
                end = len(text_data)

            raw_text = text_data[start:end]
            text = raw_text.replace(b'\x00', b'').strip()
            if not text:
                continue

            try:
                text_str = text.decode('utf-8', errors='ignore')
            except:
                continue

            if not text_str:
                continue

            # Find verse_id in verses table
            verse_row = conn.execute(
                'SELECT id FROM verses WHERE book_id=? AND chapter=? AND verse_number=?',
                (book_id, chapter, verse)
            ).fetchone()

            if not verse_row:
                continue

            verse_id = verse_row[0]

            # Check if text already exists
            existing = conn.execute(
                'SELECT 1 FROM texts WHERE verse_id=? AND lang_code=?',
                (verse_id, lang_code)
            ).fetchone()

            if not existing:
                conn.execute(
                    'INSERT INTO texts (verse_id, lang_code, content) VALUES (?, ?, ?)',
                    (verse_id, lang_code, text_str)
                )
                count += 1

    conn.commit()
    print(f"  Inserted {count} {lang_code} texts")
    conn.close()


def main():
    db_path = 'app/src/main/assets/verbum_seed.db'

    print("=" * 60)
    print("Processing Spanish Translations")
    print("=" * 60)

    # Process SpaScioNT (LZSS, NT)
    if os.path.exists('raw_data/_extracted/modules/texts/ztext/spasciont'):
        process_module(db_path, 'spasciont', 'es')

    # Process SpaPlatense (LZSS, NT)
    if os.path.exists('raw_data/_extracted/modules/texts/ztext/spaplatense'):
        process_module(db_path, 'spaplatense', 'es')

    print("\n" + "=" * 60)
    print("Done!")


if __name__ == '__main__':
    main()
