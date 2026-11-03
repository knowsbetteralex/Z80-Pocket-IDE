# Z80 Pocket IDE — SCR Screen Editor v0.38.0

Status: approved design specification, implementation pending.
Language: Russian UI + English UI using existing AppLanguage/Texts style.
Platform: native Android Java, minSdk 23, targetSdk 35, **no AndroidX**.
Branch: main. Keep all existing ASM editor, sprite editor and project/TAP functions working.

## 1. Goal

Add a full-screen ZX Spectrum 48K SCREEN$ editor for raw 6912-byte screens, integrated with Project > Project settings. Work directly with the original Spectrum screen data, not an RGB image. The new editor must support portrait and edge-to-edge landscape orientations, zoom/pan gestures, an upper-right semitransparent collapsible palette, attribute-correct INK/PAPER editing, lock, undo/redo, open SCR/BIN/ASM and save SCR/BIN/export ASM.

The end user can edit assets/loading.scr, save it and immediately build/run the project's TAP with the edited loading screen (existing v0.37.0 loader). The editor is a separate Activity; it should not force MainActivity or the existing sprite editor into landscape.

## 2. Existing code to reuse, and what not to reuse

- `tools/SpriteCanvasView.java`: reference implementation for touch gestures, locked one-finger pan, two-finger pan/zoom, stroke interpolation, Fit and grids. **Extract small reusable touch math where sensible**; do not use SpriteData as the full-screen document.
- `tools/SpriteData.java` and `tools/SpriteToolPanel.java`: reference for 8×8 attribute semantics, visual palette/swatches, lock icon and styling. SpriteData is a 64×64 sprite backing store with viewport offsets/masks; it must stay unchanged for existing sprite callers.
- `project/SpectrumScreen.java`: existing pure-Java SCREEN$ 6912-byte decoder with pixel addressing and Spectrum palette. Extend it or reuse its palette/decoder to avoid divergent rendering.
- `project/ProjectConfiguration.java`, `ProjectSettingsDialog.java`, `ProjectSettingsStore.java`: v0.37.0 selected loadingScreen.file, project-relative paths and SAF project folder. These settings and old JSON must remain compatible.
- `source/AndroidIncludeFiles.java`: existing SAF tree locator and project asset enumeration.
- `tools/ScreenTouchMath.java`: existing hit-testing for character cells and bits; reuse its pure math when appropriate.
- `app/src/main/AndroidManifest.xml`: register one new non-exported Activity.

## 3. Navigation and entry points

A) `ToolsActivity` gains an entry `Screen editor / Редактор экрана`. Prefer a compact button or a fifth tab that launches the new full-screen Activity, *not* embedding a 256×192 canvas inside the current scrollable SpriteToolPanel.

B) `Project > Project settings` gains a visible `Edit screen / Редактировать заставку` action adjacent to the existing preview/selected file. If loadingScreen.file is present, open the same project-relative SCR/BIN resource with an intent extra. If not, launch a new blank screen and allow Save As. Do **not** auto-enable a new screen in settings without the user's explicit choice.

C) Main file-open flow should not replace the ASM code editor with a binary file: selecting a .scr or .bin routes to ScreenEditorActivity. Selecting an .asm from inside Screen Editor requests data-only ASM import; opening an ordinary .asm in the main IDE continues to open code as usual.

D) The editor's title shows the current filename, a dirty marker when unsaved, and a back button; returning while dirty prompts Save / Discard / Cancel.

## 4. Editor layout and orientation

Top compact action bar, drawn inside the safe-area/system-bar insets:
- Back; Open; Save; Save as; Undo; Redo; Fit; Portrait/Landscape; Lock/Unlock.
- Use existing visual vocabulary (green/light panels, compact icons). Buttons must have accessibility content descriptions in RU/EN.
- Landscape: large canvas occupying essentially all available screen space, compact header/status, optional immersive system-bar hiding **only within ScreenEditorActivity**. No forced rotation for MainActivity.
- Portrait: full available canvas below the compact header; screen is initially fitted with its 4:3 ratio.
- Orientation toggle via Activity.setRequestedOrientation, with state retained across recreation (document bytes, undo/redo, dirty state, selected ink/paper, viewport transform, palette expanded state, lock). The same must survive a normal device rotation.
- Do not let soft navigation bars or the status bar overlap controls.

