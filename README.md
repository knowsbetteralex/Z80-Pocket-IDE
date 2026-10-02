# Z80 Pocket IDE

A small Android IDE for writing, checking, assembling and running Z80 code, with ZX Spectrum-oriented output formats.

## Current development build (v0.4)

- Pure Java Android app, no AndroidX/Jetpack dependency.
- Mobile-safe layout that respects status/navigation bar insets.
- Live Z80 syntax highlighting for mnemonics, registers, directives, labels, numbers, strings and comments.
- Searchable built-in Z80 reference with syntax, opcode patterns, T-states, flag notes and short descriptions.
- Reference opens pre-filtered when the cursor is on a mnemonic such as `LD`, `JR` or `LDIR`.
- Light ZX Spectrum-inspired branding: six-color stripe accent and matching app icon.
- Two-pass assembler core independent of Android.
- Broad documented Z80 instruction coverage, including base, CB/ED and documented IX/IY indexed forms.
- Assembler directives: `ORG`, `EQU`, `DB`, `DW`, `DS`.
- Integer expressions with symbols and numeric forms such as `$FFFF`, `#FFFF`, `%1010`, `1010b`, `FFFFh`.
- Forward label resolution and line-numbered diagnostics.
- ZX Spectrum TAP output with generated BASIC autorun loader.
- `Run` exports a temporary TAP through an Android content URI and opens the system app chooser.
- Built-in visual examples for border cycling, attribute colors and bitmap stripes.
- GitHub Actions runs tests, builds a debug APK and uploads it as an artifact.

The opcode implementation targets the documented Z80 instruction set. Undocumented IXH/IYH forms and undocumented DDCB/FDCB register-result variants remain outside the current scope.

## Next milestones

1. Line-number gutter and current-line highlight.
2. Autocomplete / quick instruction insertion.
3. Live non-destructive diagnostics before Build.
4. `INCLUDE`, `INCBIN`, project format and multiple source files.
5. Symbols panel, memory map and cycle analysis.
6. Emulator-specific launch adapters where generic `.tap` intents are insufficient.
7. Optional undocumented-instruction compatibility mode.

## Build

Requires JDK 17 and Android SDK 35.

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
