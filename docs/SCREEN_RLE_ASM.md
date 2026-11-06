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

## Legacy / SpaceCan zero-escape RLE

The importer also supports the SpaceCan variant whose ASM header declares:

```asm
; Escape: 00,count,byte = repeat; all nonzero bytes are literals.
SPACE_CAN_SCREEN_RLE:
    DB #CC,#CC,#CC,#80,#00,#19,#00
```

In this format, every byte other than zero represents itself; a zero introduces
a three-byte run `00,count,value` where `count` is 1–255. The importer must
expand exactly 6912 bytes, and rejects truncated/overflow/zero-count streams.
Unlike our original RLE v1, there are **no literal-length control bytes**.

On Save, this format is preserved with its original data/end labels when
available. Save As also offers **.asm (RLE 00)**. Newly exported files contain
`Z80_POCKET_IDE_SCREEN_RLE_ZERO_V1` as their explicit format marker, plus
the compatibility comment shown above. Other ASM screens are unchanged.

For a Z80 game, the legacy SpaceCan RLE file requires a **zero-escape**
decoder, not the `UNPACK_SCREEN` decoder above (which is for RLE v1).
