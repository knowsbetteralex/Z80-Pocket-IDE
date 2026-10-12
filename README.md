# Z80 Pocket IDE

A small Android IDE for writing, checking, assembling and running Z80 code, with ZX Spectrum-oriented output formats.

## Current development build (v0.17.0)

- Pure Java Android app, no AndroidX/Jetpack dependency.
- Mobile-safe UI that respects Android system bars.
- English / Russian language switch with the selected language persisted between launches.
- Localized interface, statuses, notifications, examples and Z80 reference.
- Live Z80 syntax highlighting for mnemonics, registers, directives, numbers, labels, strings and comments.
- Recognize defined label references, including forward references, inside instruction operands; skip comments and string literals.
- Red underline for the offending source line after a failed live assembler check.
- Optional uppercase-all typing mode and explicit uppercase-entire-source menu command (including labels, comments and string data).
- Keyboard-aware editor resizing and caret-follow scrolling on Android 6+.
- Live optional uppercase formatting for Z80 mnemonics/registers/directives without touching labels, comments or strings.
- Built-in live DEC / HEX / BIN converter with assembler-style prefixes and a one-tap Clear action.
- Editor line-number gutter and current-line highlighting, with tap/drag line selection.
- Byte size and static minimum/maximum Z80 T-states for the whole source or multiple selected lines.
- Inline current-instruction byte and T-state hints; unresolved source temporarily displays no stale counts.
- Compact header controls: build-and-run, centered green diskette, Undo/Redo, ? reference, number converter, TAB, RU/EN and rightmost menu. Editor settings appear as the last top-level menu item.
- New / Open / Save / Save As / Close / Build / Save TAP actions live under File.
- Single slim status row expands on tap into diagnostics and byte/T-state statistics.
- Best-effort multiple assembler errors in a scrollable diagnostics panel.
- Configurable auto-indent, TAB width (2/4/8), spaces-vs-tab and an on-screen TAB key.
- One-tap document formatter.
- Fold/unfold chevrons on label lines within the number gutter, separate from tap/drag line selection. Builds and saves use the full source.
- Touch-oriented code editor: tap moves the caret, one-finger drag pans the code canvas, pinch changes and remembers font size.
- Searchable built-in Z80 reference with syntax, opcode patterns, T-states, flags and descriptions.
- Reference screen now has separate Z80 / ZX 48K / ROM / I/O tabs.
- ZX 48K reference includes a drawn 256×192 screen-memory map with thirds and bit-field addressing.
- I/O reference covers ULA port $FE, the keyboard matrix, Kempston $1F, EAR/MIC/beeper and AY $FFFD/$BFFD.
- ZX 48K quick reference covers memory map, bitmap thirds, `010 TT LLL RRR CCCCC` pixel-address bit layout, attributes and useful system variables.
- ROM quick reference lists practical standard-48K entry points with addresses and register contracts.
- Russian reference search understands localized terms as well as technical mnemonics/opcodes.
- Tabbed source workspace with a horizontal tab strip, dirty markers and per-tab build state.
- New / Open / Save / Save As / Close source workflow through Android Storage Access Framework.
- Source files use normal `.asm` text files and can live in Downloads, cloud providers or any Android document provider.
- Closing a modified tab asks whether to save, discard or cancel; long-pressing a tab opens quick Save / Save As / Close actions.
- Built-in examples always open in a new tab instead of replacing current work.
- Two-pass assembler core independent of Android.
- Table/family-driven Z80 encoder with broad documented instruction coverage, including CB/ED groups and documented IX/IY forms.
- Assembler directives: `ORG`, `EQU`, `DB`, `DW`, `DS`.
- Integer expressions, labels and line-numbered diagnostics.
- ZX Spectrum TAP generation with BASIC autorun loader.
- Build / Run / Save `.tap` workflow.
- Optional remembered Android app for one-tap automatic TAP launch, with chooser fallback.
- Twenty-five built-in ZX Spectrum items split into separate Examples and Routines tabs, with localized explanations and commented/uncommented source variants.
- Example/routine detail pages can build and launch a temporary autorun TAP directly, without opening an editor tab.
- Reusable routine examples include explicit IN / OUT / DESTROYS register contracts.
- Spectrum-inspired branding and app icon.
- Rounded green action styling with explicit grey disabled Build-dependent actions.
- JUnit tests and GitHub Actions debug APK builds.

The opcode implementation is based on the documented Z80 instruction set. Undocumented instructions (for example IXH/IYH forms and undocumented DDCB/FDCB register-result variants) are intentionally outside the current scope.

## Source tabs and files

Use **☰ → Files** to create a new tab, open an existing source, save, save under a new name, or close the active tab. A `●` before the tab title means that the source has unsaved changes. The small `×` closes a tab; modified tabs show a Save / Don't save / Cancel confirmation.

Opening an example creates a separate, initially clean tab. Once an example is edited it becomes dirty like any other source and can be saved as a regular `.asm` file.

Each tab keeps its own in-memory build result, so Build/Run state cannot leak from one source tab to another.

## Language

Use the `EN` / `RU` button in the app header. On first launch the app follows the device language when it is Russian; otherwise it defaults to English. The choice is then stored locally.

## Next milestones

1. Autocomplete for mnemonics, registers and labels.
2. Inline instruction signature/T-state hints.
3. Symbols panel and memory map.
4. `INCLUDE`, `INCBIN` and multi-file project build rules.
5. Session/project restore across full app restarts.
6. Cycle analysis and timing tools.
7. Optional compatibility/undocumented-instruction mode.

## Build

Requires JDK 17 and Android SDK 35.

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## v0.16.1 fixes

- Save ASM from the top-bar diskette shortcut; first save opens Android's file picker.
- Run is active when an unsaved/never-built source tab opens; it auto-builds before launching the emulator.
- A green × at the bottom of the line-number gutter clears selected lines without affecting the cursor or code.
- Creating ASM files uses assembly-source MIME instead of text/plain to avoid .asm.txt; tries Android document-provider rename when necessary, and reports providers that refuse renaming.

## v0.16.2 editor responsiveness

- Complete syntax highlighting starts only 550 ms after typing pauses; it no longer repositions the caret.
- Automatic assembler validation starts 720 ms after typing pauses, runs on a background worker, and ignores stale results after edits or tab switches.
- Editing no longer snapshots entire source on each keystroke. Source buffers are captured before saving, building, switching tabs and preserving Activity state.
- Reversible gutter folds do not mark a source as edited or restart background analysis.

## v0.17.0 undo and redo

- Dedicated Undo and Redo buttons on the top toolbar, disabled when unavailable.
- Incremental per-document edit history, including automatic keyword capitalization and whole-document formatting.
- Fast adjacent keystrokes and Backspace actions group into a single undoable change. New edits after Undo clear redo history.
- Independent history for each source tab. The Save/Save As operation records an undo checkpoint, letting undo back to the saved state clear the dirty marker.
- Source fold/unfold does not create undo entries; image-centered save button fixes left alignment.
- Undo history is kept during tab switching; restarting the Activity currently initializes a new history.
