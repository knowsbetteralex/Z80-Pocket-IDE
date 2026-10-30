package com.alexzab.z80pocketide.assembler;

/**
 * Dot-prefixed labels have the most recent non-local colon label as their scope.
 * Both "loop.asm" and "MODULE.LOOP" remain ordinary global names: only a
 * leading dot denotes a scoped label.
 *
 * Example: MOVE: / .again: / JR .again becomes JR MOVE.again.
 */
final class LocalLabels {
    private LocalLabels() {}

    static String declaration(String name, String scope, int line) {
        if (name == null || !name.startsWith(".")) return name;
        if (scope == null)
            throw new IllegalArgumentException("line " + line
                    + ": local label " + name + " has no preceding global label");
        return scope + name;
    }

    static String references(String code, String scope, int line) {
        if (code == null || code.indexOf('.') < 0) return code;
        StringBuilder output = new StringBuilder(code.length() + 20);
        char quote = 0;
        boolean escape = false;
        for (int i = 0; i < code.length();) {
            char ch = code.charAt(i);
            if (quote != 0) {
                output.append(ch);
                if (escape) escape = false;
                else if (ch == '\\') escape = true;
                else if (ch == quote) quote = 0;
                i++;
                continue;
            }
            if (ch == '"' || ch == '\'') {
                // AF' is a register token, not the start of a string literal.
                if (ch == '\'' && i > 0 && Character.isLetter(code.charAt(i - 1))
                        && (i + 1 == code.length() || !isPart(code.charAt(i + 1)))) {
                    output.append(ch);
                    i++;
                    continue;
                }
                quote = ch;
                output.append(ch);
                i++;
                continue;
            }
            if (ch == '.' && i + 1 < code.length()
                    && isStart(code.charAt(i + 1))
                    && (i == 0 || !isPart(code.charAt(i - 1)))) {
                int end = i + 2;
                while (end < code.length() && isPart(code.charAt(end))) end++;
                if (scope == null)
                    throw new IllegalArgumentException("line " + line
                            + ": local label " + code.substring(i, end)
                            + " has no preceding global label");
                output.append(scope).append(code, i, end);
                i = end;
                continue;
            }
            output.append(ch);
            i++;
        }
        return output.toString();
    }

    private static boolean isStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '@';
    }

    private static boolean isPart(char c) {
        return Character.isLetterOrDigit(c)
                || c == '_' || c == '.' || c == '@' || c == '$';
    }
}
