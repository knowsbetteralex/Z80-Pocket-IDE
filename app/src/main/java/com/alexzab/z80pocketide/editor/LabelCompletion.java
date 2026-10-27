package com.alexzab.z80pocketide.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Platform-independent, bounded suggestions for labels already declared in the file. */
public final class LabelCompletion {
    public static final class Match {
        public final String label;
        /** Replacement positions within the current visible editor line. */
        public final int start, end;
        Match(String label, int start, int end) {
            this.label = label;
            this.start = start;
            this.end = end;
        }
    }

    private LabelCompletion() {}

    public static List<Match> suggest(String completeSource, String line,
                                      int caretInLine, int maxSuggestions) {
        List<Match> result = new ArrayList<>();
        if (line == null || caretInLine < 0 || caretInLine > line.length()
                || maxSuggestions <= 0) return result;
        int start = caretInLine;
        while (start > 0 && isLabelPart(line.charAt(start - 1))) start--;
        // One-letter prefixes mostly produce noise and too many results.
        if (caretInLine - start < 2) return result;

        // An identifier at the start of a line is being DEFINED, not used.
        if (line.substring(0, start).trim().isEmpty()) return result;

        // Do not offer labels inside quoted literals or after a semicolon.
        String codeOnly = SourceSymbols.maskLiterals(line);
        for (int i = start; i < caretInLine; i++)
            if (codeOnly.charAt(i) == ' ') return result;

        int end = caretInLine;
        while (end < line.length() && isLabelPart(line.charAt(end))) end++;
        String prefix = line.substring(start, caretInLine).toUpperCase(Locale.ROOT);
        for (String label : SourceSymbols.definedLabelNames(completeSource)) {
            if (label.toUpperCase(Locale.ROOT).startsWith(prefix)
                    && !label.equalsIgnoreCase(line.substring(start, end))) {
                result.add(new Match(label, start, end));
                if (result.size() >= maxSuggestions) break;
            }
        }
        return result;
    }

    private static boolean isLabelPart(char c) {
        return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'
                || c >= '0' && c <= '9' || c == '_' || c == '@'
                || c == '.' || c == '$';
    }
}
