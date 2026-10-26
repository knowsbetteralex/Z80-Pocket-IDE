# Z80 Pocket IDE

A small Android IDE for writing, checking, assembling and running Z80 code, with ZX Spectrum-oriented output formats.

## Current development build (v0.27.1)

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
- Compact top bar with Open ASM folder, Save, Undo/Redo, Find/Replace magnifier, ? reference, wrench workshop, TAB and RU/EN. Quick actions scroll horizontally on narrow phones; the rightmost main-menu button stays pinned. Editor settings appear as the last top-level menu item.
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

## v0.21.0 Sprite mask, shift and editing session

- Separate Sprite / Mask editing layers. Mask starts as all ones; a tap erases a bit to a black-and-white checkerboard, and the reverse gesture restores the underlying INK/PAPER pixel. Drag strokes work as on the sprite.
- Mask data gets its own HEX/BIN DB preview and Insert mask DB button. Mask convention is 1 = visible, 0 = transparent. Invert mask polarity when using classic AND-clear blitters.
- A 64x64 backing store retains the full bitmap and mask. Resizing the viewport between 8 and 64 pixels (steps of 8) never discards any hidden data.
- Arrow controls shift the drawing 1 pixel at a time, showing X/Y offsets. Pixels hidden by the viewport or backing field are clipped, not destroyed; returning restores them. Off-source areas export as zero bits.
- Two-finger pan/zoom changes the display, separately from the exported content offsets. Fit to height is retained.
- Closing/reopening ToolsActivity while the IDE process runs retains pixel/mask layers, canvas size, offsets, palette choice, selected layer and BIN/HEX format via a process-local SpriteSession. This is not written to the persistent ASM workspace; finishing the IDE or process death clears the session.
- Tests cover mask export, clipping, lossless resize/shift and in-process retention.

## v0.21.1 stable sprite scrolling and DB import

- Sprite painting, palette changes, shift controls and live DB preview updates no longer cause the Tools page to jump down: the Sprite tab's ScrollView ignores automatic focus-based scroll requests while still allowing manual scrolling and pinch gestures.
- Each tool tab remembers its current vertical scroll position when changing tabs.
- Each Sprite/Mask/Attributes DB preview now has **Load DB** (import) alongside Insert DB into ASM (export). Paste an entire `DB` / `DEFB` listing in HEX, BIN or decimal; labels, simple byte dumps and `;` comments are supported.
- The dialog checks the byte count for the selected canvas and validates all bytes before changing the image; invalid imports leave the existing sprite untouched and the dialog open for correction.
- Selecting a block of ASM text before opening Tools pre-fills import dialogs with that text, or the normal Android paste action can be used.
- Imported bitmap and mask data update only the active visible canvas area, leaving clipped data intact. Imported attributes preserve independent cell bytes, including INK, PAPER, BRIGHT and FLASH; palette edits recolor cells again.
- Added round-trip import/export, parser, validation and per-cell color tests.


## v0.22.0 edit controls and Run placement

- The sprite canvas has a top-right padlock overlay. It is **locked by default**, disabling painting while allowing one-finger panning and two-finger pan/pinch zoom. Unlocking restores drawing. Lock state stays in the in-memory sprite session and resets to locked on app restart.
- Bitmap, mask and 8×8 attribute DB previews have a pencil icon at the upper right. It opens an **inline text editor** with **✓ OK** and **✕ Cancel**; validation and conversion occur only when OK is pressed. Invalid byte counts/values leave the editor open and the sprite unchanged; Cancel discards edits.
- Existing separate Load DB and Insert DB buttons remain usable. The inline editor uses the same HEX/BIN/decimal DB parser and updates the canvas and all previews on commit.
- The Tools workshop remembers the last selected tab (number converter, addresses or sprite) in app preferences and returns to it on the next opening, including after process restarts. Inline DB fields can request scrolling to expose the keyboard; passive DB preview redraws still cannot force page scroll.
- Run/Build moves from the header to a circular green ▶ button floating at the bottom-right of the **source editor**. It remains available with the keyboard shown, preserves automatic Build & Run and leaves the menu's existing Run action intact.

## v0.22.1 Run button overlay fix

- Removed the 69dp full-width bottom padding added for the Run FAB, restoring the normal 12dp editor inset. Only the circular floating play button overlays source text; no blank full-width strip is reserved.
- The floating Run FAB stays at bottom-right of the editor and retains Build & Run behavior.

