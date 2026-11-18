package com.alexzab.z80pocketide.editor;

import java.util.Locale;

/**
 * Quote-aware keyboard transformations for ASM. Pure Java so text editing
 * behavior can be checked without Android IME or emulator dependencies.
 * A quote is scoped to one source line; semicolon starts an ASM comment.
 */
public final class QuoteTyping {
    private QuoteTyping() {}

    private static final class Context {
        char quote;
        boolean comment;
    }

    private static void step(Context context, CharSequence source, int index) {
        char c = source.charAt(index);
        if (c == '\n') {
            context.quote = 0;
            context.comment = false;
            return;
        }
        if (c == ';' && context.quote == 0) {
            context.comment = true;
            return;
        }
        // Text in comments is not parsed as executable source.
        if (context.comment) return;
        if (context.quote != 0) {
            if (c == context.quote) context.quote = 0;
            return;
        }
        if (c == '"') {
            context.quote = '"';
        } else if (c == '\'' && isSingleQuoteOpener(source, index)) {
            context.quote = '\'';
        }
    }

    private static boolean isSingleQuoteOpener(CharSequence source, int pos) {
        if (pos == 0) return true;
        char prev = source.charAt(pos - 1);
        // AF' is a Z80 register, not a string start.
        return !Character.isLetterOrDigit(prev) && prev != '_';
    }

    private static Context contextBefore(CharSequence source, int position) {
        Context result = new Context();
        int end = Math.max(0, Math.min(position, source.length()));
        int start = end;
        while (start > 0 && source.charAt(start - 1) != '\n') start--;
        for (int i = start; i < end; i++) step(result, source, i);
        return result;
    }

    public static boolean isInsideDoubleQuotes(CharSequence source, int position) {
        return contextBefore(source, position).quote == '"';
    }

    /** A typed opening double quote gets a matching quote, except inside comments. */
    public static boolean shouldPair(CharSequence source, int insertionIndex) {
        if (source == null || insertionIndex < 0 || insertionIndex >= source.length()
                || source.charAt(insertionIndex) != '"') return false;
        Context before = contextBefore(source, insertionIndex);
        return before.quote == 0 && !before.comment
                && (insertionIndex + 1 >= source.length()
                    || source.charAt(insertionIndex + 1) != '"');
    }

    /** When typing over the generated closing quote, consume it instead of duplicating it. */
    public static boolean shouldSkipClosing(CharSequence source, int insertionIndex) {
        return source != null && insertionIndex >= 0
                && insertionIndex + 1 < source.length()
                && source.charAt(insertionIndex) == '"'
                && source.charAt(insertionIndex + 1) == '"'
                && isInsideDoubleQuotes(source, insertionIndex);
    }

    /**
     * Apply uppercase-typing mode only outside quoted literals.
     * Unlike formatting whole lines, this preserves the case of typed string
     * content (including pasted strings) while preserving code capitalization.
     */
    public static String uppercaseOutsideLiterals(CharSequence source, int start, int end) {
        if (source == null) return "";
        int from = Math.max(0, Math.min(start, source.length()));
        int to = Math.max(from, Math.min(end, source.length()));
        Context context = contextBefore(source, from);
        StringBuilder output = new StringBuilder(to - from);
        for (int i = from; i < to; i++) {
            char value = source.charAt(i);
            if (context.quote != 0 || value == '"' || value == '\''
                    || (context.comment && value == ';')) {
                output.append(value);
            } else {
                output.append(String.valueOf(value).toUpperCase(Locale.ROOT));
            }
            step(context, source, i);
        }
        return output.toString();
    }
}
