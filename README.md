# Z80 Pocket IDE

A small Android IDE for writing, checking, assembling and running Z80 code, with ZX Spectrum-oriented output formats.

## Current development build (v0.20.1)

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
- Compact top bar with Run, Open ASM folder, Save, Undo/Redo, Find/Replace magnifier, ? reference, wrench workshop, TAB and RU/EN. Quick actions scroll horizontally on narrow phones; the rightmost main-menu button stays pinned. Editor settings appear as the last top-level menu item.
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

## v0.18.0 Spectrum workshop

- Wrench icon opens one **Tools** screen with tabs: Number converter / Screen addresses / Sprite painter.
- DEC / HEX / BIN converter remains available with live bidirectional updates and Clear.
- Screen-address calculator shows a read-only, derived third (0..2). Its editable character row is now global (0..23), with pixel line (0..7), byte column (0..31), attribute row (0..23), and bitmap/attribute HEX and BIN addresses linked both ways. Bit groups `010 TT LLL RRR CCCCC` are colored by field.
- Sprite painter: 8..64 px sizes in steps of 8, one-finger tap to fill/erase and drag strokes, two-finger zoom and pan, Fit to Height (sprite may extend outside the canvas), and separate globally applied INK/PAPER palette choices. Uses one shared hardware BRIGHT bit per 8x8 tile; changing normal/bright shade affects both colours.
- Sprite bitmap and attribute bytes are previewed and can be inserted into the current editor at the cursor, in HEX (`DB $xx`) or BIN (`DB %xxxxxxxx`) format. Normal editor Undo works after insertion.
- Sprite bitmap export is row-major (a portable sprite format), not interleaved screen RAM order; attributes export in character-cell order.

## v0.18.1 drawing and address UX

- The third (TT) is informational and non-editable. Editing the global character row 0..23 or any screen/attribute address recomputes the third.
- One finger draws with INK on empty pixels and erases on filled pixels; dragging applies the initial draw/erase action continuously.
- INK and PAPER are selected separately from the 16-colour Spectrum palette. Choosing a colour instantly recolours all existing drawn pixels or their paper background without changing bitmap bits. The hardware BRIGHT flag is shared.
- Pan and zoom are two-finger gestures only; Fit scales to the maximum usable canvas height even when width overflows. The thicker semi-transparent pixel/cell grid remains visible on painted regions.
- Clearing pixels preserves current INK/PAPER colours. Binary and HEX DB export remains unchanged.

## v0.19.0 editor Find / Replace

- File Open is now a green folder icon on the toolbar, alongside the centered Save diskette.
- Top-level menu includes **Edit / Правка** (formerly Tools): Find / Replace, formatting, uppercase source, Spectrum workshop and TAP emulator choice. Folding and Clear Selection commands are removed from this submenu; their gutter controls remain.
- **Edit → Find / Replace** opens a small find-and-replace panel beneath tabs. Search uses literal text with wrap-around previous/next navigation, match counter and optional case sensitivity.
- All matching occurrences are highlighted in source (capped at 500 visible highlights for large files); the active match is emphasized and scrolled into view.
- Replace one and Replace All support empty replacement strings; both respect current-case setting, with Replace All recorded as one Undo step.
- Visual folds are expanded when starting a search so offsets match actual assembler source; closing search removes highlights.

## v0.20.0 durable workspace and Undo recovery

- Reopen the exact set of tabs, active tab, file URI, cursor and scroll position after force-close, normal exit or Android process recreation.
- Every open tab keeps a private on-device draft copy of its source, including files with no saved .asm path. Autosave occurs after about 650 ms idle; onPause flushes synchronously. Drafts never silently overwrite original ASM files.
- Autosave writes a versioned CRC-checked snapshot through Android AtomicFile to internal `noBackupFilesDir`, so interrupted writes preserve the previous complete snapshot.
- The last **100 grouped edits per tab** are serialized, including both Undo and Redo branches, the position in history and the last explicit Save checkpoint. Normal typing is not delayed by immediate disk I/O.
- Intentional tab closure removes the tab from next launch; closing the whole application keeps dirty drafts. The exit prompt explains that these drafts are locally recoverable.
- Session files are kept only in the app's private device storage (not exported or included in Android cloud backups); clearing app data or uninstalling the app deletes them. A process killed before the current debounced autosave or lifecycle flush may still lose the last fraction of a second of edits.

## v0.20.1 compact search and status

- Find/Replace is accessible directly from a magnifier icon on the scrolling quick-actions bar; tapping it while search is open closes it.
- Find/Replace panel uses only two 38dp rows with tight margins and paddings to keep source visible above the keyboard. The match counter now shares the search row.
- Match-case is controlled by a compact **Aa** button that changes color when enabled, instead of a full-width checkbox.
- The expanded bottom status now shows diagnostics and byte/T-state statistics without duplicating the last action message already shown on the collapsed status row.
