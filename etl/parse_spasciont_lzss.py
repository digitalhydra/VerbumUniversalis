#!/usr/bin/env python3
"""
Parse spasciont (LZSS NT module) and extract (book, chapter, verse, text).
Uses: nt.bzz (LZSS compressed), nt.bzv (verse index), nt.bzs (book index).
"""

import struct

N = 4096
F = 18

def decompress_lzss(compressed_data):
    """SWORD LZSS decompress - 7 bits/flag byte."""
    if not compressed_data:
        return b''
    buffer = bytearray([0x20] * N)
    buf_pos = N - F
    output = bytearray()
    pos = 0
    flags = 0
    flag_count = 0

    while pos < len(compressed_data):
        if flag_count > 0:
            flags >>= 1
            flag_count -= 1
        else:
            if pos >= len(compressed_data):
                break
            flags = compressed_data[pos]
            pos += 1
            flag_count = 7

        if flags & 1:
            if pos >= len(compressed_data):
                break
            c = compressed_data[pos]
            pos += 1
            output.append(c)
            buffer[buf_pos] = c
            buf_pos = (buf_pos + 1) & (N - 1)
        else:
            if pos + 1 >= len(compressed_data):
                break
            c0 = compressed_data[pos]
            c1 = compressed_data[pos + 1]
            pos += 2
            offset = c0 | ((c1 & 0xf0) << 4)
            length = (c1 & 0x0f) + 3
            for _ in range(length):
                c = buffer[(offset + _) & (N - 1)]
                output.append(c)
                buffer[buf_pos] = c
                buf_pos = (buf_pos + 1) & (N - 1)
    return bytes(output)


def parse_bzs(bzs_path):
    """Parse .bzs: 4 bytes/entry (book_num:2, offset:2)."""
    with open(bzs_path, 'rb') as f:
        data = f.read()
    books = []
    for i in range(0, len(data), 4):
        if i + 3 >= len(data):
            break
        book_num = struct.unpack('<H', data[i:i+2])[0]
        offset = struct.unpack('<H', data[i+2:i+4])[0]
        books.append((book_num, offset))
    return books


def parse_bzv(bzv_path):
    """Parse .bzv: 6 bytes/entry (verse_num:2, offset:4)."""
    with open(bzv_path, 'rb') as f:
        data = f.read()
    verses = []
    for i in range(0, len(data), 6):
        if i + 5 >= len(data):
            break
        verse_num = struct.unpack('<H', data[i:i+2])[0]
        offset = struct.unpack('<I', data[i+2:i+6])[0]
        verses.append((verse_num, offset))
    return verses


def extract_verses(text_data, verse_index):
    """Extract verse texts using offset table."""
    result = {}
    for idx, (verse_num, offset) in enumerate(verse_index):
        start = offset
        if idx + 1 < len(verse_index):
            end = verse_index[idx + 1][1]
        else:
            end = len(text_data)
        raw = text_data[start:end]
        # Clean
        raw = raw.replace(b'\x00', b'').replace(b'\r', b'').replace(b'\n', b' ')
        if raw:
            try:
                text = raw.decode('utf-8', errors='replace').strip()
                # Strip ThML tags
                import re
                text = re.sub(r'<[^>]+>', ' ', text)
                text = re.sub(r'\s+', ' ', text).strip()
                if text:
                    result[verse_num] = text
            except:
                pass
    return result


# KJV NT books in order: Matt=1, Mark=2, ..., Rev=27
# Verse counts per book (cumulative) for KJV NT
# This is needed to map sequential verse numbers to (book, chapter, verse)
kjv_nt_verse_counts = [
    (1, 'Matt', 1071),   # Matthew: 1071 verses
    (2, 'Mark', 678),    # Mark: 678 verses
    (3, 'Luke', 1151),   # Luke: 1151 verses
    (4, 'John', 869),     # John: 869 verses
    (5, 'Acts', 1007),   # Acts: 1007 verses
    (6, 'Rom', 433),     # Romans: 433 verses
    (7, '1Cor', 437),   # 1 Corinthians: 437 verses
    (8, '2Cor', 257),   # 2 Corinthians: 257 verses
    (9, 'Gal', 149),     # Galatians: 149 verses
    (10, 'Eph', 155),    # Ephesians: 155 verses
    (11, 'Phil', 104),   # Philippians: 104 verses
    (12, 'Col', 95),     # Colossians: 95 verses
    (13, '1Thess', 89),  # 1 Thessalonians: 89 verses
    (14, '2Thess', 47),  # 2 Thessalonians: 47 verses
    (15, '1Tim', 113),   # 1 Timothy: 113 verses
    (16, '2Tim', 83),    # 2 Timothy: 83 verses
    (17, 'Titus', 46),   # Titus: 46 verses
    (18, 'Phlm', 25),    # Philemon: 25 verses
    (19, 'Heb', 303),    # Hebrews: 303 verses
    (20, 'Jas', 108),    # James: 108 verses
    (21, '1Pet', 105),   # 1 Peter: 105 verses
    (22, '2Pet', 61),    # 2 Peter: 61 verses
    (23, '1John', 105),  # 1 John: 105 verses
    (24, '2John', 13),   # 2 John: 13 verses
    (25, '3John', 14),   # 3 John: 14 verses
    (26, 'Jude', 25),    # Jude: 25 verses
    (27, 'Rev', 404),    # Revelation: 404 verses
]

# Build cumulative verse counts
cumulative = 0
kjv_nt_books = []
for book_num, book_code, count in kjv_nt_verse_counts:
    start = cumulative + 1
    cumulative += count
    end = cumulative
    kjv_nt_books.append((book_num, book_code, start, end, count))

def map_verse_to_book(verse_num):
    """Map sequential verse number to (book_code, book_verse_num)."""
    for book_num, book_code, start, end, count in kjv_nt_books:
        if start <= verse_num <= end:
            return (book_code, verse_num - start + 1)
    return (None, None)


# Main
base = '/mnt/disk2/dev/VerbunUniversalis/raw_data/_extracted/modules/texts/ztext/spasciont'

# Decompress
print("Decompressing...")
with open(f'{base}/nt.bzz', 'rb') as f:
    compressed = f.read()
text_data = decompress_lzss(compressed)
print(f"Decompressed size: {len(text_data)}")

# Parse indices
print("Parsing indices...")
bzv = parse_bzv(f'{base}/nt.bzv')
print(f"  Verse index entries: {len(bzv)}")

# Extract verses
print("Extracting verses...")
verse_texts = extract_verses(text_data, bzv)
print(f"  Extracted {len(verse_texts)} verses")

# Map to (book_code, book_verse_num)
print("\nSample verses:")
for verse_num in sorted(verse_texts.keys())[:10]:
    text = verse_texts[verse_num]
    book_code, book_verse = map_verse_to_book(verse_num)
    print(f"  Global verse {verse_num} -> {book_code} verse {book_verse}: {text[:80]}")

# Check mapping
print("\nVerse mapping check:")
# Matt 1:1 should be global verse 1
book_code, book_verse = map_verse_to_book(1)
print(f"  Global verse 1 -> {book_code} {book_verse}")

# Matt 1:1 text
if 1 in verse_texts:
    print(f"  Matt 1:1 text: {verse_texts[1][:100]}")
