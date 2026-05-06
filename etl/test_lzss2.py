#!/usr/bin/env python3
"""Test SWORD LZSS decompression - fixed offset calculation."""

import struct

def decompress_lzss_fixed(compressed_data):
    """
    Decompress SWORD LZSS data - fixed version.
    Based on SWORD lzsscomprs.cpp decode().
    - Uses 7 bits per flag byte (ignores MSB).
    - Offset: c[0] | ((c[1] & 0xf0) << 4)
    - Length: (c[1] & 0x0f) + 3
    """
    if not compressed_data:
        return b''

    N = 4096  # Ring buffer size
    F = 18    # Match length threshold

    buffer = bytearray([0x20] * N)  # Init with spaces
    buf_pos = N - F  # r = N - F

    output = bytearray()
    pos = 0  # Position in compressed data
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

            # SWORD: pos = c[0] | ((c[1] & 0xf0) << 4)
            offset = c0 | ((c1 & 0xf0) << 4)
            length = (c1 & 0x0f) + 3

            for _ in range(length):
                c = buffer[(offset + _) & (N - 1)]
                output.append(c)
                buffer[buf_pos] = c
                buf_pos = (buf_pos + 1) & (N - 1)

    return bytes(output)


# Test
with open('/mnt/disk2/dev/VerbunUniversalis/raw_data/_extracted/modules/texts/ztext/spasciont/nt.bzz', 'rb') as f:
    compressed = f.read()

print(f"Compressed size: {len(compressed)}")

text = decompress_lzss_fixed(compressed)
print(f"Decompressed size: {len(text)}")

# Show first 1000 chars
try:
    decoded = text.decode('utf-8', errors='replace')[:1000]
    print(f"\nFirst 1000 chars:\n{decoded}")
except Exception as e:
    print(f"Error: {e}")
    print(f"First 100 bytes: {text[:100]}")
