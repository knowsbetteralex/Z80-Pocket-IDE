# Z80 Pocket IDE

A small Android IDE for writing, checking, assembling and running Z80 code, with ZX Spectrum-oriented output formats.

## Current bootstrap (v0.1-dev)

- Pure Java Android app, no AndroidX/Jetpack dependency.
- Minimal source editor and Build button.
- Two-pass bootstrap assembler with labels and `ORG`.
- Initial instructions/directives: `LD A,n`, `OUT (254),A`, `JP`, `JR`, `NOP`, `HALT`, `RET`, `DB`, `DW`.
- Numeric forms: decimal, `$FFFF`, `#FFFF`, `%1010`, `FFFFh`.
- Minimal ZX Spectrum TAP CODE writer.
- JUnit tests for absolute and relative label resolution.
- GitHub Actions workflow that runs tests, builds a debug APK and uploads it as an artifact.

## Direction

The assembler core is intentionally independent of Android. The next step is to replace the bootstrap encoder with a declarative table covering the full documented Z80 instruction set and timing metadata.

Planned milestones:

1. Full Z80 instruction table and operand parser.
2. `EQU`, expressions, `DS`, `INCLUDE`, `INCBIN`.
3. Syntax highlighting, diagnostics, symbol navigation and autocomplete.
4. Built-in Z80 reference with bytes, flags and T-states.
5. TAP with generated BASIC loader and autorun.
6. Export/share and Android Intent launch into an installed emulator.
7. Project format, multiple source files, memory map and cycle analysis.

## Build

Requires JDK 17 and Android SDK 35.

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
