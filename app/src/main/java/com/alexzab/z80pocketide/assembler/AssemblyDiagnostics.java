package com.alexzab.z80pocketide.assembler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort aggregation of independently reported assembler errors.
 * The assembler stops at the first error. We temporarily comment failing
 * source lines and repeat, without ever changing the original source.
 */
public final class AssemblyDiagnostics {
    private static final Pattern LINE = Pattern.compile("^line (\\d+):");

    public static final class Issue {
        public final int line;
        public final String message;
        public Issue(int line, String message) {
            this.line = line;
            this.message = message;
        }
    }

    private AssemblyDiagnostics() {}

    public static List<Issue> collect(String source, int maxIssues) {
        List<Issue> issues = new ArrayList<>();
        if (source == null || maxIssues <= 0) return issues;
        String[] lines = source.replace("\r", "").split("\n", -1);
        String[] probe = lines.clone();
        Set<Integer> seen = new HashSet<>();
        int cap = Math.min(maxIssues, 12);
        for (int i = 0; i < cap; i++) {
            try {
                new Assembler().assemble(String.join("\n", probe));
                break;
            } catch (IllegalArgumentException failure) {
                String message = failure.getMessage() == null ? "Assembler error" : failure.getMessage();
                Matcher match = LINE.matcher(message);
                int number = match.find() ? Integer.parseInt(match.group(1)) : 0;
                if (number <= 0 || number > lines.length || !seen.add(number)) {
                    if (issues.isEmpty()) issues.add(new Issue(0, message));
                    break;
                }
                issues.add(new Issue(number, message));
                // Retain a valid label definition on a broken instruction line
                // to avoid introducing an artificial 'unknown symbol' error.
                String raw = probe[number - 1].trim();
                int colon = raw.indexOf(':');
                if (colon > 0 && raw.substring(0, colon).matches("[A-Za-z_@.][A-Za-z0-9_@.$]*")) {
                    probe[number - 1] = raw.substring(0, colon + 1);
                } else {
                    probe[number - 1] = "; syntax diagnosis skip";
                }
            }
        }
        return issues;
    }
}
