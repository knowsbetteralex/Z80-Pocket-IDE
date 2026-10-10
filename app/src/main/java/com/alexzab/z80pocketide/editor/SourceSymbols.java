package com.alexzab.z80pocketide.editor;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure Java symbol detection for colouring label declarations and references. */
public final class SourceSymbols {
    private static final Pattern DECLARATION = Pattern.compile(
            "^\\s*([A-Za-z_@.][A-Za-z0-9_@.$]*)\\s*(?::|(?i:\\s+EQU\\b))");
    private static final Pattern WORD = Pattern.compile(
            "(?<![A-Za-z0-9_@.$])([A-Za-z_@.][A-Za-z0-9_@.$]*)(?![A-Za-z0-9_@.$])");

    private SourceSymbols() {}

    /** Ignore literal strings and comments; preserve code positions for span offsets. */
    public static String maskLiterals(String line) {
        StringBuilder out = new StringBuilder(line.length());
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote != 0) {
                out.append(' ');
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quote = 0;
                continue;
            }
            if (c == ';') {
                while (i < line.length()) {
                    out.append(' ');
                    i++;
                }
                break;
            }
            if (c == '"' || c == '\'') {
                // AF' is a Z80 register name, not a single-quoted string.
                if (c == '\'' && i > 0 && Character.isLetter(line.charAt(i - 1))
                        && (i + 1 == line.length()
                        || !Character.isLetterOrDigit(line.charAt(i + 1)))) {
                    out.append(c);
                    continue;
                }
                quote = c;
                out.append(' ');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static Set<String> definedLabels(String source) {
        Set<String> labels = new HashSet<>();
        if (source == null) return labels;
        for (String line : source.split("\n", -1)) {
            Matcher matcher = DECLARATION.matcher(maskLiterals(line));
            if (matcher.find()) labels.add(matcher.group(1).toUpperCase(Locale.ROOT));
        }
        return labels;
    }

    /** Returns pairs of absolute start/end offsets for all known label tokens. */
    public static int[][] labelRanges(String source) {
        if (source == null || source.isEmpty()) return new int[0][2];
        Set<String> labels = definedLabels(source);
        java.util.List<int[]> spans = new java.util.ArrayList<>();
        int start = 0;
        for (String line : source.split("\n", -1)) {
            Matcher matcher = WORD.matcher(maskLiterals(line));
            while (matcher.find()) {
                String name = matcher.group(1).toUpperCase(Locale.ROOT);
                if (labels.contains(name)) {
                    spans.add(new int[]{start + matcher.start(1), start + matcher.end(1)});
                }
            }
            start += line.length() + 1;
        }
        return spans.toArray(new int[0][]);
    }
}
