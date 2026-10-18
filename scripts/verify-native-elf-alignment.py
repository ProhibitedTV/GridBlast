#!/usr/bin/env python3
"""Inspect native ELF LOAD segments inside an Android APK for 16 KiB readiness.

For 64-bit libraries, PT_LOAD p_align must be >= 0x4000 and file offset /
virtual address must be congruent modulo 16 KiB. Packaging ZIP alignment is
checked separately with Android SDK zipalign -c -P 16.
"""
import pathlib
import struct
import sys
import zipfile

PAGE = 16 * 1024

def require(ok, message):
    if not ok:
        raise SystemExit("16 KB ELF CHECK FAILED: " + message)

def inspect(name, elf):
    require(elf[:4] == b"\x7fELF", name + ": not ELF")
    elf_class = elf[4]
    require(elf_class in (1, 2), name + ": unsupported ELF class")
    endian = "<" if elf[5] == 1 else ">" if elf[5] == 2 else None
    require(endian is not None, name + ": unsupported byte order")
    if elf_class == 1:
        print(name + ": 32-bit (64-bit page-size requirement not applicable)")
        return
    require(len(elf) >= 64, name + ": short ELF header")
    ph_offset = struct.unpack_from(endian + "Q", elf, 32)[0]
    ph_size = struct.unpack_from(endian + "H", elf, 54)[0]
    ph_count = struct.unpack_from(endian + "H", elf, 56)[0]
    require(ph_count > 0 and ph_count < 1024 and ph_size >= 56, name + ": bad program headers")
    count = 0
    for i in range(ph_count):
        pos = ph_offset + i * ph_size
        require(pos + 56 <= len(elf), name + ": truncated program headers")
        if struct.unpack_from(endian + "I", elf, pos)[0] != 1:
            continue
        offset = struct.unpack_from(endian + "Q", elf, pos + 8)[0]
        address = struct.unpack_from(endian + "Q", elf, pos + 16)[0]
        alignment = struct.unpack_from(endian + "Q", elf, pos + 48)[0]
        require(alignment >= PAGE and (address - offset) % PAGE == 0,
                f"{name}: PT_LOAD alignment {alignment} is not 16 KiB-compatible")
        count += 1
    require(count > 0, name + ": no load segments")
    print(f"{name}: {count} aligned 64-bit LOAD segments")

apk = pathlib.Path(sys.argv[1])
require(apk.is_file(), str(apk) + ": missing")
with zipfile.ZipFile(apk) as archive:
    names = sorted(name for name in archive.namelist()
                   if name.startswith("lib/") and name.endswith(".so"))
    for name in names:
        inspect(name, archive.read(name))
print(f"Verified {len(names)} packaged native libraries; 64-bit ELF segments are 16 KiB-ready.")
