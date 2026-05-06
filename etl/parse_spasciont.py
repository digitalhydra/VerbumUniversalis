#!/usr/bin/env python3
"""Parse Spanish (Scío NT) SWORD module - LZSS format."""

import struct
import os
import re

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

                # SWORD LZSS: byte1 = low 8 bits of offset, byte2 = (high 4 bits of offset) + (4-bit length)
                offset = byte1 | ((byte2 & 0x0F) << 8)
                length = (byte2 >> 4) + 3

                for i in range(length):
                    src_pos = (buf_pos - offset + buffer_size) % buffer_size
                    byte = buffer[src_pos]
                    output.append(byte)
                    buffer[buf_pos] = byte
                    buf_pos = (buf_pos + 1) % buffer_size

    return bytes(output)


def parse_module(module_dir):
    """Parse SWORD module (LZSS format for text)."""
    base = os.path.join('raw_data/_extracted/modules/texts/ztext', module_dir)

    # Read compressed text (nt.bzz for NT)
    bzz_path = os.path.join(base, 'nt.bzz')
    with open(bzz_path, 'rb') as f:
        compressed = f.read()
    print(f"Compressed size: {len(compressed)} bytes")

    text_data = decompress_lzss(compressed)
    print(f"Decompressed size: {len(text_data)} bytes")

    # Read verse index (nt.bzv) - 6 bytes per entry: verse_num (2 bytes) + offset (4 bytes)
    bzv_path = os.path.join(base, 'nt.bzv')
    with open(bzv_path, 'rb') as f:
        bzv_data = f.read()

    verses = []
    for i in range(0, len(bzv_data), 6):
        if i + 5 >= len(bzv_data):
            break
        verse_num = struct.unpack('<H', bzv_data[i:i+2])[0]
        offset = struct.unpack('<I', bzv_data[i+2:i+6])[0]
        verses.append((verse_num, offset))

    print(f"Parsed {len(verses)} verse entries")

    # Read book index (nt.bzs) - 4 bytes per entry: book_num (2 bytes) + offset (2 bytes)
    bzs_path = os.path.join(base, 'nt.bzs')
    with open(bzs_path, 'rb') as f:
        bzs_data = f.read()

    books = []
    for i in range(0, len(bzs_data), 4):
        if i + 3 >= len(bzs_data):
            break
        book_num = struct.unpack('<H', bzs_data[i:i+2])[0]
        offset = struct.unpack('<H', bzs_data[i+2:i+4])[0]
        books.append((book_num, offset))

    print(f"Parsed {len(books)} book entries")

    # Extract verse text using offsets
    # Text between verse offsets (null-terminated strings in SWORD)
    verse_texts = []
    for idx, (verse_num, offset) in enumerate(verses):
        start = offset
        # Find next null byte or next verse offset
        if idx + 1 < len(verses):
            end = verses[idx + 1][1]
        else:
            end = len(text_data)

        # Extract text (skip null bytes at boundaries)
        raw = text_data[start:end]
        # Remove leading/trailing null bytes and control chars
        text = raw.replace(b'\x00', b'').replace(b'\n', b'').replace(b'\r', b'').strip()
        if text:
            try:
                verse_texts.append((verse_num, text.decode('utf-8', errors='ignore')))
            except:
                verse_texts.append((verse_num, repr(text)))

    return books, verses, verse_texts


def main():
    module_dir = 'spasciont'
    print(f"Parsing module: {module_dir}")

    books, verses, texts = parse_module(module_dir)

    print(f"\nSample verses:")
    for verse_num, text in texts[:5]:
        print(f"  Verse {verse_num}: {text[:100]}")

    # Show book mapping
    print(f"\nBook index entries (first 5):")
    for book_num, offset in books[:5]:
        print(f"  Book {book_num}: offset {offset}")


if __name__ == '__main__':
    main()
