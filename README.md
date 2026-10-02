# Z80 Pocket IDE

A small Android IDE for writing, checking, assembling and running Z80 code, with ZX Spectrum-oriented output formats.

## Current development build (v0.2-dev)

- Pure Java Android app, no AndroidX/Jetpack dependency.
- Minimal source editor and Build button.
- Two-pass assembler core independent of Android.
- Table/family-driven Z80 encoder instead of line-specific regular expressions.
- Broad documented Z80 instruction coverage, including:
  - 8-bit and 16-bit loads and arithmetic;
  - conditional/unconditional `JP`, `JR`, `CALL`, `RET`, `DJNZ`, `RST`;
  - stack, exchange, interrupt and I/O instructions;
  - `CB` rotate/shift/bit operations;
  - `ED` block/special instructions;
  - documented `IX` / `IY` indexed forms such as `(IX+d)` and `(IY+d)`.
- Assembler directives: `ORG`, `EQU`, `DB`, `DW`, `DS`.
- `DB` string literals and comma-aware parsing.
- Integer expressions with symbols, parentheses, `+ - * / % << >> & ^ | ~`.
- Numeric forms: decimal, `$FFFF`, `#FFFF`, `%1010`, `1010b`, `FFFFh`.
- Forward label resolution and line-numbered diagnostics.
- Minimal ZX Spectrum TAP CODE writer.
- JUnit tests for labels, expressions, directives, indexed code and prefixed opcodes.
- GitHub Actions runs tests, builds a debug APK and uploads it as an artifact.

The opcode implementation is based on the documented Z80 instruction set. Undocumented instructions (for example IXH/IYH forms and undocumented DDCB/FDCB register-result variants) are intentionally outside the current scope.

## Next milestones

1. Syntax highlighting and editor diagnostics while typing.
2. Built-in Z80 reference with bytes, flags and T-states.
3. TAP with generated BASIC loader and autorun.
4. Export/share plus Android Intent launch into an installed emulator.
5. `INCLUDE`, `INCBIN`, project format and multiple source files.
6. Memory map, symbols panel and cycle analysis.
7. Optional compatibility/undocumented-instruction mode.

## Build

Requires JDK 17 and Android SDK 35.

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
