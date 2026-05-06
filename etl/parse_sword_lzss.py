#!/usr/bin/env python3
"""Parse SWORD module with LZSS compression (Spanish SpaScioNT)."""

import struct
import os
import sqlite3

def decompress_lzss(compressed_data):
    """Decompress LZSS-compressed bytes."""
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

                # Decode reference: 12-bit offset + 4-bit length
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


def read_module(base_path, module_dir):
    """Read SWORD module files and extract text."""

    module_path = os.path.join(base_path, module_dir)
    bzz_path = os.path.join(module_path, 'nt.bzz')  # NT text
    bzv_path = os.path.join(module_path, 'nt.bzv')  # NT verse index
    bzs_path = os.path.join(module_path, 'nt.bzs')  # NT book index

    print(f"Reading module: {module_dir}")
    print(f"  bzz: {bzz_path}")
    print(f"  bzv: {bzv_path}")
    print(f"  bzs: {bzs_path}")

    # Decompress text
    with open(bzz_path, 'rb') as f:
        compressed = f.read()
    print(f"  Compressed size: {len(compressed)}")

    text_data = decompress_lzss(compressed)
    print(f"  Decompressed size: {len(text_data)}")
    print(f"  First 100 bytes: {text_data[:100]}")

    # Parse bzv (verse index)
    # Format: each entry is 6 bytes (verse_num: 2 bytes, offset: 4 bytes)
    with open(bzv_path, 'rb') as f:
        bzv_data = f.read()

    verses = []
    for i in range(0, len(bzv_data), 6):
        if i + 5 >= len(bzv_data):
            break
        verse_num = struct.unpack('<H', bzv_data[i:i+2])[0]
        offset = struct.unpack('<I', bzv_data[i+2:i+6])[0]
        verses.append((verse_num, offset))

    print(f"  Parsed {len(verses)} verse entries")
    if verses:
        print(f"  First 3 verses: {verses[:3]}")

    # Parse bzs (book index)
    # Format: each entry is 4 bytes (book_num: 2 bytes, offset: 2 bytes)
    with open(bzs_path, 'rb') as f:
        bzs_data = f.read()

    books = []
    for i in range(0, len(bzs_data), 4):
        if i + 3 >= len(bzs_data):
            break
        book_num = struct.unpack('<H', bzs_data[i:i+2])[0]
        offset = struct.unpack('<H', bzs_data[i+2:i+4])[0]
        books.append((book_num, offset))

    print(f"  Parsed {len(books)} book entries")
    if books:
        print(f"  First 3 books: {books[:3]}")

    return text_data, verses, books


def main():
    base_path = 'raw_data/_extracted/modules/texts/ztext'
    module_dir = 'spasciont'

    if not os.path.exists(os.path.join(base_path, module_dir)):
        print(f"Module not found: {os.path.join(base_path, module_dir)}")
        return

    text_data, verses, books = read_module(base_path, module_dir)

    # Show some sample text
    print("\nSample text around first verse offset:")
    if verses:
        offset = verses[0][1]
        print(text_data[offset:offset+200])


if __name__ == '__main__':
    main()
