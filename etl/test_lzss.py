#!/usr/bin/env python3
"""Test SWORD LZSS decompression with 7-bit flag handling."""

import struct

def decompress_lzss_sword(compressed_data):
    """
    Decompress SWORD LZSS data.
    Based on SWORD lzsscomprs.cpp decode().
    Uses 7 bits per flag byte (not 8).
    """
    if not compressed_data:
        return b''

    N = 4096  # Ring buffer size
    F = 18    # Match length threshold

    buffer = bytearray([0x20] * N)  # Init with spaces
    buf_pos = N - F  # r = N - F

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
            flag_count = 7  # Use 7 bits per flag byte

        if flags & 1:
            # Literal byte
            if pos >= len(compressed_data):
                break
            c = compressed_data[pos]
            pos += 1
            output.append(c)
            buffer[buf_pos] = c
            buf_pos = (buf_pos + 1) & (N - 1)
        else:
            # Reference
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


def decompress_lzss_8bit(compressed_data):
    """Original 8-bit per flag byte version."""
    if not compressed_data:
        return b''

    N = 4096
    buffer = bytearray([0x20] * N)
    buf_pos = 0
    output = bytearray()
    pos = 0

    while pos < len(compressed_data):
        flags = compressed_data[pos]
        pos += 1
        for bit in range(8):
            if pos >= len(compressed_data):
                break
            if flags & (1 << bit):
                output.append(compressed_data[pos])
                buffer[buf_pos] = compressed_data[pos]
                buf_pos = (buf_pos + 1) % N
                pos += 1
            else:
                if pos + 1 >= len(compressed_data):
                    break
                byte1 = compressed_data[pos]
                byte2 = compressed_data[pos + 1]
                pos += 2
                offset = byte1 | ((byte2 & 0x0F) << 8)
                length = (byte2 >> 4) + 3
                for _ in range(length):
                    src = (buf_pos - offset + N) % N
                    byte = buffer[src]
                    output.append(byte)
                    buffer[buf_pos] = byte
                    buf_pos = (buf_pos + 1) % N
    return bytes(output)


# Test both versions
with open('/mnt/disk2/dev/VerbunUniversalis/raw_data/_extracted/modules/texts/ztext/spasciont/nt.bzz', 'rb') as f:
    compressed = f.read()

print(f"Compressed size: {len(compressed)}")

# Try 7-bit version (SWORD)
print("\n--- 7-bit version (SWORD) ---")
text7 = decompress_lzss_sword(compressed)
print(f"Decompressed size: {len(text7)}")
try:
    print(f"First 500 chars:\n{text7[:500].decode('utf-8', errors='replace')}")
except:
    print(f"First 500 bytes: {text7[:500]}")

# Try 8-bit version (original)
print("\n--- 8-bit version (original) ---")
text8 = decompress_lzss_8bit(compressed)
print(f"Decompressed size: {len(text8)}")
try:
    print(f"First 500 chars:\n{text8[:500].decode('utf-8', errors='replace')}")
except:
    print(f"First 500 bytes: {text8[:500]}")