## v0.23.0 touch scrolling and top-level fold actions

- The editor has a dedicated **42dp-wide touch scrollbar** over the right edge. Its 30dp thumb is draggable, becomes prominent during scrolling and dims after 1.8 seconds of inactivity; it remains faintly visible while idle. The original thin Android scroll indicator is disabled.
- **▲ / ▼ buttons** at either end scroll about 15% of the visible code height per tap. Holding an arrow repeats after 330 ms, then about every 95 ms. Dragging the thumb maps directly to the actual scroll range, without moving the caret.
- The scrollbar ends **87dp above the editor bottom**, so it never covers the floating Run button. No full-width bar or blank padding is introduced.
- A pinned fold/unfold-all icon sits to the right of the horizontal file tabs. It folds only the **first label in each blank-line-separated paragraph**, treating any subsequent labels in that paragraph as nested under the first. Tapping again restores the entire source.
- Folding is a visual presentation operation: assembler source, saves and Undo history are not changed. Existing per-label gutter folding remains available.
- Added host-side tests for paragraph boundaries, nested labels, label comments and EOF without trailing newline.

## v0.23.1 transient touch scrollbar

- The wide scrollbar, including thumb and arrow buttons, is **fully invisible** until a real finger-driven vertical scroll begins in the code editor.
- After 1.8 seconds without further finger scroll the whole rail disappears; it no longer leaves a faint track overlay on the code or intercepts input while hidden.
- Dragging the exposed thumb or holding its arrow buttons keeps it visible until release, then starts the same hide delay.
- Scrolling caused by caret movement, restoring source position, tab changes, layout, folding or other programmatic actions only updates rail geometry and never reveals it.
- The rail remains clear of the floating Run button and does not allocate an extra full-width strip.

## v0.24.0 visual Spectrum screen address picker and Fit

- Sprite editor's former **Fit to height** action is now simply **Fit** (Вписать), calculating the uniform pixel scale from the limiting width/height so the entire current sprite is visible, centered with ~12dp side margins. Resizing the sprite canvas uses the same operation and retains the underlying pixels and mask.
- Screen Address tool now features a fully linked **32×24 character screen map** for the ZX Spectrum 256×192 bitmap. Tap or drag over a character square to choose its row and byte column; thick horizontal guides separate the three 64-line display thirds.
- A larger **8×8 pixel view** below the whole-screen grid picks pixel scanline LLL (0..7) and bit (0..7). The selected scanline updates the real bitmap address. The selected bit shows its byte mask ($80..$01) and absolute X/Y pixel position, but does not change the bitmap byte address.
- The screen map and 8×8 selector react immediately to numeric coordinate fields and the HEX/BIN bitmap/attribute address inputs, and screen touches update those existing fields using ScreenAddress, including third/row interleaving.
- Unit tests verify all three display thirds, pointer mapping, bit-mask endpoints and coordinate-to-address consistency.

## v0.25.0 ZX Spectrum keyboard worksheet

- Added a fourth **Keyboard / Клавиши** workshop tab with a drawn four-row 40-key ZX Spectrum membrane keyboard. Keys can be tapped to toggle simultaneous presses. Selected keys are highlighted in amber.
- For each selected half-row, shows the exact port address ($FEFE, $FDFE, $FBFE, $F7FE, $EFFE, $DFFE, $BFFE, $7FFE), read byte in HEX and BIN, and the pressed key names. Tap a readout row to focus its values in the decoder.
- Reverse lookup: edit a half-row port in HEX and a read byte in HEX or eight-bit BIN; the selected physical keys update immediately. The reverse lookup replaces previous pressed selections, while tapping the keyboard can build multi-half-row key combinations.
- Bits D0..D4 are active-low, 0 = pressed. The displayed high bits are normalized to ones; the real D6 EAR line may vary. Loading a byte ignores D5..D7 for key selection.
- Button **Insert Z80 reading snippet** inserts LD BC,port / IN A,(C) and optionally BIT n,A (for the first selected key in the focused half-row). Selection survives reopening the workshop during the current Android process.
- Added JVM unit tests for all 40 key positions, 8 ports, key combinations, reverse decoding, binary/hex input and Z80 snippet creation.

## v0.26.0 expanded Z80 instruction reference