Canvas view:
- Render 256×192 faithfully (nearest-neighbor, no filtering), with pixel grid only at a sufficiently large zoom and distinct 8×8 attribute grid at intermediate zoom.
- Fit centers all of the screen; pinch zoom around gesture focal point, sensible min/max zoom; freely pan while zoomed. Clamp or retain visible margins to avoid losing the entire image offscreen.
- One finger in unlocked Draw mode paints; two fingers always pan/pinch and immediately stop the stroke (never leave a rogue pixel). One finger in Lock mode pans and **does not** modify document bytes.
- Touch coordinates map to pixel (x=0..255,y=0..191) and tile (x/8=0..31,y/8=0..23), including at arbitrary zoom/pan. Interpolate strokes to avoid broken diagonal lines. An entire stroke is a single undo operation.
- A single click in Pick mode samples the touched 8×8 cell's INK/PAPER/BRIGHT/FLASH and currently selected pixel value without mutation.
- Optionally provide a non-destructive cursor marker. Selected coordinates/attribute cell and dirty state appear in a compact status line.

## 5. Floating palette

A semi-transparent floating overlay pinned at **top-right inside safe insets**, above the canvas. It does not scroll or zoom with the image.

Expanded state:
- Header with collapse chevron and drag-free touch target.
- INK / PAPER segmented toggle. Clear indication which type is being painted.
- Eight Spectrum swatches (BLACK, BLUE, RED, MAGENTA, GREEN, CYAN, YELLOW, WHITE).
- BRIGHT toggle (one shared BRIGHT bit per attribute tile, not separate INK and PAPER brightness).
- FLASH toggle.
- Tools: Draw (per pixel), Attributes (whole 8×8 tile), Pick (read tile), and Lock (read-only navigation). Optional Erase maps to writing bitmap bit 0 without recoloring the tile.
- A simple visible 8×8 block outline while changing attributes.

Collapsed state:
- Small semitransparent chip/icon at the same corner; always tappable, with no obstruction to most of the canvas.
- Alpha around 0.80–0.88 is a starting design value; controls/text must remain legible over any ink/paper. Palette tap events **must not** pass through to canvas and paint pixels.

## 6. Spectrum byte-level document — critical invariant

Create `project/SpectrumScreenDocument.java` (pure Java), with fixed 6912-byte array:
- Bytes 0..6143 are the exact Spectrum bitmap in ROM display order.
- Bytes 6144..6911 are the 32×24 attributes in row-major order.
- Bitmap byte offset for pixel (x,y):
  `((y & 0xC0) << 5) | ((y & 7) << 8) | ((y & 0x38) << 2) | (x >> 3)`
- Pixel bit mask: `0x80 >> (x & 7)`.
- Attribute index: `6144 + (y >> 3) * 32 + (x >> 3)`.
- Attribute: bits 0–2 INK, bits 3–5 PAPER, bit 6 BRIGHT, bit 7 FLASH.

API sketch:
- `fromRaw(byte[6912])` (defensive copy), `toRaw()` (defensive copy), `getPixel/setPixel`, `attribute/setAttribute`, `setInk/setPaper/setBright/setFlash`, `paintPixel`.
- Each setter changes **only** the required bit(s) and preserves every unrelated bit; untouched files round-trip byte-for-byte.
- Draw in INK mode sets bitmap bit=1 and updates **only** that cell's INK (and BRIGHT/FLASH if user explicitly selected them as part of brush attributes).
- Draw in PAPER mode clears bitmap bit=0 and updates **only** that cell's PAPER (and explicitly selected shared bits). Do not globally recolor other 8×8 cells.
- Editing the INK/PAPER color of a cell inevitably recolors other foreground/background pixels in that cell, matching real hardware. Explicitly explain this behavior in a small first-use hint.
- Attributes mode changes chosen attribute bits of the touched tile without changing the bitmap. Pick mode never mutates bytes.
- Paint controls are independent *brush settings*, not global properties of all cells. Loading existing SCR must not recolor the document.

FLASH preview may be static-normal by default; optional flashing simulation can be added later without changing stored bytes. Preserve FLASH bit regardless.

## 7. Undo/redo and lifecycle safety

- Group one continuous brush gesture into one operation; record before/after differences for affected bitmap bytes and attribute bytes. On repeated writes to a byte, keep its first before value and final after value.
- Attribute strokes also group into one operation. Undo/redo restore **exact bytes**, including BRIGHT/FLASH and other cells.
- Keep a bounded history (e.g., last 100 operations and/or explicit memory cap), drop redo when new edits occur, and do not mark the file dirty when no byte actually changes.
- Dirty should be based on saved revision/snapshot rather than merely any touch.
- Save/Save As failure retains dirty document, history, and URI. Cancel in a picker does not discard the document.
- Retain current document bytes, undo/redo or minimum an unsaved draft, viewport, mode, orientation, palette and lock across Activity recreation/process restoration. Do not store huge undo stacks in saved-instance-state Bundles; use a ViewModel-free retained store or small durable recovery draft appropriate for this app.
- Start locked (matching the existing sprite editor) and make the unlock state obvious.

