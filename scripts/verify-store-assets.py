#!/usr/bin/env python3
"""Check original Google Play listing artwork with Python standard library.

This validates PNG structure, dimensions, channels, checksums, compressed pixel
data and the Play icon filesize requirement. It does not replace review of
authentic in-app screenshots, visual quality, brand rights or Play Console.
"""

from __future__ import annotations

import struct
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = (
    ("docs/brand/gridblast-feature-1024x500.png", 1024, 500, 2, None),
    ("docs/brand/gridblast-icon-512x512.png", 512, 512, 6, 1024 * 1024),
)
SIGNATURE = b"\x89PNG\r\n\x1a\n"


def check(path: Path, width: int, height: int, color_type: int, max_bytes: int | None) -> None:
    data = path.read_bytes()
    if max_bytes is not None:
        assert len(data) <= max_bytes, f"{path}: exceeds {max_bytes} bytes"
    assert data.startswith(SIGNATURE), f"{path}: not a PNG"
    offset, found_ihdr, found_end = len(SIGNATURE), False, False
    idat = bytearray()

    while offset < len(data):
        assert len(data) - offset >= 12, f"{path}: incomplete PNG chunk"
        length = struct.unpack_from(">I", data, offset)[0]
        start = offset + 8
        end = start + length
        assert end + 4 <= len(data), f"{path}: chunk truncated"
        kind = data[offset + 4 : start]
        payload = data[start:end]
        expected_crc = struct.unpack_from(">I", data, end)[0]
        actual_crc = zlib.crc32(kind + payload) & 0xFFFFFFFF
        assert actual_crc == expected_crc, f"{path}: invalid {kind!r} CRC"
        offset = end + 4

        if kind == b"IHDR":
            assert not found_ihdr and length == 13, f"{path}: invalid IHDR"
            w, h, depth, typ, compression, filtering, interlace = struct.unpack(
                ">IIBBBBB", payload
            )
            assert (w, h, depth, typ, compression, filtering, interlace) == (
                width, height, 8, color_type, 0, 0, 0
            ), f"{path}: wrong dimensions, bit depth or color format"
            found_ihdr = True
        elif kind == b"IDAT":
            idat.extend(payload)
        elif kind == b"IEND":
            assert length == 0, f"{path}: invalid IEND"
            found_end = True
            break

    assert found_ihdr and found_end and offset == len(data), f"{path}: malformed PNG end"
    channels = 3 if color_type == 2 else 4
    expected_len = height * (1 + width * channels)
    pixels = zlib.decompress(idat)
    assert len(pixels) == expected_len, f"{path}: wrong pixel payload length"
    assert all(pixels[y * (width * channels + 1)] <= 4 for y in range(height)), (
        f"{path}: unsupported PNG row filter"
    )
    print(f"OK {path.relative_to(ROOT)}: {width}x{height}, {len(data)} bytes")


def main() -> int:
    try:
        for relpath, w, h, fmt, max_size in ASSETS:
            check(ROOT / relpath, w, h, fmt, max_size)
    except (AssertionError, OSError, struct.error, zlib.error) as exc:
        print(f"Store asset validation failed: {exc}", file=sys.stderr)
        return 1
    print("Artwork validated. Real screenshots, brand review and Play Console QA remain required.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
