package com.alexzab.z80pocketide.editor;

import java.util.ArrayList;
import java.util.List;

/** Literal source search without regex interpretation; offsets are UTF-16 indices. */
public final class SourceSearch {
    public static final class Match {
        public final int start;
        public final int end;

        public Match(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private SourceSearch() {}

    public static List<Match> findAll(String source, String query, boolean caseSensitive) {
        List<Match> matches = new ArrayList<>();
        if (source == null || query == null || query.isEmpty()) return matches;
        int length = query.length();
        for (int pos = 0; pos <= source.length() - length;) {
            if (source.regionMatches(!caseSensitive, pos, query, 0, length)) {
                matches.add(new Match(pos, pos + length));
                pos += length; // Non-overlapping matches, same policy as replace-all.
            } else pos++;
        }
        return matches;
    }

    /** First match beginning at/after from; wrap around if necessary. */
    public static int next(List<Match> matches, int from) {
        if (matches == null || matches.isEmpty()) return -1;
        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i).start >= from) return i;
        }
        return 0;
    }

    /** Last match ending at/before from; wrap around if necessary. */
    public static int previous(List<Match> matches, int from) {
        if (matches == null || matches.isEmpty()) return -1;
        for (int i = matches.size() - 1; i >= 0; i--) {
            if (matches.get(i).end <= from) return i;
        }
        return matches.size() - 1;
    }

    public static String replaceAll(String source, List<Match> matches, String replacement) {
        if (source == null || matches == null || matches.isEmpty()) return source;
        if (replacement == null) replacement = "";
        StringBuilder result = new StringBuilder(source.length());
        int cursor = 0;
        for (Match match : matches) {
            if (match.start < cursor || match.end > source.length()) {
                throw new IllegalArgumentException("Overlapping or invalid search match");
            }
            result.append(source, cursor, match.start);
            result.append(replacement);
            cursor = match.end;
        }
        return result.append(source, cursor, source.length()).toString();
    }
}
