# SCREEN$ in ASM: raw and RLE

The screen editor works with the exact ZX Spectrum 48K SCREEN$ layout:
6144 bitmap bytes followed by 768 attribute bytes (6912 bytes total).

## Opening and saving

- Open: `.scr`, `.bin` (exactly 6912 bytes), `.asm` (`.z80` / `.s` also accepted).
- Save as: `.scr`, `.bin`, `.asm` (uncompressed), `.asm (RLE)`.
- Press **Save** to overwrite the currently opened file in its original format,
  including an imported raw or RLE ASM file. **Save as** switches the active
  document to the newly created file.
- ASM files are **data-only**. Labels, `EQU`, `DB` and `DEFB` are accepted;
  machine instructions, `ORG` and `INCLUDE` in the imported screen file are
  intentionally rejected. You can INCLUDE the exported data from your game.

## RLE v1 wire format

An exported RLE file begins with the exact comment marker:

```asm
; Z80_POCKET_IDE_SCREEN_RLE_V1
```

The rest of the file is an ordinary assembler data block starting with
`SCREEN_RLE:`. Every control byte is followed by:

| Control | Meaning |
| --- | --- |
| `$00..$7F` | Copy the next `control + 1` bytes literally (1–128) |
| `$80..$FF` | Repeat the **next one byte** `(control & $7F) + 2` times (2–129) |

There is **no end marker**. The uncompressed length is always 6912 bytes.
The importer rejects truncated data, overflow and any stream that does
not expand to exactly 6912 bytes. Unlike a raw `.asm`, the RLE file is not
directly loadable at `$4000`: it must be decompressed first.

## Example Z80 decoder

Include your exported RLE ASM file in your game and call
`UNPACK_SCREEN` to unpack the image to `$4000..$5AFF`.
The subroutine overwrites AF, BC, DE and HL. The target video RAM must be writable.

```asm
UNPACK_SCREEN:
    LD HL,SCREEN_RLE
    LD DE,$4000
UNPACK_NEXT:
    LD A,D
    CP $5B                  ; $4000 + 6912 = $5B00
    RET Z
    LD A,(HL)
    INC HL
    BIT 7,A
    JR NZ,UNPACK_REPEAT
    INC A                   ; 1..128 literal bytes
    LD C,A
    LD B,0
    LDIR
    JR UNPACK_NEXT
UNPACK_REPEAT:
    AND $7F
    ADD A,2                 ; 2..129 copies
    LD B,A
    LD A,(HL)
    INC HL
UNPACK_REPEAT_LOOP:
    LD (DE),A
    INC DE
    DJNZ UNPACK_REPEAT_LOOP
    JR UNPACK_NEXT

; Include the exported data (SCREEN_RLE label) from your own main program:
; INCLUDE "splash-rle.asm"
```

**Important:** `UNPACK_NEXT` assumes the file was exported by the editor
and thus expands to exactly 6912 bytes. This fast sample decoder is not a
safe decoder for untrusted data. The Android importer validates length/bounds.

## Examples

A solid screen compresses particularly well; noisy/dithered screens may be
slightly larger than 6912 bytes due to literal-length control bytes.
RLE packs the raw bitmap and attributes together, preserving each byte.

## Palette lock

The screen edit lock is inside the palette header when expanded, and it is
the fourth tool in the collapsed palette beside pen, eraser and picker.
Both controls share the same locked/unlocked state with the Tools tab.
