# Z80 Pocket IDE

A small Android IDE for writing, checking, assembling and running Z80 code, with ZX Spectrum-oriented output formats.

## Current development build (v0.5)

- Pure Java Android app, no AndroidX/Jetpack dependency.
- Mobile-safe UI that respects Android system bars.
- English / Russian language switch with the selected language persisted between launches.
- Localized interface, statuses, notifications, examples and Z80 reference.
- Live Z80 syntax highlighting for mnemonics, registers, directives, numbers, labels, strings and comments.
- Searchable built-in Z80 reference with syntax, opcode patterns, T-states, flags and descriptions.
- Russian reference search understands localized terms as well as technical mnemonics/opcodes.
- Two-pass assembler core independent of Android.
- Table/family-driven Z80 encoder with broad documented instruction coverage, including CB/ED groups and documented IX/IY forms.
- Assembler directives: `ORG`, `EQU`, `DB`, `DW`, `DS`.
- Integer expressions, labels and line-numbered diagnostics.
- ZX Spectrum TAP generation with BASIC autorun loader.
- Build / Run / Save `.tap` workflow.
- Android Intent launch into installed apps that can open TAP files.
- Built-in visual ZX Spectrum example programs.
- Spectrum-inspired branding and app icon.
- JUnit tests and GitHub Actions debug APK builds.

The opcode implementation is based on the documented Z80 instruction set. Undocumented instructions (for example IXH/IYH forms and undocumented DDCB/FDCB register-result variants) are intentionally outside the current scope.

## Language

Use the `EN` / `RU` button in the app header. On first launch the app follows the device language when it is Russian; otherwise it defaults to English. The choice is then stored locally.

## Next milestones

1. Line numbers and current-line highlighting.
2. Autocomplete for mnemonics, registers and labels.
3. Inline instruction signature/T-state hints.
4. Symbols panel and memory map.
5. `INCLUDE`, `INCBIN`, project format and multiple source files.
6. Cycle analysis and timing tools.
7. Optional compatibility/undocumented-instruction mode.

## Build

Requires JDK 17 and Android SDK 35.

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
