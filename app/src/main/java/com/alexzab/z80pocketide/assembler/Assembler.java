package com.alexzab.z80pocketide.assembler;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Very small bootstrap assembler. It intentionally implements only a handful
 * of Z80 forms so the Android/UI/build pipeline has executable core logic from day one.
 * The next milestone replaces the switch with a declarative instruction table.
 */
public final class Assembler {
    public AssemblyResult assemble(String source) {
        String[] lines = source.replace("\r", "").split("\n");
        Map<String, Integer> symbols = new HashMap<>();
        int origin = 0;
        int pc = 0;
        boolean originSeen = false;

        // Pass 1: labels and sizes.
        for (int i = 0; i < lines.length; i++) {
            ParsedLine line = parseLine(lines[i]);
            if (line.label != null) {
                String key = line.label.toUpperCase(Locale.ROOT);
                if (symbols.put(key, pc) != null) {
                    throw error(i, "duplicate label " + line.label);
                }
            }
            if (line.code.isEmpty()) continue;
            String upper = line.code.toUpperCase(Locale.ROOT);
            if (upper.startsWith("ORG ")) {
                int value = parseNumber(line.code.substring(4).trim(), symbols, i);
                if (!originSeen) {
                    origin = value;
                    pc = value;
                    originSeen = true;
                } else {
                    pc = value;
                }
            } else {
                pc += instructionSize(line.code, i);
            }
        }

        // Pass 2: bytes.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pc = origin;
        for (int i = 0; i < lines.length; i++) {
            ParsedLine line = parseLine(lines[i]);
            if (line.code.isEmpty()) continue;
            String upper = line.code.toUpperCase(Locale.ROOT);
            if (upper.startsWith("ORG ")) {
                pc = parseNumber(line.code.substring(4).trim(), symbols, i);
                continue;
            }
            byte[] encoded = encode(line.code, pc, symbols, i);
            out.write(encoded, 0, encoded.length);
            pc += encoded.length;
        }
        return new AssemblyResult(origin, out.toByteArray());
    }

    private static int instructionSize(String code, int line) {
        String s = normalize(code);
        if (s.matches("LD A,.+")) return 2;
        if (s.matches("OUT \\(254\\),A")) return 2;
        if (s.matches("JP .+")) return 3;
        if (s.matches("JR .+")) return 2;
        if (s.equals("RET") || s.equals("NOP") || s.equals("HALT")) return 1;
        if (s.startsWith("DB ")) return splitArgs(code.substring(code.toUpperCase(Locale.ROOT).indexOf("DB") + 2)).length;
        if (s.startsWith("DW ")) return splitArgs(code.substring(code.toUpperCase(Locale.ROOT).indexOf("DW") + 2)).length * 2;
        throw error(line, "unsupported instruction: " + code.trim());
    }

    private static byte[] encode(String code, int pc, Map<String, Integer> symbols, int line) {
        String s = normalize(code);
        if (s.startsWith("LD A,")) {
            int n = parseNumber(code.substring(code.indexOf(',') + 1).trim(), symbols, line);
            return new byte[]{(byte) 0x3E, (byte) n};
        }
        if (s.equals("OUT (254),A")) return new byte[]{(byte) 0xD3, (byte) 0xFE};
        if (s.startsWith("JP ")) {
            int nn = parseNumber(code.trim().substring(3).trim(), symbols, line);
            return new byte[]{(byte) 0xC3, (byte) nn, (byte) (nn >>> 8)};
        }
        if (s.startsWith("JR ")) {
            int target = parseNumber(code.trim().substring(3).trim(), symbols, line);
            int disp = target - (pc + 2);
            if (disp < -128 || disp > 127) throw error(line, "JR target out of range");
            return new byte[]{0x18, (byte) disp};
        }
        if (s.equals("RET")) return new byte[]{(byte) 0xC9};
        if (s.equals("NOP")) return new byte[]{0x00};
        if (s.equals("HALT")) return new byte[]{0x76};
        if (s.startsWith("DB ")) {
            String[] args = splitArgs(code.trim().substring(2));
            byte[] bytes = new byte[args.length];
            for (int i = 0; i < args.length; i++) bytes[i] = (byte) parseNumber(args[i].trim(), symbols, line);
            return bytes;
        }
        if (s.startsWith("DW ")) {
            String[] args = splitArgs(code.trim().substring(2));
            byte[] bytes = new byte[args.length * 2];
            for (int i = 0; i < args.length; i++) {
                int v = parseNumber(args[i].trim(), symbols, line);
                bytes[i * 2] = (byte) v;
                bytes[i * 2 + 1] = (byte) (v >>> 8);
            }
            return bytes;
        }
        throw error(line, "unsupported instruction: " + code.trim());
    }

    private static String normalize(String code) {
        return code.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static String[] splitArgs(String s) {
        return s.trim().split("\\s*,\\s*");
    }

    private static int parseNumber(String token, Map<String, Integer> symbols, int line) {
        String t = token.trim();
        Integer symbol = symbols.get(t.toUpperCase(Locale.ROOT));
        if (symbol != null) return symbol;
        try {
            if (t.startsWith("$") || t.startsWith("#")) return Integer.parseInt(t.substring(1), 16);
            if (t.startsWith("%")) return Integer.parseInt(t.substring(1), 2);
            if (t.endsWith("h") || t.endsWith("H")) return Integer.parseInt(t.substring(0, t.length() - 1), 16);
            return Integer.parseInt(t);
        } catch (NumberFormatException ex) {
            throw error(line, "unknown symbol or number: " + token);
        }
    }

    private static ParsedLine parseLine(String raw) {
        String noComment = raw.split(";", 2)[0].trim();
        if (noComment.isEmpty()) return new ParsedLine(null, "");
        int colon = noComment.indexOf(':');
        if (colon >= 0) {
            String label = noComment.substring(0, colon).trim();
            String code = noComment.substring(colon + 1).trim();
            return new ParsedLine(label, code);
        }
        return new ParsedLine(null, noComment);
    }

    private static IllegalArgumentException error(int zeroBasedLine, String message) {
        return new IllegalArgumentException("line " + (zeroBasedLine + 1) + ": " + message);
    }

    private static final class ParsedLine {
        final String label;
        final String code;
        ParsedLine(String label, String code) {
            this.label = label;
            this.code = code;
        }
    }
}
