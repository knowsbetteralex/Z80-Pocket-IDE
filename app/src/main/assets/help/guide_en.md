# Z80 Pocket IDE User Guide

A practical overview of the main app features. Choose a chapter from the table of contents. The Z80 instruction and ZX Spectrum hardware reference is available separately in the main menu.

## 1. Getting started and navigation

On a fresh installation the editor starts **without open tabs**. Tap the large central **+** button to create an ASM file, or use **☰ → File → Open file**. Previously opened tabs are restored when an existing workspace is available.

The top toolbar offers quick access to Open, Save, Undo/Redo, Find/Replace, Tools and the **?** technical reference. It scrolls horizontally on small screens. The pinned **☰** opens the main menu.

The menu groups commands into **File**, **Project**, **Edit**, **Build**, **Tools** and **Settings**. Separate entries open the **User guide** (this guide) and **Z80 / Spectrum reference** (technical instruction and hardware data).

Source tabs appear above the editor. The **INC** button on their right opens included sources; it highlights green only when the cursor is on a valid INCLUDE line. The floating round ▶ button builds and runs a TAP.

The bottom status strip displays file and build information. Tap it to expand errors, byte sizes and T-state statistics.

## 2. Files, tabs and saving

Select **☰ → File → New file** for a blank tab. **Open file** uses the Android system document picker to open an ASM source. Multiple files can be opened in tabs; tap a tab to switch, × to close, or long-press it for save/close actions.

**Save** writes changes back to an existing document; for a new tab it opens Android's save dialog. **Save as** creates a separately named file or saves to another location. A ● marker on a tab indicates unsaved edits. Closing a modified tab asks whether to save or discard them.

The IDE checks open files for external changes on return and about every 12 seconds while running. Use **File → Check external file changes** to check immediately. Clean tabs reload automatically; tabs with unsaved edits are marked ↻ and let you reload, save a local copy, or keep your edits. Overwriting an externally changed source requires confirmation.

The IDE maintains local **drafts, cursor/scroll positions and Undo/Redo history** for recovery after restarting. This is not a substitute for saving your source to disk: uninstalling the app or clearing its data also removes internal drafts.

You can open an ASM file using Android **Open with** when the file manager provides a supported file type. If a provider grants read-only access, the IDE can ask you to select the same file again for write permission.

## 3. Source editor and gestures

The source editor includes **Z80 syntax highlighting**, line numbers, current-line emphasis, auto-indent, label-name suggestions and Undo/Redo. Tap a line number to select the line; drag along the gutter to select several. The × button at the bottom of the gutter clears line selection.

A **one-finger drag** pans the code for reading without moving the caret. Tap to position the caret precisely. **Pinch with two fingers** to change font size; the scale is remembered.

