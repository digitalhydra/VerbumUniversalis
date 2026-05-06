#!/usr/bin/env python3
"""LZSS decompression for SWORD modules."""

def decompress_lzss(compressed_data, decompressed_size=None):
    """
    Decompress LZSS-compressed bytes.
    LZSS is a LZ77 variant used by SWORD.
    
    Format (from SWORD lzss.cpp):
    - Uses a ring buffer of 4096 bytes
    - Flags byte: each bit indicates if next is literal (1) or reference (0)
    - Literal: copy byte as-is
    - Reference: 12-bit offset + 4-bit length (16-bit total)
      Actually: 4-bit length + 12-bit offset (stored as two bytes)
    """
    if not compressed_data:
        return b''
    
    # Ring buffer (4096 bytes)
    buffer_size = 4096
    buffer = bytearray(buffer_size)
    buf_pos = 0
    
    output = bytearray()
    pos = 0
    data = compressed_data
    
    while pos < len(data):
        # Read flags byte
        flags = data[pos]
        pos += 1
        
        # Process each bit (from LSB to MSB)
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
                
                # Decode reference: byte1 is low 8 bits of offset
                # byte2: high 4 bits are length, low 4 bits are high 4 bits of offset
                # Actually from SWORD source:
                # offset = byte1 | ((byte2 & 0x0F) << 8)  # 12-bit offset
                # length = (byte2 >> 4) + 3  # 4-bit length + 3
                
                offset = byte1 | ((byte2 & 0x0F) << 8)
                length = (byte2 >> 4) + 3
                
                # Copy from ring buffer
                for i in range(length):
                    src_pos = (buf_pos - offset + buffer_size) % buffer_size
                    byte = buffer[src_pos]
                    output.append(byte)
                    buffer[buf_pos] = byte
                    buf_pos = (buf_pos + 1) % buffer_size
    
    # Truncate to decompressed_size if specified
    if decompressed_size is not None:
        output = output[:decompressed_size]
    
    return bytes(output)


def decompress_lzss_file(filepath):
    """Decompress an LZSS-compressed file."""
    with open(filepath, 'rb') as f:
        data = f.read()
    return decompress_lzss(data)


if __name__ == '__main__':
    import sys
    
    # Test with SWORD module files
    import os
    
    # Try to decompress nt.bzz from SpaScioNT
    bzz_path = 'raw_data/_extracted/modules/texts/ztext/spasciont/nt.bzz'
    
    if os.path.exists(bzz_path):
        print(f"Decompressing {bzz_path}...")
        try:
            with open(bzz_path, 'rb') as f:
                data = f.read()
            result = decompress_lzss(data)
            print(f"Decompressed {len(data)} -> {len(result)} bytes")
            print(f"First 200 bytes: {result[:200]}")
        except Exception as e:
            print(f"Error: {e}")
    else:
        print(f"File not found: {bzz_path}")
        print("Available files:")
        for root, dirs, files in os.walk('raw_data/_extracted'):
            for f in files:
                print(f"  {os.path.join(root, f)}")