## 8. Open/import sources

File picker: Android Storage Access Framework `ACTION_OPEN_DOCUMENT`, MIME `*/*` (providers differ on extensions), read grant persistence when offered; show clear errors for missing permission or insufficient bytes.

`.scr` and `.bin`:
- **Exactly 6912 raw bytes**; not an image (PNG/JPG), not a TAP header, not 6144-byte bitmap-only.
- Reject truncated/oversized data with an explanatory message. Preserve original URI and extension for Save when write permission exists.
- A project-relative file is preferred when opened through project settings; avoid duplicating file contents in ProjectConfiguration.

`.asm`:
- A documented **data-only** importer in `ScreenAsmCodec.java`; do not pretend to import arbitrary executable Z80 programs.
- At minimum accept labels and `DB`/`DEFB` with comma-separated numeric bytes ($xx, decimal, %binary, 0xx optional), quoted characters/strings compatible with the existing assembler; support comments, whitespace and optionally top-level `EQU` constants.
- Support the exact ASM format the editor itself exports. Prefer reusing the existing assembler's expression evaluator or a sanitized data-only input fed to the assembler, rather than building an inconsistent second expression language.
- Reject opcodes, unsupported macros/INCLUDE, address gaps and ambiguous constructs with a line-specific error; never silently pad/truncate. Exactly 6912 emitted bytes are required.
- Importing from ASM is non-destructive: source ASM is **read-only as an import source**. The default Save flow for imported ASM opens Save As SCR; only an explicit Export ASM overwrites/creates ASM.

Do not modify binary content when opening, previewing, rotating or zooming.

## 9. Save/export

- `Save` for a backed .scr/.bin writes **exactly 6912 bytes** to the current SAF document URI using the existing grant; handle read-only providers by directing to Save As.
- `Save as…` uses `ACTION_CREATE_DOCUMENT` and offers .scr as default extension; .bin is an explicit choice. Do not accidentally add .txt.
- `Export ASM` produces human-readable source with exactly two labels and data-only byte rows, for example:

```asm
; ZX Spectrum SCREEN$ — 6912 bytes, no ORG
SCREEN_BITMAP:
    DB $00,$80,$FF,$3C ; ... 6144 bytes total
SCREEN_ATTRS:
    DB $47,$07,$07,$07 ; ... 768 bytes total
```

- Output **only** real bytes (not illustrative ellipses). Use a sensible fixed number of bytes per row (16 or 32). No default ORG in exported ASM so it can be INCLUDED in other sources.
- Round-trip `SCR -> ASM export -> ASM import -> SCR` must yield identical 6912 bytes.
- When a file is associated with the project's loadingScreen.file and saved, the next TAP build reads the updated file automatically. Do **not** require regenerating/synchronizing an extra copy of the image.

## 10. Integration with project settings and build

- `ProjectSettingsDialog`: add `Edit loading screen` by the existing screen preview/file chooser. If no path selected, the action creates a blank document; saving it does not silently change the project config, but may offer `Use this as loading screen?`.
- `MainActivity`: add Screen Editor launch from menu/tools, plus integration with the existing project file chooser. Keep old `Open file / Open project` behavior for ASM code.
- For project-based SCR files use the same SAF tree and project-relative path mechanisms that v0.37.0 TAP export already uses. Make changes visible in the project settings preview after returning to MainActivity.
- If user saves a new screen outside the project tree, do not write an invalid relative path into `.z80pocketide.json`. Offer project-folder copy/import first, or leave configuration unchanged with an explanation.
- Editing/saving SCR must invalidate any cached TAP build (and the next run/export must read the changed file). Verify that the current `buildSource` mechanism continues to check screen length, image path and game CODE overlap with $4000–$5AFF.
- Standalone files remain usable without opening a project.

## 11. Recommended implementation units

