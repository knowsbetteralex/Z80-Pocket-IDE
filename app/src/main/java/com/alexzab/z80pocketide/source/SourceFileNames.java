package com.alexzab.z80pocketide.source;

import java.util.Locale;

/** Filename policy for Android document-provider ASM sources. */
public final class SourceFileNames {
    private SourceFileNames() {}

    public static String suggestedName(String name) {
        if (name == null || name.trim().isEmpty()) return "program.asm";
        String value = name.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".asm.txt")) {
            return value.substring(0, value.length() - 4);
        }
        if (lower.endsWith(".asm")) return value;
        return value + ".asm";
    }

    /** Some providers attach a text/plain .txt suffix to the requested .asm name. */
    public static boolean hasUnwantedTextSuffix(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".asm.txt");
    }
}