A compact strip above the Android keyboard provides TAB, frequently used characters (quotes, ;, :, #, $, parentheses) and label suggestions. The TAB action inserts a tab character or spaces according to editor settings.

The editor can display the current instruction's **byte count and T-states**. Totals for selected lines or the full source are available in the expanded status panel.

## 4. Find, replace and formatting

Tap the magnifying-glass icon or select **☰ → Edit → Find / Replace**. Enter a search string, use previous/next controls to navigate results, and toggle **Aa** for case-sensitive matching. You can replace the current match or all matches.

**Edit → Format source** applies the editor's source formatting rules. **Edit → UPPERCASE source** converts all text, including labels, strings and comments; use carefully.

Assembler diagnostics appear in the lower panel and on the source line when applicable. Tap the indicated line to navigate to an error in the active file.

## 5. Projects, INCLUDE and INCBIN

For multi-file work, choose **☰ → Project → Open project**, then authorize a directory through Android's system folder picker. The IDE can read project files from this selected folder.

**GitHub projects (experimental):** choose **☰ → Project → Open Git project**. First, create a fine-grained GitHub token with **Contents: Read and write** on the selected repositories, then paste it into the sign-in screen. The token is encrypted with Android Keystore. Choose a repository; Pocket IDE downloads a small working copy and opens the main ASM. **Project → Git · Sync (Pull + Push)** checks for changes and sends them with one tap when there are no conflicts. On a conflict, keep your file, use GitHub's version, or manually merge text. To add a source under the working folder, choose **Project → New file in Git project**.

This initial implementation supports the default branch and selected ASM/source resources only (up to 2 MiB/file, 400 files / 20 MiB/project). Unrelated repository assets are untouched. There is no local commit history or undo for remote pushes. Working files and unsent edits live in private app storage until synchronized; do not clear app data before pushing.

**Project → Main ASM: filename** opens a list of possible entry-point source files. **Project settings** controls the project name, main ASM and optional loading screen.

INCLUDE inserts another source file:

```asm
INCLUDE "lib/sprites.asm"
```

The tab-strip **INC** button and **File → Open INCLUDE** help you open an included file for editing. Relative INCLUDE paths are resolved from the ASM file containing the directive.

INCBIN inserts **raw bytes without conversion**:

```asm
SPRITE: INCBIN "assets/ship.bin"
INCBIN "assets/title.scr", 6144, 768
```

Optional offset and length arguments are in bytes. The resource path is relative to the ASM file containing INCBIN. Access to project assets requires permission for the project folder. Check diagnostics for invalid paths, lengths or size limits.

## 6. Assembler and first program

Z80 Pocket IDE includes a **two-pass Z80 assembler**. It supports CPU instructions, expressions, labels, core directives **ORG, EQU, DB/DEFB, DW, DS**, macros, conditional assembly and **ASSERT** checks.

A minimal program:

```asm
ORG $8000

START:
    LD A,2
    OUT ($FE),A
    RET
```

Choose **Build → Build project**. If successful, the status bar displays code size and ORG address, and an autorun TAP is ready to save or run.

You can split larger programs with INCLUDE, and check assumptions with ASSERT:

```asm
ASSERT END_CODE - START <= 16384, "Code too large"
```

Assembly errors indicate source lines. For errors in an included file, inspect both the path and the line number in diagnostics.

## 7. Build, TAP export and emulator

**☰ → Build → Build project** assembles the main source and prepares a TAP with a BASIC loader. **Build and run** assembles and sends the TAP to an installed Spectrum emulator. The floating round ▶ runs this same action.

**Build → Export 48K SNA** creates a 49,179-byte ZX Spectrum memory snapshot. Android's system file picker selects the location and name of the .sna file. Unlike TAP, SNA starts at **ORG** without a BASIC loader, embedding the project's configured SCREEN$ when enabled. The project loading-screen delay does not apply. Code must be in RAM $4000–$FFFF; it cannot overlap an enabled SCREEN$. The exporter sets SP and places PC on the stack. Programs must initialize any required system variables/hardware state themselves. The floating ▶ button shows a spinning progress indicator during compilation.

**Build → Export TAP** lets you pick a destination using the Android document picker. Running the TAP requires a **separately installed ZX Spectrum emulator** capable of opening TAP files.

If several emulators are installed, choose the preferred app in **☰ → Settings → TAP emulator**. When none can handle TAP, Android or the IDE may display an error.

On failed builds, expand the diagnostic panel, correct the source and try again. **Saving ASM and exporting TAP are separate operations**; creating a TAP does not automatically save your source text.

## 8. ZX Spectrum screen editor

Open **☰ → Tools → Screen editor** or use the **Screen** tab in the Z80 workshop. SCREEN$ is a **256×192-pixel** image consisting of **6144 bitmap bytes and 768 attribute bytes** (6912 in total).

The floating palette contains **Pen (INK), Eraser (PAPER), Picker**, palette colors and BRIGHT/FLASH controls. **Unlock** the canvas to paint; keep it locked to inspect or pan safely. Zoom/pan and Undo/Redo are available.

**Selections:** mark a rectangle, tap Copy, move to another location or screen, then Paste. Arrow buttons nudge by one pixel; **✓** commits and **×** cancels. **Shift attrs 8px** controls copying character-cell color attributes at complete 8-pixel steps.

Open or save screens as **.scr / .bin**, or export screen data as **.asm**, including RLE variants. Check resource sizes and save the file before leaving.

## 9. Tools, examples and technical reference

Use **☰ → Tools → Number / Address / Sprite tools** or the toolbar tool icon. The Z80 workshop has five tabs:

- **123** — convert values among DEC, HEX and BIN.
- **Addr** — calculate Spectrum bitmap and color attribute addresses.
- **Sprite** — draw sprites and masks, move pixel data, and import/export DB bytes.
- **Keys** — inspect the ZX Spectrum keyboard matrix and port-reading examples.
- **Screen** — an embedded SCREEN$ editor with full-screen access.

**Tools → Examples and routines** opens a browsable library. Read a description, run a sample directly, or open it in the source editor **with or without comments**.

The separate **Z80 / Spectrum reference** covers machine instructions, flags, T-states, memory, ROM and ZX 48K I/O ports. This user guide explains how to operate the IDE; the reference provides technical Z80 and Spectrum details.

## 10. Editor, language and project settings

Open **☰ → Settings → Editor settings** to control automatic uppercase typing, indentation, TAB size (2/4/8), spaces instead of tabs, the quick-symbol strip and label completion.

**Interface language: RU/EN** switches languages immediately and remembers your choice; there are no additional language packs to download.

**Project settings** can select a .scr/.bin loading screen and choose when the game starts: immediately, after a short delay, or on keypress. An enabled SCREEN$ is placed in the TAP before the game CODE. Project settings are stored in **.z80pocketide.json** where the document provider permits writing; otherwise the IDE keeps a local copy.

Use **Settings → TAP emulator** to choose the Android application used for TAP launch.

## 11. Troubleshooting and data safety

**No tabs on startup?** This is normal for a fresh or completely closed workspace. Tap the central +, or open a source file/project from the menu.

**INC is not highlighted?** Green highlighting appears only on a syntactically valid INCLUDE directive line. With a file open, the button works on other lines too.

**INCLUDE/INCBIN file not found?** Verify the relative path, filename spelling/case and Android folder access permissions.

**TAP will not run?** Install a compatible emulator and choose it under TAP emulator settings.

**Cannot save an ASM file?** Check document-provider write permissions. You may have to select the original file again.

**Where are drafts?** Drafts live in the app's private storage. They help recover a workspace but **do not replace separately saved ASM and SCR files**. Back up your projects before uninstalling, clearing app data or switching builds.