New:
1. `project/SpectrumScreenDocument.java` — exact bitmap/attribute model, pure Java; byte-level tests.
2. `project/ScreenAsmCodec.java` — validated ASM import/export, pure Java; round-trip tests.
3. `screen/ScreenEditorCanvasView.java` — nearest-neighbor drawing, pan/zoom, touch hit testing, grids.
4. `screen/ScreenPaletteOverlay.java` — semitransparent collapsible overlay and brush controls.
5. `screen/ScreenEditorActivity.java` — header, orientation, SAF opening/saving, undo/redo, dirty prompts and project entry points.
6. `screen/ScreenEditHistory.java` and `screen/ScreenEditorSession.java` only if useful to separate stroke-diff history and recovery from Activity.

Modified:
- `project/SpectrumScreen.java`: share raster decoder/color constants; no change to 0.37.0 preview behavior.
- `project/ProjectSettingsDialog.java`: edit button and callback.
- `MainActivity.java` and/or `ToolsActivity.java`: editor entry points and return handling.
- `app/src/main/AndroidManifest.xml`: non-exported ScreenEditorActivity + orientation/insets support.

No new third-party libraries; prefer existing Android APIs and local unit-testable Java.

## 12. Implementation phases for Codex

Phase A — data and format:
- Screen document byte-accurate model; editor-specific palette semantics; ASM codec; tests for all byte offsets, 8x8 cells, BRIGHT/FLASH, untouched round-trips, malformed input.
- Commit with tests green before UI changes.

Phase B — view and drawing:
- Full-screen Activity and canvas, landscape/portrait toggle, lock, pinch/pan, stroke interpolation, viewport persistence, upper-right collapsible palette.
- Ensure no accidental touch painting during a pinch. Test math with pure-Java coordinate helpers.

Phase C — editing safety:
- Stroke-level undo/redo, dirty prompts, orientation/activity restoration, Save/Save As via SAF, 6912-byte read/write validation and reopen test.

Phase D — project integration:
- Project settings Edit action; menu entry; dirty save -> rebuild path; correct relative file resolution; error handling on read-only or missing files.
- Update README and example assets description, bump app version to 0.38.0, GitHub Actions tests/build, provide debug APK.

Do **not** postpone the core requested features to later phases of the same release. Defer only optional selection/copy/paste, flood fill, arbitrary RGB/PNG import, animation, attribute remapping across regions, and complex ASM/include/macro extraction.

## 13. Acceptance tests / definition of done

1. Open arbitrary valid 6912-byte SCR, save without edits and compare SHA-256 or byte-by-byte: identical.
2. Paint pixel (0,0), (255,191), and a pixel across each of the three unusual bitmap thirds; byte offsets and bit masks are correct.
3. Change INK in one 8×8 tile; adjacent tiles and PAPER/BRIGHT/FLASH bits remain unchanged. Changing BRIGHT/FLASH respects the requested bit selection.
4. INK paint sets 1 bit, PAPER paint sets 0 bit. Whole-tile attribute paint never changes any of 6144 bitmap bytes.
5. One long stroke creates exactly one Undo operation and Redo restores the same raw bytes.
6. In Lock mode, one-finger pan and two-finger pinch do not change bytes. In Draw mode, two-finger gestures also never paint.
7. Landscape toggling and rotating back retain document edits, zoom position, lock, palette state and dirty flag.
8. Expanded overlay remains touchable and legible in top-right; collapsed overlay exposes canvas space.
9. Save .scr/.bin returns 6912 bytes; Save As preserves existing file until explicit confirmation; cancel and write errors do not lose changes.
10. ASM exported by editor reimports to exactly the same bytes; unsupported ASM rejects with a source line and does not replace current document.
11. From Project settings open assets/loading.scr, edit/save, exit, build TAP; SCREEN$ CODE block contains the new bytes.
12. Existing v0.37.0 projects, BASIC SCREEN$ TAP loader, sprite editor and code editor keep working.
13. App compiles with project Gradle CI unit tests and debug APK; all Java files compile with minSdk 23 and no AndroidX.
14. Run at least one manual real-device test for toolbar touch targets, system insets, Android document provider and orientation. Record any untested behavior honestly.

## 14. Codex execution guidance

Work in small coherent commits, keep Java classes readable, avoid monolithic Activity methods. Run `./gradlew testDebugUnitTest assembleDebug` (or repository's GitHub Actions workflow) at the end of each phase. Do not overwrite any user ASM file on export/import, and do not silently lose unsaved SCR changes.

For version v0.38.0, deliver:
- committed implementation in the repository,
- README section including supported SCR/BIN/ASM limitations,
- automated tests,
- a successful CI run and its debug APK artifact,
- a concise changelog and remaining limitations.
