package com.alexzab.z80pocketide.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Parses literal bytes from Z80 assembly DB / DEFB lines and bare HEX/BIN dumps. */
public final class SpriteDbParser {
    private SpriteDbParser() {}

    public static int[] parse(String source) {
        if (source == null || source.trim().isEmpty())
            throw new IllegalArgumentException("Paste DB bytes first");
        List<Integer> values = new ArrayList<>();
        for (String line : source.split("\\r?\\n")) {
            String statement = line.split(";", 2)[0].trim();
            if (statement.isEmpty()) continue;
            // Optional label prefix, including "SPRITE: DB ...".
            if (statement.matches("^[A-Za-z_][\\w.]*\\s*:.*"))
                statement = statement.substring(statement.indexOf(':') + 1).trim();
            if (statement.isEmpty()) continue;
            statement = statement.replaceFirst("(?i)^\\.?DEFB?\\b\\s*", "");
            // Also recognize BYTE / .BYTE, useful when pasting external byte dumps.
            statement = statement.replaceFirst("(?i)^\\.?BYTE\\b\\s*", "");
            if (statement.isEmpty()) continue;
            String[] tokens = statement.split("[,\\s]+");
            for (String token : tokens) {
                if (token.isEmpty()) continue;
                if (values.size() >= 512) throw new IllegalArgumentException("More than 512 bytes");
                values.add(parseByte(token));
            }
        }
        if (values.isEmpty()) throw new IllegalArgumentException("No DB bytes found");
        int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = values.get(i);
        return result;
    }

    public static int parseByte(String token) {
        String value = token.trim().replace("_", "");
        if (value.isEmpty()) throw new IllegalArgumentException("Empty byte");
        int radix;
        if (value.startsWith("$") || value.startsWith("#")) {
            value = value.substring(1);
            radix = 16;
        } else if (value.startsWith("%")) {
            value = value.substring(1);
            radix = 2;
        } else if (value.toLowerCase(Locale.ROOT).startsWith("0x")) {
            value = value.substring(2);
            radix = 16;
        } else if (value.toLowerCase(Locale.ROOT).startsWith("0b")) {
            value = value.substring(2);
            radix = 2;
        } else if (value.matches("(?i)[0-9a-f]+h")) {
            value = value.substring(0, value.length() - 1);
            radix = 16;
        } else if (value.matches("[01]{8}[bB]")) {
            value = value.substring(0, value.length() - 1);
            radix = 2;
        } else if (value.matches("[0-9]+")) {
            radix = 10;
        } else if (value.matches("(?i)[0-9a-f]{1,2}")) {
            radix = 16;
        } else {
            throw new IllegalArgumentException("Invalid byte: " + token);
        }
        try {
            int result = Integer.parseInt(value, radix);
            if (result >= 0 && result <= 255) return result;
        } catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException("Byte out of range: " + token);
    }
}