- Z80 reference has a **two-level UI**: its existing quick card lists syntax alternatives, opcode patterns, a short description, affected flags and the approximate T-state counts; the new Show details/Подробности button opens per-form explanations and folds them back without leaving search or changing its scroll position.
- Detailed entries cover **all existing 60 instruction/directive families** (register, immediate, (HL), indexed (IX/IY+d), memory, conditional branch and repeat forms as applicable), including additional common 16-bit LD, stack and interrupt forms. Each variant gets its own T-state value or taken/not-taken/iteration distinction and a short explanation in English/Russian.
- Every expanded form displays the individual **S, Z, H, P/V, N, C flag outcomes**, distinguishing carry vs borrow, parity vs signed overflow, fixed 0/1, preservation and special instructions (BIT, DAA, NEG, SCF/CCF, block transfer/search/I/O, IN r,(C), LD A,I/R, POP AF and EX AF,AF'). Unofficial F3/F5 bits are identified separately rather than incorrectly conflated with the six documented flags.
- Expanded card state survives search rerenders and device rotation within the Reference Activity. The other ZX/ROM/I/O reference tabs remain unchanged.
- Expanded T-state counts assume the standard un-contended Z80; Spectrum-specific RAM contention delays are not part of these instruction counts.
- Added JVM coverage tests for catalog completeness, special flags, and tricky indexed/conditional timing paths.

## v0.27.0 Android 'Open with' integration

- The IDE now appears as a target for Android **Open with / Открыть с помощью** when a file manager or document provider sends an ACTION_VIEW or ACTION_EDIT for assembly/text documents. Manifest supports content:// and file:// URIs and common .asm MIME declarations: text/x-asm, application/x-asm, text/plain and application/octet-stream.
- External files use the existing ContentResolver source loader, opening in a new source tab while preserving all unsaved editor buffers and undo history. Opening an already-loaded document URI switches to that tab rather than duplicating it.
- MainActivity now uses singleTask and handles onNewIntent, allowing additional files to be opened by the system chooser into the running IDE instead of creating extra IDE activities.
- The original external launch URI is not replayed after device rotation, and files without persistent grants remain readable as drafts even if the provider stops granting the original URI. Save as remains available for read-only files.
- The chooser is MIME-based, so Android may list the IDE for other plain-text files if their provider assigns text/plain; Android content:// document IDs do not always expose filename extensions for reliable filtering.
- Added tests for external document action/scheme validation and AndroidManifest Open with declarations.

## v0.26.2 writable external ASM documents and folded label highlighting

- **Open with** now keeps the external file's original URI as its save target. Normal Save overwrites the same document whenever the launching file manager has granted write access.
- If an external provider supplied only a read grant or rejects writing, the editor offers **Select original file**, using Android ACTION_OPEN_DOCUMENT to obtain writable access to the existing file. After choosing the same filename, the pending edits are saved there automatically and the new URI replaces the previous read-only URI for future normal saves. No duplicate document or Save As is needed. Providers that never allow editing cannot be forced to overwrite a protected file.
- Write-grant fallback persists granted read/write URI rights when Android's document provider allows it; on navigation/rotation the pending request is restored. A different filename is rejected rather than overwritten.
- Folded code retains turquoise symbol highlighting for visible references to labels whose declarations are hidden inside folded blocks. The highlighter builds the symbol table from the logical (unfolded) source and applies spans only to visible text; the saved/generated source remains unchanged.
- Added pure JVM regression tests for folded forward/nested references and safe writable URI reselection.

## v0.27.1 smoother typing and error-line navigation

- Removed an O(visible lines × source length) UI hot path in editor gutter rendering. Fold markers now inspect the EditText buffer without making a full copy of the document for each painted line. Autocapitalization and newline indent also stop copying the whole file on every keystroke.
- Z80 syntax tokenization runs on a dedicated worker **after a 950 ms pause in typing**, and only the visible section plus a small margin receives spans. Viewport recoloring follows scrolling with a short debounce. This prevents tens of thousands of spans from shifting when editing at the beginning of large sources.
- Rapid typing no longer repeatedly clears already-empty assembler diagnostics/instruction metrics or re-renders unchanged status/statistics text. Live assembly is still debounced in its existing background executor.
- If an assembly diagnostic contains a line number, the compact bottom status shows a distinct underlined **↗ line N / ↗ стр. N** link. Tap it to focus and scroll to the actual source line, unfolding visual label blocks first so the assembler's source-line numbering stays correct. The first navigable error is linked when multiple issues exist.
- Added JVM tests for syntax token windows, original absolute offsets, register/mnemonic coloring and references to labels declared inside collapsed sections.
