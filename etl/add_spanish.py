#!/usr/bin/env python3
"""Parse Spanish Bible modules and insert into verbum_seed.db."""

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

                # SWORD LZSS: 12-bit offset + 4-bit length
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


def parse_verse_index(bvz_data):
    """Parse bvz (verse index). Returns list of (verse_num, offset)."""
    verses = []
    for i in range(0, len(bvz_data), 6):
        if i + 5 >= len(bvz_data):
            break
        verse_num = struct.unpack('<H', bvz_data[i:i+2])[0]
        offset = struct.unpack('<I', bvz_data[i+2:i+6])[0]
        verses.append((verse_num, offset))
    return verses


def parse_book_index(bsz_data):
    """Parse bsz (book index). Returns list of (book_num, offset)."""
    books = []
    for i in range(0, len(bsz_data), 4):
        if i + 3 >= len(bsz_data):
            break
        book_num = struct.unpack('<H', bsz_data[i:i+2])[0]
        offset = struct.unpack('<H', bsz_data[i+2:i+4])[0]
        books.append((book_num, offset))
    return books


def get_book_code(book_num):
    """Map KJV book number to 3-letter code."""
    book_map = {
        1: 'MAT', 2: 'MRK', 3: 'LUK', 4: 'JHN',
        5: 'ACT', 6: 'ROM', 7: '1CO', 8: '2CO',
        9: 'GAL', 10: 'EPH', 11: 'PHP', 12: 'COL',
        13: '1TH', 14: '2TH', 15: '1TI', 16: '2TI',
        17: 'TIT', 18: 'PHM', 19: 'HEB', 20: 'JAS',
        21: '1PE', 22: '2PE', 23: '1JN', 24: '2JN',
        25: '3JN', 26: 'JUD', 27: 'REV'
    }
    return book_map.get(book_num)


def process_module(db_path, module_dir, module_name):
    """Process a Spanish SWORD module and insert into verbum_seed.db."""
    print("\nProcessing " + module_name + "...")
    base_path = os.path.join('raw_data/_extracted/modules/texts/ztext', module_dir)

    # Read compressed text (nt.bzz for NT)
    bzz_path = os.path.join(base_path, module_name + '.bzz')
    with open(bzz_path, 'rb') as f:
        compressed = f.read()
    print("  Compressed size: " + str(len(compressed)))

    text_data = decompress_lzss(compressed)
    print("  Decompressed size: " + str(len(text_data)))

    # Parse verse index (nt.bvz)
    bvz_path = os.path.join(base_path, module_name + '.bvz')
    if os.path.exists(bvz_path):
        with open(bvz_path, 'rb') as f:
            bvz_data = f.read()
        verses = parse_verse_index(bvz_data)
        print("  Verse entries: " + str(len(verses)))
    else:
        print("  Verse index (.bvz) not found")
        verses = []

    # Parse book index (nt.bsz)
    bsz_path = os.path.join(base_path, module_name + '.bsz')
    if os.path.exists(bsz_path):
        with open(bsz_path, 'rb') as f:
            bsz_data = f.read()
        books = parse_book_index(bsz_data)
        print("  Book entries: " + str(len(books)))
    else:
        print("  Book index (.bsz) not found")
        books = []

    # Connect to database
    conn = sqlite3.connect(db_path)

    # Build book_id lookup from books table
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
    print("  Book ID map: " + str(len(book_id_map)) + " entries")

    # Insert Spanish texts
    count = 0
    for idx, (verse_num, offset) in enumerate(verses):
        # Extract book_num, chapter, verse from verse_num
        # SWORD verse_num encoding: book_num << 16 | chapter << 8 | verse
        book_num = (verse_num >> 16) & 0xFF
        chapter = (verse_num >> 8) & 0xFF
        verse = verse_num & 0xFF

        book_code = get_book_code(book_num)
        if not book_code or book_code not in book_id_map:
            continue;

        book_id = book_id_map[book_code]

        # Find text offset and length
        start = offset
        if idx + 1 < len(verses):
            end = verses[idx + 1][1]
        else:
            end = len(text_data)

        raw_text = text_data[start:end]
        # Remove null bytes and strip
        text = raw_text.replace(b'\x00', b'').strip()
        if not text:
            continue;

        try:
            text_str = text.decode('utf-8', errors='ignore')
        except:
            continue;

        if not text_str:
            continue;

        # Find verse_id in verses table
        verse_row = conn.execute(
            'SELECT id FROM verses WHERE book_id=? AND chapter=? AND verse_number=?',
            (book_id, chapter, verse)
        ).fetchone()

        if not verse_row:
            continue;

        verse_id = verse_row[0];

        # Check if ES text already exists
        existing = conn.execute(
            'SELECT 1 FROM texts WHERE verse_id=? AND lang_code=?',
            (verse_id, 'es')
        ).fetchone()

        if not existing:
            conn.execute(
                'INSERT INTO texts (verse_id, lang_code, content) VALUES (?, ?, ?)',
                (verse_id, 'es', text_str)
            )
            count += 1;

    conn.commit()
    print("  Inserted " + str(count) + " Spanish texts")
    conn.close()


def main():
    db_path = 'app/src/main/assets/verbum_seed.db'

    # Process SpaScioNT (NT, LZSS)
    process_module(db_path, 'spasciont', 'spasciont')

    # Process SpaPlatense (NT, ZIP - extracted already)
    if os.path.exists('raw_data/_extracted/modules/texts/ztext/spaplatense'):
        process_module(db_path, 'spaplatense', 'spaplatense')
    else:
        print("\nSpaPlatense not found, skipping...")


if __name__ == '__main__':
    main()
